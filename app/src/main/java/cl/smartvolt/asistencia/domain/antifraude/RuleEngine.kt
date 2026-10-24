package cl.smartvolt.asistencia.domain.antifraude

import cl.smartvolt.asistencia.domain.model.*

/**
 * Pipeline de reglas. Si falla alguna BLOQUEANTE => RECHAZADA.
 * Si solo fallan ADVERTENCIA => SOSPECHOSA (requiere visación del capataz).
 * Si todas pasan => VALIDA.
 */
class RuleEngine(private val reglas: List<FraudRule>) {

    fun evaluar(e: Evidencia): Veredicto {
        val resultados = reglas.map { it.evaluar(e) }
        val bloqueantes = resultados.filter { !it.aprobada && it.severidad == Severidad.BLOQUEANTE }
        if (bloqueantes.isNotEmpty()) {
            return Veredicto(EstadoMarca.RECHAZADA, bloqueantes.joinToString("; ") { it.detalle }, resultados)
        }
        val advertencias = resultados.filter { !it.aprobada && it.severidad == Severidad.ADVERTENCIA }
        if (advertencias.isNotEmpty()) {
            return Veredicto(EstadoMarca.SOSPECHOSA, advertencias.joinToString("; ") { it.detalle }, resultados)
        }
        return Veredicto(EstadoMarca.VALIDA, "Marca válida", resultados)
    }

    companion object {
        fun porDefecto(): RuleEngine {
            val geofence = GeofenceRule()
            val beacon = BeaconRule()
            return RuleEngine(
                listOf(
                    MockLocationRule(), SequenceRule(), geofence, beacon,
                    PresenciaFisicaRule(geofence, beacon), DeveloperModeRule(), ClockRule(),
                ),
            )
        }
    }
}
