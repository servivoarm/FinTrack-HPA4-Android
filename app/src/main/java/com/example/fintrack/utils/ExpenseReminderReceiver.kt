package com.example.fintrack.utils

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.fintrack.R
import com.example.fintrack.ui.home.HomeActivity

class ExpenseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val movimientoId = intent.getLongExtra(NotificationScheduler.EXTRA_MOVIMIENTO_ID, 0L)
        val etiqueta = intent.getStringExtra(NotificationScheduler.EXTRA_ETIQUETA)
            ?.takeIf { it.isNotBlank() }
            ?: "gasto"
        val montoUSD = intent.getDoubleExtra(NotificationScheduler.EXTRA_MONTO_USD, 0.0)
        val moneda = intent.getStringExtra(NotificationScheduler.EXTRA_MONEDA) ?: "USD"
        val monto = MoneyUtils.formatearMonto(montoUSD, moneda)

        val openIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            movimientoId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_gastos_24)
            .setContentTitle("Recordatorio de pago")
            .setContentText("Mañana tienes programado el gasto: $etiqueta por $monto")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Mañana tienes programado el gasto: $etiqueta por $monto")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(movimientoId.hashCode(), notification)
    }
}
