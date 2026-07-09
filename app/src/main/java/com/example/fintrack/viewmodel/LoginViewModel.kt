package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.repository.UsuarioRepository
import com.example.fintrack.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LoginResult(
    val exitoso: Boolean,
    val mensaje: String? = null,
    val campoError: LoginCampoError? = null
)

enum class LoginCampoError {
    CORREO,
    PASSWORD,
    GENERAL
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UsuarioRepository(
        AppDatabase.obtenerInstancia(application).usuarioDao()
    )
    private val sessionManager = SessionManager(application)

    suspend fun iniciarSesion(correo: String, password: String): LoginResult {
        val correoLimpio = correo.trim()

        if (correoLimpio.isEmpty()) {
            return LoginResult(false, "El correo es obligatorio", LoginCampoError.CORREO)
        }
        if (!correoLimpio.contains("@")) {
            return LoginResult(false, "Ingresa un correo valido", LoginCampoError.CORREO)
        }
        if (password.isEmpty()) {
            return LoginResult(false, "La contrasena es obligatoria", LoginCampoError.PASSWORD)
        }

        val usuario = withContext(Dispatchers.IO) {
            repository.iniciarSesion(correoLimpio, password)
        } ?: return LoginResult(false, "Credenciales incorrectas", LoginCampoError.GENERAL)

        sessionManager.guardarSesion(usuario)
        return LoginResult(true)
    }
}
