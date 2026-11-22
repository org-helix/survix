package org.helix.survix.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SurvixFormatTest {

    private val example = Examples.visitFeedback(french = true, now = 1_700_000_000_000)

    @Test
    fun `round trip keeps every block kind`() {
        val decoded = SurvixFormat.decode(SurvixFormat.encode(example)).single()
        assertEquals(example, decoded)
        assertEquals(BlockKind.entries.toSet(), decoded.blocks.map { it.kind }.toSet())
    }

    @Test
    fun `file has a readable envelope and type names`() {
        val text = SurvixFormat.encode(example)
        assertTrue("\"format\": \"survix\"" in text, text)
        assertTrue("\"version\": 1" in text, text)
        for (type in listOf("section", "text", "choice", "yes_no", "rating", "number", "date")) {
            assertTrue("\"type\": \"$type\"" in text, "missing $type")
        }
    }

    @Test
    fun `several surveys in one file`() {
        val other = Examples.visitFeedback(french = false, now = 1)
        assertEquals(2, SurvixFormat.decode(SurvixFormat.encode(listOf(example, other))).size)
    }

    @Test
    fun `bare survey object is accepted`() {
        val text = """{"id":"abc","title":"Hi","blocks":[{"type":"yes_no","id":"q1","label":"Ok?"}]}"""
        val s = SurvixFormat.decode(text).single()
        assertEquals("Hi", s.title)
        assertEquals(YesNoQuestion(id = "q1", label = "Ok?"), s.blocks.single())
    }

    @Test
    fun `unknown keys from a newer minor writer are ignored`() {
        val text = """{"format":"survix","version":1,"extra":true,"surveys":[{"id":"a","title":"T","colour":"red",
            "blocks":[{"type":"text","id":"b","label":"L","future":1}]}]}"""
        assertEquals("L", (SurvixFormat.decode(text).single().blocks.single() as TextQuestion).label)
    }

    @Test
    fun `errors are classified`() {
        fun err(text: String) = assertFailsWith<SurvixFormatException> { SurvixFormat.decode(text) }.error
        assertEquals(FormatError.NOT_JSON, err(""))
        assertEquals(FormatError.NOT_JSON, err("not json {"))
        assertEquals(FormatError.NOT_SURVIX, err("[1, 2]"))
        assertEquals(FormatError.NOT_SURVIX, err("""{"format":"netlix","version":1}"""))
        assertEquals(FormatError.NEWER_VERSION, err("""{"format":"survix","version":2,"surveys":[]}"""))
        assertEquals(FormatError.INVALID, err("""{"format":"survix","version":1,"surveys":[{"blocks":[{"type":"hologram"}]}]}"""))
        assertEquals(FormatError.INVALID, err("""{"format":"survix","surveys":[]}"""))
        assertEquals(FormatError.EMPTY, err("""{"format":"survix","version":1,"surveys":[]}"""))
    }

    @Test
    fun `sanitize repairs ids and ranges`() {
        val text = """{"format":"survix","version":1,"surveys":[{"id":"../../etc","title":"T","blocks":[
            {"type":"rating","id":"x","label":"a","max":40},
            {"type":"number","id":"x","label":"b","min":10,"max":2},
            {"type":"choice","id":"","label":"c","options":[{"id":"o","label":"1"},{"id":"o","label":"2"}]}
        ]}]}"""
        val s = SurvixFormat.decode(text).single()
        assertTrue(SurveyStore.isSafeId(s.id))
        assertEquals(3, s.blocks.map { it.id }.toSet().size)
        assertTrue(s.blocks.all { it.id.isNotBlank() })
        assertEquals(10, (s.blocks[0] as RatingQuestion).max)
        val n = s.blocks[1] as NumberQuestion
        assertEquals(2.0, n.min)
        assertEquals(10.0, n.max)
        val c = s.blocks[2] as ChoiceQuestion
        assertNotEquals(c.options[0].id, c.options[1].id)
    }

    @Test
    fun `file names are safe slugs`() {
        assertEquals("votre-avis-apres-la-consultation.survix.json", fileNameFor("Votre avis après la consultation"))
        assertEquals("survey.survix.json", fileNameFor("  ¿?  "))
        assertEquals("a-b.md", fileNameFor("A / B", extension = ".md"))
        assertTrue(fileNameFor("x".repeat(500)).length < 80)
    }
}
