package org.helix.survix.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import org.helix.survix.R
import org.helix.survix.core.Examples
import org.helix.survix.ui.editor.EditorScreen
import org.helix.survix.ui.library.LibraryScreen
import org.helix.survix.ui.preview.PreviewScreen
import org.helix.survix.ui.settings.SettingsScreen
import org.helix.survix.ui.welcome.WelcomeScreen

private object Routes {
    const val WELCOME = "welcome"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val EDITOR = "editor/{id}"
    const val PREVIEW = "preview/{id}"
    fun editor(id: String) = "editor/$id"
    fun preview(id: String) = "preview/$id"
}

@Composable
fun SurvixNavHost(
    snackbar: SnackbarHostState,
    navController: NavHostController = rememberNavController(),
) {
    val app = LocalSurvixApp.current
    val context = LocalContext.current
    // Outlives each screen, so "Undo" can be offered on the library after leaving the editor.
    val scope = rememberCoroutineScope()
    val start = remember { if (app.settings.welcomeDone) Routes.LIBRARY else Routes.WELCOME }

    LaunchedEffect(Unit) {
        app.repository.saveErrors.collect { snackbar.showSnackbar(context.getString(R.string.err_save)) }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onStart = { withExample ->
                app.settings.welcomeDone = true
                if (withExample) {
                    val french = context.resources.configuration.locales[0].language == "fr"
                    app.repository.create(Examples.visitFeedback(french, System.currentTimeMillis()))
                }
                navController.navigate(Routes.LIBRARY) { popUpTo(Routes.WELCOME) { inclusive = true } }
            })
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpen = { navController.navigate(Routes.editor(it)) },
                onPreview = { navController.navigate(Routes.preview(it)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                snackbar = snackbar,
            )
        }
        composable(Routes.EDITOR, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            EditorScreen(
                surveyId = id,
                onBack = { navController.popBackStack() },
                onPreview = { navController.navigate(Routes.preview(id)) },
                onDeleted = { removed ->
                    navController.popBackStack()
                    scope.launch {
                        val title = removed.title.ifBlank { context.getString(R.string.untitled) }
                        val result = snackbar.showSnackbar(
                            message = context.getString(R.string.msg_deleted, title),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) app.repository.restore(removed)
                    }
                },
                snackbar = snackbar,
            )
        }
        composable(Routes.PREVIEW, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            PreviewScreen(
                surveyId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() },
                snackbar = snackbar,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
