package cl.smartvolt.asistencia.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cl.smartvolt.asistencia.MainActivity
import cl.smartvolt.asistencia.R

/**
 * Notificaciones locales del MVP: aviso de marca sospechosa (requiere visación)
 * y confirmación de sincronización. No dependen de un backend ni de push.
 */
object Notificaciones {

    const val CANAL_ALERTAS = "smartvolt_alertas"
    const val CANAL_SYNC = "smartvolt_sync"

    private const val ID_MARCA_SOSPECHOSA = 1001
    private const val ID_SYNC_COMPLETADA = 1002

    /** Crea los canales una sola vez al iniciar la app (Android 8.0+). */
    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val alertas = NotificationChannel(
            CANAL_ALERTAS,
            "Alertas de asistencia",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Marcas sospechosas que requieren visación del capataz"
        }

        val sync = NotificationChannel(
            CANAL_SYNC,
            "Sincronización",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Confirmación de envío de marcas al servidor"
        }

        manager.createNotificationChannel(alertas)
        manager.createNotificationChannel(sync)
    }

    /** Avisa que una marca quedó SOSPECHOSA y necesita visación. */
    fun marcaSospechosa(context: Context, usuarioId: String, motivo: String) {
        val detalle = if (motivo.isBlank()) "" else ": $motivo"
        notificar(
            context = context,
            canal = CANAL_ALERTAS,
            id = ID_MARCA_SOSPECHOSA,
            titulo = "Marca sospechosa",
            texto = "La marca de $usuarioId requiere visación$detalle",
            icono = R.drawable.ic_notificacion,
        )
    }

    /** Avisa cuántas marcas pendientes se enviaron correctamente. */
    fun sincronizacionCompletada(context: Context, cantidad: Int) {
        if (cantidad <= 0) return
        notificar(
            context = context,
            canal = CANAL_SYNC,
            id = ID_SYNC_COMPLETADA,
            titulo = "Sincronización completada",
            texto = "$cantidad marca(s) enviada(s) al servidor",
            icono = R.drawable.ic_notificacion,
        )
    }

    private fun notificar(
        context: Context,
        canal: String,
        id: Int,
        titulo: String,
        texto: String,
        icono: Int,
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(icono)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notificacion) }
    }
}
