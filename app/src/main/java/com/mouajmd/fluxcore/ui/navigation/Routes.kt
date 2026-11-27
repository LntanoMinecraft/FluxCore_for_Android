package com.mouajmd.fluxcore.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object RouteProfiles

@Serializable
object RouteConsole

@Serializable
object RouteLogs

@Serializable
object RouteSettings

@Serializable
data class RouteRuleEditor(val ruleId: String? = null)

@Serializable
object RouteTutorial

@Serializable
object RouteAbout
