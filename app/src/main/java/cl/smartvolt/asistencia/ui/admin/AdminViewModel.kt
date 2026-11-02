package cl.smartvolt.asistencia.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.usecase.ExportarMarcasCsv
import cl.smartvolt.asistencia.ui.login.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val MAX_ALERTAS = 20

/** Marca sospechosa o rechazada, para la lista de alertas (solo lectura) del administrador. */
data class AlertaResumen(
    val marcaId: String,
    val usuarioId: String,
    val faenaId: String,
    val estado: EstadoMarca,
    val motivo: String,
    val timestampUtc: Long,
)

/** Métricas y resumen administrativo para el panel de administración. */
data class AdminMetricas(
    val totalMarcas: Int = 0,
    val marcasValidas: Int = 0,
    val marcasSospechosas: Int = 0,
    val marcasRechazadas: Int = 0,
    val marcasPendientesSync: Int = 0,
    val marcasPorUsuario: Map<String, Int> = emptyMap(),
    val marcasPorEstado: Map<EstadoMarca, Int> = emptyMap(),
    val marcasPorFaena: Map<String, Int> = emptyMap(),
    val horasExtraTotales: Int = 0,
    val horasExtraPendientes: Int = 0,
    val horasExtraAprobadas: Int = 0,
    val horasExtraAprobadasHoras: Double = 0.0,
    val horasExtraRechazadas: Int = 0,
    /** Porcentaje de marcas sospechosas o rechazadas sobre el total de cada faena. */
    val noValidasPorFaenaPct: Map<String, Double> = emptyMap(),
    /** Últimas marcas sospechosas o rechazadas (más recientes primero). */
    val alertas: List<AlertaResumen> = emptyList(),
    val cargando: Boolean = false,
    val exportando: Boolean = false,
    val error: String? = null,
)

/** Líneas de texto del reporte; el PDF se arma con estas mismas cifras. */
fun AdminMetricas.comoLineas(ahoraMs: Long = System.currentTimeMillis()): List<String> = buildList {
    val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    add("Generado: ${fecha.format(Date(ahoraMs))}")
    add("")
    add("MARCAS")
    add("Total de marcas: $totalMarcas")
    add("Válidas: $marcasValidas")
    add("Sospechosas: $marcasSospechosas")
    add("Rechazadas: $marcasRechazadas")
    add("Pendientes de sincronizar: $marcasPendientesSync")
    add("")
    add("HORAS EXTRA")
    add("Solicitudes: $horasExtraTotales (pendientes: $horasExtraPendientes, aprobadas: $horasExtraAprobadas, rechazadas: $horasExtraRechazadas)")
    add("Horas aprobadas: $horasExtraAprobadasHoras h")
    add("")
    add("MARCAS NO VÁLIDAS POR FAENA")
    noValidasPorFaenaPct.entries.sortedBy { it.key }.forEach { (faena, pct) ->
        add("$faena: ${"%.1f".format(Locale.US, pct)} % (${marcasPorFaena[faena] ?: 0} marcas)")
    }
    add("")
    add("ALERTAS RECIENTES")
    if (alertas.isEmpty()) add("Sin alertas")
    alertas.forEach { a ->
        val motivoCorto = if (a.motivo.isBlank()) "" else " · ${a.motivo.take(60)}"
        add("${fecha.format(Date(a.timestampUtc))} · ${a.usuarioId} · ${a.faenaId} · ${a.estado.name}$motivoCorto")
    }
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _metricas = MutableStateFlow(AdminMetricas())
    val metricas: StateFlow<AdminMetricas> = _metricas

    /** Contenido CSV listo para que la pantalla lo guarde y comparta. */
    private val _csv = MutableStateFlow<String?>(null)
    val csv: StateFlow<String?> = _csv

    /** Líneas del resumen listas para que la pantalla genere y comparta el PDF. */
    private val _pdf = MutableStateFlow<List<String>?>(null)
    val pdf: StateFlow<List<String>?> = _pdf

    /** Carga las métricas administrativas desde el repositorio local (Room). */
    fun cargarMetricas() = viewModelScope.launch {
        _metricas.value = _metricas.value.copy(cargando = true, error = null)
        try {
            val pendientes = repo.contarPendientes().first()
            val todas = repo.todasLasMarcas().first()
            val horasExtra = repo.todasLasHorasExtra()

            val porEstado = todas.groupBy { it.estado }.mapValues { it.value.size }
            val porUsuario = todas.groupBy { it.usuarioId }.mapValues { it.value.size }
            val porFaena = todas.groupBy { it.faenaId }.mapValues { it.value.size }
            val noValidasPct = todas.groupBy { it.faenaId }.mapValues { (_, l) ->
                l.count { it.estado != EstadoMarca.VALIDA } * 100.0 / l.size
            }
            val alertas = todas.filter { it.estado != EstadoMarca.VALIDA }
                .sortedByDescending { it.timestampUtc }
                .take(MAX_ALERTAS)
                .map { AlertaResumen(it.id, it.usuarioId, it.faenaId, it.estado, it.motivo, it.timestampUtc) }

            _metricas.value = AdminMetricas(
                totalMarcas = todas.size,
                marcasValidas = porEstado[EstadoMarca.VALIDA] ?: 0,
                marcasSospechosas = porEstado[EstadoMarca.SOSPECHOSA] ?: 0,
                marcasRechazadas = porEstado[EstadoMarca.RECHAZADA] ?: 0,
                marcasPendientesSync = pendientes,
                marcasPorUsuario = porUsuario,
                marcasPorEstado = porEstado,
                marcasPorFaena = porFaena,
                horasExtraTotales = horasExtra.size,
                horasExtraPendientes = horasExtra.count { it.estado == EstadoHoraExtra.PENDIENTE },
                horasExtraAprobadas = horasExtra.count { it.estado == EstadoHoraExtra.APROBADA },
                horasExtraAprobadasHoras = horasExtra
                    .filter { it.estado == EstadoHoraExtra.APROBADA }
                    .sumOf { it.horas },
                horasExtraRechazadas = horasExtra.count { it.estado == EstadoHoraExtra.RECHAZADA },
                noValidasPorFaenaPct = noValidasPct,
                alertas = alertas,
                cargando = false,
            )
        } catch (e: Exception) {
            _metricas.value = _metricas.value.copy(cargando = false, error = e.message ?: "Error al cargar métricas")
        }
    }

    /** Genera el CSV de todas las marcas para exportación administrativa (HU-ADM). */
    fun exportarCsv() = viewModelScope.launch {
        _metricas.value = _metricas.value.copy(exportando = true, error = null)
        try {
            val marcas = repo.todasLasMarcas().first()
            _csv.value = ExportarMarcasCsv.construir(marcas)
            _metricas.value = _metricas.value.copy(exportando = false)
        } catch (e: Exception) {
            _metricas.value = _metricas.value.copy(
                exportando = false,
                error = e.message ?: "No se pudo generar el CSV",
            )
        }
    }

    /** Refresca las métricas y deja listas las líneas del resumen para el PDF. */
    fun exportarPdf() = viewModelScope.launch {
        _metricas.value = _metricas.value.copy(exportando = true, error = null)
        cargarMetricas().join()
        if (_metricas.value.error == null) {
            _pdf.value = _metricas.value.comoLineas()
        }
        _metricas.value = _metricas.value.copy(exportando = false)
    }

    fun limpiarExportacion() {
        _csv.value = null
    }

    fun limpiarPdf() {
        _pdf.value = null
    }

    fun cerrarSesion() = session.cerrarSesion()
}
