package com.mouajmd.fluxcore.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.system.HapticEngine

enum class TopTab(
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    PROFILES(R.string.tab_profiles, Icons.Rounded.AutoAwesomeMotion, Icons.Outlined.AutoAwesomeMotion, "nav_profiles"),
    CONSOLE(R.string.tab_console, Icons.Rounded.Terminal, Icons.Outlined.Terminal, "nav_console"),
    LOGS(R.string.tab_logs, Icons.AutoMirrored.Rounded.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "nav_logs"),
    SETTINGS(R.string.tab_settings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
}

private val ExpressiveSpringIntSize = spring<IntSize>(dampingRatio = 0.62f, stiffness = 380f)
private val ExpressiveSpringFloat = spring<Float>(dampingRatio = 0.62f, stiffness = 380f)

/**
 * Signature Lntano M3 Expressive Floating Pill Navigation Bar:
 * Unselected tabs render icon only; selected tab expands horizontally with a spring curve
 * into a secondaryContainer pill showing icon + label.
 */
@Composable
fun FloatingPillNavBar(
    currentTab: TopTab,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .animateContentSize(animationSpec = ExpressiveSpringIntSize),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TopTab.entries.forEach { tab ->
                    val selected = tab == currentTab
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        },
                        label = "pillColor"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "contentColor"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1.0f,
                        animationSpec = ExpressiveSpringFloat,
                        label = "iconScale"
                    )

                    Row(
                        modifier = Modifier
                            .testTag(tab.tag)
                            .clip(CircleShape)
                            .background(pillColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = {
                                    if (!selected) {
                                        HapticEngine.tabSwitch(view)
                                        onSelect(tab)
                                    }
                                }
                            )
                            .padding(
                                horizontal = if (selected) 18.dp else 14.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = contentColor,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = expandHorizontally(
                                animationSpec = ExpressiveSpringIntSize,
                                expandFrom = Alignment.Start
                            ) + fadeIn(),
                            exit = shrinkHorizontally(
                                animationSpec = ExpressiveSpringIntSize,
                                shrinkTowards = Alignment.Start
                            ) + fadeOut()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FluxNavigationRail(
    currentTab: TopTab,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Spacer(modifier = Modifier.weight(1f))
        TopTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationRailItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        HapticEngine.tabSwitch(view)
                        onSelect(tab)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.labelRes)
                    )
                },
                label = { Text(text = stringResource(tab.labelRes), maxLines = 1) },
                modifier = Modifier.testTag(tab.tag)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
