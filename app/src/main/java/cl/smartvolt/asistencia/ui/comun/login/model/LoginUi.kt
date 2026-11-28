package cl.smartvolt.asistencia.ui.comun.login.model

/** Estado del formulario de login: errores por campo y error general de autenticación. */
data class LoginUi(
    val idError: String? = null,
    val pinError: String? = null,
    val errorGeneral: String? = null,
) {
    val formularioValido: Boolean get() = idError == null && pinError == null
}
