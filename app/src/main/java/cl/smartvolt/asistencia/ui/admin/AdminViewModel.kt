package cl.smartvolt.asistencia.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Métricas y resumen administrativo para el panel de administración. */
data class AdminMetricas(
    val totalMarcas: Int = 0,
    val marcasValidas: Int = 0,
    val marcasSospechosas: Int = 0,
    val marcasRechazadas: Int = 0,
    val marcasPendientesSync: Int = 0,
    val marcasPorUsuario: Map<String, Int> = emptyMap(),
    val marcasPorEstado: Map<EstadoMarca, Int> = emptyMap(),
    val cargando: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repo: AttendanceRepository,
) : ViewModel() {

    private val _metricas = MutableStateFlow(AdminMetricas())
    val metricas: StateFlow<AdminMetricas> = _metricas

    /** Carga las métricas administrativas desde el repositorio local (Room). */
    fun cargarMetricas() = viewModelScope.launch {
        _metricas.value = _metricas.value.copy(cargando = true, error = null)
        try {
            val pendientes = repo.contarPendientes().first()

            val todas = repo.todasLasMarcas().first()
            val porEstado = todas.groupBy { it.estado }.mapValues { it.value.size }
            val porUsuario = todas.groupBy { it.usuarioId }.mapValues { it.value.size }

            _metricas.value = AdminMetricas(
                totalMarcas = todas.size,
                marcasValidas = porEstado[EstadoMarca.VALIDA] ?: 0,
                marcasSospechosas = porEstado[EstadoMarca.SOSPECHOSA] ?: 0,
                marcasRechazadas = porEstado[EstadoMarca.RECHAZADA] ?: 0,
                marcasPendientesSync = pendientes,
                marcasPorUsuario = porUsuario,
                marcasPorEstado = porEstado,
                cargando = false,
            )
        } catch (e: Exception) {
            _metricas.value = _metricas.value.copy(cargando = false, error = e.message ?: "Error al cargar métricas")
        }
    }
}
