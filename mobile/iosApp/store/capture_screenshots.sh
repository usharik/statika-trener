#!/usr/bin/env bash
# Snímky pro App Store v obou jazycích stránky (cs, ru), pořízené testem StoreScreenshotTests na nejnovějším
# iPhonu Pro Max (6,9", 1320×2868) a iPadu Pro 13" (2064×2752) – velikosti, které App Store Connect vyžaduje
# a pro menší zařízení zmenší. Převzato z Ear4Music2 (iosApp/store/capture_screenshots.sh).
# macOS s Xcode, XcodeGen a JDK 21; spouští .github/workflows/ios-screenshots.yml, jde i lokálně.
# Výstup: build/ios/store/<locale>/{iphone,ipad}-NN-název.png → zkopírovat do iosApp/store/screenshots/<locale>/,
#         build/ios/store/iap/settings-remove-ads.png → iosApp/store/iap/ (snímek pro kontrolu nákupu).
set -euo pipefail
cd "$(dirname "$0")/../.."   # mobile/

OUT=build/ios/store
rm -rf "$OUT"; mkdir -p "$OUT"

# Nejnovější runtime, který umí daný typ zařízení: „iPhone NN Pro Max“, resp. „iPad Pro 13-inch (…)“.
pick() {
    xcrun simctl list -j | python3 -c '
import json, re, sys
pattern = re.compile(sys.argv[1])
data = json.load(sys.stdin)
def version(r): return [int(n) for n in re.findall(r"\d+", r["version"])]
runtimes = sorted((r for r in data["runtimes"] if r["platform"] == "iOS" and r["isAvailable"]), key=version, reverse=True)
for runtime in runtimes:
    types = [t for t in runtime.get("supportedDeviceTypes", []) if pattern.match(t["name"])]
    if types:
        best = max(types, key=lambda t: [int(n) for n in re.findall(r"\d+", t["name"])])
        print(best["name"] + "|" + best["identifier"] + "|" + runtime["identifier"]); break
' "$1"
}

(cd iosApp && xcodegen generate)

FAILED=0
# KINDS="iphone" (nebo "ipad") pořídí jen jedno zařízení – výstupní adresář se přesto vyprázdní
for KIND in ${KINDS:-iphone ipad}; do
    if [ "$KIND" = iphone ]; then DEVICE=$(pick '^iPhone \d+ Pro Max$'); else DEVICE=$(pick '^iPad Pro 13-inch'); fi
    [ -n "$DEVICE" ] || { echo "No simulator type for $KIND" >&2; exit 1; }
    IFS='|' read -r NAME TYPE RUNTIME <<< "$DEVICE"
    UDID=$(xcrun simctl create "Store $NAME" "$TYPE" "$RUNTIME")
    echo "Simulator: $NAME ($UDID)"
    xcrun simctl boot "$UDID"
    xcrun simctl bootstatus "$UDID" -b >/dev/null
    # stavový řádek jako na snímcích Applu
    xcrun simctl status_bar "$UDID" override --time 9:41 --dataNetwork wifi --wifiMode active --wifiBars 3 \
        --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100

    DERIVED="$OUT/DerivedData-$KIND"
    xcodebuild build-for-testing -project iosApp/Statika.xcodeproj -scheme Statika -destination "id=$UDID" \
        -derivedDataPath "$DERIVED" CODE_SIGNING_ALLOWED=NO >"$OUT/build-$KIND.log" 2>&1 || { tail -100 "$OUT/build-$KIND.log"; exit 1; }

    # jazyk stránky App Store : jazyk aplikace
    for pair in cs:cs ru:ru; do
        LOCALE=${pair%%:*}; LANG_TAG=${pair##*:}
        echo "=== $KIND $LOCALE"
        RESULT="$OUT/$KIND-$LOCALE.xcresult"
        # xcodebuild předá TEST_RUNNER_<NAME> testovacímu procesu jako <NAME>.
        TEST_RUNNER_STORE_LANGUAGE=$LANG_TAG xcodebuild test-without-building -project iosApp/Statika.xcodeproj \
            -scheme Statika -destination "id=$UDID" -derivedDataPath "$DERIVED" -resultBundlePath "$RESULT" \
            -only-testing:StatikaUITests/StoreScreenshotTests -parallel-testing-enabled NO CODE_SIGNING_ALLOWED=NO \
            >"$OUT/$KIND-$LOCALE.log" 2>&1 || { tail -60 "$OUT/$KIND-$LOCALE.log"; echo "::warning::$KIND $LOCALE screenshots incomplete"; FAILED=1; }
        xcrun xcresulttool export attachments --path "$RESULT" --output-path "$OUT/raw-$KIND-$LOCALE" >/dev/null
        python3 - "$OUT/raw-$KIND-$LOCALE" "$OUT/$LOCALE" "$KIND" "$OUT/iap" <<'PY'
import json, os, re, shutil, sys
src, dst, kind, iap = sys.argv[1:5]
os.makedirs(dst, exist_ok=True)
def walk(node):
    if isinstance(node, dict):
        if "exportedFileName" in node:
            yield node
        for v in node.values():
            yield from walk(v)
    elif isinstance(node, list):
        for v in node:
            yield from walk(v)
for item in walk(json.load(open(os.path.join(src, "manifest.json")))):
    name = item.get("suggestedHumanReadableName", "")
    m = re.match(r"store-(\d\d-[a-z-]+)", name)
    if m:
        shutil.copy(os.path.join(src, item["exportedFileName"]), os.path.join(dst, f"{kind}-{m.group(1)}.png"))
    # snímek nastavení s nákupem „bez reklamy“ pro kontrolu nákupu (stačí jeden: iPhone, čeština)
    if name.startswith("iap-settings") and kind == "iphone" and dst.endswith("/cs"):
        os.makedirs(iap, exist_ok=True)
        shutil.copy(os.path.join(src, item["exportedFileName"]), os.path.join(iap, "settings-remove-ads.png"))
print(dst, sorted(f for f in os.listdir(dst) if f.startswith(kind)))
PY
        rm -rf "$OUT/raw-$KIND-$LOCALE"
    done
    xcrun simctl shutdown "$UDID" || true
    xcrun simctl delete "$UDID" || true
done
exit "$FAILED"
