package com.example.fintrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.data.local.entity.Usuario
import com.example.fintrack.data.local.AppDatabase
import com.example.fintrack.data.repository.UsuarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val usuario: Usuario? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UsuarioRepository(
        AppDatabase.obtenerInstancia(application).usuarioDao()
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun cargarUsuario(usuarioId: Long) {
        viewModelScope.launch {
            val usuario = withContext(Dispatchers.IO) {
                repository.obtenerUsuarioPorId(usuarioId)
            }
            _uiState.value = ProfileUiState(usuario)
        }
    }

    fun actualizarFotoPerfil(usuarioId: Long, uri: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.actualizarFotoPerfil(usuarioId, uri)
            }
            cargarUsuario(usuarioId)
        }
    }

    suspend fun eliminarUsuarioPorId(usuarioId: Long) {
        withContext(Dispatchers.IO) {
            repository.eliminarUsuarioPorId(usuarioId)
        }
    }
}
