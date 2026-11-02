package cl.smartvolt.asistencia.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    /** DEMO: PIN fijo "1234". En una versión real, autenticar contra el backend (RF-04, RF-60). */
    fun ingresar(id: String, pin: String, onOk: (Rol) -> Unit) = viewModelScope.launch {
        val u = repo.usuario(id.trim().uppercase())
        if (u == null || pin != "1234") { _error.value = "Código o PIN incorrecto"; return@launch }
        session.usuario.value = u
        _error.value = null
        onOk(u.rol)
    }
}

@Composable
fun LoginScreen(onLogin: (Rol) -> Unit, vm: LoginViewModel = hiltViewModel()) {
    var id by remember { mutableStateOf("OP-0001") }
    var pin by remember { mutableStateOf("1234") }
    val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Smart-Volt Asistencia", style = MaterialTheme.typography.headlineMedium)
        Text("Datos ficticios · uso académico", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(id, { id = it }, label = { Text("Código de usuario (ej. OP-0001, CAP-0001, ADM-0001)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pin, { pin = it }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.ingresar(id, pin, onLogin) }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("INGRESAR") }
    }
}
