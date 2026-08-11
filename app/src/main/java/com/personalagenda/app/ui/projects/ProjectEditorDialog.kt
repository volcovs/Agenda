package com.personalagenda.app.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.personalagenda.app.data.Category
import com.personalagenda.app.data.Project
import com.personalagenda.app.ui.dashboard.CategoryChip
import com.personalagenda.app.ui.dashboard.FieldLabel
import com.personalagenda.app.ui.dashboard.SectionLabel
import com.personalagenda.app.ui.dashboard.TextAction
import com.personalagenda.app.ui.dashboard.UnderlinedField
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val deadlineFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorDialog(
    initial: Project?,
    onSave: (Project) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val colors = AgendaTheme.colors
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Category.PROJECTS) }
    var status by remember { mutableStateOf(initial?.status ?: "") }
    var nextAction by remember { mutableStateOf(initial?.nextAction ?: "") }
    var progress by remember { mutableFloatStateOf(initial?.progress ?: 0f) }
    var deadline by remember { mutableStateOf(initial?.deadline) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank()

    fun save() {
        if (!canSave) return
        onSave(
            Project(
                name = name.trim(),
                progress = progress,
                category = category,
                description = description.trim().ifEmpty { null },
                status = status.trim().ifEmpty { null },
                nextAction = nextAction.trim().ifEmpty { null },
                deadline = deadline,
                id = initial?.id ?: 0,
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
            SectionLabel(if (initial == null) "New project" else "Edit project")
            Spacer(Modifier.height(20.dp))

            UnderlinedField(name, { name = it }, "Project name", singleLine = true, imeAction = androidx.compose.ui.text.input.ImeAction.Next)
            Spacer(Modifier.height(20.dp))
            UnderlinedField(description, { description = it }, "Description (optional)", singleLine = false, imeAction = androidx.compose.ui.text.input.ImeAction.Default)

            Spacer(Modifier.height(24.dp))
            FieldLabel("Category")
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.entries.forEach { cat ->
                    CategoryChip(cat, selected = cat == category, onClick = { category = cat })
                }
            }

            Spacer(Modifier.height(24.dp))
            UnderlinedField(status, { status = it }, "Status (optional)", singleLine = true, imeAction = androidx.compose.ui.text.input.ImeAction.Next)
            Spacer(Modifier.height(20.dp))
            UnderlinedField(nextAction, { nextAction = it }, "Next action (optional)", singleLine = true, imeAction = androidx.compose.ui.text.input.ImeAction.Done)

            Spacer(Modifier.height(24.dp))
            FieldLabel("Progress · ${(progress * 100).roundToInt()}%")
            Slider(value = progress, onValueChange = { progress = it }, valueRange = 0f..1f)

            Spacer(Modifier.height(16.dp))
            FieldLabel("Deadline")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    deadline?.format(deadlineFmt) ?: "None",
                    style = AgendaTheme.type.body,
                    color = if (deadline == null) colors.textFaint else colors.textPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (deadline != null) {
                    TextAction("Clear", muted = true, enabled = true, onClick = { deadline = null })
                }
                TextAction("Set", muted = false, enabled = true, onClick = { showDatePicker = true })
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onDelete(); onDismiss() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text("Delete", style = AgendaTheme.type.bodyStrong, color = colors.deadline)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextAction("Cancel", muted = true, enabled = true, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                TextAction("Save", muted = false, enabled = canSave, onClick = { save() })
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = deadline?.toEpochDay()?.times(86_400_000L),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadline = dpState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dpState)
        }
    }
}
