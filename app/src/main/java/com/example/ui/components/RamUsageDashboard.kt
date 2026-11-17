package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RamSnapshot(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val isLowMemory: Boolean = false
) {
    val usedBytes: Long
        get() = (totalBytes - availableBytes).coerceAtLeast(0L)

    val usedFraction: Float
        get() = if (totalBytes > 0L) {
            (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val usagePercent: Int
        get() = (usedFraction * 100f).toInt().coerceIn(0, 100)
}

@Composable
fun RamUsageDashboard(
    isBengali: Boolean,
    modifier: Modifier = Modifier,
    onMemoryCleared: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var snapshot by remember { mutableStateOf(readDeviceMemoryInfo(context)) }
    var isClearing by remember { mutableStateOf(false) }
    var lastActionSummary by remember { mutableStateOf<String?>(null) }

    // Real-time ActivityManager.MemoryInfo polling while visible
    LaunchedEffect(context) {
        while (isActive) {
            snapshot = readDeviceMemoryInfo(context)
            delay(1500L)
        }
    }

    val animatedPercent by animateIntAsState(
        targetValue = snapshot.usagePercent,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "ramPercent"
    )
    val animatedFraction by animateFloatAsState(
        targetValue = snapshot.usedFraction,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "ramFraction"
    )

    val accentColor = when {
        snapshot.isLowMemory || snapshot.usagePercent >= 85 -> MaterialTheme.colorScheme.error
        snapshot.usagePercent >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    val statusBadge = when {
        snapshot.isLowMemory || snapshot.usagePercent >= 85 -> if (isBengali) "উচ্চ চাপ" else "HIGH LOAD"
        snapshot.usagePercent >= 70 -> if (isBengali) "মধ্যম" else "MODERATE"
        else -> if (isBengali) "স্বাভাবিক" else "OPTIMAL"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ram_usage_dashboard"),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Circular RAM Percentage Gauge
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .semantics {
                            contentDescription = "RAM usage $animatedPercent percent"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokePx = 8.dp.toPx()
                        val inset = strokePx / 2f + 2.dp.toPx()
                        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                        val topLeft = Offset(inset, inset)

                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$animatedPercent%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.testTag("ram_usage_percentage")
                        )
                        Text(
                            text = "RAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Memory Telemetry + Status Badge
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBengali) "র‍্যাম ব্যবহার" else "Live RAM Usage",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = accentColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = statusBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBengali) {
                                "ব্যবহৃত: ${formatBytes(snapshot.usedBytes)}"
                            } else {
                                "Used: ${formatBytes(snapshot.usedBytes)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBengali) {
                                "মোট: ${formatBytes(snapshot.totalBytes)}"
                            } else {
                                "Free: ${formatBytes(snapshot.availableBytes)} / ${formatBytes(snapshot.totalBytes)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = lastActionSummary != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastActionSummary.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            FilledTonalButton(
                onClick = {
                    if (!isClearing) {
                        isClearing = true
                        scope.launch {
                            val beforeAvail = snapshot.availableBytes
                            performSafeRamOptimization(context)
                            delay(350L)
                            val updated = readDeviceMemoryInfo(context)
                            snapshot = updated
                            val delta = (updated.availableBytes - beforeAvail).coerceAtLeast(0L)
                            val msg = if (isBengali) {
                                if (delta > 0L) {
                                    "র‍্যাম অপ্টিমাইজ সম্পন্ন • +${formatBytes(delta)} মুক্ত হয়েছে (${formatBytes(updated.availableBytes)} খালি)"
                                } else {
                                    "মেমোরি ক্যাশ ট্রিম সম্পন্ন • ${formatBytes(updated.availableBytes)} খালি আছে"
                                }
                            } else {
                                if (delta > 0L) {
                                    "RAM trimmed • +${formatBytes(delta)} freed (${formatBytes(updated.availableBytes)} available)"
                                } else {
                                    "Memory cache trimmed • ${formatBytes(updated.availableBytes)} available"
                                }
                            }
                            lastActionSummary = msg
                            onMemoryCleared?.invoke(msg)
                            isClearing = false
                        }
                    }
                },
                enabled = !isClearing,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("clear_ram_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accentColor.copy(alpha = 0.16f),
                    contentColor = accentColor
                )
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = accentColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBengali) "Clear RAM (র‍্যাম পরিষ্কার)" else "Clear RAM",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

fun readDeviceMemoryInfo(context: Context): RamSnapshot {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        ?: return RamSnapshot()
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)
    return RamSnapshot(
        totalBytes = memoryInfo.totalMem,
        availableBytes = memoryInfo.availMem,
        isLowMemory = memoryInfo.lowMemory
    )
}

private suspend fun performSafeRamOptimization(context: Context) = withContext(Dispatchers.IO) {
    runCatching {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isFile) file.delete()
        }
        Runtime.getRuntime().gc()
        System.runFinalization()
    }
}
