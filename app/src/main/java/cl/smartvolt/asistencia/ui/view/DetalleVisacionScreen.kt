package cl.smartvolt.asistencia.ui.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.ui.comun.theme.*
import cl.smartvolt.asistencia.viewmodel.DetalleVisacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleVisacionScreen(
    marcaId: String,
    onNavUp: () -> Unit,
    vm: DetalleVisacionViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(marcaId) { vm.cargarDetalle(marcaId) }

    val marca = ui.marca

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de visación") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        if (ui.cargando && marca == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (marca == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontró la marca", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Usuario: ${marca.usuarioId}", style = MaterialTheme.typography.titleMedium)
                Text("Faena: ${marca.faenaId}", style = MaterialTheme.typography.bodyMedium)
                Text("Tipo: ${marca.tipo}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                val estadoColor = when (marca.estado) {
                    EstadoMarca.VALIDA -> Ok
                    EstadoMarca.SOSPECHOSA -> Warn
                    EstadoMarca.RECHAZADA -> Bad
                }
                Text(
                    text = "Estado: ${marca.estado}",
                    style = MaterialTheme.typography.titleLarge,
                    color = estadoColor,
                )
                if (marca.motivo.isNotBlank()) {
                    Text("Motivo: ${marca.motivo}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("Ubicación", style = MaterialTheme.typography.titleMedium)
                val ubicacionTxt = if (marca.lat != null && marca.lon != null) {
                    "${marca.lat}, ${marca.lon}"
                } else {
                    "Sin ubicación"
                }
                Text(ubicacionTxt, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Visación", style = MaterialTheme.typography.titleMedium)
                if (ui.visada == null) {
                    Text("Pendiente de visación", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = if (ui.visada == true) "Visada: aprobada" else "Visada: rechazada",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ui.visada == true) Ok else Bad,
                    )
                }
                ui.visacionTimestamp?.let { ts ->
                    Text("Fecha: ${java.util.Date(ts)}", style = MaterialTheme.typography.bodySmall)
                }

                ui.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
