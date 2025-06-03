package com.example.genetics.api

import com.google.gson.annotations.SerializedName

// ========== MODELO PRINCIPAL DE USUARIO ==========

data class Usuario(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("apellidos")
    val apellidos: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("rol")
    val rol: String? = null,

    @SerializedName("activo")
    val activo: Boolean? = true,

    @SerializedName("fecha_creacion")
    val fechaCreacion: String? = null,

    @SerializedName("ultimo_acceso")
    val ultimoAcceso: String? = null,

    @SerializedName("is_staff")
    val isStaff: Boolean? = false
)

data class UserProfile(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("user")
    val userId: Int? = null,

    @SerializedName("telefono")
    val telefono: String? = null,

    @SerializedName("cargo")
    val cargo: String? = null,

    @SerializedName("departamento")
    val departamento: String? = null,

    @SerializedName("fecha_nacimiento")
    val fechaNacimiento: String? = null,

    @SerializedName("direccion")
    val direccion: String? = null,

    @SerializedName("foto_perfil")
    val fotoPerfil: String? = null,

    @SerializedName("biografia")
    val biografia: String? = null,

    @SerializedName("especialidad")
    val especialidad: String? = null,

    @SerializedName("años_experiencia")
    val anosExperiencia: Int? = null,

    @SerializedName("certificaciones")
    val certificaciones: String? = null
)

// ========== DTOs PARA REQUESTS ESPECÍFICOS ==========

data class CreateUserRequest(
    val nombre: String,
    val apellidos: String,
    val email: String,
    val password: String,
    val rol: String = "usuario"
)

data class UpdateUserRequest(
    val nombre: String? = null,
    val apellidos: String? = null,
    val email: String? = null,
    val rol: String? = null,
    val activo: Boolean? = null
)

data class UpdateProfileRequest(
    val telefono: String? = null,
    val cargo: String? = null,
    val departamento: String? = null,
    val fecha_nacimiento: String? = null,
    val direccion: String? = null,
    val biografia: String? = null,
    val especialidad: String? = null,
    val años_experiencia: Int? = null,
    val certificaciones: String? = null
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)

data class UserStatsResponse(
    @SerializedName("animales_creados")
    val animalesCreados: Int = 0,

    @SerializedName("incidencias_reportadas")
    val incidenciasReportadas: Int = 0,

    @SerializedName("tratamientos_administrados")
    val tratamientosAdministrados: Int = 0,

    @SerializedName("eventos_creados")
    val eventosCreados: Int = 0,

    @SerializedName("ultimo_acceso")
    val ultimoAcceso: String? = null
)