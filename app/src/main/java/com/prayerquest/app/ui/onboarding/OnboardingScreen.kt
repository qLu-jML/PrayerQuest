package com.prayerquest.app.ui.onboarding

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayerquest.app.data.preferences.DevotionalAuthor
import com.prayerquest.app.data.preferences.LiturgicalCalendar
import com.prayerquest.app.domain.model.Tradition
import com.prayerquest.app.notifications.rememberNotificationPermissionState
import com.prayerquest.app.ui.theme.CommunityBlue
import com.prayerquest.app.ui.theme.Gold700
import com.prayerquest.app.ui.theme.GratitudeGreen
import com.prayerquest.app.ui.theme.Indigo500
import com.prayerquest.app.ui.theme.Indigo700
import com.prayerquest.app.ui.theme.Ink
import com.prayerquest.app.ui.theme.Parchment
import com.prayerquest.app.ui.theme.Rose500
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
// Step enumeration — DD §3.2
// ═══════════════════════════════════════════════════════════════════════════

private const val STEP_WELCOME = 0
private const val STEP_NAME_GOAL = 1
private const val STEP_TRADITIONS = 2
private const val STEP_LITURGICAL = 3
private const val STEP_DEVOTIONAL = 4
private const val STEP_REMINDERS = 5
private const val STEP_FIRST_COLLECTION = 6
private const val STEP_FIRST_GRATITUDE = 7
private const val TOTAL_STEPS = 8

// ═══════════════════════════════════════════════════════════════════════════
// Main Onboarding Screen
// ═══════════════════════════════════════════════════════════════════════════

/**
 * 8-step data-collecting onboarding flow (DD §3.2). Unlike the old tutorial
 * carousel, every step persists a real preference or seeds a real piece of
 * data. The user can go back at any time and their earlier answers survive.
 *
 * On the final step we commit everything at once via
 * [OnboardingViewModel.completeOnboarding], then flip the onboarding-completed
 * flag. MainActivity observes that flag and switches to the main app.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(STEP_WELCOME) }
    val answers by viewModel.answers.collectAsState()
    val scope = rememberCoroutineScope()
    val notificationPermission = rememberNotificationPermissionState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        // Top bar — progress indicator + Back/Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { if (step > 0) step-- },
                enabled = step > 0
            ) {
                Text(
                    text = if (step > 0) "Back" else "",
                    color = Ink.copy(alpha = if (step > 0) 0.7f else 0f),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            ProgressDots(
                current = step,
                total = TOTAL_STEPS
            )
            Spacer(modifier = Modifier.weight(1f))
            // Skip button — visible on every step except Welcome. Each step's
            // answers have sensible defaults (collection = "My Prayer List",
            // gratitude = blank-and-skipped) so skipping any of them is safe.
            // On the final step Skip = commit with whatever's filled in + go
            // home; on earlier steps it just advances to the next step.
            if (step in STEP_NAME_GOAL..STEP_FIRST_GRATITUDE) {
                TextButton(onClick = {
                    if (step == TOTAL_STEPS - 1) {
                        scope.launch {
                            viewModel.completeOnboarding()
                            onNavigateToHome()
                        }
                    } else {
                        step = minOf(step + 1, TOTAL_STEPS - 1)
                    }
                }) {
                    Text(
                        text = "Skip",
                        color = Ink.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(64.dp))  // balance the row
            }
        }

        // Step content with horizontal slide transition
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) togetherWith
                        slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
                } else {
                    slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) togetherWith
                        slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "onboarding_step"
        ) { currentStep ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentStep) {
                    STEP_WELCOME -> WelcomeStep()
                    STEP_NAME_GOAL -> NameAndGoalStep(
                        displayName = answers.displayName,
                        goalMinutes = answers.dailyGoalMinutes,
                        onNameChange = viewModel::setDisplayName,
                        onGoalChange = viewModel::setDailyGoal
                    )
                    STEP_TRADITIONS -> TraditionsStep(
                        selected = answers.traditions,
                        onToggle = viewModel::toggleTradition
                    )
                    STEP_LITURGICAL -> LiturgicalStep(
                        current = answers.liturgicalCalendar,
                        onSelect = viewModel::setLiturgicalCalendar
                    )
                    STEP_DEVOTIONAL -> DevotionalStep(
                        author = answers.devotionalAuthor,
                        spurgeonMorningMin = answers.spurgeonMin,
                        spurgeonEveningMin = answers.spurgeonEveningMin,
                        spurgeonMorningEnabled = answers.spurgeonMorningEnabled,
                        spurgeonEveningEnabled = answers.spurgeonEveningEnabled,
                        bonhoefferMin = answers.bonhoefferMin,
                        onAuthorChange = viewModel::setDevotionalAuthor,
                        onSpurgeonMorningMinChange = viewModel::setSpurgeonMin,
                        onSpurgeonEveningMinChange = viewModel::setSpurgeonEveningMin,
                        onSpurgeonMorningEnabledChange = viewModel::setSpurgeonMorningEnabled,
                        onSpurgeonEveningEnabledChange = viewModel::setSpurgeonEveningEnabled,
                        onBonhoefferMinChange = viewModel::setBonhoefferMin
                    )
                    STEP_REMINDERS -> RemindersStep(
                        morningEnabled = answers.morningEnabled,
                        morningMin = answers.morningMin,
                        middayEnabled = answers.middayEnabled,
                        middayMin = answers.middayMin,
                        eveningEnabled = answers.eveningEnabled,
                        eveningMin = answers.eveningMin,
                        quietEnabled = answers.quietHoursEnabled,
                        quietStartMin = answers.quietStartMin,
                        quietEndMin = answers.quietEndMin,
                        permissionGranted = notificationPermission.isGranted,
                        onRequestPermission = notificationPermission::request,
                        onMorningChange = viewModel::setMorning,
                        onMiddayChange = viewModel::setMidday,
                        onEveningChange = viewModel::setEvening,
                        onQuietEnabledChange = viewModel::setQuietHoursEnabled,
                        onQuietWindowChange = viewModel::setQuietWindow
                    )
                    STEP_FIRST_COLLECTION -> FirstCollectionStep(
                        name = answers.firstCollectionName,
                        description = answers.firstCollectionDescription,
                        onNameChange = viewModel::setFirstCollectionName,
                        onDescriptionChange = viewModel::setFirstCollectionDescription
                    )
                    STEP_FIRST_GRATITUDE -> FirstGratitudeStep(
                        text = answers.firstGratitudeText,
                        onTextChange = viewModel::setFirstGratitudeText
                    )
                }
            }
        }

        // Primary CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isLast = step == TOTAL_STEPS - 1
            Button(
                onClick = {
                    if (isLast) {
                        scope.launch {
                            viewModel.completeOnboarding()
                            onNavigateToHome()
                        }
                    } else {
                        step++
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo700,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = when (step) {
                        STEP_WELCOME -> "Let's begin"
                        TOTAL_STEPS - 1 -> "Begin praying"
                        else -> "Next"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Progress indicator (pill dots)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val isCurrent = index == current
            val isPast = index < current
            val width by animateFloatAsState(
                targetValue = if (isCurrent) 20f else 8f,
                animationSpec = tween(250),
                label = "dot_width"
            )
            val color = when {
                isCurrent -> Indigo700
                isPast -> Indigo700.copy(alpha = 0.7f)
                else -> Indigo700.copy(alpha = 0.2f)
            }
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            if (index < total - 1) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 1 — Welcome
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun WelcomeStep() {
    StepScaffold(
        iconTint = Indigo500,
        title = "Welcome to\nPrayerQuest",
        subtitle = "Your gentle companion for a deeper prayer life"
    ) {
        Text(
            text = "Over the next few steps we'll set up your prayer rhythm together — " +
                    "your name, daily goal, how you like to pray, and when you'd like us to " +
                    "nudge you. Everything is adjustable later.",
            fontSize = 15.sp,
            color = Ink.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 2 — Name + Daily Goal
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NameAndGoalStep(
    displayName: String,
    goalMinutes: Int,
    onNameChange: (String) -> Unit,
    onGoalChange: (Int) -> Unit
) {
    StepScaffold(
        iconTint = Gold700,
        title = "Who are you\npraying as?",
        subtitle = "Name and daily goal"
    ) {
        TextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text("Your name") },
            placeholder = { Text("Prayer Warrior") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Daily prayer goal (minutes)",
            style = MaterialTheme.typography.labelMedium,
            color = Ink.copy(alpha = 0.75f),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3, 5, 10, 15, 20, 30, 45, 60).forEach { option ->
                GoalPill(
                    value = option,
                    isSelected = goalMinutes == option,
                    onClick = { onGoalChange(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Any amount counts — even three minutes of honest prayer moves mountains.",
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun GoalPill(value: Int, isSelected: Boolean, onClick: () -> Unit) {
    // clip must come BEFORE clickable so the ripple respects the rounded shape,
    // and clickable must come BEFORE padding so the whole pill is tappable.
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Indigo700 else Indigo700.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "$value min",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Indigo700
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 3 — Traditions (multi-select)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TraditionsStep(
    selected: Set<Tradition>,
    onToggle: (Tradition) -> Unit
) {
    StepScaffold(
        iconTint = CommunityBlue,
        title = "How do\nyou pray?",
        subtitle = "Pick any that speak to you"
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Tradition.values().forEach { tradition ->
                val isSelected = tradition in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(tradition) },
                    label = { Text(tradition.displayName) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Indigo700,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "This tunes which prayer modes appear first. You can enable any mode later " +
                    "regardless — traditions just shape the defaults.",
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 4 — Liturgical Calendar
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LiturgicalStep(
    current: LiturgicalCalendar,
    onSelect: (LiturgicalCalendar) -> Unit
) {
    StepScaffold(
        iconTint = Rose500,
        title = "Liturgical\ncalendar?",
        subtitle = "We'll surface seasonal packs and the day on Home"
    ) {
        val options = listOf(
            LiturgicalCalendar.NONE to "None",
            LiturgicalCalendar.WESTERN to "Western",
            LiturgicalCalendar.EASTERN to "Eastern"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = current == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    Text(label, fontSize = 13.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (current) {
                LiturgicalCalendar.NONE ->
                    "No seasonal banners — keep the Home screen plain."
                LiturgicalCalendar.WESTERN ->
                    "Advent, Christmas, Lent, Easter, Ordinary Time — Catholic/Anglican/Lutheran calendar."
                LiturgicalCalendar.EASTERN ->
                    "Great Lent, Pentecostarion, Nativity Fast — Eastern Orthodox calendar."
            },
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 5 — Devotional Author
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DevotionalStep(
    author: DevotionalAuthor,
    spurgeonMorningMin: Int,
    spurgeonEveningMin: Int,
    spurgeonMorningEnabled: Boolean,
    spurgeonEveningEnabled: Boolean,
    bonhoefferMin: Int,
    onAuthorChange: (DevotionalAuthor) -> Unit,
    onSpurgeonMorningMinChange: (Int) -> Unit,
    onSpurgeonEveningMinChange: (Int) -> Unit,
    onSpurgeonMorningEnabledChange: (Boolean) -> Unit,
    onSpurgeonEveningEnabledChange: (Boolean) -> Unit,
    onBonhoefferMinChange: (Int) -> Unit
) {
    StepScaffold(
        iconTint = Gold700,
        title = "A daily\ndevotional?",
        subtitle = "A short classical reading each morning or evening"
    ) {
        val options = listOf(
            DevotionalAuthor.NONE to "None",
            DevotionalAuthor.SPURGEON to "Spurgeon",
            DevotionalAuthor.BONHOEFFER to "Bonhoeffer",
            DevotionalAuthor.BOTH to "Both"
        )
        // "Bonhoeffer" is 10 chars — at 12sp in a quarter-width segment it wraps
        // to two lines ("Bonhoeff | er") and blows up that one button's height,
        // visually breaking the pill row. Drop to 11sp + force single line +
        // no soft-wrap. All four labels fit cleanly even on narrow phones.
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = author == value,
                    onClick = { onAuthorChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (author == DevotionalAuthor.SPURGEON || author == DevotionalAuthor.BOTH) {
            Spacer(modifier = Modifier.height(16.dp))

            // Spurgeon's "Morning and Evening" gives two readings per day.
            // Each half has its own time picker AND an independent enable
            // toggle so a user who only wants the evening reading (or the
            // morning) isn't woken at the other slot.
            ToggleableTimeBlock(
                label = "Spurgeon morning reading",
                enabled = spurgeonMorningEnabled,
                minuteOfDay = spurgeonMorningMin,
                onEnabledChange = onSpurgeonMorningEnabledChange,
                onMinuteChange = onSpurgeonMorningMinChange
            )
            Spacer(modifier = Modifier.height(8.dp))
            ToggleableTimeBlock(
                label = "Spurgeon evening reading",
                enabled = spurgeonEveningEnabled,
                minuteOfDay = spurgeonEveningMin,
                onEnabledChange = onSpurgeonEveningEnabledChange,
                onMinuteChange = onSpurgeonEveningMinChange
            )
        }

        if (author == DevotionalAuthor.BONHOEFFER || author == DevotionalAuthor.BOTH) {
            Spacer(modifier = Modifier.height(12.dp))
            TimeBlock(
                label = "Bonhoeffer delivery time",
                minuteOfDay = bonhoefferMin,
                onChange = onBonhoefferMinChange
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (author) {
                DevotionalAuthor.NONE ->
                    "Pick later in Settings if you change your mind."
                DevotionalAuthor.SPURGEON ->
                    "Charles Spurgeon's Morning and Evening — 365 public-domain readings in two daily slots."
                DevotionalAuthor.BONHOEFFER ->
                    "Dietrich Bonhoeffer's evening reflections — deep, challenging companion."
                DevotionalAuthor.BOTH ->
                    "Two voices across the day. Spurgeon morning + evening plus a Bonhoeffer reflection."
            },
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/**
 * Compact time block with an inline enable switch. Used for the Spurgeon
 * morning and evening slots where each half can be turned off independently.
 * Keeps the layout flat (no nested cards) so the onboarding step stays
 * scannable.
 */
@Composable
private fun ToggleableTimeBlock(
    label: String,
    enabled: Boolean,
    minuteOfDay: Int,
    onEnabledChange: (Boolean) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            TextButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onMinuteChange(h * 60 + m) },
                        minuteOfDay / 60,
                        minuteOfDay % 60,
                        false
                    ).show()
                },
                enabled = enabled,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 0.dp, vertical = 2.dp
                )
            ) {
                Text(
                    text = formatTime(minuteOfDay),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Gold700 else Ink.copy(alpha = 0.4f)
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 6 — Reminders + Quiet Hours + POST_NOTIFICATIONS prompt
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RemindersStep(
    morningEnabled: Boolean, morningMin: Int,
    middayEnabled: Boolean, middayMin: Int,
    eveningEnabled: Boolean, eveningMin: Int,
    quietEnabled: Boolean, quietStartMin: Int, quietEndMin: Int,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onMorningChange: (Boolean, Int) -> Unit,
    onMiddayChange: (Boolean, Int) -> Unit,
    onEveningChange: (Boolean, Int) -> Unit,
    onQuietEnabledChange: (Boolean) -> Unit,
    onQuietWindowChange: (Int, Int) -> Unit
) {
    StepScaffold(
        iconTint = Indigo500,
        title = "When should\nwe nudge you?",
        subtitle = "Reminders and Quiet Hours"
    ) {
        // Notification permission banner (API 33+ only)
        if (!permissionGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Rose500.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Notifications are turned off",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Rose500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We need permission to send reminders and your devotional.",
                        fontSize = 13.sp,
                        color = Ink.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rose500,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Allow notifications")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        ReminderRow(
            label = "Morning",
            enabled = morningEnabled,
            minuteOfDay = morningMin,
            onToggle = { onMorningChange(it, morningMin) },
            onTimeChange = { onMorningChange(morningEnabled, it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReminderRow(
            label = "Midday",
            enabled = middayEnabled,
            minuteOfDay = middayMin,
            onToggle = { onMiddayChange(it, middayMin) },
            onTimeChange = { onMiddayChange(middayEnabled, it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReminderRow(
            label = "Evening",
            enabled = eveningEnabled,
            minuteOfDay = eveningMin,
            onToggle = { onEveningChange(it, eveningMin) },
            onTimeChange = { onEveningChange(eveningEnabled, it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quiet Hours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = quietEnabled,
                onCheckedChange = onQuietEnabledChange
            )
        }

        if (quietEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeButton(
                    label = "Start",
                    minuteOfDay = quietStartMin,
                    onChange = { onQuietWindowChange(it, quietEndMin) },
                    modifier = Modifier.weight(1f)
                )
                TimeButton(
                    label = "End",
                    minuteOfDay = quietEndMin,
                    onChange = { onQuietWindowChange(quietStartMin, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(
    label: String,
    enabled: Boolean,
    minuteOfDay: Int,
    onToggle: (Boolean) -> Unit,
    onTimeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Indigo500.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.width(80.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            if (enabled) {
                TimeButton(
                    label = "At",
                    minuteOfDay = minuteOfDay,
                    onChange = onTimeChange,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Off",
                    fontSize = 14.sp,
                    color = Ink.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 7 — First Prayer Collection
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FirstCollectionStep(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    StepScaffold(
        iconTint = GratitudeGreen,
        title = "Your first\nprayer list",
        subtitle = "A place to gather the people and things on your heart"
    ) {
        TextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("List name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Optional description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "You can add prayer items to this list as soon as you're inside the app.",
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 8 — First Gratitude Entry (optional)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FirstGratitudeStep(
    text: String,
    onTextChange: (String) -> Unit
) {
    StepScaffold(
        iconTint = GratitudeGreen,
        title = "One thing\nyou're thankful for",
        subtitle = "Start your gratitude catalogue right now (optional)"
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("I'm thankful for…") },
            placeholder = { Text("One moment, one person, one blessing") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Skipping is fine — you can start your gratitude log any time from Home.",
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared scaffolding — icon halo, title, subtitle, step body
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun StepScaffold(
    iconTint: Color,
    title: String,
    subtitle: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = iconTint
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = iconTint,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        content()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Time picker button (12-hour format) and helpers
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TimeBlock(
    label: String,
    minuteOfDay: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Ink.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        TimeButton(
            label = "At",
            minuteOfDay = minuteOfDay,
            onChange = onChange,
            modifier = Modifier.width(140.dp)
        )
    }
}

@Composable
private fun TimeButton(
    label: String,
    minuteOfDay: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, pickedHour, pickedMinute ->
                    onChange(pickedHour * 60 + pickedMinute)
                },
                hour,
                minute,
                false
            ).show()
        },
        modifier = modifier
    ) {
        Text(
            text = "$label: ${formatTime(minuteOfDay)}",
            fontSize = 13.sp
        )
    }
}

private fun formatTime(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val ampm = if (h < 12) "AM" else "PM"
    return "%d:%02d %s".format(hour12, m, ampm)
}
