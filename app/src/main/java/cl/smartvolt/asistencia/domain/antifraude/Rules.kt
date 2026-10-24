package cl.smartvolt.asistencia.domain.antifraude

import cl.smartvolt.asistencia.domain.geo.GeoUtils
import cl.smartvolt.asistencia.domain.model.*
import kotlin.math.min

/** Cada control antifraude es una regla independiente, testeable por separado. */
interface FraudRule {
    val nombre: String
    fun evaluar(e: Evidencia): ResultadoRegla
}

/** R1: ubicación simulada (Fake GPS). Bloqueante. */
class MockLocationRule : FraudRule {
    override val nombre = "MOCK_LOCATION"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val mock = e.ubicacion?.isMock == true
        return ResultadoRegla(
            nombre, !mock, Severidad.BLOQUEANTE,
            if (mock) "Ubicación simulada detectada (mock location)" else "Ubicación genuina",
        )
    }
}

/** R2: no hay salida sin entrada ni entrada duplicada. Bloqueante. */
class SequenceRule : FraudRule {
    override val nombre = "SECUENCIA"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val ok = when (e.tipo) {
            TipoMarca.IN -> e.ultimaMarca != TipoMarca.IN
            TipoMarca.OUT -> e.ultimaMarca == TipoMarca.IN
        }
        val detalle = when {
            ok -> "Secuencia correcta"
            e.tipo == TipoMarca.IN -> "Entrada duplicada: ya existe una entrada abierta"
            else -> "Salida sin entrada previa"
        }
        return ResultadoRegla(nombre, ok, Severidad.BLOQUEANTE, detalle)
    }
}

/** R3: posición dentro del perímetro de la faena, con margen según la precisión del GPS. */
class GeofenceRule(private val margenMaxM: Double = 30.0) : FraudRule {
    override val nombre = "GEOFENCE"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val u = e.ubicacion
            ?: return ResultadoRegla(nombre, false, Severidad.ADVERTENCIA, "Sin ubicación disponible")
        val margen = min(u.accuracyM.toDouble(), margenMaxM)
        val dentro = GeoUtils.estaDentro(e.perimetro, LatLon(u.lat, u.lon), margen)
        return ResultadoRegla(
            nombre, dentro, Severidad.ADVERTENCIA,
            if (dentro) "Dentro del perímetro" else "Fuera del perímetro de la faena",
        )
    }
}

/** R4: baliza BLE autorizada detectada con señal suficiente (presencia física). */
class BeaconRule : FraudRule {
    override val nombre = "BEACON"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val autorizadas = e.beaconsAutorizados.associateBy { it.uuid.uppercase() }
        val ok = e.lecturasBle.any {
            val b = autorizadas[it.uuid.uppercase()]
            b != null && it.rssiPromedio >= b.rssiMinimo
        }
        return ResultadoRegla(
            nombre, ok, Severidad.ADVERTENCIA,
            if (ok) "Beacon autorizado detectado" else "No se detectó un beacon autorizado",
        )
    }
}

/** R5: al menos un factor (GPS o BLE) debe confirmar presencia; si ninguno, se rechaza. */
class PresenciaFisicaRule(
    private val geofence: GeofenceRule,
    private val beacon: BeaconRule,
) : FraudRule {
    override val nombre = "PRESENCIA_FISICA"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val ok = geofence.evaluar(e).aprobada || beacon.evaluar(e).aprobada
        return ResultadoRegla(
            nombre, ok, Severidad.BLOQUEANTE,
            if (ok) "Presencia confirmada" else "Ni el GPS ni el beacon confirman presencia en la faena",
        )
    }
}

/** R6: modo desarrollador activo = riesgo; no bloquea pero deja la marca como sospechosa. */
class DeveloperModeRule : FraudRule {
    override val nombre = "MODO_DESARROLLADOR"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val activo = e.estadoSistema.modoDesarrollador
        return ResultadoRegla(
            nombre, !activo, Severidad.ADVERTENCIA,
            if (activo) "Modo desarrollador activo" else "Modo desarrollador desactivado",
        )
    }
}

/** R7: hora automática desactivada = el reloj pudo manipularse. */
class ClockRule : FraudRule {
    override val nombre = "RELOJ"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        val ok = e.estadoSistema.horaAutomatica
        return ResultadoRegla(
            nombre, ok, Severidad.ADVERTENCIA,
            if (ok) "Hora automática activa" else "Hora automática desactivada",
        )
    }
}
