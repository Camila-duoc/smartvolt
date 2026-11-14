package cl.smartvolt.asistencia.ui.comun.horasextra.model

import cl.smartvolt.asistencia.modelo.HoraExtraEntity

data class HoraExtraUi(
    val solicitudes: List<HoraExtraEntity> = emptyList(),
    val esSupervisor: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false,
)
