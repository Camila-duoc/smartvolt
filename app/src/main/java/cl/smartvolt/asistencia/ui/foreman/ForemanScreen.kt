package cl.smartvolt.asistencia.ui.foreman

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.ui.login.SessionState
import cl.smartvolt.asistencia.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilaPresencia(val marca: MarcaEntity, val visada: Boolean?)

/** Presencia = última marca por operario; porVisar = todas las sospechosas aún sin visación. */
data class ForemanUi(
    val presencia: List<FilaPresencia> = emptyList(),
    val porVisar: List<MarcaEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForemanViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {
    val usuario = session.usuario
    val supervisorId get() = session.usuario.value?.id.orEmpty()

    init { viewModelScope.launch { session.restaurar(repo) } }

    val ui: StateFlow<ForemanUi> = session.usuario
        .map { it?.faenaId }
        .distinctUntilChanged()
        .flatMapLatest { faena ->
            if (faena == null) {
                flowOf(ForemanUi())
            } else {
                combine(repo.marcasDeFaena(faena), repo.visaciones()) { marcas, vis ->
                    val visPorMarca = vis.associate { it.marcaId to it.aprobada }
                    ForemanUi(
                        presencia = marcas.groupBy { it.usuarioId }
                            .map { (_, l) -> l.first().let { FilaPresencia(it, visPorMarca[it.id]) } }
                            .sortedBy { it.marca.usuarioId },
                        // Antes solo se mostraba la última marca por operario, así que una
                        // sospechosa antigua nunca podía visarse.
                        porVisar = marcas.filter { it.estado == EstadoMarca.SOSPECHOSA && visPorMarca[it.id] == null },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForemanUi())

    fun visar(marcaId: String, aprobada: Boolean) = viewModelScope.launch {
        val supervisor = supervisorId
        if (supervisor.isBlank()) return@launch
        repo.visar(marcaId, supervisor, aprobada, System.currentTimeMillis())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForemanScreen(vm: ForemanViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Panel de presencia") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Operarios con marcas: ${ui.presencia.size} · Sospechosas por visar: ${ui.porVisar.size}")
            }

            item { Text("Presencia (última marca por operario)", style = MaterialTheme.typography.titleSmall) }
            items(ui.presencia, key = { "p-${it.marca.id}" }) { f ->
                MarcaCard(f.marca, f.visada, permitirVisar = false, onVisar = {})
            }

            item { Text("Marcas sospechosas por visar", style = MaterialTheme.typography.titleSmall) }
            if (ui.porVisar.isEmpty()) {
                item { Text("Sin marcas pendientes de visación", style = MaterialTheme.typography.bodySmall) }
            }
            items(ui.porVisar, key = { "v-${it.id}" }) { m ->
                MarcaCard(m, visada = null, permitirVisar = true, onVisar = { aprobada -> vm.visar(m.id, aprobada) })
            }
        }
    }
}

@Composable
private fun MarcaCard(marca: MarcaEntity, visada: Boolean?, permitirVisar: Boolean, onVisar: (Boolean) -> Unit) {
    val color = when (marca.estado) {
        EstadoMarca.VALIDA -> Ok
        EstadoMarca.SOSPECHOSA -> Warn
        EstadoMarca.RECHAZADA -> Bad
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(marca.usuarioId, style = MaterialTheme.typography.titleMedium)
                    Text("${marca.tipo} · ${marca.estado}", color = color)
                }
                if (permitirVisar) {
                    TextButton(onClick = { onVisar(true) }) { Text("Aprobar") }
                    TextButton(onClick = { onVisar(false) }) { Text("Rechazar") }
                }
            }
            if (marca.motivo.isNotBlank()) Text(marca.motivo, style = MaterialTheme.typography.bodySmall)
            visada?.let { Text(if (it) "Visada: aprobada" else "Visada: rechazada", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
