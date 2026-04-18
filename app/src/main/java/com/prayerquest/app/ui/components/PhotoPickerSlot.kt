package com.prayerquest.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.prayerquest.app.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Circular photo slot with camera + gallery picker.
 *
 * Reusable across Photo Prayers (DD §3.9), Gratitude entries (DD §3.6), and
 * Answered-Prayer testimonies. The slot renders three states:
 *
 *  1. **Empty** — dashed ring + camera icon. Tapping opens the picker sheet.
 *  2. **Filled** — the compressed photo, with a small X in the corner to clear.
 *  3. **Locked** — lock icon + "Upgrade to add photo" subtitle, shown when the
 *     free-tier 200-photo cap has been reached and [onLockedTap] is non-null.
 *
 * The composable takes a raw `currentPath` (nullable absolute path) and
 * emits [onPhotoSaved] with the new absolute path once a photo is picked,
 * compressed, and written to app-private storage. Parents are responsible
 * for persisting the path into Room.
 *
 * @param currentPath Absolute path to the currently-attached photo, or null.
 * @param category    Photo category for storage bucketing (see [PhotoStorage]).
 * @param onPhotoSaved Called on the main thread with the stored absolute path.
 * @param onPhotoCleared Called when the user taps the X to remove the photo.
 * @param onLockedTap When non-null, the picker is replaced with a locked CTA
 *                    and tapping the slot invokes this (typically routes to
 *                    the paywall).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerSlot(
    currentPath: String?,
    category: PhotoStorage.Category,
    onPhotoSaved: (String) -> Unit,
    onPhotoCleared: () -> Unit,
    modifier: Modifier = Modifier,
    onLockedTap: (() -> Unit)? = null,
    size: androidx.compose.ui.unit.Dp = 80.dp,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPickerSheet by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // Resolve error-path strings at @Composable scope so the coroutine blocks
    // below (scope.launch { … }) can capture them as plain Strings. Calling
    // stringResource() inside scope.launch is a @Composable invocation in a
    // non-@Composable context — the Compose compiler rejects it.
    val couldNotSavePhotoMessage = stringResource(R.string.components_could_not_save_photo)

    // Gallery picker (system photo picker, no runtime permission needed on
    // Android 13+; on older devices PickVisualMedia falls back to GetContent).
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val saved = PhotoStorage.savePhoto(context, uri, category)
                if (saved != null) onPhotoSaved(saved) else saveError = couldNotSavePhotoMessage
            }
        }
    }

    // Camera capture — writes directly to a FileProvider URI pointing at
    // cacheDir/camera/<uuid>.jpg. PhotoStorage then re-reads and compresses
    // that file so the result lives in the permanent filesDir bucket.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val tempUri = pendingCameraUri
        pendingCameraUri = null
        if (success && tempUri != null) {
            scope.launch {
                val saved = PhotoStorage.savePhoto(context, tempUri, category)
                // Drop the raw camera file either way — we only need the
                // compressed copy that PhotoStorage wrote to filesDir.
                withContext(Dispatchers.IO) {
                    try {
                        val rawFile = File(tempUri.path ?: return@withContext)
                        if (rawFile.exists()) rawFile.delete()
                    } catch (_: Throwable) {
                    }
                }
                if (saved != null) onPhotoSaved(saved) else saveError = couldNotSavePhotoMessage
            }
        }
    }

    // Resolve at @Composable level — `semantics { }` is non-@Composable.
    val photoSlotDescription = when {
        onLockedTap != null -> stringResource(R.string.components_add_photo_locked_upgrade_to_premium)
        currentPath != null -> stringResource(R.string.components_prayer_photo_tap_to_replace)
        else -> stringResource(R.string.components_add_a_photo_to_this_prayer)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                when {
                    onLockedTap != null -> onLockedTap()
                    else -> showPickerSheet = true
                }
            }
            .semantics {
                contentDescription = photoSlotDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            onLockedTap != null && currentPath == null -> {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            currentPath != null -> {
                // Decode the file off the main thread would be ideal, but
                // these images are already capped at 1280px × 80% JPEG so
                // decoding is cheap (~ms on mid-range devices) and doing it
                // inline keeps the slot simple. If this ever shows up in
                // profiling we can swap in Coil.
                val bmp = remember(currentPath) {
                    try {
                        BitmapFactory.decodeFile(currentPath)
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                    )
                } else {
                    // File gone — fall back to the placeholder icon.
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Small clear-X in the top-right corner.
                // Resolve the accessibility label at the @Composable level —
                // `semantics { }` runs in a non-@Composable scope.
                val removePhotoLabel = stringResource(R.string.common_remove_photo)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onPhotoCleared() }
                        .semantics { contentDescription = removePhotoLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showPickerSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.components_add_a_photo),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PickerSheetRow(
                    icon = Icons.Default.PhotoCamera,
                    label = stringResource(R.string.components_take_a_photo),
                    onClick = {
                        showPickerSheet = false
                        // Build a scratch file in cacheDir/camera/ and hand
                        // the FileProvider URI to the camera app.
                        val tempUri = allocateCameraUri(context)
                        pendingCameraUri = tempUri
                        cameraLauncher.launch(tempUri)
                    }
                )
                PickerSheetRow(
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(R.string.components_choose_from_gallery),
                    onClick = {
                        showPickerSheet = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    saveError?.let { msg ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text(stringResource(R.string.components_photo_couldn_t_be_saved)) },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { saveError = null }) { Text("OK") }
            }
        )
    }

    // Reset any stale pending URI when the composable leaves composition so
    // we don't try to read a cacheDir path after the user rotates mid-flight.
    LaunchedEffect(Unit) { /* no-op placeholder for lifecycle hook */ }
}

@Composable
private fun PickerSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun allocateCameraUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg").also { it.createNewFile() }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
