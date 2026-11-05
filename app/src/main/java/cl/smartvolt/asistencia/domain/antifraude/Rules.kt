package cl.smartvolt.asistencia.domain.antifraude

import cl.smartvolt.asistencia.domain.geo.GeoUtils
import cl.smartvolt.asistencia.domain.model.*
import kotlin.math.min

/**
 * Cada control antifraude es una regla independiente, testeable por separado.
 *
 * PIPELINE DE REGLAS (RuleEngine):
 * 1. Se evalúan todas las reglas en orden
 * 2. Si alguna regla BLOQUEANTE falla (aprobada=false) => Estado RECHAZADA
 * 3. Si solo fallan reglas ADVERTENCIA => Estado SOSPECHOSA (requiere visión del capataz)
 * 4. Si todas pasan => Estado VALIDA
 *
 * ORDEN NORMAL de aplicación:
 * - MockLocationRule (R1): Bloquea marcas con GPS simulado
 * - SequenceRule (R2): Bloquea entradas duplicadas o salidas sin entrada
 * - GeofenceRule (R3): Advertencia si está fuera del perímetro (usa margen de precisión)
 * - BeaconRule (R4): Advertencia si no hay beacon detectado
 * - PresenciaFisicaRule (R5): Bloquea si ni GPS ni beacon confirman presencia
 */
interface FraudRule {
    val nombre: String
    fun evaluar(e: Evidencia): ResultadoRegla
}

/**
 * R1: Detección de ubicación simulada (Mock Location).
 *
 * IMPORTANTE: En Android, hay dos formas de detectar mock location:
 * - API 31+: Location.isMock
 * - API 30-: Location.isFromMockProvider (deprecado pero funcional)
 *
 * BLOQUEANTE: Si se detecta ubicación simulada, la marca se rechaza automáticamente.
 * Esto previene el uso de apps como "Mock Locations" para eludir el geofence.
 */
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

/**
 * R2: Validación de secuencia de marcas (entrada/salida).
 *
 * REGLAS:
 * - IN: Permite si la última marca NO es IN (no hay entrada abierta)
 * - OUT: Permite si la última marca es IN (hay entrada previa)
 *
 * BLOQUEANTE: Evita:
 * - Entradas duplicadas (dos IN seguidos)
 * - Salidas sin entrada previa
 */
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

/**
 * R3: Validación de geofence (ubicación dentro del perímetro de la faena).
 *
 * Funcionamiento:
 * - Calcula si la ubicación capturada está dentro del perímetro (circular o poligonal)
 * - Aplica un margen de tolerancia basado en la precisión del GPS
 * - El margen máximo es de 30 metros por seguridad
 *
 * NOTA: Usa GeoUtils.estaDentro() que soporta:
 * - Perímetros circulares (centro + radio)
 * - Perímetros poligonales
 *
 * ADVERTENCIA: Si el GPS no responde, la regla falla con mensaje de "Sin ubicación".
 * Esto se combina con BeaconRule en PresenciaFisicaRule.
 */
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

/**
 * R4: Validación de beacon BLE (iBeacon).
 *
 * Funcionamiento:
 * - Busca los UUIDs de los beacons autorizados de la faena
 * - Compara con las lecturas RSSI obtenidas del escaneo
 * - La lectura es válida si:
 *   1. El UUID coincide con uno autorizado
 *   2. El RSSI promedio >= RSSI mínimo configurado para ese beacon
 *
 * NOTA sobre el escaneo BLE:
 * - En Android 12+: requiere BLUETOOTH_SCAN (sin neverForLocation)
 * - En Android 11 e inferior: requiere permiso de ubicación
 * - El escaneo se hace durante 6 segundos (configurable según necesidad)
 *
 * ADVERTENCIA: Esta regla solo genera advertencia, no rechaza automáticamente.
 * Se combina con GeofenceRule en PresenciaFisicaRule.
 */
class BeaconRule : FraudRule {
    override val nombre = "BEACON"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        // Crear mapa de beacons autorizados por UUID (en mayúsculas para comparación insensible)
        val autorizadas = e.beaconsAutorizados.associateBy { it.uuid.uppercase() }

        // Verificar si alguna lectura BLE corresponde a un beacon autorizado con señal suficiente
        val ok = e.lecturasBle.any {
            val beaconEncontrado = autorizadas[it.uuid.uppercase()]
            // El beacon debe existir y tener RSSI >= al mínimo requerido
            beaconEncontrado != null && it.rssiPromedio >= beaconEncontrado.rssiMinimo
        }

        return ResultadoRegla(
            nombre, ok, Severidad.ADVERTENCIA,
            if (ok) "Beacon autorizado detectado" else "No se detectó un beacon autorizado",
        )
    }
}

/**
 * R5: Regla de presencia física (combinada).
 *
 * ESTA ES LA REGLA DECISIVA: UN USUARIO SOLO PUEDE MARCAR SI:
 * - El GPS confirma estar dentro del perímetro (GeofenceRule), O
 * - Un beacon autorizado confirma presencia física (BeaconRule)
 *
 * BLOQUEANTE: Ni GPS ni beacon pueden estar vacíos - se requiere al menos uno.
 * Esto previene eludir el sistema usando solo GPS o solo beacon.
 */
class PresenciaFisicaRule(
    private val geofence: GeofenceRule,
    private val beacon: BeaconRule,
) : FraudRule {
    override val nombre = "PRESENCIA_FISICA"
    override fun evaluar(e: Evidencia): ResultadoRegla {
        // Al menos uno de los dos debe aprobar
        val ok = geofence.evaluar(e).aprobada || beacon.evaluar(e).aprobada
        return ResultadoRegla(
            nombre, ok, Severidad.BLOQUEANTE,
            if (ok) "Presencia confirmada" else "Ni el GPS ni el beacon confirman presencia en la faena",
        )
    }
}

/**
 * R6: Detección de modo desarrollador activo.
 *
 * Cuando el modo desarrollador está activo en el dispositivo, se genera
 * una advertencia porque podría indicar que el usuario está usando herramientas
 * de depuración que podrían interferir con la aplicación.
 *
 * ADVERTENCIA: Esta regla por defecto NO está en el pipeline principal.
 * Si se activa explícitamente, puede convertir marcas válidas en sospechosas.
 */
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

/**
 * R7: Validación del reloj del dispositivo.
 *
 * Si la hora automática está desactivada, el usuario podría haber modificado
 * la hora del sistema y falsificado timestamps.
 *
 * ADVERTENCIA: La marca no se rechaza, pero se genera una advertencia.
 */
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