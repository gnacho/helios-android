package club.cloudless.helios.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import club.cloudless.helios.config.ConfigStore
import club.cloudless.helios.data.ApiResult
import club.cloudless.helios.data.HeliosApi
import club.cloudless.helios.widget.WidgetRefreshWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Server URL and API token. The token is created in Helios Settings (API
 * tokens) and only needs the read scope for the widget.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cfg = ConfigStore.create(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        initialUrl = cfg.baseUrl,
                        initialToken = cfg.token,
                        onSave = { url, token ->
                            cfg.baseUrl = url
                            cfg.token = token
                            WidgetRefreshWorker.schedule(this)
                            WidgetRefreshWorker.refreshNow(this)
                        },
                        onOpenApp = {
                            startActivity(
                                Intent(this, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            finish()
                        },
                    )
                }
            }
        }
    }
}

private sealed interface Probe {
    data object Idle : Probe
    data object Running : Probe
    data class Ok(val detail: String) : Probe
    data class Fail(val message: String) : Probe
}

@Composable
private fun SettingsScreen(
    initialUrl: String,
    initialToken: String,
    onSave: (String, String) -> Unit,
    onOpenApp: () -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl.ifEmpty { "https://panelsolar.duckdns.org" }) }
    var token by remember { mutableStateOf(initialToken) }
    var probe by remember { mutableStateOf<Probe>(Probe.Idle) }
    val scope = rememberCoroutineScope()

    fun normalized(): String = HeliosApi.normalizeBaseUrl(url)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Helios", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Dirección del servidor y token de API. El token se crea en Helios, en Ajustes, sección Tokens de API, " +
                "y le basta el permiso de solo lectura.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Servidor") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        )

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    probe = Probe.Running
                    scope.launch {
                        val api = HeliosApi(normalized(), token.trim())
                        val result = withContext(Dispatchers.IO) { api.live() }
                        probe = when (result) {
                            is ApiResult.Ok -> Probe.Ok("Conectado. Batería al %.0f %%".format(result.value.soc))
                            is ApiResult.Error -> Probe.Fail(result.message)
                        }
                    }
                },
                enabled = probe != Probe.Running && url.isNotBlank() && token.isNotBlank(),
            ) { Text("Probar") }

            Button(
                onClick = {
                    onSave(normalized(), token.trim())
                    onOpenApp()
                },
                enabled = url.isNotBlank() && token.isNotBlank(),
            ) { Text("Guardar y abrir") }
        }

        if (probe is Probe.Running) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Probando…")
            }
        }

        when (val p = probe) {
            is Probe.Ok -> Text(p.detail, style = MaterialTheme.typography.bodyMedium)
            is Probe.Fail -> Text(p.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            else -> {}
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onOpenApp) { Text("Abrir Helios sin guardar") }
    }
}
