package com.prayerquest.app.ui.premium

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Buy Me a Coffee page for PrayerQuest. Opens in the user's default browser
 * so they can:
 *  - Use saved credit card autofill, Google Pay, Apple Pay, or PayPal
 *  - See the real URL for peace of mind before entering payment info
 *  - Choose a preset amount or enter a custom number
 *
 * No account is required from the donor. Buy Me a Coffee takes a 5% platform
 * fee plus standard payment processor fees; everything else comes through as
 * weekly payouts via Stripe or PayPal. Same BMC page as ScriptureQuest — the
 * author's single page handles both apps.
 */
const val DONATE_URL = "https://buymeacoffee.com/fivecatstudios"

/**
 * Preset dollar amounts shown as quick-pick buttons on the Donate card.
 *
 * Every button opens the same Buy Me a Coffee page — BMC's checkout handles
 * final amount selection. The presets here act as a *commitment nudge*:
 * tapping "$5" anchors the donor to that number mentally, making them more
 * likely to follow through with that amount on BMC's checkout.
 *
 * Tweak freely — they're just Ints. [3, 5, 10] fits nicely on a phone row;
 * four buttons (including "Other") is the max before labels start to squish.
 */
private val PRESET_AMOUNTS = listOf(3, 5, 10)

/**
 * A soft "Support the mission" card for the Profile screen. Visible to both
 * free and premium users — premium subscribers who want to give beyond the
 * subscription can tap here too.
 *
 * Layout: heart icon + headline on top, a row of preset-amount buttons
 * plus an "Other" option underneath. Tapping any button launches the Buy
 * Me a Coffee page in the user's default browser.
 *
 * When the user returns to the app after donating (ON_RESUME fires), a
 * friendly thank-you toast appears. We can't verify they actually paid
 * (that would require BMC's webhook API), so the toast is intentionally
 * warm-but-neutral — it acknowledges the intent regardless of outcome.
 */
@Composable
fun DonateCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // True between "user tapped Donate" and "app has returned to foreground".
    // On the return trip we fire the thank-you toast exactly once, then reset.
    var awaitingReturn by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    // Resolve at @Composable level — `DisposableEffect`'s effect block is
    // non-@Composable (it's `DisposableEffectScope.() -> DisposableEffectResult`),
    // and the nested LifecycleEventObserver lambda is even more so.
    val thanksToastText = stringResource(R.string.premium_thanks_for_supporting_prayerquest)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingReturn) {
                awaitingReturn = false
                Toast.makeText(
                    context,
                    thanksToastText,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: heart icon + copy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.premium_support_prayerquest),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        stringResource(R.string.premium_buy_us_a_coffee_every_gift_fuels_more_prayer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Preset amount buttons + "Other" for custom amounts.
            //
            // We override two M3 defaults here for a reason: the stock Button
            // ships with chunky horizontal contentPadding (24.dp) and a fully
            // rounded pill shape. With four equal-weight buttons in a row that
            // padding leaves no room for the labels, so Compose wraps "$10" and
            // "Other" character-by-character into ugly little circles. Tighter
            // padding + a rounded rectangle (not pill) keeps everything on a
            // single line at typical phone widths and reads cleaner.
            val buttonShape = RoundedCornerShape(14.dp)
            val tightPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESET_AMOUNTS.forEach { amount ->
                    Button(
                        onClick = {
                            awaitingReturn = true
                            openDonateLink(context)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        shape = buttonShape,
                        contentPadding = tightPadding,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            "$$amount",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        awaitingReturn = true
                        openDonateLink(context)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shape = buttonShape,
                    contentPadding = tightPadding
                ) {
                    Text(
                        stringResource(R.string.common_other),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Launches the donate page in the user's default browser. We don't use
 * Custom Tabs (no extra dependency needed) — a plain ACTION_VIEW intent is
 * the most trustworthy flow for payment pages because the donor sees the
 * real URL in their address bar.
 */
private fun openDonateLink(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DONATE_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
