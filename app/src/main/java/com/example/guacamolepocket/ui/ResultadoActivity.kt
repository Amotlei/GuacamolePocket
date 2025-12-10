package com.example.guacamolepocket.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.guacamolepocket.MyApplication
import com.example.guacamolepocket.R
//import com.example.guacamolepocket.data.local.Resultado
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ResultadoActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resultado)

        tvResult = findViewById(R.id.tvResult)
        btnClose = findViewById(R.id.btnClose)

        // Se esperan extras: ganador (String), puntajes (opcional)
        val ganador = intent.getStringExtra("champion") ?: "Nadie"
        val myScore = intent.getIntExtra("myScore", 0)
        val rivalScore = intent.getIntExtra("rivalScore", 0)
        val myName = intent.getStringExtra("myName") ?: "Jugador"
        val rivalName = intent.getStringExtra("rivalName") ?: "Rival"

        tvResult.text = "Ganador: $ganador\n$myName: $myScore\n$rivalName: $rivalScore"

        // Guarda resultado en Room (AppDatabase) en background
//        GlobalScope.launch {
//            try {
//                val res = Resultado(
//                    jugador = myName,
//                    rival = rivalName,
//                    puntajeJugador = myScore,
//                    puntajeRival = rivalScore,
//                    fecha = System.currentTimeMillis()
//                )
//                MyApplication.Companion.database.resultadoDao().insertarResultado(res)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }

        btnClose.setOnClickListener { finish() }
    }
}