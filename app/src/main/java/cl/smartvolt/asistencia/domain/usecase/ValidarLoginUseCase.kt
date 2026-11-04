package cl.smartvolt.asistencia.domain.usecase

import javax.inject.Inject

/** Errores por campo del formulario de login. `null` significa que el campo es válido. */
data class ErroresLogin(
    val codigo: String? = null,
    val pin: String? = null,
) {
    val valido: Boolean get() = codigo == null && pin == null
}

/**
 * Reglas de validación del login, independientes de Compose.
 *
 * Mantenerlas aquí permite probarlas con JUnit y evita que la pantalla
 * tenga que conocer el formato del código o el largo del PIN.
 */
class ValidarLoginUseCase @Inject constructor() {

    companion object {
        /** Códigos aceptados: OP-0001, CAP-0001, ADM-0001 (2 o 3 letras, guion y 4 dígitos). */
        val FORMATO_CODIGO = Regex("^[A-Za-z]{2,3}-\\d{4}$")

        const val LARGO_PIN = 4
    }

    fun validarCodigo(codigo: String): String? = when {
        codigo.isBlank() -> "Ingresa tu código de usuario"
        !FORMATO_CODIGO.matches(codigo.trim()) -> "Formato esperado: OP-0001, CAP-0001 o ADM-0001"
        else -> null
    }

    fun validarPin(pin: String): String? = when {
        pin.isBlank() -> "Ingresa tu PIN"
        pin.length != LARGO_PIN || pin.any { !it.isDigit() } -> "El PIN debe tener $LARGO_PIN dígitos"
        else -> null
    }

    /** Valida ambos campos de una vez; lo usa el ViewModel antes de autenticar. */
    fun validar(codigo: String, pin: String): ErroresLogin =
        ErroresLogin(codigo = validarCodigo(codigo), pin = validarPin(pin))
}
