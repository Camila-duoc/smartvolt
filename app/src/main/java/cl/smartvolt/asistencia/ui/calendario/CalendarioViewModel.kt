package cl.smartvolt.asistencia.ui.calendario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.ui.login.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel del calendario: agrupa marcas por fecha. */
@HiltViewModel
class CalendarioViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(CalendarioUi())
    val ui: StateFlow<CalendarioUi> = _ui

    fun cargarHistorial(desde: Long, hasta: Long) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        try {
            val usuarioId = session.usuario.value?.id.orEmpty()
            var lista = repo.todasLasMarcas().first()
                .filter { it.timestampUtc >= desde && it.timestampUtc <= hasta }
            if (lista.isEmpty() && usuarioId.isNotEmpty()) {
                lista = repo.historial(usuarioId).first()
                    .filter { it.timestampUtc >= desde && it.timestampUtc <= hasta }
            }

            // Agrupar por fecha (día), más reciente primero.
            val formato = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val porFecha = lista
                .groupBy { formato.format(java.util.Date(it.timestampUtc)) }
                .entries
                .sortedByDescending { (_, marcas) -> marcas.maxOf { it.timestampUtc } }
                .associate { it.key to it.value.sortedByDescending { m -> m.timestampUtc } }

            _ui.value = _ui.value.copy(marcasPorFecha = porFecha, cargando = false)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al cargar el historial")
        }
    }
}

data class CalendarioUi(
    val marcasPorFecha: Map<String, List<cl.smartvolt.asistencia.data.local.MarcaEntity>> = emptyMap(),
    val cargando: Boolean = false,
    val error: String? = null
)
