package cl.smartvolt.asistencia.domain.geo

import cl.smartvolt.asistencia.domain.model.LatLon
import cl.smartvolt.asistencia.domain.model.Perimetro
import cl.smartvolt.asistencia.domain.model.TipoPerimetro
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Geofencing local (funciona sin red). Kotlin puro, sin dependencias de Android. */
object GeoUtils {
    private const val RADIO_TIERRA_M = 6_371_000.0
    private const val M_POR_GRADO = 111_320.0

    /** Distancia en metros entre dos coordenadas (fórmula de Haversine). */
    fun distanciaM(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_M * asin(min(1.0, sqrt(h)))
    }

    /** ¿El punto está dentro del perímetro, tolerando [margenM] metros (ej. imprecisión del GPS)? */
    fun estaDentro(perimetro: Perimetro, punto: LatLon, margenM: Double = 0.0): Boolean {
        return when (perimetro.tipo) {
            TipoPerimetro.CIRCULO ->
                distanciaM(perimetro.centro, punto) <= (perimetro.radioM ?: 0.0) + margenM
            TipoPerimetro.POLIGONO -> {
                val poly = perimetro.poligono
                if (poly == null || poly.size < 3) false
                else puntoEnPoligono(poly, punto) || distanciaABordeM(poly, punto) <= margenM
            }
        }
    }

    /** Ray casting: x = longitud, y = latitud. Suficiente para polígonos pequeños (faenas). */
    fun puntoEnPoligono(poly: List<LatLon>, p: LatLon): Boolean {
        var dentro = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            val cruza = (a.lat > p.lat) != (b.lat > p.lat) &&
                p.lon < (b.lon - a.lon) * (p.lat - a.lat) / (b.lat - a.lat) + a.lon
            if (cruza) dentro = !dentro
            j = i
        }
        return dentro
    }

    /** Distancia mínima en metros del punto al borde del polígono (proyección plana local). */
    fun distanciaABordeM(poly: List<LatLon>, p: LatLon): Double {
        val kx = cos(Math.toRadians(p.lat)) * M_POR_GRADO
        fun x(l: LatLon) = (l.lon - p.lon) * kx
        fun y(l: LatLon) = (l.lat - p.lat) * M_POR_GRADO
        var mejor = Double.MAX_VALUE
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            mejor = min(mejor, distanciaPuntoSegmento(x(a), y(a), x(b), y(b)))
        }
        return mejor
    }

    // Distancia del origen (0,0) al segmento (ax,ay)-(bx,by)
    private fun distanciaPuntoSegmento(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val largo2 = dx * dx + dy * dy
        val t = if (largo2 == 0.0) 0.0 else ((-ax) * dx + (-ay) * dy) / largo2
        val tc = t.coerceIn(0.0, 1.0)
        val px = ax + tc * dx
        val py = ay + tc * dy
        return sqrt(px * px + py * py)
    }
}
