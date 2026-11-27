package com.mouajmd.fluxcore.ui.components

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.model.RuleCondition
import com.mouajmd.fluxcore.model.ScriptCategory
import com.mouajmd.fluxcore.model.ShellScriptItem
import com.mouajmd.fluxcore.model.TriggerType
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(180)
    fun get(packageName: String): ImageBitmap? = cache.get(packageName)
    fun put(packageName: String, bitmap: ImageBitmap) {
        cache.put(packageName, bitmap)
    }
}

@Composable
fun AppPackageIcon(
    packageName: String,
    isPicked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val iconPx = remember(density) { with(density) { 40.dp.roundToPx() }.coerceIn(64, 160) }
    var iconBitmap by remember(packageName) { mutableStateOf(AppIconCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                AppIconCache.get(packageName) ?: runCatching {
                    val drawable = ctx.packageManager.getApplicationIcon(packageName)
                    drawable.toBitmap(iconPx, iconPx).asImageBitmap().also {
                        AppIconCache.put(packageName, it)
                    }
                }.getOrNull()
            }
            if (loaded != null) {
                iconBitmap = loaded
            }
        }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isPicked) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = iconBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = if (isPicked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

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
    onConfirm: (List<String>) -> Unit,
    onSelectApp: ((packageName: String, appLabel: String) -> Unit)? = null
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
                                        if (onSelectApp != null) {
                                            onSelectApp(app.packageName, app.label)
                                        } else {
                                            onConfirm(listOf(app.packageName))
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppPackageIcon(
                                    packageName = app.packageName,
                                    isPicked = isPicked
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtraConditionSheet(
    advancedMode: Boolean,
    onDetectWifi: suspend () -> String,
    onDetectBt: suspend () -> List<String>,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (RuleCondition) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val autoTypes = remember { TriggerType.entries.filter { it != TriggerType.MANUAL } }

    var selectedType by remember { mutableStateOf(TriggerType.CHARGING) }
    var stateOn by remember { mutableStateOf(true) }
    var targetName by remember { mutableStateOf("") }
    var threshold by remember { mutableIntStateOf(20) }
    var pkgs by remember { mutableStateOf<List<String>>(emptyList()) }
    var startHour by remember { mutableIntStateOf(23) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(7) }
    var endMinute by remember { mutableIntStateOf(0) }

    var btCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf<Boolean?>(null) }

    fun formatHm(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m)

    FluxModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = navBarBottom + 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.trigger_add_extra),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.cond_type_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    autoTypes.forEach { t ->
                        val isSel = selectedType == t
                        TonalFilterChip(
                            selected = isSel,
                            onClick = {
                                if (!isSel) {
                                    HapticEngine.click(view)
                                    selectedType = t
                                    stateOn = true
                                    targetName = ""
                                    btCandidates = emptyList()
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = triggerTypeIcon(t),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = triggerTypeLabel(t),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.testTag("extra_cond_chip_${t.name.lowercase()}")
                        )
                    }
                }
            }

            // Dynamic semantic parameter card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = selectedType,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "extraCondParams"
                ) { type ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.cond_config_section)} · ${triggerTypeLabel(type)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        when (type) {
                            TriggerType.APP -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.trigger_app_enter),
                                        false to stringResource(R.string.trigger_app_exit)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_app_mode"
                                )
                                FilledTonalButton(
                                    onClick = {
                                        HapticEngine.click(view)
                                        showAppPicker = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("extra_cond_pick_apps_btn")
                                ) {
                                    Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (pkgs.isEmpty()) {
                                            stringResource(R.string.trigger_pick_apps)
                                        } else {
                                            stringResource(R.string.trigger_selected_apps, pkgs.size, pkgs.take(2).joinToString(", "))
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            TriggerType.CHARGING -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.trigger_charging_on),
                                        false to stringResource(R.string.trigger_charging_off)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_charging_mode"
                                )
                            }

                            TriggerType.SCREEN -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.cond_screen_on),
                                        false to stringResource(R.string.cond_screen_off)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_screen_mode"
                                )
                            }

                            TriggerType.WIFI -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.cond_connected),
                                        false to stringResource(R.string.cond_disconnected)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_wifi_mode"
                                )
                                if (advancedMode) {
                                    TonalTextField(
                                        value = targetName,
                                        onValueChange = { targetName = it },
                                        placeholder = { Text(stringResource(R.string.trigger_wifi_ssid_hint)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            HapticEngine.click(view)
                                            scope.launch {
                                                val ssid = onDetectWifi()
                                                if (ssid.isNotEmpty()) {
                                                    targetName = ssid
                                                    onShowMessage("Wi-Fi: $ssid")
                                                } else {
                                                    onShowMessage(ctx.getString(R.string.cond_wifi_not_connected))
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.trigger_wifi_detect))
                                    }
                                }
                            }

                            TriggerType.BLUETOOTH -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.cond_connected),
                                        false to stringResource(R.string.cond_disconnected)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_bt_mode"
                                )
                                if (advancedMode) {
                                    TonalTextField(
                                        value = targetName,
                                        onValueChange = { targetName = it },
                                        placeholder = { Text(stringResource(R.string.trigger_bt_name_hint)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            HapticEngine.click(view)
                                            scope.launch {
                                                val list = onDetectBt()
                                                when {
                                                    list.isEmpty() -> {
                                                        btCandidates = emptyList()
                                                        onShowMessage(ctx.getString(R.string.cond_bt_none_detected))
                                                    }
                                                    list.size == 1 -> {
                                                        btCandidates = emptyList()
                                                        targetName = list[0]
                                                        onShowMessage(list[0])
                                                    }
                                                    else -> btCandidates = list
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.trigger_bt_detect))
                                    }
                                    if (btCandidates.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            btCandidates.forEach { dev ->
                                                TonalFilterChip(
                                                    selected = targetName == dev,
                                                    onClick = {
                                                        HapticEngine.click(view)
                                                        targetName = dev
                                                    },
                                                    label = { Text(dev, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            TriggerType.BATTERY -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.cond_battery_gte),
                                        false to stringResource(R.string.cond_battery_lte)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_battery_mode"
                                )
                                ExpressivePercentSlider(
                                    value = threshold.coerceIn(1, 100),
                                    onValueChange = { threshold = it.coerceIn(1, 100) },
                                    leadingIcon = Icons.Rounded.BatteryStd,
                                    contentDescription = triggerTypeLabel(TriggerType.BATTERY)
                                )
                            }

                            TriggerType.HEADSET -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.trigger_headset_on),
                                        false to stringResource(R.string.trigger_headset_off)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_headset_mode"
                                )
                            }

                            TriggerType.ORIENTATION -> {
                                TonalSegmentedControl(
                                    options = listOf(
                                        true to stringResource(R.string.trigger_orient_land),
                                        false to stringResource(R.string.trigger_orient_port)
                                    ),
                                    selected = stateOn,
                                    onSelect = { stateOn = it },
                                    tagPrefix = "extra_orient_mode"
                                )
                            }

                            TriggerType.TIME -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TimeSlotCard(
                                        label = stringResource(R.string.cond_time_start),
                                        value = formatHm(startHour, startMinute),
                                        onClick = {
                                            HapticEngine.click(view)
                                            pickingStartTime = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("extra_time_start_btn")
                                    )
                                    TimeSlotCard(
                                        label = stringResource(R.string.cond_time_end),
                                        value = formatHm(endHour, endMinute),
                                        onClick = {
                                            HapticEngine.click(view)
                                            pickingStartTime = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("extra_time_end_btn")
                                    )
                                }
                                val isOvernight = (startHour * 60 + startMinute) > (endHour * 60 + endMinute)
                                if (isOvernight) {
                                    Text(
                                        text = "${stringResource(R.string.cond_time_overnight)} · ${formatHm(startHour, startMinute)} – ${formatHm(endHour, endMinute)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple(23, 0, Pair(7, 0)),
                                        Triple(9, 0, Pair(18, 0)),
                                        Triple(12, 0, Pair(14, 0))
                                    ).forEach { (sh, sm, endPair) ->
                                        val label = "${formatHm(sh, sm)}-${formatHm(endPair.first, endPair.second)}"
                                        TonalAssistChip(
                                            onClick = {
                                                HapticEngine.click(view)
                                                startHour = sh
                                                startMinute = sm
                                                endHour = endPair.first
                                                endMinute = endPair.second
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }

                            TriggerType.MANUAL -> Unit
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticEngine.click(view)
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("extra_cond_cancel_btn")
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
                Button(
                    onClick = {
                        if (selectedType == TriggerType.APP && pkgs.isEmpty()) {
                            HapticEngine.reject(view)
                            onShowMessage(ctx.getString(R.string.cond_app_empty_hint))
                            return@Button
                        }
                        HapticEngine.confirm(view)
                        val resolvedTarget = if (selectedType == TriggerType.TIME) {
                            "${formatHm(startHour, startMinute)}-${formatHm(endHour, endMinute)}"
                        } else {
                            targetName.trim()
                        }
                        onAdd(
                            RuleCondition(
                                type = selectedType,
                                stateOn = stateOn,
                                pkgs = pkgs,
                                targetName = resolvedTarget,
                                threshold = threshold.coerceIn(1, 100)
                            )
                        )
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("extra_cond_confirm_btn")
                ) {
                    Text(stringResource(R.string.cond_add_btn))
                }
            }
        }
    }

    val isStart = pickingStartTime
    if (isStart != null) {
        val pickerState = rememberTimePickerState(
            initialHour = if (isStart) startHour else endHour,
            initialMinute = if (isStart) startMinute else endMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { pickingStartTime = null },
            shape = RoundedCornerShape(26.dp),
            title = {
                TransparentDialogSystemBars()
                Text(
                    stringResource(if (isStart) R.string.cond_time_start else R.string.cond_time_end),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticEngine.confirm(view)
                        if (isStart) {
                            startHour = pickerState.hour
                            startMinute = pickerState.minute
                        } else {
                            endHour = pickerState.hour
                            endMinute = pickerState.minute
                        }
                        pickingStartTime = null
                    }
                ) {
                    Text(stringResource(R.string.btn_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartTime = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

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
private fun TimeSlotCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun triggerTypeIcon(type: TriggerType): ImageVector = when (type) {
    TriggerType.MANUAL -> Icons.Rounded.Tune
    TriggerType.APP -> Icons.Rounded.Apps
    TriggerType.CHARGING -> Icons.Rounded.BatteryChargingFull
    TriggerType.SCREEN -> Icons.Rounded.Smartphone
    TriggerType.WIFI -> Icons.Rounded.Wifi
    TriggerType.BLUETOOTH -> Icons.Rounded.Bluetooth
    TriggerType.BATTERY -> Icons.Rounded.BatteryStd
    TriggerType.HEADSET -> Icons.Rounded.Headphones
    TriggerType.ORIENTATION -> Icons.Rounded.ScreenRotation
    TriggerType.TIME -> Icons.Rounded.Schedule
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
