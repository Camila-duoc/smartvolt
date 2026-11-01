package cl.smartvolt.asistencia.ui.horasextra

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
import cl.smartvolt.asistencia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AprobarHoraExtraScreen(
    onNavUp: () -> Unit,
    vm: HoraExtraViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(Unit) { vm.cargarSolicitudes() }

    val pendientes = ui.solicitudes.filter { it.estado == EstadoHoraExtra.PENDIENTE }
    val aprobadas = ui.solicitudes.filter { it.estado == EstadoHoraExtra.APROBADA }
    val rechazadas = ui.solicitudes.filter { it.estado == EstadoHoraExtra.RECHAZADA }

    val totalPendientesHoras = pendientes.sumOf { it.horas }
    val totalAprobadasHoras = aprobadas.sumOf { it.horas }
    val totalRechazadasHoras = rechazadas.sumOf { it.horas }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aprobar horas extra") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Supervisión de horas extra", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResumenCard("Pendientes", "${pendientes.size}", "${totalPendientesHoras} h", Warn)
                    ResumenCard("Aprobadas", "${aprobadas.size}", "${totalAprobadasHoras} h", Ok)
                    ResumenCard("Rechazadas", "${rechazadas.size}", "${totalRechazadasHoras} h", Bad)
                }
            }
            item {
                Text("Solicitudes por revisar", style = MaterialTheme.typography.titleMedium)
                Text("${pendientes.size} solicitud(es) esperando tu decisión", style = MaterialTheme.typography.bodyMedium)
            }
            if (ui.cargando) {
                item { CircularProgressIndicator() }
            }
            if (pendientes.isEmpty() && !ui.cargando) {
                item { Text("No hay solicitudes pendientes de revisión", style = MaterialTheme.typography.bodyMedium) }
            }
            items(items = pendientes, key = { it.id }) { h ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Usuario: ${h.usuarioId}", style = MaterialTheme.typography.titleMedium)
                            val fechaTxt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(h.fecha))
                            Text(fechaTxt, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Horas: ${h.horas}", style = MaterialTheme.typography.bodyMedium)
                        Text("Motivo: ${h.motivo}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { vm.resolverHoraExtra(h.id, true) },
                                enabled = !ui.cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = Ok)
                            ) { Text("Aprobar") }
                            Button(
                                onClick = { vm.resolverHoraExtra(h.id, false) },
                                enabled = !ui.cargando,
                                colors = ButtonDefaults.buttonColors(containerColor = Bad)
                            ) { Text("Rechazar") }
                        }
                        ui.error?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ResumenCard(titulo: String, cantidad: String, horas: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(cantidad, style = MaterialTheme.typography.headlineSmall, color = color)
            Text(horas, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
