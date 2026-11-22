package org.helix.survix.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.helix.survix.core.Survey
import org.helix.survix.core.SurveyStore
import org.helix.survix.core.duplicated
import org.helix.survix.core.prepareImport
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * The surveys of this phone. Edits are applied in memory at once (the UI never waits)
 * and written to disk shortly after; all disk work runs on one background thread, in order.
 */
class SurveyRepository(dir: File, private val scope: CoroutineScope) {
    private val store = SurveyStore(dir)
    private val disk = Executors.newSingleThreadExecutor { Thread(it, "survix-disk") }.asCoroutineDispatcher()

    private val _surveys = MutableStateFlow<List<Survey>?>(null)

    /** Newest first. Null while the first load is running. */
    val surveys: StateFlow<List<Survey>?> = _surveys.asStateFlow()

    private val _unreadableFiles = MutableStateFlow(0)

    /** How many stored files could not be read at startup. */
    val unreadableFiles: StateFlow<Int> = _unreadableFiles.asStateFlow()

    private val _saveErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when a write to disk failed (full storage, for example). */
    val saveErrors: SharedFlow<Unit> = _saveErrors.asSharedFlow()

    /** Pending writes by survey id. Only touched on the main thread. */
    private val pending = HashMap<String, Job>()

    init {
        scope.launch {
            val loaded = withContext(disk) { store.loadAll() }
            _unreadableFiles.value = loaded.unreadable.size
            // Keep anything created while the load was running (an example, an import).
            _surveys.update { early ->
                val created = early.orEmpty()
                (created + loaded.surveys.filterNot { s -> created.any { it.id == s.id } }).sortedByDescending { it.updatedAt }
            }
        }
    }

    suspend fun awaitLoaded(): List<Survey> = surveys.filterNotNull().first()

    fun get(id: String): Survey? = _surveys.value?.firstOrNull { it.id == id }

    /**
     * Stores [survey] in memory now and on disk after [delayMs] (edits made meanwhile
     * replace the pending write). The survey moves to the top of the list.
     */
    fun put(survey: Survey, touch: Boolean = true, delayMs: Long = SAVE_DELAY_MS) {
        val s = if (touch) survey.copy(updatedAt = now()) else survey
        _surveys.update { list ->
            (listOf(s) + list.orEmpty().filterNot { it.id == s.id }).sortedByDescending { it.updatedAt }
        }
        schedule(s.id, delayMs)
    }

    fun create(survey: Survey) {
        val t = now()
        put(survey.copy(createdAt = t, updatedAt = t), touch = false, delayMs = 0)
    }

    fun duplicate(id: String, title: (String) -> String): Survey? {
        val source = get(id) ?: return null
        val copy = source.duplicated(now(), title(source.title))
        put(copy, touch = false, delayMs = 0)
        return copy
    }

    /** Removes the survey and returns it, so the caller can offer to undo with [restore]. */
    fun delete(id: String): Survey? {
        val survey = get(id) ?: return null
        pending.remove(id)?.cancel()
        _surveys.update { list -> list?.filterNot { it.id == id } }
        scope.launch(disk) { store.delete(id) }
        return survey
    }

    fun restore(survey: Survey) = put(survey, touch = false, delayMs = 0)

    /** Adds imported surveys without ever replacing an existing one. Returns how many were added. */
    suspend fun import(incoming: List<Survey>): Int {
        val existing = awaitLoaded().map { it.id }.toSet()
        val ready = prepareImport(incoming, existing, now())
        ready.forEach { put(it, touch = false, delayMs = 0) }
        return ready.size
    }

    /** Writes every pending edit now. Called when the app goes to the background. */
    fun flush() {
        pending.keys.toList().forEach { schedule(it, 0) }
    }

    private fun schedule(id: String, delayMs: Long) {
        pending.remove(id)?.cancel()
        pending[id] = scope.launch {
            if (delayMs > 0) delay(delayMs)
            val survey = get(id) ?: return@launch
            val ok = withContext(disk) {
                try {
                    store.save(survey)
                    true
                } catch (e: IOException) {
                    false
                }
            }
            if (pending[id] == coroutineContext[Job]) pending.remove(id)
            if (!ok) _saveErrors.tryEmit(Unit)
        }
    }

    private fun now() = System.currentTimeMillis()

    companion object {
        const val SAVE_DELAY_MS = 400L
    }
}
