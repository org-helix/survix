package org.helix.survix.ui

import androidx.compose.runtime.staticCompositionLocalOf
import org.helix.survix.SurvixApp

/** The application object, for screens that need the repository or the settings. */
val LocalSurvixApp = staticCompositionLocalOf<SurvixApp> { error("SurvixApp not provided") }
