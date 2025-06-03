package com.example.genetics.api.models

import com.example.genetics.api.Tratamiento

data class TreatmentResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Tratamiento>
)
