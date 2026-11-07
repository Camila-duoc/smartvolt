package cl.smartvolt.asistencia.data.remote

import cl.smartvolt.asistencia.modelo.ConfiguracionDto
import cl.smartvolt.asistencia.modelo.LoteRequest
import cl.smartvolt.asistencia.modelo.LoteResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/marcas/lote") suspend fun enviarLote(@Body req: LoteRequest): LoteResponse
    @GET("api/v1/sincronizacion/configuracion") suspend fun configuracion(@Query("desde") desde: Long?): ConfiguracionDto
}
