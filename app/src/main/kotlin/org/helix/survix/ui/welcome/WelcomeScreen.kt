package org.helix.survix.ui.welcome

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.helix.survix.R
import org.helix.survix.ui.LocalSurvixApp
import org.helix.survix.ui.components.Eyebrow
import org.helix.survix.ui.components.HelixMark
import org.helix.survix.ui.components.PixelSunsetImage
import org.helix.survix.ui.components.PrimaryButton
import org.helix.survix.ui.components.SecondaryButton
import org.helix.survix.ui.settings.LanguageChoices
import org.helix.survix.ui.settings.ThemeChoices
import org.helix.survix.ui.theme.Helix

/**
 * First launch: what Survix is, then the language and the look, both applied live.
 * Not shown again once completed; both choices stay in Settings.
 */
@Composable
fun WelcomeScreen(onStart: (withExample: Boolean) -> Unit) {
    val app = LocalSurvixApp.current
    val c = Helix.colors
    val theme by app.settings.theme.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            PixelSunsetImage(Modifier.fillMaxWidth().aspectRatio(16f / 10f))
            Spacer(Modifier.height(28.dp))
            Eyebrow(stringResource(R.string.welcome_eyebrow_1), stringResource(R.string.welcome_eyebrow_2))
            Spacer(Modifier.height(10.dp))
            // Ink line, then the accent line in Helix Red — as on the Helix website.
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.welcome_title))
                    append("\n")
                    withStyle(SpanStyle(color = c.link)) { append(stringResource(R.string.welcome_title_accent)) }
                },
                style = MaterialTheme.typography.displaySmall,
                color = c.text,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.welcome_lead), style = MaterialTheme.typography.bodyLarge, color = c.body)

            Spacer(Modifier.height(28.dp))
            // Bilingual on purpose: it is read before a language is chosen.
            Eyebrow(stringResource(R.string.welcome_language))
            Spacer(Modifier.height(8.dp))
            LanguageChoices(app.settings.language) { app.settings.setLanguage(it) }

            Spacer(Modifier.height(20.dp))
            Eyebrow(stringResource(R.string.welcome_theme))
            Spacer(Modifier.height(8.dp))
            ThemeChoices(theme) { app.settings.setTheme(it) }

            Spacer(Modifier.height(32.dp))
            PrimaryButton(stringResource(R.string.welcome_start), { onStart(false) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            SecondaryButton(stringResource(R.string.welcome_start_example), { onStart(true) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Eyebrow(
                stringResource(R.string.welcome_meta_1),
                stringResource(R.string.welcome_meta_2),
                stringResource(R.string.welcome_meta_3),
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HelixMark(height = 12.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.made_by_helix), style = MaterialTheme.typography.labelMedium, color = c.muted)
            }
        }
    }
}
