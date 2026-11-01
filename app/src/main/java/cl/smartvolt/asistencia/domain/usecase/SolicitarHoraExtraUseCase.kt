package cl.smartvolt.asistencia.domain.usecase

import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoHoraExtra
import javax.inject.Inject

class SolicitarHoraExtraUseCase @Inject constructor(
    private val repo: AttendanceRepository
) {
    suspend operator fun invoke(
        usuarioId: String,
        marcaId: String?,
        fecha: Long,
        horas: Double,
        motivo: String
    ): String {
        if (horas <= 0) throw IllegalArgumentException("Las horas deben ser mayores a 0")
        if (motivo.isBlank()) throw IllegalArgumentException("El motivo es requerido")
        val h = repo.solicitarHoraExtra(usuarioId, marcaId, fecha, horas, motivo)
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
