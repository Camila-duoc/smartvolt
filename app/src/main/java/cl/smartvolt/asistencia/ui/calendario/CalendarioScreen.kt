package cl.smartvolt.asistencia.ui.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    onNavUp: () -> Unit,
    vm: CalendarioViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(Unit) {
        vm.cargarHistorial(
            System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000),
            System.currentTimeMillis()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario de asistencia") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Historial por día", style = MaterialTheme.typography.headlineSmall)
            }
            if (ui.cargando) {
                item { CircularProgressIndicator() }
            } else if (ui.marcasPorFecha.isEmpty()) {
                item { Text("Sin registros en el período seleccionado", style = MaterialTheme.typography.bodyMedium) }
            } else {
                ui.marcasPorFecha.entries.forEach { (fecha, marcas) ->
                    item(key = fecha) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = fecha,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            marcas.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${m.usuarioId} · ${m.tipo}", style = MaterialTheme.typography.bodyMedium)
                                        val horaTxt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.timestampUtc))
                                        Text(horaTxt, style = MaterialTheme.typography.bodySmall)
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
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
