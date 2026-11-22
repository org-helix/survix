package org.helix.survix.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The `.survix.json` file: readable, versioned JSON holding one or more surveys.
 * The same format is used for the app's own storage and for export, so a stored
 * survey and an exported one are the same file. Documented in `docs/format.md`.
 */
@Serializable
data class SurvixDocument(
    val format: String = SurvixFormat.FORMAT,
    val version: Int = SurvixFormat.VERSION,
    val surveys: List<Survey> = emptyList(),
)

/** Why a file could not be read. */
enum class FormatError {
    /** Not JSON at all (or empty). */
    NOT_JSON,

    /** JSON, but not a Survix file. */
    NOT_SURVIX,

    /** Written by a newer Survix: update the app to open it. */
    NEWER_VERSION,

    /** A Survix file whose content is damaged. */
    INVALID,

    /** A valid file that holds no survey. */
    EMPTY,
}

class SurvixFormatException(val error: FormatError, cause: Throwable? = null) :
    Exception("Unreadable Survix file: $error", cause)

object SurvixFormat {
    const val FORMAT = "survix"
    const val VERSION = 1

    /** File name ending. `.json` keeps the files readable by any other tool. */
    const val EXTENSION = ".survix.json"
    const val MIME = "application/json"

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun encode(surveys: List<Survey>): String =
        json.encodeToString(SurvixDocument.serializer(), SurvixDocument(surveys = surveys))

    fun encode(survey: Survey): String = encode(listOf(survey))

    /**
     * Reads a file. Also accepts a bare survey object without the envelope, so a survey
     * pasted from elsewhere still opens. Every survey returned has been [sanitized].
     *
     * @throws SurvixFormatException when the text is not a readable Survix file.
     */
    fun decode(text: String): List<Survey> {
        val root = try {
            json.parseToJsonElement(text)
        } catch (e: SerializationException) {
            throw SurvixFormatException(FormatError.NOT_JSON, e)
        }
        if (root !is JsonObject) throw SurvixFormatException(FormatError.NOT_SURVIX)

        val format = root["format"]?.jsonPrimitiveOrNull()?.contentOrNull
        val surveys = try {
            when {
                format == FORMAT -> {
                    val version = root["version"]?.jsonPrimitiveOrNull()?.intOrNull
                        ?: throw SurvixFormatException(FormatError.INVALID)
                    if (version > VERSION) throw SurvixFormatException(FormatError.NEWER_VERSION)
                    json.decodeFromJsonElement<SurvixDocument>(root).surveys
                }
                format == null && "blocks" in root -> listOf(json.decodeFromJsonElement<Survey>(root))
                else -> throw SurvixFormatException(FormatError.NOT_SURVIX)
            }
        } catch (e: SurvixFormatException) {
            throw e
        } catch (e: SerializationException) {
            throw SurvixFormatException(FormatError.INVALID, e)
        } catch (e: IllegalArgumentException) {
            throw SurvixFormatException(FormatError.INVALID, e)
        }
        if (surveys.isEmpty()) throw SurvixFormatException(FormatError.EMPTY)
        return surveys.map { it.sanitized() }
    }

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull() =
        runCatching { jsonPrimitive }.getOrNull()
}

/**
 * Repairs what a hand-edited or foreign file may get wrong: blank or duplicated ids,
 * a rating scale out of range, number bounds in the wrong order.
 */
fun Survey.sanitized(): Survey {
    val seen = HashSet<String>()
    fun fresh(id: String): String = if (id.isBlank() || !seen.add(id)) newId().also { seen.add(it) } else id

    val blocks = blocks.map { block ->
        val b = when (block) {
            is RatingQuestion -> block.copy(max = block.max.coerceIn(RatingQuestion.MIN_MAX, RatingQuestion.MAX_MAX))
            is NumberQuestion ->
                if (block.min != null && block.max != null && block.min > block.max) {
                    block.copy(min = block.max, max = block.min)
                } else {
                    block
                }
            is ChoiceQuestion -> {
                val ids = HashSet<String>()
                block.copy(options = block.options.map { if (it.id.isBlank() || !ids.add(it.id)) it.copy(id = newId()) else it })
            }
            else -> block
        }
        val id = fresh(b.id)
        if (id == b.id) b else b.withId(id)
    }
    return copy(
        // The id becomes a file name in the app's storage.
        id = if (SurveyStore.isSafeId(id)) id else newId(),
        blocks = blocks,
        updatedAt = maxOf(updatedAt, createdAt),
    )
}

private fun Block.withId(id: String): Block = when (this) {
    is SectionBlock -> copy(id = id)
    is TextQuestion -> copy(id = id)
    is ChoiceQuestion -> copy(id = id)
    is YesNoQuestion -> copy(id = id)
    is RatingQuestion -> copy(id = id)
    is NumberQuestion -> copy(id = id)
    is DateQuestion -> copy(id = id)
}

/**
 * A file name for [title]: letters and digits kept (accents removed), everything else
 * turned into single dashes, never empty.
 */
fun fileNameFor(title: String, fallback: String = "survey", extension: String = SurvixFormat.EXTENSION): String {
    val ascii = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    val slug = ascii.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(60)
        .trimEnd('-')
    return slug.ifEmpty { fallback } + extension
}
