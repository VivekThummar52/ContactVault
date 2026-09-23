package com.codecraft.contactvault.domain.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.codecraft.contactvault.BuildConfig
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
    private const val MIN_RETRY_INTERVAL_MS = 15_000L // 15 seconds backoff between failed load retries

    private var appContext: Context? = null
    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false

    private var lastInterstitialShowTime = 0L
    private var lastLoadAttemptTime = 0L
    private var actionCount = 0

    fun getLastInterstitialShowTime(): Long = lastInterstitialShowTime

    fun isNetworkAvailable(context: Context?): Boolean {
        val targetContext = context ?: appContext ?: return false
        return try {
            val connectivityManager = targetContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network availability: ${e.message}")
            false
        }
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (isInitialized) {
            Log.d(TAG, "AdManager already initialized.")
            return
        }
        try {
            Log.d(TAG, "Initializing MobileAds SDK...")
            MobileAds.initialize(context.applicationContext) { initializationStatus ->
                Log.d(TAG, "MobileAds SDK initialized: $initializationStatus")
                isInitialized = true
                preloadInterstitial(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds SDK: ${e.message}", e)
        }
    }

    fun recordAction() {
        actionCount++
        Log.d(TAG, "Action recorded. Total actions since last ad: $actionCount")
        val targetContext = appContext
        if (interstitialAd == null && !isLoadingInterstitial && targetContext != null) {
            preloadInterstitial(targetContext)
        }
    }

    fun preloadInterstitial(context: Context? = appContext) {
        val targetContext = context?.applicationContext ?: appContext
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_INTERSTITIALS) {
            Log.d(TAG, "Preload skipped: Ads or Interstitials are disabled in AdConfig.")
            return
        }
        if (interstitialAd != null) {
            Log.d(TAG, "Preload skipped: Interstitial ad is already loaded.")
            return
        }
        if (isLoadingInterstitial) {
            Log.d(TAG, "Preload skipped: Interstitial ad load is already in progress.")
            return
        }
        if (targetContext == null) {
            Log.d(TAG, "Preload skipped: Application context is null.")
            return
        }
        if (!isNetworkAvailable(targetContext)) {
            Log.d(TAG, "Network unavailable. Skipping interstitial preload attempt.")
            return
        }

        val timeSinceLastAttempt = System.currentTimeMillis() - lastLoadAttemptTime
        if (timeSinceLastAttempt < MIN_RETRY_INTERVAL_MS && lastLoadAttemptTime > 0L) {
            Log.d(TAG, "Preload attempt throttled: Attempted ${timeSinceLastAttempt}ms ago (minimum retry interval: ${MIN_RETRY_INTERVAL_MS}ms).")
            return
        }

        try {
            isLoadingInterstitial = true
            lastLoadAttemptTime = System.currentTimeMillis()
            val adUnitId = AdConfig.getInterstitialAdUnitId()
            Log.d(TAG, "Starting interstitial ad preload attempt (AdUnitId: $adUnitId)...")

            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                targetContext,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isLoadingInterstitial = false
                        Log.d(TAG, "Interstitial ad loaded successfully.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isLoadingInterstitial = false
                        Log.w(TAG, "Interstitial ad failed to load [Code ${error.code}]: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            interstitialAd = null
            isLoadingInterstitial = false
            Log.e(TAG, "Unexpected exception during interstitial ad preload: ${e.message}", e)
        }
    }

    fun canShowInterstitial(): Boolean {
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_INTERSTITIALS) {
            Log.d(TAG, "canShowInterstitial: False - Ads or Interstitials disabled in AdConfig.")
            return false
        }
        if (interstitialAd == null) {
            Log.d(TAG, "canShowInterstitial: False - Interstitial ad is not ready / null.")
            return false
        }

        val actionThreshold = if (BuildConfig.DEBUG) 3 else AdConfig.MIN_ACTIONS_BETWEEN_INTERSTITIALS
        val actionsThresholdReached = actionCount >= actionThreshold

        if (BuildConfig.DEBUG) {
            if (!actionsThresholdReached) {
                Log.d(TAG, "canShowInterstitial (DEBUG): False - Action count ($actionCount) < required threshold ($actionThreshold).")
            }
            return actionsThresholdReached
        }

        val timeElapsedMs = System.currentTimeMillis() - lastInterstitialShowTime
        val timeElapsed = timeElapsedMs >= AdConfig.MIN_INTERSTITIAL_INTERVAL_MS

        if (!timeElapsed) {
            Log.d(TAG, "canShowInterstitial: False - Time elapsed (${timeElapsedMs}ms) < required interval (${AdConfig.MIN_INTERSTITIAL_INTERVAL_MS}ms).")
        }
        if (!actionsThresholdReached) {
            Log.d(TAG, "canShowInterstitial: False - Action count ($actionCount) < required threshold ($actionThreshold).")
        }

        return timeElapsed && actionsThresholdReached
    }

    fun tryShowInterstitial(
        activity: Activity?,
        placement: AdPlacement,
        onAdDismissed: () -> Unit
    ) {
        var callbackInvoked = false
        fun invokeDismissCallbackOnce() {
            if (!callbackInvoked) {
                callbackInvoked = true
                onAdDismissed()
            }
        }

        try {
            if (activity == null) {
                Log.d(TAG, "Interstitial skipped ($placement): Activity is null.")
                invokeDismissCallbackOnce()
                return
            }

            val targetContext = activity.applicationContext ?: appContext
            if (!canShowInterstitial()) {
                Log.d(TAG, "Interstitial skipped ($placement): Conditions not met for display.")
                if (interstitialAd == null && targetContext != null) {
                    preloadInterstitial(targetContext)
                }
                invokeDismissCallbackOnce()
                return
            }

            val ad = interstitialAd
            if (ad == null) {
                Log.d(TAG, "Interstitial skipped ($placement): Ad reference is null.")
                if (targetContext != null) {
                    preloadInterstitial(targetContext)
                }
                invokeDismissCallbackOnce()
                return
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad showed full screen content ($placement).")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed ($placement).")
                    interstitialAd = null
                    lastInterstitialShowTime = System.currentTimeMillis()
                    actionCount = 0
                    if (targetContext != null) {
                        preloadInterstitial(targetContext)
                    }
                    invokeDismissCallbackOnce()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show ($placement) [Code ${adError.code}]: ${adError.message}")
                    interstitialAd = null
                    if (targetContext != null) {
                        preloadInterstitial(targetContext)
                    }
                    invokeDismissCallbackOnce()
                }
            }

            Log.d(TAG, "Showing interstitial ad ($placement)...")
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception in tryShowInterstitial ($placement): ${e.message}", e)
            interstitialAd = null
            val targetContext = activity?.applicationContext ?: appContext
            if (targetContext != null) {
                preloadInterstitial(targetContext)
            }
            invokeDismissCallbackOnce()
        }
    }
}
