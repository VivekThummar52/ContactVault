package com.codecraft.contactvault.domain.ads

import android.content.Context
import android.util.Log
import com.codecraft.contactvault.data.ads.AdConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NativeAdManager {
    private const val TAG = "NativeAdManager"

    private val _nativeAdState = MutableStateFlow<NativeAd?>(null)
    val nativeAdState: StateFlow<NativeAd?> = _nativeAdState.asStateFlow()

    private var isLoading = false

    fun loadNativeAd(context: Context) {
        if (!AdConfig.ENABLE_ADS || !AdConfig.ENABLE_NATIVE_ADS) {
            Log.d(TAG, "Native ad load skipped: Ads or Native Ads are disabled in AdConfig.")
            return
        }

        if (isLoading) {
            Log.d(TAG, "Native ad load skipped: Duplicate load request prevented.")
            return
        }

        if (!AdManager.isNetworkAvailable(context)) {
            Log.d(TAG, "Native ad load skipped: Network is unavailable.")
            return
        }

        if (_nativeAdState.value != null) {
            Log.d(TAG, "Native ad already loaded. Skipping load request.")
            return
        }

        try {
            isLoading = true
            val adUnitId = AdConfig.getNativeAdUnitId()
            Log.d(TAG, "Starting native ad load (AdUnitId: $adUnitId)...")

            val adLoader = AdLoader.Builder(context.applicationContext, adUnitId)
                .forNativeAd { ad ->
                    Log.d(TAG, "Native ad successfully loaded.")
                    destroyPreviousAd()
                    _nativeAdState.value = ad
                    isLoading = false
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoading = false
                        Log.w(TAG, "Native ad failed to load [Code ${error.code}]: ${error.message}")
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            isLoading = false
            Log.e(TAG, "Unexpected exception loading native ad: ${e.message}", e)
        }
    }

    private fun destroyPreviousAd() {
        _nativeAdState.value?.let { oldAd ->
            try {
                oldAd.destroy()
                Log.d(TAG, "Previous native ad destroyed.")
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying previous native ad: ${e.message}")
            }
        }
        _nativeAdState.value = null
    }

    fun destroy() {
        _nativeAdState.value?.let { ad ->
            try {
                ad.destroy()
                Log.d(TAG, "Native ad destroyed/cleanup.")
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying native ad: ${e.message}")
            }
        }
        _nativeAdState.value = null
        isLoading = false
    }
}
