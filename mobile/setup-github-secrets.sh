#!/usr/bin/env bash
# setup-github-secrets.sh – tajemství GitHub Actions pro repozitář usharik/statika-trener (převzato z Ear4Music2).
#
# Workflowy, které je čtou:
#   .github/workflows/testflight.yml   iOS → TestFlight            ASC_*, IOS_*
#   .github/workflows/asc-setup.yml    App Store Connect API        ASC_*
#   .github/workflows/deploy.yml       Android → Google Play        ANDROID_*, GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
#
# Zdroje (nic z nich nepatří do gitu – .gitignore je vynechává):
#   local.properties                       statika.signing.{storeFile,storePassword,keyAlias,keyPassword},
#                                          statika.admob.{appId,bannerId,interstitialId}  (ID AdMob pro Android)
#   app/google-services.json               Firebase, Android aplikace cz.usharik.statika
#   pc-api-key.json                          JSON servisního účtu Google Play (Play Console → Nastavení → Přístup k API)
#   iosApp/Statika/GoogleService-Info.plist Firebase, iOS aplikace cz.usharik.statika
#   iosApp/signing/dist.p12                certifikát Apple Distribution (+ heslo v IOS_DIST_P12_PASSWORD)
#   iosApp/signing/Statika_App_Store.mobileprovision   profil „Statika App Store“ pro cz.usharik.statika
#   iosApp/signing/AuthKey_<KEYID>.p8      klíč App Store Connect API (role App Manager)
#   proměnné prostředí ASC_KEY_ID, ASC_ISSUER_ID, IOS_DIST_P12_PASSWORD
#
# Použití (z adresáře mobile/; potřebuje přihlášené gh):
#   ./setup-github-secrets.sh                 # jen vypíše, co by nastavil a co chybí (nic nemění)
#   ./setup-github-secrets.sh --apply         # nastaví vše, co má k dispozici
#   ./setup-github-secrets.sh --apply ios     # jen iOS (nebo: android)
set -euo pipefail
cd "$(dirname "$0")"

REPO="usharik/statika-trener"
APPLY=0
ONLY=all
for a in "$@"; do
    case "$a" in
        --apply) APPLY=1 ;;
        ios|android) ONLY="$a" ;;
        *) echo "unknown argument: $a" >&2; exit 2 ;;
    esac
done

die()  { echo "❌  $*" >&2; exit 1; }
ok()   { echo "✅  $*"; }
miss() { echo "⚠️   $*"; MISSING=$((MISSING + 1)); }
MISSING=0

b64() { openssl base64 -A -in "$1"; }

prop_get() {
    [[ -f local.properties ]] || return 0
    { grep -E "^$1\s*=" local.properties || true; } | head -1 | sed 's/^[^=]*=\s*//' | tr -d '[:space:]'
}

# set_secret NAME VALUE – při --apply nastaví, jinak jen ohlásí (hodnotu nikdy nevypisuje)
set_secret() {
    local name="$1" value="$2"
    if [[ -z "$value" ]]; then miss "$name: no value"; return; fi
    if [[ $APPLY -eq 1 ]]; then
        printf '%s' "$value" | gh secret set "$name" --repo "$REPO"
        ok "set $name"
    else
        ok "would set $name (${#value} chars)"
    fi
}

if [[ $APPLY -eq 1 ]]; then
    command -v gh >/dev/null 2>&1 || die "gh CLI not found (https://cli.github.com)"
    gh auth status >/dev/null 2>&1 || die "not authenticated: gh auth login"
fi
echo "Repository: $REPO  ($([[ $APPLY -eq 1 ]] && echo "APPLY" || echo "dry run – add --apply to set"))"

# ── Android ────────────────────────────────────────────────────────────────────
if [[ $ONLY != ios ]]; then
    echo; echo "── Android (deploy.yml)"
    STORE_FILE=$(prop_get statika.signing.storeFile)
    if [[ -n "$STORE_FILE" && -f "$STORE_FILE" ]]; then
        set_secret ANDROID_KEYSTORE_BASE64 "$(b64 "$STORE_FILE")"
    else
        miss "ANDROID_KEYSTORE_BASE64: statika.signing.storeFile in local.properties missing or file not found"
    fi
    set_secret ANDROID_KEYSTORE_PASSWORD "$(prop_get statika.signing.storePassword)"
    set_secret ANDROID_KEY_ALIAS "$(prop_get statika.signing.keyAlias)"
    set_secret ANDROID_KEY_PASSWORD "$(prop_get statika.signing.keyPassword)"
    for k in appId:ANDROID_ADMOB_APP_ID bannerId:ANDROID_ADMOB_BANNER_ID interstitialId:ANDROID_ADMOB_INTERSTITIAL_ID; do
        v=$(prop_get "statika.admob.${k%%:*}")
        case "$v" in ca-app-pub-3940256099942544*) miss "${k##*:}: Google's test id in local.properties"; continue ;; esac
        set_secret "${k##*:}" "$v"
    done
    if [[ -f app/google-services.json ]]; then
        grep -q '"cz.usharik.statika"' app/google-services.json || miss "app/google-services.json has no client cz.usharik.statika"
        set_secret ANDROID_GOOGLE_SERVICES_JSON "$(cat app/google-services.json)"
    else
        miss "ANDROID_GOOGLE_SERVICES_JSON: app/google-services.json not found"
    fi
    if [[ -f pc-api-key.json ]]; then
        set_secret GOOGLE_PLAY_SERVICE_ACCOUNT_JSON "$(cat pc-api-key.json)"
    else
        miss "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON: pc-api-key.json not found"
    fi
fi

# ── iOS ────────────────────────────────────────────────────────────────────────
if [[ $ONLY != android ]]; then
    echo; echo "── iOS (testflight.yml, asc-setup.yml)"
    set_secret ASC_KEY_ID "${ASC_KEY_ID:-}"
    set_secret ASC_ISSUER_ID "${ASC_ISSUER_ID:-}"
    P8=""
    [[ -n "${ASC_KEY_ID:-}" && -f "iosApp/signing/AuthKey_${ASC_KEY_ID}.p8" ]] && P8="iosApp/signing/AuthKey_${ASC_KEY_ID}.p8"
    if [[ -n "$P8" ]]; then set_secret ASC_API_KEY_P8_BASE64 "$(b64 "$P8")"; else miss "ASC_API_KEY_P8_BASE64: iosApp/signing/AuthKey_<ASC_KEY_ID>.p8 not found"; fi
    if [[ -f iosApp/signing/dist.p12 ]]; then
        set_secret IOS_DIST_P12_BASE64 "$(b64 iosApp/signing/dist.p12)"
    else
        miss "IOS_DIST_P12_BASE64: iosApp/signing/dist.p12 not found"
    fi
    set_secret IOS_DIST_P12_PASSWORD "${IOS_DIST_P12_PASSWORD:-}"
    PROFILE=iosApp/signing/Statika_App_Store.mobileprovision
    if [[ -f "$PROFILE" ]]; then
        NAME=$(security cms -D -i "$PROFILE" 2>/dev/null | plutil -extract Name raw - 2>/dev/null || echo "?")
        [[ "$NAME" == "Statika App Store" ]] || miss "$PROFILE is named \"$NAME\", project.yml expects \"Statika App Store\""
        set_secret IOS_APPSTORE_PROFILE_BASE64 "$(b64 "$PROFILE")"
    else
        miss "IOS_APPSTORE_PROFILE_BASE64: $PROFILE not found"
    fi
    PLIST=iosApp/Statika/GoogleService-Info.plist
    if [[ -f "$PLIST" ]]; then
        [[ "$(plutil -extract BUNDLE_ID raw "$PLIST")" == "cz.usharik.statika" ]] || miss "$PLIST is not for cz.usharik.statika"
        set_secret IOS_GOOGLE_SERVICE_INFO_PLIST_BASE64 "$(b64 "$PLIST")"
    else
        miss "IOS_GOOGLE_SERVICE_INFO_PLIST_BASE64: $PLIST not found"
    fi
fi

echo
if [[ $MISSING -gt 0 ]]; then echo "$MISSING item(s) missing – see above."; else echo "Everything available."; fi
[[ $APPLY -eq 1 ]] && echo "Verify with: gh secret list --repo $REPO"
exit 0
