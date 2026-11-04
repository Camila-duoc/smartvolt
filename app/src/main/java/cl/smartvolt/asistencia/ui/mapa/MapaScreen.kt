package cl.smartvolt.asistencia.ui.mapa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.Marker
import java.util.Locale

/**
 * Pantalla de mapa de la faena usando el SDK de Mapbox (igual que el ejemplo del profesor).
 * El centro y el radio se cargan desde el perímetro de la faena del usuario en Room.
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
                lat != null && lon != null -> Box(modifier = Modifier.fillMaxSize()) {
                    MapaFaena(lat = lat, lon = lon)
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = ui.nombreFaena.ifBlank { "Faena" },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Perímetro autorizado: ${ui.radioM.toInt()} m",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Centro: %.5f, %.5f",
                                    lat,
                                    lon,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> Text(
                    text = "Sin ubicación disponible para esta faena",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
        }
    }
}

/** Mapa de Mapbox centrado en la faena, con un marcador en su punto central. */
@OptIn(MapboxExperimental::class)
@Composable
private fun MapaFaena(lat: Double, lon: Double) {
    val centro = Point.fromLngLat(lon, lat)
    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = rememberMapViewportState {
            setCameraOptions {
                center(centro)
                zoom(16.0)
                pitch(0.0)
                bearing(0.0)
            }
        },
    ) {
        Marker(point = centro)
    }
}
