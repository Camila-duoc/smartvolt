package cl.smartvolt.asistencia.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.local.UsuarioEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.Rol
import cl.smartvolt.asistencia.domain.validation.ValidadorLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionState @Inject constructor() {
    val usuario = MutableStateFlow<UsuarioEntity?>(null)

    fun cerrarSesion() {
        usuario.value = null
    }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando

    init {
        viewModelScope.launch {
            try {
                repo.cargarSemillaSiVacio()
            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    /** DEMO: PIN fijo "1234". En una versión real, autenticar contra el backend (RF-04, RF-60). */
    fun ingresar(id: String, pin: String, onOk: (Rol) -> Unit) = viewModelScope.launch {
        _cargando.value = true
        try {
            val u = repo.usuario(id.trim().uppercase())
            if (u == null || pin != "1234") {
                _error.value = "Código o PIN incorrecto"
                return@launch
            }
            session.usuario.value = u
            _error.value = null
            onOk(u.rol)
        } finally {
            _cargando.value = false
        }
    }
}

private data class AccesoDemo(
    val etiqueta: String,
    val codigo: String,
    val icono: ImageVector,
)

/** Atajos de prueba: usan los mismos usuarios de `assets/seed.json` y el PIN 1234. */
private val ACCESOS_DEMO = listOf(
    AccesoDemo("Operario", "OP-0001", Icons.Default.Person),
    AccesoDemo("Capataz", "CAP-0001", Icons.Default.SupervisorAccount),
    AccesoDemo("Admin", "ADM-0001", Icons.Default.AdminPanelSettings),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(onLogin: (Rol) -> Unit, vm: LoginViewModel = hiltViewModel()) {
    var id by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var intentoEnvio by remember { mutableStateOf(false) }
    val error by vm.error.collectAsState()
    val cargando by vm.cargando.collectAsState()

    val idError = ValidadorLogin.codigo(id)
    val pinError = ValidadorLogin.pin(pin)

    // El error se muestra mientras se escribe, no solo al enviar.
    val mostrarIdError = (intentoEnvio || id.isNotEmpty()) && idError != null
    val mostrarPinError = (intentoEnvio || pin.isNotEmpty()) && pinError != null
    val formularioValido = ValidadorLogin.formularioValido(id, pin)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Smart-Volt Asistencia", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Datos ficticios · uso académico",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = id,
            onValueChange = {
                id = it.uppercase()
                if (error != null) vm.limpiarError()
            },
            label = { Text("Código de usuario") },
            placeholder = { Text("OP-0001, CAP-0001 o ADM-0001") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Usuario") },
            isError = mostrarIdError,
            supportingText = if (mostrarIdError) ({ TextoError(idError.orEmpty()) }) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                pin = it.filter(Char::isDigit).take(4)
                if (error != null) vm.limpiarError()
            },
            label = { Text("PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "PIN") },
            isError = mostrarPinError,
            supportingText = if (mostrarPinError) ({ TextoError(pinError.orEmpty()) }) else null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                intentoEnvio = true
                if (formularioValido) vm.ingresar(id, pin, onLogin)
            },
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("INGRESAR")
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            "Acceso rápido de prueba",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ACCESOS_DEMO.forEach { acceso ->
                AssistChip(
                    onClick = {
                        id = acceso.codigo
                        pin = "1234"
                        intentoEnvio = true
                        vm.ingresar(acceso.codigo, "1234", onLogin)
                    },
                    enabled = !cargando,
                    label = { Text(acceso.etiqueta) },
                    leadingIcon = {
                        Icon(
                            acceso.icono,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
        Text(
            "PIN de todos los usuarios demo: 1234",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Mensaje de error de campo con ícono, para que la retroalimentación sea visual y consistente. */
@Composable
private fun TextoError(mensaje: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(mensaje, color = MaterialTheme.colorScheme.error)
    }
}
