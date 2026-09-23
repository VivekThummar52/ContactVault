# ContactVault — AdMob Integration & Setup Guide

## Overview

ContactVault is a privacy-first, offline-first Android application. Monitization via Google Mobile Ads (AdMob) is designed to be completely non-intrusive, privacy-compliant, and fully isolated from user contact data.

AdMob is implemented as an optional infrastructure layer. When network connectivity is available, ads are served. When offline or if ad loading fails, the application operates seamlessly without any interruption, error dialogs, or data loss risks.

---

## Architecture Structure

Advertising components are isolated in dedicated layers:

```text
com.codecraft.contactvault
├── data/ads/
│   └── AdConfig.kt             # Ad Unit IDs & frequency capping rules
├── domain/ads/
│   ├── AdPlacement.kt          # Placement definitions
│   └── AdManager.kt            # Preloading, frequency capping, interstitial trigger
└── presentation/ads/
    └── AdaptiveBannerAd.kt     # Reusable Compose Anchored Adaptive Banner component
```

---

## AdMob Configuration

### 1. Application ID in `AndroidManifest.xml`

Google Mobile Ads SDK requires an Application ID declared in `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-3940256099942544~3347511713" />
```

> **Note:** The above ID is Google's dedicated Android sample Application ID used for testing. Replace this with your official AdMob Application ID before production release.

---

### 2. Google Test Ad Unit IDs (Development)

During development and testing, **only Google-provided test ad unit IDs** must be used:

* **Anchored Adaptive Banner:** `ca-app-pub-3940256099942544/9214589741`
* **Interstitial Ad:** `ca-app-pub-3940256099942544/1033173712`

---

### 3. Debug vs Release Configuration (`AdConfig.kt`)

In `AdConfig.kt`:
* **Debug builds (`BuildConfig.DEBUG == true`):** Automatically use Google Test Ad Unit IDs.
* **Release builds:** Return production Ad Unit IDs.

To configure production Ad Unit IDs for release:
1. Open `app/src/main/java/com/codecraft/contactvault/data/ads/AdConfig.kt`.
2. Replace `PROD_BANNER_AD_UNIT_ID` and `PROD_INTERSTITIAL_AD_UNIT_ID` with your verified AdMob Ad Unit IDs.

---

## Approved Ad Placements

To protect user experience and data safety, ads are shown **only on non-sensitive list-level screens**:

### A. Anchored Adaptive Banners
1. **Home / Dashboard (`HOME_BANNER`)** — Bottom anchored banner.
2. **Contacts List (`CONTACTS_BANNER`)** — Bottom anchored banner.
3. **Groups Listing (`GROUPS_BANNER`)** — Bottom anchored banner.
4. **Tags Listing (`TAGS_BANNER`)** — Bottom anchored banner.

### B. Interstitials
* **Natural Transition (`INTERSTITIAL_AFTER_BROWSING`)** — Triggered sparingly when user finishes browsing contact lists and returns home or switches top-level tabs.
* **Frequency Rules:**
  * Minimum 5 minutes (`300_000ms`) between interstitial ads.
  * Minimum 5 user navigation/actions required between ads.

---

## Excluded Screens (No Ads)

Ads are **STRICTLY PROHIBITED** on:
* Contact Details
* Edit Contact / Create Contact forms
* Contact Notes
* Duplicate Comparison & Merge Preview
* Merge Confirmation & Delete Confirmation
* Recently Deleted / Recovery
* Backup & Restore Settings
* Permission Explanation screens

---

## Privacy & Offline Behavior

1. **Privacy Protection:**
   * No contact names, phone numbers, emails, notes, or tags are EVER passed to AdMob or targeting requests.
   * Ad requests use plain `AdRequest.Builder().build()` with zero custom user targeting data.

2. **Offline-First Handling:**
   * In Airplane Mode or when offline, ad load attempts fail silently and non-fatally.
   * UI components collapse cleanly (`isAdFailed = true`).
   * No error popups, retries, or navigation blocks occur.
