package cl.smartvolt.asistencia.ui.operario.model

import cl.smartvolt.asistencia.modelo.Veredicto

data class CheckInUi(val cargando: Boolean = false, val veredicto: Veredicto? = null, val error: String? = null)
