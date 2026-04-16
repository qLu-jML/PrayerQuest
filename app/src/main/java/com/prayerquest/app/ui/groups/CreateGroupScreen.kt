package com.prayerquest.app.ui.groups

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
import com.prayerquest.app.data.repository.PrayerGroupRepository
import kotlinx.coroutines.launch

@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: CreateGroupViewModel = viewModel(
        factory = CreateGroupViewModel.Factory(app.container.prayerGroupRepository)
    )

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🙏") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp),
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
                                items(COMMUNITY_EMOJI_OPTIONS) { emoji ->
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

        // Create button
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    isLoading = true
                    viewModel.createGroup(name, description, selectedEmoji) {
                        isLoading = false
                        onGroupCreated()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            enabled = name.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Group", style = MaterialTheme.typography.labelLarge)
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

class CreateGroupViewModel(
    private val groupRepository: PrayerGroupRepository
) : ViewModel() {

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
