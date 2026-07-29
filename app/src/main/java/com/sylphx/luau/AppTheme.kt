package com.sylphx.luau

/**
 * Definition of one selectable animated theme.
 * colorStart/colorEnd drive the animated gradient (used both in the small
 * preview chips on the Settings tab and in the full-screen animated
 * background), dotColor is the little accent dot shown on preview chips.
 */
data class AppTheme(
    val id: String,
    val displayName: String,
    val colorStart: Int,
    val colorEnd: Int,
    val dotColor: Int
) {
    companion object {
        val ALL: List<AppTheme> = listOf(
            AppTheme("cyan_darkblue", "Cyan Blue", 0xFF0F2044.toInt(), 0xFF050B18.toInt(), 0xFF22C7F5.toInt()),
            AppTheme("particles", "Particles", 0xFF2B2B5C.toInt(), 0xFF0A0A14.toInt(), 0xFF8C8CFF.toInt()),
            AppTheme("matrix_rain", "Matrix Rain", 0xFF0F3D1E.toInt(), 0xFF07140A.toInt(), 0xFF33FF66.toInt()),
            AppTheme("aurora_borealis", "Aurora Borealis", 0xFF0E3B4E.toInt(), 0xFF081824.toInt(), 0xFF33D6FF.toInt()),
            AppTheme("fireflies", "Fireflies", 0xFF2C4A12.toInt(), 0xFF0E1A08.toInt(), 0xFFB6FF3D.toInt()),
            AppTheme("nebula", "Nebula", 0xFF4A1259.toInt(), 0xFF190821.toInt(), 0xFFE23DFF.toInt()),
            AppTheme("lava_flow", "Lava Flow", 0xFF5C1A0A.toInt(), 0xFF1A0603.toInt(), 0xFFFF4D1A.toInt()),
            AppTheme("rainbow_wave", "Rainbow Wave", 0xFF4A3A0F.toInt(), 0xFF1A1406.toInt(), 0xFFFFB020.toInt()),
            AppTheme("neon_pulse", "Neon Pulse", 0xFF0E3040.toInt(), 0xFF081A22.toInt(), 0xFF00E5FF.toInt()),
            AppTheme("snowfall", "Snowfall", 0xFF1E2430.toInt(), 0xFF0A0C10.toInt(), 0xFFCFE8FF.toInt()),
            AppTheme("starfield", "Starfield", 0xFF15151F.toInt(), 0xFF050507.toInt(), 0xFFFFFFFF.toInt()),
            AppTheme("glitch", "Glitch", 0xFF4A0E4A.toInt(), 0xFF190819.toInt(), 0xFFFF3DDA.toInt()),
            AppTheme("spark_shower", "Spark Shower", 0xFF4A3A0A.toInt(), 0xFF1A1404.toInt(), 0xFFFFA500.toInt()),
            AppTheme("vortex", "Vortex", 0xFF2E0E4A.toInt(), 0xFF10061C.toInt(), 0xFF9B3DFF.toInt()),
            AppTheme("ocean_ripple", "Ocean Ripple", 0xFF0E3A4A.toInt(), 0xFF06181F.toInt(), 0xFF3DDCFF.toInt()),
            AppTheme("neon_emerald", "Neon Emerald", 0xFF0E4A2C.toInt(), 0xFF06190E.toInt(), 0xFF3DFF8C.toInt()),
            AppTheme("solar_flare", "Solar Flare", 0xFF4A2E0A.toInt(), 0xFF1C1004.toInt(), 0xFFFFC93D.toInt()),
            AppTheme("deep_space", "Deep Space", 0xFF161633.toInt(), 0xFF050510.toInt(), 0xFF5C6BFF.toInt()),
            AppTheme("cherry_blossom", "Cherry Blossom", 0xFF4A1E2E.toInt(), 0xFF1C0A11.toInt(), 0xFFFF8CB3.toInt()),
            AppTheme("toxic_waste", "Toxic Waste", 0xFF334A0A.toInt(), 0xFF141C04.toInt(), 0xFFC6FF3D.toInt()),
            AppTheme("frost_bite", "Frost Bite", 0xFF0E3A4A.toInt(), 0xFF071820.toInt(), 0xFF7DE3FF.toInt())
        )

        val DEFAULT: AppTheme = ALL.find { it.id == "cyan_darkblue" } ?: ALL.first()

        fun byId(id: String?): AppTheme = ALL.find { it.id == id } ?: DEFAULT
    }
}
