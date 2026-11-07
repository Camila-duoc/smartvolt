package cl.smartvolt.asistencia.domain.calendario

import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.modelo.EstadoMarca
import java.time.LocalDate
import java.time.YearMonth

/** Estado agregado de un día del calendario, según las marcas registradas. */
enum class EstadoDia {
    SIN_MARCAS,
    VALIDO,
    SOSPECHOSO,
    RECHAZADO,
}

/**
 * Utilidades puras del calendario (sin dependencias de Android) para poder
 * probarlas con JUnit y reutilizarlas desde la UI.
 */
object CalendarioUtils {

    /**
     * Resume el estado de un día aplicando la severidad de mayor prioridad:
     * una marca rechazada gana sobre una sospechosa, y una sospechosa sobre una válida.
     */
    fun estadoDelDia(marcas: List<MarcaEntity>): EstadoDia = when {
        marcas.isEmpty() -> EstadoDia.SIN_MARCAS
        marcas.any { it.estado == EstadoMarca.RECHAZADA } -> EstadoDia.RECHAZADO
        marcas.any { it.estado == EstadoMarca.SOSPECHOSA } -> EstadoDia.SOSPECHOSO
        else -> EstadoDia.VALIDO
    }

    /**
     * Construye las semanas del mes de lunes a domingo.
     * Cada celda es la fecha del día o `null` cuando cae fuera del mes.
     */
    fun semanasDelMes(mes: YearMonth): List<List<LocalDate?>> {
        val primerDia = mes.atDay(1)
        // DayOfWeek.value: lunes = 1 ... domingo = 7. Desplazamiento desde el lunes.
        val desplazamiento = primerDia.dayOfWeek.value - 1
        val celdas = desplazamiento + mes.lengthOfMonth()
        val cantidadSemanas = (celdas + 6) / 7

        return (0 until cantidadSemanas).map { semana ->
            (0..6).map { diaDeSemana ->
                val diaDelMes = semana * 7 + diaDeSemana - desplazamiento + 1
                if (diaDelMes in 1..mes.lengthOfMonth()) mes.atDay(diaDelMes) else null
            }
        }
    }
}
