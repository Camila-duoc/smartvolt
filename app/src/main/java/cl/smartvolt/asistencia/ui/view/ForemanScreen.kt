package cl.smartvolt.asistencia.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import cl.smartvolt.asistencia.ui.navigation.LocalNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.ui.theme.*
import cl.smartvolt.asistencia.viewmodel.ForemanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForemanScreen(vm: ForemanViewModel = hiltViewModel()) {
    val filas by vm.filas.collectAsState()
    val nav = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de presencia") },
                actions = {
                    IconButton(onClick = { nav.navigate("aprobar-horas-extra") }) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = "Horas extra")
                    }
                    IconButton(onClick = { nav.navigate("calendario") }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario")
                    }
                    IconButton(onClick = { nav.navigate("mapa") }) {
                        Icon(Icons.Default.Map, contentDescription = "Mapa")
                    }
                    IconButton(onClick = {
                        vm.cerrarSesion()
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Operarios con marcas: ${filas.size} · Sospechosas por visar: ${filas.count { it.marca.estado == EstadoMarca.SOSPECHOSA && it.visada == null }}") }
            items(filas, key = { it.marca.id }) { f ->
                val color = when (f.marca.estado) { EstadoMarca.VALIDA -> Ok; EstadoMarca.SOSPECHOSA -> Warn; EstadoMarca.RECHAZADA -> Bad }
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.marca.usuarioId, style = MaterialTheme.typography.titleMedium)
                            Text("${f.marca.tipo} · ${f.marca.estado}", color = color)
                            if (f.marca.motivo.isNotBlank()) Text(f.marca.motivo, style = MaterialTheme.typography.bodySmall)
                            f.visada?.let { Text(if (it) "Visada: aprobada" else "Visada: rechazada", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (f.marca.estado == EstadoMarca.SOSPECHOSA && f.visada == null) {
                            TextButton(onClick = { vm.visar(f.marca.id, true) }) { Text("Aprobar") }
                            TextButton(onClick = { vm.visar(f.marca.id, false) }) { Text("Rechazar") }
                        }
                        TextButton(
                            onClick = { nav.navigate("detalle-visacion/${f.marca.id}") },
                        ) { Text("Ver detalle") }
                    }
                }
            }
        }
    }
}
