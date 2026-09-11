package com.codecraft.contactvault.data.ads

import com.codecraft.contactvault.BuildConfig

object AdConfig {
    // Official Google Android Test Ad Unit IDs
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Replace these with your real Ad Unit IDs from AdMob Console (format: "ca-app-pub-6451959305686894/XXXXXXXXXX")
    var PROD_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    var PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Set to true if you want to test your real Ad Unit IDs in Debug builds
    var USE_PRODUCTION_AD_UNITS_IN_DEBUG = false

    const val ENABLE_ADS: Boolean = true
    const val ENABLE_INTERSTITIALS: Boolean = true

    // Interstitial frequency limits
    const val MIN_INTERSTITIAL_INTERVAL_MS: Long = 5 * 60 * 1000L // 5 minutes
    const val MIN_ACTIONS_BETWEEN_INTERSTITIALS: Int = 5

    fun getBannerAdUnitId(): String {
        return if (USE_PRODUCTION_AD_UNITS_IN_DEBUG || !BuildConfig.DEBUG) {
            PROD_BANNER_AD_UNIT_ID
        } else {
            TEST_BANNER_AD_UNIT_ID
        }
    }

    fun getInterstitialAdUnitId(): String {
        return if (USE_PRODUCTION_AD_UNITS_IN_DEBUG || !BuildConfig.DEBUG) {
            PROD_INTERSTITIAL_AD_UNIT_ID
        } else {
            TEST_INTERSTITIAL_AD_UNIT_ID
        }
    }
}
