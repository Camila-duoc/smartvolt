package cl.smartvolt.asistencia.ui.view.admin

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.domain.usecase.ExportarReportePdf
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.ui.navigation.LocalNavController
import cl.smartvolt.asistencia.ui.theme.Bad
import cl.smartvolt.asistencia.ui.theme.Ok
import cl.smartvolt.asistencia.ui.theme.Warn
import cl.smartvolt.asistencia.viewmodel.AdminViewModel
import cl.smartvolt.asistencia.viewmodel.AlertaResumen
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

    var tabSeleccionado by rememberSaveable { mutableIntStateOf(0) }

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel Administrador",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auditoría y métricas · Smart-Volt",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate("mapa") }) {
                        Icon(Icons.Default.Map, contentDescription = "Ubicación en el mapa")
                    }
                    IconButton(
                        onClick = {
                            vm.cerrarSesion()
                            nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tabSeleccionado == 0,
                    onClick = { tabSeleccionado = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Resumen") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = tabSeleccionado == 1,
                    onClick = { tabSeleccionado = 1 },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                    label = { Text("Alertas (${m.alertas.size})") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = tabSeleccionado == 2,
                    onClick = { tabSeleccionado = 2 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Desglose") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (m.cargando && m.totalMarcas == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                when (tabSeleccionado) {
                    // --- PESTAÑA 1: RESUMEN GENERAL Y EXPORTACIÓN ---
                    0 -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Métricas de Asistencia",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricaCard("Total", m.totalMarcas, MaterialTheme.colorScheme.primary)
                                    MetricaCard("Válidas", m.marcasValidas, Ok)
                                    MetricaCard("Sospechosas", m.marcasSospechosas, Warn)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricaCard("Rechazadas", m.marcasRechazadas, Bad)
                                    MetricaCard("Pendientes sync", m.marcasPendientesSync, MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gestión de Horas Extra",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricaCard("Solicitudes", m.horasExtraTotales, MaterialTheme.colorScheme.primary)
                                MetricaCard("Pendientes", m.horasExtraPendientes, Warn)
                                MetricaCard("Aprobadas", "${m.horasExtraAprobadasHoras} h", Ok)
                                MetricaCard("Rechazadas", m.horasExtraRechazadas, Bad)
                            }
                        }

                        // Tarjeta de Acciones y Reportes (Estética AdminHomeView)
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Herramientas y Reportes",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    OutlinedButton(
                                        onClick = { nav.navigate("mapa") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Seleccionar ubicación en el mapa", fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { vm.exportarCsv() },
                                        enabled = !m.exportando,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (m.exportando) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generando archivo…")
                                        } else {
                                            Icon(Icons.Default.TableChart, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Exportar marcas (CSV)", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { vm.exportarPdf() },
                                        enabled = !m.exportando,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Exportar resumen (PDF)", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        m.error?.let { error ->
                            item {
                                TarjetaErrorAdmin(error)
                            }
                        }
                    }

                    // --- PESTAÑA 2: ALERTAS Y AUDITORÍA DE HASHES ---
                    1 -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Integridad de cadena de hashes
                        m.cadenaIntegra?.let { integra ->
                            item {
                                val color = if (integra) Ok else Bad
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = color
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Registro inalterable",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (integra) {
                                                    "Cadena íntegra (${m.marcasVerificadas} marcas verificadas)"
                                                } else {
                                                    "Cadena alterada: ${m.marcasAlteradas} de ${m.marcasVerificadas} marcas"
                                                },
                                                fontSize = 13.sp,
                                                color = color,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (m.marcasNoVerificables > 0) {
                                                Text(
                                                    text = "${m.marcasNoVerificables} marca(s) semilla sin hash",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column {
                                Text(
                                    text = "Alertas recientes (solo lectura)",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (m.alertas.isNotEmpty()) {
                                    Text(
                                        text = "Toca una alerta para inspeccionar el detalle",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        if (m.alertas.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "No hay marcas sospechosas ni rechazadas.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(m.alertas, key = { it.marcaId }) { alerta ->
                                FilaAlerta(alerta) {
                                    nav.navigate("detalle-visacion/${alerta.marcaId}")
                                }
                            }
                        }
                    }

                    // --- PESTAÑA 3: DESGLOSE POR FAENA, USUARIO Y ESTADO ---
                    2 -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Desglose Operativo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Marcas no válidas por faena
                        item {
                            TarjetaDesglose(
                                titulo = "Marcas no válidas por faena",
                                vacio = m.noValidasPorFaenaPct.isEmpty()
                            ) {
                                m.noValidasPorFaenaPct.entries.sortedBy { it.key }.forEachIndexed { idx, (faena, pct) ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    FilaResumen(faena, "${"%.1f".format(Locale.US, pct)} %")
                                }
                            }
                        }

                        // Marcas por faena
                        item {
                            TarjetaDesglose(
                                titulo = "Marcas por faena",
                                vacio = m.marcasPorFaena.isEmpty()
                            ) {
                                m.marcasPorFaena.entries.sortedByDescending { it.value }.forEachIndexed { idx, (faena, count) ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    FilaResumen(faena, count.toString())
                                }
                            }
                        }

                        // Marcas por usuario
                        item {
                            TarjetaDesglose(
                                titulo = "Marcas por usuario",
                                vacio = m.marcasPorUsuario.isEmpty()
                            ) {
                                m.marcasPorUsuario.entries.sortedByDescending { it.value }.forEachIndexed { idx, (usuarioId, count) ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    FilaResumen(usuarioId, count.toString())
                                }
                            }
                        }

                        // Marcas por estado
                        item {
                            TarjetaDesglose(
                                titulo = "Marcas por estado",
                                vacio = m.marcasPorEstado.isEmpty()
                            ) {
                                m.marcasPorEstado.entries.forEachIndexed { idx, (estado, count) ->
                                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    val color = when (estado) {
                                        EstadoMarca.VALIDA -> Ok
                                        EstadoMarca.SOSPECHOSA -> Warn
                                        EstadoMarca.RECHAZADA -> Bad
                                    }
                                    FilaResumen(estado.name, count.toString(), colorValor = color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaDesglose(
    titulo: String,
    vacio: Boolean,
    contenido: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titulo,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (vacio) {
                Text(
                    text = "Sin datos registrados",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                contenido()
            }
        }
    }
}

@Composable
private fun FilaResumen(
    etiqueta: String,
    valor: String,
    colorValor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = valor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorValor
        )
    }
}

@Composable
private fun RowScope.MetricaCard(titulo: String, valor: Int, color: Color) {
    val animado by animateIntAsState(
        targetValue = valor,
        animationSpec = tween(durationMillis = 700),
        label = "metrica-$titulo",
    )
    MetricaCardContenido(titulo, animado.toString(), color)
}

@Composable
private fun RowScope.MetricaCard(titulo: String, valor: String, color: Color) {
    MetricaCardContenido(titulo, valor, color)
}

@Composable
private fun RowScope.MetricaCardContenido(titulo: String, valor: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun FilaAlerta(a: AlertaResumen, onClick: () -> Unit) {
    val color = if (a.estado == EstadoMarca.RECHAZADA) Bad else Warn
    val fecha = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(Date(a.timestampUtc))

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${a.usuarioId} · ${a.faenaId}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = color.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = a.estado.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fecha,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            if (a.motivo.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = a.motivo,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TarjetaErrorAdmin(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
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