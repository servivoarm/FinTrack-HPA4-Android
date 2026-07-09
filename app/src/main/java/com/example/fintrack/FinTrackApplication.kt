package com.example.fintrack

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.fintrack.utils.NotificationScheduler
import com.example.fintrack.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FinTrackApplication : Application(), DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cierreJob: Job? = null
    private var cierreAutomaticoPausado = false

    override fun onCreate() {
        super<Application>.onCreate()
        NotificationScheduler.createNotificationChannel(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        cierreJob?.cancel()
        cierreJob = null
    }

    override fun onStop(owner: LifecycleOwner) {
        if (cierreAutomaticoPausado) return

        cierreJob?.cancel()
        cierreJob = scope.launch {
            delay(500)
            if (!cierreAutomaticoPausado) {
                SessionManager(this@FinTrackApplication).cerrarSesion()
            }
        }
    }

    fun pausarCierreAutomatico() {
        cierreAutomaticoPausado = true
        cierreJob?.cancel()
        cierreJob = null
    }

    fun reanudarCierreAutomatico() {
        cierreAutomaticoPausado = false
    }

    override fun onTerminate() {
        scope.cancel()
        super.onTerminate()
    }
}
