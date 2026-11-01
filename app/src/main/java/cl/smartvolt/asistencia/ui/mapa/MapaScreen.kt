package cl.smartvolt.asistencia.ui.mapa

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.androidView
import cl.smartvolt.asistencia.ui.theme.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

/**
 * Pantalla de mapa usando OpenStreetMap (Leaflet.js) dentro de un WebView.
 * [lat] y [lon] son las coordenadas centrales a mostrar (ej. centro de la faena).
 */
@Composable
fun MapaScreen(
    lat: Double,
    lon: Double,
    zoom: Int = 16,
    onNavUp: () -> Unit,
) {
    val context = LocalContext.current

    androidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl("file:///android_asset/map.html") // se crea un asset con el mapa
                // Inyectar coordenadas tras cargar
                post { 
                    loadUrl("javascript:initMap($lat, $lon, $zoom)") 
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    // Nota: para que funcione, agregar un archivo assets/map.html con Leaflet + OpenStreetMap.
    // Se deja como esqueleto porque requiere un asset HTML adicional.
}
