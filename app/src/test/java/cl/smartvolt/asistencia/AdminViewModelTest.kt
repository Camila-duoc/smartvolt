package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.modelo.HoraExtraEntity
import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.modelo.EstadoHoraExtra
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.modelo.SyncEstado
import cl.smartvolt.asistencia.modelo.TipoMarca
import cl.smartvolt.asistencia.viewmodel.AdminViewModel
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeAttendanceRepositoryForAdmin
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        repo = FakeAttendanceRepositoryForAdmin()
        viewModel = AdminViewModel(repo, SessionState())
    }

    private class FakeAttendanceRepositoryForAdmin : AttendanceRepository {
        override fun todasLasMarcas(): Flow<List<MarcaEntity>> = flowOf(
            listOf(
                MarcaEntity("m1", "OP-0001", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 1000L, 1000L, EstadoMarca.VALIDA, "", SyncEstado.ENVIADA, "GENESIS", "h1"),
                MarcaEntity("m2", "OP-0001", "F01", TipoMarca.OUT, -33.4180, -70.6050, 5.0, 1100L, 1100L, EstadoMarca.SOSPECHOSA, "Fuera", SyncEstado.ENVIADA, "h1", "h2"),
                MarcaEntity("m3", "OP-0002", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 1200L, 1200L, EstadoMarca.RECHAZADA, "Mock GPS", SyncEstado.PENDIENTE, "h2", "h3"),
            )
        )
        override fun contarPendientes(): Flow<Int> = flowOf(3)
        override suspend fun todasLasHorasExtra(): List<HoraExtraEntity> = listOf(
            HoraExtraEntity("he1", "OP-0001", "m1", 1000L, 2.0, "Cierre de turno", EstadoHoraExtra.PENDIENTE, null, null),
            HoraExtraEntity("he2", "OP-0001", "m2", 1100L, 3.5, "Inventario", EstadoHoraExtra.APROBADA, "CAP-0001", 1500L),
            HoraExtraEntity("he3", "OP-0002", "m3", 1200L, 1.0, "Sin motivo", EstadoHoraExtra.RECHAZADA, "CAP-0001", 1600L),
        )
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

    @Test
    fun calcula_metricas_de_horas_extra() = runTest {
        viewModel.cargarMetricas()
        advanceUntilIdle()

        val m = viewModel.metricas.value
        assertEquals(3, m.horasExtraTotales)
        assertEquals(1, m.horasExtraPendientes)
        assertEquals(1, m.horasExtraAprobadas)
        assertEquals(1, m.horasExtraRechazadas)
        assertEquals(3.5, m.horasExtraAprobadasHoras, 0.0001)
    }

    @Test
    fun calcula_marcas_por_faena() = runTest {
        viewModel.cargarMetricas()
        advanceUntilIdle()

        assertEquals(mapOf("F01" to 3), viewModel.metricas.value.marcasPorFaena)
    }

    @Test
    fun exporta_csv_con_encabezado_y_filas() = runTest {
        viewModel.exportarCsv()
        advanceUntilIdle()

        val csv = viewModel.csv.value
        assertNotNull(csv)
        val lineas = csv!!.trim().lines()
        assertEquals(4, lineas.size) // encabezado + 3 marcas
        assertTrue(lineas.first().startsWith("id;usuarioId;faenaId;tipo"))
        assertTrue(csv.contains("OP-0001"))
        assertTrue(csv.contains("Mock GPS"))
        assertEquals(false, viewModel.metricas.value.exportando)

        viewModel.limpiarExportacion()
        assertNull(viewModel.csv.value)
    }
}
