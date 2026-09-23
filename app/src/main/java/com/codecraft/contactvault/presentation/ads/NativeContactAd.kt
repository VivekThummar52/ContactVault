package com.codecraft.contactvault.presentation.ads

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeContactAd(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ad",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Native Ad Preview Placeholder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This is a sample body for the native ad preview.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        return
    }

    if (nativeAd == null) return

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                val nativeAdView = NativeAdView(context)

                val container = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(32, 32, 32, 32)
                }

                // Top Header Row: Ad Badge + Advertiser
                val headerRow = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val adBadge = TextView(context).apply {
                    text = "Ad"
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTextColor(onPrimaryColor)
                    setBackgroundColor(primaryColor)
                    setPadding(12, 4, 12, 4)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val advertiserTv = TextView(context).apply {
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(onSurfaceVariantColor)
                    setPadding(16, 0, 0, 0)
                }

                headerRow.addView(adBadge)
                headerRow.addView(advertiserTv)
                container.addView(headerRow)

                // Middle Row: Icon + Headline & Body
                val mainRow = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 16, 0, 16)
                }

                val iconIv = ImageView(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(120, 120).apply {
                        marginEnd = 24
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                val textColumn = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val headlineTv = TextView(context).apply {
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTextColor(onSurfaceColor)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }

                val bodyTv = TextView(context).apply {
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(onSurfaceVariantColor)
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(0, 4, 0, 0)
                }

                textColumn.addView(headlineTv)
                textColumn.addView(bodyTv)

                mainRow.addView(iconIv)
                mainRow.addView(textColumn)
                container.addView(mainRow)

                // MediaView
                val mediaView = MediaView(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        360
                    ).apply {
                        setMargins(0, 8, 0, 16)
                    }
                }
                container.addView(mediaView)

                // Call to Action Button
                val ctaButton = Button(context).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setBackgroundColor(primaryColor)
                    setTextColor(onPrimaryColor)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                container.addView(ctaButton)

                nativeAdView.addView(container)

                // Register Views
                nativeAdView.headlineView = headlineTv
                nativeAdView.bodyView = bodyTv
                nativeAdView.callToActionView = ctaButton
                nativeAdView.iconView = iconIv
                nativeAdView.mediaView = mediaView
                nativeAdView.advertiserView = advertiserTv

                nativeAdView
            },
            update = { nativeAdView ->
                val headlineTv = nativeAdView.headlineView as? TextView
                val bodyTv = nativeAdView.bodyView as? TextView
                val ctaButton = nativeAdView.callToActionView as? Button
                val iconIv = nativeAdView.iconView as? ImageView
                val mediaView = nativeAdView.mediaView as? MediaView
                val advertiserTv = nativeAdView.advertiserView as? TextView

                headlineTv?.text = nativeAd.headline

                if (nativeAd.body != null) {
                    bodyTv?.text = nativeAd.body
                    bodyTv?.visibility = View.VISIBLE
                } else {
                    bodyTv?.visibility = View.GONE
                }

                if (nativeAd.callToAction != null) {
                    ctaButton?.text = nativeAd.callToAction
                    ctaButton?.visibility = View.VISIBLE
                } else {
                    ctaButton?.visibility = View.GONE
                }

                if (nativeAd.icon != null) {
                    iconIv?.setImageDrawable(nativeAd.icon?.drawable)
                    iconIv?.visibility = View.VISIBLE
                } else {
                    iconIv?.visibility = View.GONE
                }

                if (nativeAd.mediaContent != null) {
                    mediaView?.setMediaContent(nativeAd.mediaContent!!)
                    mediaView?.visibility = View.VISIBLE
                } else {
                    mediaView?.visibility = View.GONE
                }

                if (nativeAd.advertiser != null) {
                    advertiserTv?.text = nativeAd.advertiser
                    advertiserTv?.visibility = View.VISIBLE
                } else {
                    advertiserTv?.visibility = View.GONE
                }

                nativeAdView.setNativeAd(nativeAd)
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun NativeContactAdPreview() {
    com.codecraft.contactvault.ui.theme.ContactVaultTheme {
        NativeContactAd(nativeAd = null)
    }
}
