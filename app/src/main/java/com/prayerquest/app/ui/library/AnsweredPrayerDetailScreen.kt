package com.prayerquest.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnsweredPrayerDetailScreen(
    prayerId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: AnsweredPrayerDetailViewModel = viewModel(
        factory = AnsweredPrayerDetailViewModel.Factory(app.container.prayerRepository)
    )

    val prayer by viewModel.prayer.collectAsState(initial = null)

    if (prayer != null && prayer!!.status == PrayerItem.STATUS_ANSWERED) {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val answeredDate = if (prayer!!.answeredAt != null) {
            dateFormat.format(Date(prayer!!.answeredAt))
        } else {
            "Recently"
        }

        Column(modifier = modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = { Text("Prayer Answered") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Prayer title and answered status
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prayer!!.title,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "✓ Answered",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                item {
                    // Timeline event
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp),
                            shape = MaterialTheme.shapes.small,
                            color = SuccessGreen
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Prayer Answered",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = answeredDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Original request details
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "What You Prayed For",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (prayer!!.description.isNotEmpty()) {
                                    Text(
                                        text = prayer!!.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Testimony
                if (!prayer!!.testimonyText.isNullOrEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Your Testimony",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = prayer!!.testimonyText!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                )
                            }
                        }
                    }
                }

                // Photo section
                if (!prayer!!.testimonyPhotoUri.isNullOrEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Evidence Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "📸 Photo: ${prayer!!.testimonyPhotoUri}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice note section
                if (!prayer!!.testimonyVoiceUri.isNullOrEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Voice Note",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.playVoiceNote() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("▶", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        "Tap to play your testimony",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    } else {
        // Loading or not found state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (prayer == null) {
                CircularProgressIndicator()
            } else {
                Text("Prayer not found or not answered")
            }
        }
    }
}

class AnsweredPrayerDetailViewModel(
    private val prayerRepository: PrayerRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val prayerId: Long = savedStateHandle["prayerId"] ?: 0L

    val prayer: Flow<PrayerItem?> = emptyFlow()

    fun playVoiceNote() {
        viewModelScope.launch {
            // Would implement voice playback here
        }
    }

    class Factory(
        private val prayerRepository: PrayerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnsweredPrayerDetailViewModel(prayerRepository) as T
        }
    }
}
