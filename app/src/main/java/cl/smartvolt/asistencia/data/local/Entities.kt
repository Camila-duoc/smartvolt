package cl.smartvolt.asistencia.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.smartvolt.asistencia.domain.model.*

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,          // identificador anonimizado (ej. OP-0007)
    val rol: Rol, val faenaId: String, val frenteId: String?, val deviceId: String?,
)

@Entity(tableName = "faenas")
data class FaenaEntity(
    @PrimaryKey val id: String, val nombre: String, val tipoPerimetro: TipoPerimetro,
    val centroLat: Double, val centroLon: Double, val radioM: Double?, val poligonoJson: String?,
)

@Entity(tableName = "frentes")
data class FrenteEntity(@PrimaryKey val id: String, val faenaId: String, val nombre: String)

@Entity(tableName = "beacons")
data class BeaconEntity(
    @PrimaryKey val uuid: String, val major: Int, val minor: Int,
    val faenaId: String, val frenteId: String?, val rssiMinimo: Int,
)

@Entity(tableName = "marcas", indices = [Index("usuarioId"), Index("faenaId"), Index("syncEstado")])
data class MarcaEntity(
    @PrimaryKey val id: String,          // UUID generado en el cliente => idempotencia al sincronizar
    val usuarioId: String, val faenaId: String, val tipo: TipoMarca,
    val lat: Double?, val lon: Double?, val accuracy: Double?,
    val timestampUtc: Long, val elapsedRealtimeMs: Long,
    val estado: EstadoMarca, val motivo: String, val syncEstado: SyncEstado,
    val hashPrevio: String, val hash: String,
)

@Entity(
    tableName = "lecturas_ble", indices = [Index("marcaId")],
    foreignKeys = [ForeignKey(MarcaEntity::class, ["id"], ["marcaId"], onDelete = ForeignKey.CASCADE)],
)
data class LecturaBleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marcaId: String, val uuid: String, val rssiPromedio: Int, val muestras: Int,
)

@Entity(
    tableName = "eventos_antifraude", indices = [Index("marcaId")],
    foreignKeys = [ForeignKey(MarcaEntity::class, ["id"], ["marcaId"], onDelete = ForeignKey.CASCADE)],
)
data class EventoAntifraudeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marcaId: String, val regla: String, val severidad: Severidad, val detalle: String, val timestamp: Long,
)

@Entity(
    tableName = "visaciones",
    foreignKeys = [ForeignKey(MarcaEntity::class, ["id"], ["marcaId"], onDelete = ForeignKey.CASCADE)],
)
data class VisacionEntity(
    @PrimaryKey val marcaId: String, val supervisorId: String, val aprobada: Boolean, val timestamp: Long,
)

@Entity(
    tableName = "horas_extra",
    indices = [Index("usuarioId"), Index("estado")],
    foreignKeys = [ForeignKey(entity = MarcaEntity::class, parentColumns = ["id"], childColumns = ["marcaId"], onDelete = ForeignKey.CASCADE)]
)
data class HoraExtraEntity(
    @PrimaryKey val id: String,
    val usuarioId: String,
    val marcaId: String?,
    val fecha: Long,           // timestamp UTC del día solicitado
    val horas: Double,         // cantidad de horas solicitadas
    val motivo: String,
    val estado: EstadoHoraExtra, // PENDIENTE, APROBADA, RECHAZADA
    val supervisorId: String?,
    val timestampResolucion: Long?,
)
