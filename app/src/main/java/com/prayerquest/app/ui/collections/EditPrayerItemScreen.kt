package com.prayerquest.app.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.prayerquest.app.billing.PremiumFeatures
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.PhotoCountRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.ui.components.PhotoPickerSlot
import com.prayerquest.app.util.PhotoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Edit screen for an existing prayer item — title, description, and the
 * Photo Prayer (DD §3.9) slot.
 *
 * Reached by tapping a card on Collection Detail. Stays focused: no status
 * transitions, no testimony fields, no category editor. Answered-flow moves
 * and category changes live on their own screens so edit here is purely "I
 * want to tweak the copy or swap the photo."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPrayerItemScreen(
    prayerItemId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: EditPrayerItemViewModel = viewModel(
        key = "edit_prayer_item_$prayerItemId",
        factory = EditPrayerItemViewModel.Factory(
            prayerItemId = prayerItemId,
            prayerRepository = app.container.prayerRepository,
            photoCountRepository = app.container.photoCountRepository,
        )
    )

    val item by viewModel.item.collectAsState()
    val isPremium by app.container.premiumRepository.isPremium
        .collectAsState(initial = false)
    val photoCount by app.container.photoCountRepository.count.collectAsState()
    val canAddPhoto = PremiumFeatures.canAddPhoto(isPremium, photoCount)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }

    // Hydrate local UI state the first time the item lands. We guard this
    // with a flag so a later Flow emission (e.g. if the row updates via
    // another flow) doesn't stomp the user's in-progress edits.
    LaunchedEffect(item) {
        val loaded = item
        if (loaded != null && !loadedOnce) {
            title = loaded.title
            description = loaded.description
            photoPath = loaded.photoUri
            loadedOnce = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collections_edit_prayer)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) { padding ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.collections_photo),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.collections_stays_private_to_this_device_never_shared),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PhotoPickerSlot(
                    currentPath = photoPath,
                    category = PhotoStorage.Category.PRAYER_ITEM,
                    onPhotoSaved = { newPath ->
                        val previous = photoPath
                        photoPath = newPath
                        // When the user swaps the photo, delete the old
                        // file so the 200-cap count stays honest. Safe —
                        // the new path is already on disk before we get
                        // here.
                        if (!previous.isNullOrBlank() && previous != newPath) {
                            PhotoStorage.deletePhoto(previous)
                        }
                        viewModel.onPhotoChanged()
                    },
                    onPhotoCleared = {
                        val previous = photoPath
                        photoPath = null
                        if (!previous.isNullOrBlank()) {
                            PhotoStorage.deletePhoto(previous)
                            viewModel.onPhotoChanged()
                        }
                    },
                    onLockedTap = if (!canAddPhoto && photoPath == null) onNavigateToPaywall else null,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.collections_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.collections_description_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        viewModel.save(
                            title = title.trim(),
                            description = description.trim(),
                            photoPath = photoPath,
                        )
                        onNavigateBack()
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

class EditPrayerItemViewModel(
    private val prayerItemId: Long,
    private val prayerRepository: PrayerRepository,
    private val photoCountRepository: PhotoCountRepository,
) : ViewModel() {

    private val _item = MutableStateFlow<PrayerItem?>(null)
    val item: StateFlow<PrayerItem?> = _item.asStateFlow()

    init {
        viewModelScope.launch {
            prayerRepository.observeItem(prayerItemId).collect { _item.value = it }
        }
    }

    fun save(title: String, description: String, photoPath: String?) {
        val existing = _item.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            val updated = existing.copy(
                title = title,
                description = description,
                photoUri = photoPath,
            )
            prayerRepository.updateItem(updated)
            // Edge case: if the user cleared the photo on the edit screen
            // but never called the slot's clear-X (e.g. closed the picker
            // mid-flight), sync disk state so the old file doesn't linger
            // when we wouldn't be referencing it anymore.
            if (existing.photoUri != null && existing.photoUri != photoPath) {
                PhotoStorage.deletePhoto(existing.photoUri)
            }
            photoCountRepository.refresh()
        }
    }

    fun onPhotoChanged() {
        viewModelScope.launch { photoCountRepository.refresh() }
    }

    class Factory(
        private val prayerItemId: Long,
        private val prayerRepository: PrayerRepository,
        private val photoCountRepository: PhotoCountRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditPrayerItemViewModel(
                prayerItemId = prayerItemId,
                prayerRepository = prayerRepository,
                photoCountRepository = photoCountRepository,
            ) as T
        }
    }
}
