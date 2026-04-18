package com.prayerquest.app.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.billing.PremiumFeatures
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.CollectionRepository
import com.prayerquest.app.data.repository.PhotoCountRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.domain.tagging.PrayerTagger
import com.prayerquest.app.ui.components.PhotoPickerSlot
import com.prayerquest.app.util.PhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Screen for adding prayer items into an existing [com.prayerquest.app.data.entity.PrayerCollection].
 *
 * Two entry paths in one screen so a brand-new user with zero items doesn't
 * hit a dead-end empty state:
 *  1. Quick-create — title (required) + optional description, saved as a new
 *     PrayerItem AND immediately linked to the collection's cross-ref table.
 *  2. Pick existing — toggle list of the user's active prayer items that
 *     aren't already in the collection. Multi-select, one "Add Selected"
 *     commit at the bottom.
 *
 * Both paths go through [CollectionRepository.addItemToCollection], which
 * also refreshes the denormalized itemCount on the collection row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemsToCollectionScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: AddItemsViewModel = viewModel(
        key = "add_items_to_collection_$collectionId",
        factory = AddItemsViewModel.Factory(
            collectionId = collectionId,
            collectionRepository = app.container.collectionRepository,
            prayerRepository = app.container.prayerRepository,
            photoCountRepository = app.container.photoCountRepository,
        )
    )

    val addableItems by viewModel.addableItems.collectAsState(initial = emptyList())
    val selectedIds by viewModel.selectedIds.collectAsState()
    val suggestedTags by viewModel.suggestedTags.collectAsState()
    val isPremium by app.container.premiumRepository.isPremium
        .collectAsState(initial = false)
    val photoCount by app.container.photoCountRepository.count.collectAsState()
    val canAddPhoto = PremiumFeatures.canAddPhoto(isPremium, photoCount)

    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    // Draft photo path for the quick-create card. We stage it here and flush
    // into the new PrayerItem row in `createAndAdd`.
    var draftPhotoPath by remember { mutableStateOf<String?>(null) }

    // Which suggested category the user has tapped to accept. `null` means
    // no suggestion has been accepted — if they save in that state the
    // PrayerItem is persisted with an empty category (§3.5.1: "accept or
    // dismiss with a tap"). Manual category pickers, when one is added to
    // this flow, take precedence over this field.
    var acceptedCategory by remember { mutableStateOf<PrayerTagger.Category?>(null) }

    // Feed the VM draft state so its debounced suggestion flow can run. We
    // do this in a LaunchedEffect keyed on the inputs so the VM receives one
    // update per keystroke, not one per recomposition.
    LaunchedEffect(newTitle, newDescription) {
        viewModel.onDraftChanged(newTitle, newDescription)
    }

    // If the user clears the title, the suggestion row disappears — clear
    // any previously accepted tag so a stale category can't leak into the
    // next new prayer item they type.
    LaunchedEffect(newTitle, newDescription) {
        if (newTitle.isBlank() && newDescription.isBlank()) {
            acceptedCategory = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_add_prayers)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            // imePadding keeps the "Add Selected" bottom button above the
            // soft keyboard when the user is typing a new prayer title.
            .imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // ── Quick-create card ──────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.collections_add_a_new_prayer),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            // Title row with photo slot on the right. The
                            // circular slot is a first-class part of the
                            // add flow so it stays discoverable — DD §3.9
                            // puts it right next to the prayer copy.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    label = { Text(stringResource(R.string.collections_prayer_title)) },
                                    placeholder = { Text(stringResource(R.string.collections_e_g_healing_for_mom)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                PhotoPickerSlot(
                                    currentPath = draftPhotoPath,
                                    category = PhotoStorage.Category.PRAYER_ITEM,
                                    onPhotoSaved = { path ->
                                        // New file on disk — drop the
                                        // old draft photo if one was
                                        // staged, then accept the new
                                        // path into the draft.
                                        val previous = draftPhotoPath
                                        draftPhotoPath = path
                                        if (!previous.isNullOrBlank() && previous != path) {
                                            PhotoStorage.deletePhoto(previous)
                                        }
                                        viewModel.onPhotoAdded()
                                    },
                                    onPhotoCleared = {
                                        val previous = draftPhotoPath
                                        draftPhotoPath = null
                                        if (!previous.isNullOrBlank()) {
                                            PhotoStorage.deletePhoto(previous)
                                            viewModel.onPhotoRemoved()
                                        }
                                    },
                                    onLockedTap = if (!canAddPhoto && draftPhotoPath == null)
                                        onNavigateToPaywall
                                    else null,
                                )
                            }
                            OutlinedTextField(
                                value = newDescription,
                                onValueChange = { newDescription = it },
                                label = { Text(stringResource(R.string.collections_description_optional)) },
                                placeholder = { Text(stringResource(R.string.collections_any_context_or_scripture_to_pray_into)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp)
                            )

                            // ── Auto-suggested tag chips (DD §3.5.1) ─────
                            // Only render the row when the heuristic has
                            // produced something — hiding an empty row
                            // keeps the card tidy while the user is still
                            // typing their first word.
                            if (suggestedTags.isNotEmpty()) {
                                SuggestedTagRow(
                                    suggestions = suggestedTags,
                                    accepted = acceptedCategory,
                                    onToggle = { category ->
                                        acceptedCategory =
                                            if (acceptedCategory == category) null
                                            else category
                                    }
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.createAndAdd(
                                        title = newTitle.trim(),
                                        description = newDescription.trim(),
                                        category = acceptedCategory?.displayName.orEmpty(),
                                        photoUri = draftPhotoPath,
                                    )
                                    newTitle = ""
                                    newDescription = ""
                                    acceptedCategory = null
                                    // Ownership of the staged file transfers
                                    // to the new PrayerItem row — don't
                                    // delete it here.
                                    draftPhotoPath = null
                                },
                                enabled = newTitle.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.collections_create_add))
                            }
                        }
                    }
                }

                // ── Separator between create / pick ────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.collections_or_pick_existing),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }

                // ── Existing items list ────────────────────────────────────
                if (addableItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.collections_no_other_prayers_yet_create_one_above),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                } else {
                    items(addableItems, key = { it.id }) { item ->
                        AddableItemRow(
                            item = item,
                            checked = item.id in selectedIds,
                            onToggle = { viewModel.toggleSelection(item.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }

            // ── Bottom action bar — "Add Selected" ────────────────────────
            if (addableItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(stringResource(R.string.common_done))
                    }
                    Button(
                        onClick = {
                            viewModel.addSelected()
                            onNavigateBack()
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (selectedIds.isEmpty()) stringResource(R.string.collections_add_selected)
                            else stringResource(R.string.collections_add_x_selected, selectedIds.size)
                        )
                    }
                }
            } else {
                // No existing items — only the Done button is useful, the
                // user creates via the top card and returns.
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.common_done))
                }
            }
        }
    }
}

/**
 * Horizontal row of up to 3 [FilterChip]s bound to [PrayerTagger] output.
 *
 * Selection is single-choice: accepting chip A while chip B is already
 * accepted swaps the selection rather than accumulating — this keeps the
 * "first accepted tag" rule in [AddItemsViewModel.createAndAdd] unambiguous
 * for the user. A second tap on the currently-accepted chip clears it.
 *
 * Accessibility: each chip publishes "Suggested tag: <Name>. Tap to apply."
 * (or "Tap to dismiss." when accepted) so TalkBack users know the chip is
 * a recommendation, not a selected filter.
 */
@Composable
private fun SuggestedTagRow(
    suggestions: List<PrayerTagger.SuggestedTag>,
    accepted: PrayerTagger.Category?,
    onToggle: (PrayerTagger.Category) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.collections_suggested_tags),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { tag ->
                val isAccepted = accepted == tag.category
                val a11y = if (isAccepted) {
                    stringResource(R.string.collections_suggested_tag_x_tap_to_dismiss, tag.category.displayName)
                } else {
                    stringResource(R.string.collections_suggested_tag_x_tap_to_apply, tag.category.displayName)
                }
                FilterChip(
                    selected = isAccepted,
                    onClick = { onToggle(tag.category) },
                    label = { Text(tag.category.displayName) },
                    modifier = Modifier.semantics { contentDescription = a11y },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
    }
}

@Composable
private fun AddableItemRow(
    item: PrayerItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

private const val TAG_SUGGESTION_DEBOUNCE_MS = 300L

class AddItemsViewModel(
    private val collectionId: Long,
    private val collectionRepository: CollectionRepository,
    private val prayerRepository: PrayerRepository,
    private val photoCountRepository: PhotoCountRepository,
) : ViewModel() {

    /**
     * Active prayer items the user could add — everything active that isn't
     * already in this collection. Recomputed reactively as both lists change
     * (e.g., after `createAndAdd` inserts + links a new item, the new row
     * will not appear here because it's now in the collection).
     */
    val addableItems: StateFlow<List<PrayerItem>> = combine(
        prayerRepository.observeActiveItems(),
        collectionRepository.observeItemsForCollection(collectionId)
    ) { active, inCollection ->
        val inIds = inCollection.map { it.id }.toSet()
        active.filter { it.id !in inIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    // ── Draft state for the quick-create card ─────────────────────────────
    // The title + description the user is currently typing, fed by the
    // composable via [onDraftChanged]. We debounce them for 300 ms before
    // running the regex heuristic so we don't re-tag on every keystroke —
    // PrayerTagger is cheap, but 300 ms "feels" like the suggestion reacts
    // to what the user just finished saying rather than every key.
    private val _draftTitle = MutableStateFlow("")
    private val _draftDescription = MutableStateFlow("")

    val suggestedTags: StateFlow<List<PrayerTagger.SuggestedTag>> = combine(
        _draftTitle,
        _draftDescription
    ) { t, d -> t to d }
        .debounce(TAG_SUGGESTION_DEBOUNCE_MS)
        .map { (title, desc) -> PrayerTagger.suggest(title, desc) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun onDraftChanged(title: String, description: String) {
        _draftTitle.value = title
        _draftDescription.value = description
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    /**
     * Quick-create path. Insert the new PrayerItem, then link it to the
     * collection. Done in one viewModelScope.launch so both succeed or fail
     * atomically from the UI's perspective.
     *
     * [category] is the display name of the tag the user accepted from the
     * auto-suggestion row (or empty string if none accepted / a future
     * manual picker passed through nothing). PrayerItem.category stays as
     * the column's empty-string default when no category is chosen — no
     * Room migration required.
     */
    fun createAndAdd(
        title: String,
        description: String,
        category: String = "",
        photoUri: String? = null,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newId = prayerRepository.addItem(
                PrayerItem(
                    title = title,
                    description = description,
                    category = category,
                    photoUri = photoUri,
                )
            )
            collectionRepository.addItemToCollection(collectionId, newId)
            // Reset the draft so the suggestion row doesn't linger with
            // stale tags keyed to the prayer we just saved.
            _draftTitle.value = ""
            _draftDescription.value = ""
            // Refresh the on-disk photo count so the 200-cap check stays
            // accurate after the new row lands.
            if (photoUri != null) photoCountRepository.refresh()
        }
    }

    /** Called after the photo-picker successfully wrote a new file to disk. */
    fun onPhotoAdded() {
        viewModelScope.launch { photoCountRepository.refresh() }
    }

    /** Called after the user cleared a staged photo (file already deleted). */
    fun onPhotoRemoved() {
        viewModelScope.launch { photoCountRepository.refresh() }
    }

    /**
     * Multi-select commit. Iterates rather than batching because
     * addItemToCollection also refreshes the itemCount — doing it per-item
     * keeps the denormalized count correct without a separate pass.
     */
    fun addSelected() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                collectionRepository.addItemToCollection(collectionId, id)
            }
            _selectedIds.value = emptySet()
        }
    }

    class Factory(
        private val collectionId: Long,
        private val collectionRepository: CollectionRepository,
        private val prayerRepository: PrayerRepository,
        private val photoCountRepository: PhotoCountRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddItemsViewModel(
                collectionId = collectionId,
                collectionRepository = collectionRepository,
                prayerRepository = prayerRepository,
                photoCountRepository = photoCountRepository,
            ) as T
        }
    }
}
