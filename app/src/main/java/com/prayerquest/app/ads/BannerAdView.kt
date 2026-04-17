package com.prayerquest.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.prayerquest.app.BuildConfig
import com.prayerquest.app.ui.theme.LocalIsPremium

/**
 * Composable adaptive banner. Sizes itself to the available screen width and
 * picks the best height for the device — higher fill rates and eCPM than a
 * fixed 320x50 banner.
 *
 * Hidden when the user is premium (read from [LocalIsPremium]) or
 * [AdManager.ADS_ENABLED] is false, so callers can drop it into layouts
 * unconditionally without an if-check at the call site.
 *
 * Uses [DisposableEffect] to destroy the underlying [AdView] when removed from
 * composition — otherwise the view would leak its context and continue
 * requesting ads in the background.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
) {
    val isPremium = LocalIsPremium.current
    if (isPremium || !AdManager.ADS_ENABLED) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidthDp = configuration.screenWidthDp

    val adSize = remember(adWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }

    val adView = remember {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
    )
}
