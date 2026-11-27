package com.mouajmd.fluxcore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mouajmd.fluxcore.system.HapticEngine
import kotlin.math.roundToInt

/**
 * Material Design 3 Expressive wide-track rounded percentage slider (0%–100%).
 * - Zero hard borders; blends into tonal surfaceContainer cards.
 * - Right-aligned percentage readout with tabular figures (`tnum`) to prevent jitter.
 * - Linked to [HapticEngine.sliderTick] when crossing tick intervals or endpoints.
 */
@Composable
fun ExpressivePercentSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPeakOverride: Boolean = false,
    leadingIcon: ImageVector? = null,
    contentDescription: String = "",
    tickStep: Int = 2
) {
    val view = LocalView.current
    val clamped = value.coerceIn(0, 100)
    val latestValue by rememberUpdatedState(clamped)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestEnabled by rememberUpdatedState(enabled)

    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticBucket by remember { mutableIntStateOf(clamped / tickStep.coerceAtLeast(1)) }

    val targetFraction = clamped / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "sliderFraction"
    )
    val displayFraction = if (isDragging) targetFraction else animatedFraction

    val activeFillColor by animateColorAsState(
        targetValue = when {
            isPeakOverride -> MaterialTheme.colorScheme.tertiary
            enabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        },
        label = "sliderActiveColor"
    )
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val handleColor by animateColorAsState(
        targetValue = when {
            isPeakOverride -> MaterialTheme.colorScheme.onTertiary
            enabled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.surface
        },
        label = "sliderHandleColor"
    )
    val tickDotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    val activeTickDotColor = handleColor.copy(alpha = 0.35f)
    val valueTextColor by animateColorAsState(
        targetValue = when {
            isPeakOverride -> MaterialTheme.colorScheme.tertiary
            enabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "sliderValueTextColor"
    )

    fun dispatchValue(rawFraction: Float) {
        if (!latestEnabled) return
        val newVal = (rawFraction.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
        if (newVal != latestValue) {
            val step = tickStep.coerceAtLeast(1)
            val newBucket = newVal / step
            val isEndpoint = newVal == 0 || newVal == 100
            if (isEndpoint || newBucket != lastHapticBucket) {
                lastHapticBucket = newBucket
                HapticEngine.sliderTick(view, isEndpoint = isEndpoint)
            }
            latestOnChange(newVal)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                if (contentDescription.isNotEmpty()) {
                    this.contentDescription = contentDescription
                }
                stateDescription = "$clamped%"
                if (enabled) {
                    setProgress { target ->
                        val next = (target.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
                        latestOnChange(next)
                        true
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = valueTextColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
        }

        // Minimum 48.dp touch target container housing a 32.dp expressive pill track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .onSizeChanged { size ->
                    if (size.width > 0) trackWidthPx = size.width.toFloat()
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val frac = offset.x / trackWidthPx.coerceAtLeast(1f)
                        dispatchValue(frac)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            lastHapticBucket = latestValue / tickStep.coerceAtLeast(1)
                            val frac = offset.x / trackWidthPx.coerceAtLeast(1f)
                            dispatchValue(frac)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val frac = change.position.x / trackWidthPx.coerceAtLeast(1f)
                            dispatchValue(frac)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackHeight = 32.dp.toPx()
                val top = (size.height - trackHeight) / 2f
                val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                // Inactive full pill background
                drawRoundRect(
                    color = inactiveTrackColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = corner
                )

                // Active wide pill fill (keeps minimum capsule width so the pill thumb stays seated)
                val minActiveWidth = trackHeight
                val activeWidth = if (displayFraction <= 0f) {
                    minActiveWidth * 0.56f
                } else {
                    minActiveWidth + (size.width - minActiveWidth) * displayFraction
                }.coerceIn(0f, size.width)

                if (displayFraction > 0.001f || isPeakOverride) {
                    drawRoundRect(
                        color = activeFillColor,
                        topLeft = Offset(0f, top),
                        size = Size(activeWidth, trackHeight),
                        cornerRadius = corner
                    )
                }

                // Subtle quarter tick indicators (25%, 50%, 75%)
                val dotRadius = 2.dp.toPx()
                val centerY = size.height / 2f
                listOf(0.25f, 0.50f, 0.75f).forEach { mark ->
                    val x = minActiveWidth * 0.5f + (size.width - minActiveWidth) * mark
                    val insideActive = (displayFraction > 0.001f || isPeakOverride) && x < (activeWidth - 10.dp.toPx())
                    drawCircle(
                        color = if (insideActive) activeTickDotColor else tickDotColor,
                        radius = dotRadius,
                        center = Offset(x, centerY)
                    )
                }

                // Expressive vertical pill thumb bar nestled cleanly inside the active fill edge
                val thumbWidth = 4.dp.toPx()
                val thumbHeight = 16.dp.toPx()
                val thumbCenterX = if (displayFraction <= 0.001f && !isPeakOverride) {
                    trackHeight * 0.5f
                } else {
                    (activeWidth - trackHeight * 0.44f).coerceIn(trackHeight * 0.5f, size.width - trackHeight * 0.44f)
                }
                val thumbActualColor = if (displayFraction <= 0.001f && !isPeakOverride) {
                    activeFillColor
                } else {
                    handleColor
                }
                drawRoundRect(
                    color = thumbActualColor,
                    topLeft = Offset(thumbCenterX - thumbWidth / 2f, centerY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right-aligned live percentage readout with tabular figures (tnum) to prevent jitter
        Text(
            text = "$clamped%",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = valueTextColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.widthIn(min = 46.dp)
        )
    }
}
