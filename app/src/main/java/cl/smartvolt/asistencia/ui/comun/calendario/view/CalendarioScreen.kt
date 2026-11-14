package cl.smartvolt.asistencia.ui.comun.calendario.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.domain.calendario.CalendarioUtils
import cl.smartvolt.asistencia.domain.calendario.EstadoDia
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.ui.comun.theme.Bad
import cl.smartvolt.asistencia.ui.comun.theme.Ok
import cl.smartvolt.asistencia.ui.comun.theme.Warn
import cl.smartvolt.asistencia.ui.comun.calendario.viewmodel.CalendarioViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** Cabecera de la grilla: semana de lunes a domingo. */
private val DIAS_SEMANA = listOf("L", "M", "X", "J", "V", "S", "D")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    onNavUp: () -> Unit,
    vm: CalendarioViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()

    // Mes visible y día seleccionado. La grilla siempre muestra el mes completo,
    // así que estos dos estados viven en la pantalla y no en el ViewModel.
    var mes by remember { mutableStateOf(YearMonth.now()) }
    var diaSeleccionado by remember { mutableStateOf<LocalDate?>(null) }

    // Cada cambio de mes recarga solo ese rango y limpia la selección anterior
    // para evitar mostrar el detalle de un día que ya no pertenece a la grilla.
    LaunchedEffect(mes) {
        diaSeleccionado = null
        vm.cargarMes(mes)
    }

    val titulo = when (ui.rol) {
        Rol.OPERARIO -> "Mi historial"
        Rol.CAPATAZ -> "Historial de la faena"
        Rol.ADMIN -> "Historial general"
        null -> "Historial"
    }
    val totalMes = ui.marcasPorDia.values.sumOf { it.size }
    val dia = diaSeleccionado
    val marcasDelDia = dia?.let { ui.marcasPorDia[it] }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                navigationIcon = {
                    IconButton(onClick = onNavUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$totalMes marca(s) en ${ui.marcasPorDia.size} día(s) de ${formatearMes(mes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                CabeceraMes(
                    mes = mes,
                    onMesAnterior = { mes = mes.minusMonths(1) },
                    onMesSiguiente = { mes = mes.plusMonths(1) },
                    onHoy = { mes = YearMonth.now() },
                )
            }

            if (ui.cargando) {
                item { CircularProgressIndicator() }
            } else {
                // Grilla mensual: usa CalendarioUtils para calcular las semanas y
                // el estado agregado de cada día (rechazada > sospechosa > válida).
                item {
                    CalendarioMensual(
                        mes = mes,
                        marcasPorDia = ui.marcasPorDia,
                        diaSeleccionado = dia,
                        onDiaClick = { diaSeleccionado = it },
                    )
                }
                item { LeyendaEstados() }

                if (dia != null) {
                    item {
                        Column {
                            Text(
                                text = formatearFecha(dia),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "${marcasDelDia.size} marca(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (marcasDelDia.isEmpty()) {
                        item {
                            Text(
                                "Sin marcas este día",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(items = marcasDelDia, key = { it.id }) { marca ->
                            MarcaFila(marca)
                        }
                    }
                } else if (ui.marcasPorDia.isEmpty()) {
                    item {
                        Text(
                            "Sin registros en este mes",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    // Sin día seleccionado se muestra el resumen del mes,
                    // agrupado por fecha y con lo más reciente primero.
                    ui.marcasPorDia.forEach { (fecha, marcas) ->
                        item(key = "dia-$fecha") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatearFecha(fecha),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "${marcas.size} marca(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                marcas.forEach { MarcaFila(it) }
                            }
                        }
                    }
                }
            }

            ui.error?.let { error ->
                item {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

/** Navegación entre meses con acceso rápido al mes actual. */
@Composable
private fun CabeceraMes(
    mes: YearMonth,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onHoy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMesAnterior) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(
            text = formatearMes(mes),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onMesSiguiente) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
        }
        TextButton(onClick = onHoy) { Text("Hoy") }
    }
}

/**
 * Grilla del mes.
 *
 * - `semanasDelMes` devuelve listas de 7 celdas (lunes a domingo); las celdas
 *   que caen fuera del mes llegan como `null` y se dibujan como espacios vacíos.
 * - El color de cada día sale de `estadoDelDia`, que resume todas las marcas del día.
 * - El punto inferior solo aparece si el día tiene al menos una marca.
 */
@Composable
private fun CalendarioMensual(
    mes: YearMonth,
    marcasPorDia: Map<LocalDate, List<MarcaEntity>>,
    diaSeleccionado: LocalDate?,
    onDiaClick: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            DIAS_SEMANA.forEach { nombre ->
                Text(
                    text = nombre,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        CalendarioUtils.semanasDelMes(mes).forEach { semana ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                semana.forEach { fecha ->
                    if (fecha == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val marcas = marcasPorDia[fecha].orEmpty()
                        val estado = CalendarioUtils.estadoDelDia(marcas)
                        val color = colorDeEstado(estado)
                        val seleccionado = fecha == diaSeleccionado

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = if (seleccionado) 0.30f else 0.15f))
                                .border(
                                    width = if (seleccionado) 2.dp else 1.dp,
                                    color = if (seleccionado) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        color.copy(alpha = 0.4f)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { onDiaClick(fecha) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = fecha.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (marcas.isNotEmpty()) {
                                    Box(
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Leyenda de colores para interpretar la grilla sin abrir cada día. */
@Composable
private fun LeyendaEstados() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        listOf(
            EstadoDia.VALIDO to "Válida",
            EstadoDia.SOSPECHOSO to "Sospechosa",
            EstadoDia.RECHAZADO to "Rechazada",
        ).forEach { (estado, etiqueta) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colorDeEstado(estado))
                )
                Spacer(Modifier.width(4.dp))
                Text(etiqueta, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Fila reutilizable para el detalle de una marca. */
@Composable
private fun MarcaFila(m: MarcaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${m.usuarioId} · ${m.tipo}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val horaTxt = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(Date(m.timestampUtc))
                Text(
                    "${m.faenaId} · $horaTxt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (m.motivo.isNotBlank()) {
                    Text(
                        m.motivo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

@Composable
private fun colorDeEstado(estado: EstadoDia): Color = when (estado) {
    EstadoDia.SIN_MARCAS -> MaterialTheme.colorScheme.outline
    EstadoDia.VALIDO -> Ok
    EstadoDia.SOSPECHOSO -> Warn
    EstadoDia.RECHAZADO -> Bad
}

/** "noviembre 2026" con la primera letra en mayúscula. */
private fun formatearMes(mes: YearMonth): String {
    val texto = mes.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    return texto.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

/** "lunes 30 de noviembre" con la primera letra en mayúscula. */
private fun formatearFecha(fecha: LocalDate): String {
    val texto = fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.getDefault()))
    return texto.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
