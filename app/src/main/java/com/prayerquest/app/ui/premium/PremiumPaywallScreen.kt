package com.prayerquest.app.ui.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.billing.PremiumFeatures
import com.prayerquest.app.ui.theme.LocalIsPremium
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Premium upgrade paywall.
 *
 * Entry points:
 *  • "Upgrade to Premium" card on Profile
 *  • Inline gates (add-11th-photo, add-6th-group-member, create-3rd-group, etc.)
 *  • Optional CTA on the ads-bottom-row (tap-to-remove-ads)
 *
 * Copy emphasizes the three DD §5 premium benefits:
 *  1. Ad-free experience
 *  2. Unlimited photo storage for Gratitude + Answered Prayers
 *  3. Enhanced Prayer Groups (larger member cap, custom icons, more groups)
 *
 * Price text is pulled from Play (via [BillingManager.productDetails]) so we
 * never display a stale price — if Google changes the formatted price for
 * the user's locale or currency, we pick it up automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: PremiumPaywallViewModel = viewModel(
        factory = PremiumPaywallViewModel.Factory(app.container)
    )

    val isPremium = LocalIsPremium.current
    val priceText by viewModel.priceText.collectAsState(initial = null)
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.premium_prayerquest_premium)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HeroCard() }
            item { FeatureList() }
            item { CurrentStatusCard(isPremium = isPremium) }
            item {
                Text(
                    text = stringResource(R.string.premium_subscription_auto_renews_monthly_at_the_price_show),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Capture into a local val so the smart cast sticks — `priceText`
                // is a delegated property (by collectAsState), which Kotlin won't
                // smart-cast to non-null even after an explicit null check.
                val price = priceText
                val cta = when {
                    isPremium -> stringResource(R.string.premium_you_re_premium_thank_you)
                    price != null -> stringResource(R.string.premium_upgrade_for_x, price)
                    else -> stringResource(R.string.common_upgrade_to_premium)
                }
                Button(
                    onClick = {
                        val activity = context as? Activity ?: return@Button
                        viewModel.launchPurchase(activity)
                    },
                    enabled = !isPremium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(cta, style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = stringResource(R.string.premium_he_is_faithful_support_prayerquest_s_growth_while),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HeroCard() {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primaryContainer,
                            colorScheme.tertiaryContainer,
                        ),
                    )
                )
                .padding(vertical = 28.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(R.string.premium_pray_deeper_without_limits),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.premium_unlock_every_feature_and_support_a_faith_first_hab),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FeatureList() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumFeatureRow(
            icon = Icons.Default.Bolt,
            title = stringResource(R.string.premium_ad_free_always),
            description = stringResource(R.string.premium_no_banners_no_interstitials_no_app_open_ads_just_p),
        )
        PremiumFeatureRow(
            icon = Icons.Default.PhotoLibrary,
            title = stringResource(R.string.premium_unlimited_photos),
            description = stringResource(R.string.premium_attach_as_many_photos_as_you_want_to_gratitude_ent, PremiumFeatures.FREE_GRATITUDE_PHOTOS_PER_MONTH),
        )
        PremiumFeatureRow(
            icon = Icons.Default.Groups,
            title = stringResource(R.string.premium_enhanced_prayer_groups),
            description = stringResource(R.string.premium_up_to_x_members_per_group_free_x_create_up_to_x_gr, PremiumFeatures.PREMIUM_GROUP_MEMBER_LIMIT, PremiumFeatures.FREE_GROUP_MEMBER_LIMIT, PremiumFeatures.PREMIUM_GROUPS_CREATED_LIMIT, PremiumFeatures.FREE_GROUPS_CREATED_LIMIT),
        )
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(isPremium: Boolean) {
    if (!isPremium) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column {
                Text(
                    text = stringResource(R.string.premium_you_re_a_premium_supporter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(R.string.premium_thank_you_for_helping_us_build_prayerquest_manage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
