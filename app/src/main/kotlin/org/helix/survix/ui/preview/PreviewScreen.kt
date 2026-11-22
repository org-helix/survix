package org.helix.survix.ui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.helix.survix.R
import org.helix.survix.core.ChoiceQuestion
import org.helix.survix.core.DateQuestion
import org.helix.survix.core.MarkdownExport
import org.helix.survix.core.NumberQuestion
import org.helix.survix.core.Question
import org.helix.survix.core.RatingQuestion
import org.helix.survix.core.SectionBlock
import org.helix.survix.core.Survey
import org.helix.survix.core.TextQuestion
import org.helix.survix.core.YesNoQuestion
import org.helix.survix.ui.LocalSurvixApp
import org.helix.survix.ui.components.HelixCard
import org.helix.survix.ui.components.HelixField
import org.helix.survix.ui.components.PrimaryButton
import org.helix.survix.ui.theme.Helix
import org.helix.survix.ui.theme.Radii
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/** Answers typed in the preview. Kept only while the screen is open. */
private class Answers {
    /** Text, number, yes/no, rating and date answers, as text. */
    val values: SnapshotStateMap<String, String> = mutableStateMapOf()

    /** Ticked option ids of choice questions. [OTHER] stands for the "Other" option. */
    val choices: SnapshotStateMap<String, Set<String>> = mutableStateMapOf()
    val other: SnapshotStateMap<String, String> = mutableStateMapOf()

    fun clear() {
        values.clear()
        choices.clear()
        other.clear()
    }

    companion object {
        const val OTHER = "__other__"
    }
}

/** Why a question's answer is not acceptable yet; null when it is. */
private sealed interface Problem {
    data object Required : Problem
    data object NotANumber : Problem
    data class BelowMin(val min: Double) : Problem
    data class AboveMax(val max: Double) : Problem
}

private fun problemOf(q: Question, a: Answers): Problem? {
    val value = a.values[q.id].orEmpty().trim()
    val answered = when (q) {
        is ChoiceQuestion -> {
            val picked = a.choices[q.id].orEmpty()
            picked.isNotEmpty() && (Answers.OTHER !in picked || a.other[q.id].orEmpty().isNotBlank() || picked.size > 1)
        }
        else -> value.isNotEmpty()
    }
    if (!answered) return if (q.required) Problem.Required else null
    if (q is NumberQuestion) {
        val n = value.replace(',', '.').toDoubleOrNull() ?: return Problem.NotANumber
        if (q.min != null && n < q.min!!) return Problem.BelowMin(q.min!!)
        if (q.max != null && n > q.max!!) return Problem.AboveMax(q.max!!)
    }
    return null
}

@Composable
fun PreviewScreen(surveyId: String, onBack: () -> Unit, snackbar: SnackbarHostState) {
    val repo = LocalSurvixApp.current.repository
    val surveys by repo.surveys.collectAsStateWithLifecycle()
    val survey = surveys?.firstOrNull { it.id == surveyId }
    val c = Helix.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val answers = remember { Answers() }
    var checked by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = c.canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.canvas, scrolledContainerColor = c.canvas),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = { Text(stringResource(R.string.preview_title)) },
                actions = {
                    IconButton(onClick = {
                        answers.clear()
                        checked = false
                    }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = stringResource(R.string.preview_reset))
                    }
                },
            )
        },
    ) { padding ->
        if (survey == null) return@Scaffold
        val questions = survey.blocks.filterIsInstance<Question>()
        val numbers = survey.questionNumbers()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "notice") { Notice() }
            item(key = "intro") { Intro(survey) }
            if (survey.blocks.isEmpty()) {
                item(key = "empty") {
                    Text(stringResource(R.string.preview_empty), color = c.muted, style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(survey.blocks, key = { it.id }) { block ->
                when (block) {
                    is SectionBlock -> SectionView(block)
                    is Question -> QuestionView(
                        q = block,
                        number = numbers[block.id] ?: 0,
                        answers = answers,
                        problem = if (checked) problemOf(block, answers) else null,
                    )
                }
            }
            if (questions.isNotEmpty()) {
                item(key = "check") {
                    PrimaryButton(
                        text = stringResource(R.string.preview_check),
                        onClick = {
                            checked = true
                            val missing = questions.count { problemOf(it, answers) != null }
                            scope.launch {
                                if (missing > 0) {
                                    val first = survey.blocks.indexOfFirst { it is Question && problemOf(it, answers) != null }
                                    if (first >= 0) listState.animateScrollToItem(first + 2)
                                }
                                snackbar.showSnackbar(
                                    if (missing == 0) {
                                        context.getString(R.string.preview_all_good)
                                    } else {
                                        context.resources.getQuantityString(R.plurals.preview_missing, missing, missing)
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Notice() {
    val c = Helix.colors
    Surface(color = c.fill, contentColor = c.text, shape = Radii.lg) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = c.muted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.preview_notice), style = MaterialTheme.typography.bodySmall, color = c.body)
        }
    }
}

@Composable
private fun Intro(survey: Survey) {
    val c = Helix.colors
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            survey.title.ifBlank { stringResource(R.string.untitled) },
            style = MaterialTheme.typography.headlineMedium,
            color = c.text,
            modifier = Modifier.semantics { heading() },
        )
        if (survey.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(survey.description, style = MaterialTheme.typography.bodyLarge, color = c.body)
        }
        if (survey.blocks.any { it is Question && it.required }) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.preview_required_legend), style = MaterialTheme.typography.labelMedium, color = c.muted)
        }
    }
}

@Composable
private fun SectionView(block: SectionBlock) {
    val c = Helix.colors
    Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            block.title,
            style = MaterialTheme.typography.headlineSmall,
            color = c.text,
            modifier = Modifier.semantics { heading() },
        )
        if (block.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(block.text, style = MaterialTheme.typography.bodyLarge, color = c.body)
        }
    }
}

@Composable
private fun QuestionView(q: Question, number: Int, answers: Answers, problem: Problem?) {
    val c = Helix.colors
    HelixCard(Modifier.fillMaxWidth()) {
        Row {
            Text(
                "$number. ${q.label.ifBlank { stringResource(R.string.block_untitled) }}",
                style = MaterialTheme.typography.titleMedium,
                color = c.text,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (q.required) Text(" *", style = MaterialTheme.typography.titleMedium, color = c.accent)
        }
        if (q.help.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(q.help, style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
        Spacer(Modifier.height(12.dp))
        val value = answers.values[q.id].orEmpty()
        fun set(v: String) {
            answers.values[q.id] = v
        }
        when (q) {
            is TextQuestion -> HelixField(
                value = value,
                onValueChange = ::set,
                label = null,
                placeholder = q.placeholder.ifBlank { null },
                singleLine = !q.long,
                minLines = if (q.long) 3 else 1,
            )
            is NumberQuestion -> HelixField(
                value = value,
                onValueChange = ::set,
                label = null,
                suffix = q.unit.ifBlank { null },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            is ChoiceQuestion -> ChoiceInput(q, answers)
            is YesNoQuestion -> Options(
                options = listOf("yes" to stringResource(R.string.yes), "no" to stringResource(R.string.no)),
                selected = value,
                onSelect = ::set,
            )
            is RatingQuestion -> RatingInput(q, value, ::set)
            is DateQuestion -> DateInput(value, ::set)
        }
        if (problem != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                when (problem) {
                    Problem.Required -> stringResource(R.string.preview_required_error)
                    Problem.NotANumber -> stringResource(R.string.preview_number_error)
                    is Problem.BelowMin -> stringResource(R.string.preview_min_error, MarkdownExport.fmt(problem.min))
                    is Problem.AboveMax -> stringResource(R.string.preview_max_error, MarkdownExport.fmt(problem.max))
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.error,
            )
        }
    }
}

@Composable
private fun Options(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val c = Helix.colors
    Column(Modifier.selectableGroup()) {
        options.forEach { (key, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == key, role = Role.RadioButton, onClick = { onSelect(key) })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected == key,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.muted),
                )
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = c.text)
            }
        }
    }
}

@Composable
private fun ChoiceInput(q: ChoiceQuestion, answers: Answers) {
    val c = Helix.colors
    val picked = answers.choices[q.id].orEmpty()
    val all = q.options.map { it.id to it.label } +
        if (q.allowOther) listOf(Answers.OTHER to stringResource(R.string.preview_other)) else emptyList()

    if (!q.multiple) {
        Options(all, selected = picked.firstOrNull().orEmpty(), onSelect = { answers.choices[q.id] = setOf(it) })
    } else {
        Column {
            all.forEach { (key, label) ->
                val on = key in picked
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = on, role = Role.Checkbox, onValueChange = {
                            answers.choices[q.id] = if (it) picked + key else picked - key
                        })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = c.accent, uncheckedColor = c.muted, checkmarkColor = c.onAccent),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = c.text)
                }
            }
        }
    }
    if (Answers.OTHER in picked) {
        Spacer(Modifier.height(4.dp))
        HelixField(
            value = answers.other[q.id].orEmpty(),
            onValueChange = { answers.other[q.id] = it },
            label = null,
            placeholder = stringResource(R.string.preview_other_placeholder),
        )
    }
}

@Composable
private fun RatingInput(q: RatingQuestion, value: String, onSelect: (String) -> Unit) {
    val c = Helix.colors
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (n in 1..q.max) {
            val on = value == n.toString()
            Surface(
                selected = on,
                onClick = { onSelect(n.toString()) },
                shape = Radii.md,
                color = if (on) c.accent else c.fill,
                contentColor = if (on) c.onAccent else c.text,
                modifier = Modifier.size(48.dp),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$n",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    )
                }
            }
        }
    }
    if (q.minLabel.isNotBlank() || q.maxLabel.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("1 · ${q.minLabel}".trimEnd(' ', '·'), style = MaterialTheme.typography.bodySmall, color = c.muted, modifier = Modifier.weight(1f))
            Text("${q.max} · ${q.maxLabel}".trimEnd(' ', '·'), style = MaterialTheme.typography.bodySmall, color = c.muted)
        }
    }
}

@Composable
private fun DateInput(value: String, onPick: (String) -> Unit) {
    val c = Helix.colors
    var open by remember { mutableStateOf(false) }
    val millis = value.toLongOrNull()
    val label = millis?.let {
        DateFormat.getDateInstance(DateFormat.LONG).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(it))
    } ?: stringResource(R.string.preview_pick_date)
    Surface(
        onClick = { open = true },
        shape = Radii.md,
        color = c.field,
        contentColor = c.text,
        border = BorderStroke(1.dp, c.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Event, contentDescription = null, tint = c.muted)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (millis == null) c.muted else c.text)
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        val colors = DatePickerDefaults.colors(
            containerColor = c.surface,
            selectedDayContainerColor = c.accent,
            selectedDayContentColor = c.onAccent,
            todayDateBorderColor = c.accent,
            todayContentColor = c.text,
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(it.toString()) }
                    open = false
                }) { Text(stringResource(R.string.ok), color = c.text) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel), color = c.text) }
            },
            colors = colors,
        ) {
            DatePicker(state = state, colors = colors)
        }
    }
}
