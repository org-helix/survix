package org.helix.survix.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.ThumbsUpDown
import androidx.compose.material.icons.rounded.Title
import androidx.compose.ui.graphics.vector.ImageVector
import org.helix.survix.R
import org.helix.survix.core.Block
import org.helix.survix.core.BlockKind
import org.helix.survix.core.ChoiceQuestion
import org.helix.survix.core.FormatError
import org.helix.survix.core.Issue
import org.helix.survix.core.MarkdownLabels
import org.helix.survix.core.Survey
import org.helix.survix.core.SectionBlock
import org.helix.survix.data.ImportProblem

val BlockKind.icon: ImageVector
    get() = when (this) {
        BlockKind.SECTION -> Icons.Rounded.Title
        BlockKind.TEXT -> Icons.Rounded.TextFields
        BlockKind.CHOICE -> Icons.Rounded.RadioButtonChecked
        BlockKind.YES_NO -> Icons.Rounded.ThumbsUpDown
        BlockKind.RATING -> Icons.Rounded.Star
        BlockKind.NUMBER -> Icons.Rounded.Numbers
        BlockKind.DATE -> Icons.Rounded.Event
    }

/** A multiple-choice block shows ticked boxes rather than a radio button. */
val Block.icon: ImageVector
    get() = if (this is ChoiceQuestion && multiple) Icons.Rounded.CheckBox else kind.icon

val BlockKind.title: Int
    @StringRes get() = when (this) {
        BlockKind.SECTION -> R.string.kind_section
        BlockKind.TEXT -> R.string.kind_text
        BlockKind.CHOICE -> R.string.kind_choice
        BlockKind.YES_NO -> R.string.kind_yes_no
        BlockKind.RATING -> R.string.kind_rating
        BlockKind.NUMBER -> R.string.kind_number
        BlockKind.DATE -> R.string.kind_date
    }

val BlockKind.description: Int
    @StringRes get() = when (this) {
        BlockKind.SECTION -> R.string.kind_section_desc
        BlockKind.TEXT -> R.string.kind_text_desc
        BlockKind.CHOICE -> R.string.kind_choice_desc
        BlockKind.YES_NO -> R.string.kind_yes_no_desc
        BlockKind.RATING -> R.string.kind_rating_desc
        BlockKind.NUMBER -> R.string.kind_number_desc
        BlockKind.DATE -> R.string.kind_date_desc
    }

val Issue.message: Int
    @StringRes get() = when (this) {
        Issue.EMPTY_LABEL -> R.string.issue_empty_label
        Issue.NO_OPTIONS -> R.string.issue_no_options
        Issue.EMPTY_OPTION -> R.string.issue_empty_option
        Issue.DUPLICATE_OPTION -> R.string.issue_duplicate_option
        Issue.MIN_ABOVE_MAX -> R.string.issue_min_above_max
    }

val ImportProblem.message: Int
    @StringRes get() = when (this) {
        ImportProblem.Unreadable -> R.string.err_read
        ImportProblem.TooBig -> R.string.err_too_big
        is ImportProblem.Format -> when (error) {
            FormatError.NOT_JSON -> R.string.err_not_json
            FormatError.NOT_SURVIX -> R.string.err_not_survix
            FormatError.NEWER_VERSION -> R.string.err_newer
            FormatError.INVALID -> R.string.err_invalid
            FormatError.EMPTY -> R.string.err_empty
        }
    }

/** "7 questions · 2 sections" */
fun Context.surveyStats(survey: Survey): String {
    val r = resources
    val sections = survey.blocks.count { it is SectionBlock }
    val parts = buildList {
        add(r.getQuantityString(R.plurals.question_count, survey.questionCount, survey.questionCount))
        if (sections > 0) add(r.getQuantityString(R.plurals.section_count, sections, sections))
    }
    return parts.joinToString(" · ")
}

/** "Modifié le 22 nov." (or the time when it was today). */
fun Context.updatedOn(millis: Long): String {
    val flags = if (DateUtils.isToday(millis)) DateUtils.FORMAT_SHOW_TIME else DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    return getString(R.string.updated_on, DateUtils.formatDateTime(this, millis, flags))
}

fun Context.markdownLabels() = MarkdownLabels(
    required = getString(R.string.md_required),
    oneAnswer = getString(R.string.md_one_answer),
    severalAnswers = getString(R.string.md_several_answers),
    other = getString(R.string.preview_other),
    yes = getString(R.string.yes),
    no = getString(R.string.no),
    shortAnswer = getString(R.string.md_answer),
    longAnswer = getString(R.string.md_answer),
    date = getString(R.string.md_date),
    number = getString(R.string.md_number),
    scale = { getString(R.string.md_scale, it) },
    between = { a, b -> getString(R.string.md_between, a, b) },
    atLeast = { getString(R.string.md_at_least, it) },
    atMost = { getString(R.string.md_at_most, it) },
)
