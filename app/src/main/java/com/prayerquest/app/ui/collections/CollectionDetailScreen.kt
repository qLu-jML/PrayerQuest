package com.prayerquest.app.ui.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.CollectionRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.ui.components.PrayerPhotoAvatar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAddItems: (Long) -> Unit,
    onStartPraying: () -> Unit,
    onEditItem: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    // Key the VM by collectionId so navigating between two different
    // collections gives us a fresh VM instance scoped to the new id instead
    // of reusing the previous one (which would keep emitting the old row).
    val viewModel: CollectionDetailViewModel = viewModel(
        key = "collection_detail_$collectionId",
        factory = CollectionDetailViewModel.Factory(
            collectionId = collectionId,
            collectionRepository = app.container.collectionRepository,
            prayerRepository = app.container.prayerRepository
        )
    )
    val collection by viewModel.collection.collectAsState(initial = null)
    val items by viewModel.items.collectAsState(initial = emptyList())

    if (collection != null) {
        var showDeleteDialog by remember { mutableStateOf(false) }
        var itemToDelete by remember { mutableStateOf<PrayerItem?>(null) }

        Column(modifier = modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = { Text("Collection") },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Collection header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = collection!!.emoji,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = collection!!.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${collection!!.itemCount} prayers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (collection!!.description.isNotEmpty()) {
                            Text(
                                text = collection!!.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (collection!!.topicTag.isNotEmpty()) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        collection!!.topicTag,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Label,
                                        contentDescription = "Tag",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .height(28.dp)
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (items.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No Prayers Yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Add your first prayer to this collection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } else {
                    items(items) { item ->
                        CollectionItemCard(
                            item = item,
                            onRemoveClick = {
                                itemToDelete = item
                                showDeleteDialog = true
                            },
                            onEditClick = { onEditItem(item.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onNavigateToAddItems(collectionId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Items")
                }
                Button(
                    onClick = { onStartPraying() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Start Praying")
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog && itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Remove Prayer?") },
                text = { Text("Remove \"${itemToDelete!!.title}\" from this collection?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeItem(itemToDelete!!.id, collectionId)
                            itemToDelete = null
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun CollectionItemCard(
    item: PrayerItem,
    onRemoveClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val createdDate = dateFormat.format(Date(item.createdAt))

    // Tapping the card opens the Edit screen (photo + fields). The trash
    // icon still does its own thing and swallows the tap via IconButton's
    // own click handler, so the two actions don't conflict.
    ElevatedCard(
        onClick = onEditClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo avatar (DD §3.9) — falls back to a monogram when the
            // item has no photo, so every card still has a visual anchor.
            PrayerPhotoAvatar(
                photoPath = item.photoUri,
                fallbackLabel = item.title,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = "Added $createdDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

class CollectionDetailViewModel(
    private val collectionId: Long,
    private val collectionRepository: CollectionRepository,
    private val prayerRepository: PrayerRepository
) : ViewModel() {

    /**
     * Live-observed collection row. Was previously [emptyFlow] which never
     * emitted — leaving the screen stuck on its loading spinner. Now bound to
     * the repository so the header renders as soon as Room returns the row.
     */
    val collection: Flow<PrayerCollection?> =
        collectionRepository.observeById(collectionId)

    /**
     * Live-observed items joined through the collection cross-ref table.
     */
    val items: Flow<List<PrayerItem>> =
        collectionRepository.observeItemsForCollection(collectionId)

    fun removeItem(itemId: Long, collectionId: Long) {
        viewModelScope.launch {
            collectionRepository.removeItemFromCollection(collectionId, itemId)
        }
    }

    class Factory(
        private val collectionId: Long,
        private val collectionRepository: CollectionRepository,
        private val prayerRepository: PrayerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CollectionDetailViewModel(
                collectionId = collectionId,
                collectionRepository = collectionRepository,
                prayerRepository = prayerRepository
            ) as T
        }
    }
}
