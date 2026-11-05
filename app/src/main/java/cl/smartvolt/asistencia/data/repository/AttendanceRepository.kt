package cl.smartvolt.asistencia.data.repository

import android.content.Context
import androidx.room.withTransaction
import cl.smartvolt.asistencia.data.local.*
import cl.smartvolt.asistencia.data.remote.*
import cl.smartvolt.asistencia.domain.model.*
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contrato de acceso a datos de asistencia. Permite sustituir la implementación
 * real (Room + Retrofit) por dobles de prueba sin instanciar Android.
 */
interface AttendanceRepository {
    suspend fun usuario(id: String): UsuarioEntity? = TODO("No implementado en este doble de prueba")
    suspend fun usuariosDeFaena(faenaId: String): List<UsuarioEntity> = TODO("No implementado en este doble de prueba")
    suspend fun faena(id: String): FaenaEntity? = TODO("No implementado en este doble de prueba")
    suspend fun perimetro(faenaId: String): Perimetro? = TODO("No implementado en este doble de prueba")
    suspend fun beacons(faenaId: String): List<BeaconAutorizado> = TODO("No implementado en este doble de prueba")
    suspend fun ultimaMarcaValida(usuarioId: String): TipoMarca? = TODO("No implementado en este doble de prueba")
    suspend fun ultimoHash(usuarioId: String): String = TODO("No implementado en este doble de prueba")

    fun historial(usuarioId: String): Flow<List<MarcaEntity>> = TODO("No implementado en este doble de prueba")
    fun marcasDeFaena(faenaId: String): Flow<List<MarcaEntity>> = TODO("No implementado en este doble de prueba")
    fun visaciones(): Flow<List<VisacionEntity>> = TODO("No implementado en este doble de prueba")
    fun contarPendientes(): Flow<Int> = TODO("No implementado en este doble de prueba")
    fun todasLasMarcas(): Flow<List<MarcaEntity>> = TODO("No implementado en este doble de prueba")

    suspend fun guardar(m: MarcaEntity, lecturas: List<LecturaBle>, v: Veredicto, ahoraMs: Long): Unit =
        TODO("No implementado en este doble de prueba")

    suspend fun visar(marcaId: String, supervisorId: String, aprobada: Boolean, ahoraMs: Long): Unit =
        TODO("No implementado en este doble de prueba")

    suspend fun marcaPorId(id: String): MarcaEntity? = TODO("No implementado en este doble de prueba")
    suspend fun visacionPorMarca(marcaId: String): VisacionEntity? = TODO("No implementado en este doble de prueba")

    suspend fun sincronizarMarcas(): Unit = TODO("No implementado en este doble de prueba")
    suspend fun sincronizarConfiguracion(): Unit = TODO("No implementado en este doble de prueba")
    suspend fun aplicarConfiguracion(c: ConfiguracionDto): Unit = TODO("No implementado en este doble de prueba")
    suspend fun cargarSemillaSiVacio(): Unit = TODO("No implementado en este doble de prueba")

    suspend fun solicitarHoraExtra(
        usuarioId: String,
        marcaId: String?,
        fecha: Long,
        horas: Double,
        motivo: String
    ): HoraExtraEntity = TODO("No implementado en este doble de prueba")

    suspend fun horasExtraDeUsuario(usuarioId: String): List<HoraExtraEntity> =
        TODO("No implementado en este doble de prueba")

    suspend fun horasExtraDeUsuarios(usuarioIds: List<String>): List<HoraExtraEntity> =
        TODO("No implementado en este doble de prueba")

    suspend fun horasExtraPendientes(): List<HoraExtraEntity> =
        TODO("No implementado en este doble de prueba")

    suspend fun aprobarRechazarHoraExtra(id: String, supervisorId: String, aprobada: Boolean): Unit =
        TODO("No implementado en este doble de prueba")

    suspend fun horasExtraPorRango(desde: Long, hasta: Long): List<HoraExtraEntity> =
        TODO("No implementado en este doble de prueba")

    suspend fun resumenHorasExtraUsuario(usuarioId: String): List<ResumenHoraExtraEstado> =
        TODO("No implementado en este doble de prueba")

    suspend fun ultimasHorasExtraUsuario(usuarioId: String): List<HoraExtraEntity> =
        TODO("No implementado en este doble de prueba")

    suspend fun todasLasHorasExtra(): List<HoraExtraEntity> =
        TODO("No implementado en este doble de prueba")
}

@Singleton
class RoomAttendanceRepository @Inject constructor(
    private val db: AppDatabase,
    private val api: ApiService,
    @ApplicationContext private val context: Context,
) : AttendanceRepository {
    private val config get() = db.configDao()
    private val marcas get() = db.marcaDao()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // ---------- Lectura local (fuente de verdad: Room) ----------
    override suspend fun usuario(id: String) = config.usuario(id)

    override suspend fun usuariosDeFaena(faenaId: String) = config.usuariosDeFaena(faenaId)

    override suspend fun faena(id: String) = config.faena(id)

    override suspend fun perimetro(faenaId: String): Perimetro? {
        val f = config.faena(faenaId) ?: return null
        val poly = f.poligonoJson?.let { json.decodeFromString<List<List<Double>>>(it) }?.map { LatLon(it[0], it[1]) }
        return Perimetro(f.tipoPerimetro, LatLon(f.centroLat, f.centroLon), f.radioM, poly)
    }

    override suspend fun beacons(faenaId: String) =
        config.beacons(faenaId).map { BeaconAutorizado(it.uuid, it.rssiMinimo, it.frenteId) }

    override suspend fun ultimaMarcaValida(usuarioId: String) = marcas.ultimaNoRechazada(usuarioId)?.tipo
    override suspend fun ultimoHash(usuarioId: String) = marcas.ultimoHash(usuarioId) ?: "GENESIS"
    override fun historial(usuarioId: String): Flow<List<MarcaEntity>> = marcas.historial(usuarioId)
    override fun marcasDeFaena(faenaId: String): Flow<List<MarcaEntity>> = marcas.marcasDeFaena(faenaId)
    override fun visaciones(): Flow<List<VisacionEntity>> = marcas.visaciones()
    override fun contarPendientes(): Flow<Int> = marcas.contarPendientes()

    /** Guarda marca, lecturas BLE y eventos de auditoría en una sola transacción. */
    override suspend fun guardar(m: MarcaEntity, lecturas: List<LecturaBle>, v: Veredicto, ahoraMs: Long) {
        db.withTransaction {
            marcas.insertMarca(m)
            marcas.insertLecturas(lecturas.map { LecturaBleEntity(marcaId = m.id, uuid = it.uuid, rssiPromedio = it.rssiPromedio, muestras = it.muestras) })
            marcas.insertEventos(
                v.resultados.filter { !it.aprobada }
                    .map { EventoAntifraudeEntity(marcaId = m.id, regla = it.regla, severidad = it.severidad, detalle = it.detalle, timestamp = ahoraMs) },
            )
        }
    }

    override suspend fun visar(marcaId: String, supervisorId: String, aprobada: Boolean, ahoraMs: Long) =
        marcas.insertVisacion(VisacionEntity(marcaId, supervisorId, aprobada, ahoraMs))

    override suspend fun marcaPorId(id: String) = marcas.marcaPorId(id)
    override suspend fun visacionPorMarca(marcaId: String) = marcas.visacionPorMarca(marcaId)
    override fun todasLasMarcas(): Flow<List<MarcaEntity>> = marcas.todasLasMarcas()

    // ---------- Sincronización ----------
    /** Envía marcas pendientes. Lanza excepción si no hay red (el Worker reintenta). */
    override suspend fun sincronizarMarcas() {
        val pend = marcas.pendientes()
        if (pend.isEmpty()) return
        val lecturas = marcas.lecturasDe(pend.map { it.id }).groupBy { it.marcaId }
        val dtos = pend.map { m ->
            MarcaDto(
                m.id, m.usuarioId, m.faenaId, m.tipo.name, m.lat, m.lon, m.accuracy, m.timestampUtc,
                m.elapsedRealtimeMs, m.estado.name, m.motivo, m.hashPrevio, m.hash,
                lecturas[m.id].orEmpty().map { LecturaDto(it.uuid, it.rssiPromedio, it.muestras) },
            )
        }
        val resp = api.enviarLote(LoteRequest(pend.firstOrNull()?.usuarioId?.let { config.usuario(it)?.deviceId }, dtos))
        val ok = resp.resultados.filter { it.aceptada }.map { it.id }
        val error = resp.resultados.filter { !it.aceptada }.map { it.id }
        if (ok.isNotEmpty()) marcas.actualizarSync(ok, SyncEstado.ENVIADA)
        if (error.isNotEmpty()) marcas.actualizarSync(error, SyncEstado.ERROR)
    }

    override suspend fun sincronizarConfiguracion() = aplicarConfiguracion(api.configuracion(null))

    override suspend fun aplicarConfiguracion(c: ConfiguracionDto) {
        db.withTransaction {
            config.upsertFaenas(c.faenas.map {
                FaenaEntity(it.id, it.nombre, TipoPerimetro.valueOf(it.tipoPerimetro), it.centroLat, it.centroLon, it.radioM,
                    it.poligono?.let { p -> json.encodeToString(p) })
            })
            config.upsertFrentes(c.frentes.map { FrenteEntity(it.id, it.faenaId, it.nombre) })
            config.upsertBeacons(c.beacons.map { BeaconEntity(it.uuid.uppercase(), it.major, it.minor, it.faenaId, it.frenteId, it.rssiMinimo) })
            config.upsertUsuarios(c.usuarios.map { UsuarioEntity(it.id, Rol.valueOf(it.rol), it.faenaId, it.frenteId, it.deviceId) })
        }
    }

    // ---------- Datos sintéticos (MVP académico) ----------
    /** Carga assets/seed.json la primera vez, para operar sin backend. */
    override suspend fun cargarSemillaSiVacio() {
        if (config.contarFaenas() > 0) return
        val seed = context.assets.open("seed.json").bufferedReader().use { json.decodeFromString<SeedDto>(it.readText()) }
        aplicarConfiguracion(ConfiguracionDto(seed.faenas, seed.frentes, seed.beacons, seed.usuarios))
        val faenaDe = seed.usuarios.associate { it.id to it.faenaId }
        // La semilla trae fechas fijas. Para que el calendario y los reportes
        // muestren actividad reciente en cualquier fecha de evaluación, se
        // desplazan todas las marcas manteniendo su orden cronológico.
        val ahora = System.currentTimeMillis()
        val ultimaMarca = seed.marcas.maxOfOrNull { it.timestampUtc } ?: ahora
        val desplazamiento = (ahora - ultimaMarca).coerceAtLeast(0L)
        marcas.insertMarcas(seed.marcas.map {
            MarcaEntity(
                it.id, it.usuarioId, faenaDe[it.usuarioId].orEmpty(), TipoMarca.valueOf(it.tipo), it.lat, it.lon, it.accuracy,
                it.timestampUtc + desplazamiento, 0L, EstadoMarca.valueOf(it.estado), it.motivo, SyncEstado.valueOf(it.syncEstado), "", "",
            )
        })
    }

    // ---------- Horas extras ----------

    override suspend fun solicitarHoraExtra(
        usuarioId: String,
        marcaId: String?,
        fecha: Long,
        horas: Double,
        motivo: String
    ): HoraExtraEntity {
        val id = UUID.randomUUID().toString()
        val h = HoraExtraEntity(
            id = id,
            usuarioId = usuarioId,
            marcaId = marcaId,
            fecha = fecha,
            horas = horas,
            motivo = motivo,
            estado = EstadoHoraExtra.PENDIENTE,
            supervisorId = null,
            timestampResolucion = null
        )
        db.horaExtraDao().insertarHoraExtra(h)
        return h
    }

    override suspend fun horasExtraDeUsuario(usuarioId: String): List<HoraExtraEntity> =
        db.horaExtraDao().horasExtraDeUsuario(usuarioId)

    override suspend fun horasExtraDeUsuarios(usuarioIds: List<String>): List<HoraExtraEntity> =
        if (usuarioIds.isEmpty()) emptyList() else db.horaExtraDao().horasExtraDeUsuarios(usuarioIds)

    override suspend fun horasExtraPendientes(): List<HoraExtraEntity> =
        db.horaExtraDao().horasExtraPorEstado(EstadoHoraExtra.PENDIENTE)

    override suspend fun aprobarRechazarHoraExtra(
        id: String,
        supervisorId: String,
        aprobada: Boolean
    ) {
        val h = db.horaExtraDao().horaExtraPorId(id) ?: return
        val ahora = System.currentTimeMillis()
        val actualizada = h.copy(
            estado = if (aprobada) EstadoHoraExtra.APROBADA else EstadoHoraExtra.RECHAZADA,
            supervisorId = supervisorId,
            timestampResolucion = ahora
        )
        db.horaExtraDao().actualizarHoraExtra(actualizada)
    }

    override suspend fun horasExtraPorRango(desde: Long, hasta: Long): List<HoraExtraEntity> =
        db.horaExtraDao().horasExtraPorRango(desde, hasta)

    /** Resumen de horas extra de un usuario (cantidad y total de horas por estado). */
    override suspend fun resumenHorasExtraUsuario(usuarioId: String): List<ResumenHoraExtraEstado> =
        db.horaExtraDao().resumenHorasExtraUsuario(usuarioId)

    /** Últimas 10 solicitudes de horas extra de un usuario. */
    override suspend fun ultimasHorasExtraUsuario(usuarioId: String): List<HoraExtraEntity> =
        db.horaExtraDao().ultimasHorasExtraUsuario(usuarioId)

    /** Todas las horas extra registradas (para supervisión del capataz). */
    override suspend fun todasLasHorasExtra(): List<HoraExtraEntity> =
        db.horaExtraDao().todasLasHorasExtra()
}
