package com.personalagenda.app.ui.theme

/** User-selectable theme options, chosen in Settings. */
enum class AppThemeOption(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    MINT("Mint"),
    ROSE("Rose"),
    LEMON("Lemon"),
    SKY("Sky"),
}

/** Resolves the concrete palette for an option (System follows [systemDark]). */
fun AppThemeOption.colors(systemDark: Boolean): AgendaColors = when (this) {
    AppThemeOption.SYSTEM -> if (systemDark) DarkAgendaColors else LightAgendaColors
    AppThemeOption.LIGHT -> LightAgendaColors
    AppThemeOption.DARK -> DarkAgendaColors
    AppThemeOption.MINT -> MintAgendaColors
    AppThemeOption.ROSE -> RoseAgendaColors
    AppThemeOption.LEMON -> LemonAgendaColors
    AppThemeOption.SKY -> SkyAgendaColors
}
