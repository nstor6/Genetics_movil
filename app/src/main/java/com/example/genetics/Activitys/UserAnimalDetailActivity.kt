package com.example.genetics.Activitys

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.R
import com.example.genetics.api.models.Animals
import com.example.genetics.api.Incidencia
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.Tratamiento
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class UserAnimalDetailActivity : AppCompatActivity() {

    private val apiService = RetrofitClient.getApiService()
    private lateinit var animal: Animals

    // Referencias a las vistas (sin binding)
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var imageAnimal: android.widget.ImageView
    private lateinit var textChapeta: android.widget.TextView
    private lateinit var textNombre: android.widget.TextView
    private lateinit var textRaza: android.widget.TextView
    private lateinit var tabLayout: com.google.android.material.tabs.TabLayout
    private lateinit var contentContainer: android.widget.FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_animal_detail)

        // Obtener datos del animal desde el intent
        val animalId = intent.getIntExtra("ANIMAL_ID", -1)
        if (animalId == -1) {
            Toast.makeText(this, "Error: Animal no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupUI()
        cargarDatosAnimal(animalId)
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        imageAnimal = findViewById(R.id.imageAnimal)
        textChapeta = findViewById(R.id.textChapeta)
        textNombre = findViewById(R.id.textNombre)
        textRaza = findViewById(R.id.textRaza)
        tabLayout = findViewById(R.id.tabLayout)
        contentContainer = findViewById(R.id.contentContainer)
    }

    private fun setupUI() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalles del Animal (Solo Lectura)"

        // Configurar tabs
        tabLayout.addTab(tabLayout.newTab().setText("Info"))
        tabLayout.addTab(tabLayout.newTab().setText("Incidencias"))
        tabLayout.addTab(tabLayout.newTab().setText("Tratamientos"))

        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> mostrarInfoGeneral()
                    1 -> cargarIncidencias()
                    2 -> cargarTratamientos()
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun cargarDatosAnimal(animalId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()
                if (response.isSuccessful && response.body() != null) {
                    val animalesResponse = response.body()!!
                    val animales = animalesResponse.results
                    val animalEncontrado = animales.find { it.id == animalId }

                    if (animalEncontrado != null) {
                        animal = animalEncontrado
                        mostrarDatosAnimal()
                        mostrarInfoGeneral() // Mostrar pestaña por defecto
                    } else {
                        Toast.makeText(this@UserAnimalDetailActivity, "Animal no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@UserAnimalDetailActivity, "Error en la respuesta del servidor", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserAnimalDetailActivity, "Error cargando datos: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    private fun mostrarDatosAnimal() {
        supportActionBar?.title = "${animal.chapeta} - ${animal.nombre ?: "Sin nombre"} (Solo Lectura)"

        // Mostrar foto o placeholder
        imageAnimal.setImageResource(R.drawable.cow_image)

        // Información básica en el header
        textChapeta.text = "📋 ${animal.chapeta ?: "N/A"}"
        textNombre.text = animal.nombre ?: "Sin nombre"
        textRaza.text = "🐄 ${animal.raza ?: "N/A"}"
    }

    private fun mostrarInfoGeneral() {
        contentContainer.removeAllViews()

        // Crear vista de información programáticamente
        val scrollView = android.widget.ScrollView(this)
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Crear información básica
        val infoBasica = crearSeccionInfo("Información Básica", listOf(
            "Sexo" to formatearSexo(animal.sexo),
            "Fecha de nacimiento" to formatearFecha(animal.fecha_nacimiento),
            "Estado reproductivo" to (animal.estado_reproductivo ?: "No definido"),
            "Estado productivo" to (animal.estado_productivo ?: "No definido"),
            "Peso actual" to if (animal.peso_actual != null) "${animal.peso_actual} kg" else "No registrado",
            "Ubicación actual" to (animal.ubicacion_actual ?: "No definida")
        ))

        linearLayout.addView(infoBasica)

        // Observaciones si existen
        if (!animal.notas.isNullOrEmpty()) {
            val observaciones = crearSeccionInfo("Observaciones", listOf(
                "Notas" to animal.notas!!
            ))
            linearLayout.addView(observaciones)
        }

        // Nota de solo lectura
        val notaLectura = android.widget.TextView(this).apply {
            text = "👁️ Solo lectura - Contacta con un administrador para modificar esta información"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
            alpha = 0.7f
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER
        }
        linearLayout.addView(notaLectura)

        scrollView.addView(linearLayout)
        contentContainer.addView(scrollView)
    }

    private fun crearSeccionInfo(titulo: String, datos: List<Pair<String, String>>): android.view.View {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = 12f
            cardElevation = 4f
            useCompatPadding = true
        }

        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Título de la sección
        val tituloText = android.widget.TextView(this).apply {
            text = titulo
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.colorPrimary, null))
            setPadding(0, 0, 0, 16)
        }
        linearLayout.addView(tituloText)

        // Datos
        datos.forEach { (etiqueta, valor) ->
            val rowLayout = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }

            val etiquetaText = android.widget.TextView(this).apply {
                text = "$etiqueta:"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val valorText = android.widget.TextView(this).apply {
                text = valor
                textSize = 14f
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            rowLayout.addView(etiquetaText)
            rowLayout.addView(valorText)
            linearLayout.addView(rowLayout)
        }

        cardView.addView(linearLayout)
        return cardView
    }

    private fun cargarIncidencias() {
        contentContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val response = apiService.getIncidencias()
                if (response.isSuccessful && response.body() != null) {
                    val incidentesResponse = response.body()!!
                    val todasIncidencias = incidentesResponse.results
                    val incidenciasAnimal = todasIncidencias.filter { it.animal == animal.id }

                    mostrarIncidencias(incidenciasAnimal)
                } else {
                    Toast.makeText(this@UserAnimalDetailActivity, "Error al cargar incidencias", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserAnimalDetailActivity, "Error cargando incidencias: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun cargarTratamientos() {
        contentContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val response = apiService.getTratamientos()
                if (response.isSuccessful && response.body() != null) {
                    val tratamientoResponse = response.body()!!
                    val todosTratamientos = tratamientoResponse.results
                    val tratamientosAnimal = todosTratamientos.filter { it.animal == animal.id }

                    mostrarTratamientos(tratamientosAnimal)
                } else {
                    Toast.makeText(this@UserAnimalDetailActivity, "Error al cargar tratamientos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserAnimalDetailActivity, "Error cargando tratamientos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun mostrarIncidencias(incidencias: List<Incidencia>) {
        val scrollView = android.widget.ScrollView(this)
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        if (incidencias.isEmpty()) {
            val textView = android.widget.TextView(this).apply {
                text = "No hay incidencias registradas para este animal"
                textAlignment = android.widget.TextView.TEXT_ALIGNMENT_CENTER
                setPadding(32, 32, 32, 32)
                textSize = 16f
                setTextColor(android.graphics.Color.GRAY)
            }
            linearLayout.addView(textView)
        } else {
            incidencias.forEach { incidencia ->
                val cardView = crearCardIncidencia(incidencia)
                linearLayout.addView(cardView)
            }
        }

        scrollView.addView(linearLayout)
        contentContainer.addView(scrollView)
    }

    private fun mostrarTratamientos(tratamientos: List<Tratamiento>) {
        val scrollView = android.widget.ScrollView(this)
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        if (tratamientos.isEmpty()) {
            val textView = android.widget.TextView(this).apply {
                text = "No hay tratamientos registrados para este animal"
                textAlignment = android.widget.TextView.TEXT_ALIGNMENT_CENTER
                setPadding(32, 32, 32, 32)
                textSize = 16f
                setTextColor(android.graphics.Color.GRAY)
            }
            linearLayout.addView(textView)
        } else {
            tratamientos.forEach { tratamiento ->
                val cardView = crearCardTratamiento(tratamiento)
                linearLayout.addView(cardView)
            }
        }

        scrollView.addView(linearLayout)
        contentContainer.addView(scrollView)
    }

    private fun crearCardIncidencia(incidencia: Incidencia): android.view.View {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = 12f
            cardElevation = 4f
            useCompatPadding = true
        }

        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Título y estado
        val headerLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        val titleText = android.widget.TextView(this).apply {
            text = "🚨 ${incidencia.tipo}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val estadoText = android.widget.TextView(this).apply {
            text = incidencia.estado
            textSize = 12f
            setPadding(8, 4, 8, 4)
            background = when(incidencia.estado) {
                "pendiente" -> getDrawable(R.drawable.badge_orange)
                "en tratamiento" -> getDrawable(R.drawable.badge_blue)
                "resuelto" -> getDrawable(R.drawable.badge_green)
                else -> getDrawable(R.drawable.badge_gray)
            }
            setTextColor(android.graphics.Color.WHITE)
        }

        headerLayout.addView(titleText)
        headerLayout.addView(estadoText)

        // Descripción
        val descripcionText = android.widget.TextView(this).apply {
            text = incidencia.descripcion
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }

        // Fecha
        val fechaText = android.widget.TextView(this).apply {
            text = "📅 Detectada: ${formatearFecha(incidencia.fecha_deteccion)}"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }

        // Nota de solo lectura
        val notaLectura = android.widget.TextView(this).apply {
            text = "👁️ Solo lectura - Contacta con un administrador para modificar"
            textSize = 10f
            setTextColor(android.graphics.Color.GRAY)
            alpha = 0.7f
            setPadding(0, 8, 0, 0)
        }

        linearLayout.addView(headerLayout)
        linearLayout.addView(descripcionText)
        linearLayout.addView(fechaText)
        linearLayout.addView(notaLectura)

        cardView.addView(linearLayout)
        return cardView
    }

    private fun crearCardTratamiento(tratamiento: Tratamiento): android.view.View {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = 12f
            cardElevation = 4f
            useCompatPadding = true
        }

        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Medicamento
        val medicamentoText = android.widget.TextView(this).apply {
            text = "💊 ${tratamiento.medicamento}"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Dosis y duración
        val dosisText = android.widget.TextView(this).apply {
            text = "📏 Dosis: ${tratamiento.dosis} - ⏱️ Duración: ${tratamiento.duracion}"
            textSize = 14f
            setPadding(0, 4, 0, 4)
        }

        // Fecha
        val fechaText = android.widget.TextView(this).apply {
            text = "📅 Fecha: ${formatearFecha(tratamiento.fecha)}"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }

        linearLayout.addView(medicamentoText)
        linearLayout.addView(dosisText)
        linearLayout.addView(fechaText)

        // Observaciones si existen
        if (!tratamiento.observaciones.isNullOrEmpty()) {
            val observacionesText = android.widget.TextView(this).apply {
                text = "📝 Observaciones: ${tratamiento.observaciones}"
                textSize = 12f
                setPadding(0, 8, 0, 0)
                setTextColor(android.graphics.Color.DKGRAY)
            }
            linearLayout.addView(observacionesText)
        }

        // Nota de solo lectura
        val notaLectura = android.widget.TextView(this).apply {
            text = "👁️ Solo lectura - Contacta con un administrador para modificar"
            textSize = 10f
            setTextColor(android.graphics.Color.GRAY)
            alpha = 0.7f
            setPadding(0, 8, 0, 0)
        }
        linearLayout.addView(notaLectura)

        cardView.addView(linearLayout)
        return cardView
    }

    private fun formatearSexo(sexo: String?): String {
        return when(sexo?.lowercase()) {
            "macho" -> "♂️ Macho"
            "hembra" -> "♀️ Hembra"
            else -> "❓ No especificado"
        }
    }

    private fun formatearFecha(fecha: String?): String {
        return try {
            if (fecha != null) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: Date())
            } else {
                "No especificada"
            }
        } catch (e: Exception) {
            fecha ?: "No especificada"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}