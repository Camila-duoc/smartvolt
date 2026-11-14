package cl.smartvolt.asistencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** ViewModel del calendario: agrupa marcas por día local según el rol del usuario. */
@HiltViewModel
class CalendarioViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(CalendarioUi())
    val ui: StateFlow<CalendarioUi> = _ui

    /**
     * Carga las marcas del mes visible.
     *
     * Se calcula el rango [inicio del mes, inicio del mes siguiente) en la zona horaria
     * del dispositivo para que el agrupamiento por día coincida con lo que ve el usuario.
     * Restar 1 ms al inicio del mes siguiente hace el rango inclusivo en el último día.
     */
    fun cargarMes(mes: YearMonth) {
        val zona = ZoneId.systemDefault()
        val desde = mes.atDay(1).atStartOfDay(zona).toInstant().toEpochMilli()
        val hasta = mes.plusMonths(1).atDay(1)
            .atStartOfDay(zona).toInstant().toEpochMilli() - 1
        cargarHistorial(desde, hasta)
    }

    /**
     * Operario: su propio historial.
     * Capataz: marcas de su faena.
     * Admin: todas las marcas.
     */
    fun cargarHistorial(desde: Long, hasta: Long) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null, desde = desde, hasta = hasta)
        val usuario = session.usuario.value
        if (usuario == null) {
            _ui.value = _ui.value.copy(cargando = false, error = "No hay sesión activa")
            return@launch
        }
        try {
            // Acceso según rol: cada usuario solo consulta el alcance que le corresponde.
            val base = when (usuario.rol) {
                Rol.OPERARIO -> repo.historial(usuario.id).first()
                Rol.CAPATAZ -> repo.marcasDeFaena(usuario.faenaId).first()
                Rol.ADMIN -> repo.todasLasMarcas().first()
            }
            val lista = base.filter { it.timestampUtc in desde..hasta }

            // La clave es LocalDate (no un String) para poder compararla directamente
            // con las celdas de la grilla y con el día seleccionado.
            val zona = ZoneId.systemDefault()
            val porDia = lista
                .groupBy { Instant.ofEpochMilli(it.timestampUtc).atZone(zona).toLocalDate() }
                .entries
                .sortedByDescending { it.key }
                .associate { (fecha, marcas) ->
                    fecha to marcas.sortedByDescending { it.timestampUtc }
                }

            _ui.value = _ui.value.copy(
                marcasPorDia = porDia,
                rol = usuario.rol,
                cargando = false,
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al cargar el historial")
        }
    }
}

data class CalendarioUi(
    /** Marcas agrupadas por día local. Solo contiene días con al menos una marca. */
    val marcasPorDia: Map<LocalDate, List<MarcaEntity>> = emptyMap(),
    val rol: Rol? = null,
    val desde: Long = 0L,
    val hasta: Long = Long.MAX_VALUE,
    val cargando: Boolean = false,
    val error: String? = null,
)
