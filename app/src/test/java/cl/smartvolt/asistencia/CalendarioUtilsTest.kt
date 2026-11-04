package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.domain.calendario.CalendarioUtils
import cl.smartvolt.asistencia.domain.calendario.EstadoDia
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.domain.model.SyncEstado
import cl.smartvolt.asistencia.domain.model.TipoMarca
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth


class CalendarioUtilsTest {
    private fun marca(id: String, estado: EstadoMarca)= MarcaEntity(
        id,"OP-0001","F01", TipoMarca.IN,
        -33.4180, -70.6050, 5.0, 1L, 1L,
        estado,"", SyncEstado.ENVIADA,"GENESIS","h-$id",)

    @Test
    fun  dia_sin_marcas_no_tiene_estado(){
        assertEquals(EstadoDia.SIN_MARCAS, CalendarioUtils.estadoDelDia(emptyList()))
    }

    @Test
    fun rechazada_gana_a_sospechosa_y_sospechosa_gana_a_valida(){
        val conSospecha = listOf(marca("a", EstadoMarca.VALIDA), marca("b", EstadoMarca.SOSPECHOSA))
        val conRechazada = conSospecha + marca("c", EstadoMarca.RECHAZADA)

        assertEquals(EstadoDia.SOSPECHOSO, CalendarioUtils.estadoDelDia(conSospecha))
        assertEquals(EstadoDia.RECHAZADO, CalendarioUtils.estadoDelDia(conRechazada))
    }

    @Test
    fun noviembre_2026_empieza_en_domingo_y_ocupa_seis_semanas() {
        val semanas = CalendarioUtils.semanasDelMes(YearMonth.of(2026, 11))

        assertEquals(6, semanas.size)
        assertEquals(6, semanas[0].count { it == null })
        assertEquals(LocalDate.of(2026, 11, 1), semanas[0][6])
        assertEquals(LocalDate.of(2026, 11, 30), semanas[5][0])
    }

    @Test
    fun febrero_2027_empieza_en_lunes_y_ocupa_cuatro_semanas() {
        val semanas = CalendarioUtils.semanasDelMes(YearMonth.of(2027, 2))

        assertEquals(4, semanas.size)
        assertEquals(LocalDate.of(2027, 2, 1), semanas[0][0])
        assertEquals(LocalDate.of(2027, 2, 28), semanas[3][6])
        assertEquals(0, semanas.flatten().count { it == null })
    }
}