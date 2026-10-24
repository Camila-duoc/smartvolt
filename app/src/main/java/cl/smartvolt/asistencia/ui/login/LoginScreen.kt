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
import android.content.Context
import cl.smartvolt.asistencia.data.local.UsuarioEntity
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.model.Rol
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Sesión en memoria + copia en SharedPreferences para sobrevivir a la muerte del proceso. */
@Singleton
class SessionState @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("sesion_smartvolt", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow<UsuarioEntity?>(null)
    val usuario: StateFlow<UsuarioEntity?> = _usuario

    fun iniciar(u: UsuarioEntity) {
        prefs.edit().putString(CLAVE_USUARIO, u.id).apply()
        _usuario.value = u
    }

    fun cerrar() {
        prefs.edit().remove(CLAVE_USUARIO).apply()
        _usuario.value = null
    }

    /** Recupera el usuario guardado; lo llaman las pantallas autenticadas al abrirse. */
    suspend fun restaurar(repo: AttendanceRepository) {
        if (_usuario.value != null) return
        val id = prefs.getString(CLAVE_USUARIO, null) ?: return
        val u = repo.usuario(id)
        if (u == null) prefs.edit().remove(CLAVE_USUARIO).apply() else _usuario.value = u
    }

    private companion object { const val CLAVE_USUARIO = "usuario_id" }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: AttendanceRepository,
    private val session: SessionState,
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            runCatching { repo.cargarSemillaSiVacio() }
                .onFailure { _error.value = "No se pudieron cargar los datos iniciales: ${it.message ?: "error de lectura"}" }
        }
    }

    /** DEMO: PIN fijo "1234". En una versión real, autenticar contra el backend (RF-04, RF-60). */
    fun ingresar(id: String, pin: String, onOk: (Rol) -> Unit) = viewModelScope.launch {
        val u = repo.usuario(id.trim().uppercase())
        if (u == null || pin != "1234") { _error.value = "Código o PIN incorrecto"; return@launch }
        session.iniciar(u)
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
