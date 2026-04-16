package com.prayerquest.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.FamousPrayer
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.ui.library.LibraryViewModel
import com.prayerquest.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(
    onNavigateToCollectionDetail: (Long) -> Unit,
    onNavigateToFamousPrayerDetail: (String) -> Unit,
    onNavigateToAnsweredPrayerDetail: (Long) -> Unit,
    onNavigateToCreateCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(
            app.container.collectionRepository,
            app.container.prayerRepository,
            app.container.gratitudeRepository
        )
    )
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("My Collections") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Famous Prayers") }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Answered") }
            )
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { Text("Gratitude") }
            )
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> MyCollectionsTab(
                onNavigateToDetail = onNavigateToCollectionDetail,
                onNavigateToCreate = onNavigateToCreateCollection,
                modifier = Modifier.fillMaxSize(),
                viewModel = libraryViewModel
            )
            1 -> FamousPrayersTab(
                onNavigateToDetail = onNavigateToFamousPrayerDetail,
                modifier = Modifier.fillMaxSize(),
                viewModel = libraryViewModel
            )
            2 -> AnsweredPrayersTab(
                onNavigateToDetail = onNavigateToAnsweredPrayerDetail,
                modifier = Modifier.fillMaxSize(),
                viewModel = libraryViewModel
            )
            3 -> GratitudeTab(
                modifier = Modifier.fillMaxSize(),
                viewModel = libraryViewModel
            )
        }
    }
}

@Composable
private fun MyCollectionsTab(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel
) {
    val collections by viewModel.collections.collectAsState(initial = emptyList())

    Box(modifier = modifier) {
        if (collections.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Your First Collection",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Organize your prayers into collections like Family, Work, or Personal Growth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = onNavigateToCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Create Collection")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(collections) { collection ->
                    CollectionCard(
                        collection = collection,
                        onClick = { onNavigateToDetail(collection.id) }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onNavigateToCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Collection")
        }
    }
}

@Composable
private fun CollectionCard(
    collection: PrayerCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = collection.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${collection.itemCount} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            if (collection.description.isNotEmpty()) {
                Text(
                    text = collection.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (collection.topicTag.isNotEmpty()) {
                AssistChip(
                    onClick = {},
                    label = { Text(collection.topicTag, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

@Composable
private fun FamousPrayersTab(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val famousPrayers by viewModel.famousPrayers.collectAsState(initial = emptyList())

    val filteredPrayers = if (searchQuery.isEmpty()) {
        famousPrayers
    } else {
        famousPrayers.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search famous prayers...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {})
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredPrayers) { prayer ->
                FamousPrayerCard(
                    prayer = prayer,
                    onClick = { onNavigateToDetail(prayer.id) }
                )
            }
        }
    }
}

@Composable
private fun FamousPrayerCard(
    prayer: FamousPrayer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = prayer.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "by ${prayer.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (prayer.category.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(prayer.category, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Prayed ${prayer.userPrayedCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AnsweredPrayersTab(
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel
) {
    val answeredPrayers by viewModel.answeredPrayers.collectAsState(initial = emptyList())

    if (answeredPrayers.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Answered Prayers Will Appear Here",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "When you mark prayers as answered, they'll be celebrated here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(answeredPrayers) { prayer ->
                AnsweredPrayerCard(
                    prayer = prayer,
                    onClick = { onNavigateToDetail(prayer.id) }
                )
            }
        }
    }
}

@Composable
private fun AnsweredPrayerCard(
    prayer: PrayerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val answeredDate = if (prayer.answeredAt != null) {
        dateFormat.format(Date(prayer.answeredAt))
    } else {
        "Recently"
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prayer.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = "✓ Answered",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
            Text(
                text = "Answered on $answeredDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!prayer.testimonyText.isNullOrEmpty()) {
                Text(
                    text = prayer.testimonyText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun GratitudeTab(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val gratitudeEntries by viewModel.gratitudeEntries.collectAsState(initial = emptyList())

    val filteredEntries = if (searchQuery.isEmpty()) {
        gratitudeEntries
    } else {
        gratitudeEntries.filter {
            it.text.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search gratitude entries...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {})
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredEntries) { entry ->
                GratitudeEntryCard(entry = entry)
            }
        }
    }
}

@Composable
private fun GratitudeEntryCard(
    entry: GratitudeEntry,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val displayDate = dateFormat.format(Date(entry.timestamp))

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                AssistChip(
                    onClick = {},
                    label = { Text(entry.category, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            }
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            if (!entry.photoUri.isNullOrEmpty()) {
                Text(
                    text = "📸 Photo attached",
                    style = MaterialTheme.typography.labelSmall,
                    color = GratitudeGreen,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
