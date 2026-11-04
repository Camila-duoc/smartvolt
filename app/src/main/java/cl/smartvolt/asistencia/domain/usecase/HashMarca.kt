package cl.smartvolt.asistencia.domain.usecase

import java.security.MessageDigest

/** Datos mínimos de una marca necesarios para verificar su hash y su encadenamiento. */
data class MarcaHash(
    val id: String,
    val usuarioId: String,
    val tipo: String,
    val lat: Double?,
    val lon: Double?,
    val timestampUtc: Long,
    val estado: String,
    val hashPrevio: String,
    val hash: String,
)

/** Resultado de verificar la cadena de custodia de un conjunto de marcas. */
data class ResultadoCadena(
    val integra: Boolean,
    val verificadas: Int,
    val noVerificables: Int,
    val invalidas: List<String>,
)

/** Cálculo y verificación del hash encadenado de una marca (cadena de custodia criptográfica). */
object HashMarca {
    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    /**
     * Construye el hash de una marca encadenándolo con el hash previo.
     *
     * @param hashPrevio hash de la marca anterior del usuario ("GENESIS" si es la primera)
     * @param id UUID de la marca actual
     * @param usuarioId identificador del operario
     * @param tipo tipo de marca (ENTRADA / SALIDA)
     * @param lat latitud capturada (puede ser null)
     * @param lon longitud capturada (puede ser null)
     * @param ahora timestamp UTC en ms
     * @param estado estado resultante de la marca (VALIDA / SOSPECHOSA / RECHAZADA)
     */
    fun calcular(
        hashPrevio: String,
        id: String,
        usuarioId: String,
        tipo: String,
        lat: Double?,
        lon: Double?,
        ahora: Long,
        estado: String
    ): String = sha256("$hashPrevio|$id|$usuarioId|$tipo|$lat|$lon|$ahora|$estado")

    /** Recalcula el hash de una marca y lo compara con el almacenado. */
    fun verificar(m: MarcaHash): Boolean =
        m.hash.isNotBlank() && calcular(
            hashPrevio = m.hashPrevio,
            id = m.id,
            usuarioId = m.usuarioId,
            tipo = m.tipo,
            lat = m.lat,
            lon = m.lon,
            ahora = m.timestampUtc,
            estado = m.estado,
        ) == m.hash

    /**
     * Verifica la cadena completa: cada hash debe coincidir con su contenido y, además,
     * el `hashPrevio` debe apuntar al hash de la marca anterior del mismo usuario.
     *
     * Las marcas de la semilla no traen hash (`hash` vacío): se informan como no verificables
     * para no reportar una alteración que en realidad no existe.
     */
    fun verificarCadena(marcas: List<MarcaHash>): ResultadoCadena {
        val invalidas = mutableListOf<String>()
        var verificadas = 0
        var noVerificables = 0

        marcas.groupBy { it.usuarioId }.values.forEach { lista ->
            var previo: String? = null
            lista.sortedBy { it.timestampUtc }.forEach { m ->
                if (m.hash.isBlank()) {
                    noVerificables++
                    return@forEach
                }
                verificadas++
                val hashOk = verificar(m)
                val enlaceOk = previo == null || m.hashPrevio == previo
                if (!hashOk || !enlaceOk) invalidas += m.id
                previo = m.hash
            }
        }

        return ResultadoCadena(
            integra = invalidas.isEmpty(),
            verificadas = verificadas,
            noVerificables = noVerificables,
            invalidas = invalidas,
        )
    }
}
