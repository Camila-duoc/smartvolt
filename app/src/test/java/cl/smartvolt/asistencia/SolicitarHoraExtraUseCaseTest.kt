package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.HoraExtraEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
import cl.smartvolt.asistencia.domain.usecase.SolicitarHoraExtraUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolicitarHoraExtraUseCaseTest {

    private class RepoDePrueba : AttendanceRepository {
        var ultima: HoraExtraEntity? = null

        override suspend fun solicitarHoraExtra(
            usuarioId: String,
            marcaId: String?,
            fecha: Long,
            horas: Double,
            motivo: String
        ): HoraExtraEntity {
            val h = HoraExtraEntity(
                id = "he-1",
                usuarioId = usuarioId,
                marcaId = marcaId,
                fecha = fecha,
                horas = horas,
                motivo = motivo,
                estado = EstadoHoraExtra.PENDIENTE,
                supervisorId = null,
                timestampResolucion = null,
            )
            ultima = h
            return h
        }
    }

    private val repo = RepoDePrueba()
    private val useCase = SolicitarHoraExtraUseCase(repo)

    @Test
    fun rechaza_horas_menores_o_iguales_a_cero() = runTest {
        val cero = runCatching { useCase("OP-0001", null, 1000L, 0.0, "Motivo suficiente") }.exceptionOrNull()
        val negativo = runCatching { useCase("OP-0001", null, 1000L, -1.0, "Motivo suficiente") }.exceptionOrNull()

        assertTrue(cero is IllegalArgumentException)
        assertTrue(negativo is IllegalArgumentException)
    }

    @Test
    fun rechaza_horas_sobre_el_tope() = runTest {
        val error = runCatching {
            useCase("OP-0001", null, 1000L, SolicitarHoraExtraUseCase.MAX_HORAS + 0.5, "Motivo suficiente")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun rechaza_motivo_vacio_o_muy_corto() = runTest {
        val vacio = runCatching { useCase("OP-0001", null, 1000L, 2.0, "   ") }.exceptionOrNull()
        val corto = runCatching { useCase("OP-0001", null, 1000L, 2.0, "Cierre") }.exceptionOrNull()

        assertTrue(vacio is IllegalArgumentException)
        assertTrue(corto is IllegalArgumentException)
    }

    @Test
    fun acepta_solicitud_valida_y_recorta_el_motivo() = runTest {
        val id = useCase("OP-0001", null, 1000L, 2.5, "  Cierre de turno  ")

        assertEquals("he-1", id)
        assertEquals("Cierre de turno", repo.ultima?.motivo)
        assertEquals(2.5, repo.ultima?.horas ?: 0.0, 0.001)
    }
}
