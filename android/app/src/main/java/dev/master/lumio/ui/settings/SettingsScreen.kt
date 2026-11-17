package dev.master.lumio.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.master.lumio.R
import dev.master.lumio.data.settings.Appearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(state: SettingsState, appearance: (Appearance) -> Unit, sound: (Boolean) -> Unit, retry: () -> Unit) {
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    val enabled = !state.loading && !state.readError && !state.saving
    Column(Modifier.fillMaxSize().widthIn(max = 800.dp).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.readError) {
            Text(stringResource(R.string.settings_read_error), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = retry) { Text(stringResource(R.string.retry)) }
        }
        if (state.writeError) Text(stringResource(R.string.settings_write_error), color = MaterialTheme.colorScheme.error)
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.headlineMedium)
        Column(Modifier.selectableGroup()) {
            Appearance.entries.forEach { value ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(
                    selected = state.settings.appearance == value, enabled = enabled, role = Role.RadioButton,
                    onClick = { appearance(value) }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state.settings.appearance == value, onClick = null, enabled = enabled,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(when (value) {
                        Appearance.SYSTEM -> R.string.appearance_system
                        Appearance.LIGHT -> R.string.appearance_light
                        Appearance.DARK -> R.string.appearance_dark
                    }))
                }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(state.settings.sound, enabled = enabled,
            role = Role.Switch, onValueChange = sound), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.game_sound), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.game_sound_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.settings.sound, onCheckedChange = null, enabled = enabled)
        }
        HorizontalDivider()
        Text(stringResource(R.string.licenses_credits), style = MaterialTheme.typography.headlineMedium)
        TextButton(colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), onClick = { notice = "fraunces_ofl.txt" }) { Text("Fraunces · SIL OFL 1.1") }
        TextButton(colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), onClick = { notice = "dm_sans_ofl.txt" }) { Text("DM Sans · SIL OFL 1.1") }
        TextButton(colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), onClick = { notice = "lucide.txt" }) { Text("Lucide · ISC / MIT") }
        TextButton(colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), onClick = { notice = "catalog_sources.txt" }) { Text(stringResource(R.string.data_attribution)) }
        Text(stringResource(R.string.data_attribution_hint), style = MaterialTheme.typography.bodyMedium)
        SourceLink("prust/wikipedia-movie-data", "https://github.com/prust/wikipedia-movie-data")
        SourceLink("Wikipedia · CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/")
        SourceLink("CMU Movie Summary Corpus", "https://www.cs.cmu.edu/~ark/personas/")
        SourceLink("9,000+ Movies Dataset · dish am993", "https://www.kaggle.com/datasets/disham993/9000-movies-dataset")
    }
    notice?.let { filename ->
        val context = LocalContext.current
        val text by produceState<String?>(null, filename) {
            value = withContext(Dispatchers.IO) {
                try { context.assets.open("licenses/$filename").bufferedReader().use { it.readText() } }
                catch (_: java.io.IOException) { "" }
            }
        }
        AlertDialog(onDismissRequest = { notice = null }, title = { Text(stringResource(R.string.licenses_credits)) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    if (text == null) CircularProgressIndicator()
                    else Text(text!!.ifEmpty { stringResource(R.string.notice_read_error) }, style = MaterialTheme.typography.bodySmall)
                }
            }, confirmButton = { TextButton(colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), onClick = { notice = null }) { Text(stringResource(R.string.close_notice)) } })
    }
}
@Composable private fun SourceLink(label: String, url: String) {
    val handler = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    TextButton(colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), onClick = { try { handler.openUri(url) } catch (_: IllegalArgumentException) { failed = true } }) { Text(label) }
    if (failed) Text(stringResource(R.string.source_open_error), color = MaterialTheme.colorScheme.error)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 400, heightDp = 1000)
@Composable fun SettingsLightPreview() {
    dev.master.lumio.ui.theme.LumioTheme(false) { Surface { SettingsScreen(SettingsState(loading = false), {}, {}, {}) } }
}
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 400, heightDp = 1000, locale = "de", fontScale = 2f)
@Composable fun SettingsDarkLargePreview() {
    dev.master.lumio.ui.theme.LumioTheme(true) { Surface {
        SettingsScreen(SettingsState(settings = dev.master.lumio.data.settings.AppSettings(Appearance.DARK, false), loading = false), {}, {}, {})
    } }
}
