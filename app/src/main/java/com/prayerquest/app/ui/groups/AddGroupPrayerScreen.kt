package com.prayerquest.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.PrayerGroupRepository
import com.prayerquest.app.data.repository.PrayerRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Screen for adding a new prayer request to a Prayer Group.
 *
 * Flow: user types a title (required) + optional description, we create a
 * local [PrayerItem] row, then link it to the group via
 * [PrayerGroupRepository.addPrayerToGroup] which also mirrors the item to
 * Firestore when the user is signed in.
 *
 * The screen pops itself on success — the caller (NavHost) just passes an
 * `onPrayerAdded` callback that pops back to the group detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupPrayerScreen(
    groupId: Long,
    onNavigateBack: () -> Unit,
    onPrayerAdded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: AddGroupPrayerViewModel = viewModel(
        key = "add_group_prayer_$groupId",
        factory = AddGroupPrayerViewModel.Factory(
            prayerRepository = app.container.prayerRepository,
            groupRepository = app.container.prayerGroupRepository
        )
    )

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.groups_add_prayer)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.groups_share_a_prayer_request),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.groups_all_members_of_this_group_will_see_this_prayer_and),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text(stringResource(R.string.groups_prayer_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.groups_e_g_healing_for_my_mom)) },
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    label = { Text(stringResource(R.string.groups_details_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.groups_anything_the_group_should_know_as_they_pray)) },
                    minLines = 4,
                    maxLines = 6
                )
            }

            item {
                Text(
                    text = stringResource(R.string.groups_keep_personal_sensitive_details_light_everyone_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Pinned bottom action bar (matches CreateGroupScreen's pattern so it
        // stays tappable on gesture-nav devices).
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank() && !isSaving) {
                            isSaving = true
                            viewModel.addPrayer(groupId, title.trim(), description.trim()) {
                                isSaving = false
                                onPrayerAdded()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = title.isNotBlank() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.groups_add_to_group), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * Creates a local [PrayerItem] then links it to the group. We go through
 * [PrayerRepository.addItem] rather than inserting a PrayerItem inline so
 * the item shows up in the user's general library too — a group prayer is
 * also one of "your" prayer items, just additionally shared.
 */
class AddGroupPrayerViewModel(
    private val prayerRepository: PrayerRepository,
    private val groupRepository: PrayerGroupRepository
) : ViewModel() {

    fun addPrayer(
        groupId: Long,
        title: String,
        description: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            // Create the local prayer item first so it has a Room-generated id.
            val newItem = PrayerItem(
                title = title,
                description = description,
                category = "Group"
            )
            val prayerItemId = prayerRepository.addItem(newItem)

            // Link the newly-created item to the group (also mirrors to Firestore
            // when signed in — the repository handles that internally).
            groupRepository.addPrayerToGroup(
                groupId = groupId,
                prayerItemId = prayerItemId,
                title = title,
                description = description
            )

            onComplete()
        }
    }

    class Factory(
        private val prayerRepository: PrayerRepository,
        private val groupRepository: PrayerGroupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddGroupPrayerViewModel(prayerRepository, groupRepository) as T
        }
    }
}
