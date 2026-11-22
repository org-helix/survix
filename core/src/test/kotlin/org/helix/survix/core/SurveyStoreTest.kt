package org.helix.survix.core

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurveyStoreTest {
    private val dir: File = Files.createTempDirectory("survix").toFile()
    private val store = SurveyStore(File(dir, "surveys"))

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun `empty when nothing saved`() {
        assertEquals(SurveyStore.Loaded(emptyList(), emptyList()), store.loadAll())
    }

    @Test
    fun `save load overwrite delete`() {
        val s = Examples.visitFeedback(french = false, now = 10)
        store.save(s)
        store.save(s.copy(title = "Second", updatedAt = 20))
        val loaded = store.loadAll().surveys
        assertEquals(1, loaded.size)
        assertEquals("Second", loaded.single().title)
        assertTrue(store.delete(s.id))
        assertFalse(store.delete(s.id))
        assertTrue(store.loadAll().surveys.isEmpty())
        assertTrue(File(dir, "surveys").listFiles().orEmpty().isEmpty(), "no temporary file left")
    }

    @Test
    fun `newest first`() {
        store.save(Survey(title = "old", updatedAt = 1))
        store.save(Survey(title = "new", updatedAt = 3))
        store.save(Survey(title = "mid", updatedAt = 2))
        assertEquals(listOf("new", "mid", "old"), store.loadAll().surveys.map { it.title })
    }

    @Test
    fun `damaged files are reported, not fatal`() {
        store.save(Survey(title = "ok"))
        File(dir, "surveys/broken.survix.json").writeText("{ half")
        File(dir, "surveys/notes.txt").writeText("ignored")
        val loaded = store.loadAll()
        assertEquals(listOf("ok"), loaded.surveys.map { it.title })
        assertEquals(listOf("broken.survix.json"), loaded.unreadable.map { it.name })
    }

    @Test
    fun `stored file is a valid export`() {
        val s = Survey(id = "abc", title = "T")
        store.save(s)
        val text = File(dir, "surveys/abc.survix.json").readText()
        assertEquals(s, SurvixFormat.decode(text).single())
    }

    @Test
    fun `unsafe ids are refused`() {
        assertFailsWith<IllegalArgumentException> { store.save(Survey(id = "../x")) }
        assertFalse(store.delete("../x"))
    }
}
