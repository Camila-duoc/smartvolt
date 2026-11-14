package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.modelo.HoraExtraEntity
import cl.smartvolt.asistencia.modelo.UsuarioEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.modelo.EstadoHoraExtra
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.domain.usecase.ResolverHoraExtraUseCase
import cl.smartvolt.asistencia.domain.usecase.SolicitarHoraExtraUseCase
import cl.smartvolt.asistencia.viewmodel.HoraExtraViewModel
import cl.smartvolt.asistencia.ui.comun.sesion.SessionState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class HoraExtraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repo: FakeRepoHorasExtra
    private lateinit var session: SessionState
    private lateinit var viewModel: HoraExtraViewModel

    private val solicitudes = listOf(
        HoraExtraEntity("he1", "OP-0001", null, 1000L, 2.0, "Cierre", EstadoHoraExtra.PENDIENTE, null, null),
        HoraExtraEntity("he2", "OP-0002", null, 2000L, 3.0, "Inventario", EstadoHoraExtra.APROBADA, "CAP-0002", 2500L),
        HoraExtraEntity("he3", "OP-0003", null, 3000L, 1.5, "Aseo", EstadoHoraExtra.RECHAZADA, "CAP-0002", 3500L),
    )

    @Before
    fun setUp() {
        repo = FakeRepoHorasExtra()
        session = SessionState()
        viewModel = HoraExtraViewModel(
            repo,
            SolicitarHoraExtraUseCase(repo),
            ResolverHoraExtraUseCase(repo),
            session,
        )
    }

    private inner class FakeRepoHorasExtra : AttendanceRepository {
        override suspend fun usuariosDeFaena(faenaId: String): List<UsuarioEntity> = when (faenaId) {
            "F01" -> listOf(
                UsuarioEntity("OP-0001", Rol.OPERARIO, "F01", null, null),
                UsuarioEntity("CAP-0001", Rol.CAPATAZ, "F01", null, null),
            )
            "F02" -> listOf(UsuarioEntity("OP-0002", Rol.OPERARIO, "F02", null, null))
            else -> emptyList()
        }

        override suspend fun horasExtraDeUsuario(usuarioId: String): List<HoraExtraEntity> =
            solicitudesMutables.filter { it.usuarioId == usuarioId }

        override suspend fun horasExtraDeUsuarios(usuarioIds: List<String>): List<HoraExtraEntity> =
            solicitudesMutables.filter { it.usuarioId in usuarioIds }

        override suspend fun todasLasHorasExtra(): List<HoraExtraEntity> = solicitudesMutables

        override suspend fun solicitarHoraExtra(
            usuarioId: String,
            marcaId: String?,
            fecha: Long,
            horas: Double,
            motivo: String
        ): HoraExtraEntity = HoraExtraEntity(
            UUID.randomUUID().toString(), usuarioId, marcaId, fecha, horas, motivo,
            EstadoHoraExtra.PENDIENTE, null, null,
        )

        override suspend fun aprobarRechazarHoraExtra(
            id: String,
            supervisorId: String,
            aprobada: Boolean
        ) {
            val indice = solicitudes.indexOfFirst { it.id == id }
            if (indice >= 0) {
                val actual = solicitudes[indice]
                solicitudesMutables[indice] = actual.copy(
                    estado = if (aprobada) EstadoHoraExtra.APROBADA else EstadoHoraExtra.RECHAZADA,
                    supervisorId = supervisorId,
                    timestampResolucion = 9999L,
                )
            }
        }

        val solicitudesMutables = solicitudes.toMutableList()
    }

    @Test
    fun operario_ve_solo_sus_solicitudes() = runTest {
        session.usuario.value = UsuarioEntity("OP-0001", Rol.OPERARIO, "F01", null, null)
        viewModel.cargarSolicitudes()
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertFalse(ui.esSupervisor)
        assertEquals(1, ui.solicitudes.size)
        assertEquals("OP-0001", ui.solicitudes.first().usuarioId)
        assertNull(ui.error)
    }

    @Test
    fun capataz_ve_solicitudes_de_su_faena() = runTest {
        session.usuario.value = UsuarioEntity("CAP-0001", Rol.CAPATAZ, "F01", null, null)
        viewModel.cargarSolicitudes()
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertTrue(ui.esSupervisor)
        assertEquals(1, ui.solicitudes.size)
        assertEquals("OP-0001", ui.solicitudes.first().usuarioId)
    }

    @Test
    fun admin_ve_todas_las_solicitudes() = runTest {
        session.usuario.value = UsuarioEntity("ADM-0001", Rol.ADMIN, "F01", null, null)
        viewModel.cargarSolicitudes()
        advanceUntilIdle()

        val ui = viewModel.ui.value
        assertTrue(ui.esSupervisor)
        assertEquals(3, ui.solicitudes.size)
    }

    @Test
    fun sin_sesion_reporta_error() = runTest {
        viewModel.cargarSolicitudes()
        advanceUntilIdle()

        assertNotNull(viewModel.ui.value.error)
        assertEquals(0, viewModel.ui.value.solicitudes.size)
    }

    @Test
    fun solicitud_invalida_reporta_error() = runTest {
        session.usuario.value = UsuarioEntity("OP-0001", Rol.OPERARIO, "F01", null, null)
        viewModel.solicitarHoraExtra(null, 1000L, 0.0, "Cierre")
        advanceUntilIdle()

        assertNotNull(viewModel.ui.value.error)
        assertFalse(viewModel.ui.value.exito)
    }

    @Test
    fun solicitud_valida_marca_exito() = runTest {
        session.usuario.value = UsuarioEntity("OP-0001", Rol.OPERARIO, "F01", null, null)
        viewModel.solicitarHoraExtra(null, 1000L, 2.0, "Cierre de turno")
        advanceUntilIdle()

        assertNull(viewModel.ui.value.error)
        assertTrue(viewModel.ui.value.exito)
    }

    @Test
    fun capataz_resuelve_y_recarga_solicitudes() = runTest {
        session.usuario.value = UsuarioEntity("CAP-0001", Rol.CAPATAZ, "F01", null, null)
        viewModel.cargarSolicitudes()
        advanceUntilIdle()

        viewModel.resolverHoraExtra("he1", aprobada = true)
        advanceUntilIdle()

        val resuelta = repo.solicitudesMutables.first { it.id == "he1" }
        assertEquals(EstadoHoraExtra.APROBADA, resuelta.estado)
        assertEquals("CAP-0001", resuelta.supervisorId)
        assertTrue(viewModel.ui.value.esSupervisor)
    }
}
