package cl.smartvolt.asistencia.modelo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Upsert suspend fun upsertUsuarios(l: List<UsuarioEntity>)
    @Upsert suspend fun upsertFaenas(l: List<FaenaEntity>)
    @Upsert suspend fun upsertFrentes(l: List<FrenteEntity>)
    @Upsert suspend fun upsertBeacons(l: List<BeaconEntity>)
    @Query("SELECT * FROM usuarios WHERE id = :id") suspend fun usuario(id: String): UsuarioEntity?
    @Query("SELECT * FROM faenas WHERE id = :id") suspend fun faena(id: String): FaenaEntity?
    @Query("SELECT * FROM beacons WHERE faenaId = :faenaId") suspend fun beacons(faenaId: String): List<BeaconEntity>
    @Query("SELECT * FROM usuarios WHERE faenaId = :faenaId") suspend fun usuariosDeFaena(faenaId: String): List<UsuarioEntity>
    @Query("SELECT COUNT(*) FROM faenas") suspend fun contarFaenas(): Int

    /** Ubicación elegida por administración; hay como máximo una fila. */
    @Query("SELECT * FROM ubicacion_compartida WHERE id = 1")
    suspend fun ubicacionCompartida(): UbicacionCompartidaEntity?

    @Upsert suspend fun upsertUbicacionCompartida(u: UbicacionCompartidaEntity)
}

@Dao
interface MarcaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMarca(m: MarcaEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMarcas(l: List<MarcaEntity>)
    @Insert suspend fun insertLecturas(l: List<LecturaBleEntity>)
    @Insert suspend fun insertEventos(l: List<EventoAntifraudeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVisacion(v: VisacionEntity)

    @Query("SELECT * FROM marcas WHERE usuarioId = :u AND estado != 'RECHAZADA' ORDER BY timestampUtc DESC LIMIT 1")
    suspend fun ultimaNoRechazada(u: String): MarcaEntity?

    @Query("SELECT hash FROM marcas WHERE usuarioId = :u ORDER BY timestampUtc DESC LIMIT 1")
    suspend fun ultimoHash(u: String): String?

    @Query("SELECT * FROM marcas WHERE syncEstado = 'PENDIENTE' OR syncEstado = 'ERROR' ORDER BY timestampUtc")
    suspend fun pendientes(): List<MarcaEntity>

    @Query("SELECT * FROM lecturas_ble WHERE marcaId IN (:ids)")
    suspend fun lecturasDe(ids: List<String>): List<LecturaBleEntity>

    @Query("UPDATE marcas SET syncEstado = :e WHERE id IN (:ids)")
    suspend fun actualizarSync(ids: List<String>, e: SyncEstado)

    @Query("SELECT * FROM marcas WHERE usuarioId = :u ORDER BY timestampUtc DESC")
    fun historial(u: String): Flow<List<MarcaEntity>>

    @Query("SELECT * FROM marcas WHERE faenaId = :f ORDER BY timestampUtc DESC")
    fun marcasDeFaena(f: String): Flow<List<MarcaEntity>>

    @Query("SELECT * FROM visaciones")
    fun visaciones(): Flow<List<VisacionEntity>>

    @Query("SELECT COUNT(*) FROM marcas WHERE syncEstado != 'ENVIADA'")
    fun contarPendientes(): Flow<Int>

    @Query("SELECT * FROM marcas WHERE id = :id")
    suspend fun marcaPorId(id: String): MarcaEntity?

    @Query("SELECT * FROM visaciones WHERE marcaId = :id")
    suspend fun visacionPorMarca(id: String): VisacionEntity?

    @Query("SELECT * FROM marcas ORDER BY timestampUtc DESC")
    fun todasLasMarcas(): Flow<List<MarcaEntity>>

    /** Consultas por rango de fechas para el calendario */
    @Query("SELECT * FROM marcas WHERE usuarioId = :u AND timestampUtc >= :desde AND timestampUtc <= :hasta ORDER BY timestampUtc DESC")
    fun historialPorRango(u: String, desde: Long, hasta: Long): Flow<List<MarcaEntity>>

    @Query("SELECT * FROM marcas WHERE faenaId = :f AND timestampUtc >= :desde AND timestampUtc <= :hasta ORDER BY timestampUtc DESC")
    fun marcasDeFaenaPorRango(f: String, desde: Long, hasta: Long): Flow<List<MarcaEntity>>
}

@Dao
interface HoraExtraDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarHoraExtra(h: HoraExtraEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertarHorasExtra(l: List<HoraExtraEntity>)
    @androidx.room.Update suspend fun actualizarHoraExtra(h: HoraExtraEntity)

    @Query("SELECT * FROM horas_extra WHERE usuarioId = :u ORDER BY fecha DESC")
    suspend fun horasExtraDeUsuario(u: String): List<HoraExtraEntity>

    @Query("SELECT * FROM horas_extra WHERE estado = :e ORDER BY fecha DESC")
    suspend fun horasExtraPorEstado(e: EstadoHoraExtra): List<HoraExtraEntity>

    @Query("SELECT * FROM horas_extra WHERE id = :id")
    suspend fun horaExtraPorId(id: String): HoraExtraEntity?

    @Query("SELECT * FROM horas_extra WHERE fecha >= :desde AND fecha <= :hasta ORDER BY fecha DESC")
    suspend fun horasExtraPorRango(desde: Long, hasta: Long): List<HoraExtraEntity>

    @Query("SELECT * FROM horas_extra WHERE usuarioId IN (:ids) ORDER BY fecha DESC")
    suspend fun horasExtraDeUsuarios(ids: List<String>): List<HoraExtraEntity>

    @Query("SELECT * FROM horas_extra ORDER BY fecha DESC")
    suspend fun todasLasHorasExtra(): List<HoraExtraEntity>

    /** Horas extras de un usuario agrupadas por estado para el resumen del operario. */
    @Query("SELECT estado, COUNT(*) as cantidad, SUM(horas) as totalHoras FROM horas_extra WHERE usuarioId = :u GROUP BY estado")
    suspend fun resumenHorasExtraUsuario(u: String): List<ResumenHoraExtraEstado>

    @Query("SELECT * FROM horas_extra WHERE usuarioId = :u ORDER BY fecha DESC LIMIT 10")
    suspend fun ultimasHorasExtraUsuario(u: String): List<HoraExtraEntity>

    @Query("DELETE FROM horas_extra WHERE id = :id")
    suspend fun eliminarHoraExtra(id: String)
}

/** Resultado de agrupación de horas extra por estado. */
data class ResumenHoraExtraEstado(
    val estado: EstadoHoraExtra,
    val cantidad: Int,
    val totalHoras: Double
)
