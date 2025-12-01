package com.example.guacamolepocket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ResultadoDao {

    @Insert
    suspend fun insertarResultado(resultado: Resultado)

    @Query("SELECT * FROM resultados ORDER BY fecha DESC")
    suspend fun obtenerHistorial(): List<Resultado>
}