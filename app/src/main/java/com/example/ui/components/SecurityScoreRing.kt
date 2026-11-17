package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SecurityScoreRing(
    score: Int,
    scanProgress: Float,
    isScanning: Boolean,
    scoreLabel: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 188.dp
) {
    val animatedScore by animateIntAsState(
        targetValue = score.coerceIn(0, 100),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "scoreAnim"
    )
    val animatedSweep by animateFloatAsState(
        targetValue = if (isScanning) scanProgress.coerceIn(0.05f, 1f) else (score / 100f),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "sweepAnim"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radarLoop")
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    val activeAccent = when {
        isScanning -> secondaryColor
        score >= 85 -> primaryColor
        score >= 65 -> tertiaryColor
        else -> errorColor
    }

    Box(
        modifier = modifier
            .size(ringSize)
            .semantics { contentDescription = "$scoreLabel: $animatedScore out of 100" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14.dp.toPx()
            val inset = strokePx / 2f + 10.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer subtle radar halo
            if (isScanning) {
                drawCircle(
                    color = activeAccent.copy(alpha = 0.14f),
                    radius = (size.minDimension / 2f) * pulseRadius,
                    center = center
                )
            }

            // Inner concentric guide ring
            drawCircle(
                color = trackColor.copy(alpha = 0.25f),
                radius = (size.minDimension / 2f) - inset - 16.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Background track arc
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active progress / score arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        activeAccent.copy(alpha = 0.65f),
                        activeAccent,
                        secondaryColor,
                        activeAccent
                    ),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Rotating scanner comet arc & dot while scanning
            if (isScanning) {
                drawArc(
                    color = Color.White.copy(alpha = 0.85f),
                    startAngle = radarRotation - 90f,
                    sweepAngle = 48f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Terminal knob at end of score arc
                val endAngleRad = Math.toRadians((-90f + 360f * animatedSweep).toDouble())
                val radius = arcSize.width / 2f
                val knobOffset = Offset(
                    x = center.x + (radius * cos(endAngleRad)).toFloat(),
                    y = center.y + (radius * sin(endAngleRad)).toFloat()
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = knobOffset
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isScanning) {
                Text(
                    text = "${(scanProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.displayLarge,
                    color = activeAccent,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "$animatedScore",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = scoreLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
