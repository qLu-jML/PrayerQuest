package com.prayerquest.app.ui.celebration

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.domain.content.CelebrationVerse
import com.prayerquest.app.domain.content.CelebrationVerses
import com.prayerquest.app.domain.model.AchievementDef
import com.prayerquest.app.notifications.AnsweredPrayerAnniversaryWorker
import com.prayerquest.app.ui.theme.Gold300
import com.prayerquest.app.ui.theme.Gold500
import com.prayerquest.app.ui.theme.Indigo500
import com.prayerquest.app.ui.theme.Rose300
import com.prayerquest.app.ui.theme.Rose500
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * The Big Celebration Moment (DD §3.5.2).
 *
 * Full-screen modal shown immediately after a user marks a PrayerItem as
 * "Answered" — and only then. Items that were imported already-answered
 * or re-flagged on an already-answered row do NOT reach this screen
 * (gated upstream via [PrayerRepository.MarkAnsweredResult.wasNewlyAnswered]).
 * Partially-answered items also never reach this screen — they route
 * back to the Answered Prayer Detail directly.
 *
 * What happens here, in order:
 *  1. Confetti burst — drawn on [Canvas] with a lightweight particle
 *     system. Animates for ~3s as a concentrated burst, then settles
 *     into a gentle indefinite fall that plays until the user dismisses.
 *  2. "Praise God!" display title — springs in from 0.7× scale.
 *  3. Auto-selected thanksgiving verse card — a warm gradient card.
 *  4. CTAs: "Share testimony" (system share sheet on a rendered bitmap
 *     reproducing the verse + prayer title) and "Close" (dismiss back
 *     to the previous screen).
 *
 * The screen schedules the +365 day anniversary worker on first composition
 * (idempotent via the worker's unique-name KEEP policy) and kicks off the
 * gamification hook once per instance.
 */
@Composable
fun AnsweredCelebrationScreen(
    prayerId: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerQuestApplication

    val viewModel: AnsweredCelebrationViewModel = viewModel(
        // Keying on prayerId makes the VM stable across recompositions but
        // rebuilt for a different prayer — matches the pattern used in
        // AnsweredPrayerDetailViewModel.
        key = "celebration-$prayerId",
        factory = AnsweredCelebrationViewModel.Factory(
            prayerRepository = app.container.prayerRepository,
            gamificationRepository = app.container.gamificationRepository,
            prayerId = prayerId
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // Fire the one-shot side effects exactly once per celebration: schedule
    // the anniversary worker and award XP / evaluate achievements. Keying
    // on prayerId matches the VM and keeps us from double-awarding if the
    // user rotates the device mid-celebration.
    LaunchedEffect(prayerId) {
        AnsweredPrayerAnniversaryWorker.schedule(
            context = app,
            prayerItemId = prayerId
            // answeredAtMs defaults to "now" which is accurate — we only
            // reach this screen on a fresh Answered transition.
        )
        viewModel.onCelebrationShown()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f))
    ) {
        // Confetti is decorative — never read out by accessibility services.
        ConfettiLayer(
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics {}
        )

        CelebrationContent(
            prayer = (uiState as? AnsweredCelebrationViewModel.UiState.Loaded)?.prayer,
            verse = uiState.verse,
            newAchievements = (uiState as? AnsweredCelebrationViewModel.UiState.Loaded)?.newAchievements
                ?: emptyList(),
            onShareTestimony = {
                val prayer = (uiState as? AnsweredCelebrationViewModel.UiState.Loaded)?.prayer
                    ?: return@CelebrationContent
                viewModel.shareTestimony(
                    context = context,
                    prayer = prayer
                )
            },
            onClose = onDismiss,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Content layer: Praise God headline + verse card + CTAs.
// Extracted so the celebration layout is independent of state plumbing.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CelebrationContent(
    prayer: PrayerItem?,
    verse: CelebrationVerse,
    newAchievements: List<AchievementDef>,
    onShareTestimony: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headlineScale = remember { Animatable(0.7f) }
    val headlineAlpha = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Headline springs in first — the emotional payoff is the giant
        // "Praise God!" so it lands before anything else settles.
        headlineAlpha.animateTo(1f, tween(300))
        headlineScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        cardAlpha.animateTo(1f, tween(400))
        buttonsAlpha.animateTo(1f, tween(300))
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Resolve at @Composable level — `semantics { }` is non-@Composable.
        val praiseHeadlineA11y = stringResource(R.string.celebration_praise_god_your_prayer_was_answered)
        Text(
            text = stringResource(R.string.celebration_praise_god),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 72.sp,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(headlineAlpha.value)
                .scale(headlineScale.value)
                .semantics {
                    contentDescription = praiseHeadlineA11y
                }
        )

        VerseCard(
            verse = verse,
            prayerTitle = prayer?.title.orEmpty(),
            modifier = Modifier
                .alpha(cardAlpha.value)
                .fillMaxWidth()
                .widthIn(max = 420.dp)
        )

        if (newAchievements.isNotEmpty()) {
            NewAchievementsStrip(
                achievements = newAchievements,
                modifier = Modifier
                    .alpha(cardAlpha.value)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )
        }

        Column(
            modifier = Modifier
                .alpha(buttonsAlpha.value)
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onShareTestimony,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = Color(0xFF2D2B29)
                ),
                enabled = prayer != null
            ) {
                Text(
                    text = stringResource(R.string.celebration_share_testimony),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.common_close),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Verse card — warm gradient + Scripture. The shared bitmap rebuilds a
// similar layout in native Canvas so we don't depend on capturing the
// Compose tree (which is finicky across API levels).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VerseCard(
    verse: CelebrationVerse,
    prayerTitle: String,
    modifier: Modifier = Modifier
) {
    val contentDesc = stringResource(R.string.celebration_scripture_card_x_x, verse.text, verse.reference) +
            if (prayerTitle.isNotBlank()) stringResource(R.string.celebration_prayer_x, prayerTitle) else ""

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .semantics { contentDescription = contentDesc },
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Gold300,
                            Rose300,
                            Color(0xFFFFF8F0)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "\u201C${verse.text}\u201D",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                        color = Color(0xFF2D2B29)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.common_x, verse.reference),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A4743)
                    ),
                    textAlign = TextAlign.Center
                )
                if (prayerTitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.celebration_answered_prayer_x, prayerTitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF4A4743)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Newly-unlocked achievements — tiny celebratory strip above the CTAs.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NewAchievementsStrip(
    achievements: List<AchievementDef>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0x33FFFFFF),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = pluralStringResource(R.plurals.celebration_new_achievements_unlocked, achievements.size),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            achievements.forEach { ach ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = ach.icon.ifBlank { "🏆" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Column {
                        Text(
                            text = ach.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = ach.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Confetti — lightweight particle system. No third-party dep.
//
// 80 particles: dense enough to feel celebratory, cheap enough to render
// at 60fps on low-end devices. Two phases — a 3s burst from the top center
// (party-popper feel), then a steady-state gentle fall that plays until
// the user dismisses.
// ─────────────────────────────────────────────────────────────────────────────

private data class ConfettiParticle(
    val colorIndex: Int,
    val xSeed: Float,
    val vxSeed: Float,
    val vySeed: Float,
    val rotateSeed: Float,
    val size: Float,
    val lifeOffset: Float
)

@Composable
private fun ConfettiLayer(modifier: Modifier = Modifier) {
    val palette = remember {
        listOf(
            Gold500, Gold300, Rose500, Rose300, Indigo500,
            Color(0xFF66BB6A),   // gratitude green
            Color(0xFFFFD54F)    // gratitude gold
        )
    }
    val particles = remember {
        List(80) { idx ->
            val rng = Random(idx * 31 + 7)
            ConfettiParticle(
                colorIndex = rng.nextInt(0, palette.size),
                xSeed = rng.nextFloat(),
                vxSeed = (rng.nextFloat() - 0.5f) * 2f,
                vySeed = rng.nextFloat(),
                rotateSeed = rng.nextFloat(),
                size = 6f + rng.nextFloat() * 6f,
                lifeOffset = rng.nextFloat()
            )
        }
    }

    // Burst phase: 0→1 over 3s.
    val burstProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        burstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
        )
    }

    // Steady-state fall: infinite 4s loop.
    val transition = rememberInfiniteTransition(label = "confetti-loop")
    val loopT by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti-loop-t"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val burstT = burstProgress.value
        val burstWeight = 1f - burstT

        particles.forEach { p ->
            val burstX = w * 0.5f + (p.vxSeed * 0.45f * w) * burstT
            val burstY = (-20f) + (h + 40f) * (burstT * (0.4f + 0.6f * p.vySeed))
            val burstAngle = 720f * burstT * (if (p.vxSeed < 0) -1f else 1f)

            val life = ((loopT + p.lifeOffset) % 1f)
            val steadyX = (p.xSeed * 1.1f - 0.05f) * w +
                    kotlin.math.sin(life * 6.283f + p.rotateSeed * 3f) * 16f
            val steadyY = -20f + (h + 40f) * life
            val steadyAngle = 360f * (life * 2f + p.rotateSeed)

            val x = burstX * burstWeight + steadyX * (1f - burstWeight)
            val y = burstY * burstWeight + steadyY * (1f - burstWeight)
            val angle = burstAngle * burstWeight + steadyAngle * (1f - burstWeight)

            drawConfetti(
                x = x,
                y = y,
                sizeDp = p.size,
                angleDeg = angle,
                color = palette[p.colorIndex]
            )
        }
    }
}

private fun DrawScope.drawConfetti(
    x: Float,
    y: Float,
    sizeDp: Float,
    angleDeg: Float,
    color: Color
) {
    // Confetti piece = tiny rotated rectangle. Reads as mylar on screen.
    val halfW = sizeDp * density * 0.5f
    val halfH = sizeDp * density * 0.2f
    rotate(degrees = angleDeg, pivot = Offset(x, y)) {
        drawRect(
            color = color,
            topLeft = Offset(x - halfW, y - halfH),
            size = androidx.compose.ui.geometry.Size(halfW * 2f, halfH * 2f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel + bitmap rendering + sharing
// ─────────────────────────────────────────────────────────────────────────────

class AnsweredCelebrationViewModel(
    private val prayerRepository: PrayerRepository,
    private val gamificationRepository: GamificationRepository,
    private val prayerId: Long
) : ViewModel() {

    /**
     * Celebration is strictly a presentation surface — it either has the
     * prayer + verse (Loaded) or it hasn't loaded yet (Loading). We don't
     * model "NotFound" separately because the caller already guaranteed
     * a newly-answered transition occurred; if the row disappeared under
     * us (very rare), we fall back to a verse-only view with an empty
     * title, which the layout handles.
     */
    sealed interface UiState {
        val verse: CelebrationVerse

        data class Loading(override val verse: CelebrationVerse) : UiState

        data class Loaded(
            val prayer: PrayerItem?,
            override val verse: CelebrationVerse,
            val newAchievements: List<AchievementDef> = emptyList(),
            val xpEarned: Int = 0
        ) : UiState
    }

    private val pickedVerse: CelebrationVerse = CelebrationVerses.pickForPrayer(prayerId)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading(pickedVerse))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val row = prayerRepository.getItem(prayerId)
            _uiState.value = UiState.Loaded(
                prayer = row,
                verse = pickedVerse
            )
        }
    }

    /**
     * Called once per screen entry. Awards the fixed answered-prayer XP
     * and evaluates achievements (FIRST_ANSWERED / FAITHFUL_10 /
     * FAITHFUL_50) so the celebration can surface anything newly
     * unlocked alongside the verse card.
     */
    fun onCelebrationShown() {
        viewModelScope.launch {
            val result = gamificationRepository.onPrayerMarkedAnswered()
            val current = _uiState.value
            if (current is UiState.Loaded) {
                _uiState.value = current.copy(
                    newAchievements = result.newAchievements,
                    xpEarned = result.xpEarned
                )
            } else {
                _uiState.value = UiState.Loaded(
                    prayer = null,
                    verse = pickedVerse,
                    newAchievements = result.newAchievements,
                    xpEarned = result.xpEarned
                )
            }
        }
    }

    /**
     * Renders a testimony card bitmap and fires the system Share Sheet.
     * Failures are swallowed — sharing is best-effort; we don't want to
     * block the celebration on an I/O hiccup.
     */
    fun shareTestimony(
        context: Context,
        prayer: PrayerItem
    ) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                renderAndWriteTestimonyBitmap(context, prayer)
            } ?: return@launch

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    context.getString(R.string.celebration_praise_god_my_prayer_x_was_answered, prayer.title) +
                            context.getString(R.string.celebration_x_via_prayerquest, pickedVerse.reference)
                )
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.celebration_praise_god_answered_prayer))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(
                sendIntent,
                context.getString(R.string.celebration_share_your_testimony)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(chooser)
            } catch (_: Exception) {
                // No share targets available — silently no-op.
            }
        }
    }

    /**
     * Draws the testimony card directly into a Bitmap using Android's
     * native Canvas, then writes it to cacheDir/shared/celebration/<id>.png
     * and returns a content:// Uri via FileProvider. This avoids the
     * fiddliness of capturing the Compose tree; in exchange the shared
     * image is a hand-drawn re-render of the same gradient + text the
     * user sees on screen.
     *
     * Dimensions are 1080×1350 (4:5) — the aspect ratio Instagram,
     * Facebook, and most chat apps prefer for shared social-card images.
     */
    private fun renderAndWriteTestimonyBitmap(
        context: Context,
        prayer: PrayerItem
    ): android.net.Uri? {
        val width = 1080
        val height = 1350

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.applyCanvas {
            // 1) Warm gradient background — Gold300 → Rose300 → parchment.
            val gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Gold300.toArgb(),
                    Rose300.toArgb(),
                    android.graphics.Color.parseColor("#FFF8F0")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            val bgPaint = Paint().apply {
                isAntiAlias = true
                shader = gradient
            }
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // 2) "Praise God!" headline near the top.
            val headlinePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#2D2B29")
                textSize = 120f
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            drawText(context.getString(R.string.celebration_praise_god), width / 2f, 220f, headlinePaint)

            // 3) Subtitle: "My prayer was answered."
            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#4A4743")
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            drawText(context.getString(R.string.celebration_my_prayer_was_answered), width / 2f, 290f, subtitlePaint)

            // 4) Verse body — wrap within 840px horizontal runs.
            val versePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#2D2B29")
                textSize = 52f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
            }
            val verseText = "\u201C${pickedVerse.text}\u201D"
            val versLines = wrapText(verseText, versePaint, width - 200)
            var y = 500f
            versLines.forEach { line ->
                drawText(line, width / 2f, y, versePaint)
                y += versePaint.textSize * 1.25f
            }

            // 5) Reference line.
            val refPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#4A4743")
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            y += 40f
            drawText(context.getString(R.string.celebration_x, pickedVerse.reference), width / 2f, y, refPaint)

            // 6) Prayer title line (optional).
            if (prayer.title.isNotBlank()) {
                val titlePaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#4A4743")
                    textSize = 38f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }
                y += 80f
                val titleLines = wrapText(
                    context.getString(R.string.celebration_answered_prayer_x_2, prayer.title),
                    titlePaint,
                    width - 160
                )
                titleLines.forEach { line ->
                    drawText(line, width / 2f, y, titlePaint)
                    y += titlePaint.textSize * 1.2f
                }
            }

            // 7) Footer: "PrayerQuest" wordmark bottom-center.
            val footerPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#5C6BC0")
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            drawText(context.getString(R.string.common_prayerquest), width / 2f, (height - 80).toFloat(), footerPaint)
        }

        val dir = File(context.cacheDir, "shared/celebration").apply { mkdirs() }
        val file = File(dir, "prayer-${prayer.id}.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Naive text wrapper — splits on spaces and greedily packs words into
     * lines that fit [maxWidthPx]. Preserves a single leading quote mark
     * when present so the opening " isn't orphaned on its own line.
     */
    private fun wrapText(text: String, paint: Paint, maxWidthPx: Int): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        val bounds = Rect()
        val current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            paint.getTextBounds(candidate, 0, candidate.length, bounds)
            if (bounds.width() > maxWidthPx && current.isNotEmpty()) {
                lines += current.toString()
                current.clear()
                current.append(word)
            } else {
                if (current.isEmpty()) current.append(word)
                else current.append(' ').append(word)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }

    class Factory(
        private val prayerRepository: PrayerRepository,
        private val gamificationRepository: GamificationRepository,
        private val prayerId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnsweredCelebrationViewModel(
                prayerRepository,
                gamificationRepository,
                prayerId
            ) as T
        }
    }
}
