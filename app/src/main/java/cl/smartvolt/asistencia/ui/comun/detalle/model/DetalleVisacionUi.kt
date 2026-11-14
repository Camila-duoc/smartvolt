package cl.smartvolt.asistencia.ui.comun.detalle.model

import cl.smartvolt.asistencia.modelo.MarcaEntity

/** UI del detalle de una marca y su visación. */
data class DetalleVisacionUi(
    val marca: MarcaEntity? = null,
    val visada: Boolean? = null,
    val visacionTimestamp: Long? = null,
    val cargando: Boolean = false,
    val error: String? = null
)
