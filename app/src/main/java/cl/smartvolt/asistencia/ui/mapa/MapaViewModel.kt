package cl.smartvolt.asistencia.ui.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.ui.login.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Datos necesarios para dibujar el mapa y, si corresponde, elegir su ubicación. */
data class MapaUi(
    val lat: Double? = null,
    val lon: Double? = null,
    /** Centro original de la faena; se usa si administración todavía no elige un punto. */
    val latFaena: Double? = null,
    val lonFaena: Double? = null,
    val radioM: Double = 100.0,
    val nombreFaena: String = "",
    val esAdmin: Boolean = false,
    /** true cuando el punto mostrado fue guardado por administración. */
    val ubicacionCompartida: Boolean = false,
    /** true cuando administración movió el marcador pero aún no guardó. */
    val seleccionPendiente: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
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

    /**
     * Prioriza la ubicación guardada por administración. Si no existe, usa el
     * centro del perímetro de la faena del usuario (comportamiento anterior).
     */
    fun cargar() = viewModelScope.launch {
        val usuario = session.usuario.value
        if (usuario == null) {
            _ui.value = MapaUi(cargando = false, error = "No hay sesión activa")
            return@launch
        }
        try {
            val faena = repo.faena(usuario.faenaId)
            val perimetro = repo.perimetro(usuario.faenaId)
            val compartida = repo.ubicacionCompartida()

            val latFaena = perimetro?.centro?.lat
            val lonFaena = perimetro?.centro?.lon
            val lat = compartida?.lat ?: latFaena
            val lon = compartida?.lon ?: lonFaena
            val nombre = faena?.nombre ?: usuario.faenaId
            val esAdmin = usuario.rol == Rol.ADMIN

            if (lat == null || lon == null) {
                _ui.value = MapaUi(
                    cargando = false,
                    esAdmin = esAdmin,
                    nombreFaena = nombre,
                    error = "Sin ubicación disponible para esta faena",
                )
            } else {
                _ui.value = MapaUi(
                    lat = lat,
                    lon = lon,
                    latFaena = latFaena,
                    lonFaena = lonFaena,
                    radioM = perimetro?.radioM ?: 100.0,
                    nombreFaena = nombre,
                    esAdmin = esAdmin,
                    ubicacionCompartida = compartida != null,
                    cargando = false,
                )
            }
        } catch (e: Exception) {
            _ui.value = MapaUi(cargando = false, error = e.message ?: "Error al cargar el mapa")
        }
    }

    /** Solo administración puede mover el marcador para elegir una nueva ubicación. */
    fun seleccionar(lat: Double, lon: Double) {
        val actual = _ui.value
        if (!actual.esAdmin || actual.cargando) return
        _ui.value = actual.copy(
            lat = lat,
            lon = lon,
            seleccionPendiente = true,
            guardado = false,
            error = null,
        )
    }

    /** Guarda el punto elegido para que todos los roles vean el mismo lugar. */
    fun guardar() = viewModelScope.launch {
        val usuario = session.usuario.value ?: return@launch
        val actual = _ui.value
        val lat = actual.lat ?: return@launch
        val lon = actual.lon ?: return@launch
        if (!actual.esAdmin || usuario.rol != Rol.ADMIN) return@launch

        _ui.value = actual.copy(guardando = true, guardado = false, error = null)
        try {
            repo.guardarUbicacionCompartida(lat, lon, usuario.id)
            _ui.value = _ui.value.copy(
                guardando = false,
                guardado = true,
                ubicacionCompartida = true,
                seleccionPendiente = false,
            )
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(
                guardando = false,
                error = e.message ?: "No se pudo guardar la ubicación",
            )
        }
    }
}
