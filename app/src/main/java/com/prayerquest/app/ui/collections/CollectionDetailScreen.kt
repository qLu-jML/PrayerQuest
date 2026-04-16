package com.prayerquest.app.ui.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToAddItems: (Long) -> Unit,
    onStartPraying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: CollectionDetailViewModel = viewModel(
        factory = CollectionDetailViewModel.Factory(
            app.container.collectionRepository,
            app.container.prayerRepository
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
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .height(28.dp)
                            )
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
                            }
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
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val createdDate = dateFormat.format(Date(item.createdAt))

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    private val collectionRepository: CollectionRepository,
    private val prayerRepository: PrayerRepository
) : ViewModel() {

    val collection: Flow<PrayerCollection?> = emptyFlow()
    val items: Flow<List<PrayerItem>> = emptyFlow()

    fun removeItem(itemId: Long, collectionId: Long) {
        viewModelScope.launch {
            collectionRepository.removeItemFromCollection(collectionId, itemId)
        }
    }

    class Factory(
        private val collectionRepository: CollectionRepository,
        private val prayerRepository: PrayerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CollectionDetailViewModel(collectionRepository, prayerRepository) as T
        }
    }
}
