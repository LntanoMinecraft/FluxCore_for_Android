package com.mouajmd.fluxcore.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mouajmd.fluxcore.BuildConfig
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.AppSettings
import com.mouajmd.fluxcore.data.LanguageMode
import com.mouajmd.fluxcore.data.ThemeMode
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.components.TonalIconBadge
import com.mouajmd.fluxcore.ui.components.TonalSectionHeader
import com.mouajmd.fluxcore.ui.components.TonalSegmentedControl
import com.mouajmd.fluxcore.ui.components.TransparentDialogSystemBars
import com.mouajmd.fluxcore.ui.theme.FluxPalettes
import com.mouajmd.fluxcore.ui.theme.ThemePaletteSpec
import java.io.File

@Composable
fun SettingsScreen(
    settings: AppSettings,
    localBackupFiles: List<File>,
    isWideScreen: Boolean,
    onRefreshLocalBackups: () -> Unit,
    onToggleAdvanced: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectLanguage: (LanguageMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectPalette: (Int) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenAbout: () -> Unit,
    onExportConfig: () -> Unit,
    onImportViaSaf: (overwrite: Boolean) -> Unit,
    onImportLocalFile: (file: File, overwrite: Boolean) -> Unit
) {
    val view = LocalView.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importOverwrite by remember { mutableStateOf(false) }
    var showLocalFilesDialog by remember { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.tab_settings),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp)
            )

            // Group 1: Core & Feedback
            TonalSectionHeader(stringResource(R.string.settings_group_core))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.settings_advanced_title),
                        subtitle = stringResource(R.string.settings_advanced_desc),
                        checked = settings.advancedMode,
                        testTag = "switch_advanced_mode",
                        onCheckedChange = {
                            HapticEngine.toggle(view, it)
                            onToggleAdvanced(it)
                        }
                    )
                    SettingsSwitchRow(
                        icon = Icons.Rounded.Vibration,
                        title = stringResource(R.string.settings_haptic_title),
                        subtitle = stringResource(R.string.settings_haptic_desc),
                        checked = settings.hapticEnabled,
                        testTag = "switch_haptic_engine",
                        onCheckedChange = { enabled ->
                            onToggleHaptic(enabled)
                            if (enabled) HapticEngine.toggle(view, true)
                        }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Group 2: Appearance — 4 flat borderless rounded cards (24.dp, surfaceContainer, 12.dp spacing)
            TonalSectionHeader(stringResource(R.string.settings_group_appearance))

            // Card 1: Theme Mode
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TonalSegmentedControl(
                        options = listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_mode_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
                            ThemeMode.DARK to stringResource(R.string.theme_mode_dark)
                        ),
                        selected = settings.themeMode,
                        onSelect = onSelectThemeMode,
                        tagPrefix = "theme_mode"
                    )
                }
            }

            // Card 2: Language
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TonalSegmentedControl(
                        options = listOf(
                            LanguageMode.SYSTEM to stringResource(R.string.lang_system),
                            LanguageMode.ZH to stringResource(R.string.lang_zh),
                            LanguageMode.EN to stringResource(R.string.lang_en)
                        ),
                        selected = settings.languageMode,
                        onSelect = onSelectLanguage,
                        tagPrefix = "lang"
                    )
                }
            }

            // Card 3: Dynamic Color
            val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.palette_dynamic_title),
                    subtitle = stringResource(
                        if (supportsDynamic) R.string.palette_dynamic_desc else R.string.palette_dynamic_unsupported
                    ),
                    checked = supportsDynamic && settings.dynamicColor,
                    enabled = supportsDynamic,
                    testTag = "switch_dynamic_color",
                    onCheckedChange = { enabled ->
                        if (supportsDynamic) {
                            HapticEngine.toggle(view, enabled)
                            onToggleDynamicColor(enabled)
                        }
                    }
                )
            }

            // Card 4: Theme Color Palette (single row of 7 dual-layer concentric circles, no bottom text labels)
            val activePalette = FluxPalettes.get(settings.paletteIndex)
            val isDynamicActive = supportsDynamic && settings.dynamicColor
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.settings_color_palette),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isDynamicActive) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                        ) {
                            Text(
                                text = if (isDynamicActive) {
                                    stringResource(R.string.palette_dynamic_label)
                                } else {
                                    stringResource(activePalette.nameRes)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isDynamicActive) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FluxPalettes.all.forEach { spec ->
                            val selected = !isDynamicActive && settings.paletteIndex == spec.index
                            ConcentricColorSwatch(
                                spec = spec,
                                selected = selected,
                                onClick = {
                                    HapticEngine.confirm(view)
                                    onSelectPalette(spec.index)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Group 3: Data & Help
            TonalSectionHeader(stringResource(R.string.settings_group_data))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsNavRow(
                        icon = Icons.Rounded.FileUpload,
                        title = stringResource(R.string.settings_export_config),
                        subtitle = stringResource(R.string.settings_export_config_desc),
                        testTag = "settings_export_config",
                        onClick = {
                            HapticEngine.confirm(view)
                            onExportConfig()
                        }
                    )
                    SettingsNavRow(
                        icon = Icons.Rounded.FileDownload,
                        title = stringResource(R.string.settings_import_config),
                        subtitle = stringResource(R.string.settings_import_config_desc),
                        testTag = "settings_import_config",
                        onClick = {
                            HapticEngine.click(view)
                            importOverwrite = false
                            showImportDialog = true
                        }
                    )
                    SettingsNavRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = stringResource(R.string.settings_tutorial),
                        subtitle = stringResource(R.string.settings_tutorial_desc),
                        testTag = "nav_tutorial",
                        onClick = {
                            HapticEngine.click(view)
                            onOpenTutorial()
                        }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Group 4: About (pinned at bottom of Settings)
            TonalSectionHeader(stringResource(R.string.settings_group_about))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsNavRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_about_title),
                    subtitle = stringResource(R.string.settings_about_desc),
                    trailingText = "v${BuildConfig.VERSION_NAME}",
                    testTag = "nav_about",
                    onClick = {
                        HapticEngine.click(view)
                        onOpenAbout()
                    }
                )
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            shape = RoundedCornerShape(26.dp),
            title = {
                TransparentDialogSystemBars()
                Text(stringResource(R.string.config_import_mode_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                if (importOverwrite) R.string.config_import_overwrite else R.string.config_import_merge
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = importOverwrite,
                            onCheckedChange = {
                                HapticEngine.toggle(view, it)
                                importOverwrite = it
                            }
                        )
                    }
                    Button(
                        onClick = {
                            HapticEngine.click(view)
                            showImportDialog = false
                            onImportViaSaf(importOverwrite)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.config_import_pick_saf))
                    }
                    FilledTonalButton(
                        onClick = {
                            HapticEngine.click(view)
                            onRefreshLocalBackups()
                            showImportDialog = false
                            showLocalFilesDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.config_import_scan_dir))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showLocalFilesDialog) {
        AlertDialog(
            onDismissRequest = { showLocalFilesDialog = false },
            shape = RoundedCornerShape(26.dp),
            title = {
                TransparentDialogSystemBars()
                Text(stringResource(R.string.settings_import_config))
            },
            text = {
                if (localBackupFiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.config_no_local_files),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        localBackupFiles.forEach { f ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        HapticEngine.confirm(view)
                                        showLocalFilesDialog = false
                                        onImportLocalFile(f, importOverwrite)
                                    }
                            ) {
                                Text(
                                    text = f.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLocalFilesDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

/**
 * Dual-layer concentric circular color swatch:
 * deep saturated outer disc + luminous same-hue inner disc, with outer selection ring and spring checkmark.
 */
@Composable
private fun ConcentricColorSwatch(
    spec: ThemePaletteSpec,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(spec.nameRes)
    val ringColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.RadioButton,
                onClick = onClick
            )
            .testTag("palette_swatch_${spec.index}"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val radius = size.minDimension / 2f
            if (selected) {
                drawCircle(
                    color = ringColor,
                    radius = radius - 1.5.dp.toPx(),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            }
            val outerDiscRadius = if (selected) radius - 5.dp.toPx() else radius - 2.5.dp.toPx()
            drawCircle(
                color = spec.swatchColor,
                radius = outerDiscRadius
            )
            val innerDiscRadius = outerDiscRadius * 0.56f
            drawCircle(
                color = spec.innerDotColor,
                radius = innerDiscRadius
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f)),
            exit = scaleOut()
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TonalIconBadge(icon = icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TonalIconBadge(icon = icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingText != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
