package cl.smartvolt.asistencia.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: AttendanceRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        repo.sincronizarMarcas()
        runCatching { repo.sincronizarConfiguracion() } // la config puede fallar sin romper el envío de marcas
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
