#!/usr/bin/env python3
"""App Store Connect setup of a release through the API: the things App Store Connect otherwise wants done by hand.

    python3 asc_setup.py --bundle-id <id> status
    python3 asc_setup.py --bundle-id <id> price-free                 # the app's price schedule: free everywhere
    python3 asc_setup.py --bundle-id <id> age-rating                 # the version's age rating: the app shows ads
    python3 asc_setup.py --bundle-id <id> iap --spec iap.json        # a non-consumable with localizations, price,
                                                                     # availability, review note and screenshot

Environment: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH (the .p8 file of an App Store Connect API key with the App Manager
role). Needs PyJWT with cryptography. Every step is idempotent: what exists is kept or completed, nothing is submitted.

iap.json: {"productId": "...", "name": "...", "price": "3.99", "reviewNote": "...", "screenshot": "path.png",
           "localizations": {"en-US": {"name": "...", "description": "..."}, ...}}
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import jwt

API = "https://api.appstoreconnect.apple.com"


def token() -> str:
    now = int(time.time())
    return jwt.encode({"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 900, "aud": "appstoreconnect-v1"},
                      Path(os.environ["ASC_KEY_PATH"]).read_text(), algorithm="ES256",
                      headers={"kid": os.environ["ASC_KEY_ID"], "typ": "JWT"})


def call(method: str, path: str, body=None, ok_errors=()):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Authorization": "Bearer " + token()}
    if body is not None:
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(path if path.startswith("http") else API + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as r:
            text = r.read().decode()
            return json.loads(text) if text else None
    except urllib.error.HTTPError as e:
        detail = e.read().decode()[:1500]
        if e.code in ok_errors:
            print(f"  ({method} {path}: {e.code} {detail[:200]})")
            return None
        raise SystemExit(f"{method} {path}: {e.code} {detail}")


def pages(path: str):
    while path:
        found = call("GET", path)
        yield from found["data"]
        path = (found.get("links") or {}).get("next") or None


def app_id(bundle_id: str) -> str:
    apps = call("GET", f"/v1/apps?filter[bundleId]={bundle_id}")["data"]
    if not apps:
        raise SystemExit(f"no app with bundle id {bundle_id}")
    return apps[0]["id"]


def latest_version(app: str) -> dict | None:
    versions = call("GET", f"/v1/apps/{app}/appStoreVersions?filter[platform]=IOS&limit=5")["data"]
    return versions[0] if versions else None


# --- status ------------------------------------------------------------------------------------------------

def status(app: str):
    info = call("GET", f"/v1/apps/{app}")["data"]["attributes"]
    print(f"app {app}: {info.get('name')} ({info.get('bundleId')}), primary locale {info.get('primaryLocale')}")
    for v in call("GET", f"/v1/apps/{app}/appStoreVersions?filter[platform]=IOS&limit=5")["data"]:
        a = v["attributes"]
        print(f"version {a.get('versionString')}: {a.get('appStoreState') or a.get('appVersionState')} (id {v['id']})")
    version = latest_version(app)
    if version:
        detail = call("GET", f"/v1/appStoreVersions/{version['id']}/appStoreReviewDetail", ok_errors=(404,))
        a = ((detail or {}).get("data") or {}).get("attributes") or {}
        print("review contact:", a.get("contactFirstName"), a.get("contactLastName"), "| demo account:", a.get("demoAccountRequired"))
        print("review notes:", (a.get("notes") or "(none)").replace("\n", " / "))
    decl = age_rating_declaration(app)
    print("age rating declaration:", json.dumps(decl["attributes"], ensure_ascii=False) if decl else "none reachable")
    for v in call("GET", f"/v1/apps/{app}/appStoreVersions?filter[platform]=IOS&limit=3")["data"]:
        build = call("GET", f"/v1/appStoreVersions/{v['id']}/build", ok_errors=(404,))
        b = (build or {}).get("data")
        print(f"version {v['attributes'].get('versionString')} build: " + (f"{b['attributes'].get('version')} ({b['attributes'].get('processingState')}, id {b['id']})" if b else "none"))
    for b in call("GET", f"/v1/builds?filter[app]={app}&sort=-uploadedDate&limit=5")["data"]:
        a = b["attributes"]
        print(f"build {a.get('version')}: {a.get('processingState')}, uploaded {a.get('uploadedDate')}, expired {a.get('expired')} (id {b['id']})")
    for sub in call("GET", f"/v1/reviewSubmissions?filter[app]={app}&limit=10", ok_errors=(400, 404))["data"] if call("GET", f"/v1/reviewSubmissions?filter[app]={app}&limit=10", ok_errors=(400, 404)) else []:
        a = sub["attributes"]
        items = call("GET", f"/v1/reviewSubmissions/{sub['id']}/items?limit=10", ok_errors=(404,))
        kinds = [i["type"] + ":" + str((i.get("relationships") or {}).get("appStoreVersion", {}).get("data", {}).get("id", "") or "") for i in (items or {}).get("data", [])]
        print(f"review submission {sub['id']}: {a.get('state')} ({a.get('platform')}, submitted {a.get('submittedDate')}) items {kinds}")
    sched = call("GET", f"/v1/apps/{app}/appPriceSchedule?include=baseTerritory", ok_errors=(404,))
    if sched and sched.get("data"):
        base = (sched["data"].get("relationships", {}).get("baseTerritory", {}).get("data") or {}).get("id")
        prices = call("GET", f"/v1/appPriceSchedules/{sched['data']['id']}/manualPrices?include=appPricePoint&limit=50")
        points = {i["id"]: i["attributes"] for i in prices.get("included", []) if i["type"] == "appPricePoints"}
        for p in prices["data"]:
            point = p["relationships"]["appPricePoint"]["data"]["id"]
            print(f"price schedule (base {base}): point {point} customer price {points.get(point, {}).get('customerPrice')} from {p['attributes'].get('startDate')}")
    else:
        print("price schedule: none")
    for iap in pages(f"/v1/apps/{app}/inAppPurchasesV2?limit=50"):
        a = iap["attributes"]
        print(f"iap {a['productId']} ({a['inAppPurchaseType']}): {a['state']} (id {iap['id']})")
        for loc in call("GET", f"/v2/inAppPurchases/{iap['id']}/inAppPurchaseLocalizations?limit=20")["data"]:
            la = loc["attributes"]
            print(f"  {la['locale']}: {la['name']} / {la.get('description')} [{la.get('state')}]")
        sched = call("GET", f"/v2/inAppPurchases/{iap['id']}/iapPriceSchedule", ok_errors=(404,))
        print("  price schedule:", "yes" if sched and sched.get("data") else "none")
        avail = call("GET", f"/v2/inAppPurchases/{iap['id']}/inAppPurchaseAvailability", ok_errors=(404,))
        print("  availability:", "yes" if avail and avail.get("data") else "none")
        shot = call("GET", f"/v2/inAppPurchases/{iap['id']}/appStoreReviewScreenshot", ok_errors=(404,))
        print("  review screenshot:", (shot or {}).get("data", {}).get("attributes", {}).get("assetDeliveryState", {}).get("state") if shot and shot.get("data") else "none")


# --- price -------------------------------------------------------------------------------------------------

def price_free(app: str):
    point = None
    for p in pages(f"/v1/apps/{app}/appPricePoints?filter[territory]=USA&limit=200"):
        if p["attributes"]["customerPrice"] in ("0.0", "0", "0.00"):
            point = p["id"]
            break
    if not point:
        raise SystemExit("no free price point for USA")
    body = {"data": {"type": "appPriceSchedules",
                     "relationships": {"app": {"data": {"type": "apps", "id": app}},
                                       "baseTerritory": {"data": {"type": "territories", "id": "USA"}},
                                       "manualPrices": {"data": [{"type": "appPrices", "id": "${price1}"}]}}},
            "included": [{"type": "appPrices", "id": "${price1}", "attributes": {"startDate": None},
                          "relationships": {"appPricePoint": {"data": {"type": "appPricePoints", "id": point}}}}]}
    call("POST", "/v1/appPriceSchedules", body)
    print("price schedule: free in the base territory USA, the other territories follow automatically")


# --- age rating ------------------------------------------------------------------------------------------

def age_rating_declaration(app: str) -> dict | None:
    """The age rating declaration: on the app since the 2025 questionnaire, on the version before."""
    for info in call("GET", f"/v1/apps/{app}/appInfos?limit=5")["data"]:
        found = call("GET", f"/v1/appInfos/{info['id']}/ageRatingDeclaration", ok_errors=(404,))
        if found and found.get("data"):
            return found["data"]
    version = latest_version(app)
    if version:
        found = call("GET", f"/v1/appStoreVersions/{version['id']}/ageRatingDeclaration", ok_errors=(404,))
        if found and found.get("data"):
            return found["data"]
    return None


def age_rating(app: str):
    decl = age_rating_declaration(app)
    if not decl:
        raise SystemExit("no age rating declaration reachable through the API")
    attrs = decl["attributes"]
    print("age rating before:", json.dumps(attrs, ensure_ascii=False))
    wanted = {}
    if "advertising" in attrs:
        wanted["advertising"] = True
    if not wanted:
        print("this API version has no 'advertising' attribute in the declaration; set it in App Store Connect")
        return
    call("PATCH", f"/v1/ageRatingDeclarations/{decl['id']}",
         {"data": {"type": "ageRatingDeclarations", "id": decl["id"], "attributes": wanted}})
    after = age_rating_declaration(app)["attributes"]
    print("age rating after:", json.dumps(after, ensure_ascii=False))


# --- in-app purchase ---------------------------------------------------------------------------------------

def iap(app: str, spec_path: Path):
    spec = json.loads(spec_path.read_text())
    existing = [i for i in pages(f"/v1/apps/{app}/inAppPurchasesV2?filter[productId]={spec['productId']}&limit=50")
                if i["attributes"]["productId"] == spec["productId"]]
    if existing:
        iap_id = existing[0]["id"]
        print(f"iap {spec['productId']} exists: {existing[0]['attributes']['state']} (id {iap_id})")
        if spec.get("reviewNote") and existing[0]["attributes"].get("reviewNote") != spec["reviewNote"]:
            call("PATCH", f"/v2/inAppPurchases/{iap_id}",
                 {"data": {"type": "inAppPurchases", "id": iap_id, "attributes": {"reviewNote": spec["reviewNote"]}}})
            print("  review note updated")
    else:
        created = call("POST", "/v2/inAppPurchases", {"data": {
            "type": "inAppPurchases",
            "attributes": {"name": spec["name"], "productId": spec["productId"], "inAppPurchaseType": "NON_CONSUMABLE",
                           "reviewNote": spec.get("reviewNote", "")},
            "relationships": {"app": {"data": {"type": "apps", "id": app}}}}})
        iap_id = created["data"]["id"]
        print(f"iap {spec['productId']} created (id {iap_id})")

    have = {l["attributes"]["locale"]: l for l in call("GET", f"/v2/inAppPurchases/{iap_id}/inAppPurchaseLocalizations?limit=50")["data"]}
    for locale, text in spec["localizations"].items():
        if locale in have:
            a = have[locale]["attributes"]
            if a["name"] != text["name"] or (a.get("description") or "") != text["description"]:
                call("PATCH", f"/v1/inAppPurchaseLocalizations/{have[locale]['id']}",
                     {"data": {"type": "inAppPurchaseLocalizations", "id": have[locale]["id"], "attributes": text}})
                print(f"  localization {locale} updated")
            else:
                print(f"  localization {locale} kept")
        else:
            call("POST", "/v1/inAppPurchaseLocalizations", {"data": {
                "type": "inAppPurchaseLocalizations", "attributes": {**text, "locale": locale},
                "relationships": {"inAppPurchaseV2": {"data": {"type": "inAppPurchases", "id": iap_id}}}}})
            print(f"  localization {locale} created")

    sched = call("GET", f"/v2/inAppPurchases/{iap_id}/iapPriceSchedule", ok_errors=(404,))
    if sched and sched.get("data"):
        print("  price schedule kept")
    else:
        point = None
        for p in pages(f"/v2/inAppPurchases/{iap_id}/pricePoints?filter[territory]=USA&limit=200"):
            if p["attributes"]["customerPrice"] == spec["price"]:
                point = p["id"]
                break
        if not point:
            raise SystemExit(f"no USA price point at {spec['price']}")
        call("POST", "/v1/inAppPurchasePriceSchedules", {
            "data": {"type": "inAppPurchasePriceSchedules",
                     "relationships": {"inAppPurchase": {"data": {"type": "inAppPurchases", "id": iap_id}},
                                       "baseTerritory": {"data": {"type": "territories", "id": "USA"}},
                                       "manualPrices": {"data": [{"type": "inAppPurchasePrices", "id": "${price1}"}]}}},
            "included": [{"type": "inAppPurchasePrices", "id": "${price1}", "attributes": {"startDate": None},
                          "relationships": {"inAppPurchasePricePoint": {"data": {"type": "inAppPurchasePricePoints", "id": point}}}}]})
        print(f"  price schedule: {spec['price']} USD in the base territory USA")

    avail = call("GET", f"/v2/inAppPurchases/{iap_id}/inAppPurchaseAvailability", ok_errors=(404,))
    if avail and avail.get("data"):
        print("  availability kept")
    else:
        territories = [{"type": "territories", "id": t["id"]} for t in pages("/v1/territories?limit=200")]
        call("POST", "/v1/inAppPurchaseAvailabilities", {"data": {
            "type": "inAppPurchaseAvailabilities", "attributes": {"availableInNewTerritories": True},
            "relationships": {"inAppPurchase": {"data": {"type": "inAppPurchases", "id": iap_id}},
                              "availableTerritories": {"data": territories}}}})
        print(f"  availability: all {len(territories)} territories and new ones")

    shot = call("GET", f"/v2/inAppPurchases/{iap_id}/appStoreReviewScreenshot", ok_errors=(404,))
    if shot and shot.get("data"):
        print("  review screenshot kept:", shot["data"]["attributes"].get("assetDeliveryState", {}).get("state"))
    elif spec.get("screenshot"):
        path = spec_path.parent / spec["screenshot"]
        data = path.read_bytes()
        reserved = call("POST", "/v1/inAppPurchaseAppStoreReviewScreenshots", {"data": {
            "type": "inAppPurchaseAppStoreReviewScreenshots",
            "attributes": {"fileName": path.name, "fileSize": len(data)},
            "relationships": {"inAppPurchaseV2": {"data": {"type": "inAppPurchases", "id": iap_id}}}}})["data"]
        for op in reserved["attributes"]["uploadOperations"]:
            chunk = data[op["offset"]:op["offset"] + op["length"]]
            req = urllib.request.Request(op["url"], data=chunk, method=op["method"],
                                         headers={h["name"]: h["value"] for h in op["requestHeaders"]})
            urllib.request.urlopen(req).read()
        call("PATCH", f"/v1/inAppPurchaseAppStoreReviewScreenshots/{reserved['id']}", {"data": {
            "type": "inAppPurchaseAppStoreReviewScreenshots", "id": reserved["id"],
            "attributes": {"uploaded": True, "sourceFileChecksum": hashlib.md5(data).hexdigest()}}})
        print(f"  review screenshot uploaded: {path.name} ({len(data)} bytes)")
    state = call("GET", f"/v2/inAppPurchases/{iap_id}")["data"]["attributes"]["state"]
    print(f"  state now: {state}")


# --- review submissions ------------------------------------------------------------------------------------

def cancel_review(app: str):
    """Cancels the review submission that is waiting or in review (the version comes back to the developer)."""
    subs = call("GET", f"/v1/reviewSubmissions?filter[app]={app}&limit=10")["data"]
    open_ = [s for s in subs if s["attributes"].get("state") in ("WAITING_FOR_REVIEW", "IN_REVIEW", "UNRESOLVED_ISSUES", "READY_FOR_REVIEW")]
    if not open_:
        print("no open review submission")
        return
    for sub in open_:
        call("PATCH", f"/v1/reviewSubmissions/{sub['id']}", {"data": {"type": "reviewSubmissions", "id": sub["id"], "attributes": {"canceled": True}}})
        print(f"review submission {sub['id']} ({sub['attributes'].get('state')}) cancelled")


def attach_build(app: str, number: str):
    """Selects the processed build with this build number for the latest iOS version."""
    version = latest_version(app)
    builds = [b for b in call("GET", f"/v1/builds?filter[app]={app}&filter[version]={number}&limit=5")["data"]
              if b["attributes"].get("version") == number]
    if not builds:
        raise SystemExit(f"no build {number}")
    build = builds[0]
    if build["attributes"].get("processingState") != "VALID":
        raise SystemExit(f"build {number} is {build['attributes'].get('processingState')}, not VALID yet")
    call("PATCH", f"/v1/appStoreVersions/{version['id']}/relationships/build",
         {"data": {"type": "builds", "id": build["id"]}})
    print(f"version {version['attributes'].get('versionString')}: build {number} attached")


def submit(app: str, product_ids: list[str]):
    """Submits the latest iOS version with the given in-app purchases: the draft review submission is reused or
    created, the purchases join it through inAppPurchaseSubmissions, then the whole thing goes to App Review."""
    version = latest_version(app)
    drafts = [s for s in call("GET", f"/v1/reviewSubmissions?filter[app]={app}&limit=10")["data"]
              if s["attributes"].get("state") == "READY_FOR_REVIEW"]
    if drafts:
        sub = drafts[0]
        print(f"submission {sub['id']}: existing draft")
    else:
        sub = call("POST", "/v1/reviewSubmissions", {"data": {"type": "reviewSubmissions", "attributes": {"platform": "IOS"},
                                                               "relationships": {"app": {"data": {"type": "apps", "id": app}}}}})["data"]
        print(f"submission {sub['id']}: created")
    # The items list does not say which version an item holds: a 409 "already added" is the same as success.
    added = call("POST", "/v1/reviewSubmissionItems", {"data": {"type": "reviewSubmissionItems", "relationships": {
        "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": sub["id"]}},
        "appStoreVersion": {"data": {"type": "appStoreVersions", "id": version["id"]}}}}}, ok_errors=(409,))
    print(f"  version {version['attributes'].get('versionString')} " + ("added" if added else "already in it"))
    for product in product_ids:
        iaps = [i for i in pages(f"/v1/apps/{app}/inAppPurchasesV2?filter[productId]={product}&limit=50") if i["attributes"]["productId"] == product]
        if not iaps:
            raise SystemExit(f"no in-app purchase {product}")
        iap_id, state = iaps[0]["id"], iaps[0]["attributes"]["state"]
        if state in ("WAITING_FOR_REVIEW", "IN_REVIEW", "APPROVED"):
            print(f"  {product} is {state}: nothing to add")
            continue
        # The item of a review submission is the purchase's pending version (API 4.5, inAppPurchaseSubmissions is gone).
        versions = call("GET", f"/v2/inAppPurchases/{iap_id}/versions?limit=10")["data"]
        pending = [v for v in versions if v["attributes"].get("state") not in ("APPROVED", "WAITING_FOR_REVIEW", "IN_REVIEW")]
        if not pending:
            created = call("POST", "/v1/inAppPurchaseVersions", {"data": {"type": "inAppPurchaseVersions", "relationships": {
                "inAppPurchase": {"data": {"type": "inAppPurchases", "id": iap_id}}}}})["data"]
            pending = [created]
            print(f"  {product}: new version created")
        version_item = pending[0]
        added = call("POST", "/v1/reviewSubmissionItems", {"data": {"type": "reviewSubmissionItems", "relationships": {
            "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": sub["id"]}},
            "inAppPurchaseVersion": {"data": {"type": "inAppPurchaseVersions", "id": version_item["id"]}}}}}, ok_errors=(409,))
        print(f"  {product} version {version_item['attributes'].get('version')} ({version_item['attributes'].get('state')}) " + ("added" if added else "already in it"))
    items = call("GET", f"/v1/reviewSubmissions/{sub['id']}/items?limit=10")["data"]
    print(f"  items in the submission: {len(items)}")
    call("PATCH", f"/v1/reviewSubmissions/{sub['id']}", {"data": {"type": "reviewSubmissions", "id": sub["id"], "attributes": {"submitted": True}}})
    print(f"submission {sub['id']} submitted for review")


def review_notes(app: str, notes_path: Path):
    """Sets the App Review notes of the latest iOS version (the contact stays as it is)."""
    version = latest_version(app)
    detail = call("GET", f"/v1/appStoreVersions/{version['id']}/appStoreReviewDetail", ok_errors=(404,))
    notes = notes_path.read_text().strip()
    if detail and detail.get("data"):
        call("PATCH", f"/v1/appStoreReviewDetails/{detail['data']['id']}",
             {"data": {"type": "appStoreReviewDetails", "id": detail["data"]["id"], "attributes": {"notes": notes}}})
    else:
        call("POST", "/v1/appStoreReviewDetails", {"data": {"type": "appStoreReviewDetails", "attributes": {"notes": notes},
                                                            "relationships": {"appStoreVersion": {"data": {"type": "appStoreVersions", "id": version["id"]}}}}})
    print(f"review notes of {version['attributes'].get('versionString')} set ({len(notes)} characters)")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bundle-id", required=True)
    parser.add_argument("command", choices=["status", "price-free", "age-rating", "iap", "cancel-review", "attach-build", "submit", "review-notes"])
    parser.add_argument("--notes", type=Path, help="review-notes: the text file with the notes for App Review")
    parser.add_argument("--spec", type=Path, help="iap: the JSON file describing the product")
    parser.add_argument("--build", help="attach-build: the build number")
    parser.add_argument("--products", default="", help="submit: comma-separated product ids to submit with the version")
    args = parser.parse_args()
    app = app_id(args.bundle_id)
    if args.command == "status":
        status(app)
    elif args.command == "price-free":
        price_free(app)
    elif args.command == "age-rating":
        age_rating(app)
    elif args.command == "iap":
        if not args.spec:
            parser.error("iap needs --spec")
        iap(app, args.spec)
    elif args.command == "cancel-review":
        cancel_review(app)
    elif args.command == "attach-build":
        if not args.build:
            parser.error("attach-build needs --build")
        attach_build(app, args.build)
    elif args.command == "submit":
        submit(app, [p for p in args.products.split(",") if p])
    elif args.command == "review-notes":
        if not args.notes:
            parser.error("review-notes needs --notes")
        review_notes(app, args.notes)


if __name__ == "__main__":
    main()
