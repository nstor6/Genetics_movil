package com.example.genetics.api

import com.google.gson.annotations.SerializedName

data class Incidencia(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("animal")
    val animal: Int,

    @SerializedName("tipo")
    val tipo: String,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("fecha_deteccion")
    val fecha_deteccion: String,

    @SerializedName("estado")
    val estado: String,

    @SerializedName("fecha_resolucion")
    val fecha_resolucion: String? = null,

    @SerializedName("creado_por")
    val creado_por: Int? = null,

    @SerializedName("reportado_por")
    val reportado_por: Int? = null
)