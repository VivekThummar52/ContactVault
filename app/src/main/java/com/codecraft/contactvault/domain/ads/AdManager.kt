package com.codecraft.contactvault.domain.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.codecraft.contactvault.data.ads.AdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    private var lastInterstitialShowTime = 0L
    private var actionCount = 0

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Log.d(TAG, "MobileAds initialized: $initializationStatus")
                isInitialized = true
                preloadInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds: ${e.message}")
        }
    }

    fun recordAction() {
        actionCount++
        Log.d(TAG, "Action recorded. Total actions since last ad: $actionCount")
    }

    fun preloadInterstitial(context: Context) {
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_INTERSTITIALS) return
        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "Device is offline. Skipping interstitial preload.")
            return
        }
        if (interstitialAd != null || isLoadingInterstitial) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.getInterstitialAdUnitId()

        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun canShowInterstitial(): Boolean {
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_INTERSTITIALS) return false
        val adReady = interstitialAd != null
        if (!adReady) return false

        // In Debug mode, allow testing interstitials after at least 1 action
        if (com.codecraft.contactvault.BuildConfig.DEBUG) {
            return actionCount >= 1
        }

        val timeElapsed = System.currentTimeMillis() - lastInterstitialShowTime >= AdConfig.MIN_INTERSTITIAL_INTERVAL_MS
        val actionsThresholdReached = actionCount >= AdConfig.MIN_ACTIONS_BETWEEN_INTERSTITIALS
        return timeElapsed && actionsThresholdReached
    }

    fun tryShowInterstitial(
        activity: Activity?,
        placement: AdPlacement,
        onAdDismissed: () -> Unit
    ) {
        if (activity == null || !canShowInterstitial()) {
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed ($placement)")
                interstitialAd = null
                lastInterstitialShowTime = System.currentTimeMillis()
                actionCount = 0
                preloadInterstitial(activity.applicationContext)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                interstitialAd = null
                preloadInterstitial(activity.applicationContext)
                onAdDismissed()
            }
        }

        ad.show(activity)
    }
}
