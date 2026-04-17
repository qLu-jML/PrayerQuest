package com.prayerquest.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.billing.PremiumFeatures
import com.prayerquest.app.data.repository.PrayerGroupRepository
import com.prayerquest.app.ui.theme.LocalIsPremium
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: CreateGroupViewModel = viewModel(
        factory = CreateGroupViewModel.Factory(app.container.prayerGroupRepository)
    )

    val isPremium = LocalIsPremium.current
    val existingGroupCount by viewModel.groupCount.collectAsState()
    val groupsCreatedLimit = PremiumFeatures.groupsCreatedLimitFor(isPremium)
    val atGroupLimit = existingGroupCount >= groupsCreatedLimit

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🙏") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Create Group") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (atGroupLimit && !isPremium) {
                item {
                    // Group-creation cap banner. Free users can create up to
                    // PremiumFeatures.FREE_GROUPS_CREATED_LIMIT groups. Upgrading
                    // bumps the cap to the premium ceiling. We still render the
                    // form so the user can keep configuring the group — the
                    // Create button shifts to "Upgrade to Premium" below.
                    PremiumLimitBanner(
                        title = "Group limit reached",
                        description = "Free accounts can create up to ${PremiumFeatures.FREE_GROUPS_CREATED_LIMIT} prayer groups. Upgrade to Premium to create up to ${PremiumFeatures.PREMIUM_GROUPS_CREATED_LIMIT}.",
                        onUpgrade = onNavigateToPaywall,
                    )
                }
            }

            item {
                // Emoji picker section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Choose an Emoji",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isPremium) {
                            AssistChip(
                                onClick = onNavigateToPaywall,
                                label = { Text("Unlock all", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                },
                            )
                        }
                    }
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current selection
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedEmoji,
                                    style = MaterialTheme.typography.displaySmall
                                )
                            }

                            // Emoji grid. Free tier unlocks the first
                            // FREE_COMMUNITY_EMOJI_COUNT emojis; anything past
                            // that renders with a Lock overlay and routes to
                            // the paywall on tap. Premium unlocks everything.
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(COMMUNITY_EMOJI_OPTIONS) { index, emoji ->
                                    val locked = !isPremium &&
                                        index >= FREE_COMMUNITY_EMOJI_COUNT
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (emoji == selectedEmoji)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable {
                                                if (locked) onNavigateToPaywall()
                                                else selectedEmoji = emoji
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = emoji,
                                            style = MaterialTheme.typography.headlineSmall,
                                            modifier = if (locked) {
                                                Modifier.alpha(0.35f)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        if (locked) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Premium only",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .align(Alignment.BottomEnd),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Our Church Prayer Circle") },
                    singleLine = true,
                    maxLines = 1
                )
            }

            item {
                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("What's this group about?") },
                    minLines = 3,
                    maxLines = 3
                )
            }

            item {
                // Helper text
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Create a new prayer group to share and pray with others. A unique invite code will be generated automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "You'll be the group admin and can invite others using the share code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Pinned bottom action bar. Previously the Create button lived as the
        // last child of the outer Column with a malformed modifier chain
        // (`.height(48.dp).padding(vertical = 16.dp)` squeezed the button
        // content into 16dp of visible height) AND had no navigation-bar
        // inset, so on gesture-nav devices the button rendered behind the
        // system gesture area and was un-tappable / un-scrollable. Wrapping
        // it in a tonal Surface with `navigationBarsPadding()` matches the
        // PrayerModeScaffold pattern used everywhere else in the app.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            atGroupLimit && !isPremium -> onNavigateToPaywall()
                            name.isNotBlank() -> {
                                isLoading = true
                                viewModel.createGroup(name, description, selectedEmoji) {
                                    isLoading = false
                                    onGroupCreated()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    // Stay enabled when the user is over the free cap so they can
                    // tap to open the paywall. The click handler above branches
                    // on `atGroupLimit` to decide between create and upgrade.
                    enabled = !isLoading && (
                        (atGroupLimit && !isPremium) || name.isNotBlank()
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        val label = if (atGroupLimit && !isPremium) {
                            "Upgrade to Premium"
                        } else {
                            "Create Group"
                        }
                        Text(label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private val COMMUNITY_EMOJI_OPTIONS = listOf(
    "🙏", "🤝", "❤️", "💪", "🌟", "⛪", "🕊️", "✝️",
    "👨‍👩‍👧‍👦", "👥", "🔥", "💎", "🌈", "🎯", "📖", "🎁",
    "🌍", "🌱", "📿", "🕯️", "🌸", "💝", "😊", "🙌",
    "⏰", "🎓", "💼", "🎵", "🌙", "☀️"
)

/**
 * How many of the [COMMUNITY_EMOJI_OPTIONS] are available to free users. The
 * remainder are a Premium perk ("pick any custom emoji"). Chosen to cover the
 * prayer/community staples (praying hands through the cross) while leaving
 * variety for Premium.
 */
private const val FREE_COMMUNITY_EMOJI_COUNT = 8

/**
 * Reusable cap-reached banner. Matches the visual language used by other
 * inline Premium gates (group emoji lock, gratitude photo limit).
 */
@Composable
private fun PremiumLimitBanner(
    title: String,
    description: String,
    onUpgrade: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            TextButton(onClick = onUpgrade) {
                Text("See Premium")
            }
        }
    }
}

class CreateGroupViewModel(
    private val groupRepository: PrayerGroupRepository
) : ViewModel() {

    private val _groupCount = MutableStateFlow(0)

    /**
     * Groups currently on device. Compared against
     * [com.prayerquest.app.billing.PremiumFeatures.FREE_GROUPS_CREATED_LIMIT]
     * to decide whether the Create button triggers creation or bumps the user
     * to the paywall.
     *
     * Reads via a one-shot call to [PrayerGroupRepository.getGroupCount] each
     * time the screen opens. Creating a group and returning would re-open the
     * Create screen fresh so we don't bother with a Flow here.
     */
    val groupCount: StateFlow<Int> = _groupCount.asStateFlow()

    init {
        viewModelScope.launch {
            _groupCount.value = groupRepository.getGroupCount()
        }
    }

    fun createGroup(name: String, description: String, emoji: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            groupRepository.createGroup(name, description, emoji)
            onComplete()
        }
    }

    class Factory(
        private val groupRepository: PrayerGroupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateGroupViewModel(groupRepository) as T
        }
    }
}
