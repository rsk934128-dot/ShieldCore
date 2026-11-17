package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ScanHistoryEntity
import com.example.data.model.ScanPhase
import com.example.ui.SecurityUiState
import com.example.ui.components.FeatureModuleCard
import com.example.ui.components.SecurityScoreRing
import com.example.ui.formatBytes
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: SecurityUiState,
    strings: AppStrings,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onNavigateDeepScan: () -> Unit,
    onNavigateCleaner: () -> Unit,
    onNavigateApps: () -> Unit,
    onToggleShield: (Boolean) -> Unit,
    onRefreshSignatures: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane: Hero Security Score + Scan Now CTA
                Column(
                    modifier = Modifier
                        .weight(0.43f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroScoreCard(
                        uiState = uiState,
                        strings = strings,
                        onStartScan = onStartScan,
                        onStopScan = onStopScan
                    )
                    StorageHealthCard(uiState = uiState, strings = strings, onOpenCleaner = onNavigateCleaner)
                }

                // Right Pane: Real-time Shield + 2x2 Feature Grid + Recent Scan History
                LazyColumn(
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        RealTimeShieldCard(
                            enabled = uiState.realTimeShieldEnabled,
                            strings = strings,
                            onToggle = onToggleShield
                        )
                    }
                    item {
                        FeatureGridSection(
                            uiState = uiState,
                            strings = strings,
                            onNavigateDeepScan = onNavigateDeepScan,
                            onNavigateCleaner = onNavigateCleaner,
                            onNavigateApps = onNavigateApps,
                            onRefreshSignatures = onRefreshSignatures
                        )
                    }
                    item {
                        RecentScansHeader(
                            strings = strings,
                            hasHistory = uiState.scanHistory.isNotEmpty(),
                            onClearHistory = onClearHistory
                        )
                    }
                    if (uiState.scanHistory.isEmpty()) {
                        item { EmptyHistoryCard(strings = strings) }
                    } else {
                        items(uiState.scanHistory.take(5), key = { it.id }) { report ->
                            ScanReportRow(report = report)
                        }
                    }
                }
            }
        } else {
            // Compact Portrait Phone Layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dashboard_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeroScoreCard(
                        uiState = uiState,
                        strings = strings,
                        onStartScan = onStartScan,
                        onStopScan = onStopScan
                    )
                }
                item {
                    RealTimeShieldCard(
                        enabled = uiState.realTimeShieldEnabled,
                        strings = strings,
                        onToggle = onToggleShield
                    )
                }
                item {
                    FeatureGridSection(
                        uiState = uiState,
                        strings = strings,
                        onNavigateDeepScan = onNavigateDeepScan,
                        onNavigateCleaner = onNavigateCleaner,
                        onNavigateApps = onNavigateApps,
                        onRefreshSignatures = onRefreshSignatures
                    )
                }
                item {
                    StorageHealthCard(
                        uiState = uiState,
                        strings = strings,
                        onOpenCleaner = onNavigateCleaner
                    )
                }
                item {
                    RecentScansHeader(
                        strings = strings,
                        hasHistory = uiState.scanHistory.isNotEmpty(),
                        onClearHistory = onClearHistory
                    )
                }
                if (uiState.scanHistory.isEmpty()) {
                    item { EmptyHistoryCard(strings = strings) }
                } else {
                    items(uiState.scanHistory.take(5), key = { it.id }) { report ->
                        ScanReportRow(report = report)
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun HeroScoreCard(
    uiState: SecurityUiState,
    strings: AppStrings,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    val statusColor = when {
        uiState.isScanning -> MaterialTheme.colorScheme.secondary
        uiState.threatsDetected.isNotEmpty() || uiState.securityScore < 65 -> MaterialTheme.colorScheme.error
        uiState.securityScore < 85 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        uiState.isScanning -> strings.statusScanning
        uiState.threatsDetected.isNotEmpty() || uiState.securityScore < 65 -> strings.statusAtRisk
        uiState.securityScore < 85 -> strings.statusAttention
        else -> strings.statusProtected
    }

    val phaseText = when (uiState.scanPhase) {
        ScanPhase.IDLE -> strings.phaseIdle
        ScanPhase.SCANNING_STORAGE -> strings.phaseStorage
        ScanPhase.AUDITING_APPS -> strings.phaseApps
        ScanPhase.CHECKING_CACHE -> strings.phaseCache
        ScanPhase.COMPLETE -> strings.phaseComplete
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_shield_backdrop),
                contentDescription = stringResource(R.string.cd_shield_backdrop),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(if (uiState.isDarkTheme) 0.24f else 0.08f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.threatsDetected.isEmpty()) {
                                Icons.Rounded.VerifiedUser
                            } else {
                                Icons.Rounded.WarningAmber
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                    }
                }

                SecurityScoreRing(
                    score = uiState.securityScore,
                    scanProgress = uiState.scanProgress,
                    isScanning = uiState.isScanning,
                    scoreLabel = strings.securityScoreLabel,
                    ringSize = 156.dp
                )

                // Live scan progress bar & telemetry line
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = phaseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    AnimatedVisibility(
                        visible = uiState.isScanning || uiState.scanPhase == ScanPhase.COMPLETE,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { uiState.scanProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                            if (uiState.currentTarget.isNotBlank()) {
                                Text(
                                    text = uiState.currentTarget,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Primary Scan Now / Stop Scan Action Button
                Button(
                    onClick = { if (uiState.isScanning) onStopScan() else onStartScan() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("scan_now_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isScanning) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (uiState.isScanning) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.isScanning) Icons.Rounded.Stop else Icons.Rounded.Radar,
                        contentDescription = stringResource(R.string.cd_scan_button),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            uiState.isScanning -> strings.stopScan
                            uiState.scanPhase == ScanPhase.COMPLETE -> strings.scanAgain
                            else -> strings.scanNow
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Quick Telemetry Metrics Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TelemetryStatPill(
                        label = strings.filesScannedLabel,
                        value = "${uiState.filesScannedCount}"
                    )
                    TelemetryStatPill(
                        label = strings.appsAuditedLabel,
                        value = "${uiState.auditedApps.size}"
                    )
                    TelemetryStatPill(
                        label = strings.threatsLabel,
                        value = "${uiState.threatsDetected.size}",
                        valueColor = if (uiState.threatsDetected.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatPill(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RealTimeShieldCard(
    enabled: Boolean,
    strings: AppStrings,
    onToggle: (Boolean) -> Unit
) {
    val accent = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            if (enabled) accent.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GppGood,
                    contentDescription = strings.realTimeShield,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = strings.realTimeShield,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (enabled) strings.shieldActiveDesc else strings.shieldPausedDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("realtime_shield_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun FeatureGridSection(
    uiState: SecurityUiState,
    strings: AppStrings,
    onNavigateDeepScan: () -> Unit,
    onNavigateCleaner: () -> Unit,
    onNavigateApps: () -> Unit,
    onRefreshSignatures: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureModuleCard(
                title = strings.deepScanTitle,
                description = strings.deepScanDesc,
                badgeText = "${uiState.filesScannedCount} files",
                icon = Icons.Rounded.Radar,
                accentColor = MaterialTheme.colorScheme.secondary,
                tag = "card_deep_scan",
                onClick = onNavigateDeepScan,
                modifier = Modifier.weight(1f)
            )
            FeatureModuleCard(
                title = strings.junkCleanerTitle,
                description = strings.junkCleanerDesc,
                badgeText = formatBytes(uiState.totalJunkBytes),
                icon = Icons.Rounded.CleaningServices,
                accentColor = MaterialTheme.colorScheme.tertiary,
                tag = "card_junk_cleaner",
                onClick = onNavigateCleaner,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureModuleCard(
                title = strings.appManagerTitle,
                description = strings.appManagerDesc,
                badgeText = "${uiState.riskyAppsCount} flagged",
                icon = Icons.Rounded.Apps,
                accentColor = MaterialTheme.colorScheme.primary,
                tag = "card_app_manager",
                onClick = onNavigateApps,
                modifier = Modifier.weight(1f)
            )
            FeatureModuleCard(
                title = strings.signatureDbTitle,
                description = strings.signatureDbDesc,
                badgeText = "${uiState.signatureCount} hashes",
                icon = Icons.Rounded.Shield,
                accentColor = MaterialTheme.colorScheme.secondary,
                tag = "card_signature_db",
                onClick = onRefreshSignatures,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StorageHealthCard(
    uiState: SecurityUiState,
    strings: AppStrings,
    onOpenCleaner: () -> Unit
) {
    val storage = uiState.storageOverview
    Card(
        onClick = onOpenCleaner,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("storage_health_card"),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = strings.storageHealth,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${(storage.usedFraction * 100).toInt()}% ${strings.usedSpace}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            LinearProgressIndicator(
                progress = { storage.usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (storage.usedFraction > 0.85f) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatBytes(storage.usedBytes)} ${strings.usedSpace}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${formatBytes(storage.freeBytes)} ${strings.freeSpace}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RecentScansHeader(
    strings: AppStrings,
    hasHistory: Boolean,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.recentScans,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (hasHistory) {
            TextButton(
                onClick = onClearHistory,
                modifier = Modifier.testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = strings.clearHistory,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.clearHistory)
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(strings: AppStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strings.noHistoryYet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScanReportRow(report: ScanHistoryEntity) {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timeLabel = formatter.format(Date(report.timestamp))
    val isClean = report.threatsFound == 0
    val accent = if (isClean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isClean) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = report.scanType,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${report.securityScore}/100",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
