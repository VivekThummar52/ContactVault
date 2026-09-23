package com.codecraft.contactvault.domain.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.codecraft.contactvault.data.ads.AdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

object AppOpenAdManager : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private const val TAG = "AppOpenAdManager"
    private const val PREF_NAME = "contactvault_ad_prefs"
    private const val KEY_HAS_SEEN_FIRST_LAUNCH = "has_seen_first_launch"
    private const val MIN_RETRY_INTERVAL_MS = 15_000L

    private var appContext: Context? = null
    private var appOpenAd: AppOpenAd? = null
    private var isLoading = false
    private var isShowingAppOpenAd = false

    private var loadTimestamp = 0L
    private var lastAppOpenShowTime = 0L
    private var lastLoadAttemptTime = 0L

    private var currentActivity: Activity? = null

    fun initialize(context: Context) {
        val targetContext = context.applicationContext
        appContext = targetContext
        if (targetContext is Application) {
            targetContext.registerActivityLifecycleCallbacks(this)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "AppOpenAdManager initialized.")

        preloadAd(targetContext)
    }

    fun preloadAd(context: Context? = appContext) {
        val targetContext = context?.applicationContext ?: appContext
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_APP_OPEN_ADS) {
            Log.d(TAG, "Preload skipped: Ads or App Open Ads are disabled in AdConfig.")
            return
        }
        if (appOpenAd != null && !isAdExpired()) {
            Log.d(TAG, "Preload skipped: Valid App Open Ad is already loaded.")
            return
        }
        if (isLoading) {
            Log.d(TAG, "Preload skipped: App Open Ad load is already in progress.")
            return
        }
        if (targetContext == null) {
            Log.d(TAG, "Preload skipped: Application context is null.")
            return
        }
        if (!AdManager.isNetworkAvailable(targetContext)) {
            Log.d(TAG, "Network unavailable. Skipping App Open preload attempt.")
            return
        }

        val timeSinceLastAttempt = System.currentTimeMillis() - lastLoadAttemptTime
        if (timeSinceLastAttempt < MIN_RETRY_INTERVAL_MS && lastLoadAttemptTime > 0L) {
            Log.d(TAG, "Preload attempt throttled: Attempted ${timeSinceLastAttempt}ms ago.")
            return
        }

        try {
            isLoading = true
            lastLoadAttemptTime = System.currentTimeMillis()
            val adUnitId = AdConfig.getAppOpenAdUnitId()
            Log.d(TAG, "Starting App Open ad preload (AdUnitId: $adUnitId)...")

            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                targetContext,
                adUnitId,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoading = false
                        loadTimestamp = System.currentTimeMillis()
                        Log.d(TAG, "App Open ad loaded successfully.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        isLoading = false
                        Log.w(TAG, "App Open ad failed to load [Code ${error.code}]: ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            appOpenAd = null
            isLoading = false
            Log.e(TAG, "Unexpected exception during App Open ad preload: ${e.message}", e)
        }
    }

    private fun isAdExpired(): Boolean {
        if (appOpenAd == null) return true
        val elapsed = System.currentTimeMillis() - loadTimestamp
        val expired = elapsed >= AdConfig.APP_OPEN_AD_EXPIRATION_MS
        if (expired) {
            Log.d(TAG, "App Open ad expired (elapsed ${elapsed}ms >= ${AdConfig.APP_OPEN_AD_EXPIRATION_MS}ms).")
        }
        return expired
    }

    private fun canShowAd(context: Context): Boolean {
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_APP_OPEN_ADS) {
            Log.d(TAG, "Foreground ignored: Ads or App Open Ads disabled in AdConfig.")
            return false
        }

        // First-launch protection check
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val hasSeenFirstLaunch = prefs.getBoolean(KEY_HAS_SEEN_FIRST_LAUNCH, false)
        if (!hasSeenFirstLaunch) {
            prefs.edit().putBoolean(KEY_HAS_SEEN_FIRST_LAUNCH, true).apply()
            Log.d(TAG, "Foreground ignored because first launch.")
            return false
        }

        // Cooldown check
        val timeSinceLastShow = System.currentTimeMillis() - lastAppOpenShowTime
        if (timeSinceLastShow < AdConfig.MIN_APP_OPEN_INTERVAL_MS && lastAppOpenShowTime > 0L) {
            Log.d(TAG, "Foreground ignored because cooldown (elapsed ${timeSinceLastShow}ms < ${AdConfig.MIN_APP_OPEN_INTERVAL_MS}ms).")
            return false
        }

        // Back-to-back fullscreen ads check (Interstitial shown recently)
        val lastInterstitialShow = AdManager.getLastInterstitialShowTime()
        val timeSinceInterstitial = System.currentTimeMillis() - lastInterstitialShow
        if (lastInterstitialShow > 0L && timeSinceInterstitial < AdConfig.MIN_INTERSTITIAL_INTERVAL_MS) {
            Log.d(TAG, "Foreground ignored because interstitial shown recently (${timeSinceInterstitial}ms ago).")
            return false
        }

        // Ad valid check
        if (appOpenAd == null || isAdExpired()) {
            Log.d(TAG, "Foreground ignored because no valid ad loaded.")
            if (appOpenAd == null && !isLoading) {
                preloadAd(context)
            } else if (isAdExpired()) {
                appOpenAd = null
                preloadAd(context)
            }
            return false
        }

        // Activity availability check
        val activity = currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.d(TAG, "Foreground ignored because Activity unavailable.")
            return false
        }

        // Already showing check
        if (isShowingAppOpenAd) {
            Log.d(TAG, "Foreground ignored because App Open ad is already showing.")
            return false
        }

        return true
    }

    private fun tryShowAd() {
        val activity = currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.d(TAG, "Show skipped: Activity unavailable.")
            return
        }

        val targetContext = activity.applicationContext ?: appContext ?: return
        if (!canShowAd(targetContext)) {
            return
        }

        val ad = appOpenAd
        if (ad == null || isAdExpired()) {
            appOpenAd = null
            preloadAd(targetContext)
            return
        }

        try {
            isShowingAppOpenAd = true
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open ad showed full screen content.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App Open ad dismissed.")
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    lastAppOpenShowTime = System.currentTimeMillis()
                    preloadAd(targetContext)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "App Open ad failed to show [Code ${adError.code}]: ${adError.message}")
                    appOpenAd = null
                    isShowingAppOpenAd = false
                    preloadAd(targetContext)
                }

                override fun onAdImpression() {
                    Log.d(TAG, "App Open ad recorded impression.")
                }
            }

            Log.d(TAG, "Showing App Open ad...")
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception showing App Open ad: ${e.message}", e)
            appOpenAd = null
            isShowingAppOpenAd = false
            preloadAd(targetContext)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "Foreground detected via ProcessLifecycleOwner.")
        tryShowAd()
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
