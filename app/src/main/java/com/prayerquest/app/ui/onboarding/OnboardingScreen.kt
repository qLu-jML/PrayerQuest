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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

// ═══════════════════════════════════════════════════════════════════════════
// Step enumeration — DD §3.2
// ═══════════════════════════════════════════════════════════════════════════

private const val STEP_WELCOME = 0
private const val STEP_NAME_GOAL = 1
private const val STEP_TRADITIONS = 2
private const val STEP_LITURGICAL = 3
private const val STEP_REMINDERS = 4
private const val STEP_FIRST_COLLECTION = 5
private const val STEP_FIRST_GRATITUDE = 6
private const val TOTAL_STEPS = 7

// ═══════════════════════════════════════════════════════════════════════════
// Main Onboarding Screen
// ═══════════════════════════════════════════════════════════════════════════

/**
 * 7-step data-collecting onboarding flow (DD §3.2). Unlike the old tutorial
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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Always-visible back arrow. On step 0 we keep the slot but hide
            // the icon so the layout stays balanced; on every other step it's
            // a proper IconButton with the ArrowBack glyph (per user feedback:
            // "add visible back arrows to the onboarding screens").
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Ink.copy(alpha = 0.75f)
                        )
                    }
                }
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
            // Per user feedback: this used to be a low-contrast TextButton that
            // users didn't notice. Now it's an OutlinedButton with Indigo700
            // border + bold label so it reads as a real affordance.
            if (step in STEP_NAME_GOAL..STEP_FIRST_GRATITUDE) {
                OutlinedButton(
                    onClick = {
                        if (step == TOTAL_STEPS - 1) {
                            scope.launch {
                                viewModel.completeOnboarding()
                                onNavigateToHome()
                            }
                        } else {
                            step = minOf(step + 1, TOTAL_STEPS - 1)
                        }
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = Indigo700.copy(alpha = 0.6f)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 14.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.common_skip),
                        color = Indigo700,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))  // balance the row
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
                        onDescriptionChange = viewModel::setFirstCollectionDescription,
                        onSkipToApp = {
                            // User wants to just enter the app. Commit now
                            // with whatever defaults are in place — we'll
                            // seed the starter collection with the placeholder
                            // name and nothing more — and jump home.
                            scope.launch {
                                viewModel.completeOnboarding()
                                onNavigateToHome()
                            }
                        }
                    )
                    STEP_FIRST_GRATITUDE -> FirstGratitudeStep(
                        text = answers.firstGratitudeText,
                        onTextChange = viewModel::setFirstGratitudeText,
                        onSkipToApp = {
                            // Skip gratitude and enter the app. Identical to
                            // the primary button except no gratitude is saved
                            // (the VM already handles blank text gracefully).
                            scope.launch {
                                viewModel.completeOnboarding()
                                onNavigateToHome()
                            }
                        }
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
                        STEP_WELCOME -> stringResource(R.string.onboarding_let_s_begin)
                        TOTAL_STEPS - 1 -> stringResource(R.string.onboarding_begin_praying)
                        else -> stringResource(R.string.common_next)
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
        title = stringResource(R.string.onboarding_welcome_to_nprayerquest),
        subtitle = stringResource(R.string.onboarding_your_gentle_companion_for_a_deeper_prayer_life)
    ) {
        Text(
            text = stringResource(R.string.onboarding_over_the_next_few_steps_we_ll_set_up_your_prayer_r) +
                    stringResource(R.string.onboarding_your_name_daily_goal_how_you_like_to_pray_and_when) +
                    stringResource(R.string.onboarding_nudge_you_everything_is_adjustable_later),
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
        title = stringResource(R.string.onboarding_who_are_you_npraying_as),
        subtitle = stringResource(R.string.onboarding_name_and_daily_goal)
    ) {
        TextField(
            value = displayName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_your_name)) },
            placeholder = { Text(stringResource(R.string.common_prayer_warrior)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.onboarding_daily_prayer_goal_minutes),
            style = MaterialTheme.typography.labelMedium,
            color = Ink.copy(alpha = 0.75f),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Fixed 4-column × 2-row grid. FlowRow packed pills of uneven widths
        // ("3 min" vs "60 min") which made each row look ragged; giving every
        // pill an equal weight in a Row lines them up perfectly.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(3, 5, 10, 15, 20, 30, 45, 60).chunked(4).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowOptions.forEach { option ->
                        GoalPill(
                            value = option,
                            isSelected = goalMinutes == option,
                            onClick = { onGoalChange(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_any_amount_counts_even_three_minutes_of_honest_pra),
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun GoalPill(
    value: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // clip must come BEFORE clickable so the ripple respects the rounded shape,
    // and clickable must come BEFORE padding so the whole pill is tappable.
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) Indigo700 else Indigo700.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_x_min, value),
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
        title = stringResource(R.string.onboarding_how_do_nyou_pray),
        subtitle = stringResource(R.string.onboarding_pick_any_that_speak_to_you)
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
            text = stringResource(R.string.onboarding_this_tunes_which_prayer_modes_appear_first_you_can) +
                    stringResource(R.string.onboarding_regardless_traditions_just_shape_the_defaults),
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
        title = stringResource(R.string.onboarding_liturgical_ncalendar),
        subtitle = stringResource(R.string.onboarding_we_ll_surface_seasonal_packs_and_the_day_on_home)
    ) {
        val options = listOf(
            LiturgicalCalendar.NONE to stringResource(R.string.common_none),
            LiturgicalCalendar.WESTERN to stringResource(R.string.common_western),
            LiturgicalCalendar.EASTERN to stringResource(R.string.common_eastern)
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
                    stringResource(R.string.onboarding_no_seasonal_banners_keep_the_home_screen_plain)
                LiturgicalCalendar.WESTERN ->
                    stringResource(R.string.onboarding_advent_christmas_lent_easter_ordinary_time_catholi)
                LiturgicalCalendar.EASTERN ->
                    stringResource(R.string.onboarding_great_lent_pentecostarion_nativity_fast_eastern_or)
            },
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 5 — Reminders + Quiet Hours + POST_NOTIFICATIONS prompt
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
        title = stringResource(R.string.onboarding_when_should_nwe_nudge_you),
        subtitle = stringResource(R.string.onboarding_reminders_and_quiet_hours)
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
                        text = stringResource(R.string.onboarding_notifications_are_turned_off),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Rose500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_we_need_permission_to_send_prayer_reminders),
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
                        Text(stringResource(R.string.common_allow_notifications))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        ReminderRow(
            label = stringResource(R.string.onboarding_morning),
            enabled = morningEnabled,
            minuteOfDay = morningMin,
            onToggle = { onMorningChange(it, morningMin) },
            onTimeChange = { onMorningChange(morningEnabled, it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReminderRow(
            label = stringResource(R.string.onboarding_midday),
            enabled = middayEnabled,
            minuteOfDay = middayMin,
            onToggle = { onMiddayChange(it, middayMin) },
            onTimeChange = { onMiddayChange(middayEnabled, it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        ReminderRow(
            label = stringResource(R.string.onboarding_evening),
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
                text = stringResource(R.string.settings_quiet_hours),
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
                    label = stringResource(R.string.common_start),
                    minuteOfDay = quietStartMin,
                    onChange = { onQuietWindowChange(it, quietEndMin) },
                    modifier = Modifier.weight(1f)
                )
                TimeButton(
                    label = stringResource(R.string.common_end),
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
                    label = stringResource(R.string.onboarding_at),
                    minuteOfDay = minuteOfDay,
                    onChange = onTimeChange,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.onboarding_off),
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
// Step 6 — First Prayer Collection
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FirstCollectionStep(
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSkipToApp: () -> Unit
) {
    StepScaffold(
        iconTint = GratitudeGreen,
        title = stringResource(R.string.onboarding_your_first_nprayer_list),
        subtitle = stringResource(R.string.onboarding_create_one_now_or_jump_straight_into_the_app)
    ) {
        TextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_list_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.onboarding_optional_description)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_tap_next_below_to_create_this_list_or_skip_and_add),
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Prominent "no thanks, just let me in" escape hatch. Matches feedback
        // that some users want to poke around the app before committing to a
        // list structure.
        OutlinedButton(
            onClick = onSkipToApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip_take_me_into_the_app),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Step 7 — First Gratitude Entry (optional)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FirstGratitudeStep(
    text: String,
    onTextChange: (String) -> Unit,
    onSkipToApp: () -> Unit
) {
    StepScaffold(
        iconTint = GratitudeGreen,
        title = stringResource(R.string.onboarding_one_thing_nyou_re_thankful_for),
        subtitle = stringResource(R.string.onboarding_start_your_gratitude_catalogue_right_now_optional)
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text(stringResource(R.string.onboarding_i_m_thankful_for)) },
            placeholder = { Text(stringResource(R.string.onboarding_one_moment_one_person_one_blessing)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_skipping_is_fine_you_can_start_your_gratitude_log),
            fontSize = 13.sp,
            color = Ink.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Prominent in-content skip. The small "Skip" link in the top bar was
        // easy to miss; users asked for a clearly labeled button on the step
        // itself that ends onboarding without pressuring them to type a
        // gratitude entry right now.
        OutlinedButton(
            onClick = onSkipToApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip_for_now_begin_praying),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
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
            text = stringResource(R.string.common_x_x_2, label, formatTime(minuteOfDay)),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun formatTime(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val ampm = if (h < 12) "AM" else "PM"
    return stringResource(R.string.common_d_02d_s).format(hour12, m, ampm)
}
