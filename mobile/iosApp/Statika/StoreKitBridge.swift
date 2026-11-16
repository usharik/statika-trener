import StoreKit
import UIKit
import Shared

/// StoreKit 2 pro jednorázový produkt „bez reklamy“ za společným Kotlin rozhraním `BillingSdk`;
/// uložený stav, analytiku a pravidla nákupu drží `SharedBilling` (shared/src/commonMain).
/// Stav (`product`, `listener`) se mění jen na hlavním vlákně (MainActor).
final class StoreKitBridge: BillingSdk {
    private let productId = Products.shared.REMOVE_ADS_APP_STORE
    @MainActor private var product: Product?
    private var listener: BillingListener?
    private var updates: _Concurrency.Task<Void, Never>?

    func start(listener: BillingListener) {
        self.listener = listener
        _Concurrency.Task { @MainActor in await loadProduct() }
        _Concurrency.Task { await refreshEntitlement() }
        // nákupy dokončené mimo okno nákupu (Ask to Buy, čekající platba, jiné zařízení) a vrácení peněz
        updates = _Concurrency.Task.detached { [weak self] in
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await transaction.finish()
                    // „nákup“ jen čerstvý (ne obnovení starého nákupu na novém zařízení)
                    let fresh = transaction.revocationDate == nil && transaction.purchaseDate > Date().addingTimeInterval(-24 * 3600)
                    await self?.refreshEntitlement(purchased: fresh)
                }
            }
        }
    }

    func purchase() {
        _Concurrency.Task { @MainActor in
            // start bez sítě: produkt se načte teď (jako na Androidu)
            if product == nil { await loadProduct() }
            guard let product else {
                listener?.onPurchaseFinished(success: false)
                return
            }
            do {
                let result = try await product.purchase()
                switch result {
                case .success(let verification):
                    if case .verified(let transaction) = verification {
                        await transaction.finish()
                        listener?.onAdFree(adFree: true, purchased: true)
                    } else {
                        listener?.onPurchaseFinished(success: false)
                    }
                case .pending, .userCancelled:
                    // čekající platba se dokončí přes Transaction.updates
                    listener?.onPurchaseFinished(success: false)
                @unknown default:
                    listener?.onPurchaseFinished(success: false)
                }
            } catch {
                NSLog("Statika: purchase failed: \(error.localizedDescription)")
                listener?.onPurchaseFinished(success: false)
            }
        }
    }

    /// „Obnovit nákupy“ – AppStore.sync se smí zeptat na Apple ID, proto jen na žádost uživatele.
    func restore() {
        _Concurrency.Task {
            try? await AppStore.sync()
            await refreshEntitlement()
        }
    }

    /// Tiché ověření při návratu do aplikace (bez dotazu na účet).
    func refresh() {
        _Concurrency.Task { await refreshEntitlement() }
    }

    @MainActor private func loadProduct() async {
        do {
            product = try await Product.products(for: [productId]).first
            listener?.onPrice(price: product?.displayPrice)
        } catch {
            NSLog("Statika: product request failed: \(error.localizedDescription)")
        }
    }

    private func refreshEntitlement(purchased: Bool = false) async {
        var owned = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result, transaction.productID == productId, transaction.revocationDate == nil {
                owned = true
            }
        }
        let adFree = owned
        await MainActor.run { listener?.onAdFree(adFree: adFree, purchased: purchased && adFree) }
    }
}
