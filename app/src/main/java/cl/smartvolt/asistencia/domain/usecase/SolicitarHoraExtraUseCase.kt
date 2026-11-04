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
    }

    suspend operator fun invoke(
        usuarioId: String,
        marcaId: String?,
        fecha: Long,
        horas: Double,
        motivo: String
    ): String {
        if (!horas.isFinite() || horas <= 0) {
            throw IllegalArgumentException("Las horas deben ser mayores a 0")
        }
        if (horas > MAX_HORAS) {
            throw IllegalArgumentException("El máximo por solicitud es $MAX_HORAS horas")
        }
        val motivoLimpio = motivo.trim()
        if (motivoLimpio.isEmpty()) {
            throw IllegalArgumentException("El motivo es requerido")
        }
        if (motivoLimpio.length < MIN_MOTIVO) {
            throw IllegalArgumentException("El motivo debe tener al menos $MIN_MOTIVO caracteres")
        }
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
