package cl.smartvolt.asistencia.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import cl.smartvolt.asistencia.viewmodel.MapaUi
import cl.smartvolt.asistencia.viewmodel.MapaViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.Marker
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import java.util.Locale

/**
 * Pantalla de mapa de la faena usando el SDK de Mapbox (igual que el ejemplo del profesor).
 *
 * El administrador puede mover el marcador y guardar la ubicación. El punto guardado
 * queda en Room y lo ven los demás roles al abrir el mapa; el capataz y los operarios
 * solo pueden consultarlo.
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
                lat == null || lon == null -> Text(
                    text = ui.error ?: "Sin ubicación disponible para esta faena",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> Box(modifier = Modifier.fillMaxSize()) {
                    MapaFaena(
                        lat = lat,
                        lon = lon,
                        editable = ui.esAdmin,
                        onSeleccionar = vm::seleccionar,
                    )
                    TarjetaMapa(
                        ui = ui,
                        lat = lat,
                        lon = lon,
                        onGuardar = { vm.guardar() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

/** Mapa de Mapbox centrado en la ubicación actual, con un marcador en el punto mostrado. */
@OptIn(MapboxExperimental::class)
@Composable
private fun MapaFaena(
    lat: Double,
    lon: Double,
    editable: Boolean,
    onSeleccionar: (Double, Double) -> Unit,
) {
    val centro = Point.fromLngLat(lon, lat)
    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(centro)
            zoom(16.0)
            pitch(0.0)
            bearing(0.0)
        }
    }
    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = viewport,
    ) {
        Marker(point = centro)
        if (editable) {
            // Solo administración: un toque mueve el marcador para elegir otro punto.
            MapEffect(Unit) { mapView ->
                mapView.gestures.addOnMapClickListener { point ->
                    onSeleccionar(point.latitude(), point.longitude())
                    true
                }
            }
        }
    }
}

/** Tarjeta inferior con los datos del punto y el control de guardado solo para ADMIN. */
@Composable
private fun TarjetaMapa(
    ui: MapaUi,
    lat: Double,
    lon: Double,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
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

            val etiqueta = when {
                ui.seleccionPendiente -> "Nueva ubicación (sin guardar)"
                ui.ubicacionCompartida -> "Ubicación compartida por administración"
                else -> "Centro de la faena"
            }
            Text(
                text = String.format(Locale.US, "%s: %.5f, %.5f", etiqueta, lat, lon),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (ui.esAdmin) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Toca el mapa para mover el marcador. Al guardar, todos los " +
                        "usuarios verán este mismo punto.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (ui.guardado) {
                    Text(
                        text = "Ubicación guardada. Operarios y capataces ya la verán al abrir el mapa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onGuardar,
                    enabled = !ui.guardando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (ui.guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando…")
                    } else {
                        Text("Guardar ubicación para todos")
                    }
                }
            } else if (ui.ubicacionCompartida) {
                Text(
                    text = "Esta ubicación fue seleccionada por administración.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ui.error?.let { error ->
                Row {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
