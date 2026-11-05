package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.domain.usecase.SolicitarHoraExtraUseCase
import cl.smartvolt.asistencia.domain.validation.ValidadorHoraExtra
import cl.smartvolt.asistencia.domain.validation.ValidadorLogin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La validación vive fuera de Compose y se prueba directamente. */
class ValidacionFormulariosTest {

    @Test
    fun `codigo de login valido acepta los tres prefijos de la semilla`() {
        assertNull(ValidadorLogin.codigo("OP-0001"))
        assertNull(ValidadorLogin.codigo("CAP-0001"))
        assertNull(ValidadorLogin.codigo("ADM-0001"))
    }

    @Test
    fun `codigo de login rechaza formatos invalidos`() {
        assertEquals("Ingresa tu código de usuario", ValidadorLogin.codigo("  "))
        assertEquals(
            "Formato esperado: OP-0001, CAP-0001 o ADM-0001",
            ValidadorLogin.codigo("OP-1"),
        )
        assertEquals(
            "Formato esperado: OP-0001, CAP-0001 o ADM-0001",
            ValidadorLogin.codigo("OPERARIO"),
        )
    }

    @Test
    fun `pin debe tener exactamente cuatro digitos`() {
        assertNull(ValidadorLogin.pin("1234"))
        assertEquals("Ingresa tu PIN", ValidadorLogin.pin(""))
        assertEquals("El PIN debe tener 4 dígitos", ValidadorLogin.pin("12"))
        assertEquals("El PIN debe tener 4 dígitos", ValidadorLogin.pin("12a4"))
    }

    @Test
    fun `formulario de login valido solo con formato de codigo y pin`() {
        // El formato es válido; que el usuario exista y el PIN coincida lo
        // resuelve el ViewModel contra la semilla.
        assertTrue(ValidadorLogin.formularioValido("OP-0001", "1234"))
        assertTrue(ValidadorLogin.formularioValido("OP-0001", "0000"))
        assertFalse(ValidadorLogin.formularioValido("XX", "1234"))
        assertFalse(ValidadorLogin.formularioValido("OP-0001", "12"))
    }

    @Test
    fun `horas extra acepta valores dentro del rango`() {
        assertNull(ValidadorHoraExtra.horas("1"))
        assertNull(ValidadorHoraExtra.horas("1.5"))
        assertNull(ValidadorHoraExtra.horas("1,5"))
        assertNull(ValidadorHoraExtra.horas(SolicitarHoraExtraUseCase.MAX_HORAS.toString()))
    }

    @Test
    fun `horas extra rechaza vacio cero negativo y exceso`() {
        assertEquals("Ingresa las horas solicitadas", ValidadorHoraExtra.horas(""))
        assertEquals("Las horas deben ser mayores a 0", ValidadorHoraExtra.horas("0"))
        assertEquals("Las horas deben ser mayores a 0", ValidadorHoraExtra.horas("-1"))
        assertTrue(ValidadorHoraExtra.horas("abc") != null)
        assertTrue(
            ValidadorHoraExtra.horas((SolicitarHoraExtraUseCase.MAX_HORAS + 1).toString()) != null
        )
    }

    @Test
    fun `motivo exige el minimo de caracteres`() {
        assertEquals("El motivo es obligatorio", ValidadorHoraExtra.motivo("   "))
        assertTrue(ValidadorHoraExtra.motivo("corto") != null)
        assertNull(ValidadorHoraExtra.motivo("Trabajo autorizado en turno nocturno"))
    }

    @Test
    fun `formulario de horas extra valido solo con ambos campos correctos`() {
        assertTrue(ValidadorHoraExtra.formularioValido("2", "Trabajo autorizado en turno nocturno"))
        assertFalse(ValidadorHoraExtra.formularioValido("0", "Trabajo autorizado en turno nocturno"))
        assertFalse(ValidadorHoraExtra.formularioValido("2", "corto"))
    }
}
