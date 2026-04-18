package com.prayerquest.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular avatar for prayer items — shows either:
 *  1. The Photo Prayer (DD §3.9) when the item has one attached, or
 *  2. A monogram-style fallback (first letter of [fallbackLabel]) so cards
 *     without photos still have a visual anchor instead of a blank space.
 *
 * Kept deliberately cheap: BitmapFactory.decodeFile on a ≤1280px JPEG is a
 * few ms on mid-range hardware, and re-renders are memoized by `photoPath`.
 * If this ever shows up in profiling we'll swap in an image loader like
 * Coil, but for now avoiding a new dependency is worth more than the
 * optimization.
 */
@Composable
fun PrayerPhotoAvatar(
    photoPath: String?,
    fallbackLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = remember(photoPath) {
            if (photoPath.isNullOrBlank()) null
            else try {
                BitmapFactory.decodeFile(photoPath)
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
            Text(
                text = fallbackLabel.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
