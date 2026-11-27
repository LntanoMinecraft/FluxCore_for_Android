package com.mouajmd.fluxcore.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mouajmd.fluxcore.BuildConfig
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.system.HapticEngine
import com.mouajmd.fluxcore.ui.components.TonalIconBadge
import com.mouajmd.fluxcore.ui.components.TonalSectionHeader

private data class TutorialCardSpec(
    val stepCode: String,
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    val pathResList: List<Int>,
    val bulletResList: List<Int>,
    val showAppInfoAction: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current

    val quickStartSteps = remember {
        listOf(
            TutorialCardSpec(
                stepCode = "01",
                icon = Icons.Rounded.AdminPanelSettings,
                titleRes = R.string.tut_1_title,
                pathResList = listOf(R.string.tut_1_path),
                bulletResList = listOf(R.string.tut_1_p1, R.string.tut_1_p2)
            ),
            TutorialCardSpec(
                stepCode = "02",
                icon = Icons.Rounded.BatteryChargingFull,
                titleRes = R.string.tut_2_title,
                pathResList = listOf(R.string.tut_2_path_1, R.string.tut_2_path_2),
                bulletResList = listOf(R.string.tut_2_p1, R.string.tut_2_p2),
                showAppInfoAction = true
            )
        )
    }

    val advancedSteps = remember {
        listOf(
            TutorialCardSpec(
                stepCode = "03",
                icon = Icons.Rounded.Tune,
                titleRes = R.string.tut_3_title,
                pathResList = listOf(R.string.tut_3_path),
                bulletResList = listOf(R.string.tut_3_p1, R.string.tut_3_p2, R.string.tut_3_p3)
            ),
            TutorialCardSpec(
                stepCode = "04",
                icon = Icons.Rounded.DashboardCustomize,
                titleRes = R.string.tut_4_title,
                pathResList = listOf(R.string.tut_4_path),
                bulletResList = listOf(R.string.tut_4_p1, R.string.tut_4_p2)
            ),
            TutorialCardSpec(
                stepCode = "05",
                icon = Icons.Rounded.Terminal,
                titleRes = R.string.tut_5_title,
                pathResList = listOf(R.string.tut_5_path),
                bulletResList = listOf(R.string.tut_5_p1, R.string.tut_5_p2)
            ),
            TutorialCardSpec(
                stepCode = "06",
                icon = Icons.Rounded.Backup,
                titleRes = R.string.tut_6_title,
                pathResList = listOf(R.string.tut_6_path),
                bulletResList = listOf(R.string.tut_6_p1, R.string.tut_6_p2)
            )
        )
    }

    fun openAppInfo() {
        runCatching {
            ctx.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${ctx.packageName}")
                )
            )
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tutorial_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticEngine.click(view)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = navBarBottom + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "section_quickstart") {
                    TonalSectionHeader(stringResource(R.string.tut_section_quickstart))
                }

                items(quickStartSteps, key = { it.stepCode }) { spec ->
                    TutorialStepCard(
                        spec = spec,
                        onOpenAppInfo = {
                            HapticEngine.click(view)
                            openAppInfo()
                        }
                    )
                }

                item(key = "section_advanced") {
                    Column {
                        Spacer(Modifier.height(6.dp))
                        TonalSectionHeader(stringResource(R.string.tut_section_advanced))
                    }
                }

                items(advancedSteps, key = { it.stepCode }) { spec ->
                    TutorialStepCard(
                        spec = spec,
                        onOpenAppInfo = {}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorialStepCard(
    spec: TutorialCardSpec,
    onOpenAppInfo: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(19.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header: 40dp primaryContainer rounded-square icon + tnum step pill + Bold title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TonalIconBadge(
                    icon = spec.icon,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = spec.stepCode,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(spec.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Highlighted operation path tags
            if (spec.pathResList.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spec.pathResList.forEach { pathRes ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = stringResource(pathRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Crisp bullet points (14.sp, lineHeight = 21.sp, high contrast)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                spec.bulletResList.forEach { bulletRes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(bulletRes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (spec.showAppInfoAction) {
                FilledTonalButton(
                    onClick = onOpenAppInfo,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("tut_open_app_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SettingsApplications,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tut_btn_app_info),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private data class AboutLinkItem(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subRes: Int,
    val url: String,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current

    val links = remember {
        listOf(
            AboutLinkItem(
                icon = Icons.Rounded.Code,
                titleRes = R.string.about_link_repo,
                subRes = R.string.about_link_repo_sub,
                url = "https://github.com/LntanoMinecraft/FluxCore_for_Android",
                tag = "about_link_repo"
            ),
            AboutLinkItem(
                icon = Icons.Rounded.Person,
                titleRes = R.string.about_link_maintainer_coolapk,
                subRes = R.string.about_link_maintainer_coolapk_sub,
                url = "https://www.coolapk.com/u/34054336",
                tag = "about_link_maintainer_coolapk"
            ),
            AboutLinkItem(
                icon = Icons.Rounded.Code,
                titleRes = R.string.about_link_upstream,
                subRes = R.string.about_link_upstream_sub,
                url = "https://github.com/MOUAJMD/FluxCore_for_Android",
                tag = "about_link_upstream"
            ),
            AboutLinkItem(
                icon = Icons.Rounded.Person,
                titleRes = R.string.about_link_author_coolapk,
                subRes = R.string.about_link_author_coolapk_sub,
                url = "https://www.coolapk.com/u/39291006",
                tag = "about_link_author_coolapk"
            )
        )
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_about_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticEngine.click(view)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .align(Alignment.TopCenter)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = navBarBottom + 24.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Upper/Middle Centered Hero
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        shadowElevation = 4.dp,
                        color = Color.White,
                        modifier = Modifier.size(92.dp)
                    ) {
                        AndroidView(
                            factory = { c ->
                                ImageView(c).apply {
                                    setImageResource(R.mipmap.ic_launcher)
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.about_license_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Pinned Links Card (4 links: Maintainer + Upstream Original Author)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        links.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticEngine.click(view)
                                        runCatching {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag(item.tag),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TonalIconBadge(icon = item.icon)
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(item.titleRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(item.subRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
