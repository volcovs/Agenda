package com.personalagenda.app.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.personalagenda.app.data.Event
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorDialog(
    event: Event,
    onSave: (Event) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AgendaTheme.colors
    var title by remember { mutableStateOf(event.title) }
    var detail by remember { mutableStateOf(event.subtitle ?: "") }
    var category by remember { mutableStateOf(event.category) }
    val startState = rememberTimePickerState(event.start.hour, event.start.minute, is24Hour = true)
    val endState = rememberTimePickerState(event.end.hour, event.end.minute, is24Hour = true)

    val validTimes = (endState.hour * 60 + endState.minute) > (startState.hour * 60 + startState.minute)
    val canSave = title.isNotBlank() && validTimes

    fun save() {
        if (!canSave) return
        onSave(
            event.copy(
                title = title.trim(),
                subtitle = detail.trim().ifEmpty { null },
                category = category,
                start = LocalTime.of(startState.hour, startState.minute),
                end = LocalTime.of(endState.hour, endState.minute),
            )
        )
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
        ) {
            SectionLabel("Edit event")
            Spacer(Modifier.height(20.dp))

            EventForm(
                title = title,
                onTitleChange = { title = it },
                detail = detail,
                onDetailChange = { detail = it },
                category = category,
                onCategoryChange = { category = it },
                startState = startState,
                endState = endState,
                validTimes = validTimes,
            )

            Spacer(Modifier.height(28.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDelete(event.id); onDismiss() }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text("Delete", style = AgendaTheme.type.bodyStrong, color = colors.deadline)
                }
                Spacer(Modifier.width(0.dp).weight(1f))
                TextAction("Cancel", muted = true, enabled = true, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                TextAction("Save", muted = false, enabled = canSave, onClick = { save() })
            }
        }
    }
}
