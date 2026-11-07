package cl.smartvolt.asistencia.viewmodel

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.smartvolt.asistencia.modelo.BeaconEntity
import cl.smartvolt.asistencia.modelo.ConfigDao
import cl.smartvolt.asistencia.modelo.EventoAntifraudeEntity
import cl.smartvolt.asistencia.modelo.FaenaEntity
import cl.smartvolt.asistencia.modelo.FrenteEntity
import cl.smartvolt.asistencia.modelo.HoraExtraDao
import cl.smartvolt.asistencia.modelo.HoraExtraEntity
import cl.smartvolt.asistencia.modelo.LecturaBleEntity
import cl.smartvolt.asistencia.modelo.MarcaDao
import cl.smartvolt.asistencia.modelo.MarcaEntity
import cl.smartvolt.asistencia.modelo.UbicacionCompartidaEntity
import cl.smartvolt.asistencia.modelo.UsuarioEntity
import cl.smartvolt.asistencia.modelo.VisacionEntity

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

/** Migración de la versión 3 a la 4: ubicación del mapa compartida por todos los roles. */
val MIGRACION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ubicacion_compartida (
                id INTEGER NOT NULL,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                actualizadoPor TEXT NOT NULL,
                actualizadoEn INTEGER NOT NULL,
                PRIMARY KEY (id)
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities = [UsuarioEntity::class, FaenaEntity::class, FrenteEntity::class, BeaconEntity::class,
        MarcaEntity::class, LecturaBleEntity::class, EventoAntifraudeEntity::class, VisacionEntity::class,
        HoraExtraEntity::class, UbicacionCompartidaEntity::class],
    version = 4, exportSchema = false,
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
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
