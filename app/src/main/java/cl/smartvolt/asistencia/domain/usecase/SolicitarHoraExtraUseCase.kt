package cl.smartvolt.asistencia.domain.usecase

import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import javax.inject.Inject

class SolicitarHoraExtraUseCase @Inject constructor(
    private val repo: AttendanceRepository
) {
    companion object {
        /** Tope de seguridad por solicitud (evita errores de tipeo y abusos). */
        const val MAX_HORAS = 12.0

        /** Largo mínimo de la justificación para que la solicitud sea trazable. */
        const val MIN_MOTIVO = 10

        /** Representación legible del tope, para mostrarla en la UI. */
        val MAX_HORAS_TEXTO: String =
            if (MAX_HORAS % 1.0 == 0.0) MAX_HORAS.toInt().toString() else MAX_HORAS.toString()

        /**
         * Valida el texto tal como lo escribe el usuario (puede venir vacío,
         * con coma decimal o con caracteres no numéricos).
         */
        fun validarHorasTexto(texto: String): String? {
            if (texto.isBlank()) return "Ingresa las horas solicitadas"
            val horas = texto.replace(',', '.').toDoubleOrNull()
                ?: return "Usa un número válido (ej. 1.5)"
            return validarHoras(horas)
        }

        /** Regla de negocio pura, reutilizable por la pantalla y por el caso de uso. */
        fun validarHoras(horas: Double): String? = when {
            !horas.isFinite() || horas <= 0 -> "Las horas deben ser mayores a 0"
            horas > MAX_HORAS -> "Máximo $MAX_HORAS_TEXTO h por solicitud"
            else -> null
        }

        /** Valida el motivo recortado; devuelve el mensaje de error o `null` si es válido. */
        fun validarMotivo(motivo: String): String? {
            val limpio = motivo.trim()
            return when {
                limpio.isEmpty() -> "El motivo es obligatorio"
                limpio.length < MIN_MOTIVO ->
                    "Mínimo $MIN_MOTIVO caracteres (llevas ${limpio.length})"
                else -> null
            }
        }
    }

    suspend operator fun invoke(
        usuarioId: String,
        marcaId: String?,
        fecha: Long,
        horas: Double,
        motivo: String
    ): String {
        validarHoras(horas)?.let { throw IllegalArgumentException(it) }

        val motivoLimpio = motivo.trim()
        validarMotivo(motivoLimpio)?.let { throw IllegalArgumentException(it) }

        val h = repo.solicitarHoraExtra(usuarioId, marcaId, fecha, horas, motivoLimpio)
        return h.id
    }
}

class ResolverHoraExtraUseCase @Inject constructor(
    private val repo: AttendanceRepository
) {
    suspend operator fun invoke(
        id: String,
        supervisorId: String,
        aprobada: Boolean
    ) {
        repo.aprobarRechazarHoraExtra(id, supervisorId, aprobada)
    }
}
