package com.prayerquest.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.prayerquest.app.ui.theme.LocalIsPremium

@Composable
fun BannerAdView() {
    val isPremium = LocalIsPremium.current

    // Only show banner ads if not premium and ads are enabled
    if (isPremium || !AdManager.ADS_ENABLED) {
        return
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdManager.BANNER_AD_UNIT_ID
            }
        },
        update = { adView ->
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        }
    )
}
