package com.example.fintrack.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fintrack.R
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileHeaderController(
    private val fragment: Fragment,
    private val imageView: ImageView
) {
    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(fragment)[ProfileViewModel::class.java]
    }
    private val sessionManager: SessionManager by lazy {
        SessionManager(fragment.requireContext())
    }

    fun configurar() {
        imageView.isClickable = true
        imageView.isFocusable = true
        imageView.contentDescription = "Abrir perfil"
        imageView.setOnClickListener {
            fragment.startActivity(Intent(fragment.requireContext(), ProfileActivity::class.java))
        }

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            fragment.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val fotoUri = state.usuario?.fotoPerfilUri
                    if (fotoUri.isNullOrBlank()) {
                        imageView.setImageResource(R.drawable.ic_person_24)
                    } else {
                        runCatching {
                            imageView.setImageURI(Uri.parse(fotoUri))
                        }.onFailure {
                            imageView.setImageResource(R.drawable.ic_person_24)
                        }
                    }
                }
            }
        }
    }

    fun recargarFoto() {
        viewModel.cargarUsuario(sessionManager.obtenerUsuarioId())
    }
}
