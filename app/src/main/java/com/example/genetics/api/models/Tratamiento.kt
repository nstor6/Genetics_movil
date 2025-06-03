package com.example.genetics.api

import com.google.gson.annotations.SerializedName

data class Tratamiento(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("animal")
    val animal: Int,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("medicamento")
    val medicamento: String,

    @SerializedName("dosis")
    val dosis: String,

    @SerializedName("duracion")
    val duracion: String,

    @SerializedName("administrado_por")
    val administrado_por: Int? = null,

    @SerializedName("observaciones")
    val observaciones: String? = null
)