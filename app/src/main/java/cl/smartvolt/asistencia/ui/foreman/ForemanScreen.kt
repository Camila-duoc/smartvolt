package cl.smartvolt.asistencia.ui.foreman

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.compose.LocalNavController
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilaPresencia(val marca: MarcaEntity, val visada: Boolean?)

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForemanScreen(vm: ForemanViewModel = hiltViewModel()) {
    val filas by vm.filas.collectAsState()
    val nav = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de presencia") },
                actions = {
                    IconButton(onClick = { nav.navigate("aprobar-horas-extra") }) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = "Horas extra")
                    }
                    IconButton(onClick = { nav.navigate("calendario") }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendario")
                    }
                    IconButton(onClick = { nav.navigate("mapa/-33.4180/-70.6050") }) {
                        Icon(Icons.Default.Map, contentDescription = "Mapa")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Operarios con marcas: ${filas.size} · Sospechosas por visar: ${filas.count { it.marca.estado == EstadoMarca.SOSPECHOSA && it.visada == null }}") }
            items(filas, key = { it.marca.id }) { f ->
                val color = when (f.marca.estado) { EstadoMarca.VALIDA -> Ok; EstadoMarca.SOSPECHOSA -> Warn; EstadoMarca.RECHAZADA -> Bad }
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.marca.usuarioId, style = MaterialTheme.typography.titleMedium)
                            Text("${f.marca.tipo} · ${f.marca.estado}", color = color)
                            if (f.marca.motivo.isNotBlank()) Text(f.marca.motivo, style = MaterialTheme.typography.bodySmall)
                            f.visada?.let { Text(if (it) "Visada: aprobada" else "Visada: rechazada", style = MaterialTheme.typography.bodySmall) }
                        }
                        if (f.marca.estado == EstadoMarca.SOSPECHOSA && f.visada == null) {
                            TextButton(onClick = { vm.visar(f.marca.id, true) }) { Text("Aprobar") }
                            TextButton(onClick = { vm.visar(f.marca.id, false) }) { Text("Rechazar") }
                        }
                        TextButton(
                            onClick = { nav.navigate("detalle-visacion/${f.marca.id}") },
                        ) { Text("Ver detalle") }
                    }
                }
            }
        }
    }
}
