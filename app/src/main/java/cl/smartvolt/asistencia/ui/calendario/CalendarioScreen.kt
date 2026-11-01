package cl.smartvolt.asistencia.ui.calendario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.data.local.MarcaEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.EstadoMarca
import cl.smartvolt.asistencia.ui.login.SessionState
import cl.smartvolt.asistencia.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

/** ViewModel del calendario: agrupa marcas por fecha. */
class CalendarioViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {

    private val _ui = MutableStateFlow(CalendarioUi())
    val ui: StateFlow<CalendarioUi> = _ui

    fun cargarHistorial(desde: Long, hasta: Long) = viewModelScope.launch {
        _ui.value = _ui.copy(cargando = true)
        val usuarioId = session.usuario.value?.id.orEmpty()
        val lista = repo.todasLasMarcas().first()
            .filter { it.timestampUtc >= desde && it.timestampUtc <= hasta }
            .ifEmpty { if (usuarioId.isNotEmpty()) repo.historial(usuarioId).first() else emptyList() }

        // Agrupar por fecha (día)
        val porFecha = lista.groupBy {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.timestampUtc))
        }.toSortedMap()

        _ui.value = _ui.copy(marcasPorFecha = porFecha, cargando = false)
    }
}

data class CalendarioUi(
    val marcasPorFecha: Map<String, List<MarcaEntity>> = emptyMap(),
    val cargando: Boolean = false,
    val error: String? = null
)

@Composable
fun CalendarioScreen(
    onNavUp: () -> Unit,
    vm: CalendarioViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsState()

    val hoy = System.currentTimeMillis()
    val inicioMes = hoy - (30L * 24 * 60 * 60 * 1000) // últimos 30 días

    // Cargar al entrar (en producción usar LaunchedEffect con parámetros)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        vm.cargarHistorial(inicioMes, hoy)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario de asistencia") },
                navigationIcon = { IconButton(onClick = onNavUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Text("Historial por día", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (ui.cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (ui.marcasPorFecha.isEmpty()) {
                    Text("Sin registros en el período seleccionado", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        items(items = ui.marcasPorFecha.entries.toList(), key = { it.key }) { (fecha, marcas) ->
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Text(
                                    text = fecha,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    items(items = marcas, key = { it.id }) { m ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("${m.usuarioId} · ${m.tipo}", style = MaterialTheme.typography.bodyMedium)
                                                val horaTxt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.timestampUtc))
                                                Text(horaTxt, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text(
                                                text = m.estado.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when (m.estado) {
                                                    EstadoMarca.VALIDA -> Ok
                                                    EstadoMarca.SOSPECHOSA -> Warn
                                                    EstadoMarca.RECHAZADA -> Bad
                                                }
                                            )
                                        }
                                        Divider()
                                    }
                                )
                            }
                        }
                    )
                }
            }

            ui.error?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
