package com.personalagenda.app.ui.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.personalagenda.app.data.Category
import com.personalagenda.app.data.Event
import com.personalagenda.app.data.Project
import com.personalagenda.app.ui.theme.AgendaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class QuickAddType(val label: String) {
    TASK("Task"), NOTE("Note"), EVENT("Event")
}

/** The outcome of a Quick Add, so each kind can carry the fields it needs. */
sealed interface QuickAddResult {
    /** [date] is null for a "Someday" (undated) task; [projectId] links it to a project. */
    data class AddTask(val text: String, val date: LocalDate?, val projectId: Long?) : QuickAddResult
    data class AddNote(val text: String) : QuickAddResult
    data class NewEvent(val event: Event) : QuickAddResult
}

private val taskDateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onSubmit: (QuickAddResult) -> Unit,
    defaultDate: LocalDate = LocalDate.now(),
    projects: List<Project> = emptyList(),
) {
    val colors = AgendaTheme.colors
    var type by remember { mutableStateOf(QuickAddType.TASK) }

    // Simple (text-only) state
    var text by remember { mutableStateOf("") }
    // Task scheduling: a concrete date, or null for "Someday".
    var taskDate by remember { mutableStateOf<LocalDate?>(defaultDate) }
    var taskProjectId by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Event state
    var eventTitle by remember { mutableStateOf("") }
    var eventDetail by remember { mutableStateOf("") }
    var eventCategory by remember { mutableStateOf(Category.WORK) }
    val startState = rememberTimePickerState(initialHour = 9, initialMinute = 0, is24Hour = true)
    val endState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = true)

    val startMin = startState.hour * 60 + startState.minute
    val endMin = endState.hour * 60 + endState.minute
    val validTimes = endMin > startMin

    val canSubmit = when (type) {
        QuickAddType.EVENT -> eventTitle.isNotBlank() && validTimes
        else -> text.isNotBlank()
    }

    fun submit() {
        if (!canSubmit) return
        val result = when (type) {
            QuickAddType.EVENT -> QuickAddResult.NewEvent(
                Event(
                    start = LocalTime.of(startState.hour, startState.minute),
                    end = LocalTime.of(endState.hour, endState.minute),
                    title = eventTitle.trim(),
                    subtitle = eventDetail.trim().ifEmpty { null },
                    category = eventCategory,
                )
            )
            QuickAddType.TASK -> QuickAddResult.AddTask(text.trim(), taskDate, taskProjectId)
            QuickAddType.NOTE -> QuickAddResult.AddNote(text.trim())
        }
        onSubmit(result)
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
            SectionLabel("Add")
            Spacer(Modifier.height(20.dp))

            // Type selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAddType.entries.forEach { t ->
                    TypeChip(label = t.label, selected = t == type, onClick = { type = t })
                }
            }

            Spacer(Modifier.height(24.dp))

            if (type == QuickAddType.EVENT) {
                EventForm(
                    title = eventTitle,
                    onTitleChange = { eventTitle = it },
                    detail = eventDetail,
                    onDetailChange = { eventDetail = it },
                    category = eventCategory,
                    onCategoryChange = { eventCategory = it },
                    startState = startState,
                    endState = endState,
                    validTimes = validTimes,
                )
            } else {
                UnderlinedField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = when (type) {
                        QuickAddType.TASK -> "What needs doing?"
                        QuickAddType.NOTE -> "Write a quick note…"
                        QuickAddType.EVENT -> ""
                    },
                    singleLine = type != QuickAddType.NOTE,
                    imeAction = ImeAction.Done,
                    onImeAction = { submit() },
                )
                if (type == QuickAddType.TASK) {
                    Spacer(Modifier.height(18.dp))
                    FieldLabel("When")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TypeChip(
                            label = taskDate?.format(taskDateFmt) ?: "Pick a date",
                            selected = taskDate != null,
                            onClick = { showDatePicker = true },
                        )
                        TypeChip("Someday", selected = taskDate == null, onClick = { taskDate = null })
                    }

                    if (projects.isNotEmpty()) {
                        Spacer(Modifier.height(18.dp))
                        FieldLabel("Project")
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TypeChip("None", selected = taskProjectId == null, onClick = { taskProjectId = null })
                            projects.forEach { p ->
                                TypeChip(p.name, selected = taskProjectId == p.id, onClick = { taskProjectId = p.id })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextAction("Cancel", muted = true, enabled = true, onClick = onDismiss)
                Spacer(Modifier.width(8.dp))
                TextAction("Add", muted = false, enabled = canSubmit, onClick = { submit() })
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = (taskDate ?: defaultDate).toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        taskDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventForm(
    title: String,
    onTitleChange: (String) -> Unit,
    detail: String,
    onDetailChange: (String) -> Unit,
    category: Category,
    onCategoryChange: (Category) -> Unit,
    startState: androidx.compose.material3.TimePickerState,
    endState: androidx.compose.material3.TimePickerState,
    validTimes: Boolean,
) {
    val colors = AgendaTheme.colors
    Column {
        UnderlinedField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = "Event title",
            singleLine = true,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(20.dp))
        UnderlinedField(
            value = detail,
            onValueChange = onDetailChange,
            placeholder = "Detail (optional)",
            singleLine = true,
            imeAction = ImeAction.Done,
        )

        Spacer(Modifier.height(24.dp))
        FieldLabel("Category")
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Category.entries.forEach { cat ->
                CategoryChip(cat, selected = cat == category, onClick = { onCategoryChange(cat) })
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                FieldLabel("Starts")
                Spacer(Modifier.height(8.dp))
                TimeInput(state = startState)
            }
            Column {
                FieldLabel("Ends")
                Spacer(Modifier.height(8.dp))
                TimeInput(state = endState)
            }
        }
        if (!validTimes) {
            Text(
                "End time must be after the start time.",
                style = AgendaTheme.type.secondary,
                color = colors.deadline,
            )
        }
    }
}

@Composable
internal fun UnderlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit = {},
) {
    val colors = AgendaTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = AgendaTheme.type.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.textPrimary),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction() },
            onNext = { onImeAction() },
        ),
        decorationBox = { inner ->
            Column {
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = AgendaTheme.type.body, color = colors.textFaint)
                    }
                    inner()
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text.uppercase(), style = AgendaTheme.type.tiny, color = AgendaTheme.colors.textSecondary)
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) Modifier.background(colors.textPrimary)
                else Modifier.border(1.dp, colors.divider, RoundedCornerShape(50))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = AgendaTheme.type.secondary,
            color = if (selected) colors.background else colors.textSecondary,
        )
    }
}

@Composable
internal fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    val accent = category.color(colors)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, if (selected) accent else colors.divider, RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.12f) else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(
            category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = AgendaTheme.type.secondary,
            color = if (selected) colors.textPrimary else colors.textSecondary,
        )
    }
}

@Composable
internal fun TextAction(label: String, muted: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = AgendaTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = AgendaTheme.type.bodyStrong,
            color = if (muted) colors.textSecondary else colors.personal,
        )
    }
}
