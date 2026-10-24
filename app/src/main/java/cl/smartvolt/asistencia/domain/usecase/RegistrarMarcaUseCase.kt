package cl.smartvolt.asistencia.domain.usecase

import android.os.SystemClock
import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.antifraude.RuleEngine
import cl.smartvolt.asistencia.domain.model.*
import cl.smartvolt.asistencia.sensors.BleScanner
import cl.smartvolt.asistencia.sensors.LocationProvider
import cl.smartvolt.asistencia.sensors.SystemStateProvider
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

/** Reloj de referencia. TODO(MVP+): anclar a la hora del servidor en cada sync y persistir el ancla. */
class TrustedClock @Inject constructor() {
    fun nowUtcMs(): Long = System.currentTimeMillis()
    fun elapsedMs(): Long = SystemClock.elapsedRealtime()
}

class RegistrarMarcaUseCase @Inject constructor(
    private val repo: AttendanceRepository,
    private val location: LocationProvider,
    private val ble: BleScanner,
    private val system: SystemStateProvider,
    private val engine: RuleEngine,
    private val clock: TrustedClock,
) {
    suspend operator fun invoke(usuarioId: String, faenaId: String, tipo: TipoMarca): Veredicto {
        val perimetro = repo.perimetro(faenaId) ?: error("Faena $faenaId sin perímetro cargado")
        val beacons = repo.beacons(faenaId)

        val ubicacion = location.obtener(perimetro.centro)
        val lecturas = ble.escanear(beacons)
        val ahora = clock.nowUtcMs()

        val evidencia = Evidencia(
            usuarioId, faenaId, tipo, ubicacion, lecturas, system.estado(),
            ahora, clock.elapsedMs(), repo.ultimaMarcaValida(usuarioId), perimetro, beacons,
        )
        val veredicto = engine.evaluar(evidencia)

        val id = UUID.randomUUID().toString()
        val previo = repo.ultimoHash(usuarioId)
        val hash = sha256("$previo|$id|$usuarioId|$tipo|${ubicacion?.lat}|${ubicacion?.lon}|$ahora|${veredicto.estado}")
        repo.guardar(
            MarcaEntity(
                id, usuarioId, faenaId, tipo, ubicacion?.lat, ubicacion?.lon, ubicacion?.accuracyM?.toDouble(),
                ahora, evidencia.elapsedRealtimeMs, veredicto.estado, veredicto.motivo, SyncEstado.PENDIENTE, previo, hash,
            ),
            lecturas, veredicto, ahora,
        )
        return veredicto
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
