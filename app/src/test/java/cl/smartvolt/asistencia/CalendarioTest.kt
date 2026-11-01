package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.SyncEstado
import cl.smartvolt.asistencia.domain.model.TipoMarca
import cl.smartvolt.asistencia.ui.calendario.CalendarioViewModel
import cl.smartvolt.asistencia.ui.login.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalendarioTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeRepoForCalendario
    private lateinit var viewModel: CalendarioViewModel

    @Before
    fun setUp() {
        repo = FakeRepoForCalendario()
        viewModel = CalendarioViewModel(repo, SessionState())
    }

    private class FakeRepoForCalendario : AttendanceRepository {
        override fun todasLasMarcas(): Flow<List<MarcaEntity>> = flowOf(
            listOf(
                MarcaEntity("m1", "OP-0001", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 1000L, 1000L, EstadoMarca.VALIDA, "", SyncEstado.ENVIADA, "GENESIS", "h1"),
                MarcaEntity("m2", "OP-0001", "F01", TipoMarca.OUT, -33.4180, -70.6050, 5.0, 2000L, 2000L, EstadoMarca.SOSPECHOSA, "", SyncEstado.ENVIADA, "h1", "h2"),
                MarcaEntity("m3", "OP-0002", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 3000L, 3000L, EstadoMarca.RECHAZADA, "", SyncEstado.PENDIENTE, "h2", "h3"),
            )
        )
        override fun contarPendientes(): Flow<Int> = flowOf(0)
    }

    @Test
    fun agrupa_marcas_por_fecha() = runTest {
        viewModel.cargarHistorial(0L, 100000L)
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertEquals(3, ui.marcasPorFecha.values.sumOf { it.size })
        // Todas las marcas del ejemplo caen el mismo día (1/1/1970)
        assertEquals(1, ui.marcasPorFecha.size)
    }
}
