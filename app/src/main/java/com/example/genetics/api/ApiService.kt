package com.example.genetics.api

import retrofit2.Response
import retrofit2.http.*

// Modelos de datos
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class Animal(
    val id: Int? = null,
    val chapeta: String,
    val nombre: String?,
    val sexo: String,
    val fecha_nacimiento: String,
    val raza: String,
    val estado_reproductivo: String?,
    val estado_productivo: String?,
    val peso_actual: Float?,
    val ubicacion_actual: String?,
    val foto_perfil_url: String?,
    val notas: String?
)

data class Incidencia(
    val id: Int? = null,
    val animal: Int,
    val tipo: String,
    val descripcion: String,
    val fecha_deteccion: String,
    val estado: String,
    val fecha_resolucion: String?
)

data class Tratamiento(
    val id: Int? = null,
    val animal: Int,
    val fecha: String,
    val medicamento: String,
    val dosis: String,
    val duracion: String,
    val administrado_por: Int?,
    val observaciones: String?
)

data class Evento(
    val id: Int? = null,
    val titulo: String,
    val descripcion: String?,
    val fecha_inicio: String,
    val fecha_fin: String?,
    val animal: Int?,
    val tipo: String,
    val recurrente: Boolean
)

// Interface para las llamadas a la API
interface ApiService {

    // Autenticación
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Animales
    @GET("animales/")
    suspend fun getAnimales(): Response<List<Animals>>

    @POST("animales/")
    suspend fun crearAnimal(@Body animals: Animals): Response<Animals>

    @PUT("animales/{id}/")
    suspend fun actualizarAnimal(@Path("id") id: Int, @Body animals: Animals): Response<Animals>

    @DELETE("animales/{id}/")
    suspend fun eliminarAnimal(@Path("id") id: Int): Response<Unit>

    // Incidencias
    @GET("incidencias/")
    suspend fun getIncidencias(): Response<List<Incidencia>>

    @POST("incidencias/")
    suspend fun crearIncidencia(@Body incidencia: Incidencia): Response<Incidencia>

    @PUT("incidencias/{id}/")
    suspend fun actualizarIncidencia(@Path("id") id: Int, @Body incidencia: Incidencia): Response<Incidencia>

    @DELETE("incidencias/{id}/")
    suspend fun eliminarIncidencia(@Path("id") id: Int): Response<Unit>

    // Tratamientos
    @GET("tratamientos/")
    suspend fun getTratamientos(): Response<List<Tratamiento>>

    @POST("tratamientos/")
    suspend fun crearTratamiento(@Body tratamiento: Tratamiento): Response<Tratamiento>

    @PUT("tratamientos/{id}/")
    suspend fun actualizarTratamiento(@Path("id") id: Int, @Body tratamiento: Tratamiento): Response<Tratamiento>

    @DELETE("tratamientos/{id}/")
    suspend fun eliminarTratamiento(@Path("id") id: Int): Response<Unit>

    // Eventos
    @GET("eventos/")
    suspend fun getEventos(): Response<List<Evento>>

    @POST("eventos/")
    suspend fun crearEvento(@Body evento: Evento): Response<Evento>

    @PUT("eventos/{id}/")
    suspend fun actualizarEvento(@Path("id") id: Int, @Body evento: Evento): Response<Evento>

    @DELETE("eventos/{id}/")
    suspend fun eliminarEvento(@Path("id") id: Int): Response<Unit>
}