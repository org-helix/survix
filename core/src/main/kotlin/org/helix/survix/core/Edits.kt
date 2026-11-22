package org.helix.survix.core

/*
 * Editing operations. Each returns a new Survey and leaves the input untouched,
 * so the editor can keep the previous value for undo.
 */

/** Inserts [block] after the block with id [afterId], or at the end when it is null or unknown. */
fun Survey.insertBlock(block: Block, afterId: String? = null): Survey {
    val index = blocks.indexOfFirst { it.id == afterId }
    val list = blocks.toMutableList()
    if (index < 0) list.add(block) else list.add(index + 1, block)
    return copy(blocks = list)
}

fun Survey.replaceBlock(block: Block): Survey =
    copy(blocks = blocks.map { if (it.id == block.id) block else it })

fun Survey.removeBlock(id: String): Survey = copy(blocks = blocks.filterNot { it.id == id })

/** Moves a block by [delta] places (−1 up, +1 down). Out-of-range moves change nothing. */
fun Survey.moveBlock(id: String, delta: Int): Survey {
    val from = blocks.indexOfFirst { it.id == id }
    val to = from + delta
    if (from < 0 || to !in blocks.indices) return this
    val list = blocks.toMutableList()
    list.add(to, list.removeAt(from))
    return copy(blocks = list)
}

/** Inserts a copy of the block right after it. Returns the survey and the copy's id. */
fun Survey.duplicateBlock(id: String): Pair<Survey, String?> {
    val block = blocks.firstOrNull { it.id == id } ?: return this to null
    val copy = block.withNewIds()
    return insertBlock(copy, afterId = id) to copy.id
}

/** A full copy of the survey with new ids everywhere, for "Duplicate" and conflicting imports. */
fun Survey.duplicated(now: Long, title: String = this.title): Survey = copy(
    id = newId(),
    title = title,
    blocks = blocks.map { it.withNewIds() },
    createdAt = now,
    updatedAt = now,
)

/**
 * Surveys ready to be added to a library that already holds [existingIds]:
 * a survey whose id is taken (already imported, or imported twice in one file)
 * gets a new id, so importing never overwrites anything.
 */
fun prepareImport(incoming: List<Survey>, existingIds: Set<String>, now: Long): List<Survey> {
    val taken = existingIds.toHashSet()
    return incoming.map { survey ->
        val s = if (survey.id in taken) survey.copy(id = newId()) else survey
        taken.add(s.id)
        s.copy(
            createdAt = if (s.createdAt > 0) s.createdAt else now,
            updatedAt = if (s.updatedAt > 0) s.updatedAt else now,
        )
    }
}

/* ── Choice options ─────────────────────────────────────────────────────── */

fun ChoiceQuestion.addOption(label: String = ""): ChoiceQuestion = copy(options = options + Choice(label = label))

fun ChoiceQuestion.renameOption(id: String, label: String): ChoiceQuestion =
    copy(options = options.map { if (it.id == id) it.copy(label = label) else it })

fun ChoiceQuestion.removeOption(id: String): ChoiceQuestion = copy(options = options.filterNot { it.id == id })

fun ChoiceQuestion.moveOption(id: String, delta: Int): ChoiceQuestion {
    val from = options.indexOfFirst { it.id == id }
    val to = from + delta
    if (from < 0 || to !in options.indices) return this
    val list = options.toMutableList()
    list.add(to, list.removeAt(from))
    return copy(options = list)
}

/* ── Checks shown in the editor ─────────────────────────────────────────── */

/** Something in a block that would confuse respondents. Shown in the editor, never blocking. */
enum class Issue { EMPTY_LABEL, NO_OPTIONS, EMPTY_OPTION, DUPLICATE_OPTION, MIN_ABOVE_MAX }

fun Block.issues(): List<Issue> = buildList {
    when (val b = this@issues) {
        is SectionBlock -> if (b.title.isBlank()) add(Issue.EMPTY_LABEL)
        is Question -> {
            if (b.label.isBlank()) add(Issue.EMPTY_LABEL)
            if (b is ChoiceQuestion) {
                if (b.options.isEmpty() && !b.allowOther) add(Issue.NO_OPTIONS)
                if (b.options.any { it.label.isBlank() }) add(Issue.EMPTY_OPTION)
                val labels = b.options.map { it.label.trim().lowercase() }.filter { it.isNotEmpty() }
                if (labels.size != labels.toSet().size) add(Issue.DUPLICATE_OPTION)
            }
            if (b is NumberQuestion && b.min != null && b.max != null && b.min > b.max) add(Issue.MIN_ABOVE_MAX)
        }
    }
}
