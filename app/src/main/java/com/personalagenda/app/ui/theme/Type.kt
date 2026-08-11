package com.personalagenda.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A clear typographic hierarchy drives the design more than decoration does.
 * We use the platform's default sans-serif (Roboto) for now; a custom font
 * (e.g. Inter) can be dropped into res/font later without touching call sites.
 */
private val Sans = FontFamily.Default

@Immutable
data class AgendaTypography(
    val display: TextStyle,     // major date / dashboard heading
    val greeting: TextStyle,    // "GOOD MORNING"
    val sectionLabel: TextStyle,// FOCUS / TASKS / TODAY  (letter-spaced, small caps feel)
    val heading: TextStyle,
    val body: TextStyle,        // event names, notes
    val bodyStrong: TextStyle,
    val secondary: TextStyle,   // time, location, metadata
    val tiny: TextStyle,        // subtle status indicators
)

val AgendaType = AgendaTypography(
    display = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    greeting = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 2.5.sp,
    ),
    sectionLabel = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp,
    ),
    heading = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    body = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyStrong = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    secondary = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    tiny = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

// A minimal Material3 Typography so any stray Material components stay consistent.
val Material3Typography = Typography()
