package com.example.genetics.api.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo principal para Animals
 * Este es el modelo que se usa en toda la aplicación
 */
data class Animals(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("chapeta")
    val chapeta: String? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("sexo")
    val sexo: String? = null,

    @SerializedName("fecha_nacimiento")
    val fecha_nacimiento: String? = null,

    @SerializedName("raza")
    val raza: String? = null,

    @SerializedName("estado_reproductivo")
    val estado_reproductivo: String? = null,

    @SerializedName("estado_productivo")
    val estado_productivo: String? = null,

    @SerializedName("salud")
    val salud: List<Any>? = null,

    @SerializedName("produccion")
    val produccion: List<Any>? = null,

    @SerializedName("peso_actual")
    val peso_actual: Double? = null,

    @SerializedName("ubicacion_actual")
    val ubicacion_actual: String? = null,

    @SerializedName("historial_movimientos")
    val historial_movimientos: List<Any>? = null,

    @SerializedName("descendencia")
    val descendencia: List<Int>? = null,

    @SerializedName("fecha_alta_sistema")
    val fecha_alta_sistema: String? = null,

    @SerializedName("fecha_baja_sistema")
    val fecha_baja_sistema: String? = null,

    @SerializedName("foto_perfil_url")
    val foto_perfil_url: String? = null,

    @SerializedName("notas")
    val notas: String? = null,

    @SerializedName("creado_por")
    val creado_por: Int? = null,

    @SerializedName("modificado_por")
    val modificado_por: Int? = null
) {
    companion object {
        /**
         * Crear instancia vacía para formularios
         */
        fun empty(): Animals {
            return Animals()
        }

        /**
         * Validar que los campos obligatorios estén presentes
         */
        fun Animals.isValid(): Boolean {
            return !chapeta.isNullOrBlank() &&
                    !sexo.isNullOrBlank() &&
                    !fecha_nacimiento.isNullOrBlank() &&
                    !raza.isNullOrBlank()
        }
    }
}