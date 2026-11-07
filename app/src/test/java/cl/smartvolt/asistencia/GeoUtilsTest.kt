package cl.smartvolt.asistencia

import cl.smartvolt.asistencia.domain.geo.GeoUtils
import cl.smartvolt.asistencia.modelo.*
import org.junit.Assert.*
import org.junit.Test

class GeoUtilsTest {
    private val centro = LatLon(-33.4180, -70.6050)
    private val circulo = Perimetro(TipoPerimetro.CIRCULO, centro, radioM = 180.0)
    private val poligono = Perimetro(
        TipoPerimetro.POLIGONO, LatLon(-33.5400, -70.6600),
        poligono = listOf(LatLon(-33.5392, -70.6612), LatLon(-33.5392, -70.6588), LatLon(-33.5408, -70.6588), LatLon(-33.5408, -70.6612)),
    )

    @Test fun haversine_un_grado_de_latitud_son_unos_111_km() {
        val d = GeoUtils.distanciaM(LatLon(0.0, 0.0), LatLon(1.0, 0.0))
        assertEquals(111_195.0, d, 200.0)
    }

    @Test fun circulo_punto_interior_y_exterior() {
        assertTrue(GeoUtils.estaDentro(circulo, LatLon(-33.4181, -70.6051)))
        assertFalse(GeoUtils.estaDentro(circulo, LatLon(-33.4300, -70.6050))) // ~1,3 km
    }

    @Test fun circulo_el_margen_de_precision_amplia_el_perimetro() {
        // ~200 m al norte del centro: fuera con margen 0, dentro con margen 30
        val p = LatLon(-33.4180 + 200.0 / 111_320.0, -70.6050)
        assertFalse(GeoUtils.estaDentro(circulo, p, 0.0))
        assertTrue(GeoUtils.estaDentro(circulo, p, 30.0))
    }

    @Test fun poligono_interior_exterior_y_borde_con_margen() {
        assertTrue(GeoUtils.estaDentro(poligono, LatLon(-33.5400, -70.6600)))
        assertFalse(GeoUtils.estaDentro(poligono, LatLon(-33.5420, -70.6600)))
        val cercaDelBorde = LatLon(-33.5408 - 10.0 / 111_320.0, -70.6600) // ~10 m bajo el borde sur
        assertFalse(GeoUtils.estaDentro(poligono, cercaDelBorde, 0.0))
        assertTrue(GeoUtils.estaDentro(poligono, cercaDelBorde, 15.0))
    }
}
