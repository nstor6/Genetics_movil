package com.example.genetics.Activitys

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.CalendarActivity
import com.example.genetics.Activitys.IncidentsActivity
import com.example.genetics.Activitys.TreatmentsActivity
import com.example.genetics.Activitys.UserAnimalsActivity
import com.example.genetics.Create.AddEventActivity
import com.example.genetics.Create.AddIncidentActivity
import com.example.genetics.Create.AddTreatmentActivity
import com.example.genetics.LoginActivity
import com.example.genetics.R
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityUserDashboardBinding
import com.example.genetics.utils.*
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private val apiService = RetrofitClient.getApiService()

    // ✅ WebSocket para usuarios normales
    private var notificationsWebSocket: WebSocket? = null
    private var animalsWebSocket: WebSocket? = null
    private var isWebSocketConnected = false
    private val handler = Handler(Looper.getMainLooper())
    private var connectionRetryCount = 0
    private val maxRetryCount = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("USER_DASHBOARD", "🚀 Dashboard Usuario iniciado")
        setupUI()
        cargarDatosIniciales()

        // ✅ CONECTAR WEBSOCKETS PARA NOTIFICACIONES DE USUARIO
        connectWebSocketForUser()

        // Cargar información del usuario
        cargarInfoUsuario()
    }

    private fun setupUI() {
        Log.d("USER_DASHBOARD", "🔧 Configurando UI para usuario normal...")

        // 🔧 TARJETAS CLICKEABLES - Solo las que puede usar un usuario normal
        binding.cardAnimals.setOnClickListener {
            Log.d("USER_DASHBOARD", "🐄 Click en Animals - Abriendo versión de SOLO LECTURA")
            startActivity(Intent(this, UserAnimalsActivity::class.java))
        }

        binding.cardIncidents.setOnClickListener {
            Log.d("USER_DASHBOARD", "🚨 Click en Incidents")
            startActivity(Intent(this, IncidentsActivity::class.java))
        }

        binding.cardTreatments.setOnClickListener {
            Log.d("USER_DASHBOARD", "💊 Click en Treatments")
            startActivity(Intent(this, TreatmentsActivity::class.java))
        }

        binding.cardEvents.setOnClickListener {
            Log.d("USER_DASHBOARD", "📅 Click en Events")
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // ✅ CONFIGURAR NAVEGACIÓN BOTTOM PARA USUARIOS
        setupBottomNavigation()

        // 🔧 BOTONES DE ACCIONES RÁPIDAS - Solo las permitidas para usuarios
        // Verificar si existen antes de configurar listeners
        try {
            binding.buttonNewIncident?.setOnClickListener {
                Log.d("USER_DASHBOARD", "🆕 Click en buttonNewIncident")
                startActivity(Intent(this, AddIncidentActivity::class.java))
            }
        } catch (e: Exception) {
            Log.w("USER_DASHBOARD", "buttonNewIncident no encontrado en el layout")
        }

        try {
            binding.buttonNewTreatment?.setOnClickListener {
                Log.d("USER_DASHBOARD", "🆕 Click en buttonNewTreatment")
                startActivity(Intent(this, AddTreatmentActivity::class.java))
            }
        } catch (e: Exception) {
            Log.w("USER_DASHBOARD", "buttonNewTreatment no encontrado en el layout")
        }

        try {
            binding.buttonNewEvent?.setOnClickListener {
                Log.d("USER_DASHBOARD", "🆕 Click en buttonNewEvent")
                startActivity(Intent(this, AddEventActivity::class.java))
            }
        } catch (e: Exception) {
            Log.w("USER_DASHBOARD", "buttonNewEvent no encontrado en el layout")
        }

        // ✅ MOSTRAR ESTADO DE CONEXIÓN INICIAL
        updateConnectionStatus()
    }

    private fun setupBottomNavigation() {
        Log.d("USER_DASHBOARD", "🔧 Configurando Bottom Navigation para usuario...")

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            Log.d("USER_DASHBOARD", "📱 Bottom Nav item selected: ${item.itemId}")
            when (item.itemId) {
                R.id.nav_animals -> {
                    startActivity(Intent(this, UserAnimalsActivity::class.java))
                    true
                }
                R.id.nav_incidents -> {
                    startActivity(Intent(this, IncidentsActivity::class.java))
                    true
                }
                R.id.nav_treatments -> {
                    startActivity(Intent(this, TreatmentsActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendarActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    showUserMenu()
                    true
                }
                else -> false
            }
        }
    }

    // ===== WEBSOCKET IMPLEMENTATION =====

    private fun connectWebSocketForUser() {
        Log.d("USER_DASHBOARD", "🔌 Iniciando conexión WebSocket para usuario...")

        // Conectar WebSocket de notificaciones
        connectNotificationsWebSocket()

        // Conectar WebSocket de animales (solo para updates)
        connectAnimalsWebSocket()
    }

    private fun connectNotificationsWebSocket() {
        Log.d("USER_DASHBOARD", "🔔 Conectando WebSocket de notificaciones...")

        RetrofitClient.connectNotificationsWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("USER_DASHBOARD", "✅ WebSocket notificaciones conectado")

                runOnUiThread {
                    isWebSocketConnected = true
                    updateConnectionStatus()
                    connectionRetryCount = 0

                    // Vibración sutil de conexión
                    vibrarSutil()

                    UiUtils.showToastSafe(
                        this@UserDashboardActivity,
                        "📡 Conectado - Actualizaciones en tiempo real activadas",
                        false
                    )
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("USER_DASHBOARD", "📩 Mensaje WebSocket recibido: $text")

                WebSocketUtils.parseWebSocketMessage(text)?.let { json ->
                    runOnUiThread {
                        when (WebSocketUtils.getMessageType(json)) {
                            "notification", "pending_notification" -> {
                                handleNotificationMessage(json)
                            }
                            "broadcast" -> {
                                handleBroadcastMessage(json)
                            }
                            "unread_count" -> {
                                handleUnreadCount(json)
                            }
                            "connection_established" -> {
                                Log.d("USER_DASHBOARD", "🔗 Conexión WebSocket establecida")
                            }
                            else -> {
                                Log.d("USER_DASHBOARD", "📨 Mensaje WebSocket no reconocido")
                            }
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("USER_DASHBOARD", "🔌 WebSocket notificaciones cerrándose: $code - $reason")

                runOnUiThread {
                    isWebSocketConnected = false
                    updateConnectionStatus()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("USER_DASHBOARD", "❌ Error WebSocket notificaciones: ${t.message}")

                runOnUiThread {
                    isWebSocketConnected = false
                    updateConnectionStatus()

                    // Intentar reconectar si es un error recuperable
                    if (NetworkErrorUtils.isRecoverableError(t)) {
                        attemptReconnection()
                    }
                }
            }
        })
    }

    private fun connectAnimalsWebSocket() {
        Log.d("USER_DASHBOARD", "🐄 Conectando WebSocket de animales...")

        RetrofitClient.connectAnimalsWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("USER_DASHBOARD", "✅ WebSocket animales conectado")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("USER_DASHBOARD", "🐄 Mensaje WebSocket animales: $text")

                WebSocketUtils.parseWebSocketMessage(text)?.let { json ->
                    runOnUiThread {
                        when (WebSocketUtils.getMessageType(json)) {
                            "animal_created", "animal_updated", "animal_deleted" -> {
                                handleAnimalUpdate(json)
                            }
                            else -> {
                                Log.d("USER_DASHBOARD", "📨 Mensaje animal no reconocido")
                            }
                        }
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("USER_DASHBOARD", "🔌 WebSocket animales cerrándose: $code - $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("USER_DASHBOARD", "❌ Error WebSocket animales: ${t.message}")
            }
        })
    }

    // ===== MESSAGE HANDLERS =====

    private fun handleNotificationMessage(json: JSONObject) {
        try {
            val data = WebSocketUtils.getMessageData(json)
            if (data != null) {
                val mensaje = data.optString("mensaje", "Nueva notificación")
                val tipo = data.optString("tipo", "informativa")

                Log.d("USER_DASHBOARD", "🔔 Nueva notificación: $mensaje")

                // Mostrar notificación en pantalla
                showInAppNotification(mensaje, tipo)

                // Vibrar según el tipo
                when (tipo) {
                    "alerta_sanitaria" -> vibrarAlerta()
                    "recordatorio" -> vibrarSutil()
                    else -> vibrarSutil()
                }
            }
        } catch (e: Exception) {
            Log.e("USER_DASHBOARD", "❌ Error manejando notificación: ${e.message}")
        }
    }

    private fun handleBroadcastMessage(json: JSONObject) {
        try {
            val data = WebSocketUtils.getMessageData(json)
            if (data != null) {
                val mensaje = data.optString("mensaje", "Mensaje del sistema")

                Log.d("USER_DASHBOARD", "📢 Mensaje broadcast: $mensaje")

                // Mostrar mensaje broadcast prominente
                showBroadcastMessage(mensaje)
                vibrarAlerta()
            }
        } catch (e: Exception) {
            Log.e("USER_DASHBOARD", "❌ Error manejando broadcast: ${e.message}")
        }
    }

    private fun handleUnreadCount(json: JSONObject) {
        try {
            val count = json.optInt("count", 0)
            Log.d("USER_DASHBOARD", "📊 Notificaciones no leídas: $count")

            // Aquí podrías actualizar un badge si tienes uno en el layout
        } catch (e: Exception) {
            Log.e("USER_DASHBOARD", "❌ Error manejando contador: ${e.message}")
        }
    }

    private fun handleAnimalUpdate(json: JSONObject) {
        try {
            val data = WebSocketUtils.getMessageData(json)
            if (data != null) {
                val action = data.optString("action", "unknown")
                val animalId = data.optInt("id", -1)

                Log.d("USER_DASHBOARD", "🐄 Actualización animal: $action para ID $animalId")

                // Mostrar notificación discreta
                when (action) {
                    "created" -> showInAppNotification("🐄 Nuevo animal registrado", "informativa")
                    "updated" -> showInAppNotification("🐄 Información de animal actualizada", "informativa")
                    "deleted" -> showInAppNotification("🐄 Animal eliminado del sistema", "informativa")
                }

                // Actualizar estadísticas en tiempo real
                refreshAnimalsStats()
            }
        } catch (e: Exception) {
            Log.e("USER_DASHBOARD", "❌ Error manejando actualización animal: ${e.message}")
        }
    }

    // ===== UI HELPERS =====

    private fun showInAppNotification(mensaje: String, tipo: String) {
        val icon = when (tipo) {
            "alerta_sanitaria" -> "🚨"
            "recordatorio" -> "⏰"
            "informativa" -> "ℹ️"
            else -> "📢"
        }

        UiUtils.showToastSafe(this, "$icon $mensaje", true)
    }

    private fun showBroadcastMessage(mensaje: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📢 Mensaje del Sistema")
            .setMessage(mensaje)
            .setPositiveButton("Entendido", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    private fun updateConnectionStatus() {
        // Log del estado de conexión
        Log.d("USER_DASHBOARD", "📡 Estado conexión WebSocket: $isWebSocketConnected")

        // Si tienes un indicador visual en el layout, descoméntalo:
        // UiUtils.setViewVisibility(binding.indicatorConnection, isWebSocketConnected)
    }

    private fun attemptReconnection() {
        if (connectionRetryCount < maxRetryCount) {
            connectionRetryCount++
            Log.d("USER_DASHBOARD", "🔄 Reintentando conexión WebSocket ($connectionRetryCount/$maxRetryCount)")

            handler.postDelayed({
                connectWebSocketForUser()
            }, 2000L * connectionRetryCount) // Delay incremental
        } else {
            Log.w("USER_DASHBOARD", "⚠️ Máximo de reintentos alcanzado")
            UiUtils.showToastSafe(this, "⚠️ Conexión en tiempo real no disponible", true)
        }
    }

    // ===== VIBRATION HELPERS =====

    private fun vibrarSutil() {
        if (checkSelfPermission(android.Manifest.permission.VIBRATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("USER_DASHBOARD", "Sin permiso de vibración")
            return
        }

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.w("USER_DASHBOARD", "No se pudo vibrar: ${e.message}")
        }
    }

    private fun vibrarAlerta() {
        if (checkSelfPermission(android.Manifest.permission.VIBRATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("USER_DASHBOARD", "Sin permiso de vibración")
            return
        }

        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 200, 100, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val pattern = longArrayOf(0, 200, 100, 200)
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w("USER_DASHBOARD", "No se pudo vibrar: ${e.message}")
        }
    }

    // ===== DATA LOADING =====

    private fun cargarDatosIniciales() {
        Log.d("USER_DASHBOARD", "🔄 Cargando datos iniciales del dashboard")

        lifecycleScope.launch {
            loadAnimalsStats()
            loadIncidentsStats()
            loadTreatmentsStats()
            loadEventsStats()
        }
    }

    private fun refreshAnimalsStats() {
        Log.d("USER_DASHBOARD", "🔄 Actualizando estadísticas de animales")
        lifecycleScope.launch {
            loadAnimalsStats()
        }
    }

    private fun cargarInfoUsuario() {
        lifecycleScope.launch {
            safeApiCall("LOAD_USER_INFO") {
                apiService.getCurrentUser()
            }.onSuccess { response ->
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    runOnUiThread {
                        // Personalizar subtítulo con nombre del usuario
                        val nombreCompleto = "${user.nombre} ${user.apellidos}".trim()
                        if (nombreCompleto.isNotEmpty()) {
                            UiUtils.setTextSafe(binding.textSubtitle, "Panel de Control - $nombreCompleto")
                        } else {
                            UiUtils.setTextSafe(binding.textSubtitle, "Panel de Control - Usuario")
                        }

                        Log.d("USER_DASHBOARD", "✅ Usuario cargado: ${user.nombre}")
                    }
                }
            }.onError { message ->
                Log.e("USER_DASHBOARD", "❌ Error cargando usuario: $message")
                runOnUiThread {
                    UiUtils.setTextSafe(binding.textSubtitle, "Panel de Control - Usuario")
                }
            }
        }
    }

    private fun showUserMenu() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("👤 Mi Perfil")
            .setItems(arrayOf(
                "🔄 Actualizar datos",
                "👤 Ver mi perfil",
                "🔔 Notificaciones",
                "📱 Información de la app",
                "🚪 Cerrar sesión"
            )) { _, which ->
                when (which) {
                    0 -> cargarDatosIniciales()
                    1 -> {
                        Log.d("USER_DASHBOARD", "👤 Abriendo perfil de usuario")
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }
                    2 -> UiUtils.showToastSafe(this, "Configuración de notificaciones - Próximamente")
                    3 -> mostrarInfoApp()
                    4 -> logout()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarInfoApp() {
        val estadoConexion = if (isWebSocketConnected) "🟢 Conectado" else "🔴 Desconectado"

        val mensaje = buildString {
            append("📱 Genetics - Gestión Ganadera\n\n")
            append("🏢 Sistema integral de gestión de ganado\n\n")
            append("✨ Tus funcionalidades:\n")
            append("• 👁️ Ver información de animales (solo lectura)\n")
            append("• 🚨 Reportar incidencias\n")
            append("• 💊 Registrar tratamientos\n")
            append("• 📅 Ver calendario de eventos\n\n")
            append("📡 Tiempo real: $estadoConexion\n")
            append("🔄 Última actualización: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}\n\n")
            append("ℹ️ Para crear o editar animales, contacta con un administrador.\n\n")
            append("📞 Soporte: genetics@example.com\n")
            append("🌐 Web: www.genetics-app.com")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📱 Información de la App")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    // ===== STATS LOADING =====

    private suspend fun loadAnimalsStats() {
        safeApiCall("LOAD_ANIMALS_STATS") {
            apiService.getAnimales()
        }.onSuccess { response ->
            if (response.isSuccessful && response.body() != null) {
                val animalesResponse = response.body()!!
                val animals = animalesResponse.results

                runOnUiThread {
                    UiUtils.setTextSafe(binding.textAnimalsCount, animals.size.toString())
                }

                Log.d("USER_DASHBOARD", "📊 Animales cargados: ${animals.size}")
            }
        }.onError { message ->
            Log.w("USER_DASHBOARD", "Error cargando animales: $message")
            runOnUiThread {
                UiUtils.setTextSafe(binding.textAnimalsCount, "0")
            }
        }
    }

    private suspend fun loadIncidentsStats() {
        safeApiCall("LOAD_INCIDENTS_STATS") {
            apiService.getIncidencias()
        }.onSuccess { response ->
            if (response.isSuccessful && response.body() != null) {
                val incidentResponse = response.body()!!
                val incidents = incidentResponse.results
                val pending = incidents.count { it.estado == "pendiente" }

                runOnUiThread {
                    UiUtils.setTextSafe(binding.textIncidentsCount, pending.toString())
                }

                Log.d("USER_DASHBOARD", "📊 Incidencias pendientes: $pending de ${incidents.size}")
            }
        }.onError { message ->
            Log.w("USER_DASHBOARD", "Error cargando incidencias: $message")
            runOnUiThread {
                UiUtils.setTextSafe(binding.textIncidentsCount, "0")
            }
        }
    }

    private suspend fun loadTreatmentsStats() {
        safeApiCall("LOAD_TREATMENTS_STATS") {
            apiService.getTratamientos()
        }.onSuccess { response ->
            if (response.isSuccessful && response.body() != null) {
                val treatmentResponse = response.body()!!
                val treatments = treatmentResponse.results

                val recentTreatments = treatments.filter {
                    try {
                        val treatmentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it.fecha)
                        val weekAgo = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.DAY_OF_MONTH, -7)
                        }.time
                        treatmentDate?.after(weekAgo) == true
                    } catch (e: Exception) {
                        false
                    }
                }

                runOnUiThread {
                    UiUtils.setTextSafe(binding.textTreatmentsCount, recentTreatments.size.toString())
                }

                Log.d("USER_DASHBOARD", "📊 Tratamientos recientes: ${recentTreatments.size} de ${treatments.size}")
            }
        }.onError { message ->
            Log.w("USER_DASHBOARD", "Error cargando tratamientos: $message")
            runOnUiThread {
                UiUtils.setTextSafe(binding.textTreatmentsCount, "0")
            }
        }
    }

    private suspend fun loadEventsStats() {
        safeApiCall("LOAD_EVENTS_STATS") {
            apiService.getEventos()
        }.onSuccess { response ->
            if (response.isSuccessful) {
                val events = response.body()?.results ?: emptyList()
                val upcomingEvents = events.filter { evento ->
                    try {
                        val eventoFecha = evento.fecha_inicio.substring(0, 10)
                        val eventDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(eventoFecha)
                        val today = java.util.Calendar.getInstance().time
                        val nextWeek = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.DAY_OF_MONTH, 7)
                        }.time
                        eventDate?.let { it.after(today) && it.before(nextWeek) } == true
                    } catch (e: Exception) {
                        false
                    }
                }

                runOnUiThread {
                    UiUtils.setTextSafe(binding.textEventsCount, upcomingEvents.size.toString())
                }

                Log.d("USER_DASHBOARD", "📊 Eventos próximos: ${upcomingEvents.size} de ${events.size}")
            }
        }.onError { message ->
            Log.w("USER_DASHBOARD", "Error cargando eventos: $message")
            runOnUiThread {
                UiUtils.setTextSafe(binding.textEventsCount, "0")
            }
        }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                Log.d("USER_DASHBOARD", "🚪 Cerrando sesión...")

                // Desconectar WebSockets
                disconnectWebSockets()

                RetrofitClient.clearToken()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                UiUtils.showToastSafe(this, "Sesión cerrada correctamente")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ===== LIFECYCLE =====

    override fun onResume() {
        super.onResume()

        // Limpiar selección del bottom navigation
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        // Recargar estadísticas al volver
        cargarDatosIniciales()

        // Reconectar WebSocket si es necesario
        if (!isWebSocketConnected) {
            Log.d("USER_DASHBOARD", "🔄 Reconectando WebSocket en onResume")
            connectWebSocketForUser()
        }

        Log.d("USER_DASHBOARD", "🔄 onResume - Dashboard actualizado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("USER_DASHBOARD", "⏸️ onPause - Dashboard pausado")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("USER_DASHBOARD", "🗑️ onDestroy - Desconectando WebSockets")

        // Limpiar recursos WebSocket
        disconnectWebSockets()
    }

    private fun disconnectWebSockets() {
        try {
            RetrofitClient.disconnectNotificationsWebSocket()
            RetrofitClient.disconnectAnimalsWebSocket()

            isWebSocketConnected = false
            Log.d("USER_DASHBOARD", "🔌 WebSockets desconectados")
        } catch (e: Exception) {
            Log.e("USER_DASHBOARD", "❌ Error desconectando WebSockets: ${e.message}")
        }
    }
}