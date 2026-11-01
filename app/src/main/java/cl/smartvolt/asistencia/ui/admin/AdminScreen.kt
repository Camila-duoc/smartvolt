package cl.smartvolt.asistencia.ui.admin

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
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.ui.theme.*

@Composable
fun AdminScreen(
    onNavUp: () -> Unit,
    vm: AdminViewModel = hiltViewModel(),
) {
    val m by vm.metricas.collectAsState()
    vm.cargarMetricas()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (m.cargando && m.totalMarcas == 0) {
                Box(
                    modifier = Modifier.fillMaxSize().wrapContentSize(alignment = androidx.compose.ui.Alignment.Center)
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Tarjetas resumen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricaCard("Total", m.totalMarcas.toString(), MaterialTheme.colorScheme.primary)
                    MetricaCard("Válidas", m.marcasValidas.toString(), Ok)
                    MetricaCard("Sospechosas", m.marcasSospechosas.toString(), Warn)
                    MetricaCard("Rechazadas", m.marcasRechazadas.toString(), Bad)
                    MetricaCard("Pendientes sync", m.marcasPendientesSync.toString(), MaterialTheme.colorScheme.secondary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Marcas por usuario", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    items(items = m.marcasPorUsuario.entries.toList(), key = { it.key }) { (usuarioId, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(usuarioId, style = MaterialTheme.typography.bodyMedium)
                            Text(count.toString(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Marcas por estado", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    items(items = m.marcasPorEstado.entries.toList(), key = { it.key.toString() }) { (estado, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(estado.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (estado) {
                                    EstadoMarca.VALIDA -> Ok
                                    EstadoMarca.SOSPECHOSA -> Warn
                                    EstadoMarca.RECHAZADA -> Bad
                                },
                            )
                        }
                    }
                )

                m.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MetricaCard(titulo: String, valor: String, color: androidx.compose.ui.graphics.Color) {
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
            Text(valor, style = MaterialTheme.typography.headlineSmall, color = color)
        }
    }
}
