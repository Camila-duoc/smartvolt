package cl.smartvolt.asistencia.ui.operario.model

import cl.smartvolt.asistencia.modelo.HoraExtraEntity

data class MisHorasExtraUi(
    val horasExtra: List<HoraExtraEntity> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)
