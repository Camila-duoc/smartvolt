package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.SyncEstado
import cl.smartvolt.asistencia.domain.model.TipoMarca
import cl.smartvolt.asistencia.domain.usecase.ExportarMarcasCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportarMarcasCsvTest {

    private fun marca(
        id: String = "m1",
        motivo: String = "",
        estado: EstadoMarca = EstadoMarca.VALIDA,
    ) = MarcaEntity(
        id = id,
        usuarioId = "OP-0001",
        faenaId = "F01",
        tipo = TipoMarca.IN,
        lat = -33.418,
        lon = -70.605,
        accuracy = 5.0,
        timestampUtc = 1000L,
        elapsedRealtimeMs = 1000L,
        estado = estado,
        motivo = motivo,
        syncEstado = SyncEstado.ENVIADA,
        hashPrevio = "GENESIS",
        hash = "h1",
    )

    @Test
    fun incluye_encabezado_y_una_linea_por_marca() {
        val csv = ExportarMarcasCsv.construir(listOf(marca("m1"), marca("m2")))
        val lineas = csv.trim().lines()

        assertEquals(3, lineas.size)
        assertEquals(ExportarMarcasCsv.encabezado, lineas[0])
        assertTrue(lineas[1].startsWith("m1;OP-0001;F01;IN;"))
        assertTrue(lineas[2].startsWith("m2;"))
    }

    @Test
    fun escapa_campos_con_separador_y_comillas() {
        val csv = ExportarMarcasCsv.construir(listOf(marca(motivo = "Falla; \"GPS\" revisar")))
        val linea = csv.trim().lines()[1]

        assertTrue(linea.contains("\"Falla; \"\"GPS\"\" revisar\""))
    }

    @Test
    fun lista_vacia_solo_tiene_encabezado() {
        val csv = ExportarMarcasCsv.construir(emptyList())
        assertEquals(ExportarMarcasCsv.encabezado, csv.trim())
    }

    @Test
    fun encabezado_tiene_las_columnas_esperadas() {
        val columnas = ExportarMarcasCsv.encabezado.split(';')
        assertEquals(
            listOf(
                "id", "usuarioId", "faenaId", "tipo", "fechaHora", "timestampUtc",
                "lat", "lon", "accuracy", "estado", "motivo", "syncEstado", "hashPrevio", "hash",
            ),
            columnas,
        )
    }
}
