package cl.smartvolt.asistencia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.domain.usecase.ValidarLoginUseCase
import cl.smartvolt.asistencia.modelo.Rol
import cl.smartvolt.asistencia.modelo.UsuarioEntity
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
