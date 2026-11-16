#!/usr/bin/env python3
"""Checks an app's PrivacyInfo.xcprivacy for the mistakes App Store Connect rejects after the upload (as "Invalid Binary"
at submission, with an e-mail; Apple's TN3181): NSPrivacyTracking true without tracking domains or domains without
true (ITMS-91064), unknown data types, purposes, API categories or reason codes, and reasons that do not belong to
their category. A data row with NSPrivacyCollectedDataTypeTracking true is fine without NSPrivacyTracking true
(Google's Mobile Ads manifest does exactly that); the SDKs declare their own tracking.

    python3 lint_privacy_manifest.py <path to PrivacyInfo.xcprivacy>
"""
import plistlib
import sys

DATA_TYPES = {
    "Name", "EmailAddress", "PhoneNumber", "PhysicalAddress", "OtherUserContactInfo", "Health", "Fitness",
    "PaymentInfo", "CreditInfo", "OtherFinancialInfo", "PreciseLocation", "CoarseLocation", "SensitiveInfo",
    "Contacts", "EmailsOrTextMessages", "PhotosorVideos", "AudioData", "GameplayContent", "CustomerSupport",
    "OtherUserContent", "BrowsingHistory", "SearchHistory", "UserID", "DeviceID", "PurchaseHistory",
    "ProductInteraction", "AdvertisingData", "OtherUsageData", "CrashData", "PerformanceData",
    "OtherDiagnosticData", "EnvironmentScanning", "Hands", "Head", "OtherDataTypes",
}
PURPOSES = {"ThirdPartyAdvertising", "DeveloperAdvertising", "Analytics", "ProductPersonalization",
            "AppFunctionality", "Other"}
REASONS = {
    "NSPrivacyAccessedAPICategoryFileTimestamp": {"DDA9.1", "C617.1", "3B52.1", "0A2A.1"},
    "NSPrivacyAccessedAPICategorySystemBootTime": {"35F9.1", "8FFB.1", "3D61.1"},
    "NSPrivacyAccessedAPICategoryDiskSpace": {"85F4.1", "E174.1", "7D9E.1", "B728.1"},
    "NSPrivacyAccessedAPICategoryActiveKeyboards": {"3EC4.1", "54BD.1"},
    "NSPrivacyAccessedAPICategoryUserDefaults": {"CA92.1", "1C8F.1", "C56D.1", "AC6B.1"},
}


def main(path: str) -> int:
    with open(path, "rb") as f:
        manifest = plistlib.load(f)
    problems = []
    tracking = manifest.get("NSPrivacyTracking")
    domains = manifest.get("NSPrivacyTrackingDomains", [])
    if tracking is None:
        problems.append("NSPrivacyTracking is missing")
    if tracking and not domains:
        problems.append("NSPrivacyTracking is true but NSPrivacyTrackingDomains is empty (Apple: provide the tracking domains)")
    if not tracking and domains:
        problems.append("NSPrivacyTrackingDomains given but NSPrivacyTracking is false")
    for row in manifest.get("NSPrivacyCollectedDataTypes", []):
        t = row.get("NSPrivacyCollectedDataType", "")
        if not t.startswith("NSPrivacyCollectedDataType") or t[len("NSPrivacyCollectedDataType"):] not in DATA_TYPES:
            problems.append(f"unknown data type {t}")
        for key in ("NSPrivacyCollectedDataTypeLinked", "NSPrivacyCollectedDataTypeTracking"):
            if not isinstance(row.get(key), bool):
                problems.append(f"{t}: {key} must be a boolean")
        purposes = row.get("NSPrivacyCollectedDataTypePurposes") or []
        if not purposes:
            problems.append(f"{t}: no purpose")
        for p in purposes:
            if not p.startswith("NSPrivacyCollectedDataTypePurpose") or p[len("NSPrivacyCollectedDataTypePurpose"):] not in PURPOSES:
                problems.append(f"{t}: unknown purpose {p}")
    for api in manifest.get("NSPrivacyAccessedAPITypes", []):
        category = api.get("NSPrivacyAccessedAPIType", "")
        reasons = api.get("NSPrivacyAccessedAPITypeReasons") or []
        if category not in REASONS:
            problems.append(f"unknown API category {category}")
            continue
        if not reasons:
            problems.append(f"{category}: no reason")
        for r in reasons:
            if r not in REASONS[category]:
                problems.append(f"{category}: reason {r} is not one of {sorted(REASONS[category])}")
    for p in problems:
        print(f"{path}: {p}")
    if not problems:
        print(f"{path}: ok (tracking={tracking}, {len(domains)} tracking domains, "
              f"{len(manifest.get('NSPrivacyCollectedDataTypes', []))} data types, {len(manifest.get('NSPrivacyAccessedAPITypes', []))} API categories)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
