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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    syncConfigured: Boolean = false,
    accountEmail: String? = null,
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val colors = AgendaTheme.colors
    // Sections collapse for readability; the header shows the current value when collapsed.
    var appearanceExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    val accountSummary = when {
        !syncConfigured -> "Not set up"
        accountEmail == null -> "Signed out"
        else -> accountEmail
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            CollapsibleSection(
                title = "Appearance",
                summary = current.label,
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded },
            ) {
                AppThemeOption.entries.forEach { option ->
                    ThemeRow(
                        option = option,
                        selected = option == current,
                        systemDark = systemDark,
                        onClick = { onSelect(option) },
                    )
                }
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))

            CollapsibleSection(
                title = "Account & sync",
                summary = accountSummary,
                expanded = accountExpanded,
                onToggle = { accountExpanded = !accountExpanded },
            ) {
                AccountSection(
                    syncConfigured = syncConfigured,
                    accountEmail = accountEmail,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                TextAction("Done", muted = false, enabled = true, onClick = onDismiss)
            }
        }
    }
}

/** A section with a tappable header that expands/collapses [content]. */
@Composable
private fun CollapsibleSection(
    title: String,
    summary: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AgendaTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 14.dp),
        ) {
            SectionLabel(title)
            Spacer(Modifier.weight(1f))
            if (!expanded && summary != null) {
                Text(summary, style = AgendaTheme.type.secondary, color = colors.textSecondary)
                Spacer(Modifier.width(12.dp))
            }
            Text(
                if (expanded) "▾" else "▸", // ▾ / ▸
                style = AgendaTheme.type.body,
                color = colors.textSecondary,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            content()
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AccountSection(
    syncConfigured: Boolean,
    accountEmail: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val colors = AgendaTheme.colors
    when {
        !syncConfigured -> Text(
            "Google sync isn't set up on this build yet.",
            style = AgendaTheme.type.body,
            color = colors.textSecondary,
        )
        accountEmail == null -> {
            Text(
                "Sign in to sync your agenda across devices.",
                style = AgendaTheme.type.body,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(14.dp))
            PillButton("Sign in with Google", filled = true, onClick = onSignIn)
        }
        else -> {
            Text("Signed in", style = AgendaTheme.type.secondary, color = colors.textSecondary)
            Spacer(Modifier.height(2.dp))
            Text(accountEmail, style = AgendaTheme.type.body, color = colors.textPrimary)
            Spacer(Modifier.height(14.dp))
            PillButton("Sign out", filled = false, onClick = onSignOut)
        }
    }
}

@Composable
private fun PillButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (filled) Modifier.background(colors.textPrimary)
                else Modifier.border(1.dp, colors.divider, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Text(
            label,
            style = AgendaTheme.type.secondary,
            color = if (filled) colors.background else colors.textSecondary,
        )
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
