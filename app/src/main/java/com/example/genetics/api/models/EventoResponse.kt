package com.example.genetics.api.models

import com.example.genetics.api.Evento

data class EventoResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Evento>
)
