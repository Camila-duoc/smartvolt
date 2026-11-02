package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.domain.usecase.HashMarca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
}
