package org.helix.survix

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import org.helix.survix.data.Settings
import org.helix.survix.data.SurveyRepository
import java.io.File

/** Holds the app-wide objects. There is no dependency-injection framework: two singletons suffice. */
class SurvixApp : Application() {
    /** Outlives every screen, so a pending save finishes even after the editor closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var repository: SurveyRepository
        private set
    lateinit var settings: Settings
        private set

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        settings.applyTheme()
        repository = SurveyRepository(File(filesDir, "surveys"), appScope)
    }
}
