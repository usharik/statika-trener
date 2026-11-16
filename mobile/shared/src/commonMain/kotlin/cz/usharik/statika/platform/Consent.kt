package cz.usharik.statika.platform

/*
 * Souhlas uživatele (Google UMP + IAB TCF v2) – stejné rozhodování na Androidu i iOS.
 *
 * Pozor: „canRequestAds“ z UMP NENÍ souhlas. Je true i po „Nesouhlasím“ (reklama se pak smí žádat
 * jako neosobní/omezená). Co uživatel opravdu dovolil, je v řetězcích TCF, které UMP ukládá
 * do SharedPreferences / NSUserDefaults (IABTCF_gdprApplies, IABTCF_PurposeConsents).
 */

/** Stav souhlasu, jak ho přečte platforma (UMP + uložené klíče TCF). */
data class ConsentSnapshot(
    /** UMP: smí se žádat reklama (i neosobní) */
    val canRequestAds: Boolean,
    /** UMP: aplikace musí nabízet „možnosti soukromí“ (změnu souhlasu) */
    val privacyOptionsRequired: Boolean,
    /** UMP: souhlas se v této oblasti nevyžaduje (consentStatus == NOT_REQUIRED) */
    val consentNotRequired: Boolean,
    /** IABTCF_gdprApplies: 1 = platí GDPR, 0 = neplatí, null = CMP nic neuložila */
    val gdprApplies: Int?,
    /** IABTCF_PurposeConsents: „0“/„1“ pro účely 1, 2, 3 … (index 0 = účel 1) */
    val purposeConsents: String?,
)

/** Co z rozhodnutí plyne pro Firebase Analytics (consent mode). */
data class AnalyticsConsent(
    /** sběr analytiky (analytics_storage) */
    val analytics: Boolean,
    /** ad_storage – jen pro propojení s AdMob; podle účelu 1 */
    val adStorage: Boolean,
    /** ad_user_data – podle účelů 1 a 7 (měření reklamy) */
    val adUserData: Boolean,
) {
    companion object {
        val DENIED = AnalyticsConsent(analytics = false, adStorage = false, adUserData = false)
        val GRANTED = AnalyticsConsent(analytics = true, adStorage = true, adUserData = true)
    }
}

/** Účel TCF (číslováno od 1) odsouhlasený v řetězci IABTCF_PurposeConsents. */
fun tcfPurpose(purposeConsents: String?, purpose: Int): Boolean =
    purposeConsents != null && purpose >= 1 && purposeConsents.getOrNull(purpose - 1) == '1'

/**
 * Rozhodnutí o analytice z uloženého souhlasu:
 * - GDPR neplatí (gdprApplies = 0) → povoleno;
 * - GDPR platí → analytika jen s účelem 1 (ukládání na zařízení – Firebase si ukládá id instance)
 *   a zároveň aspoň jedním měřicím účelem 8 (měření výkonu obsahu) nebo 9 (statistiky publika);
 *   „Nesouhlasím“ (vše 0) → nic; ad_storage = účel 1, ad_user_data = účely 1 + 7;
 * - TCF chybí (CMP nic neuložila) → povoleno jen tam, kde UMP souhlas nevyžaduje; jinak nic (opatrně).
 * Personalizace reklam (ad_personalization) se nepovoluje nikdy – uživatelé jsou studenti.
 */
fun analyticsConsent(s: ConsentSnapshot): AnalyticsConsent = when {
    s.gdprApplies == 0 -> AnalyticsConsent.GRANTED
    s.gdprApplies == 1 -> {
        val p1 = tcfPurpose(s.purposeConsents, 1)
        AnalyticsConsent(
            analytics = p1 && (tcfPurpose(s.purposeConsents, 8) || tcfPurpose(s.purposeConsents, 9)),
            adStorage = p1,
            adUserData = p1 && tcfPurpose(s.purposeConsents, 7),
        )
    }
    s.consentNotRequired -> AnalyticsConsent.GRANTED
    else -> AnalyticsConsent.DENIED
}

/** Hodnota IABTCF_gdprApplies z úložiště (některé CMP ukládají číslo, jiné řetězec). */
fun parseGdprApplies(raw: Any?): Int? = when (raw) {
    is Number -> raw.toInt()
    is String -> raw.trim().toIntOrNull()
    is Boolean -> if (raw) 1 else 0
    else -> null
}?.takeIf { it == 0 || it == 1 }

/** Klíče IAB TCF v2, které UMP ukládá. */
object TcfKeys {
    const val GDPR_APPLIES = "IABTCF_gdprApplies"
    const val PURPOSE_CONSENTS = "IABTCF_PurposeConsents"
}
