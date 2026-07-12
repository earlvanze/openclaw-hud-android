package ai.openclaw.app

enum class AirVisionViewMode(
    val rawValue: String,
    val label: String,
) {
    Working("working", "Working"),
    Gaming("gaming", "Gaming"),
    Infinity("infinity", "Infinity"),
    Custom1("custom1", "Custom 1"),
    Custom2("custom2", "Custom 2"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionViewMode =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: Working
    }
}

data class AirVisionCustomProfileLabels(
    val custom1: String = AirVisionViewMode.Custom1.label,
    val custom2: String = AirVisionViewMode.Custom2.label,
) {
    fun labelFor(mode: AirVisionViewMode): String =
        when (mode) {
            AirVisionViewMode.Custom1 -> custom1
            AirVisionViewMode.Custom2 -> custom2
            else -> mode.label
        }

    companion object {
        const val MAX_LABEL_LENGTH = 24

        fun normalizeLabel(
            value: String,
            fallback: String,
        ): String {
            val compact = value.trim().replace(Regex("\\s+"), " ")
            return compact.take(MAX_LABEL_LENGTH).ifBlank { fallback }
        }
    }
}

enum class AirVisionSplendidMode(
    val rawValue: String,
    val label: String,
) {
    Standard("standard", "Standard"),
    Theater("theater", "Theater"),
    Office("office", "Office"),
    Game("game", "Game"),
    EyeCare("eye_care", "Eye Care"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionSplendidMode =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: Standard
    }
}

enum class AirVisionHudPlacement(
    val rawValue: String,
    val label: String,
) {
    UpperLeft("upper_left", "Upper Left"),
    UpperCenter("upper_center", "Upper Center"),
    UpperRight("upper_right", "Upper Right"),
    Center("center", "Center"),
    LowerCenter("lower_center", "Lower Center"),
    ;

    companion object {
        fun fromRawValue(rawValue: String?): AirVisionHudPlacement =
            entries.firstOrNull { it.rawValue == rawValue?.trim()?.lowercase() } ?: UpperLeft
    }
}

data class AirVisionDisplaySettings(
    val viewMode: AirVisionViewMode = AirVisionViewMode.Working,
    val splendidMode: AirVisionSplendidMode = AirVisionSplendidMode.Standard,
    val hudPlacement: AirVisionHudPlacement = AirVisionHudPlacement.UpperLeft,
    val brightnessPercent: Int = DEFAULT_BRIGHTNESS_PERCENT,
    val blueLightFilterPercent: Int = DEFAULT_BLUE_LIGHT_FILTER_PERCENT,
    val distanceCm: Int = DEFAULT_DISTANCE_CM,
    val hudScalePercent: Int = DEFAULT_HUD_SCALE_PERCENT,
    val ipdMm: Int = DEFAULT_IPD_MM,
    val safeAreaPercent: Int = DEFAULT_SAFE_AREA_PERCENT,
    val physicalMainScreenVisible: Boolean = true,
    val motionSyncEnabled: Boolean = true,
    val threeDModeEnabled: Boolean = false,
    val lightLoadModeEnabled: Boolean = false,
) {
    val ipdAdjustmentEnabled: Boolean
        get() = !lightLoadModeEnabled

    val threeDModeAvailable: Boolean
        get() = !lightLoadModeEnabled

    val blueLightFilterAvailable: Boolean
        get() = splendidMode == AirVisionSplendidMode.EyeCare

    val normalized: AirVisionDisplaySettings
        get() =
            copy(
                brightnessPercent = normalizeBrightnessPercent(brightnessPercent),
                blueLightFilterPercent = normalizeBlueLightFilterPercent(blueLightFilterPercent),
                distanceCm = normalizeDistanceCm(distanceCm),
                hudScalePercent = normalizeHudScalePercent(hudScalePercent),
                ipdMm = normalizeIpdMm(ipdMm),
                safeAreaPercent = normalizeSafeAreaPercent(safeAreaPercent),
                threeDModeEnabled = threeDModeEnabled && !lightLoadModeEnabled,
            )

    companion object {
        const val MIN_BRIGHTNESS_PERCENT = 15
        const val MAX_BRIGHTNESS_PERCENT = 100
        const val DEFAULT_BRIGHTNESS_PERCENT = 80
        const val MIN_BLUE_LIGHT_FILTER_PERCENT = 0
        const val MAX_BLUE_LIGHT_FILTER_PERCENT = 100
        const val DEFAULT_BLUE_LIGHT_FILTER_PERCENT = 0
        const val MIN_DISTANCE_CM = 40
        const val MAX_DISTANCE_CM = 140
        const val DEFAULT_DISTANCE_CM = 75
        const val MIN_HUD_SCALE_PERCENT = 75
        const val MAX_HUD_SCALE_PERCENT = 140
        const val DEFAULT_HUD_SCALE_PERCENT = 100
        const val MIN_IPD_MM = 52
        const val MAX_IPD_MM = 78
        const val MIN_ASUS_IPD_MM = 54
        const val MAX_ASUS_IPD_MM = 74
        const val DEFAULT_IPD_MM = 67
        const val MIN_SAFE_AREA_PERCENT = 0
        const val MAX_SAFE_AREA_PERCENT = 20
        const val DEFAULT_SAFE_AREA_PERCENT = 5
        const val HUD_TRANSCRIPT_ENTRY_COUNT = 8
        const val HUD_CAPTION_ENTRY_COUNT = 5
        const val LIGHT_LOAD_HUD_TRANSCRIPT_ENTRY_COUNT = 3
        const val LIGHT_LOAD_HUD_CAPTION_ENTRY_COUNT = 2

        fun defaultsForViewMode(mode: AirVisionViewMode): AirVisionDisplaySettings =
            when (mode) {
                AirVisionViewMode.Working ->
                    AirVisionDisplaySettings(
                        viewMode = mode,
                        splendidMode = AirVisionSplendidMode.Standard,
                        hudPlacement = AirVisionHudPlacement.UpperLeft,
                        brightnessPercent = DEFAULT_BRIGHTNESS_PERCENT,
                        blueLightFilterPercent = DEFAULT_BLUE_LIGHT_FILTER_PERCENT,
                        distanceCm = DEFAULT_DISTANCE_CM,
                        hudScalePercent = DEFAULT_HUD_SCALE_PERCENT,
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = DEFAULT_SAFE_AREA_PERCENT,
                        physicalMainScreenVisible = true,
                        motionSyncEnabled = true,
                        threeDModeEnabled = false,
                        lightLoadModeEnabled = false,
                    )
                AirVisionViewMode.Gaming ->
                    AirVisionDisplaySettings(
                        viewMode = mode,
                        splendidMode = AirVisionSplendidMode.Game,
                        hudPlacement = AirVisionHudPlacement.Center,
                        brightnessPercent = 100,
                        blueLightFilterPercent = 0,
                        distanceCm = 65,
                        hudScalePercent = 105,
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = 3,
                        physicalMainScreenVisible = true,
                        motionSyncEnabled = true,
                        threeDModeEnabled = false,
                        lightLoadModeEnabled = true,
                    )
                AirVisionViewMode.Infinity ->
                    AirVisionDisplaySettings(
                        viewMode = mode,
                        splendidMode = AirVisionSplendidMode.Standard,
                        hudPlacement = AirVisionHudPlacement.UpperCenter,
                        brightnessPercent = 70,
                        blueLightFilterPercent = 10,
                        distanceCm = 120,
                        hudScalePercent = 90,
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = 8,
                        physicalMainScreenVisible = true,
                        motionSyncEnabled = true,
                        threeDModeEnabled = false,
                        lightLoadModeEnabled = true,
                    )
                AirVisionViewMode.Custom1 ->
                    AirVisionDisplaySettings(
                        viewMode = mode,
                        splendidMode = AirVisionSplendidMode.Office,
                        hudPlacement = AirVisionHudPlacement.UpperLeft,
                        brightnessPercent = DEFAULT_BRIGHTNESS_PERCENT,
                        blueLightFilterPercent = 15,
                        distanceCm = 90,
                        hudScalePercent = DEFAULT_HUD_SCALE_PERCENT,
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = DEFAULT_SAFE_AREA_PERCENT,
                        physicalMainScreenVisible = true,
                        motionSyncEnabled = true,
                        threeDModeEnabled = false,
                        lightLoadModeEnabled = false,
                    )
                AirVisionViewMode.Custom2 ->
                    AirVisionDisplaySettings(
                        viewMode = mode,
                        splendidMode = AirVisionSplendidMode.EyeCare,
                        hudPlacement = AirVisionHudPlacement.LowerCenter,
                        brightnessPercent = 75,
                        blueLightFilterPercent = 30,
                        distanceCm = 60,
                        hudScalePercent = 110,
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = 10,
                        physicalMainScreenVisible = true,
                        motionSyncEnabled = true,
                        threeDModeEnabled = false,
                        lightLoadModeEnabled = false,
                    )
            }.normalized

        fun normalizeBrightnessPercent(value: Int): Int = value.coerceIn(MIN_BRIGHTNESS_PERCENT, MAX_BRIGHTNESS_PERCENT)

        fun normalizeBlueLightFilterPercent(value: Int): Int = value.coerceIn(MIN_BLUE_LIGHT_FILTER_PERCENT, MAX_BLUE_LIGHT_FILTER_PERCENT)

        fun normalizeDistanceCm(value: Int): Int = value.coerceIn(MIN_DISTANCE_CM, MAX_DISTANCE_CM)

        fun normalizeHudScalePercent(value: Int): Int = value.coerceIn(MIN_HUD_SCALE_PERCENT, MAX_HUD_SCALE_PERCENT)

        fun normalizeIpdMm(value: Int): Int = value.coerceIn(MIN_IPD_MM, MAX_IPD_MM)

        fun normalizeAsusIpdMm(value: Int): Int = value.coerceIn(MIN_ASUS_IPD_MM, MAX_ASUS_IPD_MM)

        fun isWithinAsusIpdRange(value: Int): Boolean = value in MIN_ASUS_IPD_MM..MAX_ASUS_IPD_MM

        fun normalizeSafeAreaPercent(value: Int): Int = value.coerceIn(MIN_SAFE_AREA_PERCENT, MAX_SAFE_AREA_PERCENT)

        fun hudScaleForDistanceCm(value: Int): Float {
            val normalized = normalizeDistanceCm(value)
            val ratio = DEFAULT_DISTANCE_CM.toFloat() / normalized.toFloat()
            return ratio.coerceIn(0.72f, 1.24f)
        }

        fun hudScaleMultiplierForPercent(value: Int): Float =
            normalizeHudScalePercent(value).toFloat() / DEFAULT_HUD_SCALE_PERCENT.toFloat()

        fun hudScaleMultiplierForViewMode(mode: AirVisionViewMode): Float =
            when (mode) {
                AirVisionViewMode.Working -> 1.0f
                AirVisionViewMode.Gaming -> 1.08f
                AirVisionViewMode.Infinity -> 0.84f
                AirVisionViewMode.Custom1 -> 0.94f
                AirVisionViewMode.Custom2 -> 1.14f
            }

        fun hudDimAlphaForBrightnessPercent(value: Int): Float {
            val normalized = normalizeBrightnessPercent(value)
            return ((100 - normalized).toFloat() / 100f * 0.72f).coerceIn(0f, 0.62f)
        }

        fun hudTranscriptEntryCount(lightLoadModeEnabled: Boolean): Int =
            if (lightLoadModeEnabled) {
                LIGHT_LOAD_HUD_TRANSCRIPT_ENTRY_COUNT
            } else {
                HUD_TRANSCRIPT_ENTRY_COUNT
            }

        fun hudCaptionEntryCount(lightLoadModeEnabled: Boolean): Int =
            if (lightLoadModeEnabled) {
                LIGHT_LOAD_HUD_CAPTION_ENTRY_COUNT
            } else {
                HUD_CAPTION_ENTRY_COUNT
            }

        fun hudColorPreviewAlpha(
            alpha: Float,
            lightLoadModeEnabled: Boolean,
        ): Float =
            if (lightLoadModeEnabled) {
                0f
            } else {
                alpha.coerceIn(0f, 1f)
            }

        fun hudWarmOverlayAlpha(
            splendidMode: AirVisionSplendidMode,
            blueLightFilterPercent: Int,
        ): Float {
            return hudSplendidOverlayAlpha(splendidMode, blueLightFilterPercent)
        }

        fun hudSplendidOverlayAlpha(
            splendidMode: AirVisionSplendidMode,
            blueLightFilterPercent: Int,
        ): Float {
            return when (splendidMode) {
                AirVisionSplendidMode.Standard -> 0f
                AirVisionSplendidMode.Theater -> 0.08f
                AirVisionSplendidMode.Office -> 0.05f
                AirVisionSplendidMode.Game -> 0.06f
                AirVisionSplendidMode.EyeCare -> {
                    val base = 0.10f
                    val slider =
                        normalizeBlueLightFilterPercent(blueLightFilterPercent).toFloat() / 100f * 0.24f
                    (base + slider).coerceIn(0f, 0.34f)
                }
            }
        }
    }
}
