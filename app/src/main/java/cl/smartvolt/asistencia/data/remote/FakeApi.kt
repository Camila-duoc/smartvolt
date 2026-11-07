package cl.smartvolt.asistencia.data.remote

import cl.smartvolt.asistencia.modelo.ConfiguracionDto
import cl.smartvolt.asistencia.modelo.LoteRequest
import cl.smartvolt.asistencia.modelo.LoteResponse
import cl.smartvolt.asistencia.modelo.ResultadoMarcaDto

/**
 * API simulada para el flavor "demo": no hay backend, así que acepta todas las marcas
 * del lote y devuelve una configuración vacía (se conserva la semilla local).
 * Así las marcas pasan de PENDIENTE a ENVIADA cuando WorkManager ejecuta la sincronización.
 */
class FakeApiService : ApiService {
    override suspend fun enviarLote(req: LoteRequest): LoteResponse =
        LoteResponse(
            req.marcas.map {
                ResultadoMarcaDto(id = it.id, aceptada = true, estadoServidor = it.estadoCliente)
            },
        )

    override suspend fun configuracion(desde: Long?): ConfiguracionDto = ConfiguracionDto()
}
