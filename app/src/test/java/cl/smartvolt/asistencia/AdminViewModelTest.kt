package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.SyncEstado
import cl.smartvolt.asistencia.domain.model.TipoMarca
import cl.smartvolt.asistencia.ui.admin.AdminViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AdminViewModelTest {

    private lateinit var repo: FakeAttendanceRepositoryForAdmin
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        repo = FakeAttendanceRepositoryForAdmin()
        viewModel = AdminViewModel(repo)
    }

    private class FakeAttendanceRepositoryForAdmin : AttendanceRepository(
        null /* db */,
        null /* api */,
        null /* context */
    ) {
        override fun todasLasMarcas(): Flow<List<MarcaEntity>> = flowOf(
            listOf(
                MarcaEntity("m1", "OP-0001", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 1000L, 1000L, EstadoMarca.VALIDA, "", SyncEstado.ENVIADA, "GENESIS", "h1"),
                MarcaEntity("m2", "OP-0001", "F01", TipoMarca.OUT, -33.4180, -70.6050, 5.0, 1100L, 1100L, EstadoMarca.SOSPECHOSA, "Fuera", SyncEstado.ENVIADA, "h1", "h2"),
                MarcaEntity("m3", "OP-0002", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 1200L, 1200L, EstadoMarca.RECHAZADA, "Mock GPS", SyncEstado.PENDIENTE, "h2", "h3"),
            )
        )
        override fun contarPendientes(): Flow<Int> = flowOf(3)
    }

    @Test
    fun calcula_metricas_correctamente() = runTest {
        viewModel.cargarMetricas()
        advanceUntilIdle()

        val m = viewModel.metricas.value
        assertEquals(3, m.totalMarcas)
        assertEquals(1, m.marcasValidas)
        assertEquals(1, m.marcasSospechosas)
        assertEquals(1, m.marcasRechazadas)
        assertEquals(3, m.marcasPendientesSync)
        assertEquals(2, m.marcasPorUsuario.size) // OP-0001 y OP-0002
        assertEquals(2, m.marcasPorUsuario["OP-0001"])
        assertEquals(1, m.marcasPorUsuario["OP-0002"])
        assertNull(m.error)
        assertEquals(false, m.cargando)
    }
}
