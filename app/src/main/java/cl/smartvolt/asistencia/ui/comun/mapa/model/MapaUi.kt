package cl.smartvolt.asistencia.ui.comun.mapa.model

/** Datos necesarios para dibujar el mapa y, si corresponde, elegir su ubicación. */
data class MapaUi(
    val lat: Double? = null,
    val lon: Double? = null,
    /** Centro original de la faena; se usa si administración todavía no elige un punto. */
    val latFaena: Double? = null,
    val lonFaena: Double? = null,
    val radioM: Double = 100.0,
    val nombreFaena: String = "",
    val esAdmin: Boolean = false,
    /** true cuando el punto mostrado fue guardado por administración. */
    val ubicacionCompartida: Boolean = false,
    /** true cuando administración movió el marcador pero aún no guardó. */
    val seleccionPendiente: Boolean = false,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val cargando: Boolean = true,
    val error: String? = null,
)
