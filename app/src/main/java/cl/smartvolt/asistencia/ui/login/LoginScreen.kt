package cl.smartvolt.asistencia.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    init { viewModelScope.launch { repo.cargarSemillaSiVacio() } }

    fun limpiarError() {
        _error.value = null
    }

    /** DEMO: PIN fijo "1234". En una versión real, autenticar contra el backend (RF-04, RF-60). */
    fun ingresar(id: String, pin: String, onOk: (Rol) -> Unit) = viewModelScope.launch {
        val u = repo.usuario(id.trim().uppercase())
        if (u == null || pin != "1234") { _error.value = "Código o PIN incorrecto"; return@launch }
        session.usuario.value = u
        _error.value = null
        onOk(u.rol)
    }
}

private val FORMATO_CODIGO = Regex("^[A-Za-z]{2,3}-\\d{4}$")

@Composable
fun LoginScreen(onLogin: (Rol) -> Unit, vm: LoginViewModel = hiltViewModel()) {
    var id by remember { mutableStateOf("OP-0001") }
    var pin by remember { mutableStateOf("1234") }
    var intentoEnvio by remember { mutableStateOf(false) }
    val error by vm.error.collectAsState()

    val idError = when {
        id.isBlank() -> "Ingresa tu código de usuario"
        !FORMATO_CODIGO.matches(id.trim()) -> "Formato esperado: OP-0001, CAP-0001 o ADM-0001"
        else -> null
    }
    val pinError = when {
        pin.isBlank() -> "Ingresa tu PIN"
        pin.length != 4 || pin.any { !it.isDigit() } -> "El PIN debe tener 4 dígitos"
        else -> null
    }

    // El error se muestra mientras se escribe, no solo al enviar.
    val mostrarIdError = (intentoEnvio || id.isNotEmpty()) && idError != null
    val mostrarPinError = (intentoEnvio || pin.isNotEmpty()) && pinError != null
    val formularioValido = idError == null && pinError == null

    val idSupport: (@Composable () -> Unit)? =
        if (mostrarIdError) ({ Text(idError.orEmpty()) }) else null
    val pinSupport: (@Composable () -> Unit)? =
        if (mostrarPinError) ({ Text(pinError.orEmpty()) }) else null

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Smart-Volt Asistencia", style = MaterialTheme.typography.headlineMedium)
        Text("Datos ficticios · uso académico", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = id,
            onValueChange = {
                id = it.uppercase()
                if (error != null) vm.limpiarError()
            },
            label = { Text("Código de usuario") },
            placeholder = { Text("OP-0001, CAP-0001 o ADM-0001") },
            isError = mostrarIdError,
            supportingText = idSupport,
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
            isError = mostrarPinError,
            supportingText = pinSupport,
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
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                intentoEnvio = true
                if (formularioValido) vm.ingresar(id, pin, onLogin)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("INGRESAR")
        }
    }
}
