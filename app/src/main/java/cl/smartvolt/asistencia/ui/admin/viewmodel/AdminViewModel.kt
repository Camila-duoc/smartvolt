package cl.smartvolt.asistencia.ui.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.usecase.ExportarMarcasCsv
import cl.smartvolt.asistencia.domain.usecase.HashMarca
import cl.smartvolt.asistencia.domain.usecase.MarcaHash
import cl.smartvolt.asistencia.modelo.EstadoHoraExtra
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.ui.admin.model.AdminMetricas
import cl.smartvolt.asistencia.ui.admin.model.AlertaResumen
import cl.smartvolt.asistencia.ui.admin.model.comoLineas
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_ALERTAS = 20

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

            val verificacion = HashMarca.verificarCadena(
                todas.map {
                    MarcaHash(
                        id = it.id,
                        usuarioId = it.usuarioId,
                        tipo = it.tipo.name,
                        lat = it.lat,
                        lon = it.lon,
                        timestampUtc = it.timestampUtc,
                        estado = it.estado.name,
                        hashPrevio = it.hashPrevio,
                        hash = it.hash,
                    )
                }
            )

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
                cadenaIntegra = if (verificacion.verificadas == 0) null else verificacion.integra,
                marcasVerificadas = verificacion.verificadas,
                marcasNoVerificables = verificacion.noVerificables,
                marcasAlteradas = verificacion.invalidas.size,
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
