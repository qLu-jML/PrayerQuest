package com.prayerquest.app.ui.crisis

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Crisis Resources screen (DD §3.10).
 *
 * ### Wellbeing-aware copy
 * Per the user-wellbeing section of the system prompt AND DD §3.10 line 265,
 * the copy here:
 * 1. **Does not claim any helpline is confidential, monitored, or staffed in
 *    any particular way.** Those assurances are not ours to make; they vary
 *    by line, by region, and by caller context. We just point to the
 *    resource.
 * 2. **Does not require any safety-assessment flow.** No "are you safe right
 *    now?" gate. No mood slider. The user taps a tile, they're taken here,
 *    they can dial immediately.
 *
 * The framing intentionally says "may help" — honest, not promotional.
 *
 * ### No XP, no tracking of whether the user dialed
 * We log only that the screen was opened (via [CrisisSessionFinalizer]). We
 * do NOT record dial-through events; a user reaching for a helpline should
 * feel private, not observed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisResourcesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        CrisisSessionFinalizer.logResourcesOpened()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Crisis resources",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Crisis Prayer"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IntroBlock()

            CrisisResource.BUNDLED.forEach { resource ->
                ResourceCard(
                    resource = resource,
                    onCall = { dialPhoneNumber(context, resource.dialString) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ClosingBlock()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IntroBlock() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            // Neutral, honest framing. "May help" — we don't over-promise.
            text = "If you're in danger or thinking about harming yourself, these organizations may help. Tap to call.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ClosingBlock() {
    Text(
        text = "If you're not in the US, UK, or Australia, a short web search for " +
                "\"crisis line\" plus your country name will usually surface a local " +
                "option.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
}

@Composable
private fun ResourceCard(
    resource: CrisisResource,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 96.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = resource.flag,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = resource.region,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = resource.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = resource.displayNumber,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics {
                        contentDescription = "Call ${resource.name}, ${resource.displayNumber}"
                    },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Hands off to the dialer pre-filled with the number. We use `ACTION_DIAL`
 * (not `ACTION_CALL`) deliberately — this shows the dialer with the number
 * in it, giving the user one more visual confirmation before the call
 * actually places. It also means we don't need `CALL_PHONE` runtime
 * permission.
 *
 * If no dialer exists (e.g. certain tablets), we swallow the exception
 * rather than crashing — the user can read the number off-screen.
 */
private fun dialPhoneNumber(context: android.content.Context, dialString: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$dialString")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}

/**
 * Bundled crisis-resource entries. Kept as a data class + top-level list so
 * a) all content is offline (no network lookups) and b) legal review only
 * has to look in one place if the list changes. Names and numbers are the
 * public, advertised contact methods for each organization.
 */
data class CrisisResource(
    val region: String,
    val flag: String,
    val name: String,
    val displayNumber: String,
    val dialString: String
) {
    companion object {
        val BUNDLED: List<CrisisResource> = listOf(
            CrisisResource(
                region = "United States",
                flag = "🇺🇸",
                name = "988 Suicide & Crisis Lifeline",
                displayNumber = "Call or text 988",
                dialString = "988"
            ),
            CrisisResource(
                region = "United Kingdom & Ireland",
                flag = "🇬🇧",
                name = "Samaritans",
                displayNumber = "116 123",
                dialString = "116123"
            ),
            CrisisResource(
                region = "Australia",
                flag = "🇦🇺",
                name = "Lifeline Australia",
                displayNumber = "13 11 14",
                dialString = "131114"
            )
        )
    }
}
