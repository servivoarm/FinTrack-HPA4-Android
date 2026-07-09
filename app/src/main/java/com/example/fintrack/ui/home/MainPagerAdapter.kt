package com.example.fintrack.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fintrack.ui.gastos.GastosFragment
import com.example.fintrack.ui.ingresos.IngresosFragment
import com.example.fintrack.ui.presupuestos.PresupuestosFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = TOTAL_PAGINAS

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> IngresosFragment()
            2 -> GastosFragment()
            3 -> PresupuestosFragment()
            else -> HomeFragment()
        }
    }

    companion object {
        const val TOTAL_PAGINAS = 4
    }
}
