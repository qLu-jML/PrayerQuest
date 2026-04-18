package com.prayerquest.app.ui.gratitude

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.dao.GratitudeDateCount
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.domain.content.CelebrationVerse
import com.prayerquest.app.domain.content.CelebrationVerses
import com.prayerquest.app.ui.theme.GratitudeGold
import com.prayerquest.app.ui.theme.GratitudeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Gratitude Catalogue (DD §3.6) — ship-quality browse/search UX for every
 * gratitude entry the user has logged. Three layered affordances:
 *   1. Calendar heat-map (year view, 53×7 grid) — collapsible, closed by
 *      default. Tapping a cell filters the list to that day.
 *   2. Debounced keyword search — 250ms off the typing thread so Room isn't
 *      thrashed on every keystroke.
 *   3. Photo-only filter chip + category chips.
 *
 * Per-entry "Share as image card" is exposed via the overflow icon on each
 * card (and a long-press gesture). The shared bitmap is rendered with
 * native Canvas to avoid Compose capture complications, then written to
 * cacheDir/shared/gratitude/<id>.png and fired via ACTION_SEND.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GratitudeCatalogueScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerQuestApplication
    val viewModel: GratitudeCatalogueViewModel = viewModel(
        factory = GratitudeCatalogueViewModel.Factory(app.container.gratitudeRepository)
    )

    val uiEntries by viewModel.visibleEntries.collectAsState(initial = emptyList())
    val dateCounts by viewModel.dateCounts.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val showPhotosOnly by viewModel.showPhotosOnly.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var heatmapExpanded by remember { mutableStateOf(false) }
    var shareSheetEntry by remember { mutableStateOf<GratitudeEntry?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Gratitude Catalogue") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // ──────────────────────────────────────────────────────────────
        // Search + filter strip
        // ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search gratitude…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {})
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Photo-only filter chip — per DD §3.6 spec.
                FilterChip(
                    selected = showPhotosOnly,
                    onClick = { viewModel.onPhotosOnlyToggle() },
                    label = {
                        Text(
                            text = "With photos only",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                // Category filter chips — null sentinel = "All".
                (listOf<String?>(null) + GratitudeEntry.ALL_CATEGORIES).forEach { category ->
                    val label = category ?: "All"
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        label = {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }

            // Active day-filter chip (shown when the user tapped a heat-map
            // cell). Keeps the filter visible + undoable once the heat-map
            // is collapsed.
            if (selectedDate != null) {
                AssistChip(
                    onClick = { viewModel.onDateFilterClear() },
                    label = {
                        Text(
                            text = "Day: ${selectedDate}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear day filter",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        // ──────────────────────────────────────────────────────────────
        // Heat-map (collapsible, closed by default)
        // ──────────────────────────────────────────────────────────────
        HeatmapSection(
            expanded = heatmapExpanded,
            onToggle = { heatmapExpanded = !heatmapExpanded },
            dateCounts = dateCounts,
            selectedDate = selectedDate,
            onDaySelected = { day -> viewModel.onDateFilterChange(day) }
        )

        // ──────────────────────────────────────────────────────────────
        // Entries list
        // ──────────────────────────────────────────────────────────────
        if (uiEntries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty() || selectedCategory != null || showPhotosOnly || selectedDate != null)
                        "No entries match those filters"
                    else
                        "No Gratitude Entries Yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (searchQuery.isNotEmpty() || selectedCategory != null || showPhotosOnly || selectedDate != null)
                        "Try clearing a filter or searching a different keyword."
                    else
                        "Start logging daily gratitude to see them here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiEntries, key = { it.id }) { entry ->
                    GratitudeCatalogueEntryCard(
                        entry = entry,
                        onShareClick = { shareSheetEntry = entry }
                    )
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // Share-as-image-card bottom sheet
    // ──────────────────────────────────────────────────────────────────
    shareSheetEntry?.let { entry ->
        ShareAsImageCardSheet(
            entry = entry,
            onDismiss = { shareSheetEntry = null },
            onShare = { verse ->
                viewModel.shareAsImageCard(context, entry, verse)
                shareSheetEntry = null
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
// Heat-map (GitHub-style 53×7 grid, year view)
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun HeatmapSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    dateCounts: List<GratitudeDateCount>,
    selectedDate: String?,
    onDaySelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Heat-map — past year",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${dateCounts.size} active day${if (dateCounts.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse heat-map" else "Expand heat-map"
            )
        }
        if (expanded) {
            Heatmap(
                dateCounts = dateCounts,
                selectedDate = selectedDate,
                onDaySelected = onDaySelected
            )
            Spacer(Modifier.height(8.dp))
            HeatmapLegend()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Heatmap(
    dateCounts: List<GratitudeDateCount>,
    selectedDate: String?,
    onDaySelected: (String) -> Unit,
) {
    // Build the 53-week × 7-day grid ending today. We anchor to Sunday as
    // column 0 (DayOfWeek.SUNDAY ordinal fixup) so the grid lines up with
    // the familiar GitHub-style layout readers already grok.
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val countByDay = remember(dateCounts) {
        dateCounts.associate { it.day to it.entryCount }
    }

    // Find the Sunday that starts the earliest week so the first column is
    // a clean Sunday — even if "one year ago" lands mid-week.
    val oneYearAgo = today.minusDays(WEEKS * 7L - 1L)
    val startSunday = oneYearAgo.let { d ->
        val shift = (d.dayOfWeek.value % 7).toLong()  // Sun=0, Mon=1, … Sat=6
        d.minusDays(shift)
    }

    // Scheme — MaterialTheme colors + GratitudeGreen-based intensity ramp
    val bgEmpty = MaterialTheme.colorScheme.surfaceVariant
    val palette = listOf(
        GratitudeGreen.copy(alpha = 0.15f),
        GratitudeGreen.copy(alpha = 0.40f),
        GratitudeGreen.copy(alpha = 0.70f),
        GratitudeGreen
    )
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // Each cell is 12dp with 2dp gaps → 14dp per column → 53 cols = 742dp.
    // That's wider than any phone; wrap in a horizontal scroll.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (week in 0 until WEEKS) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (dow in 0 until 7) {
                    val day = startSunday.plusDays((week * 7 + dow).toLong())
                    val isFuture = day.isAfter(today)
                    val key = day.format(formatter)
                    val count = if (isFuture) 0 else (countByDay[key] ?: 0)
                    val cellColor = when {
                        isFuture -> Color.Transparent
                        count == 0 -> bgEmpty
                        count == 1 -> palette[0]
                        count == 2 -> palette[1]
                        count in 3..4 -> palette[2]
                        else -> palette[3]
                    }
                    val isSelected = !isFuture && selectedDate == key
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cellColor)
                            .then(
                                if (isSelected) Modifier.border(
                                    width = 1.5.dp,
                                    color = outline,
                                    shape = RoundedCornerShape(2.dp)
                                ) else Modifier
                            )
                            .clickable(enabled = !isFuture) {
                                onDaySelected(key)
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend() {
    val bgEmpty = MaterialTheme.colorScheme.surfaceVariant
    val palette = listOf(
        bgEmpty,
        GratitudeGreen.copy(alpha = 0.15f),
        GratitudeGreen.copy(alpha = 0.40f),
        GratitudeGreen.copy(alpha = 0.70f),
        GratitudeGreen
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        palette.forEach { c ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c)
            )
        }
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val WEEKS = 53

// ══════════════════════════════════════════════════════════════════════
// Entry card
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun GratitudeCatalogueEntryCard(
    entry: GratitudeEntry,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val displayDate = dateFormat.format(Date(entry.timestamp))

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(entry.id) {
                detectTapGestures(
                    onLongPress = { onShareClick() }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(entry.category, style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.height(28.dp)
                    )
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Share as image card",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            if (!entry.photoUri.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📸", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Text(
                        text = "Photo attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = GratitudeGreen
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Share-as-image-card bottom sheet
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareAsImageCardSheet(
    entry: GratitudeEntry,
    onDismiss: () -> Unit,
    onShare: (CelebrationVerse) -> Unit
) {
    // Deterministic default verse seeded by entry id — same entry always
    // proposes the same verse on re-open, but the user can pick another.
    val defaultVerse = remember(entry.id) {
        val all = CelebrationVerses.ALL
        val i = ((entry.id % all.size) + all.size) % all.size
        all[i.toInt()]
    }
    var pickedVerse by remember(entry.id) { mutableStateOf(defaultVerse) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Share as image card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your gratitude will be rendered on a warm card with a verse overlay, date, and PrayerQuest watermark.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = GratitudeGold.copy(alpha = 0.18f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "\u201C${entry.text}\u201D",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "— ${pickedVerse.text}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pickedVerse.reference,
                        style = MaterialTheme.typography.labelSmall,
                        color = GratitudeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = "Verse overlay",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            // Horizontal strip of verse chips — feels more like a gallery
            // than a dropdown and surfaces multiple options at once.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CelebrationVerses.ALL.forEach { v ->
                    FilterChip(
                        selected = v.id == pickedVerse.id,
                        onClick = { pickedVerse = v },
                        label = {
                            Text(v.reference, style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }

            Button(
                onClick = { onShare(pickedVerse) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.IosShare, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// ViewModel
// ══════════════════════════════════════════════════════════════════════

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GratitudeCatalogueViewModel(
    private val gratitudeRepository: GratitudeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _showPhotosOnly = MutableStateFlow(false)
    val showPhotosOnly: StateFlow<Boolean> = _showPhotosOnly.asStateFlow()

    /** Day filter driven by the heat-map. Null = no day filter. */
    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    /**
     * Debounced source-of-truth Flow. When the query is blank we observe
     * the full-table Flow (cheaper + lets the client-side filters apply
     * identically). When it's non-blank we flatMap to the DAO's LIKE-based
     * searchEntries Flow, 250ms after the last keystroke.
     *
     * We only debounce *non-blank* emissions — the initial blank string
     * should resolve to the full list immediately so the screen never
     * renders "No entries match those filters" for the first 250ms after
     * open. A uniform `.debounce(250L)` on the raw flow delays the initial
     * emission too and produces that flash of empty state.
     */
    private val entriesFlow: Flow<List<GratitudeEntry>> = _searchQuery
        .debounce { q -> if (q.isBlank()) 0L else 250L }
        .flatMapLatest { q ->
            if (q.isBlank()) gratitudeRepository.observeAll()
            else gratitudeRepository.searchEntries(q.trim())
        }

    val visibleEntries: StateFlow<List<GratitudeEntry>> = combine(
        entriesFlow,
        _selectedCategory,
        _showPhotosOnly,
        _selectedDate
    ) { entries, category, photosOnly, date ->
        entries.asSequence()
            .filter { category == null || it.category == category }
            .filter { !photosOnly || !it.photoUri.isNullOrEmpty() }
            .filter { date == null || it.date == date }
            .sortedByDescending { it.timestamp }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dateCounts: StateFlow<List<GratitudeDateCount>> = gratitudeRepository
        .observeDateCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChange(q: String) {
        _searchQuery.value = q
    }

    fun onCategoryChange(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun onPhotosOnlyToggle() {
        _showPhotosOnly.value = !_showPhotosOnly.value
    }

    fun onDateFilterChange(day: String) {
        // Tapping the selected day toggles the filter off — same affordance
        // as the category chips. Prevents the "how do I clear this?" panic
        // once the heat-map is collapsed and the cell is out of sight.
        _selectedDate.value = if (_selectedDate.value == day) null else day
    }

    fun onDateFilterClear() {
        _selectedDate.value = null
    }

    /**
     * Renders a gratitude image card bitmap on a background dispatcher,
     * stashes it in cacheDir/shared/gratitude/, and fires the system share
     * sheet. Best-effort: a single I/O failure silently no-ops rather than
     * surfacing an error state we have no good UI for.
     */
    fun shareAsImageCard(
        context: Context,
        entry: GratitudeEntry,
        verse: CelebrationVerse
    ) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                renderAndWriteGratitudeBitmap(context, entry, verse)
            } ?: return@launch

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Grateful today: \"${entry.text}\" — ${verse.reference} · via PrayerQuest."
                )
                putExtra(Intent.EXTRA_SUBJECT, "A gratitude from PrayerQuest")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share your gratitude").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooser)
            } catch (_: Exception) {
                // No share targets installed → silently no-op.
            }
        }
    }

    class Factory(
        private val gratitudeRepository: GratitudeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GratitudeCatalogueViewModel(gratitudeRepository) as T
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Bitmap rendering — native Canvas, identical pattern to
// AnsweredCelebrationScreen.renderAndWriteTestimonyBitmap. Kept as a
// top-level private helper so both the screen's ViewModel and any future
// worker (e.g. scheduled weekly digest) can reuse it.
// ══════════════════════════════════════════════════════════════════════

private fun renderAndWriteGratitudeBitmap(
    context: Context,
    entry: GratitudeEntry,
    verse: CelebrationVerse
): Uri? {
    val width = 1080
    val height = 1350

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.applyCanvas {
        // 1) Warm green→gold gradient background — distinct from the
        // Celebration card (gold→rose) so gratitude shares feel like their
        // own product family.
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                android.graphics.Color.parseColor("#FFF8F0"),
                GratitudeGold.toArgb(),
                GratitudeGreen.toArgb()
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = gradient
        }
        drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2) Top label — "Grateful today"
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#2D2B29")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawText("Grateful today", width / 2f, 180f, labelPaint)

        // 3) Date line.
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val datePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#4A4743")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        drawText(dateFormat.format(Date(entry.timestamp)), width / 2f, 240f, datePaint)

        // 4) Gratitude body — quoted, centered, wrapped.
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#2D2B29")
            textSize = 58f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyText = "\u201C${entry.text}\u201D"
        val bodyLines = wrapText(bodyText, bodyPaint, width - 200)
        var y = 420f
        bodyLines.forEach { line ->
            drawText(line, width / 2f, y, bodyPaint)
            y += bodyPaint.textSize * 1.25f
        }

        // 5) Divider — thin tinted rule between gratitude and verse.
        val divPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#66FFFFFF")
            strokeWidth = 4f
        }
        y += 40f
        drawRect(
            (width / 2f) - 120f, y - 2f,
            (width / 2f) + 120f, y + 2f,
            divPaint
        )

        // 6) Verse body.
        val versePaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#2D2B29")
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        y += 80f
        val verseText = "\u201C${verse.text}\u201D"
        val verseLines = wrapText(verseText, versePaint, width - 200)
        verseLines.forEach { line ->
            drawText(line, width / 2f, y, versePaint)
            y += versePaint.textSize * 1.25f
        }

        // 7) Reference.
        val refPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#3E2723")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        y += 40f
        drawText("— ${verse.reference}", width / 2f, y, refPaint)

        // 8) Footer — PrayerQuest watermark, bottom-center.
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FFFFFF")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        drawText("PrayerQuest", width / 2f, (height - 80).toFloat(), footerPaint)
    }

    val dir = File(context.cacheDir, "shared/gratitude").apply { mkdirs() }
    val file = File(dir, "gratitude-${entry.id}.png")
    return try {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (_: Exception) {
        null
    } finally {
        bitmap.recycle()
    }
}

/**
 * Word-wrap helper — greedy pack-into-lines by pixel width. Duplicated
 * from [com.prayerquest.app.ui.celebration.AnsweredCelebrationScreen] to
 * keep the share-card renderers self-contained; both are ~20 lines and
 * the shared-abstraction overhead (a new util file) isn't worth it yet.
 */
private fun wrapText(text: String, paint: Paint, maxWidthPx: Int): List<String> {
    val words = text.split(' ')
    val lines = mutableListOf<String>()
    val bounds = Rect()
    val current = StringBuilder()
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        paint.getTextBounds(candidate, 0, candidate.length, bounds)
        if (bounds.width() > maxWidthPx && current.isNotEmpty()) {
            lines += current.toString()
            current.clear()
            current.append(word)
        } else {
            if (current.isEmpty()) current.append(word)
            else current.append(' ').append(word)
        }
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
}
