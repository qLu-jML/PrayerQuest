package com.prayerquest.app.ui.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.entity.PrayerGroup

/**
 * Shown after the user picks a prayer mode from [ModePickerScreen]. Lets them
 * choose which pool of prayers to focus on during the session:
 *
 *   1. **General Prayers** — their global active list. Default choice when
 *      no specific scope is wanted; maps to `collectionId = null` on the
 *      session route.
 *   2. **A private collection** — any [PrayerCollection] the user has
 *      created. Scopes the session to that collection's items.
 *   3. **A prayer group** — routes out to the group's detail screen so the
 *      user can pick a specific shared request (or pray the whole queue).
 *      Group sessions aren't a native mode on PrayerSessionScreen yet, so
 *      this is the cleanest path for MVP.
 *
 * Entry points that already know the scope (e.g. Collection Detail →
 * "Pray this collection") skip this screen entirely — see
 * `Routes.prayerSession(..., collectionId)` in the NavHost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerListPickerScreen(
    chosenModeName: String,
    onGeneralPrayers: () -> Unit,
    onCollectionPicked: (collectionId: Long) -> Unit,
    onAddItemsToCollection: (collectionId: Long) -> Unit,
    onBrowseLibrary: () -> Unit,
    onCreateCollection: () -> Unit,
    onGroupPicked: (groupId: Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication

    val collections by app.container.collectionRepository.observeAll()
        .collectAsState(initial = emptyList())
    val groups by app.container.prayerGroupRepository.observeAllGroups()
        .collectAsState(initial = emptyList())

    // Count of the user's active prayer items (global list). If zero, tapping
    // "General Prayers" would launch an empty session — we prompt them to
    // browse the pre-written library or create a collection instead.
    val activeItems by app.container.prayerRepository.observeActiveItems()
        .collectAsState(initial = emptyList())

    // When the user taps a collection with zero items we don't launch an
    // empty session — we surface this dialog instead. Tracks the offending
    // collection so the "Add Prayers" CTA knows where to route.
    var emptyCollectionPrompt by remember { mutableStateOf<PrayerCollection?>(null) }

    // Shown when "General Prayers" is tapped but the user's active list is
    // empty. Two-path dialog: Library for pre-written prayers, or a fresh
    // collection for their own.
    var emptyGeneralPrompt by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Choose a List") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeaderBlurb(modeName = chosenModeName) }

            // 1. General Prayers — always on top so it's the path of least
            //    resistance for users who just want to start praying. If
            //    the user has no active items at all, tapping this surfaces
            //    the empty-general dialog instead of launching an empty
            //    session.
            item {
                ListOptionCard(
                    emoji = "✨",
                    title = "General Prayers",
                    subtitle = "Your default list — quick way to pray without picking a specific scope",
                    onClick = {
                        if (activeItems.isEmpty()) {
                            emptyGeneralPrompt = true
                        } else {
                            onGeneralPrayers()
                        }
                    }
                )
            }

            // 2. Private collections — empty ones open the "add prayers first"
            //    dialog instead of kicking off an empty session.
            if (collections.isNotEmpty()) {
                item { SectionLabel("Your Collections") }
                items(collections, key = { it.id }) { collection ->
                    CollectionOptionCard(
                        collection = collection,
                        onClick = {
                            if (collection.itemCount <= 0) {
                                emptyCollectionPrompt = collection
                            } else {
                                onCollectionPicked(collection.id)
                            }
                        }
                    )
                }
            }

            // 3. Prayer groups — if any. Tapping routes into the group detail
            //    where the user can pray specific shared requests.
            if (groups.isNotEmpty()) {
                item { SectionLabel("Prayer Groups") }
                items(groups, key = { it.id }) { group ->
                    GroupOptionCard(
                        group = group,
                        onClick = { onGroupPicked(group.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // Empty-general guard dialog. Shown when the user taps General Prayers
    // but has no active prayer items anywhere. Offers two paths: browse the
    // pre-written Library, or create a collection of their own.
    if (emptyGeneralPrompt) {
        AlertDialog(
            onDismissRequest = { emptyGeneralPrompt = false },
            title = { Text(text = "Nothing to pray yet") },
            text = {
                Text(
                    text = "Your general list is empty. Browse the Library for pre-written prayers, " +
                        "or create a collection to add your own."
                )
            },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            emptyGeneralPrompt = false
                            onBrowseLibrary()
                        }
                    ) {
                        Text("Browse Library")
                    }
                    TextButton(
                        onClick = {
                            emptyGeneralPrompt = false
                            onCreateCollection()
                        }
                    ) {
                        Text("Create a Collection")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { emptyGeneralPrompt = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    // Empty-collection guard dialog. Shown when the user taps a collection
    // that has no prayer items yet — rather than dropping them into an
    // empty session, we explain and offer a direct path to add items.
    emptyCollectionPrompt?.let { collection ->
        AlertDialog(
            onDismissRequest = { emptyCollectionPrompt = null },
            title = {
                Text(text = "No prayers in this collection yet")
            },
            text = {
                Text(
                    text = "\"${collection.name}\" doesn't have any prayer items yet. " +
                        "Add a few so you have something to pray through."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = collection.id
                        emptyCollectionPrompt = null
                        onAddItemsToCollection(id)
                    }
                ) {
                    Text("Add Prayers")
                }
            },
            dismissButton = {
                TextButton(onClick = { emptyCollectionPrompt = null }) {
                    Text("Not Now")
                }
            }
        )
    }
}

@Composable
private fun HeaderBlurb(modeName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Who will you pray for?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Pick a list to focus this ${displayMode(modeName)} session — or just pray your general list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ListOptionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 28.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CollectionOptionCard(
    collection: PrayerCollection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (collection.emoji.isNotBlank()) {
                    Text(text = collection.emoji, fontSize = 28.sp)
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = when {
                    collection.itemCount == 0 -> "No prayers yet"
                    collection.itemCount == 1 -> "1 prayer"
                    else -> "${collection.itemCount} prayers"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GroupOptionCard(
    group: PrayerGroup,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (group.emoji.isNotBlank()) {
                    Text(text = group.emoji, fontSize = 28.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Shared prayer requests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Humanize a PrayerMode enum name for the header blurb. Falls back to a
 * generic phrase if the name is empty or unrecognized — we never want the
 * header to read "Pick a list to focus this  session".
 */
private fun displayMode(modeName: String): String = when (modeName) {
    "FLASH_PRAY_SWIPE" -> "Flash-Pray"
    "BREATH_PRAYER" -> "Breath Prayer"
    "INTERCESSION_DRILL" -> "Intercession"
    "GUIDED_ACTS" -> "ACTS"
    "DAILY_EXAMEN" -> "Examen"
    "LECTIO_DIVINA" -> "Lectio Divina"
    "VOICE_RECORD" -> "Voice"
    "PRAYER_JOURNAL" -> "Journal"
    "PRAYER_BEADS" -> "Prayer Beads"
    "DAILY_OFFICE" -> "Daily Office"
    else -> "prayer"
}
