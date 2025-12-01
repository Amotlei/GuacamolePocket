package com.example.guacamolepocket.data.model

data class Evento(
    val tipo: String = "",
    val data: Map<String, Any>? = null,
    val createdAt: String? = null
)
