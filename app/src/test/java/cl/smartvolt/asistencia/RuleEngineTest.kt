package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.domain.antifraude.RuleEngine
import cl.smartvolt.asistencia.modelo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    private val engine = RuleEngine.porDefecto()
    private val centro = LatLon(-33.4180, -70.6050)
    private val beacon = BeaconAutorizado("20FDCED4-000F-5342-B70D-D3D16FD767FE", -80, "F01-A")

    private fun evidencia(
        tipo: TipoMarca = TipoMarca.IN,
        ubicacion: UbicacionCapturada? = UbicacionCapturada(centro.lat, centro.lon, 6f, false),
        ble: List<LecturaBle> = listOf(LecturaBle(beacon.uuid, -62, 8)),
        sistema: EstadoSistema = EstadoSistema(modoDesarrollador = false, horaAutomatica = true),
        ultima: TipoMarca? = null,
    ) = Evidencia(
        "OP-0001", "F01", tipo, ubicacion, ble, sistema, 1_000L, 1_000L, ultima,
        Perimetro(TipoPerimetro.CIRCULO, centro, 180.0), listOf(beacon),
    )

    @Test fun marca_valida() = assertEquals(EstadoMarca.VALIDA, engine.evaluar(evidencia()).estado)

    @Test fun fake_gps_se_rechaza() {
        val v = engine.evaluar(evidencia(ubicacion = UbicacionCapturada(centro.lat, centro.lon, 3f, true)))
        assertEquals(EstadoMarca.RECHAZADA, v.estado)
        assertTrue(v.motivo.contains("simulada"))
    }

    @Test fun dentro_del_perimetro_pero_sin_beacon_es_sospechosa() =
        assertEquals(EstadoMarca.SOSPECHOSA, engine.evaluar(evidencia(ble = emptyList())).estado)

    @Test fun fuera_del_perimetro_con_beacon_es_sospechosa() {
        val lejos = UbicacionCapturada(centro.lat + 0.02, centro.lon, 8f, false)
        assertEquals(EstadoMarca.SOSPECHOSA, engine.evaluar(evidencia(ubicacion = lejos)).estado)
    }

    @Test fun fuera_del_perimetro_y_sin_beacon_se_rechaza() {
        val lejos = UbicacionCapturada(centro.lat + 0.02, centro.lon, 8f, false)
        assertEquals(EstadoMarca.RECHAZADA, engine.evaluar(evidencia(ubicacion = lejos, ble = emptyList())).estado)
    }

    @Test fun sin_gps_pero_con_beacon_es_sospechosa() =
        assertEquals(EstadoMarca.SOSPECHOSA, engine.evaluar(evidencia(ubicacion = null)).estado)

    @Test fun beacon_con_senal_debil_no_cuenta() {
        val debil = listOf(LecturaBle(beacon.uuid, -95, 3))
        assertEquals(EstadoMarca.SOSPECHOSA, engine.evaluar(evidencia(ble = debil)).estado)
    }

    @Test fun salida_sin_entrada_se_rechaza() =
        assertEquals(EstadoMarca.RECHAZADA, engine.evaluar(evidencia(tipo = TipoMarca.OUT, ultima = null)).estado)

    @Test fun entrada_duplicada_se_rechaza() =
        assertEquals(EstadoMarca.RECHAZADA, engine.evaluar(evidencia(tipo = TipoMarca.IN, ultima = TipoMarca.IN)).estado)

    @Test fun modo_desarrollador_deja_la_marca_sospechosa() {
        val v = engine.evaluar(evidencia(sistema = EstadoSistema(modoDesarrollador = true, horaAutomatica = true)))
        assertEquals(EstadoMarca.SOSPECHOSA, v.estado)
    }
}
