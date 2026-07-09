package com.example.fintrack.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.fintrack.R
import com.example.fintrack.databinding.ActivityHomeBinding
import com.example.fintrack.ui.login.LoginActivity
import com.example.fintrack.utils.SessionManager

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var sessionManager: SessionManager
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "No se podrán mostrar recordatorios sin permiso de notificaciones",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val itemId = itemIdPorPosicion(position)
            if (binding.bottomNavigation.selectedItemId != itemId) {
                binding.bottomNavigation.selectedItemId = itemId
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (!sessionManager.estaLogueado()) {
            volverAlLogin()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        solicitarPermisoNotificaciones()

        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = MainPagerAdapter.TOTAL_PAGINAS
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.viewPager.setCurrentItem(posicionPorItemId(item.itemId), true)
            true
        }

    }

    override fun onResume() {
        super.onResume()
        if (::sessionManager.isInitialized && !sessionManager.estaLogueado()) {
            volverAlLogin()
        }
    }

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        }
        super.onDestroy()
    }

    private fun volverAlLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun posicionPorItemId(itemId: Int): Int {
        return when (itemId) {
            R.id.nav_ingresos -> 1
            R.id.nav_gastos -> 2
            R.id.nav_presupuestos -> 3
            else -> 0
        }
    }

    private fun itemIdPorPosicion(position: Int): Int {
        return when (position) {
            1 -> R.id.nav_ingresos
            2 -> R.id.nav_gastos
            3 -> R.id.nav_presupuestos
            else -> R.id.nav_inicio
        }
    }
}
