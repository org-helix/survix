package org.helix.survix.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.helix.survix.BuildConfig
import org.helix.survix.R
import org.helix.survix.data.Language
import org.helix.survix.data.ThemeMode
import org.helix.survix.ui.LocalSurvixApp
import org.helix.survix.ui.components.ChoiceTile
import org.helix.survix.ui.components.GhostButton
import org.helix.survix.ui.components.HelixCard
import org.helix.survix.ui.components.HelixMark
import org.helix.survix.ui.theme.Helix

const val SOURCE_URL = "https://github.com/hunixcode/survix"

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalSurvixApp.current
    val c = Helix.colors
    val theme by app.settings.theme.collectAsStateWithLifecycle()
    val surveys by app.repository.surveys.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        containerColor = c.canvas,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.canvas, scrolledContainerColor = c.canvas),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = { Text(stringResource(R.string.settings)) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Section(stringResource(R.string.settings_appearance)) {
                ThemeChoices(theme) { app.settings.setTheme(it) }
            }
            Section(stringResource(R.string.settings_language)) {
                LanguageChoices(app.settings.language) { app.settings.setLanguage(it) }
            }
            Section(stringResource(R.string.settings_data)) {
                HelixCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_data_text), style = MaterialTheme.typography.bodyMedium, color = c.body)
                    val n = surveys?.size ?: 0
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_data_count, pluralStringResource(R.plurals.survey_count, n, n)),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.muted,
                    )
                }
                Spacer(Modifier.height(12.dp))
                HelixCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_format), style = MaterialTheme.typography.titleSmall, color = c.text)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.settings_format_text), style = MaterialTheme.typography.bodyMedium, color = c.body)
                }
            }
            Section(stringResource(R.string.settings_about)) {
                HelixCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        HelixMark(height = 14.dp)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = c.text)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = c.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_about_text), style = MaterialTheme.typography.bodyMedium, color = c.body)
                    Spacer(Modifier.height(4.dp))
                    val sourceLabel = stringResource(R.string.settings_source)
                    val described = sourceLabel + " " + stringResource(R.string.opens_in_browser)
                    GhostButton(
                        text = sourceLabel,
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
                            } catch (e: ActivityNotFoundException) {
                                // No browser: nothing to open.
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = described },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = Helix.colors.text,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun ThemeChoices(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, R.string.theme_system, Icons.Rounded.BrightnessAuto),
        Triple(ThemeMode.LIGHT, R.string.theme_light, Icons.Rounded.LightMode),
        Triple(ThemeMode.DARK, R.string.theme_dark, Icons.Rounded.DarkMode),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (mode, label, icon) ->
            ChoiceTile(
                text = stringResource(label),
                icon = icon,
                selected = selected == mode,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun LanguageChoices(selected: Language, onSelect: (Language) -> Unit) {
    val options = listOf(
        Language.SYSTEM to R.string.language_system,
        Language.FRENCH to R.string.language_fr,
        Language.ENGLISH to R.string.language_en,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (lang, label) ->
            ChoiceTile(
                text = stringResource(label),
                selected = selected == lang,
                onClick = { onSelect(lang) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
