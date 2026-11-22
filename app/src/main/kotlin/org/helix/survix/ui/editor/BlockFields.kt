package org.helix.survix.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.helix.survix.R
import org.helix.survix.core.Block
import org.helix.survix.core.BlockKind
import org.helix.survix.core.ChoiceQuestion
import org.helix.survix.core.DateQuestion
import org.helix.survix.core.MarkdownExport
import org.helix.survix.core.NumberQuestion
import org.helix.survix.core.Question
import org.helix.survix.core.RatingQuestion
import org.helix.survix.core.SectionBlock
import org.helix.survix.core.TextQuestion
import org.helix.survix.core.YesNoQuestion
import org.helix.survix.core.addOption
import org.helix.survix.core.removeOption
import org.helix.survix.core.renameOption
import org.helix.survix.ui.components.GhostButton
import org.helix.survix.ui.components.HelixField
import org.helix.survix.ui.components.IconBadge
import org.helix.survix.ui.components.SwitchRow
import org.helix.survix.ui.description
import org.helix.survix.ui.icon
import org.helix.survix.ui.theme.Helix
import org.helix.survix.ui.theme.Radii
import org.helix.survix.ui.title

private val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

/** The fields of an expanded block, by kind. */
@Composable
fun BlockFields(block: Block, onChange: (Block) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (block) {
            is SectionBlock -> {
                HelixField(
                    value = block.title,
                    onValueChange = { onChange(block.copy(title = it)) },
                    label = stringResource(R.string.field_section_title),
                    textStyle = MaterialTheme.typography.titleMedium,
                    singleLine = false,
                    keyboardOptions = sentences,
                )
                HelixField(
                    value = block.text,
                    onValueChange = { onChange(block.copy(text = it)) },
                    label = stringResource(R.string.field_section_text),
                    placeholder = stringResource(R.string.field_section_text_placeholder),
                    singleLine = false,
                    minLines = 2,
                    keyboardOptions = sentences,
                )
            }
            is Question -> {
                HelixField(
                    value = block.label,
                    onValueChange = { onChange(block.withCommon(label = it)) },
                    label = stringResource(R.string.field_label),
                    placeholder = stringResource(R.string.field_label_placeholder),
                    singleLine = false,
                    keyboardOptions = sentences,
                )
                HelixField(
                    value = block.help,
                    onValueChange = { onChange(block.withCommon(help = it)) },
                    label = stringResource(R.string.field_help),
                    placeholder = stringResource(R.string.field_help_placeholder),
                    singleLine = false,
                    keyboardOptions = sentences,
                )
                when (block) {
                    is TextQuestion -> TextFields(block, onChange)
                    is ChoiceQuestion -> ChoiceFields(block, onChange)
                    is RatingQuestion -> RatingFields(block, onChange)
                    is NumberQuestion -> NumberFields(block, onChange)
                    is YesNoQuestion -> Hint(stringResource(R.string.field_yes_no_hint))
                    is DateQuestion -> Hint(stringResource(R.string.field_date_hint))
                }
                SwitchRow(
                    text = stringResource(R.string.field_required),
                    checked = block.required,
                    onCheckedChange = { onChange(block.withCommon(required = it)) },
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Helix.colors.muted)
}

@Composable
private fun TextFields(block: TextQuestion, onChange: (Block) -> Unit) {
    val c = Helix.colors
    val options = listOf(false to R.string.field_text_short, true to R.string.field_text_long)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (long, label) ->
            SegmentedButton(
                selected = block.long == long,
                onClick = { onChange(block.copy(long = long)) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = c.selected,
                    activeContentColor = c.text,
                    activeBorderColor = c.ring,
                    inactiveContainerColor = c.surface,
                    inactiveContentColor = c.text,
                    inactiveBorderColor = c.muted,
                ),
            ) { Text(stringResource(label)) }
        }
    }
    HelixField(
        value = block.placeholder,
        onValueChange = { onChange(block.copy(placeholder = it)) },
        label = stringResource(R.string.field_placeholder),
        placeholder = stringResource(R.string.field_placeholder_placeholder),
    )
}

@Composable
private fun ChoiceFields(block: ChoiceQuestion, onChange: (Block) -> Unit) {
    val c = Helix.colors
    Text(stringResource(R.string.field_options), style = MaterialTheme.typography.labelMedium, color = c.muted)
    block.options.forEachIndexed { i, option ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (block.multiple) Icons.Rounded.CheckBoxOutlineBlank else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            HelixField(
                value = option.label,
                onValueChange = { onChange(block.renameOption(option.id, it)) },
                label = null,
                placeholder = stringResource(R.string.field_option_placeholder, i + 1),
                modifier = Modifier.weight(1f),
                keyboardOptions = sentences.copy(imeAction = ImeAction.Next),
            )
            IconButton(onClick = { onChange(block.removeOption(option.id)) }) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.field_remove_option, i + 1), tint = c.muted)
            }
        }
    }
    if (block.allowOther) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Icon(
                if (block.multiple) Icons.Rounded.CheckBoxOutlineBlank else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.preview_other) + "…", style = MaterialTheme.typography.bodyLarge, color = c.muted)
        }
    }
    GhostButton(
        text = stringResource(R.string.field_add_option),
        onClick = { onChange(block.addOption()) },
        icon = Icons.Rounded.Add,
    )
    SwitchRow(
        text = stringResource(R.string.field_multiple),
        checked = block.multiple,
        onCheckedChange = { onChange(block.copy(multiple = it)) },
    )
    SwitchRow(
        text = stringResource(R.string.field_allow_other),
        checked = block.allowOther,
        onCheckedChange = { onChange(block.copy(allowOther = it)) },
    )
}

@Composable
private fun RatingFields(block: RatingQuestion, onChange: (Block) -> Unit) {
    val c = Helix.colors
    Column {
        Text(stringResource(R.string.field_scale), style = MaterialTheme.typography.labelMedium, color = c.muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange(block.copy(max = block.max - 1)) },
                enabled = block.max > RatingQuestion.MIN_MAX,
            ) { Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.field_scale_less)) }
            Text(
                stringResource(R.string.field_scale_value, block.max),
                style = MaterialTheme.typography.titleSmall,
                color = c.text,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(
                onClick = { onChange(block.copy(max = block.max + 1)) },
                enabled = block.max < RatingQuestion.MAX_MAX,
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.field_scale_more)) }
        }
        // What respondents will see.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (n in 1..block.max) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(c.fill, Radii.md),
                    contentAlignment = Alignment.Center,
                ) { Text("$n", style = MaterialTheme.typography.labelMedium, color = c.text) }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HelixField(
            value = block.minLabel,
            onValueChange = { onChange(block.copy(minLabel = it)) },
            label = stringResource(R.string.field_min_label),
            placeholder = stringResource(R.string.field_min_label_placeholder),
            modifier = Modifier.weight(1f),
            keyboardOptions = sentences,
        )
        HelixField(
            value = block.maxLabel,
            onValueChange = { onChange(block.copy(maxLabel = it)) },
            label = stringResource(R.string.field_max_label),
            placeholder = stringResource(R.string.field_max_label_placeholder),
            modifier = Modifier.weight(1f),
            keyboardOptions = sentences,
        )
    }
}

@Composable
private fun NumberFields(block: NumberQuestion, onChange: (Block) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoundField(
            key = block.id + "min",
            value = block.min,
            onValue = { onChange(block.copy(min = it)) },
            label = stringResource(R.string.field_min),
            modifier = Modifier.weight(1f),
        )
        BoundField(
            key = block.id + "max",
            value = block.max,
            onValue = { onChange(block.copy(max = it)) },
            label = stringResource(R.string.field_max),
            modifier = Modifier.weight(1f),
        )
    }
    HelixField(
        value = block.unit,
        onValueChange = { onChange(block.copy(unit = it)) },
        label = stringResource(R.string.field_unit),
        placeholder = stringResource(R.string.field_unit_placeholder),
    )
}

/** A number typed as text: kept as typed while it is being edited, stored once it parses. */
@Composable
private fun BoundField(key: String, value: Double?, onValue: (Double?) -> Unit, label: String, modifier: Modifier) {
    var text by remember(key) { mutableStateOf(value?.let(MarkdownExport::fmt).orEmpty()) }
    val normalized = text.trim().replace(',', '.')
    val invalid = normalized.isNotEmpty() && normalized.toDoubleOrNull() == null
    HelixField(
        value = text,
        onValueChange = { t ->
            text = t
            val n = t.trim().replace(',', '.')
            when {
                n.isEmpty() -> onValue(null)
                else -> n.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(onValue)
            }
        },
        label = label,
        placeholder = stringResource(R.string.field_bound_placeholder),
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        error = if (invalid) stringResource(R.string.field_not_a_number) else null,
    )
}

/** The list of modules to add, as a bottom sheet. */
@Composable
fun AddBlockSheet(afterOpenBlock: Boolean, onDismiss: () -> Unit, onPick: (BlockKind) -> Unit) {
    val c = Helix.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.text,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                stringResource(R.string.add_block_title),
                style = MaterialTheme.typography.titleLarge,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            if (afterOpenBlock) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.add_block_after), style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Spacer(Modifier.height(12.dp))
            BlockKind.entries.forEach { kind ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onPick(kind) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(kind.icon, size = 40.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(kind.title), style = MaterialTheme.typography.titleSmall, color = c.text)
                        Text(stringResource(kind.description), style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
        }
    }
}
