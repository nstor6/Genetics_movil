package com.example.genetics.api

import com.google.gson.annotations.SerializedName

data class Evento(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("fecha_inicio")
    val fecha_inicio: String,

    @SerializedName("fecha_fin")
    val fecha_fin: String? = null,

    @SerializedName("animal")
    val animal: Int? = null,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("recurrente")
    val recurrente: Boolean = false,

    @SerializedName("creado_por")
    val creado_por: Int? = null
)