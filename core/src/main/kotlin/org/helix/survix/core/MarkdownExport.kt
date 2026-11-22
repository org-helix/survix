package org.helix.survix.core

/**
 * Words used in the Markdown export, supplied by the app in the interface language.
 * [scale] receives the top of the scale; [between] receives the two bounds of a number.
 */
data class MarkdownLabels(
    val required: String,
    val oneAnswer: String,
    val severalAnswers: String,
    val other: String,
    val yes: String,
    val no: String,
    val shortAnswer: String,
    val longAnswer: String,
    val date: String,
    val number: String,
    val scale: (max: Int) -> String,
    val between: (min: String, max: String) -> String,
    val atLeast: (min: String) -> String,
    val atMost: (max: String) -> String,
)

/**
 * Renders a survey as Markdown, ready to print, paste into a document or turn into
 * a paper form: numbered questions, empty boxes to tick, lines to write on.
 */
object MarkdownExport {

    fun render(survey: Survey, l: MarkdownLabels): String = buildString {
        appendLine("# ${survey.title.trim().ifEmpty { "—" }}")
        if (survey.description.isNotBlank()) {
            appendLine()
            appendLine(survey.description.trim())
        }
        if (survey.blocks.any { it is Question && it.required }) {
            appendLine()
            appendLine("_* ${l.required}_")
        }
        val numbers = survey.questionNumbers()
        for (block in survey.blocks) {
            appendLine()
            when (block) {
                is SectionBlock -> {
                    appendLine("## ${block.title.trim()}")
                    if (block.text.isNotBlank()) {
                        appendLine()
                        appendLine(block.text.trim())
                    }
                }
                is Question -> question(block, numbers[block.id] ?: 0, l)
            }
        }
    }

    private fun StringBuilder.question(q: Question, number: Int, l: MarkdownLabels) {
        val star = if (q.required) " *" else ""
        appendLine("**$number. ${q.label.trim()}**$star")
        if (q.help.isNotBlank()) {
            appendLine()
            appendLine("_${q.help.trim()}_")
        }
        appendLine()
        when (q) {
            is TextQuestion -> {
                appendLine("${if (q.long) l.longAnswer else l.shortAnswer}: ____________________")
                if (q.long) {
                    appendLine()
                    appendLine("____________________________________________")
                    appendLine()
                    appendLine("____________________________________________")
                }
            }
            is ChoiceQuestion -> {
                appendLine("_${if (q.multiple) l.severalAnswers else l.oneAnswer}_")
                appendLine()
                q.options.forEach { appendLine("- [ ] ${it.label.trim()}") }
                if (q.allowOther) appendLine("- [ ] ${l.other}: ____________")
            }
            is YesNoQuestion -> {
                appendLine("- [ ] ${l.yes}")
                appendLine("- [ ] ${l.no}")
            }
            is RatingQuestion -> {
                appendLine("_${l.scale(q.max)}_")
                appendLine()
                val ends = listOf(q.minLabel.trim(), q.maxLabel.trim())
                val boxes = (1..q.max).joinToString("  ") { "[ ] $it" }
                appendLine(
                    when {
                        ends.all { it.isEmpty() } -> boxes
                        else -> "${ends[0]}  $boxes  ${ends[1]}".trim()
                    },
                )
            }
            is NumberQuestion -> {
                val bounds = when {
                    q.min != null && q.max != null -> l.between(fmt(q.min), fmt(q.max))
                    q.min != null -> l.atLeast(fmt(q.min))
                    q.max != null -> l.atMost(fmt(q.max))
                    else -> null
                }
                if (bounds != null) {
                    appendLine("_${bounds}_")
                    appendLine()
                }
                appendLine("${l.number}: ________ ${q.unit.trim()}".trimEnd())
            }
            is DateQuestion -> appendLine("${l.date}: ____ / ____ / ________")
        }
    }

    /** 3.0 → "3", 2.5 → "2.5". */
    fun fmt(v: Double): String = if (v == Math.floor(v) && !v.isInfinite() && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString()
}
