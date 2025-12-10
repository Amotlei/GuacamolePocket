package com.example.guacamolepocket.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.guacamolepocket.R
import com.example.guacamolepocket.repository.GameRepository
import com.example.guacamolepocket.view.VistaGuacamole
import com.example.guacamolepocket.viewmodel.JuegoViewModel
import com.example.guacamolepocket.viewmodel.JuegoViewModelFactory

class JuegoActivity : AppCompatActivity() {

    private lateinit var vista: VistaGuacamole
    private lateinit var tvScore: TextView
    private lateinit var tvRound: TextView
    private lateinit var btnBack: Button

    private val viewModel: JuegoViewModel by viewModels {
        JuegoViewModelFactory(GameRepository())
    }

    private var roomId: String? = null
    private var playerName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_juego)

        vista = findViewById(R.id.vistaGuacamole)
        tvScore = findViewById(R.id.tvScore)
        tvRound = findViewById(R.id.tvRound)
        btnBack = findViewById(R.id.btnBack)

        roomId = intent.getStringExtra("roomId")
        playerName = intent.getStringExtra("playerName")
        if (playerName != null) viewModel.setPlayerName(playerName!!)

        vista.setOnHitListener { spawnId ->
            // cuando el usuario toca el objetivo, notificamos al ViewModel para llamar hitTarget
            viewModel.hitTarget(spawnId)
        }

        // Observamos spawn para dibujarlo
        viewModel.spawn.observe(this) { spawn ->
            if (spawn != null) {
                vista.showSpawn(spawn.x, spawn.y, spawn.radio, spawn.spawnId)

            } else {
                vista.hideSpawn()
            }
        }

        // Observamos score y ronda
        viewModel.score.observe(this) { map ->
            // Mostrar simple: "Yo: X  - Rival: Y" (adaptar si hay más jugadores)
            val me = playerName ?: "Yo"
            val myScore = map[me] ?: 0
            // toma primer rival (si existe) para mostrar
            val rival = map.entries.firstOrNull { it.key != me }?.let { "${it.key}: ${it.value}" } ?: ""
            tvScore.text = "$me: $myScore    $rival"
        }

        viewModel.round.observe(this) { r ->
            val maxR = viewModel.maxRounds.value ?: 0
            tvRound.text = "Ronda: $r / $maxR"
        }

        btnBack.setOnClickListener { finish() }
    }
}