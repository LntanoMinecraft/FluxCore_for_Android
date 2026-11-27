package com.mouajmd.fluxcore.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.system.TileRequestOutcome
import com.mouajmd.fluxcore.ui.components.FloatingPillNavBar
import com.mouajmd.fluxcore.ui.components.FluxNavigationRail
import com.mouajmd.fluxcore.ui.components.TopTab
import com.mouajmd.fluxcore.ui.navigation.RouteAbout
import com.mouajmd.fluxcore.ui.navigation.RouteConsole
import com.mouajmd.fluxcore.ui.navigation.RouteLogs
import com.mouajmd.fluxcore.ui.navigation.RouteProfiles
import com.mouajmd.fluxcore.ui.navigation.RouteRuleEditor
import com.mouajmd.fluxcore.ui.navigation.RouteSettings
import com.mouajmd.fluxcore.ui.navigation.RouteTutorial
import com.mouajmd.fluxcore.ui.screens.AboutScreen
import com.mouajmd.fluxcore.ui.screens.ConsoleScreen
import com.mouajmd.fluxcore.ui.screens.LogScreen
import com.mouajmd.fluxcore.ui.screens.ProfilesScreen
import com.mouajmd.fluxcore.ui.screens.RuleEditorScreen
import com.mouajmd.fluxcore.ui.screens.SettingsScreen
import com.mouajmd.fluxcore.ui.screens.TutorialScreen
import com.mouajmd.fluxcore.viewmodel.FluxCoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FluxCoreApp(viewModel: FluxCoreViewModel) {
    val ctx = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingImportOverwrite by remember { mutableStateOf(false) }

    fun showSnack(msg: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val exportConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportConfigToUri(uri) { ok ->
                showSnack(
                    if (ok) ctx.getString(R.string.config_exported, uri.lastPathSegment ?: "FluxCore.json")
                    else ctx.getString(R.string.config_export_failed)
                )
            }
        }
    }

    val importConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importConfigFromUri(uri, pendingImportOverwrite) { count ->
                showSnack(
                    if (count >= 0) ctx.getString(R.string.config_imported, count)
                    else ctx.getString(R.string.config_import_failed)
                )
            }
        }
    }

    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            viewModel.exportLogsToUri(uri) { ok ->
                showSnack(
                    if (ok) ctx.getString(R.string.logs_exported, uri.lastPathSegment ?: "FluxCore_log.txt")
                    else ctx.getString(R.string.logs_export_failed)
                )
            }
        }
    }

    fun handleTileOutcome(outcome: TileRequestOutcome) {
        when (outcome) {
            TileRequestOutcome.Added -> showSnack(ctx.getString(R.string.tile_added_success))
            TileRequestOutcome.AlreadyAdded -> showSnack(ctx.getString(R.string.tile_already_added))
            TileRequestOutcome.SlotsFull -> showSnack(ctx.getString(R.string.tile_slots_full))
            is TileRequestOutcome.LegacyEnabled -> showSnack(ctx.getString(R.string.tile_legacy_guide, outcome.ruleName))
            TileRequestOutcome.Dismissed -> Unit
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTopTab: TopTab? = when {
        destination == null -> TopTab.PROFILES
        destination.hasRoute<RouteProfiles>() -> TopTab.PROFILES
        destination.hasRoute<RouteConsole>() -> TopTab.CONSOLE
        destination.hasRoute<RouteLogs>() -> TopTab.LOGS
        destination.hasRoute<RouteSettings>() -> TopTab.SETTINGS
        else -> null
    }

    fun navigateToTopTab(tab: TopTab) {
        val route: Any = when (tab) {
            TopTab.PROFILES -> RouteProfiles
            TopTab.CONSOLE -> RouteConsole
            TopTab.LOGS -> RouteLogs
            TopTab.SETTINGS -> RouteSettings
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (isWideScreen && currentTopTab != null) {
                    FluxNavigationRail(
                        currentTab = currentTopTab,
                        onSelect = ::navigateToTopTab
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteProfiles,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<RouteProfiles> {
                            val rules by viewModel.rules.collectAsStateWithLifecycle()
                            val activeRuleIds by viewModel.activeRuleIds.collectAsStateWithLifecycle()
                            val busyRuleIds by viewModel.busyRuleIds.collectAsStateWithLifecycle()
                            val systemHealth by viewModel.systemHealth.collectAsStateWithLifecycle()
                            ProfilesScreen(
                                rules = rules,
                                activeRuleIds = activeRuleIds,
                                busyRuleIds = busyRuleIds,
                                systemHealth = systemHealth,
                                isWideScreen = isWideScreen,
                                onRefreshStatus = viewModel::refreshSystemStatus,
                                onTryGrantDnd = viewModel::tryGrantDndViaRoot,
                                onStartService = viewModel::startEngineService,
                                onCreateRule = { navController.navigate(RouteRuleEditor(null)) },
                                onEditRule = { rule -> navController.navigate(RouteRuleEditor(rule.id)) },
                                onToggleEnabled = viewModel::toggleRuleEnabled,
                                onToggleActive = viewModel::toggleRuleActive,
                                onDeleteRule = viewModel::deleteRule,
                                onAddQsTile = { rule -> viewModel.requestAddQsTile(rule, ::handleTileOutcome) },
                                onPinShortcut = { rule, label ->
                                    val ok = viewModel.requestPinShortcut(rule, label)
                                    showSnack(
                                        ctx.getString(
                                            if (ok) R.string.shortcut_requested else R.string.shortcut_unsupported
                                        )
                                    )
                                }
                            )
                        }

                        composable<RouteConsole> {
                            val isCleaning by viewModel.isCleaning.collectAsStateWithLifecycle()
                            val shellRunning by viewModel.shellRunning.collectAsStateWithLifecycle()
                            val shellOutput by viewModel.shellOutput.collectAsStateWithLifecycle()
                            val scannedScripts by viewModel.scannedScripts.collectAsStateWithLifecycle()
                            val isScanningScripts by viewModel.isScanningScripts.collectAsStateWithLifecycle()
                            ConsoleScreen(
                                isCleaning = isCleaning,
                                shellRunning = shellRunning,
                                shellOutput = shellOutput,
                                scannedScripts = scannedScripts,
                                isScanningScripts = isScanningScripts,
                                isWideScreen = isWideScreen,
                                onRunCleanup = {
                                    viewModel.runDeepCleanup {
                                        showSnack(ctx.getString(R.string.console_cleanup_done))
                                    }
                                },
                                onRunShell = viewModel::runQuickShell,
                                onClearOutput = viewModel::clearShellOutput,
                                onScanScripts = viewModel::scanLocalScripts,
                                onReadScriptPreview = viewModel::readScriptPreview,
                                onShowMessage = ::showSnack
                            )
                        }

                        composable<RouteLogs> {
                            val logs by viewModel.logs.collectAsStateWithLifecycle()
                            LogScreen(
                                logs = logs,
                                isWideScreen = isWideScreen,
                                onExportLogs = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    runCatching {
                                        exportLogLauncher.launch("FluxCore_log_$stamp.txt")
                                    }.onFailure {
                                        viewModel.exportLogsToDownload { path ->
                                            showSnack(
                                                if (path != null) ctx.getString(R.string.logs_exported, path)
                                                else ctx.getString(R.string.logs_export_failed)
                                            )
                                        }
                                    }
                                },
                                onClearLogs = viewModel::clearLogs,
                                onShowMessage = ::showSnack
                            )
                        }

                        composable<RouteSettings> {
                            val settings by viewModel.settings.collectAsStateWithLifecycle()
                            val localBackupFiles by viewModel.localBackupFiles.collectAsStateWithLifecycle()
                            SettingsScreen(
                                settings = settings,
                                localBackupFiles = localBackupFiles,
                                isWideScreen = isWideScreen,
                                onRefreshLocalBackups = viewModel::refreshLocalBackupFiles,
                                onToggleAdvanced = viewModel::setAdvancedMode,
                                onToggleHaptic = viewModel::setHapticEnabled,
                                onToggleHideNotification = { hide ->
                                    viewModel.setHidePersistentNotification(hide) {
                                        showSnack(
                                            ctx.getString(
                                                if (hide) R.string.settings_hide_notif_hidden_ok
                                                else R.string.settings_hide_notif_shown_ok
                                            )
                                        )
                                    }
                                },
                                onSelectThemeMode = viewModel::setThemeMode,
                                onSelectLanguage = { mode ->
                                    viewModel.setLanguageMode(mode)
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        (ctx as? Activity)?.recreate()
                                    }
                                },
                                onToggleDynamicColor = viewModel::setDynamicColor,
                                onSelectPalette = viewModel::setPaletteIndex,
                                onOpenTutorial = { navController.navigate(RouteTutorial) },
                                onOpenAbout = { navController.navigate(RouteAbout) },
                                onExportConfig = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    runCatching {
                                        exportConfigLauncher.launch("FluxCore_config_$stamp.json")
                                    }.onFailure {
                                        viewModel.exportConfigToDownload { path ->
                                            showSnack(
                                                if (path != null) ctx.getString(R.string.config_exported, path)
                                                else ctx.getString(R.string.config_export_failed)
                                            )
                                        }
                                    }
                                },
                                onImportViaSaf = { overwrite ->
                                    pendingImportOverwrite = overwrite
                                    importConfigLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                onImportLocalFile = { file, overwrite ->
                                    viewModel.importConfigFromFile(file, overwrite) { count ->
                                        showSnack(
                                            if (count >= 0) ctx.getString(R.string.config_imported, count)
                                            else ctx.getString(R.string.config_import_failed)
                                        )
                                    }
                                }
                            )
                        }

                        composable<RouteRuleEditor> { entry ->
                            val route = entry.toRoute<RouteRuleEditor>()
                            val rules by viewModel.rules.collectAsStateWithLifecycle()
                            val settings by viewModel.settings.collectAsStateWithLifecycle()
                            val scannedScripts by viewModel.scannedScripts.collectAsStateWithLifecycle()
                            val isScanningScripts by viewModel.isScanningScripts.collectAsStateWithLifecycle()
                            val existing = remember(route.ruleId, rules) {
                                route.ruleId?.let { id -> rules.firstOrNull { it.id == id } }
                            }
                            RuleEditorScreen(
                                existingRule = existing,
                                advancedMode = settings.advancedMode,
                                scannedScripts = scannedScripts,
                                isScanningScripts = isScanningScripts,
                                onScanScripts = viewModel::scanLocalScripts,
                                onReadScriptPreview = viewModel::readScriptPreview,
                                onDetectWifi = viewModel::detectCurrentWifi,
                                onDetectBt = viewModel::detectConnectedBluetooth,
                                onSave = viewModel::saveRule,
                                onAddQsTile = { rule -> viewModel.requestAddQsTile(rule, ::handleTileOutcome) },
                                onPinShortcut = { rule, label ->
                                    val ok = viewModel.requestPinShortcut(rule, label)
                                    showSnack(
                                        ctx.getString(
                                            if (ok) R.string.shortcut_requested else R.string.shortcut_unsupported
                                        )
                                    )
                                },
                                onBack = { navController.popBackStack() },
                                onShowMessage = ::showSnack
                            )
                        }

                        composable<RouteTutorial> {
                            TutorialScreen(onBack = { navController.popBackStack() })
                        }

                        composable<RouteAbout> {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                    }

                    // Snackbar safely above Floating Pill Nav Bar
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(
                                bottom = if (!isWideScreen && currentTopTab != null) 90.dp else 16.dp,
                                start = 16.dp,
                                end = 16.dp
                            )
                    )

                    // Signature Floating Pill Navigation Bar (hidden on secondary screens & wide screens)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isWideScreen && currentTopTab != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        FloatingPillNavBar(
                            currentTab = currentTopTab ?: TopTab.PROFILES,
                            onSelect = ::navigateToTopTab
                        )
                    }
                }
            }
        }
    }
}
