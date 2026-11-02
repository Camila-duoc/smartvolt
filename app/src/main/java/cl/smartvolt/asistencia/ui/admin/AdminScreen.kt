package cl.smartvolt.asistencia.ui.admin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.usecase.ExportarReportePdf
import cl.smartvolt.asistencia.ui.navigation.LocalNavController
import cl.smartvolt.asistencia.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavUp: () -> Unit,
    vm: AdminViewModel = hiltViewModel(),
) {
    val m by vm.metricas.collectAsState()
    val csv by vm.csv.collectAsState()
    val pdf by vm.pdf.collectAsState()
    val context = LocalContext.current
    val nav = LocalNavController.current

    LaunchedEffect(Unit) { vm.cargarMetricas() }
    LaunchedEffect(csv) {
        csv?.let { contenido ->
            val archivo = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val nombre = "marcas_" +
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".csv"
                File(dir, nombre).apply { writeText("\uFEFF$contenido", Charsets.UTF_8) }
            }
            compartirCsv(context, archivo)
            vm.limpiarExportacion()
        }
    }

    LaunchedEffect(pdf) {
        pdf?.let { lineas ->
            val archivo = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val nombre = "resumen_" +
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".pdf"
                File(dir, nombre).apply {
                    outputStream().use { ExportarReportePdf.escribir("Smart-Volt · Resumen administrativo", lineas, it) }
                }
            }
            compartirPdf(context, archivo)
            vm.limpiarPdf()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración") },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vm.cerrarSesion()
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (m.cargando && m.totalMarcas == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text("Marcas", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricaCard("Total", m.totalMarcas.toString(), MaterialTheme.colorScheme.primary)
                    MetricaCard("Válidas", m.marcasValidas.toString(), Ok)
                    MetricaCard("Sospechosas", m.marcasSospechosas.toString(), Warn)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricaCard("Rechazadas", m.marcasRechazadas.toString(), Bad)
                    MetricaCard("Pendientes sync", m.marcasPendientesSync.toString(), MaterialTheme.colorScheme.secondary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Horas extra", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricaCard("Solicitudes", m.horasExtraTotales.toString(), MaterialTheme.colorScheme.primary)
                    MetricaCard("Pendientes", m.horasExtraPendientes.toString(), Warn)
                    MetricaCard("Aprobadas", "${m.horasExtraAprobadasHoras} h", Ok)
                    MetricaCard("Rechazadas", m.horasExtraRechazadas.toString(), Bad)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { vm.exportarCsv() },
                    enabled = !m.exportando,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (m.exportando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generando CSV…")
                    } else {
                        Text("Exportar marcas (CSV)")
                    }
                }

                OutlinedButton(
                    onClick = { vm.exportarPdf() },
                    enabled = !m.exportando,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Exportar resumen (PDF)") }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Marcas no válidas por faena", style = MaterialTheme.typography.titleMedium)
                if (m.noValidasPorFaenaPct.isEmpty()) {
                    Text("Sin datos", style = MaterialTheme.typography.bodySmall)
                }
                m.noValidasPorFaenaPct.entries.sortedBy { it.key }.forEach { (faena, pct) ->
                    FilaResumen(faena, "${"%.1f".format(Locale.US, pct)} %")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Alertas recientes (solo lectura)", style = MaterialTheme.typography.titleMedium)
                if (m.alertas.isEmpty()) {
                    Text("No hay marcas sospechosas ni rechazadas", style = MaterialTheme.typography.bodySmall)
                }
                m.alertas.forEach { FilaAlerta(it) }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Marcas por faena", style = MaterialTheme.typography.titleMedium)
                m.marcasPorFaena.entries.sortedByDescending { it.value }.forEach { (faena, count) ->
                    FilaResumen(faena, count.toString())
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Marcas por usuario", style = MaterialTheme.typography.titleMedium)
                m.marcasPorUsuario.entries.sortedByDescending { it.value }.forEach { (usuarioId, count) ->
                    FilaResumen(usuarioId, count.toString())
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Marcas por estado", style = MaterialTheme.typography.titleMedium)
                m.marcasPorEstado.entries.forEach { (estado, count) ->
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

                m.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RowScope.MetricaCard(titulo: String, valor: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

/** Comparte el CSV generado mediante el selector de apps del sistema. */
private fun compartirCsv(context: Context, archivo: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Marcas SmartVolt")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir marcas"))
}

@Composable
private fun FilaAlerta(a: AlertaResumen) {
    val color = if (a.estado == EstadoMarca.RECHAZADA) Bad else Warn
    val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(a.timestampUtc))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${a.usuarioId} · ${a.faenaId} · ${a.estado.name}", style = MaterialTheme.typography.bodyMedium, color = color)
            Text(fecha, style = MaterialTheme.typography.bodySmall)
            if (a.motivo.isNotBlank()) Text(a.motivo, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Comparte el PDF del resumen mediante el selector de apps del sistema. */
private fun compartirPdf(context: Context, archivo: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Resumen SmartVolt")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir resumen"))
}
