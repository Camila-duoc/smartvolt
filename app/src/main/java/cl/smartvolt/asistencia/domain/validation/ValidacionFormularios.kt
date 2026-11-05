package cl.smartvolt.asistencia.domain.validation

import cl.smartvolt.asistencia.domain.usecase.SolicitarHoraExtraUseCase

/**
 * Validaciones de formularios centralizadas y sin dependencias de Compose.
 *
 * Las pantallas solo pintan el resultado de estas funciones: así la lógica
 * queda desacoplada de la UI y se puede probar con pruebas unitarias.
 */
object ValidadorLogin {
    /** Códigos aceptados por la semilla: OP-0001, CAP-0001, ADM-0001. */
    private val FORMATO_CODIGO = Regex("^[A-Za-z]{2,3}-\\d{4}$")

    fun codigo(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu código de usuario"
        !FORMATO_CODIGO.matches(valor.trim()) ->
            "Formato esperado: OP-0001, CAP-0001 o ADM-0001"
        else -> null
    }

    fun pin(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu PIN"
        valor.length != 4 || valor.any { !it.isDigit() } -> "El PIN debe tener 4 dígitos"
        else -> null
    }

    fun formularioValido(codigo: String, pin: String): Boolean =
        codigo(codigo) == null && pin(pin) == null
}

object ValidadorHoraExtra {
    /** Texto del tope para mostrarlo en la UI sin duplicar la regla. */
    val maxHorasTexto: String =
        if (SolicitarHoraExtraUseCase.MAX_HORAS % 1.0 == 0.0) {
            SolicitarHoraExtraUseCase.MAX_HORAS.toInt().toString()
        } else {
            SolicitarHoraExtraUseCase.MAX_HORAS.toString()
        }

    fun horas(valor: String): String? {
        val numero = valor.replace(',', '.').toDoubleOrNull()
        return when {
            valor.isBlank() -> "Ingresa las horas solicitadas"
            numero == null -> "Usa un número válido (ej. 1.5)"
            numero <= 0 -> "Las horas deben ser mayores a 0"
            numero > SolicitarHoraExtraUseCase.MAX_HORAS -> "Máximo $maxHorasTexto h por solicitud"
            else -> null
        }
    }

    fun motivo(valor: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> "El motivo es obligatorio"
            limpio.length < SolicitarHoraExtraUseCase.MIN_MOTIVO ->
                "Mínimo ${SolicitarHoraExtraUseCase.MIN_MOTIVO} caracteres (llevas ${limpio.length})"
            else -> null
        }
    }

    fun formularioValido(horas: String, motivo: String): Boolean =
        horas(horas) == null && motivo(motivo) == null
}
