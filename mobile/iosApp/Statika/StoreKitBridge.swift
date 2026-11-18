import StoreKit
import UIKit
import Shared

/// StoreKit 2 za společným Kotlin rozhraním `BillingSdk`; uložený stav, analytiku a pravidla nákupu
/// drží `SharedBilling` (shared/src/commonMain).
/// - „bez reklamy“ (nespotřební): vlastnictví z `Transaction.currentEntitlements`.
/// - PDF s řešením (spotřební): zaplacená transakce se ohlásí `onExportPaid` a dokončí (`finish`) až
///   v `consumeExport` – nedokončené transakce vrací StoreKit po restartu v `Transaction.unfinished`.
/// Stav (`products`, `listener`) se mění jen na hlavním vlákně (MainActor).
final class StoreKitBridge: BillingSdk {
    private let adFreeId = Products.shared.REMOVE_ADS_APP_STORE
    private let pdfId = Products.shared.PDF_SOLUTION_APP_STORE
    @MainActor private var products: [String: Product] = [:]
    private var listener: BillingListener?
    private var updates: _Concurrency.Task<Void, Never>?

    func start(listener: BillingListener) {
        self.listener = listener
        _Concurrency.Task { @MainActor in await loadProducts() }
        _Concurrency.Task { await refreshEntitlement() }
        _Concurrency.Task { await reportUnfinishedExports() }
        // nákupy dokončené mimo okno nákupu (Ask to Buy, čekající platba, jiné zařízení) a vrácení peněz
        let pdfId = self.pdfId
        updates = _Concurrency.Task.detached { [weak self] in
            for await result in Transaction.updates {
                guard case .verified(let transaction) = result else { continue }
                if transaction.productID == pdfId {
                    // dokončí se až po započtení (consumeExport)
                    if transaction.revocationDate == nil {
                        let token = String(transaction.id)
                        await MainActor.run { self?.listener?.onExportPaid(token: token) }
                    }
                    continue
                }
                await transaction.finish()
                // „nákup“ jen čerstvý (ne obnovení starého nákupu na novém zařízení)
                let fresh = transaction.revocationDate == nil && transaction.purchaseDate > Date().addingTimeInterval(-24 * 3600)
                await self?.refreshEntitlement(purchased: fresh)
            }
        }
    }

    func purchase() {
        _Concurrency.Task { @MainActor in
            guard let transaction = await buy(adFreeId) else {
                listener?.onPurchaseFinished(success: false)
                return
            }
            await transaction.finish()
            listener?.onAdFree(adFree: true, purchased: true)
        }
    }

    func purchaseExport() {
        _Concurrency.Task { @MainActor in
            guard let transaction = await buy(pdfId) else {
                listener?.onExportPurchaseFinished(success: false)
                return
            }
            listener?.onExportPaid(token: String(transaction.id))
        }
    }

    /// Ověřená transakce, nebo nil (zrušení, čekající platba – dokončí se přes Transaction.updates –, chyba).
    @MainActor private func buy(_ id: String) async -> Transaction? {
        // start bez sítě: produkty se načtou teď (jako na Androidu)
        if products[id] == nil { await loadProducts() }
        guard let product = products[id] else { return nil }
        do {
            if case .success(.verified(let transaction)) = try await product.purchase() { return transaction }
        } catch {
            NSLog("Statika: purchase failed: \(error.localizedDescription)")
        }
        return nil
    }

    func consumeExport(token: String) {
        _Concurrency.Task {
            for await result in Transaction.unfinished {
                if case .verified(let transaction) = result, String(transaction.id) == token {
                    await transaction.finish()
                }
            }
        }
    }

    /// „Obnovit nákupy“ – AppStore.sync se smí zeptat na Apple ID, proto jen na žádost uživatele.
    func restore() {
        _Concurrency.Task {
            try? await AppStore.sync()
            await refreshEntitlement()
            await reportUnfinishedExports()
        }
    }

    /// Tiché ověření při návratu do aplikace (bez dotazu na účet).
    func refresh() {
        _Concurrency.Task {
            await refreshEntitlement()
            await reportUnfinishedExports()
        }
    }

    @MainActor private func loadProducts() async {
        do {
            for product in try await Product.products(for: [adFreeId, pdfId]) { products[product.id] = product }
            listener?.onPrice(price: products[adFreeId]?.displayPrice)
            listener?.onExportPrice(price: products[pdfId]?.displayPrice)
        } catch {
            NSLog("Statika: product request failed: \(error.localizedDescription)")
        }
    }

    /// Zaplacená, ještě nedokončená stažení PDF (pád nebo ukončení aplikace mezi platbou a vytvořením PDF).
    private func reportUnfinishedExports() async {
        var tokens: [String] = []
        for await result in Transaction.unfinished {
            if case .verified(let transaction) = result, transaction.productID == pdfId, transaction.revocationDate == nil {
                tokens.append(String(transaction.id))
            }
        }
        let paid = tokens
        await MainActor.run { paid.forEach { listener?.onExportPaid(token: $0) } }
    }

    private func refreshEntitlement(purchased: Bool = false) async {
        var owned = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result, transaction.productID == adFreeId, transaction.revocationDate == nil {
                owned = true
            }
        }
        let adFree = owned
        await MainActor.run { listener?.onAdFree(adFree: adFree, purchased: purchased && adFree) }
    }
}
