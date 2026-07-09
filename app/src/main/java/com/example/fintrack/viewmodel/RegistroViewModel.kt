package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.repository.RegistroRepositoryResult
import com.example.fintrack.data.repository.UsuarioRepository
import com.example.fintrack.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RegistroResult(
    val exitoso: Boolean,
    val mensaje: String? = null,
    val campoError: RegistroCampoError? = null
)

enum class RegistroCampoError {
    NOMBRE,
    CORREO,
    PASSWORD,
    CONFIRMAR_PASSWORD,
    GENERAL
}

class RegistroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UsuarioRepository(
        AppDatabase.obtenerInstancia(application).usuarioDao()
    )
    private val sessionManager = SessionManager(application)

    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String,
        confirmarPassword: String
    ): RegistroResult {
        val nombreLimpio = nombre.trim()
        val correoLimpio = correo.trim()

        if (nombreLimpio.isEmpty()) {
            return RegistroResult(false, "El nombre es obligatorio", RegistroCampoError.NOMBRE)
        }
        if (correoLimpio.isEmpty()) {
            return RegistroResult(false, "El correo es obligatorio", RegistroCampoError.CORREO)
        }
        if (!correoLimpio.contains("@")) {
            return RegistroResult(false, "Ingresa un correo valido", RegistroCampoError.CORREO)
        }
        if (password.isEmpty()) {
            return RegistroResult(false, "La contrasena es obligatoria", RegistroCampoError.PASSWORD)
        }
        if (password.length < 4) {
            return RegistroResult(false, "La contrasena debe tener minimo 4 caracteres", RegistroCampoError.PASSWORD)
        }
        if (confirmarPassword.isEmpty()) {
            return RegistroResult(false, "Confirma la contrasena", RegistroCampoError.CONFIRMAR_PASSWORD)
        }
        if (password != confirmarPassword) {
            return RegistroResult(false, "Las contrasenas no coinciden", RegistroCampoError.CONFIRMAR_PASSWORD)
        }

        return when (val result = withContext(Dispatchers.IO) {
            repository.registrarUsuario(nombreLimpio, correoLimpio, password)
        }) {
            is RegistroRepositoryResult.Exito -> {
                sessionManager.guardarSesion(result.usuario)
                RegistroResult(true)
            }
            is RegistroRepositoryResult.Error -> {
                RegistroResult(false, result.mensaje, RegistroCampoError.CORREO)
            }
        }
    }
}
