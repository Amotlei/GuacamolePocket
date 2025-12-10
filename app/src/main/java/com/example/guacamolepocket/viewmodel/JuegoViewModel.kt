package com.example.guacamolepocket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.guacamolepocket.repository.GameRepository
import com.example.guacamolepocket.repository.SpawnData
import com.example.guacamolepocket.repository.RoomState
import android.util.Log
import com.example.guacamolepocket.data.model.Spawn

/**
 * JuegoViewModel: expone estado a la UI y coordina llamadas al repositorio.
 */
class JuegoViewModel(private val repo: GameRepository) : ViewModel() {

    private val _roomId = MutableLiveData<String?>()
    val roomId: LiveData<String?> = _roomId

    private val _playerName = MutableLiveData<String?>()
    val playerName: LiveData<String?> = _playerName

    // <-- Cambiado: ahora exponemos Spawn (UI model), no SpawnData (repo model)
    private val _spawn = MutableLiveData<Spawn?>()
    val spawn: LiveData<Spawn?> = _spawn

    private val _score = MutableLiveData<Map<String, Int>>(emptyMap())
    val score: LiveData<Map<String, Int>> = _score

    private val _round = MutableLiveData<Int>(0)
    val round: LiveData<Int> = _round

    private val _maxRounds = MutableLiveData<Int>(0)
    val maxRounds: LiveData<Int> = _maxRounds

    private val _gameEnded = MutableLiveData<Boolean>(false)
    val gameEnded: LiveData<Boolean> = _gameEnded

    // Mensaje de estado/error para UI
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // Iniciar nombre jugador
    fun setPlayerName(name: String) {
        _playerName.postValue(name)
    }

    // Llama joinRoom → arranca polling
    fun joinRoom(code: String, player: String) {
        _playerName.postValue(player)
        viewModelScope.launch {
            try {
                val rid = repo.joinRoom(code)
                _roomId.postValue(rid)
                _message.postValue("Sala unida: $rid")

                // arranca el polling si rid no es nulo
                if (rid != null) startPollingRoom(rid)

            } catch (e: Exception) {
                Log.e("VM", "joinRoom error: ${e.message}")
                _message.postValue("Error al unir a la sala: ${e.message}")
            }
        }
    }

    fun startGame() {
        val rid = _roomId.value ?: return
        viewModelScope.launch {
            try {
                repo.startGame(rid)
                _message.postValue("Juego iniciado")
            } catch (e: Exception) {
                _message.postValue("Error al iniciar: ${e.message}")
            }
        }
    }

    fun hitTarget(spawnId: String) {
        val rid = _roomId.value ?: return
        val player = _playerName.value ?: "Jugador"

        viewModelScope.launch {
            try {
                repo.hitTarget(rid, spawnId, player)
                // Los cambios reales vendrán desde getRoomState en polling
            } catch (e: Exception) {
                _message.postValue("Error al registrar toque: ${e.message}")
            }
        }
    }

    /**
     * Polling usando getRoomState (Cloud Function real)
     */
    private fun startPollingRoom(roomId: String) {
        viewModelScope.launch {
            while (true) {
                try {
                    val state: RoomState = repo.getRoomState(roomId)

                    // Actualizar score / rounds / maxRounds
                    _score.postValue(state.score)
                    _round.postValue(state.round)
                    _maxRounds.postValue(state.maxRounds)

                    // Detectar fin del juego
                    if (state.round >= state.maxRounds && _gameEnded.value != true) {
                        _gameEnded.postValue(true)
                    }

                    // Convertir SpawnData -> Spawn (UI model) y exponerla
                    _spawn.postValue(state.lastSpawn?.toSpawn())

                } catch (e: Exception) {
                    Log.e("VM", "Polling error: ${e.message}")
                    // opcional: podrías postear un mensaje de usuario
                }

                delay(350) // intervalo de polling
            }
        }
    }

    // EXTENSION: convierte SpawnData (repositorio) a Spawn (UI)
    private fun SpawnData.toSpawn(): Spawn? {
        if (this.spawnId == null || this.cx == null || this.cy == null || this.r == null) return null
        return Spawn(
            spawnId = this.spawnId,
            x = this.cx,
            y = this.cy,
            radio = this.r
        )
    }

}
