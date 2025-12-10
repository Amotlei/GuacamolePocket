package com.example.guacamolepocket.viewmodel

// IMPORTAR:
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.ViewModelProvider
// import com.example.guacamolepocket.repository.GameRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.guacamolepocket.repository.GameRepository

class JuegoViewModelFactory(private val repo: GameRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JuegoViewModel::class.java)) {
            return JuegoViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
