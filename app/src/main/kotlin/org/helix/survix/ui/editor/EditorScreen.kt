package org.helix.survix.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.helix.survix.R
import org.helix.survix.core.Block
import org.helix.survix.core.Question
import org.helix.survix.core.SectionBlock
import org.helix.survix.core.Survey
import org.helix.survix.core.issues
import org.helix.survix.ui.LocalSurvixApp
import org.helix.survix.ui.components.Eyebrow
import org.helix.survix.ui.components.HelixCard
import org.helix.survix.ui.components.HelixField
import org.helix.survix.ui.components.IconBadge
import org.helix.survix.ui.components.PrimaryButton
import org.helix.survix.ui.components.SecondaryButton
import org.helix.survix.ui.icon
import org.helix.survix.ui.library.MenuItem
import org.helix.survix.ui.message
import org.helix.survix.ui.rememberExporter
import org.helix.survix.ui.theme.Helix
import org.helix.survix.ui.title

@Composable
fun EditorScreen(
    surveyId: String,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onDeleted: (Survey) -> Unit,
    snackbar: SnackbarHostState,
) {
    val app = LocalSurvixApp.current
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(app.repository, surveyId))
    val survey = vm.survey
    val c = Helix.colors

    if (survey == null) {
        if (vm.loaded) NotFound(onBack)
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var scrollTo by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    fun message(text: String) = scope.launch { snackbar.showSnackbar(text) }

    val exporter = rememberExporter(
        surveysById = { ids -> ids.mapNotNull(app.repository::get) },
        onImported = {},
        onMessage = { message(it) },
    )

    // Scroll to a block that was just added or duplicated, once it is in the list.
    LaunchedEffect(scrollTo, survey.blocks.size) {
        val id = scrollTo ?: return@LaunchedEffect
        val index = survey.blocks.indexOfFirst { it.id == id }
        if (index >= 0) {
            listState.animateScrollToItem(index + 1) // + the header item
            scrollTo = null
        }
    }

    fun removeBlock(id: String) {
        val before = vm.removeBlock(id) ?: return
        if (expanded == id) expanded = null
        scope.launch {
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.msg_block_deleted),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restore(before)
        }
    }

    val numbers = survey.questionNumbers()

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
                title = { Text(stringResource(R.string.editor_title)) },
                actions = {
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Rounded.Visibility, contentDescription = stringResource(R.string.action_preview))
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        fun pick(action: () -> Unit): () -> Unit = {
                            menuOpen = false
                            action()
                        }
                        MenuItem(R.string.action_export_json, Icons.Rounded.SaveAlt, onClick = pick { exporter.exportJson(listOf(survey)) })
                        MenuItem(R.string.action_export_markdown, Icons.Rounded.Description, onClick = pick { exporter.exportMarkdown(survey) })
                        MenuItem(R.string.action_share, Icons.Rounded.Share, onClick = pick { exporter.share(survey) })
                        MenuItem(R.string.action_duplicate, Icons.Rounded.ContentCopy, onClick = pick {
                            app.repository.duplicate(survey.id) { t ->
                                context.getString(R.string.copy_suffix, t.ifBlank { context.getString(R.string.untitled) })
                            }
                            message(context.getString(R.string.msg_duplicated))
                        })
                        HorizontalDivider(color = c.hairline)
                        MenuItem(R.string.action_delete, Icons.Rounded.Delete, danger = true, onClick = pick {
                            vm.deleteSurvey()?.let(onDeleted)
                        })
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { sheetOpen = true },
                containerColor = c.accent,
                contentColor = c.onAccent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_block)) },
            )
        },
    ) { padding ->
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
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                SurveyHeader(survey, onTitle = vm::setTitle, onDescription = vm::setDescription)
            }
            if (survey.blocks.isEmpty()) {
                item(key = "empty") { EmptyEditor(onAdd = { sheetOpen = true }) }
            }
            itemsIndexed(survey.blocks, key = { _, b -> b.id }) { index, block ->
                BlockCard(
                    block = block,
                    number = numbers[block.id],
                    isFirst = index == 0,
                    isLast = index == survey.blocks.lastIndex,
                    expanded = expanded == block.id,
                    onToggle = { expanded = if (expanded == block.id) null else block.id },
                    onChange = vm::updateBlock,
                    onMove = { delta -> vm.moveBlock(block.id, delta) },
                    onDuplicate = {
                        vm.duplicateBlock(block.id)?.let {
                            expanded = it
                            scrollTo = it
                        }
                    },
                    onDelete = { removeBlock(block.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (survey.blocks.isNotEmpty()) {
                item(key = "add") {
                    SecondaryButton(
                        text = stringResource(R.string.add_block),
                        onClick = { sheetOpen = true },
                        icon = Icons.Rounded.Add,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        AddBlockSheet(
            afterOpenBlock = expanded != null,
            onDismiss = { sheetOpen = false },
            onPick = { kind ->
                sheetOpen = false
                val id = vm.addBlock(kind, afterId = expanded)
                expanded = id
                scrollTo = id
            },
        )
    }
}

@Composable
private fun SurveyHeader(survey: Survey, onTitle: (String) -> Unit, onDescription: (String) -> Unit) {
    val c = Helix.colors
    HelixCard(Modifier.fillMaxWidth()) {
        Eyebrow(stringResource(R.string.editor_survey))
        Spacer(Modifier.height(12.dp))
        HelixField(
            value = survey.title,
            onValueChange = onTitle,
            label = stringResource(R.string.editor_title_label),
            placeholder = stringResource(R.string.editor_title_placeholder),
            singleLine = false,
            textStyle = MaterialTheme.typography.titleLarge,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(12.dp))
        HelixField(
            value = survey.description,
            onValueChange = onDescription,
            label = stringResource(R.string.editor_description_label),
            placeholder = stringResource(R.string.editor_description_placeholder),
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.editor_saved), style = MaterialTheme.typography.labelMedium, color = c.muted)
        }
    }
}

@Composable
private fun EmptyEditor(onAdd: () -> Unit) {
    val c = Helix.colors
    Column(Modifier.padding(vertical = 24.dp, horizontal = 4.dp)) {
        Text(
            stringResource(R.string.editor_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = c.text,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.editor_empty_text), style = MaterialTheme.typography.bodyLarge, color = c.body)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.add_block), onAdd, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
    }
}

@Composable
private fun BlockCard(
    block: Block,
    number: Int?,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onChange: (Block) -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Helix.colors
    val issues = block.issues()
    val requiredText = stringResource(R.string.required_mark)
    val kind = stringResource(block.kind.title)
    val summary = when (block) {
        is SectionBlock -> block.title
        is Question -> block.label
    }.trim()

    HelixCard(modifier = modifier.fillMaxWidth(), selected = expanded, contentPadding = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(if (expanded) R.string.block_collapse else R.string.block_expand),
                    onClick = onToggle,
                )
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(block.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (number != null) {
                    Eyebrow(stringResource(R.string.question_number, number), kind)
                } else {
                    Eyebrow(kind)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary.ifEmpty { stringResource(R.string.block_untitled) },
                    style = if (block is SectionBlock) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                    color = if (summary.isEmpty()) c.muted else c.text,
                    maxLines = if (expanded) 4 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!expanded && issues.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.block_needs_attention), style = MaterialTheme.typography.labelSmall, color = c.error)
                }
            }
            if (block is Question && block.required) {
                Text(
                    "*",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.accent,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clearAndSetSemantics { contentDescription = requiredText },
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.padding(8.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                HorizontalDivider(color = c.hairline)
                Spacer(Modifier.height(16.dp))
                BlockFields(block, onChange)
                if (issues.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    issues.forEach { issue ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(issue.message), style = MaterialTheme.typography.bodySmall, color = c.error)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = c.hairline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onMove(-1) }, enabled = !isFirst) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.block_move_up))
                    }
                    IconButton(onClick = { onMove(+1) }, enabled = !isLast) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.block_move_down))
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.block_duplicate))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.block_delete), tint = c.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotFound(onBack: () -> Unit) {
    val c = Helix.colors
    Scaffold(containerColor = c.canvas) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            Text(stringResource(R.string.survey_not_found), style = MaterialTheme.typography.headlineSmall, color = c.text)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.back), onBack)
        }
    }
}
