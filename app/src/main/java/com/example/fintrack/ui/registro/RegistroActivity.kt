package com.example.fintrack.ui.registro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fintrack.databinding.ActivityRegistroBinding
import com.example.fintrack.ui.home.HomeActivity
import com.example.fintrack.ui.login.LoginActivity
import com.example.fintrack.viewmodel.RegistroCampoError
import com.example.fintrack.viewmodel.RegistroViewModel
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCrearCuenta.setOnClickListener {
            registrarUsuario()
        }

        binding.btnVolverLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registrarUsuario() {
        limpiarErrores()

        lifecycleScope.launch {
            val result = viewModel.registrarUsuario(
                nombre = binding.etNombre.text.toString(),
                correo = binding.etCorreo.text.toString(),
                password = binding.etPassword.text.toString(),
                confirmarPassword = binding.etConfirmarPassword.text.toString()
            )

            if (result.exitoso) {
                abrirHome()
            } else {
                mostrarError(result.campoError, result.mensaje.orEmpty())
            }
        }
    }

    private fun mostrarError(campoError: RegistroCampoError?, mensaje: String) {
        when (campoError) {
            RegistroCampoError.NOMBRE -> binding.etNombre.error = mensaje
            RegistroCampoError.CORREO -> binding.etCorreo.error = mensaje
            RegistroCampoError.PASSWORD -> binding.etPassword.error = mensaje
            RegistroCampoError.CONFIRMAR_PASSWORD -> binding.etConfirmarPassword.error = mensaje
            RegistroCampoError.GENERAL, null -> Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarErrores() {
        binding.etNombre.error = null
        binding.etCorreo.error = null
        binding.etPassword.error = null
        binding.etConfirmarPassword.error = null
    }

    private fun abrirHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
