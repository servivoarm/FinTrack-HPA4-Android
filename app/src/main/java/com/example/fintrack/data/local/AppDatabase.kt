package com.example.fintrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fintrack.data.local.dao.AporteAhorroDao
import com.example.fintrack.data.local.dao.MovimientoDao
import com.example.fintrack.data.local.dao.ObjetivoAhorroDao
import com.example.fintrack.data.local.dao.PresupuestoCategoriaDao
import com.example.fintrack.data.local.dao.UsuarioDao
import com.example.fintrack.data.local.entity.AporteAhorro
import com.example.fintrack.data.local.entity.MovimientoFinanciero
import com.example.fintrack.data.local.entity.ObjetivoAhorro
import com.example.fintrack.data.local.entity.PresupuestoCategoria
import com.example.fintrack.data.local.entity.Usuario

@Database(
    entities = [
        Usuario::class,
        MovimientoFinanciero::class,
        ObjetivoAhorro::class,
        AporteAhorro::class,
        PresupuestoCategoria::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun objetivoAhorroDao(): ObjetivoAhorroDao
    abstract fun aporteAhorroDao(): AporteAhorroDao
    abstract fun presupuestoCategoriaDao(): PresupuestoCategoriaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun obtenerInstancia(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fintrack.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
