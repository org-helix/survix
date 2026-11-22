package org.helix.survix.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/** A new random identifier for a survey, a block or a choice. */
fun newId(): String = UUID.randomUUID().toString()

/**
 * A survey: a title, an optional introduction and an ordered list of blocks.
 * Times are milliseconds since the epoch (UTC).
 */
@Serializable
data class Survey(
    val id: String = newId(),
    val title: String = "",
    val description: String = "",
    val blocks: List<Block> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    val questionCount: Int get() = blocks.count { it is Question }

    /** 1-based question number of each question block, by block id. Sections are not numbered. */
    fun questionNumbers(): Map<String, Int> {
        var n = 0
        return blocks.filterIsInstance<Question>().associate { it.id to ++n }
    }
}

/** The kinds of block a survey can hold, in the order they are offered in the editor. */
enum class BlockKind { SECTION, TEXT, CHOICE, YES_NO, RATING, NUMBER, DATE }

/**
 * One module of a survey. Stored in JSON with a `"type"` field naming the kind
 * (see `docs/format.md`).
 */
@Serializable
sealed class Block {
    abstract val id: String
    abstract val kind: BlockKind

    /** A copy with a fresh id (and fresh choice ids), for duplication and conflict-free import. */
    abstract fun withNewIds(): Block

    companion object {
        /** An empty block of the given kind, ready to be filled in the editor. */
        fun blank(kind: BlockKind): Block = when (kind) {
            BlockKind.SECTION -> SectionBlock()
            BlockKind.TEXT -> TextQuestion()
            BlockKind.CHOICE -> ChoiceQuestion(options = listOf(Choice(), Choice()))
            BlockKind.YES_NO -> YesNoQuestion()
            BlockKind.RATING -> RatingQuestion()
            BlockKind.NUMBER -> NumberQuestion()
            BlockKind.DATE -> DateQuestion()
        }
    }
}

/** A block that asks for an answer. */
@Serializable
sealed class Question : Block() {
    abstract val label: String
    abstract val help: String
    abstract val required: Boolean

    abstract fun withCommon(label: String = this.label, help: String = this.help, required: Boolean = this.required): Question
}

/** A heading that splits the survey into parts. It asks for nothing. */
@Serializable
@SerialName("section")
data class SectionBlock(
    override val id: String = newId(),
    val title: String = "",
    val text: String = "",
) : Block() {
    override val kind get() = BlockKind.SECTION
    override fun withNewIds() = copy(id = newId())
}

/** Free text: one line, or a paragraph when [long] is set. */
@Serializable
@SerialName("text")
data class TextQuestion(
    override val id: String = newId(),
    override val label: String = "",
    override val help: String = "",
    override val required: Boolean = false,
    val long: Boolean = false,
    val placeholder: String = "",
) : Question() {
    override val kind get() = BlockKind.TEXT
    override fun withNewIds() = copy(id = newId())
    override fun withCommon(label: String, help: String, required: Boolean) =
        copy(label = label, help = help, required = required)
}

@Serializable
data class Choice(
    val id: String = newId(),
    val label: String = "",
)

/** A list of options: one answer, or several when [multiple] is set. */
@Serializable
@SerialName("choice")
data class ChoiceQuestion(
    override val id: String = newId(),
    override val label: String = "",
    override val help: String = "",
    override val required: Boolean = false,
    val options: List<Choice> = emptyList(),
    val multiple: Boolean = false,
    /** Adds an "Other" option with a free-text field. */
    val allowOther: Boolean = false,
) : Question() {
    override val kind get() = BlockKind.CHOICE
    override fun withNewIds() = copy(id = newId(), options = options.map { it.copy(id = newId()) })
    override fun withCommon(label: String, help: String, required: Boolean) =
        copy(label = label, help = help, required = required)
}

@Serializable
@SerialName("yes_no")
data class YesNoQuestion(
    override val id: String = newId(),
    override val label: String = "",
    override val help: String = "",
    override val required: Boolean = false,
) : Question() {
    override val kind get() = BlockKind.YES_NO
    override fun withNewIds() = copy(id = newId())
    override fun withCommon(label: String, help: String, required: Boolean) =
        copy(label = label, help = help, required = required)
}

/** A score from 1 to [max]. The two labels describe the ends of the scale. */
@Serializable
@SerialName("rating")
data class RatingQuestion(
    override val id: String = newId(),
    override val label: String = "",
    override val help: String = "",
    override val required: Boolean = false,
    val max: Int = 5,
    val minLabel: String = "",
    val maxLabel: String = "",
) : Question() {
    override val kind get() = BlockKind.RATING
    override fun withNewIds() = copy(id = newId())
    override fun withCommon(label: String, help: String, required: Boolean) =
        copy(label = label, help = help, required = required)

    companion object {
        const val MIN_MAX = 2
        const val MAX_MAX = 10
    }
}

/** A number, optionally bounded, with an optional unit shown after the field. */
@Serializable
@SerialName("number")
data class NumberQuestion(
    override val id: String = newId(),
    override val label: String = "",
    override val help: String = "",
    override val required: Boolean = false,
    val min: Double? = null,
    val max: Double? = null,
    val unit: String = "",
) : Question() {
    override val kind get() = BlockKind.NUMBER
    override fun withNewIds() = copy(id = newId())
    override fun withCommon(label: String, help: String, required: Boolean) =
        copy(label = label, help = help, required = required)
}

@Serializable
@SerialName("date")
data class DateQuestion(
    override val id: String = newId(),
    override val label: String = "",
    override val help: String = "",
    override val required: Boolean = false,
) : Question() {
    override val kind get() = BlockKind.DATE
    override fun withNewIds() = copy(id = newId())
    override fun withCommon(label: String, help: String, required: Boolean) =
        copy(label = label, help = help, required = required)
}
