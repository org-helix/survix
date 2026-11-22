package org.helix.survix.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownExportTest {
    private val labels = MarkdownLabels(
        required = "Required", oneAnswer = "One answer", severalAnswers = "Several answers", other = "Other",
        yes = "Yes", no = "No", shortAnswer = "Answer", longAnswer = "Answer", date = "Date", number = "Number",
        scale = { "From 1 to $it" }, between = { a, b -> "Between $a and $b" },
        atLeast = { "At least $it" }, atMost = { "At most $it" },
    )

    @Test
    fun `renders every block`() {
        val md = MarkdownExport.render(Examples.visitFeedback(french = false, now = 0), labels)
        assertTrue(md.startsWith("# Your feedback after the visit\n"), md)
        assertTrue("## Your visit" in md)
        assertEquals(1, Regex("Required").findAll(md).count(), "one legend for required questions")
        assertTrue("**1. Date of the visit** *" in md)
        assertTrue("- [ ] On foot" in md)
        assertTrue("- [ ] Other: ____________" in md)
        assertTrue("_At least 0_" in md)
        assertTrue("Number: ________ minutes" in md)
        assertTrue("Poor  [ ] 1  [ ] 2  [ ] 3  [ ] 4  [ ] 5  Excellent" in md)
        assertTrue("- [ ] Yes\n- [ ] No" in md)
        assertTrue("_Several answers_" in md)
        assertTrue("**7. Anything to tell the team?**" in md, md)
    }

    @Test
    fun numbers() {
        assertEquals("3", MarkdownExport.fmt(3.0))
        assertEquals("2.5", MarkdownExport.fmt(2.5))
        assertEquals("-1", MarkdownExport.fmt(-1.0))
    }
}
