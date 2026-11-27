package com.mouajmd.fluxcore.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mouajmd.fluxcore.R
import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.data.RuleStore
import com.mouajmd.fluxcore.engine.ActionExecutor
import com.mouajmd.fluxcore.system.TileSlotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class FluxCoreBaseTileService(private val slotIndex: Int) : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val ruleId = TileSlotManager.getRuleIdForSlot(this, slotIndex) ?: return
        val rule = RuleStore.findById(this, ruleId)
        if (rule == null) {
            TileSlotManager.releaseRuleSlot(this, ruleId)
            refreshTile()
            return
        }
        AppLogger.log("TILE", "QS Tile #$slotIndex tapped -> [${rule.name}]")
        val appCtx = applicationContext
        tileScope.launch {
            ActionExecutor.toggle(appCtx, rule)
            withContext(Dispatchers.Main) {
                refreshTile()
            }
        }
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val ruleId = TileSlotManager.getRuleIdForSlot(this, slotIndex)
        val rule = ruleId?.let { RuleStore.findById(this, it) }
        if (rule == null) {
            tile.label = getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(R.string.rule_idle_badge)
            }
            tile.state = Tile.STATE_UNAVAILABLE
        } else {
            val active = ActionExecutor.isActive(rule.id)
            tile.label = rule.name.ifBlank { getString(R.string.app_name) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = getString(if (active) R.string.rule_active_badge else R.string.rule_idle_badge)
            }
            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
}

class FluxCoreTileSlot1Service : FluxCoreBaseTileService(1)
class FluxCoreTileSlot2Service : FluxCoreBaseTileService(2)
class FluxCoreTileSlot3Service : FluxCoreBaseTileService(3)
class FluxCoreTileSlot4Service : FluxCoreBaseTileService(4)
class FluxCoreTileSlot5Service : FluxCoreBaseTileService(5)
class FluxCoreTileSlot6Service : FluxCoreBaseTileService(6)
