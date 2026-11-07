package cl.smartvolt.asistencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.modelo.MarcaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilaPresencia(val marca: MarcaEntity, val visada: Boolean?)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForemanViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {
    val supervisorId get() = session.usuario.value?.id.orEmpty()

    /** Última marca de cada operario de la faena + su visación (si existe). Se alimenta de Room. */
    val filas: StateFlow<List<FilaPresencia>> = flow { emit(session.usuario.value?.faenaId) }
        .filterNotNull()
        .flatMapLatest { faena -> combine(repo.marcasDeFaena(faena), repo.visaciones()) { marcas, vis ->
            val visPorMarca = vis.associate { it.marcaId to it.aprobada }
            marcas.groupBy { it.usuarioId }.map { (_, l) -> l.first().let { FilaPresencia(it, visPorMarca[it.id]) } }
                .sortedBy { it.marca.usuarioId }
        } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun visar(marcaId: String, aprobada: Boolean) = viewModelScope.launch {
        repo.visar(marcaId, supervisorId, aprobada, System.currentTimeMillis())
    }

    fun cerrarSesion() = session.cerrarSesion()
}
