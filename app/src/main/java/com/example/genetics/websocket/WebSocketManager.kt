package com.example.genetics.websocket

import android.util.Log
import com.example.genetics.api.RetrofitClient
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manager para manejar conexiones WebSocket en la aplicación Android
 */
object WebSocketManager {

    private var notificationsListener: NotificationWebSocketListener? = null
    private var animalsListener: AnimalWebSocketListener? = null
    private var logsListener: LogWebSocketListener? = null

    // Callbacks para diferentes tipos de eventos
    private var onNotificationReceived: ((JSONObject) -> Unit)? = null
    private var onAnimalUpdate: ((JSONObject) -> Unit)? = null
    private var onLogReceived: ((JSONObject) -> Unit)? = null
    private var onConnectionStatusChanged: ((String, Boolean) -> Unit)? = null

    /**
     * Conectar WebSocket de notificaciones
     */
    fun connectNotifications(
        onNotification: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit = {}
    ) {
        onNotificationReceived = onNotification

        notificationsListener = NotificationWebSocketListener(
            onMessage = onNotification,
            onStatusChange = onStatusChange
        )

        RetrofitClient.connectNotificationsWebSocket(notificationsListener!!)
        Log.d("WEBSOCKET_MANAGER", "Conectando WebSocket de notificaciones")
    }

    /**
     * Conectar WebSocket de animales
     */
    fun connectAnimals(
        onUpdate: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit = {}
    ) {
        onAnimalUpdate = onUpdate

        animalsListener = AnimalWebSocketListener(
            onMessage = onUpdate,
            onStatusChange = onStatusChange
        )

        RetrofitClient.connectAnimalsWebSocket(animalsListener!!)
        Log.d("WEBSOCKET_MANAGER", "Conectando WebSocket de animales")
    }

    /**
     * Conectar WebSocket de logs (solo para admins)
     */
    fun connectLogs(
        onLog: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit = {}
    ) {
        onLogReceived = onLog

        logsListener = LogWebSocketListener(
            onMessage = onLog,
            onStatusChange = onStatusChange
        )

        RetrofitClient.connectLogsWebSocket(logsListener!!)
        Log.d("WEBSOCKET_MANAGER", "Conectando WebSocket de logs")
    }

    /**
     * Desconectar WebSocket de notificaciones
     */
    fun disconnectNotifications() {
        RetrofitClient.disconnectNotificationsWebSocket()
        notificationsListener = null
        onNotificationReceived = null
        Log.d("WEBSOCKET_MANAGER", "WebSocket de notificaciones desconectado")
    }

    /**
     * Desconectar WebSocket de animales
     */
    fun disconnectAnimals() {
        RetrofitClient.disconnectAnimalsWebSocket()
        animalsListener = null
        onAnimalUpdate = null
        Log.d("WEBSOCKET_MANAGER", "WebSocket de animales desconectado")
    }

    /**
     * Desconectar WebSocket de logs
     */
    fun disconnectLogs() {
        RetrofitClient.disconnectLogsWebSocket()
        logsListener = null
        onLogReceived = null
        Log.d("WEBSOCKET_MANAGER", "WebSocket de logs desconectado")
    }

    /**
     * Desconectar todos los WebSockets
     */
    fun disconnectAll() {
        disconnectNotifications()
        disconnectAnimals()
        disconnectLogs()
        Log.d("WEBSOCKET_MANAGER", "Todos los WebSockets desconectados")
    }

    /**
     * Enviar mensaje al WebSocket de notificaciones
     */
    fun sendNotificationMessage(type: String, data: Map<String, Any> = emptyMap()): Boolean {
        val message = JSONObject().apply {
            put("type", type)
            data.forEach { (key, value) ->
                put(key, value)
            }
        }
        return RetrofitClient.sendNotificationMessage(message.toString())
    }

    /**
     * Enviar mensaje al WebSocket de animales
     */
    fun sendAnimalsMessage(type: String, data: Map<String, Any> = emptyMap()): Boolean {
        val message = JSONObject().apply {
            put("type", type)
            data.forEach { (key, value) ->
                put(key, value)
            }
        }
        return RetrofitClient.sendAnimalsMessage(message.toString())
    }

    /**
     * Enviar mensaje al WebSocket de logs
     */
    fun sendLogsMessage(type: String, data: Map<String, Any> = emptyMap()): Boolean {
        val message = JSONObject().apply {
            put("type", type)
            data.forEach { (key, value) ->
                put(key, value)
            }
        }
        return RetrofitClient.sendLogsMessage(message.toString())
    }

    /**
     * Marcar notificación como leída
     */
    fun markNotificationAsRead(notificationId: Int): Boolean {
        return sendNotificationMessage("mark_as_read", mapOf("notification_id" to notificationId))
    }

    /**
     * Suscribirse a actualizaciones de un animal específico
     */
    fun subscribeToAnimal(animalId: Int): Boolean {
        return sendAnimalsMessage("subscribe_animal", mapOf("animal_id" to animalId))
    }

    /**
     * Desuscribirse de actualizaciones de un animal específico
     */
    fun unsubscribeFromAnimal(animalId: Int): Boolean {
        return sendAnimalsMessage("unsubscribe_animal", mapOf("animal_id" to animalId))
    }

    /**
     * Solicitar logs recientes
     */
    fun requestRecentLogs(limit: Int = 50): Boolean {
        return sendLogsMessage("get_recent_logs", mapOf("limit" to limit))
    }

    /**
     * Verificar estado de conexiones
     */
    fun getConnectionStatus(): Map<String, Boolean> {
        return mapOf(
            "notifications" to RetrofitClient.isNotificationsWebSocketConnected(),
            "animals" to RetrofitClient.isAnimalsWebSocketConnected(),
            "logs" to RetrofitClient.isLogsWebSocketConnected()
        )
    }
}

/**
 * WebSocketListener para notificaciones
 */
class NotificationWebSocketListener(
    private val onMessage: (JSONObject) -> Unit,
    private val onStatusChange: (Boolean) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("NOTIFICATIONS_WS", "Conexión establecida")
        onStatusChange(true)

        // Solicitar contador de notificaciones no leídas
        webSocket.send("""{"type": "get_unread_count"}""")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("NOTIFICATIONS_WS", "Mensaje recibido: $text")
        try {
            val json = JSONObject(text)
            onMessage(json)
        } catch (e: Exception) {
            Log.e("NOTIFICATIONS_WS", "Error parseando mensaje: ${e.message}")
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("NOTIFICATIONS_WS", "Error de conexión: ${t.message}")
        onStatusChange(false)

        // Intentar reconectar después de 5 segundos
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            Log.d("NOTIFICATIONS_WS", "Intentando reconectar...")
            // Aquí podrías implementar lógica de reconexión automática
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("NOTIFICATIONS_WS", "Conexión cerrada: $code - $reason")
        onStatusChange(false)
    }
}

/**
 * WebSocketListener para animales
 */
class AnimalWebSocketListener(
    private val onMessage: (JSONObject) -> Unit,
    private val onStatusChange: (Boolean) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("ANIMALS_WS", "Conexión establecida")
        onStatusChange(true)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("ANIMALS_WS", "Mensaje recibido: $text")
        try {
            val json = JSONObject(text)
            onMessage(json)
        } catch (e: Exception) {
            Log.e("ANIMALS_WS", "Error parseando mensaje: ${e.message}")
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("ANIMALS_WS", "Error de conexión: ${t.message}")
        onStatusChange(false)

        // Intentar reconectar después de 5 segundos
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            Log.d("ANIMALS_WS", "Intentando reconectar...")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("ANIMALS_WS", "Conexión cerrada: $code - $reason")
        onStatusChange(false)
    }
}

/**
 * WebSocketListener para logs
 */
class LogWebSocketListener(
    private val onMessage: (JSONObject) -> Unit,
    private val onStatusChange: (Boolean) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("LOGS_WS", "Conexión establecida")
        onStatusChange(true)

        // Solicitar logs recientes al conectar
        webSocket.send("""{"type": "get_recent_logs", "limit": 50}""")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("LOGS_WS", "Mensaje recibido: $text")
        try {
            val json = JSONObject(text)
            onMessage(json)
        } catch (e: Exception) {
            Log.e("LOGS_WS", "Error parseando mensaje: ${e.message}")
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("LOGS_WS", "Error de conexión: ${t.message}")
        onStatusChange(false)

        // Intentar reconectar después de 5 segundos
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            Log.d("LOGS_WS", "Intentando reconectar...")
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.d("LOGS_WS", "Conexión cerrada: $code - $reason")
        onStatusChange(false)
    }
}