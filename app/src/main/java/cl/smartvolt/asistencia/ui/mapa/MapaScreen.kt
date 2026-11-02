package cl.smartvolt.asistencia.ui.mapa

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.json.JSONObject

/**
 * Pantalla de mapa usando OpenStreetMap (Leaflet.js) dentro de un WebView.
 * El centro y radio se cargan desde el perímetro de la faena del usuario en Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onNavUp: () -> Unit,
    vm: MapaViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }

    val lat = ui.lat
    val lon = ui.lon
    val titulo = if (ui.nombreFaena.isBlank()) "Mapa de la faena" else "Mapa · ${ui.nombreFaena}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    text = ui.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                lat != null && lon != null -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val radio = ui.radioM
                        val nombre = JSONObject.quote(ui.nombreFaena)
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.loadUrl("javascript:initMap($lat, $lon, 16, $radio, $nombre)")
                                }
                            }
                            loadUrl("file:///android_asset/map.html")
                        }
                    }
                )
                else -> Text(
                    text = "Sin ubicación disponible para esta faena",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
        }
    }
}
