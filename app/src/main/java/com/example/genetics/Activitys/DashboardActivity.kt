package com.example.genetics.Activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Activitys.AnimalsActivity
import com.example.genetics.Activitys.CalendarActivity
import com.example.genetics.Activitys.IncidentsActivity
import com.example.genetics.Activitys.TreatmentsActivity
import com.example.genetics.Activitys.UsersActivity
import com.example.genetics.Create.AddAnimalActivity
import com.example.genetics.Create.AddEventActivity
import com.example.genetics.Create.AddIncidentActivity
import com.example.genetics.Create.AddTreatmentActivity
import com.example.genetics.Create.AddUserActivity
import com.example.genetics.LoginActivity
import com.example.genetics.R
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityDashboardBinding
import com.example.genetics.utils.safeApiCall
import com.example.genetics.utils.onSuccess
import com.example.genetics.utils.onError
import com.example.genetics.websocket.WebSocketManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val apiService = RetrofitClient.getApiService()
    private var unreadNotificationsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("DASHBOARD", "Dashboard iniciado")
        setupUI()
        setupWebSockets()
        loadStats()
    }

    private fun setupUI() {
        Log.d("DASHBOARD", "Configurando UI...")

        // Tarjetas clickeables - Navegar a las respectivas activities
        binding.cardAnimals.setOnClickListener {
            Log.d("DASHBOARD", "Click en Animals")
            startActivity(Intent(this, AnimalsActivity::class.java))
        }

        binding.cardIncidents.setOnClickListener {
            Log.d("DASHBOARD", "Click en Incidents")
            startActivity(Intent(this, IncidentsActivity::class.java))
        }

        binding.cardTreatments.setOnClickListener {
            Log.d("DASHBOARD", "Click en Treatments")
            startActivity(Intent(this, TreatmentsActivity::class.java))
        }

        binding.cardEvents.setOnClickListener {
            Log.d("DASHBOARD", "Click en Events")
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // Tarjeta de usuarios - Siempre visible (ya que este dashboard es solo para admins)
        binding.cardUsers.setOnClickListener {
            Log.d("DASHBOARD", "Click en Users")
            startActivity(Intent(this, UsersActivity::class.java))
        }

        // Configurar navegación bottom
        setupBottomNavigation()

        // Botones de acciones rápidas
        binding.buttonViewAnimals.setOnClickListener {
            Log.d("DASHBOARD", "Click en buttonViewAnimals")
            startActivity(Intent(this, AddAnimalActivity::class.java))
        }

        binding.buttonNewIncident.setOnClickListener {
            Log.d("DASHBOARD", "Click en buttonNewIncident")
            startActivity(Intent(this, AddIncidentActivity::class.java))
        }

        binding.buttonNewTreatment.setOnClickListener {
            Log.d("DASHBOARD", "Click en buttonNewTreatment")
            startActivity(Intent(this, AddTreatmentActivity::class.java))
        }

        binding.buttonNewEvent.setOnClickListener {
            Log.d("DASHBOARD", "Click en buttonNewEvent")
            startActivity(Intent(this, AddEventActivity::class.java))
        }

        // Botón de nuevo usuario - Siempre visible (solo para admins)
        binding.buttonNewUser.setOnClickListener {
            Log.d("DASHBOARD", "Click en buttonNewUser")
            startActivity(Intent(this, AddUserActivity::class.java))
        }
    }

    /**
     * Configurar WebSockets para tiempo real
     */
    private fun setupWebSockets() {
        Log.d("DASHBOARD", "Configurando WebSockets...")

        // Conectar WebSocket de notificaciones
        WebSocketManager.connectNotifications(
            onNotification = { notification ->
                handleNotification(notification)
            },
            onStatusChange = { isConnected ->
                Log.d("DASHBOARD", "WebSocket notificaciones: ${if (isConnected) "Conectado" else "Desconectado"}")
                if (isConnected) {
                    // Mostrar indicador de conexión
                    showConnectionStatus(true)
                } else {
                    showConnectionStatus(false)
                }
            }
        )

        // Conectar WebSocket de animales
        WebSocketManager.connectAnimals(
            onUpdate = { update ->
                handleAnimalUpdate(update)
            },
            onStatusChange = { isConnected ->
                Log.d("DASHBOARD", "WebSocket animales: ${if (isConnected) "Conectado" else "Desconectado"}")
            }
        )

        // Conectar WebSocket de logs (para admins)
        WebSocketManager.connectLogs(
            onLog = { log ->
                handleLogUpdate(log)
            },
            onStatusChange = { isConnected ->
                Log.d("DASHBOARD", "WebSocket logs: ${if (isConnected) "Conectado" else "Desconectado"}")
            }
        )
    }

    /**
     * Manejar notificaciones recibidas por WebSocket
     */
    private fun handleNotification(notification: JSONObject) {
        try {
            val type = notification.getString("type")
            Log.d("DASHBOARD", "Notificación recibida: $type")

            when (type) {
                "notification", "pending_notification" -> {
                    val data = notification.getJSONObject("data")
                    val mensaje = data.getString("mensaje")
                    val tipoNotificacion = data.getString("tipo")
                    val visto = data.getBoolean("visto")

                    // Mostrar toast con la notificación
                    runOnUiThread {
                        when (tipoNotificacion) {
                            "alerta_sanitaria" -> {
                                Toast.makeText(this, "⚠️ Alerta Sanitaria: $mensaje", Toast.LENGTH_LONG).show()
                            }
                            "recordatorio" -> {
                                Toast.makeText(this, "🔔 Recordatorio: $mensaje", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(this, "📢 $mensaje", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // Actualizar contador si no está vista
                        if (!visto) {
                            unreadNotificationsCount++
                            updateNotificationBadge()
                        }
                    }
                }

                "unread_count" -> {
                    val count = notification.getInt("count")
                    unreadNotificationsCount = count
                    runOnUiThread {
                        updateNotificationBadge()
                    }
                }

                "connection_established" -> {
                    Log.d("DASHBOARD", "✅ WebSocket de notificaciones establecido")
                    // Solicitar contador de no leídas
                    WebSocketManager.sendNotificationMessage("get_unread_count")
                }
            }
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error procesando notificación: ${e.message}")
        }
    }

    /**
     * Manejar actualizaciones de animales
     */
    private fun handleAnimalUpdate(update: JSONObject) {
        try {
            val type = update.getString("type")
            Log.d("DASHBOARD", "🐄 Actualización de animal: $type")

            when (type) {
                "animal_created" -> {
                    runOnUiThread {
                        Toast.makeText(this, "✅ Nuevo animal registrado", Toast.LENGTH_SHORT).show()
                        // Recargar estadísticas en una corrutina
                        lifecycleScope.launch {
                            loadAnimalsStats()
                        }
                    }
                }

                "animal_updated" -> {
                    runOnUiThread {
                        Toast.makeText(this, "✏️ Animal actualizado", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            loadAnimalsStats()
                        }
                    }
                }

                "animal_deleted" -> {
                    runOnUiThread {
                        Toast.makeText(this, "🗑️ Animal eliminado", Toast.LENGTH_SHORT).show()
                        lifecycleScope.launch {
                            loadAnimalsStats()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error procesando actualización de animal: ${e.message}")
        }
    }

    /**
     * Manejar logs recibidos
     */
    private fun handleLogUpdate(log: JSONObject) {
        try {
            val type = log.getString("type")
            Log.d("DASHBOARD", "📋 Log recibido: $type")

            when (type) {
                "new_log" -> {
                    val data = log.getJSONObject("data")
                    val tipoAccion = data.getString("tipo_accion")
                    val entidad = data.getString("entidad_afectada")

                    Log.d("DASHBOARD", "📝 Nueva acción registrada: $tipoAccion en $entidad")
                    // Aquí podrías mostrar una notificación discreta o actualizar un indicador
                }
            }
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error procesando log: ${e.message}")
        }
    }

    /**
     * Mostrar estado de conexión WebSocket
     */
    private fun showConnectionStatus(isConnected: Boolean) {
        // Aquí podrías actualizar un indicador visual del estado de conexión
        // Por ejemplo, cambiar el color de un pequeño indicador en la UI
        Log.d("DASHBOARD", "🌐 Estado de conexión WebSocket: ${if (isConnected) "Online" else "Offline"}")
    }

    /**
     * Actualizar badge de notificaciones
     */
    private fun updateNotificationBadge() {
        // Aquí podrías actualizar un badge visual que muestre el número de notificaciones no leídas
        Log.d("DASHBOARD", "🔔 Notificaciones no leídas: $unreadNotificationsCount")

        // Ejemplo de implementación:
        // if (unreadNotificationsCount > 0) {
        //     binding.badgeNotifications.visibility = View.VISIBLE
        //     binding.badgeNotifications.text = unreadNotificationsCount.toString()
        // } else {
        //     binding.badgeNotifications.visibility = View.GONE
        // }
    }

    /**
     * Función básica: Bottom Navigation sin tanto menú complejo
     */
    private fun setupBottomNavigation() {
        Log.d("DASHBOARD", "🔧 Configurando Bottom Navigation...")

        try {
            binding.bottomNavigation.setOnItemSelectedListener { item ->
                Log.d("DASHBOARD", "📱 Bottom Nav item selected: ${item.itemId}")
                when (item.itemId) {
                    R.id.nav_animals -> {
                        Log.d("DASHBOARD", "📱 Nav a Animals")
                        startActivity(Intent(this, AnimalsActivity::class.java))
                        true
                    }
                    R.id.nav_incidents -> {
                        Log.d("DASHBOARD", "📱 Nav a Incidents")
                        startActivity(Intent(this, IncidentsActivity::class.java))
                        true
                    }
                    R.id.nav_treatments -> {
                        Log.d("DASHBOARD", "📱 Nav a Treatments")
                        startActivity(Intent(this, TreatmentsActivity::class.java))
                        true
                    }
                    R.id.nav_calendar -> {
                        Log.d("DASHBOARD", "📱 Nav a Calendar")
                        startActivity(Intent(this, CalendarActivity::class.java))
                        true
                    }
                    R.id.nav_settings -> {
                        Log.d("DASHBOARD", "📱 Nav a Settings - Mostrando menú de ajustes")
                        showSettingsMenu()
                        true
                    }
                    else -> {
                        Log.w("DASHBOARD", "⚠️ Item no reconocido en Bottom Nav: ${item.itemId}")
                        false
                    }
                }
            }
            Log.d("DASHBOARD", "✅ Bottom Navigation configurado")
        } catch (e: Exception) {
            Log.e("DASHBOARD", "❌ Error configurando Bottom Navigation: ${e.message}")
        }
    }

    /**
     * Menú de ajustes simplificado
     */
    private fun showSettingsMenu() {
        val status = WebSocketManager.getConnectionStatus()
        val connectionInfo = buildString {
            append("Estado de conexiones:\n")
            append("🔔 Notificaciones: ${if (status["notifications"] == true) "Conectado" else "Desconectado"}\n")
            append("🐄 Animales: ${if (status["animals"] == true) "Conectado" else "Desconectado"}\n")
            append("📋 Logs: ${if (status["logs"] == true) "Conectado" else "Desconectado"}")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚙️ Configuración")
            .setItems(arrayOf(
                "👥 Gestionar usuarios",
                "🔄 Actualizar datos",
                "👤 Mi perfil",
                "🌐 Estado WebSocket",
                "📱 Información de la app",
                "🚪 Cerrar sesión"
            )) { _, which ->
                when (which) {
                    0 -> {
                        // Gestionar usuarios
                        Log.d("DASHBOARD", "⚙️ Abriendo gestión de usuarios desde ajustes")
                        startActivity(Intent(this, UsersActivity::class.java))
                    }
                    1 -> {
                        // Actualizar datos
                        Toast.makeText(this, "🔄 Actualizando datos...", Toast.LENGTH_SHORT).show()
                        // 🔧 CORREGIDO: Usar lifecycleScope.launch
                        lifecycleScope.launch {
                            loadStats()
                        }
                    }
                    2 -> {
                        // Mi perfil
                        Toast.makeText(this, "👤 Mi perfil - Próximamente", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        // Estado WebSocket
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("🌐 Estado WebSocket")
                            .setMessage(connectionInfo)
                            .setPositiveButton("🔄 Reconectar") { _, _ ->
                                reconnectWebSockets()
                            }
                            .setNegativeButton("Cerrar", null)
                            .show()
                    }
                    4 -> {
                        // Información de la app
                        mostrarInfoApp()
                    }
                    5 -> {
                        // Cerrar sesión
                        logout()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Reconectar WebSockets
     */
    private fun reconnectWebSockets() {
        Log.d("DASHBOARD", "🔄 Reconectando WebSockets...")
        WebSocketManager.disconnectAll()

        // Esperar un momento antes de reconectar
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000)
            setupWebSockets()
            Toast.makeText(this@DashboardActivity, "🔄 Reconectando...", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Información de la app
     */
    private fun mostrarInfoApp() {
        val mensaje = buildString {
            append("📱 Genetics - Gestión Ganadera\n\n")
            append("🏢 Desarrollado para la gestión integral de ganado\n\n")
            append("✨ Funcionalidades:\n")
            append("• 🐄 Gestión de animales\n")
            append("• 🚨 Control de incidencias\n")
            append("• 💊 Registro de tratamientos\n")
            append("• 📅 Calendario de eventos\n")
            append("• 👥 Administración de usuarios\n")
            append("• 🔔 Notificaciones en tiempo real\n\n")
            append("📞 Soporte: genetics@example.com\n")
            append("🌐 Web: www.genetics-app.com")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📱 Información de la App")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    // 🔧 FUNCIÓN LOADSTATS NO SUSPEND - Solo lanza las corrutinas
    private fun loadStats() {
        lifecycleScope.launch {
            loadAnimalsStats()
            loadIncidentsStats()
            loadTreatmentsStats()
            loadEventsStats()
            loadUsersStats()
        }
    }

    private suspend fun loadAnimalsStats() {
        safeApiCall("LOAD_ANIMALS_STATS") {
            apiService.getAnimales()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val animals = response.body() ?: emptyList()
                    runOnUiThread {
                        binding.textAnimalsCount.text = animals.size.toString()
                    }
                }
            }
            .onError { message ->
                Log.w("DASHBOARD", "Error cargando animales: $message")
            }
    }

    private suspend fun loadIncidentsStats() {
        safeApiCall("LOAD_INCIDENTS_STATS") {
            apiService.getIncidencias()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val incidents = response.body() ?: emptyList()
                    val pending = incidents.count { it.estado == "pendiente" }
                    runOnUiThread {
                        binding.textIncidentsCount.text = pending.toString()
                    }
                }
            }
            .onError { message ->
                Log.w("DASHBOARD", "Error cargando incidencias: $message")
            }
    }

    private suspend fun loadTreatmentsStats() {
        safeApiCall("LOAD_TREATMENTS_STATS") {
            apiService.getTratamientos()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val treatments = response.body() ?: emptyList()
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
                        binding.textTreatmentsCount.text = recentTreatments.size.toString()
                    }
                }
            }
            .onError { message ->
                Log.w("DASHBOARD", "Error cargando tratamientos: $message")
            }
    }

    private suspend fun loadEventsStats() {
        safeApiCall("LOAD_EVENTS_STATS") {
            apiService.getEventos()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
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
                        binding.textEventsCount.text = upcomingEvents.size.toString()
                    }
                }
            }
            .onError { message ->
                Log.w("DASHBOARD", "Error cargando eventos: $message")
            }
    }

    private suspend fun loadUsersStats() {
        safeApiCall("LOAD_USERS_STATS") {
            apiService.getUsers()
        }
            .onSuccess { response ->
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    val activeUsers = users.count { it.activo == true }
                    runOnUiThread {
                        binding.textUsersCount.text = activeUsers.toString()
                    }
                }
            }
            .onError { message ->
                Log.w("DASHBOARD", "Error cargando usuarios: $message")
                runOnUiThread {
                    binding.textUsersCount.text = "0"
                }
            }
    }

    private fun logout() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Cerrar Sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?\n\nTendrás que volver a introducir tus credenciales.")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                Log.d("DASHBOARD", "🚪 Cerrando sesión...")

                // Desconectar WebSockets antes de limpiar token
                WebSocketManager.disconnectAll()

                // Limpiar token
                RetrofitClient.clearToken()

                // Ir al login
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
                Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.menu.setGroupCheckable(0, true, false)
        for (i in 0 until binding.bottomNavigation.menu.size()) {
            binding.bottomNavigation.menu.getItem(i).isChecked = false
        }
        binding.bottomNavigation.menu.setGroupCheckable(0, true, true)

        // 🔧 CORREGIDO: Usar lifecycleScope.launch para llamar a loadStats()
        lifecycleScope.launch {
            loadStats()
        }

        // Verificar estado de WebSockets
        val status = WebSocketManager.getConnectionStatus()
        if (!status.values.any { it }) {
            Log.d("DASHBOARD", "🔄 Reconectando WebSockets en onResume")
            setupWebSockets()
        }
    }

    override fun onPause() {
        super.onPause()
        // No desconectar WebSockets en onPause para mantener notificaciones en background
        Log.d("DASHBOARD", "⏸️ onPause - Manteniendo WebSockets conectados")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desconectar WebSockets solo cuando se destruye la actividad
        WebSocketManager.disconnectAll()
        Log.d("DASHBOARD", "🗑️ onDestroy - WebSockets desconectados")
    }
}