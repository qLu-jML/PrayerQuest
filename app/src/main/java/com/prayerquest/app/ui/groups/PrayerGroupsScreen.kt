package com.prayerquest.app.ui.groups

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.ads.BannerAdView
import com.prayerquest.app.data.entity.PrayerGroup
import com.prayerquest.app.data.repository.PrayerGroupRepository
import com.prayerquest.app.firebase.AuthState
import com.prayerquest.app.firebase.FirebaseAuthManager
import com.prayerquest.app.ui.theme.CommunityBlue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerGroupsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetail: (Long) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToJoinGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: PrayerGroupsViewModel = viewModel(
        factory = PrayerGroupsViewModel.Factory(
            app.container.prayerGroupRepository,
            app.container.firebaseAuthManager
        )
    )

    var showActionMenu by remember { mutableStateOf(false) }
    val groups by viewModel.userGroups.collectAsState(initial = emptyList())
    val authState by viewModel.authState.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Prayer Groups",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect and pray with your faith community",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // Manual refresh — real-time Firestore listeners still run
                // via the application-scoped mirror, but early builds had a
                // window where the list looked empty after sign-in until
                // the first snapshot landed. The explicit Refresh button
                // gives users an escape hatch if they ever see stale state.
                actions = {
                    if (authState is AuthState.SignedIn) {
                        IconButton(onClick = { viewModel.refreshFromCloud() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh groups"
                            )
                        }
                    }
                }
            )

            // Sign-in banner (if not signed in)
            if (authState is AuthState.SignedOut) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sign in to share prayer groups",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Sign in with Google to create and join prayer groups that sync across devices and with other members.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = {
                                val intent = viewModel.getSignInIntent()
                                if (intent != null) signInLauncher.launch(intent)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Sign in with Google")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Transient info message (e.g. sign-in result). Real-time group
            // changes don't need a banner — they just appear in the list.
            // Error messages here can be long (ApiException status codes +
            // remediation hints) so let them wrap fully and give the user a
            // dismiss button instead of truncating with labelSmall.
            if (infoMessage.isNotEmpty()) {
                val isError = infoMessage.startsWith("Google Sign-In failed", ignoreCase = true) ||
                    infoMessage.startsWith("Firebase auth failed", ignoreCase = true) ||
                    infoMessage.startsWith("Sign-in failed", ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (isError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = infoMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissInfoMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp),
                                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Wrap list + empty state in a weighted Box so the banner ad
            // below claims its space without stealing from the list.
            Box(modifier = Modifier.weight(1f)) {
                if (groups.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You're Not In Any Groups Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Create a new prayer group or join an existing one to start praying with others",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onNavigateToCreateGroup,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create")
                            }
                            OutlinedButton(
                                onClick = onNavigateToJoinGroup,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text("Join")
                            }
                        }
                    }
                } else {
                    // Groups list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(groups) { group ->
                            PrayerGroupCard(
                                group = group,
                                onClick = { onNavigateToGroupDetail(group.id) }
                            )
                        }
                    }
                }
            }

            // Banner ad above the bottom nav. Self-hides for premium users.
            BannerAdView(modifier = Modifier.fillMaxWidth())
        }

        // FAB with menu
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showActionMenu) {
                SmallFloatingActionButton(
                    onClick = onNavigateToJoinGroup,
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Join Group")
                }
                SmallFloatingActionButton(
                    onClick = onNavigateToCreateGroup,
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                }
            }
            FloatingActionButton(
                onClick = { showActionMenu = !showActionMenu },
                containerColor = CommunityBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Groups Menu")
            }
        }

        // No post-sync popup: group activity now streams in live via the
        // snapshot-listener mirror in PrayerGroupRepository. When the app
        // is closed, FCM push notifications (Sprint 4) surface new activity
        // instead of a batched "you missed X prayers" dialog.
    }
}

@Composable
private fun PrayerGroupCard(
    group: PrayerGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: PrayerGroupsViewModel = viewModel(
        factory = PrayerGroupsViewModel.Factory(
            app.container.prayerGroupRepository,
            app.container.firebaseAuthManager
        )
    )

    val memberCount by viewModel.getMemberCount(group.id).collectAsState(initial = 0)
    val prayerCount by viewModel.getPrayerCount(group.id).collectAsState(initial = 0)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
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
                    text = group.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cloud sync indicator
                    if (group.firestoreId != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "synced",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "members",
                                style = MaterialTheme.typography.labelSmall,
                                color = CommunityBlue
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            if (group.description.isNotEmpty()) {
                Text(
                    text = group.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

class PrayerGroupsViewModel(
    private val groupRepository: PrayerGroupRepository,
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    val userGroups = groupRepository.observeAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authState = authManager.authState

    /**
     * Transient informational message surface — sign-in result copy lives
     * here. Not used for sync status (there is no manual sync anymore).
     */
    private val _infoMessage = MutableStateFlow("")
    val infoMessage: StateFlow<String> = _infoMessage

    // No startCloudMirror here: the cloud → Room mirror is owned by
    // AppContainer and bound to the application scope so listeners run for
    // the full app lifetime, not just while the Groups screen is on
    // screen. That fixes the "blank list after sign-in" bug where the
    // previous viewModelScope-bound mirror only started when the user
    // opened Groups and sometimes didn't land a snapshot before the
    // screen composed.

    fun getMemberCount(groupId: Long): Flow<Int> =
        groupRepository.observeMembers(groupId).map { it.size }

    fun getPrayerCount(groupId: Long): Flow<Int> =
        groupRepository.observeGroupPrayerItemDetails(groupId).map { it.size }

    fun getSignInIntent() = authManager.getSignInIntent()

    fun handleSignInResult(result: androidx.activity.result.ActivityResult) {
        viewModelScope.launch {
            val signInResult = authManager.handleSignInResult(result)
            if (signInResult.isSuccess) {
                _infoMessage.value = "Signed in as ${signInResult.getOrNull()?.displayName}"
                // No explicit sync call: the authState emission will trigger
                // startCloudMirror's collector to attach listeners automatically.
            } else {
                // FirebaseAuthManager now returns a message already prefixed
                // with "Google Sign-In failed (code X): <hint>" or "Firebase
                // auth failed: ..." — pass it through verbatim so the user
                // sees the real status code and remediation hint.
                _infoMessage.value = signInResult.exceptionOrNull()?.message
                    ?: "Sign-in failed (unknown error)."
            }
        }
    }

    /** Clears the transient info/error banner. */
    fun dismissInfoMessage() {
        _infoMessage.value = ""
    }

    /**
     * Manual refresh — one-shot pull from Firestore into Room. Backs the
     * Refresh button in the top bar. Real-time snapshot listeners are
     * still active via AppContainer; this is purely an escape hatch for
     * "I don't want to wait for the listener to reconnect."
     */
    fun refreshFromCloud() {
        viewModelScope.launch {
            val result = groupRepository.refreshFromCloud()
            _infoMessage.value = result.fold(
                onSuccess = { n ->
                    if (n == 0) "No synced groups yet — create or join one below."
                    else "Refreshed $n group${if (n == 1) "" else "s"}."
                },
                onFailure = { "Refresh failed: ${it.message ?: "unknown error"}." }
            )
        }
    }

    class Factory(
        private val groupRepository: PrayerGroupRepository,
        private val authManager: FirebaseAuthManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrayerGroupsViewModel(groupRepository, authManager) as T
        }
    }
}
