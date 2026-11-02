package cl.smartvolt.asistencia.domain.usecase

import cl.smartvolt.asistencia.data.local.MarcaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Genera el CSV de marcas para exportación administrativa (HU-ADM). */
object ExportarMarcasCsv {

    private const val SEPARADOR = ';'

    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    val encabezado: String = listOf(
        "id", "usuarioId", "faenaId", "tipo", "fechaHora", "timestampUtc",
        "lat", "lon", "accuracy", "estado", "motivo", "syncEstado", "hashPrevio", "hash",
    ).joinToString(SEPARADOR.toString())

    fun construir(marcas: List<MarcaEntity>): String {
        val sb = StringBuilder(encabezado).append('\n')
        marcas.forEach { m ->
            sb.append(campo(m.id)).append(SEPARADOR)
                .append(campo(m.usuarioId)).append(SEPARADOR)
                .append(campo(m.faenaId)).append(SEPARADOR)
                .append(campo(m.tipo.name)).append(SEPARADOR)
                .append(campo(formatoFecha.format(Date(m.timestampUtc)))).append(SEPARADOR)
                .append(m.timestampUtc).append(SEPARADOR)
                .append(m.lat?.toString().orEmpty()).append(SEPARADOR)
                .append(m.lon?.toString().orEmpty()).append(SEPARADOR)
                .append(m.accuracy?.toString().orEmpty()).append(SEPARADOR)
                .append(campo(m.estado.name)).append(SEPARADOR)
                .append(campo(m.motivo)).append(SEPARADOR)
                .append(campo(m.syncEstado.name)).append(SEPARADOR)
                .append(campo(m.hashPrevio)).append(SEPARADOR)
                .append(campo(m.hash))
                .append('\n')
        }
        return sb.toString()
    }

    private fun campo(valor: String?): String {
        val s = valor.orEmpty()
        val requiereComillas = s.contains(SEPARADOR) || s.contains('"') || s.contains('\n') || s.contains('\r')
        return if (requiereComillas) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }
}
