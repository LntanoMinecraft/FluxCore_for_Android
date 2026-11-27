package com.mouajmd.fluxcore.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.components.ShortcutPinDialog
import com.mouajmd.fluxcore.ui.components.TransparentDialogSystemBars
import com.mouajmd.fluxcore.viewmodel.SystemHealthStatus

@Composable
fun ProfilesScreen(
    rules: List<AutomationRule>,
    activeRuleIds: Set<String>,
    busyRuleIds: Set<String>,
    systemHealth: SystemHealthStatus,
    isWideScreen: Boolean,
    onRefreshStatus: () -> Unit,
    onStartService: () -> Unit,
    onCreateRule: () -> Unit,
    onEditRule: (AutomationRule) -> Unit,
    onToggleEnabled: (AutomationRule, Boolean) -> Unit,
    onToggleActive: (AutomationRule) -> Unit,
    onDeleteRule: (AutomationRule) -> Unit,
    onAddQsTile: (AutomationRule) -> Unit,
    onPinShortcut: (AutomationRule, String) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-recheck permissions & runtime status whenever returning to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var rulePendingDelete by remember { mutableStateOf<AutomationRule?>(null) }
    var rulePendingShortcut by remember { mutableStateOf<AutomationRule?>(null) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    fun openBatteryOptimizationSettings() {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${ctx.packageName}")
                    )
                )
            }
        }
    }

    fun openDndAccessSettings() {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }.onFailure {
            runCatching {
                ctx.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${ctx.packageName}")
                    )
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            // Clean single-language brand header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 2.dp,
                    bottom = navBarBottom + if (isWideScreen) 96.dp else 188.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Real-Time Permissions & Runtime Status Card (Error Red <-> Theme Accent)
                item(key = "hero_system_health_card") {
                    SystemHealthHeroCard(
                        health = systemHealth,
                        onRecheck = {
                            HapticEngine.click(view)
                            onRefreshStatus()
                        },
                        onRequestRoot = {
                            HapticEngine.confirm(view)
                            onRefreshStatus()
                        },
                        onOpenBatterySettings = {
                            HapticEngine.click(view)
                            openBatteryOptimizationSettings()
                        },
                        onOpenDndSettings = {
                            HapticEngine.click(view)
                            openDndAccessSettings()
                        },
                        onStartService = {
                            HapticEngine.confirm(view)
                            onStartService()
                        }
                    )
                }

                if (rules.isEmpty()) {
                    item(key = "empty_profiles_placeholder") {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesomeMotion,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.empty_profiles_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.empty_profiles_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        RuleCardItem(
                            rule = rule,
                            isActive = rule.id in activeRuleIds,
                            isBusy = rule.id in busyRuleIds,
                            onClick = {
                                HapticEngine.click(view)
                                onEditRule(rule)
                            },
                            onToggleEnabled = { checked ->
                                HapticEngine.toggle(view, checked)
                                onToggleEnabled(rule, checked)
                            },
                            onToggleActive = {
                                HapticEngine.confirm(view)
                                onToggleActive(rule)
                            },
                            onAddQsTile = { onAddQsTile(rule) },
                            onRequestShortcut = { rulePendingShortcut = rule },
                            onRequestDelete = { rulePendingDelete = rule }
                        )
                    }
                }
            }
        }

        // FAB positioned cleanly above the Floating Pill Navigation Bar without overlap
        ExtendedFloatingActionButton(
            onClick = {
                HapticEngine.click(view)
                onCreateRule()
            },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.action_new_profile)) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = if (isWideScreen) 24.dp else 94.dp)
                .testTag("fab_new_profile")
        )
    }

    val deleteTarget = rulePendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { rulePendingDelete = null },
            shape = RoundedCornerShape(26.dp),
            title = {
                TransparentDialogSystemBars()
                Text(stringResource(R.string.dialog_delete_title))
            },
            text = { Text(stringResource(R.string.dialog_delete_msg, deleteTarget.name)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    onClick = {
                        HapticEngine.reject(view)
                        onDeleteRule(deleteTarget)
                        rulePendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_rule_btn")
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { rulePendingDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    val shortcutTarget = rulePendingShortcut
    if (shortcutTarget != null) {
        ShortcutPinDialog(
            initialLabel = shortcutTarget.name,
            onDismiss = { rulePendingShortcut = null },
            onConfirm = { label ->
                onPinShortcut(shortcutTarget, label)
                rulePendingShortcut = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemHealthHeroCard(
    health: SystemHealthStatus,
    onRecheck: () -> Unit,
    onRequestRoot: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onStartService: () -> Unit
) {
    val isReady = health.allReady

    val containerBg by animateColorAsState(
        targetValue = if (isReady) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        label = "healthCardBg"
    )
    val iconBadgeBg by animateColorAsState(
        targetValue = if (isReady) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        label = "healthIconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isReady) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onError
        },
        label = "healthIconTint"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isReady) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        label = "healthContentColor"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerBg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onRecheck)
            .animateContentSize()
            .testTag("hero_status_card")
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBadgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Rounded.VerifiedUser else Icons.Rounded.GppBad,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isReady) R.string.status_ready_title else R.string.status_missing_title
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isReady) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.status_ready_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onRecheck,
                    modifier = Modifier.testTag("status_recheck_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.status_recheck_cd),
                        tint = contentColor.copy(alpha = 0.85f)
                    )
                }
            }

            AnimatedVisibility(visible = !isReady) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Compact status checklist pills (2-4 chars, zero ellipsis)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusCheckChip(
                            label = stringResource(
                                if (health.hasRoot) R.string.status_check_root_ok else R.string.status_check_root_missing
                            ),
                            ok = health.hasRoot,
                            contentColor = contentColor
                        )
                        StatusCheckChip(
                            label = stringResource(
                                if (health.batteryUnrestricted) R.string.status_check_battery_ok else R.string.status_check_battery_missing
                            ),
                            ok = health.batteryUnrestricted,
                            contentColor = contentColor
                        )
                        StatusCheckChip(
                            label = stringResource(
                                if (health.hasDndAccess) R.string.status_check_dnd_ok else R.string.status_check_dnd_missing
                            ),
                            ok = health.hasDndAccess,
                            contentColor = contentColor
                        )
                        StatusCheckChip(
                            label = stringResource(
                                if (health.serviceRunning) R.string.status_check_service_ok else R.string.status_check_service_missing
                            ),
                            ok = health.serviceRunning,
                            contentColor = contentColor
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Direct fix actions for missing items
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!health.hasRoot) {
                            StatusFixPillButton(
                                text = stringResource(R.string.status_action_request_root),
                                onClick = onRequestRoot,
                                primary = true,
                                tag = "status_fix_root"
                            )
                        }
                        if (!health.batteryUnrestricted) {
                            StatusFixPillButton(
                                text = stringResource(R.string.status_action_battery_settings),
                                onClick = onOpenBatterySettings,
                                primary = health.hasRoot,
                                tag = "status_fix_battery"
                            )
                        }
                        if (!health.hasDndAccess) {
                            StatusFixPillButton(
                                text = stringResource(R.string.status_action_dnd_settings),
                                onClick = onOpenDndSettings,
                                primary = health.hasRoot && health.batteryUnrestricted,
                                tag = "status_fix_dnd"
                            )
                        }
                        if (health.hasRoot && !health.serviceRunning) {
                            StatusFixPillButton(
                                text = stringResource(R.string.status_action_start_service),
                                onClick = onStartService,
                                primary = health.batteryUnrestricted && health.hasDndAccess,
                                tag = "status_fix_service"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCheckChip(
    label: String,
    ok: Boolean,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = CircleShape,
        color = contentColor.copy(alpha = if (ok) 0.10f else 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (ok) contentColor.copy(alpha = 0.72f) else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (ok) FontWeight.Normal else FontWeight.SemiBold,
                color = contentColor.copy(alpha = if (ok) 0.78f else 1f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun StatusFixPillButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean,
    tag: String
) {
    val bg = if (primary) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.14f)
    }
    val fg = if (primary) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = CircleShape,
        color = bg,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RuleCardItem(
    rule: AutomationRule,
    isActive: Boolean,
    isBusy: Boolean,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleActive: () -> Unit,
    onAddQsTile: () -> Unit,
    onRequestShortcut: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var menuExpanded by remember { mutableStateOf(false) }
    val triggerDesc = remember(rule, ctx) { rule.describeTrigger(ctx) }
    val actionsDesc = remember(rule, ctx) { rule.describeActions(ctx) }

    val cardBg by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "cardBg"
    )
    val titleColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("rule_card_${rule.id}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = rule.name.ifBlank { stringResource(R.string.editor_rule_name_default) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isActive) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = stringResource(R.string.rule_active_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = triggerDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = subColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("rule_switch_${rule.id}")
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = actionsDesc,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !isBusy, onClick = onToggleActive)
                            .testTag("rule_run_btn_${rule.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = if (isActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = stringResource(if (isActive) R.string.btn_revert_now else R.string.btn_trigger_now),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.rule_steps_count, rule.actionSteps.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = subColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = {
                            HapticEngine.click(view)
                            menuExpanded = true
                        },
                        modifier = Modifier.testTag("rule_more_btn_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.btn_more),
                            tint = titleColor
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_qs_tile)) },
                            leadingIcon = { Icon(Icons.Rounded.DashboardCustomize, contentDescription = null) },
                            onClick = {
                                HapticEngine.click(view)
                                menuExpanded = false
                                onAddQsTile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_shortcut)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, contentDescription = null) },
                            onClick = {
                                HapticEngine.click(view)
                                menuExpanded = false
                                onRequestShortcut()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_edit_rule)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                HapticEngine.click(view)
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_delete_rule), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                HapticEngine.click(view)
                                menuExpanded = false
                                onRequestDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
