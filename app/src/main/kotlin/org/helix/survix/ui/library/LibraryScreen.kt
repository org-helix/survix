package org.helix.survix.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.helix.survix.R
import org.helix.survix.core.Examples
import org.helix.survix.core.Survey
import org.helix.survix.ui.Exporter
import org.helix.survix.ui.LocalSurvixApp
import org.helix.survix.ui.components.Eyebrow
import org.helix.survix.ui.components.GhostButton
import org.helix.survix.ui.components.HelixCard
import org.helix.survix.ui.components.HelixField
import org.helix.survix.ui.components.HelixMark
import org.helix.survix.ui.components.PixelSunsetImage
import org.helix.survix.ui.components.PrimaryButton
import org.helix.survix.ui.components.SecondaryButton
import org.helix.survix.ui.rememberExporter
import org.helix.survix.ui.surveyStats
import org.helix.survix.ui.theme.Helix
import org.helix.survix.ui.updatedOn

@Composable
fun LibraryScreen(
    onOpen: (String) -> Unit,
    onPreview: (String) -> Unit,
    onSettings: () -> Unit,
    snackbar: SnackbarHostState,
) {
    val app = LocalSurvixApp.current
    val repo = app.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val surveys by repo.surveys.collectAsStateWithLifecycle()
    val unreadable by repo.unreadableFiles.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val c = Helix.colors

    fun message(text: String) = scope.launch { snackbar.showSnackbar(text) }

    val exporter = rememberExporter(
        surveysById = { ids -> ids.mapNotNull(repo::get) },
        onImported = { incoming ->
            scope.launch {
                val n = repo.import(incoming)
                message(context.resources.getQuantityString(R.plurals.msg_imported, n, n))
            }
        },
        onMessage = { message(it) },
    )

    fun newSurvey() {
        val survey = Survey()
        repo.create(survey)
        onOpen(survey.id)
    }

    fun addExample() {
        val french = context.resources.configuration.locales[0].language == "fr"
        repo.create(Examples.visitFeedback(french, System.currentTimeMillis()))
        message(context.getString(R.string.msg_example_added))
    }

    fun delete(survey: Survey) {
        val removed = repo.delete(survey.id) ?: return
        scope.launch {
            val title = removed.title.ifBlank { context.getString(R.string.untitled) }
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.msg_deleted, title),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) repo.restore(removed)
        }
    }

    LaunchedEffect(unreadable) {
        if (unreadable > 0) snackbar.showSnackbar(context.getString(R.string.unreadable_files))
    }

    val list = surveys
    Scaffold(
        containerColor = c.canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.canvas, scrolledContainerColor = c.canvas),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HelixMark(height = 16.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = exporter::importFile) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = stringResource(R.string.import_file))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MenuItem(R.string.export_all, Icons.Rounded.SaveAlt, enabled = !list.isNullOrEmpty()) {
                            menuOpen = false
                            exporter.exportJson(list.orEmpty())
                        }
                        MenuItem(R.string.add_example, Icons.Rounded.Add) {
                            menuOpen = false
                            addExample()
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!list.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = ::newSurvey,
                    containerColor = c.accent,
                    contentColor = c.onAccent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_survey)) },
                )
            }
        },
    ) { padding ->
        when {
            list == null -> Unit // first load: a few milliseconds, nothing worth a spinner
            list.isEmpty() -> EmptyLibrary(
                padding = padding,
                onNew = ::newSurvey,
                onImport = exporter::importFile,
                onExample = ::addExample,
            )
            else -> SurveyList(
                surveys = list,
                query = query,
                onQuery = { query = it },
                padding = padding,
                onOpen = onOpen,
                onPreview = onPreview,
                onDuplicate = { s ->
                    repo.duplicate(s.id) { t -> context.getString(R.string.copy_suffix, t.ifBlank { context.getString(R.string.untitled) }) }
                    message(context.getString(R.string.msg_duplicated))
                },
                onDelete = ::delete,
                exporter = exporter,
            )
        }
    }
}

@Composable
private fun SurveyList(
    surveys: List<Survey>,
    query: String,
    onQuery: (String) -> Unit,
    padding: PaddingValues,
    onOpen: (String) -> Unit,
    onPreview: (String) -> Unit,
    onDuplicate: (Survey) -> Unit,
    onDelete: (Survey) -> Unit,
    exporter: Exporter,
) {
    val c = Helix.colors
    val needle = query.trim()
    val shown = if (needle.isEmpty()) surveys else surveys.filter {
        it.title.contains(needle, ignoreCase = true) || it.description.contains(needle, ignoreCase = true)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(Modifier.padding(bottom = 8.dp)) {
                Eyebrow(stringResource(R.string.library_eyebrow_1), stringResource(R.string.library_eyebrow_2))
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.library_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = c.text,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    pluralStringResource(R.plurals.survey_count, surveys.size, surveys.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                )
                if (surveys.size > 3) {
                    Spacer(Modifier.height(16.dp))
                    HelixField(
                        value = query,
                        onValueChange = onQuery,
                        label = null,
                        placeholder = stringResource(R.string.search_placeholder),
                        onSurface = false,
                    )
                }
            }
        }
        items(shown, key = { it.id }) { survey ->
            SurveyCard(
                survey = survey,
                onOpen = { onOpen(survey.id) },
                onPreview = { onPreview(survey.id) },
                onDuplicate = { onDuplicate(survey) },
                onExportJson = { exporter.exportJson(listOf(survey)) },
                onExportMarkdown = { exporter.exportMarkdown(survey) },
                onShare = { exporter.share(survey) },
                onDelete = { onDelete(survey) },
                modifier = Modifier.animateItem(),
            )
        }
        if (shown.isEmpty()) {
            item(key = "no-results") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = c.muted)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.no_results, needle), color = c.muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear_search))
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyCard(
    survey: Survey,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onDuplicate: () -> Unit,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Helix.colors
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val title = survey.title.ifBlank { stringResource(R.string.untitled) }
    HelixCard(onClick = onOpen, modifier = modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 14.dp, end = 4.dp)) {
            Column(Modifier.weight(1f).padding(top = 4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (survey.title.isBlank()) c.muted else c.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (survey.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        survey.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Eyebrow(context.surveyStats(survey), context.updatedOn(survey.updatedAt))
            }
            Column {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.survey_actions, title))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    fun pick(action: () -> Unit): () -> Unit = {
                        menu = false
                        action()
                    }
                    MenuItem(R.string.action_open, Icons.Rounded.Edit, onClick = pick(onOpen))
                    MenuItem(R.string.action_preview, Icons.Rounded.Visibility, onClick = pick(onPreview))
                    MenuItem(R.string.action_duplicate, Icons.Rounded.ContentCopy, onClick = pick(onDuplicate))
                    HorizontalDivider(color = c.hairline)
                    MenuItem(R.string.action_export_json, Icons.Rounded.SaveAlt, onClick = pick(onExportJson))
                    MenuItem(R.string.action_export_markdown, Icons.Rounded.Description, onClick = pick(onExportMarkdown))
                    MenuItem(R.string.action_share, Icons.Rounded.Share, onClick = pick(onShare))
                    HorizontalDivider(color = c.hairline)
                    MenuItem(R.string.action_delete, Icons.Rounded.Delete, danger = true, onClick = pick(onDelete))
                }
            }
        }
    }
}

@Composable
fun MenuItem(text: Int, icon: ImageVector, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    val c = Helix.colors
    val color = if (danger) c.error else c.text
    DropdownMenuItem(
        text = { Text(stringResource(text), color = color) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = color) },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun EmptyLibrary(padding: PaddingValues, onNew: () -> Unit, onImport: () -> Unit, onExample: () -> Unit) {
    val c = Helix.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            Eyebrow(stringResource(R.string.library_eyebrow_1), stringResource(R.string.library_eyebrow_2))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.library_title),
                style = MaterialTheme.typography.displaySmall,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(20.dp))
            PixelSunsetImage(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineSmall, color = c.text)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.empty_text), style = MaterialTheme.typography.bodyLarge, color = c.body)
            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(R.string.new_survey), onNew, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
            Spacer(Modifier.height(12.dp))
            SecondaryButton(stringResource(R.string.import_file), onImport, Modifier.fillMaxWidth(), icon = Icons.Rounded.FileOpen)
            Spacer(Modifier.height(4.dp))
            GhostButton(stringResource(R.string.start_from_example), onExample, Modifier.fillMaxWidth())
            Spacer(Modifier.size(0.dp))
        }
    }
}
