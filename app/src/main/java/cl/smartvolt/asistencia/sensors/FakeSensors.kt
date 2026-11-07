package cl.smartvolt.asistencia.sensors

import cl.smartvolt.asistencia.modelo.*
import kotlinx.coroutines.delay

class FakeLocationProvider : LocationProvider {
    override suspend fun obtener(referencia: LatLon): UbicacionCapturada? {
        delay(400)
        return when (DemoState.escenario.value) {
            Escenario.FAKE_GPS -> UbicacionCapturada(referencia.lat, referencia.lon, 3f, isMock = true)
            Escenario.FUERA_PERIMETRO ->
                UbicacionCapturada(referencia.lat + 0.02, referencia.lon + 0.02, 9f, isMock = false)
            else -> UbicacionCapturada(referencia.lat + 0.00005, referencia.lon + 0.00005, 6f, isMock = false)
        }
    }
}

class FakeBleScanner : BleScanner {
    override suspend fun escanear(autorizados: List<BeaconAutorizado>, ventanaMs: Long): List<LecturaBle> {
        delay(600)
        return when (DemoState.escenario.value) {
            Escenario.FAKE_GPS, Escenario.FUERA_PERIMETRO, Escenario.SIN_BEACON -> emptyList()
            else -> autorizados.firstOrNull()?.let { listOf(LecturaBle(it.uuid, -62, 8)) } ?: emptyList()
        }
    }
}

class FakeSystemStateProvider : SystemStateProvider {
    override fun estado() = EstadoSistema(
        modoDesarrollador = DemoState.escenario.value == Escenario.MODO_DESARROLLADOR,
        horaAutomatica = true,
    )
}
