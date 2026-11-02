package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.HoraExtraEntity
import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.SyncEstado
import cl.smartvolt.asistencia.domain.model.TipoMarca
import cl.smartvolt.asistencia.ui.admin.AdminViewModel
import cl.smartvolt.asistencia.ui.admin.comoLineas
import cl.smartvolt.asistencia.ui.login.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

class AdminReporteTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: AdminViewModel

    private class RepoDePrueba : AttendanceRepository {
        private fun marca(id: String, faena: String, estado: EstadoMarca, motivo: String, ts: Long) =
            MarcaEntity(id, "OP-0001", faena, TipoMarca.IN, -33.4180, -70.6050, 5.0, ts, ts, estado, motivo, SyncEstado.ENVIADA, "h0", "h-$id")

        override fun todasLasMarcas(): Flow<List<MarcaEntity>> = flowOf(
            listOf(
                marca("m1", "F01", EstadoMarca.VALIDA, "", 1000L),
                marca("m2", "F01", EstadoMarca.SOSPECHOSA, "Sin beacon", 1100L),
                marca("m3", "F02", EstadoMarca.RECHAZADA, "Mock GPS", 1200L),
                marca("m4", "F02", EstadoMarca.VALIDA, "", 1300L),
            )
        )

        override fun contarPendientes(): Flow<Int> = flowOf(0)
        override suspend fun todasLasHorasExtra(): List<HoraExtraEntity> = emptyList()
    }

    @Before
    fun setUp() {
        viewModel = AdminViewModel(RepoDePrueba(), SessionState())
    }

    @Test
    fun calcula_porcentaje_de_marcas_no_validas_por_faena() = runTest {
        viewModel.cargarMetricas()
        advanceUntilIdle()

        val pct = viewModel.metricas.value.noValidasPorFaenaPct
        assertEquals(50.0, pct["F01"]!!, 0.0001)
        assertEquals(50.0, pct["F02"]!!, 0.0001)
    }

    @Test
    fun alertas_solo_incluyen_marcas_no_validas_de_la_mas_reciente_a_la_mas_antigua() = runTest {
        viewModel.cargarMetricas()
        advanceUntilIdle()

        val alertas = viewModel.metricas.value.alertas
        assertEquals(listOf("m3", "m2"), alertas.map { it.marcaId })
        assertEquals("Mock GPS", alertas.first().motivo)
    }

    @Test
    fun las_lineas_del_reporte_incluyen_totales_faenas_y_alertas() = runTest {
        viewModel.cargarMetricas()
        advanceUntilIdle()

        val texto = viewModel.metricas.value.comoLineas(ahoraMs = 0L).joinToString("\n")
        assertTrue(texto.contains("Total de marcas: 4"))
        assertTrue(texto.contains("Válidas: 2"))
        assertTrue(texto.contains("F01: 50.0 % (2 marcas)"))
        assertTrue(texto.contains("Mock GPS"))
    }

    @Test
    fun exportar_pdf_deja_las_lineas_listas_y_termina_de_exportar() = runTest {
        viewModel.exportarPdf()
        advanceUntilIdle()

        assertNotNull(viewModel.pdf.value)
        assertFalse(viewModel.metricas.value.exportando)

        viewModel.limpiarPdf()
        assertNull(viewModel.pdf.value)
    }
}
