package com.example.genetics.websocket

import android.util.Log
import com.example.genetics.api.RetrofitClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

object WebSocketManager {

    private var notificationsListener: ((JSONObject) -> Unit)? = null
    private var animalsListener: ((JSONObject) -> Unit)? = null
    private var logsListener: ((JSONObject) -> Unit)? = null

    private var notificationsStatusListener: ((Boolean) -> Unit)? = null
    private var animalsStatusListener: ((Boolean) -> Unit)? = null
    private var logsStatusListener: ((Boolean) -> Unit)? = null

    /**
     * Conectar WebSocket de notificaciones
     */
    fun connectNotifications(
        onNotification: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        notificationsListener = onNotification
        notificationsStatusListener = onStatusChange

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WEBSOCKET_MANAGER", "✅ Notificaciones WebSocket conectado")
                onStatusChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObject = JSONObject(text)
                    Log.d("WEBSOCKET_MANAGER", "📢 Notificación recibida: $text")
                    onNotification(jsonObject)
                } catch (e: Exception) {
                    Log.e("WEBSOCKET_MANAGER", "❌ Error parseando mensaje de notificación: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WEBSOCKET_MANAGER", "⚠️ Notificaciones WebSocket cerrando: $code - $reason")
                onStatusChange(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WEBSOCKET_MANAGER", "❌ Notificaciones WebSocket cerrado: $code - $reason")
                onStatusChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WEBSOCKET_MANAGER", "❌ Error en notificaciones WebSocket: ${t.message}")
                onStatusChange(false)
            }
        }

        RetrofitClient.connectNotificationsWebSocket(listener)
    }

    /**
     * Conectar WebSocket de animales
     */
    fun connectAnimals(
        onUpdate: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        animalsListener = onUpdate
        animalsStatusListener = onStatusChange

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WEBSOCKET_MANAGER", "✅ Animales WebSocket conectado")
                onStatusChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObject = JSONObject(text)
                    Log.d("WEBSOCKET_MANAGER", "🐄 Actualización de animal recibida: $text")
                    onUpdate(jsonObject)
                } catch (e: Exception) {
                    Log.e("WEBSOCKET_MANAGER", "❌ Error parseando mensaje de animal: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WEBSOCKET_MANAGER", "⚠️ Animales WebSocket cerrando: $code - $reason")
                onStatusChange(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WEBSOCKET_MANAGER", "❌ Animales WebSocket cerrado: $code - $reason")
                onStatusChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WEBSOCKET_MANAGER", "❌ Error en animales WebSocket: ${t.message}")
                onStatusChange(false)
            }
        }

        RetrofitClient.connectAnimalsWebSocket(listener)
    }

    /**
     * Conectar WebSocket de logs (solo para admins)
     */
    fun connectLogs(
        onLog: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        logsListener = onLog
        logsStatusListener = onStatusChange

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WEBSOCKET_MANAGER", "✅ Logs WebSocket conectado")
                onStatusChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObject = JSONObject(text)
                    Log.d("WEBSOCKET_MANAGER", "📋 Log recibido: $text")
                    onLog(jsonObject)
                } catch (e: Exception) {
                    Log.e("WEBSOCKET_MANAGER", "❌ Error parseando mensaje de log: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WEBSOCKET_MANAGER", "⚠️ Logs WebSocket cerrando: $code - $reason")
                onStatusChange(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w("WEBSOCKET_MANAGER", "❌ Logs WebSocket cerrado: $code - $reason")
                onStatusChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WEBSOCKET_MANAGER", "❌ Error en logs WebSocket: ${t.message}")
                onStatusChange(false)
            }
        }

        RetrofitClient.connectLogsWebSocket(listener)
    }

    /**
     * Desconectar todos los WebSockets
     */
    fun disconnectAll() {
        Log.d("WEBSOCKET_MANAGER", "🔌 Desconectando todos los WebSockets")
        RetrofitClient.disconnectAllWebSockets()

        // Limpiar listeners
        notificationsListener = null
        animalsListener = null
        logsListener = null
        notificationsStatusListener = null
        animalsStatusListener = null
        logsStatusListener = null
    }

    /**
     * Desconectar WebSocket específico
     */
    fun disconnectNotifications() {
        RetrofitClient.disconnectNotificationsWebSocket()
        notificationsListener = null
        notificationsStatusListener = null
    }

    fun disconnectAnimals() {
        RetrofitClient.disconnectAnimalsWebSocket()
        animalsListener = null
        animalsStatusListener = null
    }

    fun disconnectLogs() {
        RetrofitClient.disconnectLogsWebSocket()
        logsListener = null
        logsStatusListener = null
    }

    /**
     * Enviar mensajes por WebSocket
     */
    fun sendNotificationMessage(message: String): Boolean {
        return try {
            val jsonMessage = if (message.startsWith("{")) {
                message
            } else {
                """{"type": "$message"}"""
            }
            RetrofitClient.sendNotificationMessage(jsonMessage)
        } catch (e: Exception) {
            Log.e("WEBSOCKET_MANAGER", "❌ Error enviando mensaje de notificación: ${e.message}")
            false
        }
    }

    fun sendAnimalsMessage(message: String): Boolean {
        return try {
            val jsonMessage = if (message.startsWith("{")) {
                message
            } else {
                """{"type": "$message"}"""
            }
            RetrofitClient.sendAnimalsMessage(jsonMessage)
        } catch (e: Exception) {
            Log.e("WEBSOCKET_MANAGER", "❌ Error enviando mensaje de animales: ${e.message}")
            false
        }
    }

    fun sendLogsMessage(message: String): Boolean {
        return try {
            val jsonMessage = if (message.startsWith("{")) {
                message
            } else {
                """{"type": "$message"}"""
            }
            RetrofitClient.sendLogsMessage(jsonMessage)
        } catch (e: Exception) {
            Log.e("WEBSOCKET_MANAGER", "❌ Error enviando mensaje de logs: ${e.message}")
            false
        }
    }

    /**
     * Obtener estado de conexiones
     */
    fun getConnectionStatus(): Map<String, Boolean> {
        return mapOf(
            "notifications" to RetrofitClient.isNotificationsWebSocketConnected(),
            "animals" to RetrofitClient.isAnimalsWebSocketConnected(),
            "logs" to RetrofitClient.isLogsWebSocketConnected()
        )
    }

    /**
     * Verificar si al menos una conexión está activa
     */
    fun isAnyConnected(): Boolean {
        val status = getConnectionStatus()
        return status.values.any { it }
    }

    /**
     * Verificar si todas las conexiones están activas
     */
    fun areAllConnected(): Boolean {
        val status = getConnectionStatus()
        return status.values.all { it }
    }
}