package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.remote.FakeApiService
import cl.smartvolt.asistencia.modelo.LoteRequest
import cl.smartvolt.asistencia.modelo.MarcaDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeApiServiceTest {

    private fun marca(id: String, estado: String = "VALIDA") = MarcaDto(
        id = id, usuarioId = "OP-0001", faenaId = "F01", tipo = "IN",
        lat = -33.4180, lon = -70.6050, accuracy = 5.0,
        timestampUtc = 1000L, elapsedRealtimeMs = 1000L,
        estadoCliente = estado, motivo = "", hashPrevio = "GENESIS", hash = "h-$id",
    )

    @Test
    fun acepta_todas_las_marcas_del_lote() = runTest {
        val resp = FakeApiService().enviarLote(LoteRequest("DEV-0001", listOf(marca("m1"), marca("m2"))))

        assertEquals(listOf("m1", "m2"), resp.resultados.map { it.id })
        assertTrue(resp.resultados.all { it.aceptada })
    }

    @Test
    fun conserva_el_estado_enviado_por_el_cliente() = runTest {
        val resp = FakeApiService().enviarLote(LoteRequest(null, listOf(marca("m1", "SOSPECHOSA"))))

        assertEquals("SOSPECHOSA", resp.resultados.single().estadoServidor)
    }

    @Test
    fun lote_vacio_devuelve_respuesta_vacia() = runTest {
        val resp = FakeApiService().enviarLote(LoteRequest(null, emptyList()))

        assertTrue(resp.resultados.isEmpty())
    }

    @Test
    fun configuracion_vacia_no_modifica_los_datos_locales() = runTest {
        val c = FakeApiService().configuracion(null)

        assertTrue(c.faenas.isEmpty() && c.frentes.isEmpty() && c.beacons.isEmpty() && c.usuarios.isEmpty())
    }
}
