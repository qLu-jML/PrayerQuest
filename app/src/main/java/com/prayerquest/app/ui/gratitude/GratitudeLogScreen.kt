package com.prayerquest.app.ui.gratitude

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.billing.PremiumFeatures
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.ui.theme.GratitudeGold
import com.prayerquest.app.ui.theme.GratitudeGreen
import com.prayerquest.app.ui.theme.LocalIsPremium
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GratitudeLogScreen(
    onNavigateBack: () -> Unit,
    onGratitudeSaved: (xpEarned: Int) -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: GratitudeLogViewModel = viewModel(
        factory = GratitudeLogViewModel.Factory(app.container.gratitudeRepository, app.container.gamificationRepository)
    )

    val isPremium = LocalIsPremium.current
    val photoCountThisMonth by viewModel.photoCountThisMonth.collectAsState()
    val monthlyPhotoCap = PremiumFeatures.gratitudePhotosPerMonthFor(isPremium)
    // For free users, the cap is a small int (10). Premium passes Int.MAX_VALUE
    // from PremiumFeatures.gratitudePhotosPerMonthFor — the comparison below
    // always yields false in that case, so premium users are never gated.
    val atPhotoCap = photoCountThisMonth >= monthlyPhotoCap

    var entryCount by remember { mutableIntStateOf(1) }
    var gratitudeTexts by remember { mutableStateOf(listOf("")) }
    var selectedCategories by remember { mutableStateOf(listOf(GratitudeEntry.CATEGORY_OTHER)) }
    var hasPhotos by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val dateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val today = LocalDate.now().format(dateFormat)

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Daily Gratitude") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with verse prompt
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What are you thankful for today?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = today,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "\"Give thanks in all circumstances; for this is God's will for you in Christ Jesus.\" — 1 Thessalonians 5:18",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                // Entry count selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "How many gratitudes today?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5).forEach { count ->
                            FilterChip(
                                selected = entryCount == count,
                                onClick = {
                                    entryCount = count
                                    gratitudeTexts = List(count) { "" }
                                    selectedCategories = List(count) { GratitudeEntry.CATEGORY_OTHER }
                                },
                                label = { Text("$count gratitude${if (count > 1) "s" else ""}", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = "Logging ${if (entryCount == 1) "1 gratitude" else "$entryCount gratitudes"} will earn you ${5 + ((entryCount - 1).coerceAtLeast(0) * 3) + (if (hasPhotos) 5 else 0)} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = GratitudeGreen
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Gratitude entry fields
            items(entryCount) { index ->
                GratitudeEntryField(
                    index = index,
                    text = if (index < gratitudeTexts.size) gratitudeTexts[index] else "",
                    onTextChange = {
                        val newList = gratitudeTexts.toMutableList()
                        while (newList.size <= index) newList.add("")
                        newList[index] = it
                        gratitudeTexts = newList
                    },
                    selectedCategory = if (index < selectedCategories.size) selectedCategories[index] else GratitudeEntry.CATEGORY_OTHER,
                    onCategoryChange = {
                        val newList = selectedCategories.toMutableList()
                        while (newList.size <= index) newList.add(GratitudeEntry.CATEGORY_OTHER)
                        newList[index] = it
                        selectedCategories = newList
                    },
                    onPhotoClick = {
                        if (atPhotoCap && !isPremium) {
                            onNavigateToPaywall()
                        } else {
                            hasPhotos = true
                        }
                    },
                    photoLocked = atPhotoCap && !isPremium,
                )
            }

            if (atPhotoCap && !isPremium) {
                item {
                    // Inline cap notice. Kept deliberately small / non-modal so
                    // the user can still log text-only gratitude without
                    // interruption — the paywall opens only if they tap
                    // "Add Photo" or the upgrade CTA here.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Monthly photo limit reached (${PremiumFeatures.FREE_GRATITUDE_PHOTOS_PER_MONTH}). Keep logging without photos, or upgrade for unlimited.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onNavigateToPaywall) {
                            Text("Upgrade", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Save button
        Button(
            onClick = {
                if (gratitudeTexts.take(entryCount).all { it.isNotBlank() }) {
                    isLoading = true
                    viewModel.logGratitudes(
                        entries = gratitudeTexts.take(entryCount),
                        categories = selectedCategories.take(entryCount),
                        hasPhoto = hasPhotos,
                        onComplete = { xpEarned ->
                            isLoading = false
                            onGratitudeSaved(xpEarned)
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            enabled = !isLoading && gratitudeTexts.take(entryCount).all { it.isNotBlank() }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save & Earn XP", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun GratitudeEntryField(
    index: Int,
    text: String,
    onTextChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onPhotoClick: () -> Unit,
    photoLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Gratitude #${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            // Text input with voice icon
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What are you grateful for?") },
                minLines = 2,
                maxLines = 3,
                trailingIcon = {
                    IconButton(onClick = { /* Voice to text placeholder */ }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice to text",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedCategory,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GratitudeEntry.ALL_CATEGORIES.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onCategoryChange(category)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Photo picker button — swaps to a lock icon + "Upgrade" label
            // once the free monthly photo cap is hit. Tapping it still opens
            // the paywall (the Screen's onPhotoClick branches on the same
            // condition), so users always have a path to unlock.
            OutlinedButton(
                onClick = onPhotoClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (photoLocked) Icons.Default.Lock else Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = if (photoLocked) "Add Photo (Premium)" else "Add Photo (Optional)",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

class GratitudeLogViewModel(
    private val gratitudeRepository: GratitudeRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _photoCountThisMonth = MutableStateFlow(0)

    /**
     * How many gratitude-photo entries the user has logged this calendar
     * month. Compared against
     * [com.prayerquest.app.billing.PremiumFeatures.FREE_GRATITUDE_PHOTOS_PER_MONTH]
     * by the Screen to decide whether "Add Photo" attaches a photo or bumps
     * the user into the paywall.
     *
     * Loaded once in `init` — fresh every time this VM is created, which
     * happens on every Screen entry since the VM isn't retained across nav.
     */
    val photoCountThisMonth: StateFlow<Int> = _photoCountThisMonth.asStateFlow()

    init {
        viewModelScope.launch {
            val ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            _photoCountThisMonth.value = gratitudeRepository.getPhotoCountForMonth(ym)
        }
    }

    fun logGratitudes(
        entries: List<String>,
        categories: List<String>,
        hasPhoto: Boolean,
        onComplete: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val entryObjects = entries.mapIndexed { index, text ->
                GratitudeEntry(
                    date = today,
                    text = text,
                    category = if (index < categories.size) categories[index] else GratitudeEntry.CATEGORY_OTHER,
                    photoUri = if (hasPhoto) "placeholder_photo_uri" else null
                )
            }
            gratitudeRepository.addAll(entryObjects)
            val xpEarned = gamificationRepository.onGratitudeLogged(entries.size, hasPhoto)
            onComplete(xpEarned)
        }
    }

    class Factory(
        private val gratitudeRepository: GratitudeRepository,
        private val gamificationRepository: GamificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GratitudeLogViewModel(gratitudeRepository, gamificationRepository) as T
        }
    }
}
