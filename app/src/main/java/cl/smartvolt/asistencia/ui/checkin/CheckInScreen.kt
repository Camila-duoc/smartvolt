package cl.smartvolt.asistencia.ui.checkin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.ui.navigation.LocalNavController
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.BuildConfig
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.*
import cl.smartvolt.asistencia.domain.usecase.RegistrarMarcaUseCase
import cl.smartvolt.asistencia.notifications.Notificaciones
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
                .onSuccess {
                    _ui.value = CheckInUi(veredicto = it)
                    if (it.estado == EstadoMarca.SOSPECHOSA) {
                        Notificaciones.marcaSospechosa(ctx, u.id, it.motivo)
                    }
                    SyncScheduler.enqueue(ctx)
                }
                .onFailure {
                    // Mensaje entendible para el usuario final; el detalle técnico
                    // (por ejemplo el código 17 de GMS) queda en los logs.
                    val mensaje = when (it) {
                        is SecurityException -> "Permiso de ubicación denegado. Actívalo en Ajustes."
                        else -> it.message ?: "Error inesperado"
                    }
                    _ui.value = CheckInUi(error = mensaje)
                }
        }
    }

    fun cerrarSesion() = session.cerrarSesion()
}

private fun permisosCriticos(): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_SCAN)
}.toTypedArray()

private fun faltanPermisos(c: Context) = !BuildConfig.USE_FAKE_SENSORS && permisosCriticos()
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

    // Notificaciones locales (Android 13+). Se piden aparte porque no bloquean la marca.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val nav = LocalNavController.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Marcar asistencia · ${vm.usuarioId}") },
            actions = {
                IconButton(onClick = {
                    vm.cerrarSesion()
                    nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                }) {
                    Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
                }
            }
        )
    }) { pad ->
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
            AnimatedVisibility(
                visible = ui.cargando,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Validando ubicación y balizas…")
                }
            }

            AnimatedVisibility(
                visible = ui.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            AnimatedContent(
                targetState = ui.veredicto,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) togetherWith
                        fadeOut(tween(150))
                },
                label = "resultado-marca",
            ) { veredicto ->
                if (veredicto != null) ResultadoCard(veredicto)
            }
            Text("Marcas pendientes de envío: $pendientes")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { nav.navigate("mis-horas-extra") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mis horas extra")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { nav.navigate("calendario") },
                    modifier = Modifier.weight(1f)
                ) { Text("Mi historial") }
                OutlinedButton(
                    onClick = { nav.navigate("mapa") },
                    modifier = Modifier.weight(1f)
                ) { Text("Ver mapa") }
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
    Card(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(v.estado.name, color = color, style = MaterialTheme.typography.titleLarge)
            Text(v.motivo)
            v.resultados.forEach { Text((if (it.aprobada) "✔ " else "✖ ") + it.detalle, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
