package com.example.genetics.utils

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CancellationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

/**
 * Función de extensión para manejar llamadas API de forma segura
 * Evita crashes por "Job was cancelled" y otros errores de red
 */
suspend fun <T> safeApiCall(
    tag: String = "API_CALL",
    apiCall: suspend () -> T
): ApiResult<T> {
    return try {
        val result = apiCall()
        ApiResult.Success(result)
    } catch (e: CancellationException) {
        Log.d(tag, "🔄 Operación cancelada (normal al cambiar de pantalla)")
        ApiResult.Cancelled
    } catch (e: ConnectException) {
        Log.e(tag, "❌ Error de conexión: ${e.message}")
        ApiResult.Error("No se puede conectar al servidor. Verifica tu conexión a internet.")
    } catch (e: SocketTimeoutException) {
        Log.e(tag, "⏱️ Timeout: ${e.message}")
        ApiResult.Error("La conexión tardó demasiado. Inténtalo de nuevo.")
    } catch (e: UnknownHostException) {
        Log.e(tag, "🌐 Host desconocido: ${e.message}")
        ApiResult.Error("No se puede encontrar el servidor. Verifica tu conexión.")
    } catch (e: IOException) {
        Log.e(tag, "📡 Error de E/S: ${e.message}")
        ApiResult.Error("Error de red. Verifica tu conexión e inténtalo de nuevo.")
    } catch (e: Exception) {
        Log.e(tag, "❌ Error inesperado: ${e.message}")
        ApiResult.Error("Error inesperado: ${e.localizedMessage ?: e.message ?: "Error desconocido"}")
    }
}

/**
 * Clase sellada para representar los posibles resultados de una llamada API
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Cancelled : ApiResult<Nothing>()
}

/**
 * Función de extensión para manejar fácilmente los resultados de API
 * en Activities
 */
fun <T> ApiResult<T>.handleResult(
    activity: AppCompatActivity,
    onSuccess: (T) -> Unit,
    onError: (String) -> Unit = { message ->
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    },
    onCancelled: () -> Unit = {
        Log.d("API_RESULT", "Operación cancelada")
    }
) {
    when (this) {
        is ApiResult.Success -> onSuccess(this.data)
        is ApiResult.Error -> onError(this.message)
        is ApiResult.Cancelled -> onCancelled()
    }
}

/**
 * Función de extensión más simple para casos donde solo necesitas el éxito
 */
fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(this.data)
    }
    return this
}

/**
 * Función de extensión para manejar errores
 */
fun <T> ApiResult<T>.onError(action: (String) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) {
        action(this.message)
    }
    return this
}

/**
 * Función de extensión para manejar cancelaciones
 */
fun <T> ApiResult<T>.onCancelled(action: () -> Unit): ApiResult<T> {
    if (this is ApiResult.Cancelled) {
        action()
    }
    return this
}