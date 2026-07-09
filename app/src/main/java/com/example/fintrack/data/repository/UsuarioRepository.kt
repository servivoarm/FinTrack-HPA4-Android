package com.example.fintrack.data.repository

import com.example.fintrack.data.local.dao.UsuarioDao
import com.example.fintrack.data.local.entity.Usuario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class RegistroRepositoryResult {
    data class Exito(val usuario: Usuario) : RegistroRepositoryResult()
    data class Error(val mensaje: String) : RegistroRepositoryResult()
}

class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {
    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        password: String
    ): RegistroRepositoryResult {
        val correoNormalizado = normalizarCorreo(correo)

        if (existeCorreo(correoNormalizado)) {
            return RegistroRepositoryResult.Error("Ya existe una cuenta con este correo")
        }

        val usuario = Usuario(
            nombre = nombre.trim(),
            correo = correoNormalizado,
            password = password,
            fechaRegistro = fechaActual()
        )

        val id = usuarioDao.insertar(usuario)
        return RegistroRepositoryResult.Exito(usuario.copy(id = id))
    }

    suspend fun iniciarSesion(correo: String, password: String): Usuario? {
        return usuarioDao.login(normalizarCorreo(correo), password)
    }

    suspend fun obtenerUsuarioPorId(usuarioId: Long): Usuario? {
        return usuarioDao.obtenerPorId(usuarioId)
    }

    suspend fun existeCorreo(correo: String): Boolean {
        return usuarioDao.existeCorreo(normalizarCorreo(correo)) > 0
    }

    suspend fun eliminarUsuarioPorId(id: Long) {
        usuarioDao.eliminarUsuarioPorId(id)
    }

    suspend fun actualizarFotoPerfil(usuarioId: Long, uri: String) {
        usuarioDao.actualizarFotoPerfil(usuarioId, uri)
    }

    private fun normalizarCorreo(correo: String): String {
        return correo.trim().lowercase(Locale.getDefault())
    }

    private fun fechaActual(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
