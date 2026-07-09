package com.example.fintrack.utils

import android.content.Context
import com.example.fintrack.data.local.entity.Usuario

class SessionManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun guardarSesion(usuario: Usuario) {
        preferences.edit()
            .putLong(KEY_USUARIO_ID, usuario.id)
            .putString(KEY_NOMBRE, usuario.nombre)
            .putString(KEY_CORREO, usuario.correo)
            .putBoolean(KEY_LOGUEADO, true)
            .apply()
    }

    fun estaLogueado(): Boolean {
        return preferences.getBoolean(KEY_LOGUEADO, false)
    }

    fun obtenerNombre(): String {
        return preferences.getString(KEY_NOMBRE, "") ?: ""
    }

    fun obtenerCorreo(): String {
        return preferences.getString(KEY_CORREO, "") ?: ""
    }

    fun obtenerUsuarioId(): Long {
        return preferences.getLong(KEY_USUARIO_ID, 0)
    }

    fun guardarMoneda(moneda: String) {
        preferences.edit()
            .putString(KEY_MONEDA, moneda)
            .apply()
    }

    fun obtenerMoneda(): String {
        return preferences.getString(KEY_MONEDA, DEFAULT_MONEDA) ?: DEFAULT_MONEDA
    }

    fun cerrarSesion() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREF_NAME = "fintrack_session"
        const val KEY_USUARIO_ID = "usuarioId"
        const val KEY_NOMBRE = "nombre"
        const val KEY_CORREO = "correo"
        const val KEY_LOGUEADO = "logueado"
        const val KEY_MONEDA = "moneda"
        const val DEFAULT_MONEDA = "USD"
    }
}
