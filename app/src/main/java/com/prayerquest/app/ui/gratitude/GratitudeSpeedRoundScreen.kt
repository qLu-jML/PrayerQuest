package com.prayerquest.app.ui.gratitude

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.gratitude.GratitudePromptsLoader
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.ui.theme.GratitudeGold
import com.prayerquest.app.ui.theme.GratitudeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Gratitude Speed Round (DD §3.6). 60-second rapid-fire gratitude logger
 * that writes every entry into the same `gratitude_entries` table the
 * Daily Log uses — no separate store. Finishing under 60 seconds with
 * five or more entries earns a flat speed-bonus XP on top of the
 * per-entry XP; the bonus is awarded server-side by
 * [GamificationRepository.onGratitudeLogged] via the `speedBonus` flag.
 *
 * Interaction model:
 *   * Tap a chip → a new GratitudeEntry is staged locally with the chip
 *     text as its seed text. Entries don't persist to Room until the
 *     round ends (so the user can bail without creating junk rows).
 *   * Hold the mic → SpeechRecognizer runs a pass; on a natural pause
 *     the final transcript is staged as a new entry.
 *   * 60s timer exhausts OR user taps "Finish" → all staged entries are
 *     flushed to Room in one batch and the result screen renders.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GratitudeSpeedRoundScreen(
    onNavigateBack: () -> Unit,
    onCompleted: (entriesLogged: Int, xpEarned: Int, speedBonus: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerQuestApplication
    val viewModel: GratitudeSpeedRoundViewModel = viewModel(
        factory = GratitudeSpeedRoundViewModel.Factory(
            app.container.gratitudeRepository,
            app.container.gamificationRepository
        )
    )

    val chips by viewModel.chips.collectAsState()
    LaunchedEffect(Unit) {
        if (chips.isEmpty()) {
            viewModel.loadChips { GratitudePromptsLoader.loadSpeedChips(context) }
        }
    }
    val entries = remember { mutableStateListOf<String>() }
    var secondsRemaining by remember { mutableIntStateOf(TOTAL_SECONDS) }
    var roundStarted by remember { mutableStateOf(false) }
    var roundFinished by remember { mutableStateOf(false) }
    var voicePartial by remember { mutableStateOf("") }

    // ──────────────────────────────────────────────────────────────────
    // Mic permission + SpeechRecognizer wiring. If the permission isn't
    // granted yet we ask on first mic-press; the user can still log via
    // chips without mic access.
    // ──────────────────────────────────────────────────────────────────
    var micPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micPermissionGranted = granted }

    val recognizer = remember {
        SpeedRoundVoiceRecognizer(
            context = context,
            onPartialResult = { partial -> voicePartial = partial },
            onFinalResult = { text ->
                voicePartial = ""
                if (!roundFinished && entries.size < MAX_ENTRIES) {
                    entries.add(text)
                }
            }
        )
    }
    DisposableEffect(Unit) {
        onDispose { recognizer.destroy() }
    }

    // ──────────────────────────────────────────────────────────────────
    // Countdown timer. Lives inside LaunchedEffect keyed on roundStarted
    // so we only start ticking once the user has interacted — a dead
    // 60s clock on a screen nobody uses is the worst kind of UX regret.
    // ──────────────────────────────────────────────────────────────────
    LaunchedEffect(roundStarted) {
        if (!roundStarted) return@LaunchedEffect
        while (secondsRemaining > 0 && !roundFinished) {
            delay(1000L)
            secondsRemaining = (secondsRemaining - 1).coerceAtLeast(0)
        }
        if (!roundFinished) {
            // Auto-finish on timer exhaustion.
            roundFinished = true
            recognizer.cancel()
        }
    }

    val scope = rememberCoroutineScope()

    val startedAt = remember { mutableStateOf(0L) }
    LaunchedEffect(roundStarted) {
        if (roundStarted && startedAt.value == 0L) startedAt.value = System.currentTimeMillis()
    }

    fun finishRound() {
        if (roundFinished) return
        roundFinished = true
        recognizer.cancel()
        val elapsedMs = if (startedAt.value > 0L)
            System.currentTimeMillis() - startedAt.value
        else
            (TOTAL_SECONDS * 1000L - secondsRemaining * 1000L)
        val underTime = elapsedMs in 1..SPEED_BONUS_WINDOW_MS
        val earnedBonus = underTime && entries.size >= SPEED_BONUS_MIN_ENTRIES
        scope.launch {
            val xp = viewModel.commitRound(entries.toList(), speedBonus = earnedBonus)
            onCompleted(entries.size, xp, earnedBonus)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp)
        ) {
            // ═══════════════════════════════════════════════════════
            // Top bar: close + title + live counter
            // ═══════════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close Speed Round")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Speed Round",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rapid-fire gratitude",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GratitudeGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${entries.size} thanks logged",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = GratitudeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ═══════════════════════════════════════════════════════
            // Countdown timer
            // ═══════════════════════════════════════════════════════
            SpeedRoundTimer(
                secondsRemaining = secondsRemaining,
                totalSeconds = TOTAL_SECONDS,
                roundStarted = roundStarted,
                roundFinished = roundFinished
            )

            Spacer(Modifier.height(12.dp))

            // ═══════════════════════════════════════════════════════
            // Voice mic row
            // ═══════════════════════════════════════════════════════
            if (recognizer.isAvailable()) {
                VoiceMicRow(
                    isListening = recognizer.isListening,
                    partialTranscript = voicePartial,
                    onPressStart = {
                        if (!micPermissionGranted) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@VoiceMicRow
                        }
                        if (!roundStarted) roundStarted = true
                        if (roundFinished || entries.size >= MAX_ENTRIES) return@VoiceMicRow
                        recognizer.start()
                    },
                    onPressEnd = { recognizer.cancel() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ═══════════════════════════════════════════════════════
            // Chip bank
            // ═══════════════════════════════════════════════════════
            Text(
                text = "Tap a chip to log instantly",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { chip ->
                    val alreadyLogged = entries.contains(chip.text)
                    AssistChip(
                        onClick = {
                            if (roundFinished) return@AssistChip
                            if (!roundStarted) roundStarted = true
                            if (entries.size >= MAX_ENTRIES) return@AssistChip
                            if (!alreadyLogged) entries.add(chip.text)
                        },
                        label = {
                            Text(
                                text = chip.text,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (alreadyLogged)
                                GratitudeGreen.copy(alpha = 0.18f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (alreadyLogged) GratitudeGreen else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (alreadyLogged) 1.dp else 0.dp,
                            color = if (alreadyLogged) GratitudeGreen else Color.Transparent
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ═══════════════════════════════════════════════════════
            // Most-recent entry preview — reassures the user their
            // last tap/spoken phrase landed.
            // ═══════════════════════════════════════════════════════
            AnimatedVisibility(visible = entries.isNotEmpty()) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Just logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = entries.lastOrNull().orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ═══════════════════════════════════════════════════════
            // Finish button — locks the round and flushes to Room.
            // ═══════════════════════════════════════════════════════
            Button(
                onClick = { finishRound() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GratitudeGold),
                enabled = !roundFinished && entries.isNotEmpty()
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                // Horizontal gap — we're inside Button's RowScope, so
                // `height` here does nothing. Switched to `width`.
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (roundFinished) "Finishing…" else "Finish & Earn XP",
                    color = Color(0xFF3E2723),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Sub-composables
// ──────────────────────────────────────────────────────────────────────

@Composable
private fun SpeedRoundTimer(
    secondsRemaining: Int,
    totalSeconds: Int,
    roundStarted: Boolean,
    roundFinished: Boolean
) {
    val fraction by animateFloatAsState(
        targetValue = secondsRemaining.toFloat() / totalSeconds.toFloat(),
        label = "speedTimerFraction"
    )
    val timerColor = when {
        roundFinished -> MaterialTheme.colorScheme.onSurfaceVariant
        secondsRemaining <= 10 -> MaterialTheme.colorScheme.error
        secondsRemaining <= 30 -> GratitudeGold
        else -> GratitudeGreen
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (roundStarted) "${secondsRemaining}s" else "60s",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = timerColor
            )
            Text(
                text = when {
                    roundFinished -> "Round complete"
                    !roundStarted -> "Tap a chip or hold the mic to begin"
                    else -> "Go!"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = timerColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun VoiceMicRow(
    isListening: Boolean,
    partialTranscript: String,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isListening) GratitudeGreen else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(56.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    )
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Hold to speak a gratitude",
                    tint = if (isListening) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isListening) "Listening…" else "Hold to speak",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = partialTranscript.ifEmpty { "Each pause logs a new gratitude." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// ViewModel
// ──────────────────────────────────────────────────────────────────────

class GratitudeSpeedRoundViewModel(
    private val gratitudeRepository: GratitudeRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _chips = MutableStateFlow<List<GratitudePromptsLoader.SpeedChip>>(emptyList())
    val chips = _chips.asStateFlow()

    fun loadChips(loader: suspend () -> List<GratitudePromptsLoader.SpeedChip>) {
        viewModelScope.launch {
            _chips.value = withContext(Dispatchers.IO) { loader() }
        }
    }

    /**
     * Writes every staged entry to Room as one batch, then fires the
     * gamification hot path with the speed-bonus flag. Returns the XP
     * awarded so the caller can surface it in a toast.
     */
    suspend fun commitRound(
        entries: List<String>,
        speedBonus: Boolean
    ): Int {
        if (entries.isEmpty()) return 0
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val rows = entries.map { text ->
            GratitudeEntry(
                date = today,
                text = text,
                category = GratitudeEntry.CATEGORY_OTHER,
                photoUri = null
            )
        }
        gratitudeRepository.addAll(rows)
        return gamificationRepository.onGratitudeLogged(
            count = entries.size,
            hasPhoto = false,
            speedBonus = speedBonus
        )
    }

    class Factory(
        private val gratitudeRepository: GratitudeRepository,
        private val gamificationRepository: GamificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GratitudeSpeedRoundViewModel(gratitudeRepository, gamificationRepository) as T
        }
    }
}

// ──────────────────────────────────────────────────────────────────────
// Tunables
// ──────────────────────────────────────────────────────────────────────

/** Total round length. Keep in sync with the copy on the start screen. */
private const val TOTAL_SECONDS = 60

/**
 * Cap on how many entries a single round can stage. Higher caps risk
 * cluttering the Catalogue with noise-entries; lower caps punish users
 * who are genuinely on a roll. 15 is the "feels generous but not silly"
 * middle ground.
 */
private const val MAX_ENTRIES = 15

/** "Grateful heart, fast hands!" speed bonus eligibility window. */
private const val SPEED_BONUS_WINDOW_MS = 60_000L

/** Minimum entry count for speed bonus (DD §3.6 says "≥5 entries"). */
private const val SPEED_BONUS_MIN_ENTRIES = 5
