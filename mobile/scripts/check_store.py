#!/usr/bin/env python3
"""Kontrola podkladů pro obchody (spouštět z mobile/ nebo odkudkoli):

    python3 scripts/check_store.py

- App Store: limity textů stránky (iosApp/store/metadata), URL, nákup v aplikaci (iap/*.json: limity Applu, shoda
  ID se StoreKit konfigurací a kódem), soulad štítku App Privacy (fastlane/app_privacy_details.json)
  s PrivacyInfo.xcprivacy, žádná výzva ATT, verze v project.yml = VERSION, rozměry snímků;
- Google Play: limity textů (docs/google-play/metadata/android), „Co je nového“;
- workflowy (../.github/workflows): platný YAML, žádné spouštění při každém pushi u vydávacích workflowů
  a existence souborů, na které odkazují (cesty v mobile/).
Vrací 1 při chybě; varování (W) build nezastaví.
"""
from __future__ import annotations

import json
import plistlib
import re
import shutil
import struct
import subprocess
import sys
from pathlib import Path

MOBILE = Path(__file__).resolve().parent.parent
ROOT = MOBILE.parent
errors: list[str] = []
warnings: list[str] = []


def err(msg: str):
    errors.append(msg)


def warn(msg: str):
    warnings.append(msg)


def text(p: Path) -> str:
    return p.read_text(encoding="utf-8").strip()


# ── App Store: stránka ────────────────────────────────────────────────────────────────────────────────────────
STORE = MOBILE / "iosApp/store"
APP_STORE_LIMITS = {"name.txt": 30, "subtitle.txt": 30, "keywords.txt": 100, "promotional_text.txt": 170,
                    "description.txt": 4000}
APP_STORE_LOCALES = ["cs", "ru"]


def check_app_store_metadata():
    for loc in APP_STORE_LOCALES:
        d = STORE / "metadata" / loc
        for name, limit in APP_STORE_LIMITS.items():
            f = d / name
            if not f.exists():
                err(f"App Store {loc}: {name} missing")
                continue
            t = text(f)
            if not t:
                err(f"App Store {loc}: {name} empty")
            if len(t) > limit:
                err(f"App Store {loc}: {name} has {len(t)} > {limit} characters")
        for u in ["support_url.txt", "marketing_url.txt", "privacy_url.txt"]:
            f = d / u
            if not f.exists() or not text(f).startswith("https://"):
                err(f"App Store {loc}: {u} missing or not https")
        if (d / "keywords.txt").exists():
            kw = [k.strip().lower() for k in text(d / "keywords.txt").split(",")]
            if any(not k for k in kw):
                err(f"App Store {loc}: empty keyword (double comma or trailing comma)")
            if any(" ," in k or k != k.strip() for k in kw):
                err(f"App Store {loc}: keywords with spaces around commas waste characters")
            title_words = set(re.findall(r"\w+", (text(d / "name.txt") + " " + text(d / "subtitle.txt")).lower()))
            dup = [k for k in kw if k in title_words]
            if dup:
                warn(f"App Store {loc}: keywords repeat words of name/subtitle (already indexed): {dup}")
        desc = text(d / "description.txt") if (d / "description.txt").exists() else ""
        if "<" in desc:
            err(f"App Store {loc}: description contains '<' (HTML is not allowed)")
    for f in ["copyright.txt", "primary_category.txt"]:
        if not (STORE / "metadata" / f).exists():
            err(f"App Store: metadata/{f} missing")


def png_size(p: Path) -> tuple[int, int]:
    with p.open("rb") as fh:
        head = fh.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("not a PNG")
    return struct.unpack(">II", head[16:24])


IPHONE_SIZES = {(1320, 2868), (2868, 1320), (1290, 2796), (2796, 1290)}
IPAD_SIZES = {(2064, 2752), (2752, 2064), (2048, 2732), (2732, 2048)}


def check_screenshots():
    shots = STORE / "screenshots"
    if not shots.exists() or not any(shots.rglob("*.png")):
        warn("App Store: no screenshots yet (run the iOS store screenshots workflow, copy into iosApp/store/screenshots)")
        return
    for loc in APP_STORE_LOCALES:
        files = sorted((shots / loc).glob("*.png")) if (shots / loc).exists() else []
        if not files:
            err(f"App Store {loc}: no screenshots in screenshots/{loc}")
        for kind, sizes in (("iphone", IPHONE_SIZES), ("ipad", IPAD_SIZES)):
            mine = [f for f in files if f.name.startswith(kind + "-")]
            if files and not mine:
                err(f"App Store {loc}: no {kind} screenshots (the app supports iPhone and iPad)")
            if len(mine) > 10:
                err(f"App Store {loc}: {len(mine)} {kind} screenshots > 10")
            for f in mine:
                if png_size(f) not in sizes:
                    err(f"App Store {loc}: {f.name} is {png_size(f)}, not an accepted {kind} size")


# ── App Store: nákup ──────────────────────────────────────────────────────────────────────────────────────────
def check_iap():
    specs = sorted((STORE / "iap").glob("*.json"))
    if not specs:
        err("App Store: no iap/*.json")
    storekit = json.loads((MOBILE / "iosApp/Statika/Statika.storekit").read_text())
    storekit_ids = {p["productID"] for p in storekit.get("products", [])}
    kotlin = "\n".join(p.read_text() for p in (MOBILE / "shared/src").rglob("*.kt"))
    for spec_path in specs:
        spec = json.loads(spec_path.read_text())
        pid = spec["productId"]
        if pid not in storekit_ids:
            err(f"IAP {pid}: not in Statika.storekit ({storekit_ids})")
        if f'"{pid}"' not in kotlin:
            err(f"IAP {pid}: the shared Kotlin code does not use this product id")
        if not re.fullmatch(r"\d+\.\d\d", spec["price"]):
            err(f"IAP {pid}: price {spec['price']!r} is not like 1.99")
        if len(spec["name"]) > 64:
            err(f"IAP {pid}: reference name over 64 characters")
        for loc in APP_STORE_LOCALES:
            l10n = spec["localizations"].get(loc)
            if not l10n:
                err(f"IAP {pid}: no localization {loc}")
                continue
            if len(l10n["name"]) > 30:
                err(f"IAP {pid} {loc}: display name {len(l10n['name'])} > 30")
            if len(l10n["description"]) > 45:
                err(f"IAP {pid} {loc}: description {len(l10n['description'])} > 45 characters")
        shot = spec_path.parent / spec.get("screenshot", "")
        if not shot.is_file():
            warn(f"IAP {pid}: review screenshot {shot.name} not there yet (iOS store screenshots workflow makes it)")
        if pid not in text(STORE / "review_notes.txt"):
            err(f"IAP {pid}: review_notes.txt does not mention it")


# ── App Store: soukromí ───────────────────────────────────────────────────────────────────────────────────────
MANIFEST_TO_LABEL = {"DeviceID": "DEVICE_ID", "CoarseLocation": "COARSE_LOCATION",
                     "AdvertisingData": "ADVERTISING_DATA", "ProductInteraction": "PRODUCT_INTERACTION",
                     "PerformanceData": "PERFORMANCE_DATA", "CrashData": "CRASH_DATA",
                     "OtherDiagnosticData": "OTHER_DIAGNOSTIC_DATA", "PurchaseHistory": "PURCHASE_HISTORY"}
PURPOSE_TO_LABEL = {"ThirdPartyAdvertising": "THIRD_PARTY_ADVERTISING", "DeveloperAdvertising": "DEVELOPER_ADVERTISING",
                    "Analytics": "ANALYTICS", "AppFunctionality": "APP_FUNCTIONALITY",
                    "ProductPersonalization": "PRODUCT_PERSONALIZATION", "Other": "OTHER_PURPOSES"}


def check_privacy():
    manifest_path = MOBILE / "iosApp/Statika/PrivacyInfo.xcprivacy"
    manifest = plistlib.loads(manifest_path.read_bytes())
    label = {r["category"]: r for r in json.loads((MOBILE / "fastlane/app_privacy_details.json").read_text())}
    seen = set()
    for row in manifest.get("NSPrivacyCollectedDataTypes", []):
        kind = row["NSPrivacyCollectedDataType"].removeprefix("NSPrivacyCollectedDataType")
        cat = MANIFEST_TO_LABEL.get(kind)
        if not cat:
            err(f"privacy: manifest type {kind} has no mapping in check_store.py")
            continue
        seen.add(cat)
        lab = label.get(cat)
        if not lab:
            err(f"privacy: {cat} in PrivacyInfo.xcprivacy but not in app_privacy_details.json")
            continue
        linked = "DATA_LINKED_TO_YOU" in lab["data_protections"]
        tracked = "DATA_USED_TO_TRACK_YOU" in lab["data_protections"]
        if linked != row["NSPrivacyCollectedDataTypeLinked"]:
            err(f"privacy: {cat} linked differs (manifest {row['NSPrivacyCollectedDataTypeLinked']}, label {linked})")
        if tracked != row["NSPrivacyCollectedDataTypeTracking"]:
            err(f"privacy: {cat} tracking differs (manifest {row['NSPrivacyCollectedDataTypeTracking']}, label {tracked})")
        purposes = {PURPOSE_TO_LABEL[p.removeprefix("NSPrivacyCollectedDataTypePurpose")] for p in row["NSPrivacyCollectedDataTypePurposes"]}
        if purposes != set(lab["purposes"]):
            err(f"privacy: {cat} purposes differ (manifest {sorted(purposes)}, label {sorted(lab['purposes'])})")
        if tracked:
            err(f"privacy: {cat} declared as tracking, but the app has no App Tracking Transparency prompt "
                "(Apple rejects that, guideline 5.1.2(i) – Ear4Music 2026-10-30)")
    for cat in set(label) - seen:
        err(f"privacy: {cat} in app_privacy_details.json but not in PrivacyInfo.xcprivacy")
    project = (MOBILE / "iosApp/project.yml").read_text()
    if "NSUserTrackingUsageDescription" in project:
        err("privacy: project.yml has NSUserTrackingUsageDescription but App Privacy says no tracking")
    for strings in (MOBILE / "iosApp/Statika").glob("*.lproj/InfoPlist.strings"):
        if "NSUserTrackingUsageDescription" in strings.read_text(encoding="utf-8", errors="replace"):
            err(f"privacy: {strings.relative_to(MOBILE)} has NSUserTrackingUsageDescription (no ATT prompt)")
    policy = (ROOT / "public/privacy.html").read_text()
    for needle in ["iPhone", "App Store", "AdMob", "Firebase", "App Tracking Transparency"]:
        if needle not in policy:
            err(f"privacy policy public/privacy.html does not mention {needle}")


def check_version():
    version = text(MOBILE / "VERSION")
    m = re.search(r'MARKETING_VERSION:\s*"?([\d.]+)"?', (MOBILE / "iosApp/project.yml").read_text())
    if not m:
        err("project.yml has no MARKETING_VERSION")
    elif m.group(1) != version:
        warn(f"project.yml MARKETING_VERSION {m.group(1)} != VERSION {version} "
             "(testflight.yml overrides it from VERSION; local Xcode builds show the old one)")


# ── Google Play ───────────────────────────────────────────────────────────────────────────────────────────────
PLAY = MOBILE / "docs/google-play/metadata/android"
PLAY_LIMITS = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000}


def check_play():
    for loc in ["cs-CZ", "ru-RU"]:
        for name, limit in PLAY_LIMITS.items():
            f = PLAY / loc / name
            if not f.exists():
                err(f"Play {loc}: {name} missing")
                continue
            t = text(f)
            if not t or len(t) > limit:
                err(f"Play {loc}: {name} has {len(t)} characters (1..{limit})")
        wn = MOBILE / f"distribution/whatsnew/whatsnew-{loc}"
        if not wn.exists() or len(text(wn)) > 500:
            err(f"Play {loc}: distribution/whatsnew/whatsnew-{loc} missing or over 500 characters")


# ── workflowy ─────────────────────────────────────────────────────────────────────────────────────────────────
RELEASE_WORKFLOWS = ["testflight.yml", "asc-setup.yml", "ios-screenshots.yml", "deploy.yml"]
# vznikají až během běhu (tajemství, sestavení), nemusí existovat v repozitáři
GENERATED = ("iosApp/Statika/GoogleService-Info.plist", "app/google-services.json", "app/build/", "build/",
             "iosApp/Statika.xcodeproj", "distribution/whatsnew/whatsnew-")


def load_yaml(path: Path):
    try:
        import yaml  # type: ignore
        return yaml.safe_load(path.read_text())
    except ModuleNotFoundError:
        pass
    ruby = shutil.which("ruby")
    if not ruby:
        warn(f"{path.name}: neither PyYAML nor ruby available, YAML not parsed")
        return None
    out = subprocess.run([ruby, "-ryaml", "-rjson", "-e", "puts JSON.generate(YAML.safe_load(File.read(ARGV[0]), aliases: true))", str(path)],
                         capture_output=True, text=True)
    if out.returncode != 0:
        err(f"{path.name}: invalid YAML: {out.stderr.strip()[:300]}")
        return None
    return json.loads(out.stdout)


def check_workflows():
    wf_dir = ROOT / ".github/workflows"
    for wf in sorted(wf_dir.glob("*.yml")):
        data = load_yaml(wf)
        if data is None:
            continue
        triggers = next((data[k] for k in ("on", True, "true") if k in data), None)  # YAML 1.1: `on` = true
        if wf.name in RELEASE_WORKFLOWS:
            if not isinstance(triggers, dict):
                err(f"{wf.name}: unexpected trigger format")
            else:
                push = triggers.get("push")
                if push is not None and (not isinstance(push, dict) or set(push) != {"tags"}):
                    err(f"{wf.name}: release workflow must not run on ordinary pushes (only tags / workflow_dispatch)")
                if "pull_request" in triggers or "schedule" in triggers:
                    err(f"{wf.name}: release workflow must not run on pull requests or a schedule")
        if wf.name not in RELEASE_WORKFLOWS:
            continue
        body = wf.read_text()
        for m in re.finditer(r"(?<![\w$/.-])((?:mobile/)?(?:iosApp|app|docs|distribution|fastlane|scripts)/[\w./{}*-]+)", body):
            ref = m.group(1).rstrip(".,")
            rel = ref.removeprefix("mobile/")
            if any(rel.startswith(g) for g in GENERATED) or "*" in rel or "{" in rel or "/build/" in rel:
                continue
            if not (MOBILE / rel).exists():
                err(f"{wf.name}: references {ref}, which does not exist in mobile/")


def main() -> int:
    for check in (check_app_store_metadata, check_screenshots, check_iap, check_privacy, check_version, check_play, check_workflows):
        try:
            check()
        except Exception as e:  # noqa: BLE001 – kontrola nesmí spadnout potichu
            err(f"{check.__name__} crashed: {e!r}")
    for w in warnings:
        print("W", w)
    for e in errors:
        print("E", e)
    print(f"check_store: {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
