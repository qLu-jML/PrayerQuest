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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.CollectionRepository
import com.prayerquest.app.data.repository.PrayerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: AddItemsViewModel = viewModel(
        key = "add_items_to_collection_$collectionId",
        factory = AddItemsViewModel.Factory(
            collectionId = collectionId,
            collectionRepository = app.container.collectionRepository,
            prayerRepository = app.container.prayerRepository
        )
    )

    val addableItems by viewModel.addableItems.collectAsState(initial = emptyList())
    val selectedIds by viewModel.selectedIds.collectAsState()

    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Prayers") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                                text = "Add a new prayer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Prayer title") },
                                placeholder = { Text("e.g., Healing for Mom") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newDescription,
                                onValueChange = { newDescription = it },
                                label = { Text("Description (optional)") },
                                placeholder = { Text("Any context or Scripture to pray into…") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 80.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.createAndAdd(newTitle.trim(), newDescription.trim())
                                    newTitle = ""
                                    newDescription = ""
                                },
                                enabled = newTitle.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create & Add")
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
                            text = "  or pick existing  ",
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
                                text = "No other prayers yet — create one above.",
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
                        Text("Done")
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
                            text = if (selectedIds.isEmpty()) "Add Selected"
                            else "Add ${selectedIds.size} Selected"
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
                    Text("Done")
                }
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

class AddItemsViewModel(
    private val collectionId: Long,
    private val collectionRepository: CollectionRepository,
    private val prayerRepository: PrayerRepository
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

    fun toggleSelection(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    /**
     * Quick-create path. Insert the new PrayerItem, then link it to the
     * collection. Done in one viewModelScope.launch so both succeed or fail
     * atomically from the UI's perspective.
     */
    fun createAndAdd(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val newId = prayerRepository.addItem(
                PrayerItem(title = title, description = description)
            )
            collectionRepository.addItemToCollection(collectionId, newId)
        }
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
        private val prayerRepository: PrayerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddItemsViewModel(
                collectionId = collectionId,
                collectionRepository = collectionRepository,
                prayerRepository = prayerRepository
            ) as T
        }
    }
}
