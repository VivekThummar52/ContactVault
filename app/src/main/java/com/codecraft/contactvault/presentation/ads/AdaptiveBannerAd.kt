package com.codecraft.contactvault.presentation.ads

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.codecraft.contactvault.data.ads.AdConfig
import com.codecraft.contactvault.domain.ads.AdManager
import com.codecraft.contactvault.domain.ads.AdPlacement
import com.codecraft.contactvault.ui.theme.ContactVaultTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdaptiveBannerAd(
    placement: AdPlacement,
    modifier: Modifier = Modifier
) {
    if (!AdConfig.ENABLE_ADS) return

    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AdMob Adaptive Banner ($placement)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val context = LocalContext.current
    if (!AdManager.isNetworkAvailable(context)) return

    val configuration = LocalConfiguration.current
    var isAdFailed by remember { mutableStateOf(false) }

    if (isAdFailed) return

    val adWidthDp = configuration.screenWidthDp

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { factoryContext ->
                AdView(factoryContext).apply {
                    setAdSize(
                        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                            factoryContext,
                            adWidthDp
                        )
                    )
                    adUnitId = AdConfig.getBannerAdUnitId()
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdFailed = false
                            Log.d("AdaptiveBannerAd", "Banner ad loaded ($placement)")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdFailed = true
                            Log.w("AdaptiveBannerAd", "Banner ad failed to load ($placement) [Code ${error.code}]: ${error.message}")
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdaptiveBannerAdPreview() {
    ContactVaultTheme {
        AdaptiveBannerAd(
            placement = AdPlacement.HOME_BANNER
        )
    }
}
