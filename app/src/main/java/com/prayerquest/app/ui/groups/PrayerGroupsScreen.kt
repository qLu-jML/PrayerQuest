package com.prayerquest.app.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.prayerquest.app.data.entity.PrayerGroup
import com.prayerquest.app.data.repository.PrayerGroupRepository
import com.prayerquest.app.ui.theme.CommunityBlue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn

@Composable
fun PrayerGroupsScreen(
    onNavigateToGroupDetail: (Long) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToJoinGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: PrayerGroupsViewModel = viewModel(
        factory = PrayerGroupsViewModel.Factory(app.container.prayerGroupRepository)
    )

    var showActionMenu by remember { mutableStateOf(false) }
    val groups by viewModel.userGroups.collectAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Prayer Groups",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connect and pray with your faith community",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
        factory = PrayerGroupsViewModel.Factory(app.container.prayerGroupRepository)
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
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "👥 $memberCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = CommunityBlue
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
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
            Text(
                text = "🙏 $prayerCount prayers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

class PrayerGroupsViewModel(
    private val groupRepository: PrayerGroupRepository
) : ViewModel() {

    val userGroups = groupRepository.observeAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getMemberCount(groupId: Long): Flow<Int> = emptyFlow()

    fun getPrayerCount(groupId: Long): Flow<Int> = emptyFlow()

    class Factory(
        private val groupRepository: PrayerGroupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrayerGroupsViewModel(groupRepository) as T
        }
    }
}
