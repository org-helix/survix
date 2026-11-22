package org.helix.survix.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.helix.survix.core.FormatError
import org.helix.survix.core.Survey
import org.helix.survix.core.SurvixFormat
import org.helix.survix.core.SurvixFormatException
import org.helix.survix.core.fileNameFor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** Why an import failed, in terms the UI can explain. */
sealed interface ImportProblem {
    data object Unreadable : ImportProblem
    data object TooBig : ImportProblem
    data class Format(val error: FormatError) : ImportProblem
}

sealed interface ImportResult {
    data class Ok(val surveys: List<Survey>) : ImportResult
    data class Failed(val problem: ImportProblem) : ImportResult
}

/** Reading and writing user-chosen files (Storage Access Framework) and sharing. No permission needed. */
object SurveyFiles {
    /** A survey file is a few kilobytes; anything far larger is not one. */
    private const val MAX_BYTES = 8L * 1024 * 1024

    suspend fun read(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readNBytesCompat(MAX_BYTES + 1)
                if (bytes.size > MAX_BYTES) return@withContext ImportResult.Failed(ImportProblem.TooBig)
                bytes.toString(Charsets.UTF_8)
            } ?: return@withContext ImportResult.Failed(ImportProblem.Unreadable)
        } catch (e: IOException) {
            return@withContext ImportResult.Failed(ImportProblem.Unreadable)
        } catch (e: SecurityException) {
            return@withContext ImportResult.Failed(ImportProblem.Unreadable)
        }
        try {
            ImportResult.Ok(SurvixFormat.decode(text.removePrefix("﻿")))
        } catch (e: SurvixFormatException) {
            ImportResult.Failed(ImportProblem.Format(e.error))
        }
    }

    /** Writes [text] to a document the user picked. Returns false when it could not be written. */
    suspend fun write(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        // "wt" truncates an existing file; a few providers only accept "w".
        for (mode in listOf("wt", "w")) {
            try {
                context.contentResolver.openOutputStream(uri, mode)?.use { it.write(bytes) } ?: continue
                return@withContext true
            } catch (e: FileNotFoundException) {
                continue
            } catch (e: IllegalArgumentException) {
                continue
            } catch (e: IOException) {
                return@withContext false
            } catch (e: SecurityException) {
                return@withContext false
            }
        }
        false
    }

    /** Opens the system share sheet with the survey as a `.survix.json` file. */
    suspend fun share(context: Context, survey: Survey, chooserTitle: String): Boolean {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared")
            dir.deleteRecursively()
            dir.mkdirs()
            File(dir, fileNameFor(survey.title)).apply { writeText(SurvixFormat.encode(survey)) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(SurvixFormat.MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, survey.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (total < limit) {
            val n = read(buffer, 0, minOf(buffer.size.toLong(), limit - total).toInt())
            if (n < 0) break
            out.write(buffer, 0, n)
            total += n
        }
        return out.toByteArray()
    }
}
