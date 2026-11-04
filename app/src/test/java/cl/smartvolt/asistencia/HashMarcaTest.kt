package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.domain.usecase.HashMarca
import cl.smartvolt.asistencia.domain.usecase.MarcaHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashMarcaTest {

    @Test
    fun hash_es_deterministico() {
        val h1 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        val h2 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        assertEquals(h1, h2)
    }

    @Test
    fun hash_cambia_si_cambia_el_hash_previo() {
        val h1 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        val h2 = HashMarca.calcular("OTRO-HASH", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        assertNotEquals(h1, h2)
    }

    @Test
    fun hash_cambia_si_cambia_el_estado() {
        val h1 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        val h2 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "RECHAZADA")
        assertNotEquals(h1, h2)
    }

    @Test
    fun hash_cambia_si_cambian_las_coordenadas() {
        val h1 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        val h2 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.5000, -70.7000, 1000L, "VALIDA")
        assertNotEquals(h1, h2)
    }

    @Test
    fun hash_con_latitud_nula_funciona() {
        val h1 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", null, null, 1000L, "VALIDA")
        val h2 = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", null, null, 1000L, "VALIDA")
        assertEquals(h1, h2)
    }

    @Test
    fun hash_tiene_longitud_esperada_sha256_hex() {
        val h = HashMarca.calcular("GENESIS", "id-1", "OP-0001", "IN", -33.4180, -70.6050, 1000L, "VALIDA")
        assertEquals(64, h.length) // SHA-256 = 32 bytes = 64 caracteres hex
    }

    private fun marca(
        id: String,
        previo: String,
        usuarioId: String = "OP-0001",
        timestamp: Long = 1000L,
        estado: String = "VALIDA",
    ): MarcaHash {
        val hash = HashMarca.calcular(
            previo, id, usuarioId, "IN", -33.4180, -70.6050, timestamp, estado,
        )
        return MarcaHash(id, usuarioId, "IN", -33.4180, -70.6050, timestamp, estado, previo, hash)
    }

    @Test
    fun cadena_valida_es_integra() {
        val m1 = marca("m1", "GENESIS", timestamp = 1L)
        val m2 = marca("m2", m1.hash, timestamp = 2L)
        val m3 = marca("m3", m2.hash, timestamp = 3L)

        val resultado = HashMarca.verificarCadena(listOf(m1, m2, m3))

        assertTrue(resultado.integra)
        assertEquals(3, resultado.verificadas)
        assertEquals(0, resultado.noVerificables)
        assertTrue(resultado.invalidas.isEmpty())
    }

    @Test
    fun detecta_una_marca_alterada() {
        val m1 = marca("m1", "GENESIS", timestamp = 1L)
        val m2 = marca("m2", m1.hash, timestamp = 2L)
        val alterada = m2.copy(estado = "RECHAZADA") // el hash ya no coincide con el contenido

        val resultado = HashMarca.verificarCadena(listOf(m1, alterada))

        assertFalse(resultado.integra)
        assertEquals(listOf("m2"), resultado.invalidas)
    }

    @Test
    fun detecta_un_enlace_roto_entre_marcas() {
        val m1 = marca("m1", "GENESIS", timestamp = 1L)
        val m2 = marca("m2", "HASH-QUE-NO-CORRESPONDE", timestamp = 2L)

        val resultado = HashMarca.verificarCadena(listOf(m1, m2))

        assertFalse(resultado.integra)
        assertEquals(listOf("m2"), resultado.invalidas)
    }

    @Test
    fun marcas_sin_hash_se_informan_como_no_verificables() {
        val semilla = MarcaHash("s1", "OP-0001", "IN", null, null, 1L, "VALIDA", "", "")

        val resultado = HashMarca.verificarCadena(listOf(semilla))

        assertTrue(resultado.integra)
        assertEquals(0, resultado.verificadas)
        assertEquals(1, resultado.noVerificables)
    }
}
