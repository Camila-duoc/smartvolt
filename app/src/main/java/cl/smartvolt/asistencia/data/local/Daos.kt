package cl.smartvolt.asistencia.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import cl.smartvolt.asistencia.domain.model.SyncEstado
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
    @Query("SELECT COUNT(*) FROM faenas") suspend fun contarFaenas(): Int
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
}

@Database(
    entities = [UsuarioEntity::class, FaenaEntity::class, FrenteEntity::class, BeaconEntity::class,
        MarcaEntity::class, LecturaBleEntity::class, EventoAntifraudeEntity::class, VisacionEntity::class],
    version = 1, exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun marcaDao(): MarcaDao
}
