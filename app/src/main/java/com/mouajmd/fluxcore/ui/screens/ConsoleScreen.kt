package com.mouajmd.fluxcore.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.model.QuickCommandPreset
import com.mouajmd.fluxcore.model.ShellScriptItem
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.components.ScriptScannerSheet
import com.mouajmd.fluxcore.ui.components.TonalAssistChip
import com.mouajmd.fluxcore.ui.components.TonalTextField
import com.mouajmd.fluxcore.ui.theme.JetBrainsMonoFamily

@Composable
fun ConsoleScreen(
    isCleaning: Boolean,
    shellRunning: Boolean,
    shellOutput: String,
    scannedScripts: List<ShellScriptItem>,
    isScanningScripts: Boolean,
    isWideScreen: Boolean,
    onRunCleanup: () -> Unit,
    onRunShell: (String) -> Unit,
    onClearOutput: () -> Unit,
    onScanScripts: () -> Unit,
    onReadScriptPreview: suspend (String) -> String,
    onShowMessage: (String) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    var command by rememberSaveable { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = navBarBottom + if (isWideScreen) 32.dp else 112.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.tab_console),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp)
            )

            // 1. Background Cleanup Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.console_cleanup_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.console_cleanup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        enabled = !isCleaning,
                        onClick = {
                            HapticEngine.confirm(view)
                            onRunCleanup()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("console_cleanup_btn")
                    ) {
                        if (isCleaning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.console_cleanup_running))
                        } else {
                            Text(stringResource(R.string.console_cleanup_btn))
                        }
                    }
                }
            }

            // 2. Quick Root Shell Terminal Card (borderless tonal surface + Edge-to-Edge LazyRow chips)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.console_shell_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilledTonalButton(
                            onClick = {
                                HapticEngine.click(view)
                                showScanner = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("console_scan_scripts_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.scan_local_scripts),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Edge-to-Edge LazyRow spanning full card width with symmetric contentPadding so labels never clip
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(QuickCommandPreset.DEFAULTS, key = { it.label }) { preset ->
                            TonalAssistChip(
                                onClick = {
                                    HapticEngine.click(view)
                                    command = preset.command
                                },
                                label = {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    TonalTextField(
                        value = command,
                        onValueChange = { command = it },
                        placeholder = { Text(stringResource(R.string.console_shell_hint)) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .testTag("console_cmd_input")
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            enabled = !shellRunning && command.isNotBlank(),
                            onClick = {
                                HapticEngine.confirm(view)
                                onRunShell(command)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("console_run_btn")
                        ) {
                            if (shellRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.console_shell_running))
                            } else {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.console_shell_run))
                            }
                        }

                        if (shellOutput.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    HapticEngine.click(view)
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("flux_shell_output", shellOutput))
                                    onShowMessage(ctx.getString(R.string.copied_to_clipboard))
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.console_copy))
                            }
                            IconButton(
                                onClick = {
                                    HapticEngine.click(view)
                                    onClearOutput()
                                }
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.console_clear))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .heightIn(min = 120.dp)
                    ) {
                        val display = when {
                            shellRunning -> stringResource(R.string.console_shell_running)
                            shellOutput.isEmpty() -> stringResource(R.string.console_shell_idle)
                            else -> shellOutput
                        }
                        Text(
                            text = display,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = if (shellOutput.isEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showScanner) {
        ScriptScannerSheet(
            scripts = scannedScripts,
            isScanning = isScanningScripts,
            onRescan = onScanScripts,
            onReadPreview = onReadScriptPreview,
            onDismiss = { showScanner = false },
            onPickCommand = { picked ->
                command = picked
                showScanner = false
            }
        )
    }
}
