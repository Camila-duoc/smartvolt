package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.modelo.VisacionEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.modelo.SyncEstado
import cl.smartvolt.asistencia.modelo.TipoMarca
import cl.smartvolt.asistencia.ui.comun.detalle.viewmodel.DetalleVisacionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DetalleVisacionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeAttendanceRepository
    private lateinit var viewModel: DetalleVisacionViewModel

    @Before
    fun setUp() {
        repo = FakeAttendanceRepository()
        viewModel = DetalleVisacionViewModel(repo)
    }

    private class FakeAttendanceRepository : AttendanceRepository {
        var marcaPorIdResult: MarcaEntity? = null
        var visacionPorMarcaResult: VisacionEntity? = null

        override suspend fun marcaPorId(id: String) = marcaPorIdResult
        override suspend fun visacionPorMarca(marcaId: String) = visacionPorMarcaResult
        override fun contarPendientes(): Flow<Int> = flowOf(0)
    }

    @Test
    fun carga_detalle_con_visacion_aprobada() = runTest {
        val marca = MarcaEntity(
            id = "m1", usuarioId = "OP-0001", faenaId = "F01", tipo = TipoMarca.IN,
            lat = -33.4180, lon = -70.6050, accuracy = 5.0,
            timestampUtc = 1000L, elapsedRealtimeMs = 1000L,
            estado = EstadoMarca.SOSPECHOSA, motivo = "Fuera de perímetro",
            syncEstado = SyncEstado.ENVIADA, hashPrevio = "GENESIS", hash = "abc123"
        )
        val visacion = VisacionEntity(marcaId = "m1", supervisorId = "CAP-0001", aprobada = true, timestamp = 2000L)

        repo.marcaPorIdResult = marca
        repo.visacionPorMarcaResult = visacion

        viewModel.cargarDetalle("m1")
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertEquals("m1", ui.marca?.id)
        assertEquals("OP-0001", ui.marca?.usuarioId)
        assertTrue(ui.visada == true)
        assertEquals(2000L, ui.visacionTimestamp)
        assertNull(ui.error)
    }

    @Test
    fun carga_detalle_sin_visacion() = runTest {
        val marca = MarcaEntity(
            id = "m2", usuarioId = "OP-0002", faenaId = "F01", tipo = TipoMarca.OUT,
            lat = -33.4180, lon = -70.6050, accuracy = 5.0,
            timestampUtc = 1000L, elapsedRealtimeMs = 1000L,
            estado = EstadoMarca.VALIDA, motivo = "",
            syncEstado = SyncEstado.ENVIADA, hashPrevio = "GENESIS", hash = "def456"
        )

        repo.marcaPorIdResult = marca
        repo.visacionPorMarcaResult = null

        viewModel.cargarDetalle("m2")
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertEquals("m2", ui.marca?.id)
        assertNull(ui.visada)
        assertNull(ui.visacionTimestamp)
        assertNull(ui.error)
    }

    @Test
    fun marca_no_encontrada_deja_ui_vacía() = runTest {
        repo.marcaPorIdResult = null

        viewModel.cargarDetalle("inexistente")
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertNull(ui.marca)
        assertNull(ui.visada)
        assertNull(ui.error)
    }
}
