package com.prayerquest.app.ui.gratitude

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.ui.theme.GratitudeGreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GratitudeCatalogueScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: GratitudeCatalogueViewModel = viewModel(
        factory = GratitudeCatalogueViewModel.Factory(app.container.gratitudeRepository)
    )
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPhotosOnly by remember { mutableStateOf(false) }

    val allEntries by viewModel.allEntries.collectAsState(initial = emptyList())

    val filteredEntries = allEntries.filter { entry ->
        val matchesSearch = searchQuery.isEmpty() || entry.text.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || entry.category == selectedCategory
        val matchesPhotoFilter = !showPhotosOnly || !entry.photoUri.isNullOrEmpty()
        matchesSearch && matchesCategory && matchesPhotoFilter
    }.sortedByDescending { it.timestamp }

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Gratitude Catalogue") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Search and filter section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search gratitude...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {})
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showPhotosOnly,
                    onClick = { showPhotosOnly = !showPhotosOnly },
                    label = { Text("📸 With Photos", style = MaterialTheme.typography.labelSmall) }
                )
                listOf(null) + GratitudeEntry.ALL_CATEGORIES.distinct().take(3).map { it as String? }.toList() + (if (GratitudeEntry.ALL_CATEGORIES.size > 3) "All" else null).let { if (it != null) listOf(it) else emptyList() }.forEach { category ->
                    val label = category ?: "All"
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Entries list
        if (filteredEntries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Gratitude Entries Yet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Start logging daily gratitude to see them here",
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
                items(filteredEntries) { entry ->
                    GratitudeCatalogueEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun GratitudeCatalogueEntryCard(
    entry: GratitudeEntry,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val displayDate = dateFormat.format(Date(entry.timestamp))

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
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

class GratitudeCatalogueViewModel(
    gratitudeRepository: GratitudeRepository
) : ViewModel() {

    val allEntries = gratitudeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(
        private val gratitudeRepository: GratitudeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GratitudeCatalogueViewModel(gratitudeRepository) as T
        }
    }
}
