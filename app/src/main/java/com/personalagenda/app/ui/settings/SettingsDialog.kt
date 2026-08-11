package com.personalagenda.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.dashboard.TextAction
import com.personalagenda.app.ui.theme.AgendaTheme
import com.personalagenda.app.ui.theme.AppThemeOption
import com.personalagenda.app.ui.theme.colors as resolveColors

@Composable
fun SettingsDialog(
    current: AppThemeOption,
    systemDark: Boolean,
    onSelect: (AppThemeOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AgendaTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .padding(28.dp),
        ) {
            SectionLabel("Appearance")
            Spacer(Modifier.height(20.dp))

            AppThemeOption.entries.forEach { option ->
                ThemeRow(
                    option = option,
                    selected = option == current,
                    systemDark = systemDark,
                    onClick = { onSelect(option) },
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextAction("Done", muted = false, enabled = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ThemeRow(
    option: AppThemeOption,
    selected: Boolean,
    systemDark: Boolean,
    onClick: () -> Unit,
) {
    val colors = AgendaTheme.colors
    val swatch = option.resolveColors(systemDark)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        // Swatch: the theme's background with its accent dot.
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(swatch.background)
                .border(1.dp, colors.divider, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(swatch.personal))
        }
        Spacer(Modifier.width(16.dp))
        Text(option.label, style = AgendaTheme.type.body, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        if (selected) {
            Text("✓", style = AgendaTheme.type.bodyStrong, color = colors.personal)
        }
    }
}
