package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.modelo.UsuarioEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.modelo.EstadoMarca
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.modelo.SyncEstado
import cl.smartvolt.asistencia.modelo.TipoMarca
import cl.smartvolt.asistencia.viewmodel.CalendarioViewModel
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CalendarioTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeRepoForCalendario
    private lateinit var session: SessionState
    private lateinit var viewModel: CalendarioViewModel

    private val marcas = listOf(
        MarcaEntity("m1", "OP-0001", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 1000L, 1000L, EstadoMarca.VALIDA, "", SyncEstado.ENVIADA, "GENESIS", "h1"),
        MarcaEntity("m2", "OP-0001", "F01", TipoMarca.OUT, -33.4180, -70.6050, 5.0, 2000L, 2000L, EstadoMarca.SOSPECHOSA, "", SyncEstado.ENVIADA, "h1", "h2"),
        MarcaEntity("m3", "OP-0002", "F01", TipoMarca.IN, -33.4180, -70.6050, 5.0, 3000L, 3000L, EstadoMarca.RECHAZADA, "", SyncEstado.PENDIENTE, "h2", "h3"),
    )

    @Before
    fun setUp() {
        repo = FakeRepoForCalendario()
        session = SessionState()
        viewModel = CalendarioViewModel(repo, session)
    }

    private inner class FakeRepoForCalendario : AttendanceRepository {
        override fun todasLasMarcas(): Flow<List<MarcaEntity>> = flowOf(marcas)

        override fun marcasDeFaena(faenaId: String): Flow<List<MarcaEntity>> =
            flowOf(marcas.filter { it.faenaId == faenaId })

        override fun historial(usuarioId: String): Flow<List<MarcaEntity>> =
            flowOf(marcas.filter { it.usuarioId == usuarioId })

        override fun contarPendientes(): Flow<Int> = flowOf(0)
    }

    @Test
    fun agrupa_marcas_por_fecha_para_admin() = runTest {
        session.usuario.value = UsuarioEntity("ADM-0001", Rol.ADMIN, "F01", null, null)
        viewModel.cargarHistorial(0L, 100000L)
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertEquals(Rol.ADMIN, ui.rol)
        assertEquals(3, ui.marcasPorDia.values.sumOf { it.size })
        // Todas las marcas del ejemplo caen el mismo día (1/1/1970)
        assertEquals(1, ui.marcasPorDia.size)
        assertNull(ui.error)
    }

    @Test
    fun operario_solo_ve_sus_propias_marcas() = runTest {
        session.usuario.value = UsuarioEntity("OP-0001", Rol.OPERARIO, "F01", null, null)
        viewModel.cargarHistorial(0L, 100000L)
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertEquals(Rol.OPERARIO, ui.rol)
        assertEquals(2, ui.marcasPorDia.values.sumOf { it.size })
        assertEquals(listOf("OP-0001"), ui.marcasPorDia.values.flatten().map { it.usuarioId }.distinct())
    }

    @Test
    fun capataz_ve_las_marcas_de_su_faena() = runTest {
        session.usuario.value = UsuarioEntity("CAP-0001", Rol.CAPATAZ, "F01", null, null)
        viewModel.cargarHistorial(0L, 100000L)
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertEquals(Rol.CAPATAZ, ui.rol)
        assertEquals(3, ui.marcasPorDia.values.sumOf { it.size })
    }

    @Test
    fun capataz_de_otra_faena_no_ve_marcas() = runTest {
        session.usuario.value = UsuarioEntity("CAP-0002", Rol.CAPATAZ, "F02", null, null)
        viewModel.cargarHistorial(0L, 100000L)
        advanceUntilIdle()

        assertEquals(0, viewModel.ui.value.marcasPorDia.size)
    }

    @Test
    fun sin_sesion_reporta_error() = runTest {
        viewModel.cargarHistorial(0L, 100000L)
        advanceUntilIdle()

        assertNotNull(viewModel.ui.value.error)
        assertEquals(0, viewModel.ui.value.marcasPorDia.size)
    }
}
