package cl.smartvolt.asistencia.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable data class FaenaDto(
    val id: String, val nombre: String, val tipoPerimetro: String,
    val centroLat: Double, val centroLon: Double, val radioM: Double? = null,
    val poligono: List<List<Double>>? = null,
)
@Serializable data class FrenteDto(val id: String, val faenaId: String, val nombre: String)
@Serializable data class BeaconDto(
    val uuid: String, val major: Int = 0, val minor: Int = 0,
    val faenaId: String, val frenteId: String? = null, val rssiMinimo: Int = -80,
)
@Serializable data class UsuarioDto(
    val id: String, val rol: String, val faenaId: String, val frenteId: String? = null, val deviceId: String? = null,
)
@Serializable data class ConfiguracionDto(
    val faenas: List<FaenaDto> = emptyList(), val frentes: List<FrenteDto> = emptyList(),
    val beacons: List<BeaconDto> = emptyList(), val usuarios: List<UsuarioDto> = emptyList(),
    val version: Long = 0,
)

@Serializable data class LecturaDto(val uuid: String, val rssiPromedio: Int, val muestras: Int)
@Serializable data class MarcaDto(
    val id: String, val usuarioId: String, val faenaId: String, val tipo: String,
    val lat: Double? = null, val lon: Double? = null, val accuracy: Double? = null,
    val timestampUtc: Long, val elapsedRealtimeMs: Long,
    val estadoCliente: String, val motivo: String,
    val hashPrevio: String, val hash: String, val lecturas: List<LecturaDto> = emptyList(),
)
@Serializable data class LoteRequest(val deviceId: String?, val marcas: List<MarcaDto>)
@Serializable data class ResultadoMarcaDto(val id: String, val aceptada: Boolean, val estadoServidor: String? = null, val error: String? = null)
@Serializable data class LoteResponse(val resultados: List<ResultadoMarcaDto>)

interface ApiService {
    @POST("api/v1/marcas/lote") suspend fun enviarLote(@Body req: LoteRequest): LoteResponse
    @GET("api/v1/sincronizacion/configuracion") suspend fun configuracion(@Query("desde") desde: Long?): ConfiguracionDto
}

/** Formato del archivo assets/seed.json (datos sintéticos entregados con el caso). */
@Serializable data class SeedMarcaDto(
    val id: String, val usuarioId: String, val tipo: String, val lat: Double, val lon: Double,
    val accuracy: Double, val timestampUtc: Long, val estado: String, val motivo: String = "",
    val syncEstado: String = "ENVIADA", val escenario: String = "normal",
)
@Serializable data class SeedDto(
    val faenas: List<FaenaDto>, val frentes: List<FrenteDto>, val beacons: List<BeaconDto>,
    val usuarios: List<UsuarioDto>, val marcas: List<SeedMarcaDto> = emptyList(),
)
