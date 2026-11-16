#!/usr/bin/env bash
# Sestaví a otestuje iOS aplikaci; potřebuje macOS s Xcode, XcodeGen a JDK 21. Používá .github/workflows/ios.yml
# a stejně funguje na Macu. Postup (jako ~/dev/Ear4Music2/iosApp/ci.sh):
#   1. sdílené Kotlin testy na simulátoru iOS + link frameworku pro zařízení;
#   2. aplikace a UI testy sestavené jednou (build-for-testing);
#   3. UI testy na iPhonu a pak na iPadu (aplikace je univerzální);
#   4. snímky obrazovky z testů jako PNG: build/ios/<zařízení>/screenshots/<TestClass>/.
# Simulátory: IPHONE_UDID / IPAD_UDID, jinak nejnovější dostupný iPhone a iPad.
set -euo pipefail
cd "$(dirname "$0")/.."
# vybrané Xcode; když je vybrané jen Command Line Tools (bez simulátorů), vezme /Applications/Xcode.app
if [ -z "${DEVELOPER_DIR:-}" ]; then
    case "$(xcode-select -p)" in
        *CommandLineTools*) export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ;;
    esac
fi

OUT=build/ios
rm -rf "$OUT"
mkdir -p "$OUT"

# Simulátor pro testy: vlastní „Statika iPhone“ / „Statika iPad“, jinak nejnovější běžný model
# („iPhone N Pro“, „iPad Pro …“); zvláštní tvary (skládací „iPhone Duo“ apod.) jen jako poslední možnost.
newest() {
    xcrun simctl list devices available -j | python3 -c '
import json, re, sys
prefix = sys.argv[1]
devices = json.load(sys.stdin)["devices"]
def version(runtime): return [int(n) for n in re.findall(r"\d+", runtime.split("iOS-")[-1])]
runtimes = sorted((r for r in devices if ".iOS-" in r), key=version, reverse=True)
regular = re.compile(r"^iPhone \d+ Pro$" if prefix == "iPhone" else r"^iPad Pro ")
for rule in (lambda n: n == "Statika " + prefix, lambda n: bool(regular.match(n)), lambda n: n.startswith(prefix)):
    found = [d for r in runtimes for d in devices[r] if rule(d["name"])]
    if found:
        print(found[0]["udid"]); break
' "$1"
}
IPHONE="${IPHONE_UDID:-$(newest iPhone)}"
IPAD="${IPAD_UDID:-$(newest iPad)}"
[ -n "$IPHONE" ] && [ -n "$IPAD" ] || { echo "No iPhone / iPad simulator available" >&2; exit 1; }

# 1. sdílené testy na simulátoru a framework pro zařízení (chyba linkování se ukáže dřív než v TestFlightu)
./gradlew :shared:iosSimulatorArm64Test :shared:linkReleaseFrameworkIosArm64 -PiosSimulator="$IPHONE" --console=plain

# 2. sestavení aplikace a UI testů
(cd iosApp && xcodegen generate)
BUILD_STATUS=0
xcodebuild build-for-testing \
    -project iosApp/Statika.xcodeproj \
    -scheme Statika \
    -destination "id=$IPHONE" \
    -derivedDataPath "$OUT/DerivedData" \
    CODE_SIGNING_ALLOWED=NO >"$OUT/build.log" 2>&1 || BUILD_STATUS=$?
grep -E "error:|BUILD (SUCCEEDED|FAILED)" "$OUT/build.log" || true
[ "$BUILD_STATUS" -eq 0 ] || { tail -150 "$OUT/build.log"; exit "$BUILD_STATUS"; }

export_shots() {
    local MEDIA="$1"
    mkdir -p "$MEDIA/attachments"
    if xcrun xcresulttool export attachments --path "$MEDIA/UITests.xcresult" --output-path "$MEDIA/attachments"; then
    python3 - "$MEDIA/attachments" "$MEDIA/screenshots" <<'PY'
import json, os, re, shutil, sys
src, dst = sys.argv[1], sys.argv[2]
manifest = json.load(open(os.path.join(src, "manifest.json")))
def attachments(node):
    if isinstance(node, dict):
        if "exportedFileName" in node:
            yield node
        for value in node.values():
            yield from attachments(value)
    elif isinstance(node, list):
        for value in node:
            yield from attachments(value)
# Artifact file names must not contain " : < > | * ? (XCTest names its failure attachments with quotes).
def safe(text): return re.sub(r"[^A-Za-z0-9._-]+", "_", text).strip("_") or "attachment"
count = 0
for item in attachments(manifest):
    name = item.get("suggestedHumanReadableName") or item["exportedFileName"]
    exported = item["exportedFileName"]
    ext = os.path.splitext(exported)[1] or ".bin"
    # Our names contain dots ("Class.test.01-checkpoint"): strip only the real extension.
    base = name[: -len(ext)] if name.lower().endswith(ext.lower()) else name
    stem = re.sub(r"_\d+_[0-9A-F-]{36}", "", base)  # xcresulttool vkládá _0_UUID doprostřed názvu
    # "<Class>.<test>.<NN>-<checkpoint>" from StatikaUITestCase.screenshot; XCTest's own attachments
    # (failure descriptions, UI hierarchies) go to xctest/.
    ours = re.match(r"^[A-Za-z]+Tests\.", stem) and ext == ".png"
    folder = os.path.join(dst, safe(stem.split(".")[0]) if ours else "xctest")
    os.makedirs(folder, exist_ok=True)
    target = os.path.join(folder, safe(stem) + ext)
    n = 1
    while os.path.exists(target):
        n += 1
        target = os.path.join(folder, f"{safe(stem)}_{n}{ext}")
    shutil.copy(os.path.join(src, exported), target)
    count += ours and 1 or 0
print(f"Exported {count} UI test screenshots to {dst}")
PY
        rm -rf "$MEDIA/attachments"
    else
        echo "::warning::Could not export the UI test screenshots"
    fi
}

# 3. UI testy na iPhonu a na iPadu (reklamní test potřebuje síť a Google – v CI se přeskakuje)
STATUS=0
for DEVICE in "iphone:$IPHONE" "ipad:$IPAD"; do
    NAME="${DEVICE%%:*}"; UDID="${DEVICE#*:}"
    MEDIA="$OUT/$NAME"
    mkdir -p "$MEDIA"
    xcrun simctl boot "$UDID" 2>/dev/null || true
    xcrun simctl bootstatus "$UDID" -b >/dev/null
    xcodebuild test-without-building \
        -project iosApp/Statika.xcodeproj \
        -scheme Statika \
        -destination "id=$UDID" \
        -derivedDataPath "$OUT/DerivedData" \
        -resultBundlePath "$MEDIA/UITests.xcresult" \
        -parallel-testing-enabled NO \
        ${SKIP_ADS:+-skip-testing:StatikaUITests/AdsTests} \
        CODE_SIGNING_ALLOWED=NO || STATUS=$?
    export_shots "$MEDIA"
done
exit "$STATUS"
