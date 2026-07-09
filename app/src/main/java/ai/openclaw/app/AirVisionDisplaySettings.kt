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
    val ipdMm: Int = DEFAULT_IPD_MM,
    val safeAreaPercent: Int = DEFAULT_SAFE_AREA_PERCENT,
    val motionSyncEnabled: Boolean = true,
    val lightLoadModeEnabled: Boolean = false,
) {
    val normalized: AirVisionDisplaySettings
        get() =
            copy(
                brightnessPercent = normalizeBrightnessPercent(brightnessPercent),
                blueLightFilterPercent = normalizeBlueLightFilterPercent(blueLightFilterPercent),
                distanceCm = normalizeDistanceCm(distanceCm),
                ipdMm = normalizeIpdMm(ipdMm),
                safeAreaPercent = normalizeSafeAreaPercent(safeAreaPercent),
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
        const val MIN_IPD_MM = 52
        const val MAX_IPD_MM = 78
        const val DEFAULT_IPD_MM = 67
        const val MIN_SAFE_AREA_PERCENT = 0
        const val MAX_SAFE_AREA_PERCENT = 20
        const val DEFAULT_SAFE_AREA_PERCENT = 5

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
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = DEFAULT_SAFE_AREA_PERCENT,
                        motionSyncEnabled = true,
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
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = 3,
                        motionSyncEnabled = true,
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
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = 8,
                        motionSyncEnabled = true,
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
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = DEFAULT_SAFE_AREA_PERCENT,
                        motionSyncEnabled = true,
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
                        ipdMm = DEFAULT_IPD_MM,
                        safeAreaPercent = 10,
                        motionSyncEnabled = true,
                        lightLoadModeEnabled = false,
                    )
            }.normalized

        fun normalizeBrightnessPercent(value: Int): Int = value.coerceIn(MIN_BRIGHTNESS_PERCENT, MAX_BRIGHTNESS_PERCENT)

        fun normalizeBlueLightFilterPercent(value: Int): Int = value.coerceIn(MIN_BLUE_LIGHT_FILTER_PERCENT, MAX_BLUE_LIGHT_FILTER_PERCENT)

        fun normalizeDistanceCm(value: Int): Int = value.coerceIn(MIN_DISTANCE_CM, MAX_DISTANCE_CM)

        fun normalizeIpdMm(value: Int): Int = value.coerceIn(MIN_IPD_MM, MAX_IPD_MM)

        fun normalizeSafeAreaPercent(value: Int): Int = value.coerceIn(MIN_SAFE_AREA_PERCENT, MAX_SAFE_AREA_PERCENT)

        fun hudScaleForDistanceCm(value: Int): Float {
            val normalized = normalizeDistanceCm(value)
            val ratio = DEFAULT_DISTANCE_CM.toFloat() / normalized.toFloat()
            return ratio.coerceIn(0.72f, 1.24f)
        }

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

        fun hudWarmOverlayAlpha(
            splendidMode: AirVisionSplendidMode,
            blueLightFilterPercent: Int,
        ): Float {
            val base =
                if (splendidMode == AirVisionSplendidMode.EyeCare) {
                    0.10f
                } else {
                    0f
                }
            val slider =
                normalizeBlueLightFilterPercent(blueLightFilterPercent).toFloat() / 100f * 0.24f
            return (base + slider).coerceIn(0f, 0.34f)
        }
    }
}
