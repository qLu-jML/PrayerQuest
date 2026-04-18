package com.prayerquest.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.ads.BannerAdView
import com.prayerquest.app.data.entity.BiblePrayer
import com.prayerquest.app.data.entity.FamousPrayer
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.prayer.SuggestedPrayerPack
import com.prayerquest.app.ui.library.LibraryViewModel
import com.prayerquest.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Library screen — single scrolling page of "shelves" (per user feedback).
//
// Previously this screen had five tabs (Collections, Famous Prayers, Bible
// Prayers, Answered, Gratitude). Users reported that the tabs hid content
// and forced them to remember what lived where. The new layout stacks every
// section as a horizontally-scrolling shelf on one vertical scroll, mirroring
// the streaming-app pattern we already use in the Mode Picker.
//
// A global search bar at the top filters across every section. When a search
// is active the shelves collapse into flat match lists so users can scan
// everything that matches without losing the section groupings.
// ─────────────────────────────────────────────────────────────────────────────

private val RAIL_CARD_WIDTH = 160.dp
private val RAIL_CARD_HEIGHT = 180.dp

@Composable
fun LibraryScreen(
    onNavigateToCollectionDetail: (Long) -> Unit,
    onNavigateToFamousPrayerDetail: (String) -> Unit,
    onNavigateToBiblePrayerDetail: (String) -> Unit,
    onNavigateToAnsweredPrayerDetail: (Long) -> Unit,
    onNavigateToCreateCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.Factory(
            app.container.collectionRepository,
            app.container.prayerRepository,
            app.container.gratitudeRepository,
            app.container.userPreferences,
            app.container.suggestedPackLoader
        )
    )

    val collections by libraryViewModel.collections.collectAsState(initial = emptyList())
    val famousPrayers by libraryViewModel.famousPrayers.collectAsState(initial = emptyList())
    val biblePrayers by libraryViewModel.biblePrayers.collectAsState(initial = emptyList())
    val answeredPrayers by libraryViewModel.answeredPrayers.collectAsState(initial = emptyList())
    val gratitudeEntries by libraryViewModel.gratitudeEntries.collectAsState(initial = emptyList())
    // Null unless the user has the liturgical calendar enabled AND today falls
    // in Advent / Lent / Holy Week AND the matching pack exists in assets.
    val seasonalPack by libraryViewModel.todaySeasonalPack.collectAsState(initial = null)

    var searchQuery by remember { mutableStateOf("") }
    val trimmedQuery = searchQuery.trim()
    val isSearching = trimmedQuery.isNotEmpty()

    // Pre-filter each section by the same search string so each shelf's
    // "empty" state can be detected up front. Keeps the render loop simple.
    val filteredCollections = collections.filterIfSearch(isSearching) {
        it.name.contains(trimmedQuery, ignoreCase = true) ||
            it.description.contains(trimmedQuery, ignoreCase = true) ||
            it.topicTag.contains(trimmedQuery, ignoreCase = true)
    }
    val filteredFamous = famousPrayers.filterIfSearch(isSearching) {
        it.title.contains(trimmedQuery, ignoreCase = true) ||
            it.author.contains(trimmedQuery, ignoreCase = true) ||
            it.text.contains(trimmedQuery, ignoreCase = true)
    }
    val filteredBible = biblePrayers.filterIfSearch(isSearching) {
        it.title.contains(trimmedQuery, ignoreCase = true) ||
            it.person.contains(trimmedQuery, ignoreCase = true) ||
            it.reference.contains(trimmedQuery, ignoreCase = true) ||
            it.description.contains(trimmedQuery, ignoreCase = true)
    }
    val filteredAnswered = answeredPrayers.filterIfSearch(isSearching) {
        it.title.contains(trimmedQuery, ignoreCase = true) ||
            (it.testimonyText?.contains(trimmedQuery, ignoreCase = true) == true)
    }
    val filteredGratitude = gratitudeEntries.filterIfSearch(isSearching) {
        it.text.contains(trimmedQuery, ignoreCase = true) ||
            it.category.contains(trimmedQuery, ignoreCase = true)
    }

    // Featured items — picked ONCE at composition time (not per-frame) so
    // they don't jump around as data loads or state updates churn.
    val featuredFamous = remember(famousPrayers) {
        famousPrayers.maxByOrNull { it.userPrayedCount } ?: famousPrayers.firstOrNull()
    }

    Column(modifier = modifier.fillMaxSize()) {
        LibrarySearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        // When search is active AND nothing matches anywhere, show one
        // empty-state instead of five empty shelves.
        val allEmptyInSearch = isSearching &&
            filteredCollections.isEmpty() &&
            filteredFamous.isEmpty() &&
            filteredBible.isEmpty() &&
            filteredAnswered.isEmpty() &&
            filteredGratitude.isEmpty()

        if (allEmptyInSearch) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No results for \"$trimmedQuery\"",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Try a different word, or clear the search to browse everything.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Hero featured (only when not searching) ────────────────
                if (!isSearching && featuredFamous != null) {
                    item(key = "hero-featured") {
                        HeroCard(
                            title = featuredFamous.title,
                            subtitle = "by ${featuredFamous.author}",
                            body = featuredFamous.text,
                            tag = featuredFamous.category.ifEmpty { null },
                            onClick = { onNavigateToFamousPrayerDetail(featuredFamous.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // ── Seasonal pack pin (Advent / Lent / Holy Week) ──────────
                //
                // Rendered ABOVE the Collections shelf (and above the empty-
                // state CTA) so during the three liturgical seasons that have
                // matching packs, the seasonal content is the first thing
                // under the hero. DD §3.5.4 — "packs auto-surface during the
                // appropriate season". Skipped entirely when search is active
                // so match lists aren't diluted by a featured banner.
                if (!isSearching && seasonalPack != null) {
                    item(key = "shelf-seasonal-pack-${seasonalPack!!.id}") {
                        SeasonalPackCard(
                            pack = seasonalPack!!,
                            onClick = onNavigateToCreateCollection
                        )
                    }
                }

                // ── My Collections shelf ───────────────────────────────────
                if (filteredCollections.isNotEmpty()) {
                    item(key = "shelf-collections") {
                        ShelfHeader(
                            title = "My Collections",
                            countLabel = pluralize(filteredCollections.size, "collection"),
                            action = {
                                TextButton(onClick = onNavigateToCreateCollection) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("New", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        )
                        HorizontalRail(
                            items = filteredCollections,
                            key = { it.id }
                        ) { collection ->
                            CollectionPoster(
                                collection = collection,
                                modifier = Modifier
                                    .width(RAIL_CARD_WIDTH)
                                    .height(RAIL_CARD_HEIGHT),
                                onClick = { onNavigateToCollectionDetail(collection.id) }
                            )
                        }
                    }
                } else if (!isSearching) {
                    // Zero-state CTA — critical because Collections is where
                    // users' own content lives. Without a visible prompt to
                    // create one, the Library feels read-only.
                    item(key = "shelf-collections-empty") {
                        EmptyCollectionsCta(onCreate = onNavigateToCreateCollection)
                    }
                }

                // ── Famous Prayers shelves ─────────────────────────────────
                //
                // 32+ prayers in a single horizontal rail meant users
                // had to scroll forever to see what was there. Now each
                // category becomes its own mini-shelf ("Foundation",
                // "Peace", "Service", etc.) so users can scan the
                // category labels like a table of contents and stop at
                // the one they want. Categories with no content after
                // filtering are omitted; an "Other" bucket catches
                // prayers with no category so nothing is hidden.
                //
                // When search is active we fall back to a single flat
                // "Famous Prayers — Search Results" shelf, because
                // splitting tiny match lists across five sub-shelves
                // is worse than one clearly labeled results rail.
                if (filteredFamous.isNotEmpty()) {
                    if (isSearching) {
                        item(key = "shelf-famous-results") {
                            ShelfHeader(
                                title = "Famous Prayers",
                                countLabel = "${filteredFamous.size}"
                            )
                            HorizontalRail(
                                items = filteredFamous,
                                key = { it.id }
                            ) { prayer ->
                                FamousPrayerPoster(
                                    prayer = prayer,
                                    modifier = Modifier
                                        .width(RAIL_CARD_WIDTH)
                                        .height(RAIL_CARD_HEIGHT),
                                    onClick = { onNavigateToFamousPrayerDetail(prayer.id) }
                                )
                            }
                        }
                    } else {
                        // Group-by-category. `groupBy` preserves the order
                        // of first encounter, which matches insertion order
                        // from the asset importer — a stable, predictable
                        // sequence that the content team controls by
                        // ordering entries in `famous_prayers.json`.
                        val grouped = filteredFamous.groupBy {
                            it.category.ifBlank { "Other" }
                        }
                        for ((category, prayersInCategory) in grouped) {
                            item(key = "shelf-famous-$category") {
                                ShelfHeader(
                                    title = category,
                                    countLabel = "${prayersInCategory.size}"
                                )
                                HorizontalRail(
                                    items = prayersInCategory,
                                    key = { it.id }
                                ) { prayer ->
                                    FamousPrayerPoster(
                                        prayer = prayer,
                                        modifier = Modifier
                                            .width(RAIL_CARD_WIDTH)
                                            .height(RAIL_CARD_HEIGHT),
                                        onClick = { onNavigateToFamousPrayerDetail(prayer.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Bible Prayers shelf ────────────────────────────────────
                if (filteredBible.isNotEmpty()) {
                    item(key = "shelf-bible") {
                        ShelfHeader(
                            title = "Bible Prayers",
                            countLabel = "${filteredBible.size}"
                        )
                        HorizontalRail(
                            items = filteredBible,
                            key = { it.id }
                        ) { prayer ->
                            BiblePrayerPoster(
                                prayer = prayer,
                                modifier = Modifier
                                    .width(RAIL_CARD_WIDTH)
                                    .height(RAIL_CARD_HEIGHT),
                                onClick = { onNavigateToBiblePrayerDetail(prayer.id) }
                            )
                        }
                    }
                }

                // ── Answered Prayers shelf ─────────────────────────────────
                if (filteredAnswered.isNotEmpty()) {
                    item(key = "shelf-answered") {
                        ShelfHeader(
                            title = "Answered Prayers",
                            countLabel = pluralize(filteredAnswered.size, "answer")
                        )
                        HorizontalRail(
                            items = filteredAnswered,
                            key = { it.id }
                        ) { prayer ->
                            AnsweredPoster(
                                prayer = prayer,
                                modifier = Modifier
                                    .width(RAIL_CARD_WIDTH)
                                    .height(RAIL_CARD_HEIGHT),
                                onClick = { onNavigateToAnsweredPrayerDetail(prayer.id) }
                            )
                        }
                    }
                }

                // ── Gratitude shelf ────────────────────────────────────────
                if (filteredGratitude.isNotEmpty()) {
                    item(key = "shelf-gratitude") {
                        ShelfHeader(
                            title = "Gratitude",
                            countLabel = pluralize(filteredGratitude.size, "entry", "entries")
                        )
                        HorizontalRail(
                            items = filteredGratitude,
                            key = { it.id }
                        ) { entry ->
                            GratitudePoster(
                                entry = entry,
                                modifier = Modifier
                                    .width(RAIL_CARD_WIDTH)
                                    .height(RAIL_CARD_HEIGHT)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }

        BannerAdView(modifier = Modifier.fillMaxWidth())
    }
}

// ─── Shared building blocks ──────────────────────────────────────────────────

@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search the library…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {})
    )
}

@Composable
private fun ShelfHeader(
    title: String,
    countLabel: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (action != null) action()
    }
}

@Composable
private fun <T> HorizontalRail(
    items: List<T>,
    key: (T) -> Any,
    card: @Composable (T) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        items(items, key = key) { item ->
            card(item)
        }
    }
}

@Composable
private fun EmptyCollectionsCta(
    onCreate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "My Collections",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ElevatedCard(
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Create Your First Collection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Organize prayers by family, work, or any theme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Hero card — a big featured item at the top of the Library.
 *
 * Single vertical Column (no stacked BottomStart overlay) so a 2-line
 * title can't collide with the body text below it — prior version laid
 * out the header and the body as two separate Columns (top-left and
 * bottom-start anchored), which overlapped in the middle on longer
 * titles and made the subtitle ("by Author") bleed into the body.
 *
 * The tag is now a plain styled pill rather than an [AssistChip] —
 * the whole card is clickable, and the chip's own onClick={} handler
 * was swallowing taps in its region (the "Surrender" button that "did
 * nothing" was that chip). Plain Text keeps clicks passing through to
 * the card.
 */
@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    body: String,
    tag: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer
        )
    )
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Featured",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            // Body fills remaining vertical space so the tag sits neatly
            // at the bottom without overlapping the header.
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 3,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!tag.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// ─── Posters (one per section) ───────────────────────────────────────────────

@Composable
private fun FamousPrayerPoster(
    prayer: FamousPrayer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = prayer.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2
                )
                Text(
                    text = "by ${prayer.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (prayer.category.isNotEmpty()) {
                    Text(
                        text = prayer.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                if (prayer.userPrayedCount > 0) {
                    Text(
                        text = "${prayer.userPrayedCount}×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BiblePrayerPoster(
    prayer: BiblePrayer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = prayer.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2
                )
                if (prayer.person.isNotEmpty()) {
                    Text(
                        text = prayer.person,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (prayer.reference.isNotEmpty()) {
                    Text(
                        text = prayer.reference,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (prayer.category.isNotEmpty()) {
                    Text(
                        text = prayer.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (prayer.userPrayedCount > 0) {
                    Text(
                        text = "${prayer.userPrayedCount}×",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionPoster(
    collection: PrayerCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = collection.emoji.ifBlank { "📖" },
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${collection.itemCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2
                )
                if (collection.topicTag.isNotEmpty()) {
                    Text(
                        text = collection.topicTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AnsweredPoster(
    prayer: PrayerItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dateText = prayer.answeredAt?.let { dateFormat.format(Date(it)) } ?: "Recently"

    ElevatedCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = prayer.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2
                )
                if (!prayer.testimonyText.isNullOrBlank()) {
                    Text(
                        text = prayer.testimonyText!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun GratitudePoster(
    entry: GratitudeEntry,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dateText = dateFormat.format(Date(entry.timestamp))

    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!entry.photoUri.isNullOrEmpty()) {
                    Text(text = "📸", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4
            )
            if (entry.category.isNotEmpty()) {
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = GratitudeGreen
                )
            }
        }
    }
}

/**
 * Pinned banner for the current liturgical season's pack (Advent / Lent /
 * Holy Week). Subtle stained-glass accent — a thin violet leading bar and an
 * amber "In Season" badge, over [MaterialTheme.colorScheme.secondaryContainer]
 * so the card sits warmly between the hero and the rest of the shelves
 * without fighting them for attention.
 *
 * Tapping navigates to the Create Collection sheet so the user can turn the
 * pack into one of their collections — same CTA the existing empty-
 * collections state uses. Future revision may open a dedicated
 * "Preview pack → Add to Library" screen.
 */
@Composable
private fun SeasonalPackCard(
    pack: SuggestedPrayerPack,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thin stained-glass leading bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StainedGlassViolet)
            )
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pack.emoji.ifBlank { "🕯️" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 2
                    )
                }
                if (pack.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Amber "In Season" badge — the compact seasonal signal.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StainedGlassAmber.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "In Season · ${pluralize(pack.prayers.size, "prayer")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── Tiny utilities ──────────────────────────────────────────────────────────

/**
 * `filter` only when a search query is active. When no search is in progress
 * we return the full list untouched so every shelf keeps its full content.
 */
private fun <T> List<T>.filterIfSearch(
    isSearching: Boolean,
    predicate: (T) -> Boolean
): List<T> = if (isSearching) filter(predicate) else this

/**
 * Tiny singular/plural helper so shelf count labels read naturally
 * ("1 collection" / "3 collections") without littering each call site
 * with an `if` expression.
 */
private fun pluralize(n: Int, singular: String, plural: String = "${singular}s"): String =
    "$n ${if (n == 1) singular else plural}"
