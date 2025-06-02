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
        Log.d(tag, "Operación cancelada (normal al cambiar de pantalla)")
        ApiResult.Cancelled
    } catch (e: ConnectException) {
        Log.e(tag, "Error de conexión: ${e.message}")
        ApiResult.Error("No se puede conectar al servidor. Verifica tu conexión a internet.")
    } catch (e: SocketTimeoutException) {
        Log.e(tag, "Timeout: ${e.message}")
        ApiResult.Error("La conexión tardó demasiado. Inténtalo de nuevo.")
    } catch (e: UnknownHostException) {
        Log.e(tag, "Host desconocido: ${e.message}")
        ApiResult.Error("No se puede encontrar el servidor. Verifica tu conexión.")
    } catch (e: IOException) {
        Log.e(tag, "Error de E/S: ${e.message}")
        ApiResult.Error("Error de red. Verifica tu conexión e inténtalo de nuevo.")
    } catch (e: Exception) {
        Log.e(tag, "Error inesperado: ${e.message}")
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

/**
 * Clase de utilidades para WebSocket
 */
object WebSocketUtils {

    /**
     * Verificar si una URL de WebSocket es válida
     */
    fun isValidWebSocketUrl(url: String): Boolean {
        return url.startsWith("ws://") || url.startsWith("wss://")
    }

    /**
     * Construir URL de WebSocket con parámetros
     */
    fun buildWebSocketUrl(baseUrl: String, endpoint: String, params: Map<String, String> = emptyMap()): String {
        val cleanBaseUrl = baseUrl.removeSuffix("/")
        val cleanEndpoint = endpoint.removePrefix("/").removeSuffix("/")

        var url = "$cleanBaseUrl/$cleanEndpoint/"

        if (params.isNotEmpty()) {
            val queryParams = params.map { "${it.key}=${it.value}" }.joinToString("&")
            url += "?$queryParams"
        }

        return url
    }

    /**
     * Parsear mensaje JSON de WebSocket de forma segura
     */
    fun parseWebSocketMessage(message: String): org.json.JSONObject? {
        return try {
            org.json.JSONObject(message)
        } catch (e: Exception) {
            Log.e("WEBSOCKET_UTILS", "Error parseando mensaje WebSocket: ${e.message}")
            null
        }
    }

    /**
     * Crear mensaje JSON para WebSocket
     */
    fun createWebSocketMessage(type: String, data: Map<String, Any> = emptyMap()): String {
        return try {
            val json = org.json.JSONObject()
            json.put("type", type)
            data.forEach { (key, value) ->
                json.put(key, value)
            }
            json.toString()
        } catch (e: Exception) {
            Log.e("WEBSOCKET_UTILS", "Error creando mensaje WebSocket: ${e.message}")
            """{"type": "error", "message": "Error creando mensaje"}"""
        }
    }

    /**
     * Obtener el tipo de mensaje de un JSON de WebSocket
     */
    fun getMessageType(jsonObject: org.json.JSONObject): String? {
        return try {
            jsonObject.getString("type")
        } catch (e: Exception) {
            Log.e("WEBSOCKET_UTILS", "Error obteniendo tipo de mensaje: ${e.message}")
            null
        }
    }

    /**
     * Obtener datos de un mensaje de WebSocket
     */
    fun getMessageData(jsonObject: org.json.JSONObject): org.json.JSONObject? {
        return try {
            if (jsonObject.has("data")) {
                jsonObject.getJSONObject("data")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("WEBSOCKET_UTILS", "Error obteniendo datos de mensaje: ${e.message}")
            null
        }
    }
}

/**
 * Extensiones para manejo de errores de red
 */
object NetworkErrorUtils {

    /**
     * Determinar si un error es recuperable
     */
    fun isRecoverableError(throwable: Throwable): Boolean {
        return when (throwable) {
            is SocketTimeoutException,
            is ConnectException,
            is IOException -> true
            else -> false
        }
    }

    /**
     * Obtener mensaje de error amigable para el usuario
     */
    fun getUserFriendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is ConnectException -> "No se puede conectar al servidor. Verifica tu conexión a internet."
            is SocketTimeoutException -> "La conexión tardó demasiado. Inténtalo de nuevo."
            is UnknownHostException -> "No se puede encontrar el servidor. Verifica tu conexión."
            is IOException -> "Error de red. Verifica tu conexión e inténtalo de nuevo."
            else -> "Error inesperado: ${throwable.localizedMessage ?: "Error desconocido"}"
        }
    }

    /**
     * Determinar si se debe reintentar automáticamente
     */
    fun shouldRetryAutomatically(throwable: Throwable, retryCount: Int, maxRetries: Int = 3): Boolean {
        if (retryCount >= maxRetries) return false

        return when (throwable) {
            is SocketTimeoutException,
            is ConnectException -> true
            is IOException -> {
                // Solo reintentar para algunos tipos de IOException
                throwable.message?.contains("Network", ignoreCase = true) == true
            }
            else -> false
        }
    }
}

/**
 * Utilidades para validación de datos
 */
object ValidationUtils {

    /**
     * Validar email
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validar URL
     */
    fun isValidUrl(url: String): Boolean {
        return android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    /**
     * Validar que un string no esté vacío
     */
    fun isNotEmpty(value: String?): Boolean {
        return !value.isNullOrBlank()
    }

    /**
     * Validar longitud mínima
     */
    fun hasMinLength(value: String?, minLength: Int): Boolean {
        return value != null && value.length >= minLength
    }

    /**
     * Validar que sea un número positivo
     */
    fun isPositiveNumber(value: String?): Boolean {
        return try {
            val number = value?.toDoubleOrNull()
            number != null && number > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validar formato de fecha (yyyy-MM-dd)
     */
    fun isValidDateFormat(date: String?): Boolean {
        if (date.isNullOrBlank()) return false

        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            dateFormat.isLenient = false
            dateFormat.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Utilidades para formateo de datos
 */
object FormatUtils {

    /**
     * Formatear fecha para mostrar
     */
    fun formatDateForDisplay(date: String?): String {
        if (date.isNullOrBlank()) return "No especificada"

        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate ?: java.util.Date())
        } catch (e: Exception) {
            date
        }
    }

    /**
     * Formatear fecha y hora para mostrar
     */
    fun formatDateTimeForDisplay(dateTime: String?): String {
        if (dateTime.isNullOrBlank()) return "No especificada"

        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val parsedDate = inputFormat.parse(dateTime)
            outputFormat.format(parsedDate ?: java.util.Date())
        } catch (e: Exception) {
            // Intentar solo con fecha
            try {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val parsedDate = inputFormat.parse(dateTime.substring(0, 10))
                outputFormat.format(parsedDate ?: java.util.Date())
            } catch (e2: Exception) {
                dateTime
            }
        }
    }

    /**
     * Formatear fecha para API (yyyy-MM-dd)
     */
    fun formatDateForApi(displayDate: String?): String {
        if (displayDate.isNullOrBlank()) return ""

        return try {
            val inputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val parsedDate = inputFormat.parse(displayDate)
            outputFormat.format(parsedDate ?: java.util.Date())
        } catch (e: Exception) {
            displayDate
        }
    }

    /**
     * Formatear sexo del animal
     */
    fun formatAnimalSex(sex: String?): String {
        return when (sex?.lowercase()) {
            "macho" -> "♂️ Macho"
            "hembra" -> "♀️ Hembra"
            else -> "❓ No especificado"
        }
    }

    /**
     * Formatear estado de incidencia
     */
    fun formatIncidentStatus(status: String?): String {
        return when (status?.lowercase()) {
            "pendiente" -> "⏳ Pendiente"
            "en tratamiento" -> "🔄 En Tratamiento"
            "resuelto" -> "✅ Resuelto"
            else -> status?.replaceFirstChar { it.uppercase() } ?: "Sin estado"
        }
    }

    /**
     * Formatear rol de usuario
     */
    fun formatUserRole(role: String?): String {
        return when (role?.lowercase()) {
            "admin" -> "👑 Administrador"
            "usuario" -> "👤 Usuario"
            "dueño" -> "🏠 Dueño"
            else -> "❓ Sin definir"
        }
    }

    /**
     * Capitalizar primera letra
     */
    fun capitalizeFirst(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault())
            else it.toString()
        }
    }

    /**
     * Formatear peso con unidad
     */
    fun formatWeight(weight: Double?): String {
        return if (weight != null) "$weight kg" else "No registrado"
    }

    /**
     * Formatear duración relativa (hace X tiempo)
     */
    fun formatRelativeTime(dateTime: String?): String {
        if (dateTime.isNullOrBlank()) return "Nunca"

        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            val now = java.util.Date()
            val diffInMs = now.time - (date?.time ?: 0)
            val diffInDays = diffInMs / (1000 * 60 * 60 * 24)

            when {
                diffInDays == 0L -> "Hoy"
                diffInDays == 1L -> "Ayer"
                diffInDays < 7 -> "Hace $diffInDays días"
                diffInDays < 30 -> "Hace ${diffInDays / 7} semanas"
                diffInDays < 365 -> "Hace ${diffInDays / 30} meses"
                else -> "Hace ${diffInDays / 365} años"
            }
        } catch (e: Exception) {
            "Desconocido"
        }
    }
}

/**
 * Utilidades para logging
 */
object LogUtils {

    private const val MAX_LOG_LENGTH = 4000

    /**
     * Log largo que puede exceder el límite de Android
     */
    fun longLog(tag: String, message: String) {
        if (message.length <= MAX_LOG_LENGTH) {
            Log.d(tag, message)
        } else {
            var index = 0
            while (index < message.length) {
                val endIndex = kotlin.math.min(index + MAX_LOG_LENGTH, message.length)
                Log.d(tag, message.substring(index, endIndex))
                index = endIndex
            }
        }
    }

    /**
     * Log de respuesta HTTP
     */
    fun logHttpResponse(tag: String, method: String, url: String, code: Int, body: String?) {
        val message = buildString {
            appendLine("=== HTTP RESPONSE ===")
            appendLine("Method: $method")
            appendLine("URL: $url")
            appendLine("Code: $code")
            if (!body.isNullOrBlank()) {
                appendLine("Body: $body")
            }
            appendLine("===================")
        }
        longLog(tag, message)
    }

    /**
     * Log de WebSocket
     */
    fun logWebSocket(tag: String, type: String, message: String) {
        val logMessage = buildString {
            appendLine("=== WEBSOCKET $type ===")
            appendLine("Message: $message")
            appendLine("========================")
        }
        Log.d(tag, logMessage)
    }
}

/**
 * Utilidades para UI
 */
object UiUtils {

    /**
     * Obtener color de recurso de forma segura
     */
    fun getColorSafe(activity: AppCompatActivity, colorRes: Int): Int {
        return try {
            activity.resources.getColor(colorRes, activity.theme)
        } catch (e: Exception) {
            android.graphics.Color.GRAY
        }
    }

    /**
     * Mostrar toast de forma segura
     */
    fun showToastSafe(activity: AppCompatActivity, message: String, isLong: Boolean = false) {
        try {
            val duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            Toast.makeText(activity, message, duration).show()
        } catch (e: Exception) {
            Log.e("UI_UTILS", "Error mostrando toast: ${e.message}")
        }
    }

    /**
     * Ocultar/mostrar vista de forma segura
     */
    fun setViewVisibility(view: android.view.View?, isVisible: Boolean) {
        try {
            view?.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
        } catch (e: Exception) {
            Log.e("UI_UTILS", "Error cambiando visibilidad: ${e.message}")
        }
    }

    /**
     * Configurar texto de forma segura
     */
    fun setTextSafe(textView: android.widget.TextView?, text: String?) {
        try {
            textView?.text = text ?: ""
        } catch (e: Exception) {
            Log.e("UI_UTILS", "Error configurando texto: ${e.message}")
        }
    }
}

/**
 * Extensiones para Activity
 */
fun AppCompatActivity.showLoadingDialog(message: String = "Cargando..."): androidx.appcompat.app.AlertDialog {
    return androidx.appcompat.app.AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(false)
        .create().apply { show() }
}

fun AppCompatActivity.hideKeyboard() {
    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
    val view = currentFocus ?: android.view.View(this)
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

/**
 * Extensiones para String
 */
fun String?.isValidEmail(): Boolean = ValidationUtils.isValidEmail(this ?: "")
fun String?.isValidUrl(): Boolean = ValidationUtils.isValidUrl(this ?: "")
fun String?.formatAsDisplayDate(): String = FormatUtils.formatDateForDisplay(this)
fun String?.formatAsApiDate(): String = FormatUtils.formatDateForApi(this)
fun String?.capitalizeFirstLetter(): String = FormatUtils.capitalizeFirst(this)

/**
 * Extensiones para Double
 */
fun Double?.formatAsWeight(): String = FormatUtils.formatWeight(this)