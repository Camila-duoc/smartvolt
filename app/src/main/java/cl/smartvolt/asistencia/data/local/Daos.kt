package cl.smartvolt.asistencia.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
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
    @Query("SELECT * FROM usuarios WHERE faenaId = :faenaId") suspend fun usuariosDeFaena(faenaId: String): List<UsuarioEntity>
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

/** Migración de la versión 1 a la 2: crea la tabla horas_extra. */
val MIGRACION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS horas_extra (
                id TEXT NOT NULL,
                usuarioId TEXT NOT NULL,
                marcaId TEXT,
                fecha INTEGER NOT NULL,
                horas REAL NOT NULL,
                motivo TEXT NOT NULL,
                estado TEXT NOT NULL,
                supervisorId TEXT,
                timestampResolucion INTEGER,
                PRIMARY KEY (id),
                FOREIGN KEY (marcaId) REFERENCES marcas (id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_horas_extra_usuarioId ON horas_extra (usuarioId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_horas_extra_estado ON horas_extra (estado)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_horas_extra_marcaId ON horas_extra (marcaId)")
    }
}

/** Migración de la versión 2 a la 3: índice para la FK marcaId de horas_extra. */
val MIGRACION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE INDEX IF NOT EXISTS index_horas_extra_marcaId ON horas_extra (marcaId)")
    }
}

@Database(
    entities = [UsuarioEntity::class, FaenaEntity::class, FrenteEntity::class, BeaconEntity::class,
        MarcaEntity::class, LecturaBleEntity::class, EventoAntifraudeEntity::class, VisacionEntity::class,
        HoraExtraEntity::class],
    version = 3, exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun marcaDao(): MarcaDao
    abstract fun horaExtraDao(): HoraExtraDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "smartvolt.db")
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
