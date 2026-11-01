package cl.smartvolt.asistencia.ui.horasextra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.data.local.HoraExtraEntity
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
import cl.smartvolt.asistencia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AprobarHoraExtraScreen(
    onNavUp: () -> Unit,
    vm: HoraExtraViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()

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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Text("Supervisión de horas extra", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas resumen para el capataz (nivel de detalle superior al del operario)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Pendientes", "${pendientes.size}", "${totalPendientesHoras} h", Warn)
                ResumenCard("Aprobadas", "${aprobadas.size}", "${totalAprobadasHoras} h", Ok)
                ResumenCard("Rechazadas", "${rechazadas.size}", "${totalRechazadasHoras} h", Bad)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Solicitudes por revisar", style = MaterialTheme.typography.titleMedium)
            Text("${pendientes.size} solicitud(es) esperando tu decisión", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (ui.cargando) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                items(items = pendientes, key = { it.id }) { h ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                                Text("Usuario: ${h.usuarioId}", style = MaterialTheme.typography.titleMedium)
                                val fechaTxt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(h.fecha))
                                Text(fechaTxt, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("Horas: ${h.horas}", style = MaterialTheme.typography.bodyMedium)
                            Text("Motivo: ${h.motivo}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { vm.resolver(h.id, true) },
                                    enabled = !ui.cargando,
                                    colors = ButtonDefaults.buttonColors(containerColor = Ok)
                                ) { Text("Aprobar") }
                                Button(
                                    onClick = { vm.resolver(h.id, false) },
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
            )

            if (pendientes.isEmpty()) {
                Text("No hay solicitudes pendientes de revisión", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ResumenCard(titulo: String, cantidad: String, horas: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(cantidad, style = MaterialTheme.typography.headlineSmall, color = color)
            Text(horas, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
