package cl.smartvolt.asistencia.domain.usecase

import java.security.MessageDigest

/** Cálculo del hash encadenado de una marca (cadena de custodia criptográfica). */
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
}
