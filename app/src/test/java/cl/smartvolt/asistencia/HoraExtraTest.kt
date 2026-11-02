package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.HoraExtraEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HoraExtraTest {

    private lateinit var repo: FakeRepoForHoraExtra

    @Before
    fun setUp() {
        repo = FakeRepoForHoraExtra()
    }

    private class FakeRepoForHoraExtra : AttendanceRepository {
        var store = mutableMapOf<String, HoraExtraEntity>()
        override suspend fun solicitarHoraExtra(
            usuarioId: String,
            marcaId: String?,
            fecha: Long,
            horas: Double,
            motivo: String
        ): HoraExtraEntity {
            val id = "he-${store.size + 1}"
            val h = HoraExtraEntity(
                id = id, usuarioId = usuarioId, marcaId = marcaId, fecha = fecha,
                horas = horas, motivo = motivo, estado = EstadoHoraExtra.PENDIENTE,
                supervisorId = null, timestampResolucion = null
            )
            store[id] = h
            return h
        }
        override suspend fun horasExtraDeUsuario(usuarioId: String): List<HoraExtraEntity> =
            store.values.filter { it.usuarioId == usuarioId }.toList()
        override suspend fun horasExtraPendientes(): List<HoraExtraEntity> =
            store.values.filter { it.estado == EstadoHoraExtra.PENDIENTE }.toList()
        override suspend fun aprobarRechazarHoraExtra(id: String, supervisorId: String, aprobada: Boolean) {
            val h = store[id] ?: return
            store[id] = h.copy(
                estado = if (aprobada) EstadoHoraExtra.APROBADA else EstadoHoraExtra.RECHAZADA,
                supervisorId = supervisorId,
                timestampResolucion = System.currentTimeMillis()
            )
        }
        override fun contarPendientes(): Flow<Int> = flowOf(0)
        override suspend fun horasExtraPorRango(desde: Long, hasta: Long): List<HoraExtraEntity> =
            store.values.filter { it.fecha >= desde && it.fecha <= hasta }.toList()
        override suspend fun resumenHorasExtraUsuario(usuarioId: String): List<cl.smartvolt.asistencia.data.local.ResumenHoraExtraEstado> =
            store.values.filter { it.usuarioId == usuarioId }
                .groupBy { it.estado }
                .map { (estado, lista) ->
                    cl.smartvolt.asistencia.data.local.ResumenHoraExtraEstado(
                        estado = estado,
                        cantidad = lista.size,
                        totalHoras = lista.sumOf { it.horas }
                    )
                }
        override suspend fun ultimasHorasExtraUsuario(usuarioId: String): List<HoraExtraEntity> =
            store.values.filter { it.usuarioId == usuarioId }
                .sortedByDescending { it.fecha }
                .take(10)
    }

    @Test
    fun solicitar_horas_extra_crea_registro_pendiente() = runTest {
        val h = repo.solicitarHoraExtra("OP-0001", null, 1000L, 2.5, "Entrega urgente")
        assertNotNull(h.id)
        assertEquals("OP-0001", h.usuarioId)
        assertEquals(2.5, h.horas, 0.001)
        assertEquals(EstadoHoraExtra.PENDIENTE, h.estado)
        assertNull(h.supervisorId)
        assertNull(h.timestampResolucion)
    }

    @Test
    fun aprobar_horas_extra_cambia_estado() = runTest {
        val h = repo.solicitarHoraExtra("OP-0001", null, 1000L, 2.0, "Prueba")
        repo.aprobarRechazarHoraExtra(h.id, "CAP-0001", true)
        val actualizada = repo.horasExtraDeUsuario("OP-0001").firstOrNull { it.id == h.id }
        assertNotNull(actualizada)
        assertEquals(EstadoHoraExtra.APROBADA, actualizada?.estado)
        assertEquals("CAP-0001", actualizada?.supervisorId)
        assertNotNull(actualizada?.timestampResolucion)
    }

    @Test
    fun rechazar_horas_extra_cambia_estado() = runTest {
        val h = repo.solicitarHoraExtra("OP-0001", null, 1000L, 1.0, "Prueba")
        repo.aprobarRechazarHoraExtra(h.id, "CAP-0001", false)
        val actualizada = repo.horasExtraDeUsuario("OP-0001").firstOrNull { it.id == h.id }
        assertNotNull(actualizada)
        assertEquals(EstadoHoraExtra.RECHAZADA, actualizada?.estado)
    }

    @Test
    fun horas_extra_pendientes_filtran_correctamente() = runTest {
        repo.solicitarHoraExtra("OP-0001", null, 1000L, 2.0, "A")
        repo.solicitarHoraExtra("OP-0002", null, 1000L, 3.0, "B")
        repo.aprobarRechazarHoraExtra(repo.horasExtraDeUsuario("OP-0001")[0].id, "CAP-0001", true)
        val pendientes = repo.horasExtraPendientes()
        assertEquals(1, pendientes.size)
        assertEquals("OP-0002", pendientes[0].usuarioId)
    }

    @Test
    fun resumen_horas_extra_por_usuario_agrupa_por_estado() = runTest {
        repo.solicitarHoraExtra("OP-0001", null, 1000L, 2.0, "A")
        repo.solicitarHoraExtra("OP-0001", null, 2000L, 3.0, "B")
        repo.solicitarHoraExtra("OP-0001", null, 3000L, 1.5, "C")
        val idB = repo.horasExtraDeUsuario("OP-0001")[1].id
        repo.aprobarRechazarHoraExtra(idB, "CAP-0001", true)
        val idC = repo.horasExtraDeUsuario("OP-0001")[2].id
        repo.aprobarRechazarHoraExtra(idC, "CAP-0001", false)

        val resumen = repo.resumenHorasExtraUsuario("OP-0001")
        val porEstado = resumen.associate { it.estado to it }
        assertEquals(1, porEstado[EstadoHoraExtra.PENDIENTE]!!.cantidad)
        assertEquals(2.0, porEstado[EstadoHoraExtra.PENDIENTE]!!.totalHoras, 0.001)
        assertEquals(1, porEstado[EstadoHoraExtra.APROBADA]!!.cantidad)
        assertEquals(3.0, porEstado[EstadoHoraExtra.APROBADA]!!.totalHoras, 0.001)
        assertEquals(1, porEstado[EstadoHoraExtra.RECHAZADA]!!.cantidad)
        assertEquals(1.5, porEstado[EstadoHoraExtra.RECHAZADA]!!.totalHoras, 0.001)
    }

    @Test
    fun ultimas_horas_extra_devuelve_maximo_10() = runTest {
        repeat(12) { i ->
            repo.solicitarHoraExtra("OP-0001", null, 1000L + i, 1.0, "Prueba $i")
        }
        val ultimas = repo.ultimasHorasExtraUsuario("OP-0001")
        assertEquals(10, ultimas.size)
    }
}
