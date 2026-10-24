package cl.smartvolt.asistencia.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import cl.smartvolt.asistencia.data.repository.AttendanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: AttendanceRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        repo.sincronizarMarcas()
        runCatching { repo.sincronizarConfiguracion() } // la config puede fallar sin romper el envío de marcas
        Result.success()
    } catch (e: HttpException) {
        // Un 4xx (salvo timeout o rate limit) es rechazo permanente: reintentar no sirve.
        val permanente = e.code() in 400..499 && e.code() != 408 && e.code() != 429
        if (permanente) Result.failure() else reintentar()
    } catch (e: Exception) {
        reintentar() // sin red o error transitorio del servidor
    }

    private fun reintentar() = if (runAttemptCount >= MAX_INTENTOS) Result.failure() else Result.retry()

    private companion object { const val MAX_INTENTOS = 8 }
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
