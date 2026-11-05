package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.FaenaEntity
import cl.smartvolt.asistencia.data.local.UsuarioEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.LatLon
import cl.smartvolt.asistencia.domain.model.Perimetro
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.domain.model.TipoPerimetro
import cl.smartvolt.asistencia.domain.model.UbicacionCompartida
import cl.smartvolt.asistencia.ui.login.SessionState
import cl.smartvolt.asistencia.ui.mapa.MapaViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeRepoMapa : AttendanceRepository {
        var compartida: UbicacionCompartida? = null
        var guardados: Int = 0

        override suspend fun faena(id: String) = FaenaEntity(
            id = id,
            nombre = "Faena demo",
            tipoPerimetro = TipoPerimetro.CIRCULO,
            centroLat = -33.418,
            centroLon = -70.605,
            radioM = 180.0,
            poligonoJson = null,
        )

        override suspend fun perimetro(faenaId: String) = Perimetro(
            tipo = TipoPerimetro.CIRCULO,
            centro = LatLon(-33.418, -70.605),
            radioM = 180.0,
        )

        override suspend fun ubicacionCompartida(): UbicacionCompartida? = compartida

        override suspend fun guardarUbicacionCompartida(lat: Double, lon: Double, usuarioId: String) {
            compartida = UbicacionCompartida(lat, lon, usuarioId, 1L)
            guardados++
        }
    }

    private fun sesion(usuario: UsuarioEntity): SessionState = SessionState().apply {
        this.usuario.value = usuario
    }

    @Test
    fun admin_guarda_la_ubicacion_que_veran_los_demas() = runTest {
        val repo = FakeRepoMapa()
        val session = sesion(UsuarioEntity("ADM-0001", Rol.ADMIN, "F01", null, null))
        val vm = MapaViewModel(repo, session)

        vm.cargar()
        advanceUntilIdle()
        assertTrue(vm.ui.value.esAdmin)
        // Sin ubicación compartida, parte en el centro de la faena.
        assertEquals(-33.418, vm.ui.value.lat!!, 0.0001)
        assertEquals(-70.605, vm.ui.value.lon!!, 0.0001)

        vm.seleccionar(-33.5, -70.7)
        assertTrue(vm.ui.value.seleccionPendiente)

        vm.guardar()
        advanceUntilIdle()

        assertEquals(1, repo.guardados)
        assertTrue(vm.ui.value.ubicacionCompartida)
        assertFalse(vm.ui.value.seleccionPendiente)
        assertTrue(vm.ui.value.guardado)
        assertEquals(-33.5, vm.ui.value.lat!!, 0.0001)
        assertEquals(-70.7, vm.ui.value.lon!!, 0.0001)
    }

    @Test
    fun operario_ve_la_ubicacion_compartida_pero_no_puede_cambiarla() = runTest {
        val repo = FakeRepoMapa()
        repo.compartida = UbicacionCompartida(-33.5, -70.7, "ADM-0001", 1L)
        val session = sesion(UsuarioEntity("OP-0001", Rol.OPERARIO, "F01", "F01-A", null))
        val vm = MapaViewModel(repo, session)

        vm.cargar()
        advanceUntilIdle()

        assertFalse(vm.ui.value.esAdmin)
        assertTrue(vm.ui.value.ubicacionCompartida)
        assertEquals(-33.5, vm.ui.value.lat!!, 0.0001)
        assertEquals(-70.7, vm.ui.value.lon!!, 0.0001)

        vm.seleccionar(-33.6, -70.8)
        advanceUntilIdle()

        // El intento de un rol no administrador se ignora.
        assertEquals(-33.5, vm.ui.value.lat!!, 0.0001)
        assertEquals(-70.7, vm.ui.value.lon!!, 0.0001)
        assertEquals(0, repo.guardados)
    }
}
