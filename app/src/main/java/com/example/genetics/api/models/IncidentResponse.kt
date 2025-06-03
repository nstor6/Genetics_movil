package com.example.genetics.api.models

import com.example.genetics.api.Incidencia

data class IncidentResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Incidencia>
)
