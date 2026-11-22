#!/usr/bin/env python3
"""The App Store page of Statika through the App Store Connect API (what `fastlane deliver` would do, without
fastlane): texts, URLs, categories, copyright, screenshots, review notes and contact. Nothing is submitted.

    python3 asc_listing.py metadata          # app info + version localizations, categories, copyright
    python3 asc_listing.py screenshots       # replaces the iPhone 6.9" and iPad 13" sets of every locale
    python3 asc_listing.py review            # review notes (review_notes.txt); the contact is copied from
                                             # another app of the account if this version has none
    python3 asc_listing.py availability      # all territories + new ones, if no availability exists yet
    python3 asc_listing.py verify            # reads everything back

Taken from ~/dev/MySongbook/iosApp/store/tools/asc_listing.py (fastlane is not needed); age rating, price and
in-app purchases are in asc_setup.py. Run from mobile/ through iosApp/store/asc.sh (key from Infisical), e.g.
    iosApp/store/asc.sh python3 iosApp/store/tools/asc_listing.py metadata
Environment: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH. Needs PyJWT + cryptography, Pillow for screenshots.
Layout: metadata/<locale>/{name,subtitle,privacy_url,description,keywords,promotional_text,support_url,marketing_url}.txt,
metadata/{copyright,primary_category}.txt, screenshots/<locale>/{iphone,ipad}-NN-*.png (capture_screenshots.sh).
"""
from __future__ import annotations

import hashlib
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from asc_setup import call, pages, age_rating_declaration  # noqa: E402

BUNDLE_ID = "cz.usharik.statika"
STORE = Path(__file__).resolve().parent.parent
META = STORE / "metadata"
SHOTS = STORE / "screenshots"
LOCALES = ["cs", "ru"]
DISPLAY = {"iphone": ("APP_IPHONE_67", (1320, 2868)), "ipad": ("APP_IPAD_PRO_3GEN_129", (2064, 2752))}
# Another app of the same account whose App Review contact is reused (kept out of git).
CONTACT_SOURCE_BUNDLE = "ru.usharik.ear4music"


def text(locale: str | None, name: str) -> str | None:
    path = (META / locale / f"{name}.txt") if locale else (META / f"{name}.txt")
    return path.read_text().strip() if path.exists() else None


def app_id() -> str:
    return call("GET", f"/v1/apps?filter[bundleId]={BUNDLE_ID}")["data"][0]["id"]


def version(app: str) -> dict:
    found = call("GET", f"/v1/apps/{app}/appStoreVersions?filter[platform]=IOS&filter[appStoreState]=PREPARE_FOR_SUBMISSION,DEVELOPER_REJECTED,REJECTED,METADATA_REJECTED&limit=1")["data"]
    if not found:
        raise SystemExit("no editable iOS version")
    return found[0]


def app_info(app: str) -> dict:
    infos = call("GET", f"/v1/apps/{app}/appInfos")["data"]
    editable = [i for i in infos if i["attributes"].get("state") not in ("READY_FOR_DISTRIBUTION", "REPLACED_WITH_NEW_INFO")]
    return (editable or infos)[0]


def upsert(kind: str, existing: dict | None, attrs: dict, rel: dict):
    if existing:
        changed = {k: v for k, v in attrs.items() if existing["attributes"].get(k) != v and k != "locale"}
        if changed:
            call("PATCH", f"/v1/{kind}/{existing['id']}", {"data": {"type": kind, "id": existing["id"], "attributes": changed}})
        return existing["id"], sorted(changed)
    created = call("POST", f"/v1/{kind}", {"data": {"type": kind, "attributes": attrs, "relationships": rel}})
    return created["data"]["id"], ["created"]


def metadata(app: str):
    info = app_info(app)
    ver = version(app)
    have_info = {l["attributes"]["locale"]: l for l in pages(f"/v1/appInfos/{info['id']}/appInfoLocalizations?limit=50")}
    have_ver = {l["attributes"]["locale"]: l for l in pages(f"/v1/appStoreVersions/{ver['id']}/appStoreVersionLocalizations?limit=50")}
    for loc in LOCALES:
        attrs = {"locale": loc, "name": text(loc, "name"), "subtitle": text(loc, "subtitle"), "privacyPolicyUrl": text(loc, "privacy_url")}
        try:
            _, changed = upsert("appInfoLocalizations", have_info.get(loc), attrs,
                                {"appInfo": {"data": {"type": "appInfos", "id": info["id"]}}})
        except SystemExit as e:  # a localized name that is taken: keep the English one
            if "name" in str(e).lower() and loc != "cs":
                print(f"  {loc}: name rejected ({str(e)[:160]}); retrying with the Czech name")
                attrs["name"] = text("cs", "name")
                _, changed = upsert("appInfoLocalizations", have_info.get(loc), attrs,
                                    {"appInfo": {"data": {"type": "appInfos", "id": info["id"]}}})
            else:
                raise
        print(f"app info {loc}: {changed or 'kept'}")
        if loc not in have_ver:  # a new app info language creates the version language as well
            have_ver = {l["attributes"]["locale"]: l for l in pages(f"/v1/appStoreVersions/{ver['id']}/appStoreVersionLocalizations?limit=50")}
        attrs = {"locale": loc, "description": text(loc, "description"), "keywords": text(loc, "keywords"),
                 "promotionalText": text(loc, "promotional_text"), "supportUrl": text(loc, "support_url"),
                 "marketingUrl": text(loc, "marketing_url")}
        _, changed = upsert("appStoreVersionLocalizations", have_ver.get(loc), attrs,
                            {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": ver["id"]}}})
        print(f"version {loc}: {changed or 'kept'}")
    rel = {"primaryCategory": {"data": {"type": "appCategories", "id": text(None, "primary_category")}}}
    if text(None, "secondary_category"):
        rel["secondaryCategory"] = {"data": {"type": "appCategories", "id": text(None, "secondary_category")}}
    call("PATCH", f"/v1/appInfos/{info['id']}", {"data": {"type": "appInfos", "id": info["id"], "relationships": rel}})
    print("categories:", text(None, "primary_category"), "/", text(None, "secondary_category"))
    call("PATCH", f"/v1/appStoreVersions/{ver['id']}", {"data": {"type": "appStoreVersions", "id": ver["id"],
                                                                 "attributes": {"copyright": text(None, "copyright")}}})
    print("copyright:", text(None, "copyright"))


def upload_screenshot(set_id: str, path: Path):
    data = path.read_bytes()
    reserved = call("POST", "/v1/appScreenshots", {"data": {"type": "appScreenshots",
                    "attributes": {"fileName": path.name, "fileSize": len(data)},
                    "relationships": {"appScreenshotSet": {"data": {"type": "appScreenshotSets", "id": set_id}}}}})["data"]
    for op in reserved["attributes"]["uploadOperations"]:
        chunk = data[op["offset"]:op["offset"] + op["length"]]
        req = urllib.request.Request(op["url"], data=chunk, method=op["method"],
                                     headers={h["name"]: h["value"] for h in op["requestHeaders"]})
        urllib.request.urlopen(req).read()
    call("PATCH", f"/v1/appScreenshots/{reserved['id']}", {"data": {"type": "appScreenshots", "id": reserved["id"],
         "attributes": {"uploaded": True, "sourceFileChecksum": hashlib.md5(data).hexdigest()}}})


def screenshots(app: str, only: list[str]):
    from PIL import Image
    ver = version(app)
    locs = {l["attributes"]["locale"]: l["id"] for l in pages(f"/v1/appStoreVersions/{ver['id']}/appStoreVersionLocalizations?limit=50")}
    for loc in (only or LOCALES):
        for device, (display, size) in DISPLAY.items():
            files = sorted((SHOTS / loc).glob(f"{device}-*.png"))
            if not files:
                print(f"{loc} {device}: no files, skipped")
                continue
            for f in files:
                if Image.open(f).size != size:
                    raise SystemExit(f"{f}: {Image.open(f).size}, expected {size}")
            sets = call("GET", f"/v1/appStoreVersionLocalizations/{locs[loc]}/appScreenshotSets?filter[screenshotDisplayType]={display}")["data"]
            if sets:
                set_id = sets[0]["id"]
                for old in pages(f"/v1/appScreenshotSets/{set_id}/appScreenshots?limit=50"):
                    call("DELETE", f"/v1/appScreenshots/{old['id']}")
            else:
                set_id = call("POST", "/v1/appScreenshotSets", {"data": {"type": "appScreenshotSets",
                              "attributes": {"screenshotDisplayType": display},
                              "relationships": {"appStoreVersionLocalization": {"data": {"type": "appStoreVersionLocalizations", "id": locs[loc]}}}}})["data"]["id"]
            for f in files:
                upload_screenshot(set_id, f)
            print(f"{loc} {device} ({display}): {len(files)} uploaded")


def review(app: str):
    ver = version(app)
    notes = (STORE / "review_notes.txt").read_text().strip()
    detail = (call("GET", f"/v1/appStoreVersions/{ver['id']}/appStoreReviewDetail", ok_errors=(404,)) or {}).get("data")
    attrs = {"notes": notes, "demoAccountRequired": False}
    if not (detail and detail["attributes"].get("contactEmail")):
        src_app = call("GET", f"/v1/apps?filter[bundleId]={CONTACT_SOURCE_BUNDLE}")["data"][0]["id"]
        for v in call("GET", f"/v1/apps/{src_app}/appStoreVersions?limit=5")["data"]:
            src = (call("GET", f"/v1/appStoreVersions/{v['id']}/appStoreReviewDetail", ok_errors=(404,)) or {}).get("data")
            if src and src["attributes"].get("contactEmail"):
                for k in ("contactFirstName", "contactLastName", "contactPhone", "contactEmail"):
                    attrs[k] = src["attributes"][k]
                print(f"contact copied from {CONTACT_SOURCE_BUNDLE} {v['attributes']['versionString']} (not printed)")
                break
    if detail:
        call("PATCH", f"/v1/appStoreReviewDetails/{detail['id']}", {"data": {"type": "appStoreReviewDetails", "id": detail["id"], "attributes": attrs}})
    else:
        call("POST", "/v1/appStoreReviewDetails", {"data": {"type": "appStoreReviewDetails", "attributes": attrs,
             "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": ver["id"]}}}}})
    print(f"review notes set ({len(notes)} characters)")


EXCLUDED_TERRITORIES = {"CHN"}


def availability(app: str):
    found = call("GET", f"/v1/apps/{app}/appAvailabilityV2", ok_errors=(404,))
    if found and found.get("data"):
        print("availability exists:", found["data"]["attributes"])
        return
    # China mainland needs an ICP filing and the App Compliance forms (AI content, Security Assessment); AdMob does not
    # serve there either, so the app is not offered there.
    territories = [t["id"] for t in pages("/v1/territories?limit=200")]
    body = {"data": {"type": "appAvailabilities", "attributes": {"availableInNewTerritories": True},
                     "relationships": {"app": {"data": {"type": "apps", "id": app}},
                                       "territoryAvailabilities": {"data": [{"type": "territoryAvailabilities", "id": f"${{{t}}}"} for t in territories]}}},
            "included": [{"type": "territoryAvailabilities", "id": f"${{{t}}}", "attributes": {"available": t not in EXCLUDED_TERRITORIES},
                          "relationships": {"territory": {"data": {"type": "territories", "id": t}}}} for t in territories]}
    call("POST", "/v2/appAvailabilities", body)
    print(f"availability: {len(territories) - len(EXCLUDED_TERRITORIES)} territories and new ones, not {sorted(EXCLUDED_TERRITORIES)}")


def verify(app: str):
    info = app_info(app)
    ver = version(app)
    print("version", ver["attributes"]["versionString"], ver["attributes"].get("appStoreState"), "copyright:",
          call("GET", f"/v1/appStoreVersions/{ver['id']}")["data"]["attributes"].get("copyright"))
    cats = call("GET", f"/v1/appInfos/{info['id']}?include=primaryCategory,secondaryCategory")["data"]["relationships"]
    print("categories:", (cats["primaryCategory"].get("data") or {}).get("id"), "/", (cats["secondaryCategory"].get("data") or {}).get("id"))
    for l in pages(f"/v1/appInfos/{info['id']}/appInfoLocalizations?limit=50"):
        a = l["attributes"]
        print(f"  info {a['locale']}: {a.get('name')!r} | {a.get('subtitle')!r} | {a.get('privacyPolicyUrl')}")
    for l in pages(f"/v1/appStoreVersions/{ver['id']}/appStoreVersionLocalizations?limit=50"):
        a = l["attributes"]
        sets = call("GET", f"/v1/appStoreVersionLocalizations/{l['id']}/appScreenshotSets?include=appScreenshots&limit[appScreenshots]=20")
        counts = {s["attributes"]["screenshotDisplayType"]: len(s["relationships"]["appScreenshots"]["data"]) for s in sets["data"]}
        states = {}
        for inc in sets.get("included", []):
            st = (inc["attributes"].get("assetDeliveryState") or {}).get("state")
            states[st] = states.get(st, 0) + 1
        print(f"  version {a['locale']}: description {len(a.get('description') or '')} ch, keywords {len(a.get('keywords') or '')} ch,"
              f" promo {len(a.get('promotionalText') or '')} ch, support {a.get('supportUrl')}, marketing {a.get('marketingUrl')}; screenshots {counts} {states}")
    detail = (call("GET", f"/v1/appStoreVersions/{ver['id']}/appStoreReviewDetail", ok_errors=(404,)) or {}).get("data")
    if detail:
        a = detail["attributes"]
        print("review: contact", "set" if a.get("contactEmail") else "MISSING", "| notes", len(a.get("notes") or ""), "ch | demo account", a.get("demoAccountRequired"))
    else:
        print("review: none")
    decl = age_rating_declaration(app)
    print("age rating:", {k: v for k, v in decl["attributes"].items() if v not in (None, "NONE", False)})
    sched = call("GET", f"/v1/apps/{app}/appPriceSchedule", ok_errors=(404,))
    if sched and sched.get("data"):
        prices = call("GET", f"/v1/appPriceSchedules/{sched['data']['id']}/manualPrices?include=appPricePoint&limit=5")
        points = [i["attributes"].get("customerPrice") for i in prices.get("included", []) if i["type"] == "appPricePoints"]
        print("price: customer price", points)
    else:
        print("price: no schedule")
    avail = call("GET", f"/v1/apps/{app}/appAvailabilityV2", ok_errors=(404,))
    print("availability:", (avail or {}).get("data", {}).get("attributes") if avail else "none")


def main():
    cmd = sys.argv[1]
    app = app_id()
    if cmd == "screenshots":
        screenshots(app, sys.argv[2:])
    elif cmd in ("metadata", "review", "availability", "verify"):
        {"metadata": metadata, "review": review, "availability": availability, "verify": verify}[cmd](app)
    else:
        raise SystemExit(f"unknown command {cmd}")


if __name__ == "__main__":
    main()
