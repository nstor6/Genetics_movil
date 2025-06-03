package com.example.genetics.utils

import android.util.Log
import retrofit2.Response

/**
 * Llamada segura a API con manejo automático de errores
 * Retorna directamente la Response para mantener compatibilidad
 */
suspend fun <T> safeApiCall(
    tag: String = "API_CALL",
    apiCall: suspend () -> Response<T>
): SafeApiResult<T> {
    return try {
        Log.d(tag, "🔄 Iniciando llamada API...")
        val response = apiCall()
        Log.d(tag, "📡 Response recibida - Código: ${response.code()}")

        if (response.isSuccessful) {
            Log.d(tag, "✅ Llamada API exitosa")
            SafeApiResult.Success(response)
        } else {
            val errorMessage = response.errorBody()?.string() ?: "Error desconocido"
            Log.e(tag, "❌ Error API: ${response.code()} - $errorMessage")
            SafeApiResult.Failure("Error ${response.code()}: $errorMessage")
        }
    } catch (e: java.net.ConnectException) {
        val message = "Sin conexión a internet"
        Log.e(tag, "❌ ConnectException: ${e.message}")
        SafeApiResult.Failure(message)
    } catch (e: java.net.SocketTimeoutException) {
        val message = "Tiempo de espera agotado"
        Log.e(tag, "❌ TimeoutException: ${e.message}")
        SafeApiResult.Failure(message)
    } catch (e: java.net.UnknownHostException) {
        val message = "Servidor no encontrado"
        Log.e(tag, "❌ UnknownHostException: ${e.message}")
        SafeApiResult.Failure(message)
    } catch (e: Exception) {
        val message = "Error inesperado: ${e.localizedMessage ?: e.message ?: "Error desconocido"}"
        Log.e(tag, "❌ Exception general: ${e.message}", e)
        SafeApiResult.Failure(message)
    }
}

/**
 * Resultado de API seguro (nombre único para evitar conflictos)
 */
sealed class SafeApiResult<T> {
    data class Success<T>(val response: Response<T>) : SafeApiResult<T>()
    data class Failure<T>(val message: String) : SafeApiResult<T>()
}

/**
 * Extensión para manejar éxito
 */
fun <T> SafeApiResult<T>.onSuccess(action: (Response<T>) -> Unit): SafeApiResult<T> {
    if (this is SafeApiResult.Success) {
        action(response)
    }
    return this
}

/**
 * Extensión para manejar errores
 */
fun <T> SafeApiResult<T>.onError(action: (String) -> Unit): SafeApiResult<T> {
    if (this is SafeApiResult.Failure) {
        action(message)
    }
    return this
}