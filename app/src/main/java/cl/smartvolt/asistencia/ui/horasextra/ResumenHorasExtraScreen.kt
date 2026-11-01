package cl.smartvolt.asistencia.ui.horasextra

import androidx.compose.foundation.layout.*
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

/** Pantalla de resumen simple para el operario: ve sus propias horas extra. */
@Composable
fun ResumenHorasExtraScreen(
    horasSolicitadas: List<HoraExtraEntity>,
    onNavUp: () -> Unit,
) {
    val pendientes = horasSolicitadas.filter { it.estado == EstadoHoraExtra.PENDIENTE }
    val aprobadas = horasSolicitadas.filter { it.estado == EstadoHoraExtra.APROBADA }
    val rechazadas = horasSolicitadas.filter { it.estado == EstadoHoraExtra.RECHAZADA }

    val totalHorasSolicitadas = horasSolicitadas.sumOf { it.horas }
    val totalHorasAprobadas = aprobadas.sumOf { it.horas }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis horas extra") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Text("Resumen de horas extra", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas resumen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Solicitadas", "${horasSolicitadas.size}", "${totalHorasSolicitadas} h", MaterialTheme.colorScheme.primary)
                ResumenCard("Pendientes", "${pendientes.size}", "${pendientes.sumOf { it.horas }} h", Warn)
                ResumenCard("Aprobadas", "${aprobadas.size}", "${totalHorasAprobadas} h", Ok)
                ResumenCard("Rechazadas", "${rechazadas.size}", "${rechazadas.sumOf { it.horas }} h", Bad)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Últimas solicitudes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (horasSolicitadas.isEmpty()) {
                Text("No tienes solicitudes de horas extra registradas.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    items(items = horasSolicitadas, key = { it.id }) { h ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    val fechaTxt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(h.fecha))
                                    Text(fechaTxt, style = MaterialTheme.typography.bodyMedium)
                                    Text("${h.horas} h", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(h.motivo, style = MaterialTheme.typography.bodySmall)
                                val color = when (h.estado) {
                                    EstadoHoraExtra.PENDIENTE -> Warn
                                    EstadoHoraExtra.APROBADA -> Ok
                                    EstadoHoraExtra.RECHAZADA -> Bad
                                }
                                Text(
                                    text = h.estado.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color
                                )
                                h.supervisorId?.let { sup ->
                                    Text("Revisado por: $sup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                )
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
