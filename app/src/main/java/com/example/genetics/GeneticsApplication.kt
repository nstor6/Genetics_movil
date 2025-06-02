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

        private lateinit var instance: GeneticsApplication

        fun getInstance(): GeneticsApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        Log.d("GENETICS_APP", "Iniciando aplicación Genetics...")

        // Inicializar RetrofitClient
        initializeRetrofitClient()

        // Crear canal de notificaciones
        createNotificationChannel()

        Log.d("GENETICS_APP", "Aplicación Genetics inicializada correctamente")
    }

    private fun initializeRetrofitClient() {
        try {
            RetrofitClient.initialize(this)
            Log.d("GENETICS_APP", "RetrofitClient inicializado en Application.onCreate()")
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "Error crítico inicializando RetrofitClient: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de la aplicación Genetics"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d("GENETICS_APP", "Canal de notificaciones creado")
        }
    }

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
                "package_name" to packageName
            )
        } catch (e: Exception) {
            Log.e("GENETICS_APP", "Error obteniendo info de la app: ${e.message}")
            mapOf(
                "version_name" to "1.0",
                "version_code" to "1",
                "package_name" to packageName
            )
        }
    }

    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT.toString(),
            "architecture" to (System.getProperty("os.arch") ?: "unknown")
        )
    }

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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        when (level) {
            TRIM_MEMORY_BACKGROUND -> {
                Log.d("GENETICS_APP", "App en background - conservando memoria")
            }
            TRIM_MEMORY_MODERATE -> {
                Log.d("GENETICS_APP", "Presión de memoria moderada")
            }
            TRIM_MEMORY_COMPLETE -> {
                Log.d("GENETICS_APP", "Presión de memoria crítica")
            }
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d("GENETICS_APP", "App corriendo con presión de memoria moderada")
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                Log.d("GENETICS_APP", "App corriendo con poca memoria")
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.d("GENETICS_APP", "App corriendo con memoria crítica")
            }
        }
    }
}