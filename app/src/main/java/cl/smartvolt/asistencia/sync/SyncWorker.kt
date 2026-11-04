package cl.smartvolt.asistencia.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import cl.smartvolt.asistencia.notifications.Notificaciones
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: AttendanceRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val pendientesAntes = runCatching { repo.contarPendientes().first() }.getOrDefault(0)
        repo.sincronizarMarcas()
        runCatching { repo.sincronizarConfiguracion() } // la config puede fallar sin romper el envío de marcas
        val pendientesDespues = runCatching { repo.contarPendientes().first() }.getOrDefault(pendientesAntes)
        val enviadas = pendientesAntes - pendientesDespues
        if (enviadas > 0) {
            runCatching { Notificaciones.sincronizacionCompletada(applicationContext, enviadas) }
        }
        Result.success()
    } catch (e: Exception) {
        Result.retry() // sin red o error del servidor: WorkManager reintenta con backoff exponencial
    }
}

object SyncScheduler {
    fun enqueue(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync-marcas", ExistingWorkPolicy.KEEP, req)
    }
}
