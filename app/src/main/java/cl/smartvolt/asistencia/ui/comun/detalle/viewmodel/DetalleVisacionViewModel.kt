package cl.smartvolt.asistencia.ui.comun.detalle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.ui.comun.detalle.model.DetalleVisacionUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetalleVisacionViewModel @Inject constructor(
    private val repo: AttendanceRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(DetalleVisacionUi())
    val ui: StateFlow<DetalleVisacionUi> = _ui

    /** Carga el detalle de una marca y su visación (si existe). */
    fun cargarDetalle(marcaId: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        try {
            val marca = repo.marcaPorId(marcaId) ?: return@launch
            val visacion = repo.visacionPorMarca(marcaId)

            _ui.value = DetalleVisacionUi(
                marca = marca,
                visada = visacion?.aprobada,
                visacionTimestamp = visacion?.timestamp,
                cargando = false,
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(cargando = false, error = e.message ?: "Error al cargar el detalle")
        }
    }
}
