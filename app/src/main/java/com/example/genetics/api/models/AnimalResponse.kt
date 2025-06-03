package com.example.genetics.api.models

data class AnimalResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Animals>
)
