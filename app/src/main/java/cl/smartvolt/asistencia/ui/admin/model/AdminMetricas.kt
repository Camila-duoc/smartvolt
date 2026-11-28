package cl.smartvolt.asistencia.ui.admin.model

import cl.smartvolt.asistencia.modelo.EstadoMarca
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Marca sospechosa o rechazada, para la lista de alertas (solo lectura) del administrador. */
data class AlertaResumen(
    val marcaId: String,
    val usuarioId: String,
    val faenaId: String,
    val estado: EstadoMarca,
    val motivo: String,
    val timestampUtc: Long,
)

/** Métricas y resumen administrativo para el panel de administración. */
data class AdminMetricas(
    val totalMarcas: Int = 0,
    val marcasValidas: Int = 0,
    val marcasSospechosas: Int = 0,
    val marcasRechazadas: Int = 0,
    val marcasPendientesSync: Int = 0,
    val marcasPorUsuario: Map<String, Int> = emptyMap(),
    val marcasPorEstado: Map<EstadoMarca, Int> = emptyMap(),
    val marcasPorFaena: Map<String, Int> = emptyMap(),
    val horasExtraTotales: Int = 0,
    val horasExtraPendientes: Int = 0,
    val horasExtraAprobadas: Int = 0,
    val horasExtraAprobadasHoras: Double = 0.0,
    val horasExtraRechazadas: Int = 0,
    /** Porcentaje de marcas sospechosas o rechazadas sobre el total de cada faena. */
    val noValidasPorFaenaPct: Map<String, Double> = emptyMap(),
    /** Últimas marcas sospechosas o rechazadas (más recientes primero). */
    val alertas: List<AlertaResumen> = emptyList(),
    /** Resultado de verificar la cadena de hashes (null si no hay marcas verificables). */
    val cadenaIntegra: Boolean? = null,
    val marcasVerificadas: Int = 0,
    val marcasNoVerificables: Int = 0,
    val marcasAlteradas: Int = 0,
    val cargando: Boolean = false,
    val exportando: Boolean = false,
    val error: String? = null,
)

/** Líneas de texto del reporte; el PDF se arma con estas mismas cifras. */
fun AdminMetricas.comoLineas(ahoraMs: Long = System.currentTimeMillis()): List<String> = buildList {
    val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    add("Generado: ${fecha.format(Date(ahoraMs))}")
    add("")
    add("MARCAS")
    add("Total de marcas: $totalMarcas")
    add("Válidas: $marcasValidas")
    add("Sospechosas: $marcasSospechosas")
    add("Rechazadas: $marcasRechazadas")
    add("Pendientes de sincronizar: $marcasPendientesSync")
    add("")
    add("HORAS EXTRA")
    add("Solicitudes: $horasExtraTotales (pendientes: $horasExtraPendientes, aprobadas: $horasExtraAprobadas, rechazadas: $horasExtraRechazadas)")
    add("Horas aprobadas: $horasExtraAprobadasHoras h")
    add("")
    add("MARCAS NO VÁLIDAS POR FAENA")
    noValidasPorFaenaPct.entries.sortedBy { it.key }.forEach { (faena, pct) ->
        add("$faena: ${"%.1f".format(Locale.US, pct)} % (${marcasPorFaena[faena] ?: 0} marcas)")
    }
    add("")
    add("ALERTAS RECIENTES")
    if (alertas.isEmpty()) add("Sin alertas")
    alertas.forEach { a ->
        val motivoCorto = if (a.motivo.isBlank()) "" else " · ${a.motivo.take(60)}"
        add("${fecha.format(Date(a.timestampUtc))} · ${a.usuarioId} · ${a.faenaId} · ${a.estado.name}$motivoCorto")
    }
    add("")
    add("REGISTRO INALTERABLE")
    add(
        when {
            cadenaIntegra == null -> "Sin marcas verificables (semilla sin hash)"
            cadenaIntegra -> "Cadena de hashes íntegra ($marcasVerificadas marcas verificadas)"
            else -> "Cadena alterada: $marcasAlteradas de $marcasVerificadas marcas"
        }
    )
    if (marcasNoVerificables > 0) {
        add("Marcas no verificables (sin hash): $marcasNoVerificables")
    }
}
