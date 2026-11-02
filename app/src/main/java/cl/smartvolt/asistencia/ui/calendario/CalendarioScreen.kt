package cl.smartvolt.asistencia.ui.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private enum class RangoHistorial(val etiqueta: String) {
    SIETE("7 días"),
    TREINTA("30 días"),
    TODO("Todo"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    onNavUp: () -> Unit,
    vm: CalendarioViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    var rango by remember { mutableStateOf(RangoHistorial.TODO) }

    LaunchedEffect(rango) {
        val ahora = System.currentTimeMillis()
        val desde = when (rango) {
            RangoHistorial.SIETE -> ahora - 7L * 24 * 60 * 60 * 1000
            RangoHistorial.TREINTA -> ahora - 30L * 24 * 60 * 60 * 1000
            RangoHistorial.TODO -> 0L
        }
        vm.cargarHistorial(desde, ahora)
    }

    val titulo = when (ui.rol) {
        Rol.OPERARIO -> "Mi historial"
        Rol.CAPATAZ -> "Historial de la faena"
        Rol.ADMIN -> "Historial general"
        null -> "Historial"
    }
    val totalMarcas = ui.marcasPorFecha.values.sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$totalMarcas marca(s) en ${ui.marcasPorFecha.size} día(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangoHistorial.values().forEach { r ->
                        FilterChip(
                            selected = rango == r,
                            onClick = { rango = r },
                            label = { Text(r.etiqueta) },
                        )
                    }
                }
            }
            if (ui.cargando) {
                item { CircularProgressIndicator() }
            } else if (ui.marcasPorFecha.isEmpty()) {
                item { Text("Sin registros en el período seleccionado", style = MaterialTheme.typography.bodyMedium) }
            } else {
                ui.marcasPorFecha.forEach { (fecha, marcas) ->
                    item(key = fecha) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = fecha,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "${marcas.size} marca(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            marcas.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${m.usuarioId} · ${m.tipo}",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        val horaTxt = SimpleDateFormat("HH:mm", Locale.getDefault())
                                            .format(Date(m.timestampUtc))
                                        Text(
                                            "${m.faenaId} · $horaTxt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (m.motivo.isNotBlank()) {
                                            Text(
                                                m.motivo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Text(
                                        text = m.estado.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (m.estado) {
                                            EstadoMarca.VALIDA -> Ok
                                            EstadoMarca.SOSPECHOSA -> Warn
                                            EstadoMarca.RECHAZADA -> Bad
                                        }
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
            ui.error?.let { error ->
                item {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
