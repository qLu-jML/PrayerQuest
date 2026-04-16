package com.prayerquest.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prayerquest.app.ui.theme.AppTheme

@Composable
fun ThemePickerDialog(
    onDismiss: () -> Unit,
    onSaveTheme: (AppTheme) -> Unit
) {
    val themeName = remember { mutableStateOf("My Custom Theme") }
    val selectedPrimaryColor = remember { mutableStateOf(0xFF3949AB) }
    val selectedSecondaryColor = remember { mutableStateOf(0xFFFFA000) }
    val selectedBackgroundColor = remember { mutableStateOf(0xFFFFF8F0) }

    val colorPalette = listOf(
        0xFF3949AB, 0xFF1565C0, 0xFF2E7D32, 0xFF6A1B9A,
        0xFFE65100, 0xFFB71C1C, 0xFF0288D1, 0xFF827717,
        0xFFFFB300, 0xFFFFA000, 0xFFFFD54F, 0xFFFFE082
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Create Custom Theme",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Theme name input
                Text(
                    text = "Theme Name",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = themeName.value,
                    onValueChange = { themeName.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("My Custom Theme") }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary color picker
                Text(
                    text = "Primary Color",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorGridPicker(
                    colors = colorPalette,
                    selectedColor = selectedPrimaryColor.value,
                    onColorSelected = { selectedPrimaryColor.value = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary color picker
                Text(
                    text = "Secondary Color",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorGridPicker(
                    colors = colorPalette,
                    selectedColor = selectedSecondaryColor.value,
                    onColorSelected = { selectedSecondaryColor.value = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Background color picker
                Text(
                    text = "Background Color",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorGridPicker(
                    colors = listOf(
                        0xFFFFF8F0, 0xFFFEF8E7, 0xFFFFF3E0, 0xFFE1F5FE,
                        0xFFF1F8E9, 0xFFF3E5F5, 0xFFFFFFFF, 0xFFFAFAFA,
                        0xFFF5F5F5, 0xFFEEEEEE, 0xFFE0E0E0, 0xFFD0D0D0
                    ),
                    selectedColor = selectedBackgroundColor.value,
                    onColorSelected = { selectedBackgroundColor.value = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Preview section
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(selectedBackgroundColor.value))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Primary swatch
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(selectedPrimaryColor.value))
                        )

                        // Secondary swatch
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(selectedSecondaryColor.value))
                        )

                        // Text preview
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = themeName.value,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(selectedPrimaryColor.value),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Custom theme",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(selectedSecondaryColor.value)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Create custom theme with minimal required data
                    // Derive other colors from primary/secondary/background
                    val customTheme = AppTheme(
                        id = "custom_${System.currentTimeMillis()}",
                        name = themeName.value,
                        description = "Custom theme",
                        isBuiltIn = false,
                        lightPrimary = selectedPrimaryColor.value,
                        lightOnPrimary = 0xFFFFFFFF,
                        lightPrimaryContainer = lighten(selectedPrimaryColor.value, 0.3f),
                        lightOnPrimaryContainer = 0xFF2D2B29,
                        lightSecondary = selectedSecondaryColor.value,
                        lightOnSecondary = 0xFFFFFFFF,
                        lightSecondaryContainer = lighten(selectedSecondaryColor.value, 0.3f),
                        lightBackground = selectedBackgroundColor.value,
                        lightSurface = 0xFFFFFFFF,
                        lightOnBackground = 0xFF2D2B29,
                        lightOnSurface = 0xFF2D2B29,
                        lightSurfaceVariant = lighten(selectedBackgroundColor.value, 0.2f),
                        darkPrimary = lighten(selectedPrimaryColor.value, 0.4f),
                        darkOnPrimary = 0xFF0D1B2A,
                        darkPrimaryContainer = selectedPrimaryColor.value,
                        darkOnPrimaryContainer = 0xFFE8E6F0,
                        darkSecondary = lighten(selectedSecondaryColor.value, 0.3f),
                        darkOnSecondary = 0xFF0D1B2A,
                        darkSecondaryContainer = selectedSecondaryColor.value,
                        darkBackground = 0xFF1A1A2E,
                        darkSurface = 0xFF222240,
                        darkOnBackground = 0xFFE8E6F0,
                        darkOnSurface = 0xFFE8E6F0,
                        darkSurfaceVariant = 0xFF2E2E50
                    )
                    onSaveTheme(customTheme)
                }
            ) {
                Text("Save Theme")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorGridPicker(
    colors: List<Long>,
    selectedColor: Long,
    onColorSelected: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Grid of 4 columns
        for (i in colors.indices step 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0 until 4) {
                    val index = i + j
                    if (index < colors.size) {
                        ColorButton(
                            color = colors[index],
                            isSelected = colors[index] == selectedColor,
                            onClick = { onColorSelected(colors[index]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorButton(
    color: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(color))
            .border(
                width = if (isSelected) 4.dp else 0.dp,
                color = if (isSelected) Color.Black else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    )
}

// Helper to lighten a color
private fun lighten(color: Long, factor: Float): Long {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF

    val newR = ((r + (255 - r) * factor).toInt()).coerceIn(0, 255)
    val newG = ((g + (255 - g) * factor).toInt()).coerceIn(0, 255)
    val newB = ((b + (255 - b) * factor).toInt()).coerceIn(0, 255)

    return (0xFF shl 24) or (newR.toLong() shl 16) or (newG.toLong() shl 8) or newB.toLong()
}

// Extension function for aspectRatio
private fun Modifier.aspectRatio(ratio: Float): Modifier = this.then(
    androidx.compose.foundation.layout.aspectRatio(ratio)
)
