package cl.smartvolt.asistencia.ui.comun.sesion

import cl.smartvolt.asistencia.modelo.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionState @Inject constructor() {
    val usuario = MutableStateFlow<UsuarioEntity?>(null)

    fun cerrarSesion() {
        usuario.value = null
    }
}
