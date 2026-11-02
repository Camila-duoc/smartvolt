package cl.smartvolt.asistencia.ui.calendario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.Rol
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

/** ViewModel del calendario: agrupa marcas por fecha según el rol del usuario. */
@HiltViewModel
class CalendarioViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(CalendarioUi())
    val ui: StateFlow<CalendarioUi> = _ui

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
            val base = when (usuario.rol) {
                Rol.OPERARIO -> repo.historial(usuario.id).first()
                Rol.CAPATAZ -> repo.marcasDeFaena(usuario.faenaId).first()
                Rol.ADMIN -> repo.todasLasMarcas().first()
            }
            val lista = base.filter { it.timestampUtc in desde..hasta }

            // Agrupar por fecha (día), más reciente primero.
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val porFecha = lista
                .groupBy { formato.format(Date(it.timestampUtc)) }
                .entries
                .sortedByDescending { (_, marcas) -> marcas.maxOf { it.timestampUtc } }
                .associate { it.key to it.value.sortedByDescending { m -> m.timestampUtc } }

            _ui.value = _ui.value.copy(
                marcasPorFecha = porFecha,
                rol = usuario.rol,
                cargando = false,
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al cargar el historial")
        }
    }
}

data class CalendarioUi(
    val marcasPorFecha: Map<String, List<MarcaEntity>> = emptyMap(),
    val rol: Rol? = null,
    val desde: Long = 0L,
    val hasta: Long = Long.MAX_VALUE,
    val cargando: Boolean = false,
    val error: String? = null,
)
