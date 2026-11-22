package org.helix.survix.core

import java.io.File
import java.io.IOException

/**
 * Surveys on disk: one `<id>.survix.json` file per survey in [dir], in the same format
 * as an export. Writes go to a temporary file first and are then renamed over the old
 * one, so a crash or a full disk never leaves a half-written survey.
 *
 * Not thread-safe: callers serialise access (the app does it on a single dispatcher).
 */
class SurveyStore(private val dir: File) {

    /** What [loadAll] found: the surveys, plus the files that could not be read. */
    data class Loaded(val surveys: List<Survey>, val unreadable: List<File>)

    fun loadAll(): Loaded {
        if (!dir.isDirectory) return Loaded(emptyList(), emptyList())
        val surveys = ArrayList<Survey>()
        val unreadable = ArrayList<File>()
        dir.listFiles { f -> f.isFile && f.name.endsWith(SurvixFormat.EXTENSION) }
            .orEmpty()
            .sortedBy { it.name }
            .forEach { file ->
                try {
                    // A stored file holds exactly one survey, named after its id.
                    val survey = SurvixFormat.decode(file.readText()).first()
                    val expected = fileFor(survey.id)
                    surveys += if (expected.name == file.name) survey else survey.copy(id = file.name.removeSuffix(SurvixFormat.EXTENSION))
                } catch (e: SurvixFormatException) {
                    unreadable += file
                } catch (e: IOException) {
                    unreadable += file
                }
            }
        return Loaded(surveys.sortedByDescending { it.updatedAt }, unreadable)
    }

    @Throws(IOException::class)
    fun save(survey: Survey) {
        require(isSafeId(survey.id)) { "Unsafe survey id: ${survey.id}" }
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create $dir")
        val target = fileFor(survey.id)
        val tmp = File(dir, ".${survey.id}.tmp")
        tmp.writeText(SurvixFormat.encode(survey))
        if (!tmp.renameTo(target)) {
            // Some filesystems refuse to rename over an existing file.
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("Cannot write $target")
            }
        }
    }

    fun delete(id: String): Boolean = isSafeId(id) && fileFor(id).delete()

    private fun fileFor(id: String) = File(dir, id + SurvixFormat.EXTENSION)

    companion object {
        /** Ids become file names: refuse anything that could escape the directory. */
        fun isSafeId(id: String) = id.isNotBlank() && id.length <= 64 && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }
}
