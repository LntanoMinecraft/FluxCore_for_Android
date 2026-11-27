package com.mouajmd.fluxcore.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PauseCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.LogEntry
import com.mouajmd.fluxcore.data.LogLevel
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.components.TonalFilterChip
import com.mouajmd.fluxcore.ui.components.TonalTextField
import com.mouajmd.fluxcore.ui.theme.JetBrainsMonoFamily

private data class LogLevelCounts(
    val info: Int,
    val warn: Int,
    val error: Int
)

@Composable
fun LogScreen(
    logs: List<LogEntry>,
    isWideScreen: Boolean,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var searchQuery by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<LogLevel?>(null) }
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val levelCounts = remember(logs) {
        var info = 0
        var warn = 0
        var error = 0
        for (entry in logs) {
            when (entry.level) {
                LogLevel.INFO -> info++
                LogLevel.WARN -> warn++
                LogLevel.ERROR -> error++
            }
        }
        LogLevelCounts(info = info, warn = warn, error = error)
    }

    val filtered by remember(logs, searchQuery, levelFilter) {
        derivedStateOf {
            val kw = searchQuery.trim().lowercase()
            logs.filter { entry ->
                (levelFilter == null || entry.level == levelFilter) &&
                    (kw.isEmpty() || entry.tag.lowercase().contains(kw) || entry.message.lowercase().contains(kw))
            }
        }
    }

    LaunchedEffect(filtered.size, autoScroll) {
        if (autoScroll && filtered.isNotEmpty()) {
            listState.animateScrollToItem(filtered.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header row with title & toolbar actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.tab_logs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            HapticEngine.toggle(view, !autoScroll)
                            autoScroll = !autoScroll
                        },
                        modifier = Modifier.testTag("logs_autoscroll_btn")
                    ) {
                        Icon(
                            imageVector = if (autoScroll) Icons.Rounded.VerticalAlignBottom else Icons.Rounded.PauseCircleOutline,
                            contentDescription = stringResource(
                                if (autoScroll) R.string.logs_autoscroll_on else R.string.logs_autoscroll_off
                            ),
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            HapticEngine.click(view)
                            val allText = filtered.joinToString("\n") { it.toPlainLine() }
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("flux_logs", allText))
                            onShowMessage(ctx.getString(R.string.copied_to_clipboard))
                        },
                        modifier = Modifier.testTag("logs_copy_all_btn")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.logs_copy_all))
                    }
                    IconButton(
                        onClick = {
                            HapticEngine.confirm(view)
                            onExportLogs()
                        },
                        modifier = Modifier.testTag("logs_export_btn")
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.logs_export))
                    }
                    IconButton(
                        onClick = {
                            HapticEngine.reject(view)
                            onClearLogs()
                        },
                        modifier = Modifier.testTag("logs_clear_btn")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.logs_clear))
                    }
                }
            }

            // Borderless pill search bar
            TonalTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.logs_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("logs_search_input")
            )

            Spacer(Modifier.height(10.dp))

            // Edge-to-Edge borderless level filter chips with tnum counts
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "filter_all") {
                    TonalFilterChip(
                        selected = levelFilter == null,
                        onClick = {
                            HapticEngine.click(view)
                            levelFilter = null
                        },
                        label = {
                            Text(
                                text = "${stringResource(R.string.logs_filter_all)} ${logs.size}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
                item(key = "filter_info") {
                    TonalFilterChip(
                        selected = levelFilter == LogLevel.INFO,
                        onClick = {
                            HapticEngine.click(view)
                            levelFilter = LogLevel.INFO
                        },
                        label = {
                            Text(
                                text = "${stringResource(R.string.logs_filter_info)} ${levelCounts.info}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
                item(key = "filter_warn") {
                    TonalFilterChip(
                        selected = levelFilter == LogLevel.WARN,
                        onClick = {
                            HapticEngine.click(view)
                            levelFilter = LogLevel.WARN
                        },
                        label = {
                            Text(
                                text = "${stringResource(R.string.logs_filter_warn)} ${levelCounts.warn}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
                item(key = "filter_error") {
                    TonalFilterChip(
                        selected = levelFilter == LogLevel.ERROR,
                        onClick = {
                            HapticEngine.click(view)
                            levelFilter = LogLevel.ERROR
                        },
                        label = {
                            Text(
                                text = "${stringResource(R.string.logs_filter_error)} ${levelCounts.error}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.logs_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = navBarBottom + if (isWideScreen) 32.dp else 112.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { entry ->
                        LogRowCard(
                            entry = entry,
                            onCopy = {
                                HapticEngine.click(view)
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("flux_log_line", entry.toPlainLine()))
                                onShowMessage(ctx.getString(R.string.copied_to_clipboard))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRowCard(
    entry: LogEntry,
    onCopy: () -> Unit
) {
    val containerColor = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiaryContainer
        LogLevel.INFO -> MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        LogLevel.WARN -> MaterialTheme.colorScheme.onTertiaryContainer
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
    }
    val tagBg = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
        LogLevel.INFO -> MaterialTheme.colorScheme.secondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCopy)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.formattedTime,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = textColor.copy(alpha = 0.72f)
                )
                Surface(shape = CircleShape, color = tagBg) {
                    Text(
                        text = entry.tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.message,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = textColor
            )
        }
    }
}
