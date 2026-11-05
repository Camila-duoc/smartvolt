package cl.smartvolt.asistencia.sensors

import cl.smartvolt.asistencia.domain.model.*

/** Los sensores se abstraen para poder inyectar versiones simuladas (demo) o reales (campo). */
interface LocationProvider {
    /** Posición puntual (no tracking continuo). [referencia] solo la usa el simulador. */
    suspend fun obtener(referencia: LatLon): UbicacionCapturada?
}

interface BleScanner {
    /** Escanea durante [ventanaMs] y devuelve el RSSI promedio por UUID. [autorizados] solo lo usa el simulador. */
    suspend fun escanear(autorizados: List<BeaconAutorizado>, ventanaMs: Long = 6_000): List<LecturaBle>
}

interface SystemStateProvider {
    fun estado(): EstadoSistema
}

/** Escenarios para demostrar el sistema sin hardware (flavor "demo"). */
enum class Escenario { VALIDA, FAKE_GPS, FUERA_PERIMETRO, SIN_BEACON, MODO_DESARROLLADOR }

object DemoState {
    val escenario = kotlinx.coroutines.flow.MutableStateFlow(Escenario.VALIDA)
}
