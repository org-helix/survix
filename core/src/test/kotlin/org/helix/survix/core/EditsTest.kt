package org.helix.survix.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EditsTest {
    private val a = TextQuestion(id = "a", label = "A")
    private val b = YesNoQuestion(id = "b", label = "B")
    private val c = SectionBlock(id = "c", title = "C")
    private val survey = Survey(id = "s", blocks = listOf(a, b, c))

    private fun Survey.ids() = blocks.map { it.id }

    @Test
    fun insert() {
        val d = DateQuestion(id = "d")
        assertEquals(listOf("a", "b", "c", "d"), survey.insertBlock(d).ids())
        assertEquals(listOf("a", "d", "b", "c"), survey.insertBlock(d, afterId = "a").ids())
        assertEquals(listOf("a", "b", "c", "d"), survey.insertBlock(d, afterId = "nope").ids())
    }

    @Test
    fun move() {
        assertEquals(listOf("b", "a", "c"), survey.moveBlock("b", -1).ids())
        assertEquals(listOf("a", "c", "b"), survey.moveBlock("b", +1).ids())
        assertSame(survey, survey.moveBlock("a", -1))
        assertSame(survey, survey.moveBlock("c", +1))
        assertSame(survey, survey.moveBlock("zzz", +1))
    }

    @Test
    fun `remove and replace`() {
        assertEquals(listOf("a", "c"), survey.removeBlock("b").ids())
        val renamed = survey.replaceBlock(b.copy(label = "B2"))
        assertEquals("B2", (renamed.blocks[1] as YesNoQuestion).label)
    }

    @Test
    fun duplicateBlock() {
        val choice = ChoiceQuestion(id = "q", label = "Q", options = listOf(Choice("o1", "x")))
        val (s, newId) = Survey(blocks = listOf(choice, a)).duplicateBlock("q")
        assertEquals(3, s.blocks.size)
        val copy = s.blocks[1] as ChoiceQuestion
        assertEquals(newId, copy.id)
        assertNotEquals("q", copy.id)
        assertNotEquals("o1", copy.options.single().id)
        assertEquals("x", copy.options.single().label)
    }

    @Test
    fun `duplicated survey has only new ids`() {
        val d = survey.duplicated(now = 5, title = "Copy")
        assertNotEquals(survey.id, d.id)
        assertTrue(d.ids().none { it in survey.ids() })
        assertEquals("Copy", d.title)
        assertEquals(5, d.createdAt)
    }

    @Test
    fun `import never overwrites`() {
        val incoming = listOf(Survey(id = "s"), Survey(id = "s"), Survey(id = "t", createdAt = 3, updatedAt = 4))
        val ready = prepareImport(incoming, existingIds = setOf("s"), now = 9)
        assertEquals(3, ready.map { it.id }.toSet().size)
        assertTrue("s" !in ready.map { it.id })
        assertEquals("t", ready[2].id)
        assertEquals(9, ready[0].createdAt)
        assertEquals(3, ready[2].createdAt)
    }

    @Test
    fun options() {
        var q = ChoiceQuestion(options = emptyList())
        q = q.addOption("one").addOption("two")
        val (one, two) = q.options
        q = q.renameOption(two.id, "deux").moveOption(two.id, -1)
        assertEquals(listOf("deux", "one"), q.options.map { it.label })
        assertEquals(listOf("one"), q.removeOption(two.id).options.map { it.label })
        assertEquals(q, q.moveOption(one.id, +1))
    }

    @Test
    fun issues() {
        assertEquals(listOf(Issue.EMPTY_LABEL), TextQuestion().issues())
        assertEquals(emptyList(), a.issues())
        val q = ChoiceQuestion(label = "Q", options = listOf(Choice(label = "A"), Choice(label = " a "), Choice(label = "")))
        assertEquals(listOf(Issue.EMPTY_OPTION, Issue.DUPLICATE_OPTION), q.issues())
        assertEquals(listOf(Issue.NO_OPTIONS), ChoiceQuestion(label = "Q").issues())
        assertEquals(emptyList(), ChoiceQuestion(label = "Q", allowOther = true).issues())
        assertEquals(listOf(Issue.MIN_ABOVE_MAX), NumberQuestion(label = "N", min = 5.0, max = 1.0).issues())
        assertEquals(emptyList(), NumberQuestion(label = "N", min = 1.0, max = 1.0).issues())
    }

    @Test
    fun `blank blocks of every kind`() {
        for (kind in BlockKind.entries) assertEquals(kind, Block.blank(kind).kind)
        assertEquals(2, (Block.blank(BlockKind.CHOICE) as ChoiceQuestion).options.size)
    }

    @Test
    fun `question numbers skip sections`() {
        assertEquals(mapOf("a" to 1, "b" to 2), survey.questionNumbers())
        assertEquals(2, survey.questionCount)
    }
}
