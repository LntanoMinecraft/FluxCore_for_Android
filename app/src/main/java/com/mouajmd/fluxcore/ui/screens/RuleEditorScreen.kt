package com.mouajmd.fluxcore.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.model.ActionStep
import com.mouajmd.fluxcore.model.ActionStepType
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.model.QuickCommandPreset
import com.mouajmd.fluxcore.model.RuleCondition
import com.mouajmd.fluxcore.model.ShellScriptItem
import com.mouajmd.fluxcore.model.TriggerType
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.components.AppPickerSheet
import com.mouajmd.fluxcore.ui.components.ExpressivePercentSlider
import com.mouajmd.fluxcore.ui.components.ExtraConditionDialog
import com.mouajmd.fluxcore.ui.components.FluxModalBottomSheet
import com.mouajmd.fluxcore.ui.components.QsTilePickerSheet
import com.mouajmd.fluxcore.ui.components.ScriptScannerSheet
import com.mouajmd.fluxcore.ui.components.ShortcutPinDialog
import com.mouajmd.fluxcore.ui.components.TonalAssistChip
import com.mouajmd.fluxcore.ui.components.TonalFilterChip
import com.mouajmd.fluxcore.ui.components.TonalSectionHeader
import com.mouajmd.fluxcore.ui.components.TonalTextField
import com.mouajmd.fluxcore.ui.components.TransparentDialogSystemBars
import com.mouajmd.fluxcore.ui.components.triggerTypeLabel
import com.mouajmd.fluxcore.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    existingRule: AutomationRule?,
    advancedMode: Boolean,
    scannedScripts: List<ShellScriptItem>,
    isScanningScripts: Boolean,
    onScanScripts: () -> Unit,
    onReadScriptPreview: suspend (String) -> String,
    onDetectWifi: suspend () -> String,
    onDetectBt: suspend () -> List<String>,
    onSave: (AutomationRule) -> Unit,
    onAddQsTile: (AutomationRule) -> Unit,
    onPinShortcut: (AutomationRule, String) -> Unit,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val base = remember(existingRule) { existingRule ?: AutomationRule() }
    var name by remember(base) { mutableStateOf(base.name) }
    var autoRevert by remember(base) { mutableStateOf(base.autoRevert) }
    var triggerType by remember(base) { mutableStateOf(base.triggerType) }
    var triggerOn by remember(base) { mutableStateOf(base.triggerOn) }
    var triggerPkgs by remember(base) { mutableStateOf(base.triggerPkgs) }
    var triggerTarget by remember(base) { mutableStateOf(base.triggerTarget) }
    var triggerThreshold by remember(base) { mutableIntStateOf(base.triggerThreshold) }
    val extraConditions = remember(base) { mutableStateListOf<RuleCondition>().apply { addAll(base.extraConditions) } }
    val actionSteps = remember(base) { mutableStateListOf<ActionStep>().apply { addAll(base.actionSteps) } }
    var revertCmd by remember(base) { mutableStateOf(base.revertCmd) }

    // Drag-to-reorder state for actionSteps
    var draggingStepId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val stepHeightsPx = remember { mutableStateMapOf<String, Int>() }

    // Sheet & Dialog triggers
    var showTriggerAppPicker by remember { mutableStateOf(false) }
    var showExtraCondDialog by remember { mutableStateOf(false) }
    var showAddActionSheet by remember { mutableStateOf(false) }
    var btCandidatesDialog by remember { mutableStateOf<List<String>?>(null) }
    var tilePickerTargetStepId by remember { mutableStateOf<String?>(null) }
    var killAppsTargetStepId by remember { mutableStateOf<String?>(null) }
    var scriptScannerTargetStepId by remember { mutableStateOf<String?>(null) } // "__REVERT__" for revertCmd
    var topMenuExpanded by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }

    fun buildRuleDraft(): AutomationRule = base.copy(
        name = name.trim().ifEmpty { ctx.getString(R.string.editor_rule_name_default) },
        autoRevert = autoRevert,
        triggerType = triggerType,
        triggerOn = triggerOn,
        triggerPkgs = triggerPkgs,
        triggerTarget = triggerTarget.trim(),
        triggerThreshold = triggerThreshold,
        extraConditions = extraConditions.toList(),
        actionSteps = actionSteps.toList(),
        revertCmd = revertCmd.trim()
    )

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (existingRule == null) R.string.editor_title_new else R.string.editor_title_edit
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticEngine.click(view)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                actions = {
                    if (existingRule != null) {
                        Box {
                            IconButton(onClick = {
                                HapticEngine.click(view)
                                topMenuExpanded = true
                            }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.btn_more))
                            }
                            DropdownMenu(
                                expanded = topMenuExpanded,
                                onDismissRequest = { topMenuExpanded = false },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_add_qs_tile)) },
                                    leadingIcon = { Icon(Icons.Rounded.DashboardCustomize, contentDescription = null) },
                                    onClick = {
                                        HapticEngine.click(view)
                                        topMenuExpanded = false
                                        val draft = buildRuleDraft()
                                        onSave(draft)
                                        onAddQsTile(draft)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_add_shortcut)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.AddToHomeScreen, contentDescription = null) },
                                    onClick = {
                                        HapticEngine.click(view)
                                        topMenuExpanded = false
                                        showShortcutDialog = true
                                    }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            HapticEngine.confirm(view)
                            onSave(buildRuleDraft())
                            onBack()
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .testTag("editor_save_btn")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = navBarBottom + 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: Basics
                TonalSectionHeader(stringResource(R.string.editor_section_basic))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TonalTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.editor_rule_name_label)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_name_input")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticEngine.toggle(view, !autoRevert)
                                    autoRevert = !autoRevert
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.editor_auto_revert),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.editor_auto_revert_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = autoRevert,
                                onCheckedChange = {
                                    HapticEngine.toggle(view, it)
                                    autoRevert = it
                                },
                                modifier = Modifier.testTag("editor_auto_revert_switch")
                            )
                        }
                    }
                }

                // SECTION 2: Trigger (IF)
                TonalSectionHeader(stringResource(R.string.editor_section_trigger))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Edge-to-Edge scrollable 10 trigger chips (never clipped)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TriggerType.entries.forEach { t ->
                                TonalFilterChip(
                                    selected = triggerType == t,
                                    onClick = {
                                        HapticEngine.click(view)
                                        triggerType = t
                                    },
                                    label = { Text(triggerTypeLabel(t)) }
                                )
                            }
                        }

                        if (triggerType != TriggerType.MANUAL) {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (triggerType != TriggerType.TIME) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = triggerDirectionText(triggerType, triggerOn, advancedMode),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Switch(
                                            checked = triggerOn,
                                            onCheckedChange = {
                                                HapticEngine.toggle(view, it)
                                                triggerOn = it
                                            }
                                        )
                                    }
                                }

                                when (triggerType) {
                                    TriggerType.APP -> {
                                        FilledTonalButton(
                                            onClick = {
                                                HapticEngine.click(view)
                                                showTriggerAppPicker = true
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (triggerPkgs.isEmpty()) {
                                                    stringResource(R.string.trigger_pick_apps)
                                                } else {
                                                    stringResource(
                                                        R.string.trigger_selected_apps,
                                                        triggerPkgs.size,
                                                        triggerPkgs.take(2).joinToString(", ")
                                                    )
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    TriggerType.WIFI -> if (advancedMode) {
                                        TonalTextField(
                                            value = triggerTarget,
                                            onValueChange = { triggerTarget = it },
                                            placeholder = { Text(stringResource(R.string.trigger_wifi_ssid_hint)) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                HapticEngine.click(view)
                                                scope.launch {
                                                    val ssid = onDetectWifi()
                                                    if (ssid.isNotEmpty()) {
                                                        triggerTarget = ssid
                                                        onShowMessage("Wi-Fi: $ssid")
                                                    } else {
                                                        onShowMessage(ctx.getString(R.string.trigger_wifi_ssid_hint))
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.trigger_wifi_detect))
                                        }
                                    }
                                    TriggerType.BLUETOOTH -> if (advancedMode) {
                                        TonalTextField(
                                            value = triggerTarget,
                                            onValueChange = { triggerTarget = it },
                                            placeholder = { Text(stringResource(R.string.trigger_bt_name_hint)) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        FilledTonalButton(
                                            onClick = {
                                                HapticEngine.click(view)
                                                scope.launch {
                                                    val candidates = onDetectBt()
                                                    when {
                                                        candidates.isEmpty() -> onShowMessage(ctx.getString(R.string.trigger_bt_name_hint))
                                                        candidates.size == 1 -> {
                                                            triggerTarget = candidates[0]
                                                            onShowMessage(candidates[0])
                                                        }
                                                        else -> btCandidatesDialog = candidates
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(stringResource(R.string.trigger_bt_detect))
                                        }
                                    }
                                    TriggerType.BATTERY -> {
                                        Text(
                                            text = if (triggerOn) {
                                                stringResource(R.string.trigger_battery_above, triggerThreshold)
                                            } else {
                                                stringResource(R.string.trigger_battery_below, triggerThreshold)
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Slider(
                                            value = triggerThreshold.toFloat(),
                                            onValueChange = { triggerThreshold = it.roundToInt() },
                                            valueRange = 1f..100f
                                        )
                                    }
                                    TriggerType.TIME -> {
                                        TonalTextField(
                                            value = triggerTarget,
                                            onValueChange = { triggerTarget = it },
                                            placeholder = { Text(stringResource(R.string.trigger_time_hint)) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    else -> Unit
                                }

                                // Advanced Mode: Extra AND conditions
                                if (advancedMode) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.trigger_extra_and_title),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    extraConditions.forEachIndexed { idx, cond ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "+ ${cond.describe(ctx)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        HapticEngine.click(view)
                                                        extraConditions.removeAt(idx)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.Close,
                                                        contentDescription = stringResource(R.string.action_remove),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            HapticEngine.click(view)
                                            showExtraCondDialog = true
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.trigger_add_extra))
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 3: Ordered Action Pipeline with Drag-to-Reorder Handle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.editor_section_actions),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (actionSteps.size > 1) {
                            Text(
                                text = stringResource(R.string.editor_drag_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.rule_steps_count, actionSteps.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (actionSteps.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.editor_actions_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val spacingPx = with(density) { 10.dp.toPx() }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        actionSteps.forEachIndexed { index, step ->
                            key(step.id) {
                                val isDragging = draggingStepId == step.id
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 10.dp else 0.dp,
                                    label = "dragElevation"
                                )

                                ActionStepPipelineCard(
                                    stepNumber = index + 1,
                                    totalSteps = actionSteps.size,
                                    step = step,
                                    isDragging = isDragging,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDragging) 10f else 0f)
                                        .onSizeChanged { sz -> stepHeightsPx[step.id] = sz.height }
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                            scaleX = if (isDragging) 1.02f else 1f
                                            scaleY = if (isDragging) 1.02f else 1f
                                            shadowElevation = with(density) { elevation.toPx() }
                                        },
                                    dragHandleModifier = Modifier
                                        .pointerInput(step.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggingStepId = step.id
                                                    dragOffsetY = 0f
                                                    HapticEngine.dragStart(view)
                                                },
                                                onDragEnd = {
                                                    draggingStepId = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingStepId = null
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    val curIdx = actionSteps.indexOfFirst { it.id == step.id }
                                                    if (curIdx >= 0) {
                                                        val curHeight = (stepHeightsPx[step.id] ?: 160).toFloat() + spacingPx
                                                        val thresholdPx = curHeight * 0.55f
                                                        if (dragOffsetY > thresholdPx && curIdx < actionSteps.lastIndex) {
                                                            val item = actionSteps.removeAt(curIdx)
                                                            actionSteps.add(curIdx + 1, item)
                                                            dragOffsetY -= curHeight
                                                            HapticEngine.dragTick(view)
                                                        } else if (dragOffsetY < -thresholdPx && curIdx > 0) {
                                                            val item = actionSteps.removeAt(curIdx)
                                                            actionSteps.add(curIdx - 1, item)
                                                            dragOffsetY += curHeight
                                                            HapticEngine.dragTick(view)
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                    onUpdate = { updated ->
                                        val i = actionSteps.indexOfFirst { it.id == step.id }
                                        if (i >= 0) actionSteps[i] = updated
                                    },
                                    onMoveUp = {
                                        val i = actionSteps.indexOfFirst { it.id == step.id }
                                        if (i > 0) {
                                            HapticEngine.dragTick(view)
                                            val item = actionSteps.removeAt(i)
                                            actionSteps.add(i - 1, item)
                                        }
                                    },
                                    onMoveDown = {
                                        val i = actionSteps.indexOfFirst { it.id == step.id }
                                        if (i in 0 until actionSteps.lastIndex) {
                                            HapticEngine.dragTick(view)
                                            val item = actionSteps.removeAt(i)
                                            actionSteps.add(i + 1, item)
                                        }
                                    },
                                    onRemove = {
                                        val i = actionSteps.indexOfFirst { it.id == step.id }
                                        if (i >= 0) {
                                            HapticEngine.click(view)
                                            actionSteps.removeAt(i)
                                        }
                                    },
                                    onOpenTilePicker = { tilePickerTargetStepId = step.id },
                                    onOpenKillAppsPicker = { killAppsTargetStepId = step.id },
                                    onOpenScriptScanner = { scriptScannerTargetStepId = step.id }
                                )
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = {
                        HapticEngine.click(view)
                        showAddActionSheet = true
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_add_action_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.editor_add_action))
                }

                // SECTION 4: Optional Exit Revert Shell Script
                TonalSectionHeader(stringResource(R.string.editor_section_revert_cmd))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TonalTextField(
                            value = revertCmd,
                            onValueChange = { revertCmd = it },
                            placeholder = { Text(stringResource(R.string.editor_revert_cmd_hint)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                            minLines = 2,
                            maxLines = 5,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilledTonalButton(
                            onClick = {
                                HapticEngine.click(view)
                                scriptScannerTargetStepId = "__REVERT__"
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.scan_local_scripts))
                        }
                    }
                }
            }
        }
    }

    // Add Action Step Catalog Bottom Sheet
    if (showAddActionSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        FluxModalBottomSheet(
            onDismissRequest = { showAddActionSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.editor_add_action),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + 24.dp),
                    modifier = Modifier.height(420.dp)
                ) {
                    itemsIndexed(ActionStepType.entries) { _, stepType ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    HapticEngine.confirm(view)
                                    actionSteps.add(ActionStep.defaultFor(stepType))
                                    showAddActionSheet = false
                                }
                                .testTag("add_step_${stepType.key}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = stepIcon(stepType),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = stringResource(stepTitleRes(stepType)),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTriggerAppPicker) {
        AppPickerSheet(
            multiSelect = advancedMode,
            initialSelection = triggerPkgs,
            onDismiss = { showTriggerAppPicker = false },
            onConfirm = {
                triggerPkgs = it
                showTriggerAppPicker = false
            }
        )
    }

    if (showExtraCondDialog) {
        ExtraConditionDialog(
            onDismiss = { showExtraCondDialog = false },
            onAdd = {
                extraConditions.add(it)
                showExtraCondDialog = false
            }
        )
    }

    val btList = btCandidatesDialog
    if (btList != null) {
        AlertDialog(
            onDismissRequest = { btCandidatesDialog = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                TransparentDialogSystemBars()
                Text(stringResource(R.string.trigger_bt_detect))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    btList.forEach { dev ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    HapticEngine.click(view)
                                    triggerTarget = dev
                                    btCandidatesDialog = null
                                }
                        ) {
                            Text(
                                text = dev,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { btCandidatesDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    val tileStepId = tilePickerTargetStepId
    if (tileStepId != null) {
        val currentStep = actionSteps.firstOrNull { it.id == tileStepId }
        QsTilePickerSheet(
            currentComponent = currentStep?.stringValue.orEmpty(),
            onDismiss = { tilePickerTargetStepId = null },
            onSelect = { comp, label ->
                val i = actionSteps.indexOfFirst { it.id == tileStepId }
                if (i >= 0) {
                    actionSteps[i] = actionSteps[i].copy(stringValue = comp, secondaryString = label)
                }
                tilePickerTargetStepId = null
            }
        )
    }

    val killStepId = killAppsTargetStepId
    if (killStepId != null) {
        val currentStep = actionSteps.firstOrNull { it.id == killStepId }
        AppPickerSheet(
            multiSelect = true,
            initialSelection = currentStep?.stringList.orEmpty(),
            onDismiss = { killAppsTargetStepId = null },
            onConfirm = { picked ->
                val i = actionSteps.indexOfFirst { it.id == killStepId }
                if (i >= 0) {
                    actionSteps[i] = actionSteps[i].copy(stringList = picked)
                }
                killAppsTargetStepId = null
            }
        )
    }

    val scannerTarget = scriptScannerTargetStepId
    if (scannerTarget != null) {
        ScriptScannerSheet(
            scripts = scannedScripts,
            isScanning = isScanningScripts,
            onRescan = onScanScripts,
            onReadPreview = onReadScriptPreview,
            onDismiss = { scriptScannerTargetStepId = null },
            onPickCommand = { cmd ->
                if (scannerTarget == "__REVERT__") {
                    revertCmd = cmd
                } else {
                    val i = actionSteps.indexOfFirst { it.id == scannerTarget }
                    if (i >= 0) {
                        actionSteps[i] = actionSteps[i].copy(stringValue = cmd)
                    }
                }
                scriptScannerTargetStepId = null
            }
        )
    }

    if (showShortcutDialog) {
        ShortcutPinDialog(
            initialLabel = name.ifBlank { stringResource(R.string.editor_rule_name_default) },
            onDismiss = { showShortcutDialog = false },
            onConfirm = { customLabel ->
                val draft = buildRuleDraft()
                onSave(draft)
                onPinShortcut(draft, customLabel)
                showShortcutDialog = false
            }
        )
    }
}

@Composable
private fun ActionStepPipelineCard(
    stepNumber: Int,
    totalSteps: Int,
    step: ActionStep,
    isDragging: Boolean,
    modifier: Modifier,
    dragHandleModifier: Modifier,
    onUpdate: (ActionStep) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onOpenTilePicker: () -> Unit,
    onOpenKillAppsPicker: () -> Unit,
    onOpenScriptScanner: () -> Unit
) {
    val view = LocalView.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isDragging) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag Handle (touch target >= 48dp)
                Box(
                    modifier = dragHandleModifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = stringResource(R.string.action_drag_handle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Sequence badge with tnum
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = stepIcon(step.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(stepTitleRes(step.type)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (totalSteps > 1) {
                    IconButton(
                        enabled = stepNumber > 1,
                        onClick = onMoveUp,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = stringResource(R.string.action_move_up),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        enabled = stepNumber < totalSteps,
                        onClick = onMoveDown,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ArrowDownward,
                            contentDescription = stringResource(R.string.action_move_down),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_remove),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Step-specific inline configuration controls
            when (step.type) {
                ActionStepType.QS_TILE -> {
                    FilledTonalButton(
                        onClick = {
                            HapticEngine.click(view)
                            onOpenTilePicker()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = step.secondaryString.ifBlank {
                                step.stringValue.ifBlank { stringResource(R.string.pick_tile_btn) }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ActionStepType.KILL_APPS -> {
                    FilledTonalButton(
                        onClick = {
                            HapticEngine.click(view)
                            onOpenKillAppsPicker()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (step.stringList.isEmpty()) {
                                stringResource(R.string.pick_kill_apps_btn)
                            } else {
                                "${stringResource(R.string.pick_kill_apps_btn)} (${step.stringList.size})"
                            }
                        )
                    }
                }
                ActionStepType.DND_MODE -> {
                    val modes = listOf(0 to R.string.dnd_off, 1 to R.string.dnd_priority, 2 to R.string.dnd_total)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { (code, res) ->
                            TonalFilterChip(
                                selected = step.intValue == code,
                                onClick = {
                                    HapticEngine.click(view)
                                    onUpdate(step.copy(intValue = code))
                                },
                                label = { Text(stringResource(res)) }
                            )
                        }
                    }
                }
                ActionStepType.REFRESH_RATE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120).forEach { hz ->
                            TonalFilterChip(
                                selected = step.intValue == hz,
                                onClick = {
                                    HapticEngine.click(view)
                                    onUpdate(step.copy(intValue = hz))
                                },
                                label = { Text("${hz}Hz", style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
                ActionStepType.BRIGHTNESS -> {
                    val hbmEnabled = step.boolValue
                    val displayPct = if (hbmEnabled) 100 else step.intValue.coerceIn(0, 100)
                    ExpressivePercentSlider(
                        value = displayPct,
                        onValueChange = { pct ->
                            onUpdate(
                                step.copy(
                                    intValue = pct,
                                    boolValue = false,
                                    stringValue = ActionStep.BRIGHTNESS_UNIT_PCT
                                )
                            )
                        },
                        enabled = !hbmEnabled,
                        isPeakOverride = hbmEnabled,
                        leadingIcon = if (hbmEnabled) Icons.Rounded.WbSunny else Icons.Rounded.Brightness6,
                        contentDescription = stringResource(R.string.brightness_slider_cd),
                        modifier = Modifier.testTag("brightness_slider")
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hbmEnabled) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = !hbmEnabled
                                    HapticEngine.toggle(view, next)
                                    onUpdate(
                                        step.copy(
                                            boolValue = next,
                                            stringValue = ActionStep.BRIGHTNESS_UNIT_PCT
                                        )
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.brightness_hbm_toggle),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(10.dp))
                                Switch(
                                    checked = hbmEnabled,
                                    onCheckedChange = { enabled ->
                                        HapticEngine.toggle(view, enabled)
                                        onUpdate(
                                            step.copy(
                                                boolValue = enabled,
                                                stringValue = ActionStep.BRIGHTNESS_UNIT_PCT
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("brightness_hbm_switch")
                                )
                            }
                            AnimatedVisibility(visible = hbmEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = stringResource(R.string.brightness_hbm_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.brightness_hbm_warning),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                ActionStepType.MEDIA_VOLUME -> {
                    ExpressivePercentSlider(
                        value = step.intValue.coerceIn(0, 100),
                        onValueChange = { pct -> onUpdate(step.copy(intValue = pct)) },
                        leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = stringResource(R.string.media_volume_slider_cd),
                        modifier = Modifier.testTag("media_volume_slider")
                    )
                }
                ActionStepType.WIFI,
                ActionStepType.MOBILE_DATA,
                ActionStepType.AIRPLANE_MODE,
                ActionStepType.BATTERY_SAVER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TonalFilterChip(
                            selected = step.boolValue,
                            onClick = {
                                HapticEngine.click(view)
                                onUpdate(step.copy(boolValue = true))
                            },
                            label = { Text(stringResource(R.string.state_enable)) }
                        )
                        TonalFilterChip(
                            selected = !step.boolValue,
                            onClick = {
                                HapticEngine.click(view)
                                onUpdate(step.copy(boolValue = false))
                            },
                            label = { Text(stringResource(R.string.state_disable)) }
                        )
                    }
                }
                ActionStepType.DARK_MODE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TonalFilterChip(
                            selected = step.boolValue,
                            onClick = {
                                HapticEngine.click(view)
                                onUpdate(step.copy(boolValue = true))
                            },
                            label = { Text(stringResource(R.string.dark_on)) }
                        )
                        TonalFilterChip(
                            selected = !step.boolValue,
                            onClick = {
                                HapticEngine.click(view)
                                onUpdate(step.copy(boolValue = false))
                            },
                            label = { Text(stringResource(R.string.dark_off)) }
                        )
                    }
                }
                ActionStepType.AUTO_ROTATE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TonalFilterChip(
                            selected = step.boolValue,
                            onClick = {
                                HapticEngine.click(view)
                                onUpdate(step.copy(boolValue = true))
                            },
                            label = { Text(stringResource(R.string.rotate_on)) }
                        )
                        TonalFilterChip(
                            selected = !step.boolValue,
                            onClick = {
                                HapticEngine.click(view)
                                onUpdate(step.copy(boolValue = false))
                            },
                            label = { Text(stringResource(R.string.rotate_off)) }
                        )
                    }
                }
                ActionStepType.VIBRATE -> {
                    val ms = step.intValue.coerceIn(20, 300)
                    Text(
                        text = "${ms}ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = ms.toFloat(),
                        onValueChange = { onUpdate(step.copy(intValue = it.roundToInt())) },
                        valueRange = 20f..300f
                    )
                }
                ActionStepType.DELAY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(200, 500, 1000, 2000, 3000).forEach { ms ->
                            TonalFilterChip(
                                selected = step.intValue == ms,
                                onClick = {
                                    HapticEngine.click(view)
                                    onUpdate(step.copy(intValue = ms))
                                },
                                label = { Text("${ms}ms", style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
                ActionStepType.CUSTOM_SHELL -> {
                    TonalTextField(
                        value = step.stringValue,
                        onValueChange = { onUpdate(step.copy(stringValue = it)) },
                        placeholder = { Text(stringResource(R.string.custom_shell_hint)) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        minLines = 2,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                HapticEngine.click(view)
                                onOpenScriptScanner()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.scan_local_scripts), style = MaterialTheme.typography.labelMedium)
                        }
                        QuickCommandPreset.DEFAULTS.take(3).forEach { preset ->
                            TonalAssistChip(
                                onClick = {
                                    HapticEngine.click(view)
                                    onUpdate(step.copy(stringValue = preset.command))
                                },
                                label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun triggerDirectionText(type: TriggerType, on: Boolean, adv: Boolean): String = when (type) {
    TriggerType.APP -> when {
        adv && on -> stringResource(R.string.trigger_app_enter_multi)
        adv && !on -> stringResource(R.string.trigger_app_exit_multi)
        on -> stringResource(R.string.trigger_app_enter)
        else -> stringResource(R.string.trigger_app_exit)
    }
    TriggerType.CHARGING -> stringResource(if (on) R.string.trigger_charging_on else R.string.trigger_charging_off)
    TriggerType.SCREEN -> stringResource(if (on) R.string.trigger_screen_on else R.string.trigger_screen_off)
    TriggerType.HEADSET -> stringResource(if (on) R.string.trigger_headset_on else R.string.trigger_headset_off)
    TriggerType.ORIENTATION -> stringResource(if (on) R.string.trigger_orient_land else R.string.trigger_orient_port)
    else -> stringResource(if (on) R.string.trigger_state_on else R.string.trigger_state_off)
}

private fun stepIcon(type: ActionStepType): ImageVector = when (type) {
    ActionStepType.QS_TILE -> Icons.Rounded.Tune
    ActionStepType.KILL_APPS -> Icons.Rounded.PowerSettingsNew
    ActionStepType.DND_MODE -> Icons.Rounded.DoNotDisturbOn
    ActionStepType.REFRESH_RATE -> Icons.Rounded.Speed
    ActionStepType.BRIGHTNESS -> Icons.Rounded.Brightness6
    ActionStepType.MEDIA_VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
    ActionStepType.WIFI -> Icons.Rounded.Wifi
    ActionStepType.MOBILE_DATA -> Icons.Rounded.SignalCellularAlt
    ActionStepType.AIRPLANE_MODE -> Icons.Rounded.AirplanemodeActive
    ActionStepType.BATTERY_SAVER -> Icons.Rounded.BatterySaver
    ActionStepType.DARK_MODE -> Icons.Rounded.DarkMode
    ActionStepType.AUTO_ROTATE -> Icons.Rounded.ScreenRotation
    ActionStepType.VIBRATE -> Icons.Rounded.Vibration
    ActionStepType.CUSTOM_SHELL -> Icons.Rounded.Terminal
    ActionStepType.DELAY -> Icons.Rounded.Timer
}

@StringRes
private fun stepTitleRes(type: ActionStepType): Int = when (type) {
    ActionStepType.QS_TILE -> R.string.step_qs_tile
    ActionStepType.KILL_APPS -> R.string.step_kill_apps
    ActionStepType.DND_MODE -> R.string.step_dnd
    ActionStepType.REFRESH_RATE -> R.string.step_refresh_rate
    ActionStepType.BRIGHTNESS -> R.string.step_brightness
    ActionStepType.MEDIA_VOLUME -> R.string.step_media_volume
    ActionStepType.WIFI -> R.string.step_wifi
    ActionStepType.MOBILE_DATA -> R.string.step_mobile_data
    ActionStepType.AIRPLANE_MODE -> R.string.step_airplane
    ActionStepType.BATTERY_SAVER -> R.string.step_battery_saver
    ActionStepType.DARK_MODE -> R.string.step_dark_mode
    ActionStepType.AUTO_ROTATE -> R.string.step_auto_rotate
    ActionStepType.VIBRATE -> R.string.step_vibrate
    ActionStepType.CUSTOM_SHELL -> R.string.step_custom_shell
    ActionStepType.DELAY -> R.string.step_delay
}
