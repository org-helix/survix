package org.helix.survix.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.helix.survix.R
import org.helix.survix.core.MarkdownExport
import org.helix.survix.core.Survey
import org.helix.survix.core.SurvixFormat
import org.helix.survix.core.fileNameFor
import org.helix.survix.data.ImportResult
import org.helix.survix.data.SurveyFiles

/**
 * Export, share and import, wired to the system file picker. Every action reports
 * its outcome through [onMessage] (a string to show in a snackbar).
 */
class Exporter internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val startJson: (fileName: String, ids: List<String>) -> Unit,
    private val startMarkdown: (fileName: String, id: String) -> Unit,
    private val startImport: () -> Unit,
    private val onMessage: (String) -> Unit,
) {
    /** Saves one survey, or several in one file, as `.survix.json`. */
    fun exportJson(surveys: List<Survey>) {
        if (surveys.isEmpty()) return
        val name = if (surveys.size == 1) fileNameFor(surveys.single().title) else "survix-${android.text.format.DateFormat.format("yyyy-MM-dd", java.util.Date())}${SurvixFormat.EXTENSION}"
        startJson(name, surveys.map { it.id })
    }

    fun exportMarkdown(survey: Survey) = startMarkdown(fileNameFor(survey.title, extension = ".md"), survey.id)

    fun share(survey: Survey) {
        scope.launch {
            val title = survey.title.ifBlank { context.getString(R.string.untitled) }
            if (!SurveyFiles.share(context, survey, context.getString(R.string.share_title, title))) {
                onMessage(context.getString(R.string.err_no_share_app))
            }
        }
    }

    fun importFile() = startImport()
}

/**
 * @param surveysById looks surveys up when the file picker returns (the list may have
 *   changed while it was open).
 * @param onImported receives the surveys read from a picked file.
 */
@Composable
fun rememberExporter(
    surveysById: (List<String>) -> List<Survey>,
    onImported: (List<Survey>) -> Unit,
    onMessage: (String) -> Unit,
): Exporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnMessage by rememberUpdatedState(onMessage)
    // Survives the activity being recreated while the system picker is open.
    var pendingIds by rememberSaveable { mutableStateOf(listOf<String>()) }

    fun written(ok: Boolean) = onMessage(context.getString(if (ok) R.string.msg_exported else R.string.err_write))

    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SurvixFormat.MIME)) { uri ->
        val surveys = surveysById(pendingIds)
        if (uri != null && surveys.isNotEmpty()) {
            scope.launch { written(SurveyFiles.write(context, uri, SurvixFormat.encode(surveys))) }
        }
    }
    val markdownLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val survey = surveysById(pendingIds).firstOrNull()
        if (uri != null && survey != null) {
            scope.launch { written(SurveyFiles.write(context, uri, MarkdownExport.render(survey, context.markdownLabels()))) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                when (val result = SurveyFiles.read(context, uri)) {
                    is ImportResult.Ok -> onImported(result.surveys)
                    is ImportResult.Failed -> onMessage(context.getString(result.problem.message))
                }
            }
        }
    }

    return remember(context, scope) {
        Exporter(
            context = context,
            scope = scope,
            startJson = { name, ids ->
                pendingIds = ids
                jsonLauncher.launch(name)
            },
            startMarkdown = { name, id ->
                pendingIds = listOf(id)
                markdownLauncher.launch(name)
            },
            startImport = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            onMessage = { currentOnMessage(it) },
        )
    }
}
