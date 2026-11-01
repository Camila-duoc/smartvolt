package cl.smartvolt.asistencia.ui.horasextra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.local.HoraExtraEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.usecase.ResolverHoraExtraUseCase
import cl.smartvolt.asistencia.domain.usecase.SolicitarHoraExtraUseCase
import cl.smartvolt.asistencia.ui.login.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HoraExtraUi(
    val solicitudes: List<HoraExtraEntity> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

@HiltViewModel
class HoraExtraViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val solicitarUseCase: SolicitarHoraExtraUseCase,
    private val resolverUseCase: ResolverHoraExtraUseCase,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(HoraExtraUi())
    val ui: StateFlow<HoraExtraUi> = _ui

    /** Para el capataz: carga todas las solicitudes de horas extra del equipo. */
    fun cargarSolicitudes() = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        try {
            val lista = repo.todasLasHorasExtra().sortedByDescending { it.fecha }
            _ui.value = _ui.value.copy(solicitudes = lista, cargando = false)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al cargar horas extra")
        }
    }

    fun solicitarHoraExtra(
        marcaId: String?,
        fecha: Long,
        horas: Double,
        motivo: String
    ) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null, exito = false)
        try {
            val usuarioId = session.usuario.value?.id.orEmpty()
            if (usuarioId.isEmpty()) {
                _ui.value = _ui.value.copy(cargando = false, error = "No hay sesión activa")
                return@launch
            }
            solicitarUseCase(usuarioId, marcaId, fecha, horas, motivo)
            _ui.value = _ui.value.copy(cargando = false, exito = true)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al solicitar horas extra")
        }
    }

    /** Para el capataz: aprueba o rechaza una solicitud de horas extra. */
    fun resolverHoraExtra(id: String, aprobada: Boolean) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        try {
            val supervisorId = session.usuario.value?.id.orEmpty()
            resolverUseCase(id, supervisorId, aprobada)
            val lista = repo.todasLasHorasExtra().sortedByDescending { it.fecha }
            _ui.value = _ui.value.copy(solicitudes = lista, cargando = false, exito = true)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al resolver la solicitud")
        }
    }
}
