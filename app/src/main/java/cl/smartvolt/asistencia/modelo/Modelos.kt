package cl.smartvolt.asistencia.modelo

enum class Rol { ADMIN, CAPATAZ, OPERARIO }
enum class TipoMarca { IN, OUT }
enum class EstadoMarca { VALIDA, SOSPECHOSA, RECHAZADA }
enum class SyncEstado { PENDIENTE, ENVIADA, ERROR }
enum class Severidad { BLOQUEANTE, ADVERTENCIA, INFO }
enum class TipoPerimetro { CIRCULO, POLIGONO }
enum class EstadoHoraExtra { PENDIENTE, APROBADA, RECHAZADA }

data class LatLon(val lat: Double, val lon: Double)

data class Perimetro(
    val tipo: TipoPerimetro,
    val centro: LatLon,
    val radioM: Double? = null,
    val poligono: List<LatLon>? = null,
)

/** Punto del mapa elegido por administración y compartido con todos los roles. */
data class UbicacionCompartida(
    val lat: Double,
    val lon: Double,
    val actualizadoPor: String = "",
    val actualizadoEn: Long = 0L,
)

data class BeaconAutorizado(val uuid: String, val rssiMinimo: Int, val frenteId: String?)
data class LecturaBle(val uuid: String, val rssiPromedio: Int, val muestras: Int)
data class UbicacionCapturada(val lat: Double, val lon: Double, val accuracyM: Float, val isMock: Boolean)
data class EstadoSistema(val modoDesarrollador: Boolean, val horaAutomatica: Boolean)

/** Todo lo que se captura al momento de marcar. Es la entrada del motor de reglas. */
data class Evidencia(
    val usuarioId: String,
    val faenaId: String,
    val tipo: TipoMarca,
    val ubicacion: UbicacionCapturada?,          // null si el GPS no respondió (ej. subterráneo)
    val lecturasBle: List<LecturaBle>,
    val estadoSistema: EstadoSistema,
    val timestampUtcMs: Long,
    val elapsedRealtimeMs: Long,
    val ultimaMarca: TipoMarca?,                 // última marca no rechazada del usuario
    val perimetro: Perimetro,
    val beaconsAutorizados: List<BeaconAutorizado>,
)

data class ResultadoRegla(val regla: String, val aprobada: Boolean, val severidad: Severidad, val detalle: String)
data class Veredicto(val estado: EstadoMarca, val motivo: String, val resultados: List<ResultadoRegla>)
