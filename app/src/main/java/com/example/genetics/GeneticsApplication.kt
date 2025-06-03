package com.example.genetics

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.genetics.api.RetrofitClient

class GeneticsApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "genetics_notifications"
        const val NOTIFICATION_CHANNEL_NAME = "Genetics Notifications"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notificaciones de la aplicación Genetics"

        private lateinit var instance: GeneticsApplication

        fun getInstance(): GeneticsApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d("GENETICS_APP", "🚀 ===== INICIANDO GENETICS APPLICATION =====")
        Log.d("GENETICS_APP", "📱 Dispositivo: ${Build.MODEL}")
        Log.d("GENETICS_APP", "🤖 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d("GENETICS_APP", "📦 Package: $packageName")

        try {
            // Paso 1: Inicializar servicios básicos
            initializeBasicServices()

            // Paso 2: Inicializar RetrofitClient
            initializeNetworking()

            // Paso 3: Crear canal de notificaciones
            createNotificationChannel()

            // Paso 4: Configurar manejo de excepciones no capturadas
            setupExceptionHandler()

            Log.d("GENETICS_APP", "✅ ===== GENETICS APPLICATION INICIALIZADA CORRECTAMENTE =====")

        } catch (e: Exception) {
            Log.e("GENETICS_APP", "💥 Error crítico inicializando aplicación: ${e.message}")
            e.printStackTrace()

            // En caso de error crítico, todavía intentar funcionar
            // pero registrar el error para debugging
            logCriticalError(e)
        }
    }

    /**
     * Inicializar servicios básicos
     */
    private fun initializeBasicServices() {
        try {
            Log.d("GENETICS_APP", "🔧 Inicializando servicios básicos...")

            // Verificar conectividad básica
            val hasNetwork = isNetworkAvailable()
            Log.d("GENETICS_APP", "🌐 Conectividad: ${if (hasNetwork) "Disponible" else "No disponible"}")

            // Log de información del dispositivo
            logDeviceInfo()

            Log.d("GENETICS_APP", "✅ Servicios básicos inicializados")

        } catch (e: Exception) {
            Log.e("GENETICS_APP", "❌ Error inicializando servicios básicos: ${e.message}")
            throw e
        }
    }

    /**
     * Inicializar networking (RetrofitClient)
     */
    private fun initializeNetworking() {
        try {
            Log.d("GENETICS_APP", "🌐 Inicializando networking...")

            val success = RetrofitClient.initialize(this)

            if (success) {
                Log.d("GENETICS_APP", "✅ RetrofitClient inicializado en Application.onCreate()")

                // Debug del estado
                RetrofitClient.debugStatus()

            } else {
                Log.e("GENETICS_APP", "❌ Falló inicialización de RetrofitClient")

                // No lanzar excepción, la app puede funcionar parcialmente
                // El usuario verá errores específicos cuando trate de hacer login
            }

        } catch (e: Exception) {
            Log.e("GENETICS_APP", "💥 Error crítico inicializando networking: ${e.message}")
            e.printStackTrace()

            // No lanzar excepción para que la app no crashee completamente
            // pero registrar el error
        }
    }

    /**
     * Crear canal de notificaciones para Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Log.d("GENETICS_APP", "🔔 Creando canal de notificaciones...")

                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = NOTIFICATION_CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                    lightColor = android.graphics.Color.BLUE
                    vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                Log.d("GENETICS_APP", "✅ Canal de notificaciones creado: $NOTIFICATION_CHANNEL_ID")

            } catch (e: Exception) {
                Log.e("GENETICS_APP", "❌ Error creando canal de notificaciones: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.d("GENETICS_APP", "ℹ️ Android < O, no se necesita canal de notificaciones")
        }
    }

    /**
     * Configurar manejo de excepciones no capturadas
     */
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("GENETICS_APP", "💥 EXCEPCIÓN NO CAPTURADA en hilo: ${thread.name}")
            Log.e("GENETICS_APP", "💥 Excepción: ${exception.message}")
            exception.printStackTrace()

            // Registrar en archivo o servicio de crash reporting si tienes uno
            logCriticalError(exception)

            // Llamar al handler por defecto
            defaultHandler?.uncaughtException(thread, exception)
        }

        Log.d("GENETICS_APP", "🛡️ Exception handler configurado")
    }

    /**
     * Log de información del dispositivo
     */
    private fun logDeviceInfo() {
        Log.d("GENETICS_APP", "📱 === INFORMACIÓN DEL DISPOSITIVO ===")
        Log.d("GENETICS_APP", "Fabricante: ${Build.MANUFACTURER}")
        Log.d("GENETICS_APP", "Modelo: ${Build.MODEL}")
        Log.d("GENETICS_APP", "Producto: ${Build.PRODUCT}")
        Log.d("GENETICS_APP", "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d("GENETICS_APP", "Arquitectura: ${System.getProperty("os.arch")}")
        Log.d("GENETICS_APP", "Kernel: ${System.getProperty("os.version")}")

        // Información de la app
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Log.d("GENETICS_APP", "📦 === INFORMACIÓN DE LA APP ===")
            Log.d("GENETICS_APP", "Nombre del paquete: $packageName")
            Log.d("GENETICS_APP", "Versión: ${packageInfo.versionName}")
            Log.d("GENETICS_APP", "Código de versión: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode}")
            Log.d("GENETICS_APP", "Debug mode: ${isDebugMode()}")
        } catch (e: Exception) {
            Log.w("GENETICS_APP", "No se pudo obtener info del paquete: ${e.message}")
        }

        Log.d("GENETICS_APP", "========================================")
    }

    /**
     * Verificar si hay conectividad de red
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "Error verificando conectividad: ${e.message}")
            false
        }
    }

    /**
     * Obtener información de la aplicación
     */
    fun getAppInfo(): Map<String, String> {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)

            mapOf(
                "version_name" to (packageInfo.versionName ?: "1.0"),
                "version_code" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                },
                "package_name" to packageName,
                "first_install_time" to packageInfo.firstInstallTime.toString(),
                "last_update_time" to packageInfo.lastUpdateTime.toString()
            )
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "Error obteniendo info de la app: ${e.message}")
            mapOf(
                "version_name" to "1.0",
                "version_code" to "1",
                "package_name" to packageName,
                "error" to e.message.orEmpty()
            )
        }
    }

    /**
     * Obtener información del dispositivo
     */
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "product" to Build.PRODUCT,
            "brand" to Build.BRAND,
            "android_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT.toString(),
            "architecture" to (System.getProperty("os.arch") ?: "unknown"),
            "kernel_version" to (System.getProperty("os.version") ?: "unknown"),
            "board" to Build.BOARD,
            "bootloader" to Build.BOOTLOADER,
            "fingerprint" to Build.FINGERPRINT.take(50) // Truncar para logs
        )
    }

    /**
     * Verificar si está en modo debug
     */
    fun isDebugMode(): Boolean {
        return try {
            val buildConfigClass = Class.forName("${packageName}.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            Log.w("GENETICS_APP", "No se pudo determinar modo debug: ${e.message}")
            false // Por defecto, asumir modo producción
        }
    }

    /**
     * Obtener información completa del sistema
     */
    fun getSystemInfo(): Map<String, Any> {
        return mapOf(
            "app_info" to getAppInfo(),
            "device_info" to getDeviceInfo(),
            "network_available" to isNetworkAvailable(),
            "debug_mode" to isDebugMode(),
            "total_memory" to getTotalMemory(),
            "available_memory" to getAvailableMemory(),
            "storage_info" to getStorageInfo()
        )
    }

    /**
     * Obtener memoria total del dispositivo
     */
    private fun getTotalMemory(): Long {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Obtener memoria disponible
     */
    private fun getAvailableMemory(): Long {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            memInfo.availMem
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Obtener información de almacenamiento
     */
    private fun getStorageInfo(): Map<String, Long> {
        return try {
            val statsFs = android.os.StatFs(filesDir.path)
            val blockSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                statsFs.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                statsFs.blockSize.toLong()
            }

            val totalBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                statsFs.blockCountLong
            } else {
                @Suppress("DEPRECATION")
                statsFs.blockCount.toLong()
            }

            val availableBlocks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                statsFs.availableBlocksLong
            } else {
                @Suppress("DEPRECATION")
                statsFs.availableBlocks.toLong()
            }

            mapOf(
                "total_space" to (totalBlocks * blockSize),
                "available_space" to (availableBlocks * blockSize),
                "used_space" to ((totalBlocks - availableBlocks) * blockSize)
            )
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "Error obteniendo info de almacenamiento: ${e.message}")
            mapOf(
                "total_space" to -1L,
                "available_space" to -1L,
                "used_space" to -1L
            )
        }
    }

    /**
     * Registrar error crítico
     */
    private fun logCriticalError(exception: Throwable) {
        try {
            val errorInfo = buildString {
                appendLine("=== ERROR CRÍTICO GENETICS APP ===")
                appendLine("Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                appendLine("Excepción: ${exception.javaClass.simpleName}")
                appendLine("Mensaje: ${exception.message}")
                appendLine("Stack trace:")
                appendLine(exception.stackTraceToString())
                appendLine("=== INFO DEL SISTEMA ===")
                getSystemInfo().forEach { (key, value) ->
                    appendLine("$key: $value")
                }
                appendLine("=================================")
            }

            Log.e("GENETICS_APP_CRASH", errorInfo)

            // Aquí podrías enviar el error a un servicio de crash reporting
            // como Firebase Crashlytics, Bugsnag, etc.

        } catch (e: Exception) {
            Log.e("GENETICS_APP", "Error registrando error crítico: ${e.message}")
        }
    }

    /**
     * Manejo de memoria - llamado por el sistema cuando hay presión de memoria
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        val levelDescription = when (level) {
            TRIM_MEMORY_BACKGROUND -> "App en background"
            TRIM_MEMORY_MODERATE -> "Presión de memoria moderada"
            TRIM_MEMORY_COMPLETE -> "Presión de memoria crítica"
            TRIM_MEMORY_RUNNING_MODERATE -> "App corriendo - presión moderada"
            TRIM_MEMORY_RUNNING_LOW -> "App corriendo - memoria baja"
            TRIM_MEMORY_RUNNING_CRITICAL -> "App corriendo - memoria crítica"
            TRIM_MEMORY_UI_HIDDEN -> "UI oculta"
            else -> "Nivel desconocido: $level"
        }

        Log.d("GENETICS_APP", "🧠 Trim Memory: $levelDescription")

        // Aquí podrías limpiar caches, desconectar WebSockets no esenciales, etc.
        when (level) {
            TRIM_MEMORY_COMPLETE -> {
                // Limpiar todo lo posible
                Log.d("GENETICS_APP", "🗑️ Limpieza agresiva de memoria")
                // RetrofitClient.clearNonEssentialCaches() // Si implementas caches
            }
            TRIM_MEMORY_MODERATE -> {
                // Limpiar caches menos críticos
                Log.d("GENETICS_APP", "🗑️ Limpieza moderada de memoria")
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App está corriendo pero memoria crítica
                Log.w("GENETICS_APP", "⚠️ Memoria crítica mientras app está activa")
            }
        }
    }

    /**
     * Manejo de cambios de configuración
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("GENETICS_APP", "📱 Configuration changed: ${newConfig.toString()}")
    }

    /**
     * Limpieza cuando la aplicación termina
     */
    override fun onTerminate() {
        super.onTerminate()
        Log.d("GENETICS_APP", "🛑 Application terminando...")

        try {
            // Limpiar recursos
            RetrofitClient.disconnectAllWebSockets()
            Log.d("GENETICS_APP", "✅ Recursos limpiados")
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "❌ Error limpiando recursos: ${e.message}")
        }

        Log.d("GENETICS_APP", "👋 Genetics Application terminada")
    }
}