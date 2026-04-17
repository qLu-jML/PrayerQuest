package com.prayerquest.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.repository.PrayerGroupRepository
import com.prayerquest.app.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupJoined: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: JoinGroupViewModel = viewModel(
        factory = JoinGroupViewModel.Factory(app.container.prayerGroupRepository)
    )

    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Join Group") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "✨ Join a Prayer Group",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Share prayers with your faith community",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Invite code field
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter Invite Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = {
                        // Format as PRAY-XXXXXX automatically
                        val cleaned = it.uppercase().filter { c -> c.isLetterOrDigit() }
                        inviteCode = when {
                            cleaned.length <= 6 -> cleaned
                            else -> cleaned.take(6)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., PRAY-ABC123") },
                    prefix = { Text("PRAY-") },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inviteCode.length == 6) {
                            isLoading = true
                            errorMessage = ""
                            successMessage = ""
                            viewModel.joinGroup("PRAY-$inviteCode") { success, message ->
                                isLoading = false
                                if (success) {
                                    successMessage = message
                                    onGroupJoined()
                                } else {
                                    errorMessage = message
                                }
                            }
                        }
                    })
                )

                Text(
                    text = "Ask a group admin for the invite code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Error message
            if (errorMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("❌", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Success message
            if (successMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓", style = MaterialTheme.typography.headlineSmall, color = SuccessGreen)
                        Text(
                            text = successMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Join button
            Button(
                onClick = {
                    if (inviteCode.length == 6) {
                        isLoading = true
                        errorMessage = ""
                        successMessage = ""
                        viewModel.joinGroup("PRAY-$inviteCode") { success, message ->
                            isLoading = false
                            if (success) {
                                successMessage = message
                                onGroupJoined()
                            } else {
                                errorMessage = message
                            }
                        }
                    } else {
                        errorMessage = "Please enter a valid 6-character code"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = inviteCode.length == 6 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Join Group", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Info section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How It Works",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Groups are invite-only for privacy and community",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Share group prayers with all members",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• See how many members have prayed for each request",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

class JoinGroupViewModel(
    private val groupRepository: PrayerGroupRepository
) : ViewModel() {

    fun joinGroup(shareCode: String, onResult: (success: Boolean, message: String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = groupRepository.joinGroup(shareCode)
                if (result != null) {
                    onResult(true, "Successfully joined the group! Pull to refresh to see shared prayers.")
                } else {
                    onResult(false, "Invalid invite code. Make sure you're signed in and the code is correct.")
                }
            } catch (e: Exception) {
                onResult(false, "Error joining group: ${e.message ?: "Unknown error"}")
            }
        }
    }

    class Factory(
        private val groupRepository: PrayerGroupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return JoinGroupViewModel(groupRepository) as T
        }
    }
}
