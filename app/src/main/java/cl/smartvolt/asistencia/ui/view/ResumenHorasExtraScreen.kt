package cl.smartvolt.asistencia.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.modelo.EstadoHoraExtra
import cl.smartvolt.asistencia.modelo.HoraExtraEntity
import cl.smartvolt.asistencia.ui.comun.compoments.ResumenCard
import cl.smartvolt.asistencia.ui.comun.theme.*
import java.text.SimpleDateFormat
import java.util.*

/** Contenido del resumen de horas extra del operario (sin Scaffold; lo provee la pantalla contenedora). */
@Composable
fun ResumenHorasExtraContent(
    horasSolicitadas: List<HoraExtraEntity>,
    modifier: Modifier = Modifier,
) {
    val pendientes = horasSolicitadas.filter { it.estado == EstadoHoraExtra.PENDIENTE }
    val aprobadas = horasSolicitadas.filter { it.estado == EstadoHoraExtra.APROBADA }
    val rechazadas = horasSolicitadas.filter { it.estado == EstadoHoraExtra.RECHAZADA }

    val totalHorasSolicitadas = horasSolicitadas.sumOf { it.horas }
    val totalHorasAprobadas = aprobadas.sumOf { it.horas }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Resumen de horas extra", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenCard("Solicitadas", "${horasSolicitadas.size}", "${totalHorasSolicitadas} h", MaterialTheme.colorScheme.primary)
                ResumenCard("Pendientes", "${pendientes.size}", "${pendientes.sumOf { it.horas }} h", Warn)
                ResumenCard("Aprobadas", "${aprobadas.size}", "${totalHorasAprobadas} h", Ok)
                ResumenCard("Rechazadas", "${rechazadas.size}", "${rechazadas.sumOf { it.horas }} h", Bad)
            }
        }
        item {
            Text("Últimas solicitudes", style = MaterialTheme.typography.titleMedium)
        }
        if (horasSolicitadas.isEmpty()) {
            item { Text("No tienes solicitudes de horas extra registradas.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(items = horasSolicitadas, key = { it.id }) { h ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val fechaTxt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(h.fecha))
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
    }
}
