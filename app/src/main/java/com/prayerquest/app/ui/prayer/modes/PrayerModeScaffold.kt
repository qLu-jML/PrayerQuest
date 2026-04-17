package com.prayerquest.app.ui.prayer.modes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared layout for every Prayer Mode composable — scrollable content above, a
 * **pinned bottom action bar** that always stays visible.
 *
 * ### Why this exists
 * Before this helper, every mode rendered its "Next Step" / "Complete" button
 * as the last child of a single `verticalScroll` Column. As soon as the prompt,
 * timer, and text field were on screen, the button got pushed below the fold —
 * users had to scroll down every time they wanted to advance a step. On a mode
 * like ACTS (4 steps) that meant 4 unnecessary scrolls per session.
 *
 * ### Layout contract
 * ```
 * ┌──────────────────────────────────────┐
 * │ content() — scrollable, grows with   │
 * │            weight(1f)                │
 * │                                      │
 * │                                      │
 * ├──────────────────────────────────────┤
 * │ action()  — pinned, elevated surface │
 * └──────────────────────────────────────┘
 * ```
 *
 * `content` is placed inside a `verticalScroll` Column so the mode can include
 * any combination of prompts, timers, text fields, and secondary widgets. If a
 * mode wants to do its own scrolling (e.g. nested LazyColumn), it should NOT
 * use this scaffold — pick the component-level layout instead.
 *
 * `action` is wrapped in a tonal Surface so the divide between scrolling body
 * and the persistent action is visually clear even against busy mode content.
 * `imePadding()` on the whole scaffold keeps the action above the soft
 * keyboard when the user is typing in a [androidx.compose.material3.OutlinedTextField].
 *
 * @param modifier Outer modifier — typically `Modifier.weight(1f)` from
 *   `PrayerSessionScreen`, giving this scaffold the full available height
 *   between the progress bar and grade bar.
 * @param contentPadding Inner padding applied to the scrollable content area
 *   (defaults to 16dp on all sides). The action area applies its own padding.
 * @param contentArrangement Spacing between items in the scrollable body.
 * @param actionPadding Inner padding of the pinned action surface.
 * @param action The always-visible bottom content — usually a single primary
 *   Button, but any composable is allowed (e.g. a Row of Edit/Confirm).
 * @param content The scrollable body — prompt cards, timers, text fields, etc.
 */
@Composable
fun PrayerModeScaffold(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    contentArrangement: Arrangement.Vertical = Arrangement.spacedBy(20.dp),
    contentHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    actionPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    action: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Scrollable body — grows to fill the space above the pinned action.
        // weight(1f) ensures the action bar never gets squeezed off even when
        // the mode's content is much taller than the screen.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = contentArrangement,
            horizontalAlignment = contentHorizontalAlignment,
            content = content
        )

        // Pinned action surface. Tonal elevation + thin divider-by-contrast
        // against the body draws a clean line without us having to add an
        // explicit Divider.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(actionPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = action
            )
        }
    }
}
