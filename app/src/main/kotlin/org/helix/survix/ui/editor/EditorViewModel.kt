package org.helix.survix.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import org.helix.survix.core.Block
import org.helix.survix.core.BlockKind
import org.helix.survix.core.Survey
import org.helix.survix.core.duplicateBlock
import org.helix.survix.core.insertBlock
import org.helix.survix.core.moveBlock
import org.helix.survix.core.removeBlock
import org.helix.survix.core.replaceBlock
import org.helix.survix.data.SurveyRepository

/**
 * The survey being edited. It is held in Compose state and changed synchronously, so
 * text fields never lag behind typing; every change is handed to the repository, which
 * saves it to disk a moment later.
 */
class EditorViewModel(private val repo: SurveyRepository, private val surveyId: String) : ViewModel() {

    var survey by mutableStateOf(repo.get(surveyId))
        private set

    /** False until the repository has finished its first load. */
    var loaded by mutableStateOf(survey != null)
        private set

    init {
        if (survey == null) {
            viewModelScope.launch {
                repo.awaitLoaded()
                survey = repo.get(surveyId)
                loaded = true
            }
        }
    }

    fun edit(transform: (Survey) -> Survey) {
        val current = survey ?: return
        val next = transform(current)
        if (next != current) {
            survey = next
            repo.put(next)
        }
    }

    fun setTitle(title: String) = edit { it.copy(title = title) }
    fun setDescription(text: String) = edit { it.copy(description = text) }
    fun updateBlock(block: Block) = edit { it.replaceBlock(block) }
    fun moveBlock(id: String, delta: Int) = edit { it.moveBlock(id, delta) }

    /** Adds an empty block after [afterId] (or at the end) and returns its id. */
    fun addBlock(kind: BlockKind, afterId: String?): String {
        val block = Block.blank(kind)
        edit { it.insertBlock(block, afterId) }
        return block.id
    }

    fun duplicateBlock(id: String): String? {
        var newId: String? = null
        edit {
            val (s, copyId) = it.duplicateBlock(id)
            newId = copyId
            s
        }
        return newId
    }

    /** Removes a block and returns the survey as it was, for undo. */
    fun removeBlock(id: String): Survey? {
        val before = survey ?: return null
        edit { it.removeBlock(id) }
        return before
    }

    fun restore(previous: Survey) = edit { previous }

    /** Deletes the whole survey; the caller offers undo. */
    fun deleteSurvey(): Survey? = repo.delete(surveyId)

    companion object {
        fun factory(repo: SurveyRepository, id: String) = viewModelFactory {
            initializer { EditorViewModel(repo, id) }
        }
    }
}
