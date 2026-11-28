package cl.smartvolt.asistencia.ui.operario.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import cl.smartvolt.asistencia.ui.operario.model.MisHorasExtraUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MisHorasExtraViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(MisHorasExtraUi())
    val ui: StateFlow<MisHorasExtraUi> = _ui

    fun cargarMisHorasExtra() = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true)
        val usuarioId = session.usuario.value?.id.orEmpty()
        if (usuarioId.isEmpty()) {
            _ui.value = _ui.value.copy(cargando = false, error = "No hay sesión activa")
            return@launch
        }
        try {
            val lista = repo.horasExtraDeUsuario(usuarioId)
            _ui.value = _ui.value.copy(horasExtra = lista, cargando = false)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al cargar horas extra")
        }
    }
}
