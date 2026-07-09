package com.example.fintrack.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fintrack.databinding.ActivityLoginBinding
import com.example.fintrack.ui.home.HomeActivity
import com.example.fintrack.ui.registro.RegistroActivity
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.LoginCampoError
import com.example.fintrack.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager(this).estaLogueado()) {
            abrirHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        binding.btnCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    private fun iniciarSesion() {
        limpiarErrores()

        lifecycleScope.launch {
            val result = viewModel.iniciarSesion(
                correo = binding.etCorreo.text.toString(),
                password = binding.etPassword.text.toString()
            )

            if (result.exitoso) {
                abrirHome()
            } else {
                mostrarError(result.campoError, result.mensaje.orEmpty())
            }
        }
    }

    private fun mostrarError(campoError: LoginCampoError?, mensaje: String) {
        when (campoError) {
            LoginCampoError.CORREO -> binding.etCorreo.error = mensaje
            LoginCampoError.PASSWORD -> binding.etPassword.error = mensaje
            LoginCampoError.GENERAL, null -> Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarErrores() {
        binding.etCorreo.error = null
        binding.etPassword.error = null
    }

    private fun abrirHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
