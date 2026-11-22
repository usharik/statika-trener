#!/usr/bin/env bash
# Builds the signed iOS app on this Mac and uploads it to App Store Connect (TestFlight / App Review).
# Same as ~/dev/XeroxPhaserPrint/iosApp/upload-testflight.sh and CzechDeclensionQuiz: needs Xcode, XcodeGen, JDK 21
# and the Infisical CLI logged in. The team's Apple Distribution certificate and the App Store Connect API key are
# shared (team B7PSU5B367): Infisical project of CzechQuiz, env prod, folders /ios-signing and /app-store.
# The "Statika App Store" profile is downloaded from App Store Connect with that key (and created on the first run).
# .github/workflows/testflight.yml does the same on GitHub; it needs the secrets in the repository.
#
# Usage (from mobile/): iosApp/upload-testflight.sh <build number> [archive|upload|all]   (default: all)
#   archive  signs and archives build/ios/Statika-<build>.xcarchive
#   upload   exports that archive and uploads it (outward-facing: only when asked)
# The version is mobile/VERSION (shared with Android); the build number must grow with every upload.
set -euo pipefail
cd "$(dirname "$0")/.."
BUILD=${1:?build number}; MODE=${2:-all}
PROJECT_ID=2e201525-a8e8-4d99-802e-f17d1355659a
PROFILE_NAME="Statika App Store"
BUNDLE_ID=cz.usharik.statika
VERSION=$(tr -d '[:space:]' < VERSION)
[ -d /Applications/Xcode.app ] && export DEVELOPER_DIR=${DEVELOPER_DIR:-/Applications/Xcode.app/Contents/Developer}
[ -z "$(git status --porcelain)" ] || echo "WARNING: uncommitted changes in the build of $(git rev-parse --short=8 HEAD)" >&2

if [ "$MODE" != upload ]; then
    # App Privacy and the privacy policy describe the build with Firebase: without the plist it must not ship.
    PLIST=iosApp/Statika/GoogleService-Info.plist
    [ -f "$PLIST" ] || { echo "$PLIST is missing (Firebase console → statika-learn → iOS app)" >&2; exit 1; }
    [ "$(plutil -extract BUNDLE_ID raw "$PLIST")" = "$BUNDLE_ID" ] || { echo "$PLIST is not for $BUNDLE_ID" >&2; exit 1; }
    # Release with REPLACE would run without ads (AdMobBridge.ifAvailable() = nil).
    if grep -qE '^\s*ADMOB_(APP|BANNER|INTERSTITIAL)_ID: *REPLACE' iosApp/project.yml; then
        echo "iosApp/project.yml still has the AdMob placeholders" >&2; exit 1
    fi
    python3 iosApp/store/tools/lint_privacy_manifest.py iosApp/Statika/PrivacyInfo.xcprivacy
fi

# Runs the rest of this script with the secrets of both folders in the environment.
if [ -z "${DIST_P12_BASE64:-}" ]; then
    exec infisical run --projectId "$PROJECT_ID" --env prod --path /ios-signing --silent -- \
        infisical run --projectId "$PROJECT_ID" --env prod --path /app-store --silent -- "$0" "$BUILD" "$MODE"
fi

TMP=$(mktemp -d "${TMPDIR:-/tmp}/statika-sign.XXXXXX")
KEYCHAIN="$TMP/signing.keychain-db"; KEYCHAIN_PASSWORD=$(uuidgen)
ORIGINAL_KEYCHAINS=$(security list-keychains -d user | tr -d '"')
cleanup() {
    security list-keychains -d user -s $ORIGINAL_KEYCHAINS
    security delete-keychain "$KEYCHAIN" 2>/dev/null || true
    rm -rf "$TMP"
}
trap cleanup EXIT

# The certificate in a throwaway keychain, with Apple's intermediates so the identity validates on a fresh Mac.
echo "$DIST_P12_BASE64" | base64 --decode > "$TMP/dist.p12"
security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security set-keychain-settings -lut 21600 "$KEYCHAIN"
security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security import "$TMP/dist.p12" -P "$DIST_P12_PASSWORD" -A -t cert -f pkcs12 -k "$KEYCHAIN"
security set-key-partition-list -S apple-tool:,apple: -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN" > /dev/null
for ca in AppleWWDRCAG3 AppleWWDRCAG6 AppleRootCA-G3; do
    curl -sSfL -o "$TMP/$ca.cer" "https://www.apple.com/certificateauthority/$ca.cer"
    security import "$TMP/$ca.cer" -k "$KEYCHAIN" -A > /dev/null 2>&1 || true
done
security list-keychains -d user -s "$KEYCHAIN" $ORIGINAL_KEYCHAINS
security find-identity -v -p codesigning "$KEYCHAIN" | grep -q "Apple Distribution" || { echo "No valid Apple Distribution identity" >&2; exit 1; }

# The App Store profile from App Store Connect (ES256 JWT signed through openssl); created for the bundle id and
# the team certificate DIST_CERT_ID when it does not exist yet.
printf '%s' "$ASC_KEY_P8" > "$TMP/AuthKey_$ASC_KEY_ID.p8"
python3 -I - "$TMP" "$PROFILE_NAME" "$BUNDLE_ID" <<'PY'
import base64, json, os, subprocess, sys, time, urllib.parse, urllib.request
tmp, name, bundle = sys.argv[1:]; kid = os.environ["ASC_KEY_ID"]; iss = os.environ["ASC_ISSUER_ID"]
b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b"=").decode()
now = int(time.time())
header = b64(json.dumps({"alg": "ES256", "kid": kid, "typ": "JWT"}).encode())
claims = b64(json.dumps({"iss": iss, "aud": "appstoreconnect-v1", "iat": now, "exp": now + 600}).encode())
data = os.path.join(tmp, "jwt.bin"); open(data, "wb").write(f"{header}.{claims}".encode())
der = subprocess.run(["openssl", "dgst", "-sha256", "-sign", os.path.join(tmp, f"AuthKey_{kid}.p8"), data], capture_output=True, check=True).stdout
i = 2; l = der[i + 1]; r = der[i + 2:i + 2 + l]; i += 2 + l; l = der[i + 1]; s = der[i + 2:i + 2 + l]
token = f"{header}.{claims}." + b64(r[-32:].rjust(32, b"\0") + s[-32:].rjust(32, b"\0"))
def api(path, body=None):
    req = urllib.request.Request("https://api.appstoreconnect.apple.com" + path, method="POST" if body else "GET",
                                 data=json.dumps(body).encode() if body else None,
                                 headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
    return json.load(urllib.request.urlopen(req))["data"]
profiles = api("/v1/profiles?filter[name]=" + urllib.parse.quote(name)
               + "&filter[profileState]=ACTIVE&fields[profiles]=name,uuid,profileContent")
if profiles:
    profile = profiles[0]
else:
    bundles = [b for b in api("/v1/bundleIds?filter[identifier]=" + bundle) if b["attributes"]["identifier"] == bundle]
    if not bundles: sys.exit(f"bundle id {bundle} is not registered (Certificates, Identifiers & Profiles)")
    profile = api("/v1/profiles", {"data": {"type": "profiles",
        "attributes": {"name": name, "profileType": "IOS_APP_STORE"},
        "relationships": {"bundleId": {"data": {"type": "bundleIds", "id": bundles[0]["id"]}},
                          "certificates": {"data": [{"type": "certificates", "id": os.environ["DIST_CERT_ID"]}]}}}})
    print("profile created")
open(os.path.join(tmp, "appstore.mobileprovision"), "wb").write(base64.b64decode(profile["attributes"]["profileContent"]))
print("profile:", profile["attributes"]["name"], profile["attributes"]["uuid"])
PY
PROFILES="$HOME/Library/MobileDevice/Provisioning Profiles"; mkdir -p "$PROFILES"
UUID=$(security cms -D -i "$TMP/appstore.mobileprovision" | plutil -extract UUID raw -)
cp "$TMP/appstore.mobileprovision" "$PROFILES/$UUID.mobileprovision"

ARCHIVE="$PWD/build/ios/Statika-$BUILD.xcarchive"
if [ "$MODE" = archive ] || [ "$MODE" = all ]; then
    export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home)}"
    (cd iosApp && xcodegen generate)
    xcodebuild archive -project iosApp/Statika.xcodeproj -scheme Statika -configuration Release \
        -destination 'generic/platform=iOS' -archivePath "$ARCHIVE" \
        MARKETING_VERSION="$VERSION" CURRENT_PROJECT_VERSION="$BUILD" -quiet
    # Invalid privacy manifests are rejected only after the upload (ITMS-91064, TN3181); no ATT in this app.
    APP="$ARCHIVE/Products/Applications/Statika.app"
    fail=0
    while IFS= read -r m; do
        t=$(plutil -extract NSPrivacyTracking raw "$m" 2>/dev/null || echo absent)
        n=$(plutil -extract NSPrivacyTrackingDomains raw "$m" 2>/dev/null || echo 0)
        if [ "$t" = true ] && [ "$n" = 0 ]; then echo "$m: NSPrivacyTracking true without domains" >&2; fail=1; fi
        if [ "$t" != true ] && [ "$n" != 0 ]; then echo "$m: tracking domains without NSPrivacyTracking" >&2; fail=1; fi
    done < <(find "$APP" -name PrivacyInfo.xcprivacy)
    if plutil -extract NSUserTrackingUsageDescription raw "$APP/Info.plist" > /dev/null 2>&1; then
        echo "Info.plist has NSUserTrackingUsageDescription, but App Privacy says no tracking" >&2; fail=1
    fi
    [ "$(plutil -extract GADApplicationIdentifier raw "$APP/Info.plist")" != REPLACE ] || fail=1
    [ $fail = 0 ] || exit 1
    echo "Archived $(plutil -extract ApplicationProperties.CFBundleShortVersionString raw "$ARCHIVE/Info.plist") ($(plutil -extract ApplicationProperties.CFBundleVersion raw "$ARCHIVE/Info.plist")): $ARCHIVE"
fi
if [ "$MODE" = upload ] || [ "$MODE" = all ]; then
    cat > "$TMP/ExportOptions.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>method</key><string>app-store-connect</string>
  <key>destination</key><string>upload</string>
  <key>teamID</key><string>B7PSU5B367</string>
  <key>signingStyle</key><string>manual</string>
  <key>provisioningProfiles</key><dict><key>$BUNDLE_ID</key><string>$PROFILE_NAME</string></dict>
  <key>uploadSymbols</key><true/>
  <key>manageAppVersionAndBuildNumber</key><false/>
</dict></plist>
PLIST
    xcodebuild -exportArchive -archivePath "$ARCHIVE" -exportOptionsPlist "$TMP/ExportOptions.plist" -exportPath "$TMP/export" \
        -authenticationKeyPath "$TMP/AuthKey_$ASC_KEY_ID.p8" -authenticationKeyID "$ASC_KEY_ID" -authenticationKeyIssuerID "$ASC_ISSUER_ID"
    echo "Uploaded $VERSION ($BUILD); App Store Connect processes it in a few minutes."
fi
