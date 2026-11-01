package cl.smartvolt.asistencia.ui.checkin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.LocalNavController
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.BuildConfig
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.*
import cl.smartvolt.asistencia.domain.usecase.RegistrarMarcaUseCase
import cl.smartvolt.asistencia.sensors.DemoState
import cl.smartvolt.asistencia.sensors.Escenario
import cl.smartvolt.asistencia.sync.SyncScheduler
import cl.smartvolt.asistencia.ui.login.SessionState
import cl.smartvolt.asistencia.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckInUi(val cargando: Boolean = false, val veredicto: Veredicto? = null, val error: String? = null)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val registrar: RegistrarMarcaUseCase,
    repo: AttendanceRepository,
    private val session: SessionState,
    @ApplicationContext private val ctx: Context,
) : ViewModel() {
    private val _ui = MutableStateFlow(CheckInUi())
    val ui: StateFlow<CheckInUi> = _ui
    val pendientes = repo.contarPendientes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val usuarioId get() = session.usuario.value?.id.orEmpty()

    fun marcar(tipo: TipoMarca) {
        val u = session.usuario.value ?: return
        viewModelScope.launch {
            _ui.value = CheckInUi(cargando = true)
            runCatching { registrar(u.id, u.faenaId, tipo) }
                .onSuccess { _ui.value = CheckInUi(veredicto = it); SyncScheduler.enqueue(ctx) }
                .onFailure { _ui.value = CheckInUi(error = it.message ?: "Error inesperado") }
        }
    }
}

private fun permisosCriticos(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun faltanPermisos(c: Context) = permisosCriticos()
    .filter { it != Manifest.permission.POST_NOTIFICATIONS }
    .any { ContextCompat.checkSelfPermission(c, it) != PackageManager.PERMISSION_GRANTED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(vm: CheckInViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()
    val pendientes by vm.pendientes.collectAsState()
    var sinPermisos by remember { mutableStateOf(faltanPermisos(ctx)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { sinPermisos = faltanPermisos(ctx) }
    LaunchedEffect(Unit) { if (sinPermisos) launcher.launch(permisosCriticos()) }

    val nav = LocalNavController.current
    Scaffold(topBar = { TopAppBar(title = { Text("Marcar asistencia · ${vm.usuarioId}") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sinPermisos) {
                Card { Column(Modifier.padding(16.dp)) {
                    Text("Faltan permisos de ubicación o Bluetooth para validar tu presencia.")
                    TextButton(onClick = { launcher.launch(permisosCriticos()) }) { Text("Conceder permisos") }
                } }
            }
            // Botones grandes: uso con guantes (RNF-20).
            Button(onClick = { vm.marcar(TipoMarca.IN) }, enabled = !ui.cargando && !sinPermisos,
                colors = ButtonDefaults.buttonColors(containerColor = Ok), modifier = Modifier.fillMaxWidth().height(88.dp)) { Text("MARCAR ENTRADA") }
            Button(onClick = { vm.marcar(TipoMarca.OUT) }, enabled = !ui.cargando && !sinPermisos,
                modifier = Modifier.fillMaxWidth().height(88.dp)) { Text("MARCAR SALIDA") }
            if (ui.cargando) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Validando ubicación y balizas…") }
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ui.veredicto?.let { ResultadoCard(it) }
            Text("Marcas pendientes de envío: $pendientes")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { nav.navigate("mis-horas-extra") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mis horas extra")
            }

            if (BuildConfig.USE_FAKE_SENSORS) {
                val actual by DemoState.escenario.collectAsState()
                Text("Modo demo: escenario simulado", style = MaterialTheme.typography.titleSmall)
                Escenario.values().forEach { e ->
                    FilterChip(selected = actual == e, onClick = { DemoState.escenario.value = e }, label = { Text(e.name) })
                }
                Text("Para probar offline, activa el modo avión.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultadoCard(v: Veredicto) {
    val color = when (v.estado) { EstadoMarca.VALIDA -> Ok; EstadoMarca.SOSPECHOSA -> Warn; EstadoMarca.RECHAZADA -> Bad }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(v.estado.name, color = color, style = MaterialTheme.typography.titleLarge)
            Text(v.motivo)
            v.resultados.forEach { Text((if (it.aprobada) "✔ " else "✖ ") + it.detalle, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
