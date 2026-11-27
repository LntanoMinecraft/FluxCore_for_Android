package com.mouajmd.fluxcore.ui.components

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.sp
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.model.RuleCondition
import com.mouajmd.fluxcore.model.ScriptCategory
import com.mouajmd.fluxcore.model.ShellScriptItem
import com.mouajmd.fluxcore.model.TriggerType
import com.mouajmd.fluxcore.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class InstalledAppEntry(
    val label: String,
    val packageName: String,
    val isSystem: Boolean
)

private data class QsTileEntry(
    val label: String,
    val appName: String,
    val component: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    multiSelect: Boolean,
    initialSelection: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<InstalledAppEntry>>(emptyList()) }
    val selected = remember { mutableStateListOf<String>().apply { addAll(initialSelection) } }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val raw = runCatching { pm.getInstalledApplications(0) }.getOrDefault(emptyList())
            raw.map { info ->
                val isSys = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val lbl = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(info.packageName)
                InstalledAppEntry(lbl, info.packageName, isSys)
            }.sortedWith(
                compareByDescending<InstalledAppEntry> { it.packageName in initialSelection }
                    .thenBy { it.label.lowercase() }
            )
        }
        apps = loaded
        loading = false
    }

    val filtered = remember(apps, search, showSystem, selected.toList()) {
        val kw = search.trim().lowercase()
        apps.filter { entry ->
            (showSystem || !entry.isSystem || entry.packageName in selected) &&
                (kw.isEmpty() || entry.label.lowercase().contains(kw) || entry.packageName.lowercase().contains(kw))
        }.sortedByDescending { it.packageName in selected }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    FluxModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.picker_apps_title),
                    style = MaterialTheme.typography.titleLarge
                )
                if (multiSelect) {
                    Button(
                        onClick = {
                            HapticEngine.confirm(view)
                            onConfirm(selected.toList())
                        },
                        modifier = Modifier.testTag("app_picker_confirm")
                    ) {
                        Text(text = "${stringResource(R.string.btn_confirm)} (${selected.size})")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TonalTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(stringResource(R.string.picker_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.picker_show_system),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = showSystem,
                    onCheckedChange = {
                        HapticEngine.toggle(view, it)
                        showSystem = it
                    }
                )
            }
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + 24.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isPicked = app.packageName in selected
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isPicked) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    HapticEngine.click(view)
                                    if (multiSelect) {
                                        if (isPicked) selected.remove(app.packageName)
                                        else selected.add(app.packageName)
                                    } else {
                                        onConfirm(listOf(app.packageName))
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = if (isPicked) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (multiSelect) {
                                    Checkbox(checked = isPicked, onCheckedChange = null)
                                } else if (isPicked) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QsTilePickerSheet(
    currentComponent: String,
    onDismiss: () -> Unit,
    onSelect: (component: String, label: String) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var tiles by remember { mutableStateOf<List<QsTileEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        tiles = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val ris = runCatching {
                pm.queryIntentServices(Intent("android.service.quicksettings.action.QS_TILE"), 0)
            }.getOrDefault(emptyList())
            ris.mapNotNull { ri ->
                val si = ri.serviceInfo ?: return@mapNotNull null
                val comp = "${si.packageName}/${si.name}"
                val lbl = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(si.name)
                val appName = runCatching { si.applicationInfo.loadLabel(pm).toString() }.getOrDefault(si.packageName)
                QsTileEntry(lbl, appName, comp)
            }.sortedWith(
                compareByDescending<QsTileEntry> { it.component == currentComponent }
                    .thenBy { it.appName.lowercase() }
            )
        }
        loading = false
    }

    val filtered = remember(tiles, search) {
        val kw = search.trim().lowercase()
        if (kw.isEmpty()) tiles else tiles.filter {
            it.label.lowercase().contains(kw) ||
                it.appName.lowercase().contains(kw) ||
                it.component.lowercase().contains(kw)
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    FluxModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.picker_tiles_title),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(12.dp))
            TonalTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(stringResource(R.string.picker_tiles_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (loading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = navBarBottom + 24.dp)
                ) {
                    items(filtered, key = { it.component }) { tile ->
                        val active = tile.component == currentComponent
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    HapticEngine.confirm(view)
                                    onSelect(tile.component, "${tile.label} (${tile.appName})")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tile.label} · ${tile.appName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tile.component,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptScannerSheet(
    scripts: List<ShellScriptItem>,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onReadPreview: suspend (String) -> String,
    onDismiss: () -> Unit,
    onPickCommand: (String) -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<ScriptCategory?>(null) }
    var previewItem by remember { mutableStateOf<ShellScriptItem?>(null) }
    var previewContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (scripts.isEmpty() && !isScanning) {
            onRescan()
        }
    }

    val filtered = remember(scripts, search, categoryFilter) {
        val kw = search.trim().lowercase()
        scripts.filter { item ->
            (categoryFilter == null || item.category == categoryFilter) &&
                (kw.isEmpty() || item.fileName.lowercase().contains(kw) || item.path.lowercase().contains(kw))
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    FluxModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.scanner_title),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    onClick = {
                        HapticEngine.click(view)
                        onRescan()
                    },
                    modifier = Modifier.testTag("scanner_rescan_btn")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.scanner_rescan))
                }
            }
            Spacer(Modifier.height(10.dp))
            TonalTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text(stringResource(R.string.scanner_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TonalFilterChip(
                    selected = categoryFilter == null,
                    onClick = {
                        HapticEngine.click(view)
                        categoryFilter = null
                    },
                    label = { Text("${stringResource(R.string.scanner_cat_all)} (${scripts.size})") }
                )
                TonalFilterChip(
                    selected = categoryFilter == ScriptCategory.STORAGE,
                    onClick = {
                        HapticEngine.click(view)
                        categoryFilter = ScriptCategory.STORAGE
                    },
                    label = { Text(stringResource(R.string.scanner_cat_storage)) }
                )
                TonalFilterChip(
                    selected = categoryFilter == ScriptCategory.ADB,
                    onClick = {
                        HapticEngine.click(view)
                        categoryFilter = ScriptCategory.ADB
                    },
                    label = { Text(stringResource(R.string.scanner_cat_adb)) }
                )
            }
            Spacer(Modifier.height(10.dp))

            when {
                isScanning -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.scanner_scanning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                filtered.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.scanner_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val dateFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = navBarBottom + 24.dp)
                    ) {
                        items(filtered, key = { it.path }) { item ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        HapticEngine.click(view)
                                        previewItem = item
                                        previewContent = null
                                        scope.launch {
                                            previewContent = onReadPreview(item.path)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.fileName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.parentDir,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sizeStr = formatBytes(item.sizeBytes)
                                        val timeStr = if (item.lastModifiedMs > 0L) dateFmt.format(Date(item.lastModifiedMs)) else ""
                                        Text(
                                            text = listOfNotNull(sizeStr, timeStr.ifBlank { null }).joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Read-only preview dialog before choosing to reference path or insert script contents
    val activePreview = previewItem
    if (activePreview != null) {
        AlertDialog(
            onDismissRequest = { previewItem = null },
            shape = RoundedCornerShape(26.dp),
            title = {
                TransparentDialogSystemBars()
                Column {
                    Text(
                        text = activePreview.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.scanner_preview_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 300.dp)
                ) {
                    val body = previewContent
                    if (body == null) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else {
                        Text(
                            text = body.ifBlank { "# (empty file)" },
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            HapticEngine.confirm(view)
                            val escaped = activePreview.path.replace("'", "'\\''")
                            onPickCommand("sh '$escaped'")
                            previewItem = null
                        },
                        modifier = Modifier.testTag("scanner_use_path_btn")
                    ) {
                        Text(stringResource(R.string.scanner_use_path))
                    }
                    Button(
                        enabled = previewContent != null,
                        onClick = {
                            HapticEngine.confirm(view)
                            onPickCommand(previewContent.orEmpty().trim())
                            previewItem = null
                        },
                        modifier = Modifier.testTag("scanner_insert_content_btn")
                    ) {
                        Text(stringResource(R.string.scanner_insert_content))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { previewItem = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
    else -> "$bytes B"
}

@Composable
fun ShortcutPinDialog(
    initialLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val view = LocalView.current
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = {
            TransparentDialogSystemBars()
            Text(stringResource(R.string.dialog_shortcut_title))
        },
        text = {
            TonalTextField(
                value = label,
                onValueChange = { label = it.take(24) },
                label = { Text(stringResource(R.string.dialog_shortcut_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    HapticEngine.confirm(view)
                    onConfirm(label.trim())
                },
                modifier = Modifier.testTag("shortcut_pin_confirm_btn")
            ) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun ExtraConditionDialog(
    onDismiss: () -> Unit,
    onAdd: (RuleCondition) -> Unit
) {
    val view = LocalView.current
    val autoTypes = remember { TriggerType.entries.filter { it != TriggerType.MANUAL } }
    var selectedType by remember { mutableStateOf(TriggerType.CHARGING) }
    var stateOn by remember { mutableStateOf(true) }
    var targetName by remember { mutableStateOf("") }
    var threshold by remember { mutableFloatStateOf(20f) }
    var pkgs by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = {
            TransparentDialogSystemBars()
            Text(stringResource(R.string.trigger_add_extra))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    autoTypes.forEach { t ->
                        TonalFilterChip(
                            selected = selectedType == t,
                            onClick = {
                                HapticEngine.click(view)
                                selectedType = t
                            },
                            label = { Text(triggerTypeLabel(t)) }
                        )
                    }
                }

                if (selectedType != TriggerType.TIME) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (stateOn) stringResource(R.string.trigger_state_on) else stringResource(R.string.trigger_state_off),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = stateOn,
                            onCheckedChange = {
                                HapticEngine.toggle(view, it)
                                stateOn = it
                            }
                        )
                    }
                }

                when (selectedType) {
                    TriggerType.APP -> {
                        FilledTonalButton(
                            onClick = { showAppPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (pkgs.isEmpty()) stringResource(R.string.trigger_pick_apps)
                                else "${stringResource(R.string.trigger_pick_apps)} (${pkgs.size})"
                            )
                        }
                    }
                    TriggerType.WIFI -> {
                        TonalTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            placeholder = { Text(stringResource(R.string.trigger_wifi_ssid_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerType.BLUETOOTH -> {
                        TonalTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            placeholder = { Text(stringResource(R.string.trigger_bt_name_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TriggerType.BATTERY -> {
                        Text(
                            text = if (stateOn) {
                                stringResource(R.string.trigger_battery_above, threshold.roundToInt())
                            } else {
                                stringResource(R.string.trigger_battery_below, threshold.roundToInt())
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = threshold,
                            onValueChange = { threshold = it },
                            valueRange = 1f..100f
                        )
                    }
                    TriggerType.TIME -> {
                        TonalTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            placeholder = { Text(stringResource(R.string.trigger_time_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    HapticEngine.confirm(view)
                    onAdd(
                        RuleCondition(
                            type = selectedType,
                            stateOn = stateOn,
                            pkgs = pkgs,
                            targetName = targetName.trim(),
                            threshold = threshold.roundToInt()
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )

    if (showAppPicker) {
        AppPickerSheet(
            multiSelect = true,
            initialSelection = pkgs,
            onDismiss = { showAppPicker = false },
            onConfirm = {
                pkgs = it
                showAppPicker = false
            }
        )
    }
}

@Composable
fun triggerTypeLabel(type: TriggerType): String = when (type) {
    TriggerType.MANUAL -> stringResource(R.string.trigger_manual)
    TriggerType.APP -> stringResource(R.string.trigger_app)
    TriggerType.CHARGING -> stringResource(R.string.trigger_charging)
    TriggerType.SCREEN -> stringResource(R.string.trigger_screen)
    TriggerType.WIFI -> stringResource(R.string.trigger_wifi)
    TriggerType.BLUETOOTH -> stringResource(R.string.trigger_bt)
    TriggerType.BATTERY -> stringResource(R.string.trigger_battery)
    TriggerType.HEADSET -> stringResource(R.string.trigger_headset)
    TriggerType.ORIENTATION -> stringResource(R.string.trigger_orientation)
    TriggerType.TIME -> stringResource(R.string.trigger_time)
}
