package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.domain.usecase.ValidarLoginUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidarLoginUseCaseTest {

    private val validar = ValidarLoginUseCase()

    @Test
    fun acepta_codigos_con_el_formato_de_la_semilla() {
        listOf("OP-0001", "CAP-0001", "ADM-0001").forEach { codigo ->
            assertNull("El código $codigo debería ser válido", validar.validarCodigo(codigo))
        }
    }

    @Test
    fun acepta_codigo_en_minusculas_y_con_espacios() {
        assertNull(validar.validarCodigo("  op-0001  "))
    }

    @Test
    fun rechaza_codigo_vacio_o_con_formato_incorrecto() {
        assertEquals("Ingresa tu código de usuario", validar.validarCodigo("   "))
        assertEquals(
            "Formato esperado: OP-0001, CAP-0001 o ADM-0001",
            validar.validarCodigo("OP-001"),
        )
        assertEquals(
            "Formato esperado: OP-0001, CAP-0001 o ADM-0001",
            validar.validarCodigo("OPERARIO-0001"),
        )
    }

    @Test
    fun acepta_pin_de_cuatro_digitos() {
        assertNull(validar.validarPin("1234"))
    }

    @Test
    fun rechaza_pin_vacio_corto_o_con_letras() {
        assertEquals("Ingresa tu PIN", validar.validarPin(""))
        assertEquals("El PIN debe tener 4 dígitos", validar.validarPin("12"))
        assertEquals("El PIN debe tener 4 dígitos", validar.validarPin("12a4"))
    }

    @Test
    fun validar_devuelve_ambos_errores_y_marca_el_formulario_invalido() {
        val errores = validar.validar("X", "12")

        assertFalse(errores.valido)
        assertEquals("Formato esperado: OP-0001, CAP-0001 o ADM-0001", errores.codigo)
        assertEquals("El PIN debe tener 4 dígitos", errores.pin)
    }

    @Test
    fun validar_marca_el_formulario_valido_cuando_ambos_campos_estan_bien() {
        val errores = validar.validar("OP-0001", "1234")

        assertTrue(errores.valido)
        assertNull(errores.codigo)
        assertNull(errores.pin)
    }
}
