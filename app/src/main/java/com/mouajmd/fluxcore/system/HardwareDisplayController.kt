package com.mouajmd.fluxcore.system

import com.mouajmd.fluxcore.data.AppLogger
import com.mouajmd.fluxcore.model.ActionStep
import java.util.Locale

data class BrightnessBaseline(
    val mode: String?,
    val legacyValue: String?,
    val floatValue: String?
)

data class HbmProbeResult(
    val hbmNodePath: String? = null,
    val hbmOriginalVal: String? = null,
    val hbmWrittenVal: String? = null,
    val backlightNodePath: String? = null,
    val backlightOriginalVal: String? = null,
    val backlightMaxVal: String? = null,
    val backlightWritten: Boolean = false,
    val rejectedNodes: List<String> = emptyList()
)

/**
 * Hardware display controller for standard percentage brightness (0%–100%)
 * and adaptive Root HBM peak-brightness sysfs probing & safety reversion.
 */
object HardwareDisplayController {

    private val validSysPathRegex = Regex("^/sys/[a-zA-Z0-9._/@:+-]+(?:/[a-zA-Z0-9._/@:+-]+)*$")

    fun captureBaseline(): BrightnessBaseline = BrightnessBaseline(
        mode = RootShell.exec("settings get system screen_brightness_mode"),
        legacyValue = RootShell.exec("settings get system screen_brightness"),
        floatValue = RootShell.exec("settings get system screen_brightness_float")
    )

    fun applyPercentBrightness(ruleName: String, stepNumber: Int, percent: Int) {
        val pct = percent.coerceIn(0, 100)
        val legacy255 = ActionStep.percentToLegacy255(pct)
        val floatStr = String.format(Locale.US, "%.4f", ActionStep.percentToFloat(pct))
        val cmd = buildString {
            appendLine("settings put system screen_brightness_mode 0")
            appendLine("settings put system screen_brightness $legacy255")
            append("settings put system screen_brightness_float $floatStr")
        }
        val result = RootShell.execDetailed(cmd)
        if (result.isSuccess) {
            AppLogger.log(
                "BRIGHT",
                "[$ruleName #$stepNumber] Brightness -> $pct% (legacy=$legacy255, float=$floatStr)"
            )
        } else {
            AppLogger.error(
                "BRIGHT",
                "[$ruleName #$stepNumber] Brightness failed (exit=${result.exitCode}): ${result.output}"
            )
        }
    }

    fun activateHbmPeakBrightness(ruleName: String, stepNumber: Int): HbmProbeResult {
        val probeScript = """
            settings put system screen_brightness_mode 0
            settings put system screen_brightness 255
            settings put system screen_brightness_float 1.0
            HBM_HIT=""
            for p in \
              /sys/class/backlight/panel0-backlight/hbm_mode \
              /sys/class/backlight/panel0-backlight/hbm \
              /sys/class/drm/card0-DSI-1/hbm \
              /sys/class/drm/card0-DSI-1/hbm_mode \
              /sys/class/oplus_display/hbm \
              /sys/kernel/oplus_display/hbm \
              /sys/devices/platform/soc/ae00000.qcom,mdss_mdp/backlight/panel0-backlight/hbm_mode \
              /sys/class/lcd/panel/hbm \
              /sys/class/backlight/*/hbm_mode \
              /sys/class/backlight/*/hbm \
              /sys/class/drm/*/hbm \
              /sys/class/drm/*/hbm_mode; do
              if [ -e "${'$'}p" ] && [ -w "${'$'}p" ]; then
                ORIG=${'$'}(cat "${'$'}p" 2>/dev/null | head -n 1 | tr -d '\r\n')
                if echo 1 > "${'$'}p" 2>/dev/null; then
                  echo "HBM_NODE=${'$'}p"
                  echo "HBM_ORIG=${'$'}ORIG"
                  echo "HBM_WRITTEN=1"
                  HBM_HIT="${'$'}p"
                  break
                elif echo 2 > "${'$'}p" 2>/dev/null; then
                  echo "HBM_NODE=${'$'}p"
                  echo "HBM_ORIG=${'$'}ORIG"
                  echo "HBM_WRITTEN=2"
                  HBM_HIT="${'$'}p"
                  break
                elif echo on > "${'$'}p" 2>/dev/null; then
                  echo "HBM_NODE=${'$'}p"
                  echo "HBM_ORIG=${'$'}ORIG"
                  echo "HBM_WRITTEN=on"
                  HBM_HIT="${'$'}p"
                  break
                else
                  echo "HBM_REJECTED=${'$'}p"
                fi
              fi
            done
            if [ -z "${'$'}HBM_HIT" ]; then
              echo "HBM_NODE=NONE"
            fi
            BL_DIR=""
            for d in /sys/class/backlight/panel0-backlight /sys/class/backlight/*; do
              if [ -d "${'$'}d" ] && [ -r "${'$'}d/max_brightness" ] && [ -w "${'$'}d/brightness" ]; then
                BL_DIR="${'$'}d"
                break
              fi
            done
            if [ -n "${'$'}BL_DIR" ]; then
              BL_MAX=${'$'}(cat "${'$'}BL_DIR/max_brightness" 2>/dev/null | head -n 1 | tr -d '\r\n')
              BL_ORIG=${'$'}(cat "${'$'}BL_DIR/brightness" 2>/dev/null | head -n 1 | tr -d '\r\n')
              echo "BL_NODE=${'$'}BL_DIR/brightness"
              echo "BL_ORIG=${'$'}BL_ORIG"
              echo "BL_MAX=${'$'}BL_MAX"
              if [ -n "${'$'}BL_MAX" ] && echo "${'$'}BL_MAX" > "${'$'}BL_DIR/brightness" 2>/dev/null; then
                echo "BL_WRITE=OK"
              else
                echo "BL_WRITE=FAIL"
              fi
            else
              echo "BL_NODE=NONE"
            fi
        """.trimIndent()

        val result = RootShell.execDetailed(probeScript, timeoutMs = 10_000L)
        var hbmNode: String? = null
        var hbmOrig: String? = null
        var hbmWritten: String? = null
        var blNode: String? = null
        var blOrig: String? = null
        var blMax: String? = null
        var blWriteOk = false
        val rejectedNodes = mutableListOf<String>()

        result.output.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("HBM_NODE=") -> {
                    val v = line.removePrefix("HBM_NODE=").trim()
                    if (isSafeSysfsPath(v)) hbmNode = v
                }
                line.startsWith("HBM_ORIG=") -> hbmOrig = line.removePrefix("HBM_ORIG=").trim()
                line.startsWith("HBM_WRITTEN=") -> hbmWritten = line.removePrefix("HBM_WRITTEN=").trim()
                line.startsWith("HBM_REJECTED=") -> rejectedNodes.add(line.removePrefix("HBM_REJECTED=").trim())
                line.startsWith("BL_NODE=") -> {
                    val v = line.removePrefix("BL_NODE=").trim()
                    if (isSafeSysfsPath(v)) blNode = v
                }
                line.startsWith("BL_ORIG=") -> blOrig = line.removePrefix("BL_ORIG=").trim()
                line.startsWith("BL_MAX=") -> blMax = line.removePrefix("BL_MAX=").trim()
                line == "BL_WRITE=OK" -> blWriteOk = true
            }
        }

        val probe = HbmProbeResult(
            hbmNodePath = hbmNode,
            hbmOriginalVal = hbmOrig,
            hbmWrittenVal = hbmWritten,
            backlightNodePath = if (blWriteOk) blNode else null,
            backlightOriginalVal = if (blWriteOk) blOrig else null,
            backlightMaxVal = blMax,
            backlightWritten = blWriteOk,
            rejectedNodes = rejectedNodes
        )

        if (hbmNode != null) {
            val blSummary = if (blNode != null && blWriteOk) "$blNode=$blMax" else "float=1.0"
            AppLogger.log(
                "HBM",
                "[$ruleName #$stepNumber] HBM peak ON -> node=$hbmNode (orig=${hbmOrig.orEmpty()} -> $hbmWritten), backlight=$blSummary"
            )
        } else {
            val rejectedInfo = if (rejectedNodes.isNotEmpty()) " (rejected: ${rejectedNodes.joinToString()})" else ""
            val blSummary = if (blNode != null && blWriteOk) "$blNode=$blMax" else "screen_brightness_float=1.0 (255)"
            AppLogger.warn(
                "HBM",
                "[$ruleName #$stepNumber] No writable HBM sysfs node$rejectedInfo; safe fallback -> $blSummary"
            )
        }
        return probe
    }

    fun appendRevertCommands(
        sb: StringBuilder,
        baseline: BrightnessBaseline?,
        hbmProbe: HbmProbeResult?
    ) {
        val hbmPath = hbmProbe?.hbmNodePath
        if (hbmPath != null && isSafeSysfsPath(hbmPath)) {
            val orig = hbmProbe.hbmOriginalVal?.trim()
            val safeOff: String = if (
                orig != null &&
                (orig == "0" || orig.equals("off", ignoreCase = true) || orig.equals("false", ignoreCase = true))
            ) {
                orig
            } else {
                "0"
            }
            sb.append("echo ").append(RootShell.shellQuote(safeOff))
                .append(" > ").append(RootShell.shellQuote(hbmPath))
                .append(" 2>/dev/null || true\n")
        }

        val blPath = hbmProbe?.backlightNodePath
        val blOrig = hbmProbe?.backlightOriginalVal?.trim()
        if (blPath != null && isSafeSysfsPath(blPath) && isValidSetting(blOrig)) {
            sb.append("echo ").append(RootShell.shellQuote(blOrig!!))
                .append(" > ").append(RootShell.shellQuote(blPath))
                .append(" 2>/dev/null || true\n")
        }

        if (baseline != null) {
            if (isValidSetting(baseline.legacyValue)) {
                sb.append("settings put system screen_brightness ").append(baseline.legacyValue!!.trim()).append('\n')
            }
            if (isValidSetting(baseline.floatValue)) {
                sb.append("settings put system screen_brightness_float ").append(baseline.floatValue!!.trim()).append('\n')
            }
            if (isValidSetting(baseline.mode)) {
                sb.append("settings put system screen_brightness_mode ").append(baseline.mode!!.trim()).append('\n')
            }
        }
    }

    fun isSafeSysfsPath(path: String?): Boolean =
        !path.isNullOrBlank() && !path.contains("..") && validSysPathRegex.matches(path)

    fun isValidSetting(v: String?): Boolean =
        !v.isNullOrBlank() && !v.equals("null", ignoreCase = true) && !v.startsWith("ERR")
}
