package cl.smartvolt.asistencia.ui.comun.calendario.model

import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.modelo.Rol
import java.time.LocalDate

data class CalendarioUi(
    /** Marcas agrupadas por día local. Solo contiene días con al menos una marca. */
    val marcasPorDia: Map<LocalDate, List<MarcaEntity>> = emptyMap(),
    val rol: Rol? = null,
    val desde: Long = 0L,
    val hasta: Long = Long.MAX_VALUE,
    val cargando: Boolean = false,
    val error: String? = null,
)
