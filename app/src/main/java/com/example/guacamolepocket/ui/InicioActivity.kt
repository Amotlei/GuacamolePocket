package com.example.guacamolepocket.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.guacamolepocket.R
import com.example.guacamolepocket.repository.GameRepository
import com.example.guacamolepocket.viewmodel.JuegoViewModel
import com.example.guacamolepocket.viewmodel.JuegoViewModelFactory

class InicioActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etCode: EditText
    private lateinit var btnJoin: Button
    private lateinit var btnStart: Button

    // Instanciamos ViewModel con fábrica (ver nota al final)
    private val viewModel: JuegoViewModel by viewModels {
        JuegoViewModelFactory(GameRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        etName = findViewById(R.id.etName)
        etCode = findViewById(R.id.etCode)
        btnJoin = findViewById(R.id.btnJoin)
        btnStart = findViewById(R.id.btnStart)

        btnJoin.setOnClickListener {
            val name = etName.text.toString().ifBlank { "Jugador" }
            val code = etCode.text.toString().ifBlank { "0000" }
            viewModel.joinRoom(code, name)
            // guarda name en viewmodel
            viewModel.setPlayerName(name)
            // ir a JuegoActivity pasando roomId será manejado cuando ViewModel actualice roomId
            viewModel.roomId.observe(this) { rid ->
                if (rid != null) {
                    val i = Intent(this, JuegoActivity::class.java)
                    i.putExtra("roomId", rid)
                    i.putExtra("playerName", name)
                    startActivity(i)
                }
            }
        }

        btnStart.setOnClickListener {
            // si eres anfitrión, iniciar juego. suponemos que el flujo de joinRoom ya se hizo.
            viewModel.startGame()
        }
    }
}