package com.example.genetics.api

import com.example.genetics.api.models.AnimalResponse
import com.example.genetics.api.models.Animals
import com.example.genetics.api.models.EventoResponse
import com.example.genetics.api.models.IncidentResponse
import com.example.genetics.api.models.TreatmentResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ========== MODELOS DE REQUEST/RESPONSE ==========

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

// ========== INTERFACE PRINCIPAL ==========

interface ApiService {

    // ========== AUTENTICACIÓN ==========

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh/")
    suspend fun refreshToken(@Body refreshToken: Map<String, String>): Response<LoginResponse>

    @GET("auth/me/")
    suspend fun getCurrentUser(): Response<Usuario>

    @POST("auth/logout/")
    suspend fun logout(): Response<Unit>

    // ========== ANIMALES ==========

    @GET("animales/")
    suspend fun getAnimales(): Response<AnimalResponse>

    @GET("animales/{id}/")
    suspend fun getAnimal(@Path("id") id: Int): Response<Animals>

    @POST("animales/")
    suspend fun crearAnimal(@Body animal: Animals): Response<Animals>

    @PUT("animales/{id}/")
    suspend fun actualizarAnimal(@Path("id") id: Int, @Body animal: Animals): Response<Animals>

    @DELETE("animales/{id}/")
    suspend fun eliminarAnimal(@Path("id") id: Int): Response<Unit>

    @Multipart
    @POST("animales/{id}/subir_imagen/")
    suspend fun subirImagenAnimal(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>

    @DELETE("animales/{id}/eliminar_imagen/")
    suspend fun eliminarImagenAnimal(@Path("id") id: Int): Response<Unit>

    // ========== INCIDENCIAS ==========

    @GET("incidencias/")
    suspend fun getIncidencias(): Response<IncidentResponse>

    @GET("incidencias/{id}/")
    suspend fun getIncidencia(@Path("id") id: Int): Response<Incidencia>

    @POST("incidencias/")
    suspend fun crearIncidencia(@Body incidencia: Incidencia): Response<Incidencia>

    @PUT("incidencias/{id}/")
    suspend fun actualizarIncidencia(@Path("id") id: Int, @Body incidencia: Incidencia): Response<Incidencia>

    @DELETE("incidencias/{id}/")
    suspend fun eliminarIncidencia(@Path("id") id: Int): Response<Unit>

    // ========== TRATAMIENTOS ==========

    @GET("tratamientos/")
    suspend fun getTratamientos(): Response<TreatmentResponse>

    @GET("tratamientos/{id}/")
    suspend fun getTratamiento(@Path("id") id: Int): Response<Tratamiento>

    @POST("tratamientos/")
    suspend fun crearTratamiento(@Body tratamiento: Tratamiento): Response<Tratamiento>

    @PUT("tratamientos/{id}/")
    suspend fun actualizarTratamiento(@Path("id") id: Int, @Body tratamiento: Tratamiento): Response<Tratamiento>

    @DELETE("tratamientos/{id}/")
    suspend fun eliminarTratamiento(@Path("id") id: Int): Response<Unit>

    // ========== EVENTOS ==========

    @GET("eventos/")
    suspend fun getEventos(): Response<EventoResponse>

    @GET("eventos/{id}/")
    suspend fun getEvento(@Path("id") id: Int): Response<Evento>

    @POST("eventos/")
    suspend fun crearEvento(@Body evento: Evento): Response<Evento>

    @PUT("eventos/{id}/")
    suspend fun actualizarEvento(@Path("id") id: Int, @Body evento: Evento): Response<Evento>

    @DELETE("eventos/{id}/")
    suspend fun eliminarEvento(@Path("id") id: Int): Response<Unit>

    // ========== NOTIFICACIONES ==========

    @GET("notificaciones/")
    suspend fun getNotificaciones(): Response<List<Notificacion>>

    @PUT("notificaciones/{id}/")
    suspend fun actualizarNotificacion(@Path("id") id: Int, @Body notificacion: Notificacion): Response<Notificacion>

    @DELETE("notificaciones/{id}/")
    suspend fun eliminarNotificacion(@Path("id") id: Int): Response<Unit>

    // ========== USUARIOS (Solo Admin) ==========

    @GET("auth/")
    suspend fun getUsers(): Response<List<Usuario>>

    @GET("auth/{id}/")
    suspend fun getUser(@Path("id") id: Int): Response<Usuario>

    @POST("auth/register/")
    suspend fun createUser(@Body request: CreateUserRequest): Response<Usuario>

    @PUT("auth/{id}/")
    suspend fun updateUser(@Path("id") id: Int, @Body request: UpdateUserRequest): Response<Usuario>

    @DELETE("auth/{id}/")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>

    @PATCH("auth/{id}/toggle-active/")
    suspend fun toggleUserActive(@Path("id") id: Int): Response<Usuario>

    // ========== PERFIL DE USUARIO ==========

    @GET("auth/profile/")
    suspend fun getCurrentUserProfile(): Response<UserProfile>

    @PUT("auth/profile/")
    suspend fun updateCurrentUserProfile(@Body request: UpdateProfileRequest): Response<UserProfile>

    @Multipart
    @POST("auth/profile/upload-photo/")
    suspend fun uploadProfilePhoto(@Part photo: MultipartBody.Part): Response<UserProfile>

    @POST("auth/change-password/")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    // ========== LOGS (Solo Admin) ==========

    @GET("logs/")
    suspend fun getLogs(): Response<List<Log>>

    // ========== GRUPOS ==========

    @GET("grupos/")
    suspend fun getGrupos(): Response<List<Grupo>>

    @POST("grupos/")
    suspend fun crearGrupo(@Body grupo: Grupo): Response<Grupo>

    @PUT("grupos/{id}/")
    suspend fun actualizarGrupo(@Path("id") id: Int, @Body grupo: Grupo): Response<Grupo>

    @DELETE("grupos/{id}/")
    suspend fun eliminarGrupo(@Path("id") id: Int): Response<Unit>

    // ========== ENDPOINTS DE SALUD ==========

    @GET("health/")
    suspend fun healthCheck(): Response<HealthResponse>

    @GET("status/")
    suspend fun getStatus(): Response<StatusResponse>
}

// ========== MODELOS DE RESPUESTA AUXILIARES ==========

data class ImageUploadResponse(
    val success: Boolean,
    val url: String,
    val message: String
)

data class HealthResponse(
    val status: String,
    val timestamp: String,
    val version: String,
    val database: String
)

data class StatusResponse(
    val api_version: String,
    val endpoints: Map<String, String>,
    val server_time: String,
    val status: String
)

data class Log(
    val id: Int? = null,
    val usuario: Int? = null,
    val tipo_accion: String,
    val entidad_afectada: String,
    val entidad_id: String,
    val fecha_hora: String? = null,
    val cambios: Map<String, Any>? = null,
    val observaciones: String? = null
)

data class Grupo(
    val id: Int? = null,
    val nombre: String,
    val descripcion: String? = null,
    val tipo: String,
    val animal_ids: List<Int>? = null,
    val fecha_creacion: String? = null,
    val estado_actual: String? = null
)

data class Notificacion(
    val id: Int? = null,
    val usuario: Int,
    val mensaje: String,
    val tipo: String,
    val fecha_creacion: String? = null,
    val visto: Boolean = false,
    val relacionado_con_animal: Int? = null,
    val relacionado_con_evento: Int? = null
)