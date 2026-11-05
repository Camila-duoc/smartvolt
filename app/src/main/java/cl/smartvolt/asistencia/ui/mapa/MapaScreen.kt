package cl.smartvolt.asistencia.ui.mapa

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.webkit.WebViewAssetLoader
import cl.smartvolt.asistencia.BuildConfig
import org.json.JSONObject

/**
 * Pantalla de mapa usando Mapbox GL JS dentro de un WebView.
 *
 * IMPLEMENTACIÓN:
 * - Se usa un WebView empotrado con JavaScript habilitado
 * - El HTML map.html contiene ambos estilos: Mapbox CDN y fallback a Leaflet/OSM
 * - Se inyecta la ubicación del usuario vía JavaScript cuando la página carga
 *
 * PROBLEMA RESUELTO:
 * - El WebViewAssetLoader no funciona bien con recursos externos de Mapbox
 * - Ahora se carga map.html desde la URL del asset loader y Mapbox usa CDN directamente
 * - Si no hay internet o Mapbox falla, Leaflet con OpenStreetMap funciona como fallback
 *
 * NOTA: Para uso offline total, necesitarías:
 * 1. Un token de Mapbox válido
 * 2. Descargar y servir los tiles del estilo localmente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onNavUp: () -> Unit,
    vm: MapaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    var recarga by remember { mutableIntStateOf(0) }

    val lat = ui.lat
    val lon = ui.lon
    val titulo = if (ui.nombreFaena.isBlank()) "Mapa de la faena" else "Mapa · ${ui.nombreFaena}"
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { recarga++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar mapa")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = ui.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { vm.cargar() }) { Text("Reintentar") }
                }
                lat != null && lon != null -> key(recarga) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { webCtx ->
                            val radio = ui.radioM
                            val nombre = JSONObject.quote(ui.nombreFaena)

                            // WebViewAssetLoader sirve los assets locales (leaflet) desde un origen seguro HTTPS.
                            // IMPORTANTE: mapbox-gl.js se carga desde CDN en el HTML, así que este loader
                            // solo es necesario para el fallback de Leaflet.
                            val assetLoader = WebViewAssetLoader.Builder()
                                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(webCtx))
                                .build()

                            WebView(webCtx).apply {
                                // Habilitar depuración Web Inspector solo en debug
                                if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

                                // Configuración mínima requerida para Mapbox
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.cacheMode = WebSettings.LOAD_DEFAULT

                                // Configuración adicional para WebView moderno
                                settings.setSupportZoom(false)
                                settings.builtInZoomControls = false

                                // Manejar las peticiones a assets locales (leaflet)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): WebResourceResponse? {
                                        // Interceptar peticiones a assets locales para servir desde el bundle
                                        return request?.let { assetLoader.shouldInterceptRequest(it.url) }
                                    }

                                    override fun onPageFinished(view: WebView?, url: string?) {
                                        super.onPageFinished(view, url)
                                        Log.d("SmartVoltMap", "Página cargada: $url")

                                        // Inyectar la función initMap con los parámetros del usuario
                                        // Esto es seguro porque map.html ya tiene la función definida
                                        view?.evaluateJavascript(
                                            "initMap($lat, $lon, 16, $radio, $nombre);",
                                            null
                                        )
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                                        // Loggear mensajes de consola para debugging
                                        Log.d(
                                            "SmartVoltMap",
                                            "${m.message()} (${m.sourceId()}:${m.lineNumber()})",
                                        )
                                        return true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                       ErrorCode: net.WebViewClient.ErrorRequestErrorCode,
                                        error: net.WebViewClient.WebResourceError?,
                                    ) {
                                        Log.e("SmartVoltMap", "Error en WebView: $errorCode, ${error?.description}")
                                    }
                                }

                                // Cargar la página desde assets
                                loadUrl("https://appassets.androidplatform.net/assets/map.html")
                            }
                        }
                    )
                }
                else -> Text(
                    text = "Sin ubicación disponible para esta faena",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}