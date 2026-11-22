package org.helix.survix

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.helix.survix.data.ImportResult
import org.helix.survix.data.SurveyFiles
import org.helix.survix.ui.LocalSurvixApp
import org.helix.survix.ui.SurvixNavHost
import org.helix.survix.ui.message
import org.helix.survix.ui.theme.SurvixTheme

/**
 * The only activity. AppCompat is used for its per-app language and night mode;
 * everything on screen is Compose.
 */
class MainActivity : AppCompatActivity() {
    private val snackbar = SnackbarHostState()
    private val app get() = application as SurvixApp

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalSurvixApp provides app) {
                SurvixTheme {
                    SurvixNavHost(snackbar)
                }
            }
        }
        if (savedInstanceState == null) handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    override fun onStop() {
        super.onStop()
        app.repository.flush()
    }

    /** A .survix.json file opened from a file manager, or shared from another app: import it. */
    private fun handleIncoming(intent: Intent?) {
        val uri: Uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> streamOf(intent)
            else -> null
        } ?: return
        lifecycleScope.launch {
            val text = when (val result = SurveyFiles.read(this@MainActivity, uri)) {
                is ImportResult.Ok -> {
                    val n = app.repository.import(result.surveys)
                    resources.getQuantityString(R.plurals.msg_imported, n, n)
                }
                is ImportResult.Failed -> getString(result.problem.message)
            }
            snackbar.showSnackbar(text)
        }
    }

    @Suppress("DEPRECATION")
    private fun streamOf(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
}
