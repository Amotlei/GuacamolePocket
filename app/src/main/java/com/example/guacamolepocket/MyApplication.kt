package com.example.guacamolepocket

import android.app.Application
import com.parse.Parse
import com.parse.livequery.ParseLiveQueryClient
import androidx.room.Room
import com.example.guacamolepocket.data.local.AppDatabase

class MyApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
        // si quieres exponer el client globalmente:
        var liveQueryClient: ParseLiveQueryClient? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Inicializar Parse (Back4App)
        Parse.initialize(
            Parse.Configuration.Builder(this)
                .applicationId("PM6vQTF1E4lAIQ0998UwAaqbke5ZkyfJ3QpGT8cJ")
                .clientKey("1PJPIgYlMdiqaqhjJeoDZf0ngnX899Nas6qCc5gn")
                .server("https://parseapi.back4app.com/")
                .build()
        )

        // Inicializar client LiveQuery (Factory.getClient() requiere que Parse esté inicializado)
        try {
            liveQueryClient = ParseLiveQueryClient.Factory.getClient()
        } catch (ex: Exception) {
            // si la versión del SDK cambia, el factory puede diferir; captura y loggea.
            ex.printStackTrace()
        }

        // Inicializar Room
        database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "guacamole_db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
