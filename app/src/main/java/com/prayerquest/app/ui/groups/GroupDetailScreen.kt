package com.prayerquest.app.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
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
import com.prayerquest.app.data.entity.PrayerGroupMember
import com.prayerquest.app.data.entity.GroupPrayerItem
import com.prayerquest.app.data.repository.PrayerGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    onNavigateBack: () -> Unit,
    onAddPrayer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    // The VM holds a groupId-scoped Flow graph, so key it on groupId — if the
    // user navigates to a different group the VM is recreated with the right
    // groupId instead of reusing the old one's Flows. Factory now takes the
    // groupId directly: the previous implementation read it from an empty
    // SavedStateHandle and always got 0L, which is why observeGroup(0L)
    // returned null forever and the screen sat on the loading spinner.
    val viewModel: GroupDetailViewModel = viewModel(
        key = "group_detail_$groupId",
        factory = GroupDetailViewModel.Factory(groupId, app.container.prayerGroupRepository)
    )

    val group by viewModel.group.collectAsState(initial = null)
    val prayerItems by viewModel.prayerItems.collectAsState(initial = emptyList())
    val members by viewModel.members.collectAsState(initial = emptyList())
    val weeklyCounts by viewModel.weeklyCounts.collectAsState(initial = emptyMap())
    // `isCreator` is computed fresh once `group` is loaded — the repo owns
    // the identity check so the UI doesn't duplicate "compare to currentUserId"
    // logic and risk drift.
    val isCreator = remember(group) {
        group?.let { viewModel.isGroupCreator(it) } ?: false
    }
    var showShareCodeCopied by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (group != null) {
        Column(modifier = modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = { Text("Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share placeholder */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
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
                    // Group header
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group!!.emoji,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = group!!.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Text(
                                text = "${members.size}",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (group!!.description.isNotEmpty()) {
                            Text(
                                text = group!!.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    // Share code section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Invite Others",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Share Code",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = group!!.shareCode,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.copyShareCode(group!!.shareCode)
                                        showShareCodeCopied = true
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                        if (showShareCodeCopied) {
                            Text(
                                text = "Share code copied!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Shared prayers section
                item {
                    Text(
                        text = "Group Prayers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (prayerItems.isEmpty()) {
                    item {
                        Text(
                            text = "No prayers shared yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(prayerItems) { prayerItem ->
                        GroupPrayerItemCard(
                            prayerItem = prayerItem,
                            weeklyPrayerCount = weeklyCounts[prayerItem.id] ?: 0,
                            onPrayedClick = { viewModel.markPrayed(it) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Action buttons.
            //
            // Non-creators see [Add Prayer] + [Leave Group].
            // Creators see [Add Prayer] + [Leave] + [Delete] — the delete
            // path tears down the group for ALL members and is clearly
            // marked destructive (error-colored icon button to keep the
            // hit target small so it isn't tapped by accident). Leaving
            // your own group is still a valid action; e.g. the creator
            // wants out but trusts another admin to keep it running.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddPrayer(groupId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Add Prayer")
                }
                Button(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isCreator) "Leave" else "Leave Group")
                }
                if (isCreator) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .width(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete group",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Leave group dialog
        if (showLeaveDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveDialog = false },
                title = { Text("Leave Group?") },
                text = { Text("You'll no longer see prayers from \"${group!!.name}\"") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.leaveGroup(groupId)
                            onNavigateBack()
                            showLeaveDialog = false
                        }
                    ) {
                        Text("Leave")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLeaveDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete group dialog (creator only). Separate from Leave because
        // the copy + consequence are very different: Leave only affects
        // me, Delete affects everyone. Keeping these as distinct dialogs
        // with distinct copy reduces the chance of a mis-tap.
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Group?") },
                text = {
                    Text(
                        "This will permanently delete \"${group!!.name}\" and " +
                            "remove every member, prayer, and piece of activity. " +
                            "This can't be undone."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteGroupAsCreator(groupId)
                            onNavigateBack()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
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
private fun GroupPrayerItemCard(
    prayerItem: GroupPrayerItem,
    weeklyPrayerCount: Int,
    onPrayedClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = prayerItem.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (prayerItem.description.isNotEmpty()) {
                Text(
                    text = prayerItem.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Prayed by ${prayerItem.prayedByCount} members",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Positive-signal-only: only show the weekly chip when there's
                    // actual recent activity. No "stale" nudge per product decision.
                    if (weeklyPrayerCount > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "\uD83D\uDE4F $weeklyPrayerCount this week",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { onPrayedClick(prayerItem.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "I Prayed This",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

class GroupDetailViewModel(
    private val groupId: Long,
    private val groupRepository: PrayerGroupRepository
) : ViewModel() {

    val group: Flow<PrayerGroup?> = groupRepository.observeGroup(groupId)
    val prayerItems: Flow<List<GroupPrayerItem>> =
        groupRepository.observeGroupPrayerItemDetails(groupId)
    val members: Flow<List<PrayerGroupMember>> = groupRepository.observeMembers(groupId)

    /**
     * Map of groupPrayerItemId → number of times the item was prayed for in
     * the last 7 days. UI reads this to render the "prayed this week" chip.
     * Items with 0 (or missing) count simply omit the chip — positive signal only.
     */
    val weeklyCounts: Flow<Map<Long, Int>> =
        groupRepository.observeWeeklyCountsForGroup(groupId)

    fun markPrayed(itemId: Long) {
        viewModelScope.launch {
            groupRepository.markPrayedForGroupItem(itemId)
        }
    }

    fun leaveGroup(groupId: Long) {
        viewModelScope.launch {
            groupRepository.leaveGroup(groupId)
        }
    }

    /**
     * Exposes the repo's creator check to the Compose layer so the UI
     * can conditionally show the destructive "Delete Group" action.
     */
    fun isGroupCreator(group: PrayerGroup): Boolean =
        groupRepository.isGroupCreator(group)

    /**
     * Fire the full "tear down this group for everyone" cascade. The
     * screen navigates back immediately after this returns so the user
     * doesn't stare at a group that's mid-delete.
     */
    fun deleteGroupAsCreator(groupId: Long) {
        viewModelScope.launch {
            groupRepository.deleteGroupAsCreator(groupId)
        }
    }

    fun copyShareCode(code: String) {
        // Clipboard copy implementation
    }

    class Factory(
        private val groupId: Long,
        private val groupRepository: PrayerGroupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupDetailViewModel(groupId, groupRepository) as T
        }
    }
}
