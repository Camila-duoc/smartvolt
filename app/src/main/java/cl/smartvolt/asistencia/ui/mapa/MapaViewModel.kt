package cl.smartvolt.asistencia.ui.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.ui.login.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Datos necesarios para dibujar el mapa de la faena del usuario activo. */
data class MapaUi(
    val lat: Double? = null,
    val lon: Double? = null,
    val radioM: Double = 100.0,
    val nombreFaena: String = "",
    val cargando: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class MapaViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(MapaUi())
    val ui: StateFlow<MapaUi> = _ui

    /** Carga el centro y radio del perímetro de la faena del usuario desde Room. */
    fun cargar() = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        val usuario = session.usuario.value
        if (usuario == null) {
            _ui.value = MapaUi(cargando = false, error = "No hay sesión activa")
            return@launch
        }
        try {
            val faena = repo.faena(usuario.faenaId)
            val perimetro = repo.perimetro(usuario.faenaId)
            if (perimetro == null) {
                _ui.value = MapaUi(
                    cargando = false,
                    error = "La faena ${usuario.faenaId} no tiene perímetro cargado"
                )
            } else {
                _ui.value = MapaUi(
                    lat = perimetro.centro.lat,
                    lon = perimetro.centro.lon,
                    radioM = perimetro.radioM ?: 100.0,
                    nombreFaena = faena?.nombre ?: usuario.faenaId,
                    cargando = false,
                )
            }
        } catch (e: Exception) {
            _ui.value = MapaUi(cargando = false, error = e.message ?: "Error al cargar el mapa")
        }
    }
}
