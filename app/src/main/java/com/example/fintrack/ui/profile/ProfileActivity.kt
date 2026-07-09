package com.example.fintrack.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.fintrack.FinTrackApplication
import com.example.fintrack.R
import com.example.fintrack.databinding.ActivityProfileBinding
import com.example.fintrack.ui.login.LoginActivity
import com.example.fintrack.utils.SessionManager
import com.example.fintrack.viewmodel.ProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: ProfileViewModel by viewModels()
    private val monedas = listOf("USD", "PAB", "EUR", "COP", "MXN")
    private var uriCamaraPendiente: Uri? = null

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        reanudarCierreAutomatico()
        if (uri != null) {
            guardarFotoDesdeGaleria(uri)
        }
    }

    private val tomarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exitoso ->
        reanudarCierreAutomatico()
        val uri = uriCamaraPendiente
        if (exitoso && uri != null) {
            guardarFotoPerfil(uri)
        } else {
            Toast.makeText(this, "No se guardo la foto", Toast.LENGTH_SHORT).show()
        }
        uriCamaraPendiente = null
    }

    private val permisoCamaraLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (!sessionManager.estaLogueado()) {
            abrirLogin()
            return
        }

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvNombre.text = sessionManager.obtenerNombre()
        binding.tvCorreo.text = sessionManager.obtenerCorreo()
        binding.tvMonedaActual.text = sessionManager.obtenerMoneda()

        configurarSpinnerMoneda()
        observarPerfil()
        viewModel.cargarUsuario(sessionManager.obtenerUsuarioId())

        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnCambiarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        binding.btnCerrarSesion.setOnClickListener {
            sessionManager.cerrarSesion()
            abrirLogin()
        }

        binding.btnEliminarCuenta.setOnClickListener {
            confirmarEliminacion()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::sessionManager.isInitialized && !sessionManager.estaLogueado()) {
            abrirLogin()
        }
    }

    private fun observarPerfil() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val fotoUri = state.usuario?.fotoPerfilUri
                    if (fotoUri.isNullOrBlank()) {
                        binding.ivUsuario.setImageResource(R.drawable.ic_person_24)
                    } else {
                        mostrarFoto(Uri.parse(fotoUri))
                    }
                }
            }
        }
    }

    private fun configurarSpinnerMoneda() {
        val adapter = ArrayAdapter(this, com.example.fintrack.R.layout.spinner_item_selected, monedas).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerMoneda.adapter = adapter
        binding.spinnerMoneda.setSelection(monedas.indexOf(sessionManager.obtenerMoneda()).coerceAtLeast(0))

        var primeraSeleccion = true
        binding.spinnerMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (primeraSeleccion) {
                    primeraSeleccion = false
                    return
                }

                val moneda = monedas[position]
                sessionManager.guardarMoneda(moneda)
                binding.tvMonedaActual.text = moneda
                Toast.makeText(this@ProfileActivity, "Moneda cambiada a $moneda", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun confirmarEliminacion() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("¿Seguro que deseas eliminar tu cuenta? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCuenta()
            }
            .show()
    }

    private fun mostrarOpcionesFoto() {
        AlertDialog.Builder(this)
            .setTitle("Cambiar foto")
            .setItems(arrayOf("Galeria", "Camara")) { _, which ->
                when (which) {
                    0 -> abrirGaleria()
                    1 -> solicitarCamara()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirGaleria() {
        pausarCierreAutomatico()
        runCatching {
            seleccionarImagenLauncher.launch(arrayOf("image/*"))
        }.onFailure {
            reanudarCierreAutomatico()
            Toast.makeText(this, "No se pudo abrir la galeria", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val uri = crearUriFotoCamara()
        if (uri == null) {
            Toast.makeText(this, "No se pudo preparar la camara", Toast.LENGTH_SHORT).show()
            return
        }

        uriCamaraPendiente = uri
        pausarCierreAutomatico()
        runCatching {
            tomarFotoLauncher.launch(uri)
        }.onFailure {
            reanudarCierreAutomatico()
            uriCamaraPendiente = null
            Toast.makeText(this, "No se pudo abrir la camara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarFotoDesdeGaleria(uri: Uri) {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        }
        guardarFotoPerfil(uri)
    }

    private fun guardarFotoPerfil(uri: Uri) {
        mostrarFoto(uri)
        viewModel.actualizarFotoPerfil(sessionManager.obtenerUsuarioId(), uri.toString())
        Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarFoto(uri: Uri) {
        runCatching {
            binding.ivUsuario.setImageURI(uri)
        }.onFailure {
            binding.ivUsuario.setImageResource(R.drawable.ic_person_24)
        }
    }

    private fun crearUriFotoCamara(): Uri? {
        return runCatching {
            val directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (directorio?.exists() == false) {
                directorio.mkdirs()
            }
            val nombre = "perfil_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            val archivo = File(directorio, nombre)
            FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
        }.getOrNull()
    }

    private fun pausarCierreAutomatico() {
        (application as? FinTrackApplication)?.pausarCierreAutomatico()
    }

    private fun reanudarCierreAutomatico() {
        (application as? FinTrackApplication)?.reanudarCierreAutomatico()
    }

    private fun eliminarCuenta() {
        val usuarioId = sessionManager.obtenerUsuarioId()

        lifecycleScope.launch {
            viewModel.eliminarUsuarioPorId(usuarioId)
            sessionManager.cerrarSesion()
            abrirLogin()
        }
    }

    private fun abrirLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
