package com.prayerquest.app.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.repository.CollectionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCollectionScreen(
    onNavigateBack: () -> Unit,
    onCollectionCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: CreateCollectionViewModel = viewModel(
        factory = CreateCollectionViewModel.Factory(app.container.collectionRepository)
    )

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var topicTag by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🙏") }
    var showEmojis by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // imePadding lets the bottom Button slide up above the soft
            // keyboard when the tag / description / name fields are focused,
            // so "Create Collection" stays reachable instead of hiding behind
            // the IME.
            .imePadding()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Create Collection") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Emoji picker section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose an Emoji",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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

                            // Emoji grid
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(EMOJI_OPTIONS) { emoji ->
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
                                            .clickable { selectedEmoji = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, style = MaterialTheme.typography.headlineSmall)
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
                    label = { Text("Collection Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Family Prayers") },
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
                    placeholder = { Text("What's this collection about?") },
                    minLines = 3,
                    maxLines = 3
                )
            }

            item {
                // Topic tag field
                OutlinedTextField(
                    value = topicTag,
                    onValueChange = { if (it.length <= 30) topicTag = it },
                    label = { Text("Topic Tag (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Health, Relationships") },
                    singleLine = true,
                    maxLines = 1
                )
            }

            item {
                // Helper text
                Text(
                    text = "Create a collection to organize related prayers together. Name is required to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Create button — padding BEFORE height so the 16dp outer margin
        // doesn't eat into the button's 52dp tap target.
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.createCollection(name, description, topicTag, selectedEmoji)
                    onCollectionCreated()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            enabled = name.isNotBlank()
        ) {
            Text("Create Collection", style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val EMOJI_OPTIONS = listOf(
    "🙏", "❤️", "✝️", "🕊️", "⛪", "📖", "🌟", "💪",
    "🌈", "🎯", "💝", "🌱", "🔥", "👨‍👩‍👧‍👦", "💼", "🏥",
    "🎓", "🌍", "⏰", "😊", "🙌", "📿", "🕯️", "🌸",
    "💎", "🎁", "🌙", "☀️", "🌊", "🏔️"
)

class CreateCollectionViewModel(
    private val collectionRepository: CollectionRepository
) : ViewModel() {

    fun createCollection(name: String, description: String, topicTag: String, emoji: String) {
        viewModelScope.launch {
            val collection = PrayerCollection(
                name = name,
                description = description,
                topicTag = topicTag,
                emoji = emoji,
                itemCount = 0
            )
            collectionRepository.create(collection)
        }
    }

    class Factory(
        private val collectionRepository: CollectionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateCollectionViewModel(collectionRepository) as T
        }
    }
}
