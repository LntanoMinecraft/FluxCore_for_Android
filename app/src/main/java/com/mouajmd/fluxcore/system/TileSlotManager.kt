package com.mouajmd.fluxcore.system

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.TileService
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.data.RuleStore
import com.mouajmd.fluxcore.model.AutomationRule
import com.mouajmd.fluxcore.service.FluxCoreTileSlot1Service
import com.mouajmd.fluxcore.service.FluxCoreTileSlot2Service
import com.mouajmd.fluxcore.service.FluxCoreTileSlot3Service
import com.mouajmd.fluxcore.service.FluxCoreTileSlot4Service
import com.mouajmd.fluxcore.service.FluxCoreTileSlot5Service
import com.mouajmd.fluxcore.service.FluxCoreTileSlot6Service

sealed class TileRequestOutcome {
    data object Added : TileRequestOutcome()
    data object AlreadyAdded : TileRequestOutcome()
    data object SlotsFull : TileRequestOutcome()
    data class LegacyEnabled(val ruleName: String) : TileRequestOutcome()
    data object Dismissed : TileRequestOutcome()
}

object TileSlotManager {
    private const val PREF_TILES = "fluxcore_qs_slots_v1"
    private const val SLOT_COUNT = 6

    private val slotClasses = listOf(
        FluxCoreTileSlot1Service::class.java,
        FluxCoreTileSlot2Service::class.java,
        FluxCoreTileSlot3Service::class.java,
        FluxCoreTileSlot4Service::class.java,
        FluxCoreTileSlot5Service::class.java,
        FluxCoreTileSlot6Service::class.java
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_TILES, Context.MODE_PRIVATE)

    fun getRuleIdForSlot(context: Context, slotIndex: Int): String? =
        prefs(context).getString("slot_$slotIndex", null)

    fun findSlotForRule(context: Context, ruleId: String): Int? {
        val sp = prefs(context)
        for (i in 1..SLOT_COUNT) {
            if (sp.getString("slot_$i", null) == ruleId) return i
        }
        return null
    }

    fun bindAndRequestTile(
        context: Context,
        rule: AutomationRule,
        onResult: (TileRequestOutcome) -> Unit
    ) {
        val appCtx = context.applicationContext
        val sp = prefs(appCtx)

        var slot = findSlotForRule(appCtx, rule.id)
        if (slot == null) {
            for (i in 1..SLOT_COUNT) {
                val existingId = sp.getString("slot_$i", null)
                if (existingId == null || RuleStore.findById(appCtx, existingId) == null) {
                    slot = i
                    break
                }
            }
        }

        if (slot == null) {
            onResult(TileRequestOutcome.SlotsFull)
            return
        }

        sp.edit().putString("slot_$slot", rule.id).apply()
        val comp = ComponentName(appCtx, slotClasses[slot - 1])
        setComponentEnabled(appCtx, comp, true)
        runCatching { TileService.requestListeningState(appCtx, comp) }
        AppLogger.log("TILE", "Bound QS Tile slot #$slot -> [${rule.name}]")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val sbm = appCtx.getSystemService(StatusBarManager::class.java)
            if (sbm != null) {
                sbm.requestAddTileService(
                    comp,
                    rule.name.ifBlank { appCtx.getString(R.string.app_name) },
                    Icon.createWithResource(appCtx, R.drawable.ic_qs_flux),
                    appCtx.mainExecutor
                ) { resultCode ->
                    when (resultCode) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                            onResult(TileRequestOutcome.Added)
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                            onResult(TileRequestOutcome.AlreadyAdded)
                        else ->
                            onResult(TileRequestOutcome.Dismissed)
                    }
                }
                return
            }
        }

        onResult(TileRequestOutcome.LegacyEnabled(rule.name.ifBlank { appCtx.getString(R.string.app_name) }))
    }

    fun releaseRuleSlot(context: Context, ruleId: String) {
        val appCtx = context.applicationContext
        val slot = findSlotForRule(appCtx, ruleId) ?: return
        prefs(appCtx).edit().remove("slot_$slot").apply()
        val comp = ComponentName(appCtx, slotClasses[slot - 1])
        setComponentEnabled(appCtx, comp, false)
        AppLogger.log("TILE", "Released QS Tile slot #$slot")
    }

    fun syncAllTiles(context: Context) {
        val appCtx = context.applicationContext
        val sp = prefs(appCtx)
        for (i in 1..SLOT_COUNT) {
            val boundId = sp.getString("slot_$i", null) ?: continue
            val comp = ComponentName(appCtx, slotClasses[i - 1])
            if (RuleStore.findById(appCtx, boundId) == null) {
                sp.edit().remove("slot_$i").apply()
                setComponentEnabled(appCtx, comp, false)
            } else {
                runCatching { TileService.requestListeningState(appCtx, comp) }
            }
        }
    }

    private fun setComponentEnabled(context: Context, component: ComponentName, enabled: Boolean) {
        runCatching {
            val state = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            context.packageManager.setComponentEnabledSetting(
                component,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
