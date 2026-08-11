package com.personalagenda.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.personalagenda.app.ui.theme.AgendaTheme

/** Letter-spaced section label used across the dashboard (TODAY, FOCUS, TASKS...). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AgendaTheme.type.sectionLabel,
        color = AgendaTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A slim, restrained progress bar — used by Right Now, projects and workload. */
@Composable
fun ThinProgressBar(
    progress: Float,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    height: Int = 6,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(height.dp)
                .clip(RoundedCornerShape(50))
                .background(fill),
        )
    }
}
