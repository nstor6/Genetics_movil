package com.example.genetics.websocket

import android.util.Log
import com.example.genetics.api.RetrofitClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

object WebSocketManager {

    private var notificationsListener: WebSocketListener? = null
    private var animalsListener: WebSocketListener? = null
    private var logsListener: WebSocketListener? = null

    /**
     * Conectar WebSocket de notificaciones
     */
    fun connectNotifications(
        onNotification: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        notificationsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS_NOTIFICATIONS", "🔔 WebSocket abierto")
                onStatusChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WS_NOTIFICATIONS", "📨 Mensaje recibido: $text")
                    val json = JSONObject(text)
                    onNotification(json)
                } catch (e: Exception) {
                    Log.e("WS_NOTIFICATIONS", "❌ Error parseando mensaje: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS_NOTIFICATIONS", "🔔 WebSocket cerrando: $code - $reason")
                onStatusChange(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS_NOTIFICATIONS", "🔔 WebSocket cerrado: $code - $reason")
                onStatusChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS_NOTIFICATIONS", "❌ Error en WebSocket: ${t.message}")
                onStatusChange(false)
            }
        }

        RetrofitClient.connectNotificationsWebSocket(notificationsListener!!)
    }

    /**
     * Conectar WebSocket de animales
     */
    fun connectAnimals(
        onUpdate: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        animalsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS_ANIMALS", "🐄 WebSocket abierto")
                onStatusChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WS_ANIMALS", "📨 Mensaje recibido: $text")
                    val json = JSONObject(text)
                    onUpdate(json)
                } catch (e: Exception) {
                    Log.e("WS_ANIMALS", "❌ Error parseando mensaje: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS_ANIMALS", "🐄 WebSocket cerrando: $code - $reason")
                onStatusChange(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS_ANIMALS", "🐄 WebSocket cerrado: $code - $reason")
                onStatusChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS_ANIMALS", "❌ Error en WebSocket: ${t.message}")
                onStatusChange(false)
            }
        }

        RetrofitClient.connectAnimalsWebSocket(animalsListener!!)
    }

    /**
     * Conectar WebSocket de logs (solo admins)
     */
    fun connectLogs(
        onLog: (JSONObject) -> Unit,
        onStatusChange: (Boolean) -> Unit
    ) {
        logsListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS_LOGS", "📋 WebSocket abierto")
                onStatusChange(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WS_LOGS", "📨 Mensaje recibido: $text")
                    val json = JSONObject(text)
                    onLog(json)
                } catch (e: Exception) {
                    Log.e("WS_LOGS", "❌ Error parseando mensaje: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS_LOGS", "📋 WebSocket cerrando: $code - $reason")
                onStatusChange(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WS_LOGS", "📋 WebSocket cerrado: $code - $reason")
                onStatusChange(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS_LOGS", "❌ Error en WebSocket: ${t.message}")
                onStatusChange(false)
            }
        }

        RetrofitClient.connectLogsWebSocket(logsListener!!)
    }

    /**
     * Enviar mensaje a WebSocket de notificaciones
     */
    fun sendNotificationMessage(type: String, data: Map<String, Any>? = null): Boolean {
        val message = JSONObject().apply {
            put("type", type)
            data?.forEach { (key, value) -> put(key, value) }
        }
        return RetrofitClient.sendNotificationMessage(message.toString())
    }

    /**
     * Desconectar todos los WebSockets
     */
    fun disconnectAll() {
        RetrofitClient.disconnectNotificationsWebSocket()
        RetrofitClient.disconnectAnimalsWebSocket()
        RetrofitClient.disconnectLogsWebSocket()

        notificationsListener = null
        animalsListener = null
        logsListener = null

        Log.d("WEBSOCKET_MANAGER", "🔌 Todos los WebSockets desconectados")
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
}