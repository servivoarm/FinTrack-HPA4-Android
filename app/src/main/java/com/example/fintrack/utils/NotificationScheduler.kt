package com.example.fintrack.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationScheduler {
    const val CHANNEL_ID = "expense_reminders"
    const val EXTRA_MOVIMIENTO_ID = "extra_movimiento_id"
    const val EXTRA_ETIQUETA = "extra_etiqueta"
    const val EXTRA_MONTO_USD = "extra_monto_usd"
    const val EXTRA_MONEDA = "extra_moneda"
    const val EXTRA_FECHA_GASTO = "extra_fecha_gasto"

    private const val TAG = "NotificationScheduler"
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recordatorios de gastos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones para recordar pagos futuros"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun scheduleExpenseReminder(
        context: Context,
        movimientoId: Long,
        etiqueta: String,
        montoUSD: Double,
        moneda: String,
        fechaGasto: String
    ): Boolean {
        val fecha = parseFecha(fechaGasto) ?: return false
        val hoy = LocalDate.now()
        if (!fecha.isAfter(hoy)) {
            Log.d(TAG, "No se programa recordatorio: fecha no futura para movimiento $movimientoId")
            return false
        }

        createNotificationChannel(context)
        val triggerAtMillis = calcularTriggerMillis(context, fecha)
        val intent = Intent(context, ExpenseReminderReceiver::class.java).apply {
            putExtra(EXTRA_MOVIMIENTO_ID, movimientoId)
            putExtra(EXTRA_ETIQUETA, etiqueta)
            putExtra(EXTRA_MONTO_USD, montoUSD)
            putExtra(EXTRA_MONEDA, moneda)
            putExtra(EXTRA_FECHA_GASTO, fechaGasto)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(movimientoId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }.onFailure {
            Log.w(TAG, "Fallo alarma exacta; usando alarma flexible para movimiento $movimientoId", it)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

        val fechaHoraProgramada = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(triggerAtMillis),
            ZoneId.systemDefault()
        )
        Log.d(TAG, "Recordatorio programado para movimiento $movimientoId en $fechaHoraProgramada")
        return true
    }

    fun cancelExpenseReminder(context: Context, movimientoId: Long) {
        val intent = Intent(context, ExpenseReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(movimientoId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Recordatorio cancelado para movimiento $movimientoId")
        }
    }

    private fun calcularTriggerMillis(context: Context, fechaGasto: LocalDate): Long {
        val fechaNotificacion = fechaGasto.minusDays(1)
        val esDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val fechaHora = if (esDebug && fechaNotificacion == LocalDate.now()) {
            LocalDateTime.now().plusMinutes(1)
        } else {
            LocalDateTime.of(fechaNotificacion, LocalTime.of(9, 0))
        }
        return fechaHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun parseFecha(fecha: String): LocalDate? {
        return runCatching { LocalDate.parse(fecha, dateFormatter) }.getOrNull()
    }

    private fun requestCode(movimientoId: Long): Int {
        return movimientoId.hashCode()
    }
}
