package com.example.fintrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.fintrack.data.local.entity.Usuario

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE correo = :correo AND password = :password LIMIT 1")
    suspend fun login(correo: String, password: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios WHERE correo = :correo")
    suspend fun existeCorreo(correo: String): Int

    @Query("DELETE FROM usuarios WHERE id = :id")
    suspend fun eliminarUsuarioPorId(id: Long): Int

    @Query("UPDATE usuarios SET fotoPerfilUri = :fotoPerfilUri WHERE id = :usuarioId")
    suspend fun actualizarFotoPerfil(usuarioId: Long, fotoPerfilUri: String)
}
