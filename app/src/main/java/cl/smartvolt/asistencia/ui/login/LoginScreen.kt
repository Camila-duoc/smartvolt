package cl.smartvolt.asistencia.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import cl.smartvolt.asistencia.domain.usecase.ValidarLoginUseCase
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

/** Estado del formulario de login: errores por campo y error general de autenticación. */
data class LoginUi(
    val idError: String? = null,
    val pinError: String? = null,
    val errorGeneral: String? = null,
) {
    val formularioValido: Boolean get() = idError == null && pinError == null
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
    private val validarLogin: ValidarLoginUseCase,
) : ViewModel() {
    private val _ui = MutableStateFlow(LoginUi())
    val ui: StateFlow<LoginUi> = _ui

    init { viewModelScope.launch { repo.cargarSemillaSiVacio() } }

    /** Valida los campos con la lógica del dominio y publica los errores por campo. */
    fun validar(id: String, pin: String) {
        val errores = validarLogin.validar(id, pin)
        _ui.value = _ui.value.copy(idError = errores.codigo, pinError = errores.pin)
    }

    fun limpiarError() {
        if (_ui.value.errorGeneral != null) {
            _ui.value = _ui.value.copy(errorGeneral = null)
        }
    }

    /** DEMO: PIN fijo "1234". En una versión real, autenticar contra el backend (RF-04, RF-60). */
    fun ingresar(id: String, pin: String, onOk: (Rol) -> Unit) = viewModelScope.launch {
        val errores = validarLogin.validar(id, pin)
        if (!errores.valido) {
            _ui.value = _ui.value.copy(idError = errores.codigo, pinError = errores.pin)
            return@launch
        }

        val u = repo.usuario(id.trim().uppercase())
        if (u == null || pin != "1234") {
            _ui.value = _ui.value.copy(errorGeneral = "Código o PIN incorrecto")
            return@launch
        }

        session.usuario.value = u
        _ui.value = LoginUi()
        onOk(u.rol)
    }
}

@Composable
fun LoginScreen(onLogin: (Rol) -> Unit, vm: LoginViewModel = hiltViewModel()) {
    var id by rememberSaveable { mutableStateOf("OP-0001") }
    var pin by rememberSaveable { mutableStateOf("1234") }
    var intentoEnvio by rememberSaveable { mutableStateOf(false) }
    var tocadoId by rememberSaveable { mutableStateOf(false) }
    var tocadoPin by rememberSaveable { mutableStateOf(false) }
    val ui by vm.ui.collectAsState()

    // La validación vive en el dominio; aquí solo se sincroniza con lo que se escribe.
    LaunchedEffect(id, pin) { vm.validar(id, pin) }

    val mostrarIdError = (intentoEnvio || tocadoId) && ui.idError != null
    val mostrarPinError = (intentoEnvio || tocadoPin) && ui.pinError != null

    val idSupport: (@Composable () -> Unit)? =
        if (mostrarIdError) ({ Text(ui.idError.orEmpty()) }) else null
    val pinSupport: (@Composable () -> Unit)? =
        if (mostrarPinError) ({ Text(ui.pinError.orEmpty()) }) else null

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
                tocadoId = true
                vm.limpiarError()
            },
            label = { Text("Código de usuario") },
            placeholder = { Text("OP-0001, CAP-0001 o ADM-0001") },
            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
            trailingIcon = if (mostrarIdError) {
                { Icon(Icons.Default.ErrorOutline, contentDescription = "Error en el código") }
            } else {
                null
            },
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
                pin = it.filter(Char::isDigit).take(ValidarLoginUseCase.LARGO_PIN)
                tocadoPin = true
                vm.limpiarError()
            },
            label = { Text("PIN") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = if (mostrarPinError) {
                { Icon(Icons.Default.ErrorOutline, contentDescription = "Error en el PIN") }
            } else {
                null
            },
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
            visible = ui.errorGeneral != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ui.errorGeneral.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                intentoEnvio = true
                tocadoId = true
                tocadoPin = true
                vm.ingresar(id, pin, onLogin)
            },
            enabled = ui.formularioValido,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("INGRESAR")
        }
    }
}
