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
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

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
            title = { Text(stringResource(R.string.groups_join_group)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                    text = stringResource(R.string.groups_join_a_prayer_group),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.groups_share_prayers_with_your_faith_community),
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
                    text = stringResource(R.string.groups_enter_invite_code),
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
                    placeholder = { Text(stringResource(R.string.groups_e_g_pray_abc123)) },
                    prefix = { Text(stringResource(R.string.groups_pray)) },
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
                            // Pass raw 6-char share code (not "PRAY-XXXXXX") to
                            // match Firestore's exact-match lookup. Also avoids
                            // calling @Composable stringResource from a plain lambda.
                            viewModel.joinGroup(inviteCode) { success, message ->
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
                    text = stringResource(R.string.groups_ask_a_group_admin_for_the_invite_code),
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

            // Resolve at @Composable level — onClick is plain `() -> Unit`.
            // (Also: previously this wrapped the invite code in a "Pray for X"
            // sentence which would have broken Firestore's exact-match lookup.
            // The first arg to joinGroup is the raw 6-char share code.)
            val invalidCodeMessage = stringResource(R.string.groups_please_enter_a_valid_6_character_code)
            // Join button
            Button(
                onClick = {
                    if (inviteCode.length == 6) {
                        isLoading = true
                        errorMessage = ""
                        successMessage = ""
                        viewModel.joinGroup(inviteCode) { success, message ->
                            isLoading = false
                            if (success) {
                                successMessage = message
                                onGroupJoined()
                            } else {
                                errorMessage = message
                            }
                        }
                    } else {
                        errorMessage = invalidCodeMessage
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
                    Text(stringResource(R.string.groups_join_group), style = MaterialTheme.typography.labelLarge)
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
                        text = stringResource(R.string.groups_how_it_works),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.groups_groups_are_invite_only_for_privacy_and_community),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.groups_share_group_prayers_with_all_members),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.groups_see_how_many_members_have_prayed_for_each_request),
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
        // NOTE: Strings in this ViewModel coroutine can't use @Composable
        // stringResource(). Localization-pending — see LOCALIZATION_AUDIT.md
        // (ViewModel strings need Context.getString via Application ref).
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
