package com.personalagenda.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The design brief calls for a calm, editorial palette rather than a generic
 * SaaS look, so we define our own semantic color tokens instead of leaning on
 * Material defaults. Category accents are intentionally muted.
 */
@Immutable
data class AgendaColors(
    val background: Color,
    val surface: Color,        // slightly differentiated surface for real "objects"
    val surfaceElevated: Color,
    val textPrimary: Color,    // charcoal
    val textSecondary: Color,  // muted gray
    val textFaint: Color,      // tiny metadata
    val divider: Color,
    // Muted category accents
    val personal: Color,
    val work: Color,
    val learning: Color,
    val health: Color,
    val social: Color,
    val projects: Color,
    val deadline: Color,
    val isDark: Boolean,
)

val LightAgendaColors = AgendaColors(
    background = Color(0xFFFAF8F4),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFDFBF7),
    textPrimary = Color(0xFF2A2A28),
    textSecondary = Color(0xFF8A867E),
    textFaint = Color(0xFFB4AFA4),
    divider = Color(0xFFE9E4DA),
    personal = Color(0xFF7C8DB0),
    work = Color(0xFF8E82A8),
    learning = Color(0xFF5E9A97),
    health = Color(0xFF7BA07C),
    social = Color(0xFFC79A6B),
    projects = Color(0xFF6C74A6),
    deadline = Color(0xFFB87A72),
    isDark = false,
)

val DarkAgendaColors = AgendaColors(
    background = Color(0xFF17171A),
    surface = Color(0xFF212125),
    surfaceElevated = Color(0xFF26262B),
    textPrimary = Color(0xFFECEAE4),
    textSecondary = Color(0xFF9C978E),
    textFaint = Color(0xFF6C6862),
    divider = Color(0xFF33333A),
    personal = Color(0xFF8FA0C4),
    work = Color(0xFFA396BE),
    learning = Color(0xFF6FB0AD),
    health = Color(0xFF8DB48E),
    social = Color(0xFFD6AC7D),
    projects = Color(0xFF838BC0),
    deadline = Color(0xFFCE8B82),
    isDark = true,
)

/**
 * Fun, tinted light themes. Each keeps dark, readable text on a softly colored
 * background, and uses its signature hue as the primary accent (Right Now progress,
 * selected controls, action buttons, Material sliders/pickers).
 */

/** Light green. */
val MintAgendaColors = AgendaColors(
    background = Color(0xFFE7F3EA),
    surface = Color(0xFFF3FAF4),
    surfaceElevated = Color(0xFFEDF7EF),
    textPrimary = Color(0xFF213028),
    textSecondary = Color(0xFF5C7A67),
    textFaint = Color(0xFF9BB7A6),
    divider = Color(0xFFD2E6D8),
    personal = Color(0xFF3F9D6B),
    work = Color(0xFF8E82A8),
    learning = Color(0xFF2F9088),
    health = Color(0xFF5F9E60),
    social = Color(0xFFC0894F),
    projects = Color(0xFF6C74A6),
    deadline = Color(0xFFC2584F),
    isDark = false,
)

/** Pinkish red / rose. */
val RoseAgendaColors = AgendaColors(
    background = Color(0xFFF8EBEE),
    surface = Color(0xFFFDF5F7),
    surfaceElevated = Color(0xFFF9EFF2),
    textPrimary = Color(0xFF342529),
    textSecondary = Color(0xFF8A6470),
    textFaint = Color(0xFFC4A2AC),
    divider = Color(0xFFECD6DC),
    personal = Color(0xFFC85A72),
    work = Color(0xFF9E6BA0),
    learning = Color(0xFF5E9A97),
    health = Color(0xFF7BA07C),
    social = Color(0xFFCE7A52),
    projects = Color(0xFF8A6FB0),
    deadline = Color(0xFFC24E5C),
    isDark = false,
)

/** Warm yellow. */
val LemonAgendaColors = AgendaColors(
    background = Color(0xFFF7F1D8),
    surface = Color(0xFFFCF8E8),
    surfaceElevated = Color(0xFFF8F3DE),
    textPrimary = Color(0xFF33301C),
    textSecondary = Color(0xFF847A50),
    textFaint = Color(0xFFC3B888),
    divider = Color(0xFFE9E1C0),
    personal = Color(0xFFC79A22),
    work = Color(0xFF8E82A8),
    learning = Color(0xFF5E9A97),
    health = Color(0xFF7BA07C),
    social = Color(0xFFC97F35),
    projects = Color(0xFF6C74A6),
    deadline = Color(0xFFC2584F),
    isDark = false,
)

/** Light blue / sky. */
val SkyAgendaColors = AgendaColors(
    background = Color(0xFFE7F0F8),
    surface = Color(0xFFF3F8FD),
    surfaceElevated = Color(0xFFEDF4FB),
    textPrimary = Color(0xFF22303C),
    textSecondary = Color(0xFF5D7688),
    textFaint = Color(0xFF9CB4C6),
    divider = Color(0xFFD2E2EF),
    personal = Color(0xFF3E82C4),
    work = Color(0xFF8E82A8),
    learning = Color(0xFF2F9088),
    health = Color(0xFF5F9E85),
    social = Color(0xFFC0894F),
    projects = Color(0xFF5A73C0),
    deadline = Color(0xFFC2584F),
    isDark = false,
)

val LocalAgendaColors = staticCompositionLocalOf { LightAgendaColors }
