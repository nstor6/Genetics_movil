package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Create.AddIncidentActivity
import com.example.genetics.api.Animals
import com.example.genetics.api.Incidencia
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.Tratamiento
import com.example.genetics.databinding.ActivityAnimalDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnimalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimalDetailBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var animal: Animals

    companion object {
        private const val REQUEST_EDIT_ANIMAL = 5001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del animal desde el intent
        val animalId = intent.getIntExtra("ANIMAL_ID", -1)
        if (animalId == -1) {
            Toast.makeText(this, "Error: Animal no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarDatosAnimal(animalId)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalles del Animal"

        // AÑADIDO: Forzar color negro para los iconos del toolbar
        binding.toolbar.navigationIcon?.setTint(android.graphics.Color.BLACK)
        binding.toolbar.overflowIcon?.setTint(android.graphics.Color.BLACK)

        // Configurar tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Info"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Incidencias"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tratamientos"))

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
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

        // FAB para nueva incidencia
        binding.fabNewIncident.setOnClickListener {
            // Abrir formulario de nueva incidencia con este animal preseleccionado
            val intent = Intent(this, AddIncidentActivity::class.java)
            intent.putExtra("PRESELECTED_ANIMAL_ID", animal.id)
            intent.putExtra("PRESELECTED_ANIMAL_NAME", "${animal.chapeta} - ${animal.nombre ?: "Sin nombre"}")
            startActivity(intent)
        }
    }

    // CORREGIDO: Crear el menú en el toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.animal_detail_menu, menu)

        // AÑADIDO: Forzar color negro para los iconos del menú
        menu?.let {
            for (i in 0 until it.size()) {
                val item = it.getItem(i)
                item.icon?.setTint(android.graphics.Color.BLACK)
            }
        }

        return true
    }

    // CORREGIDO: Manejar clicks del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_animal -> {
                // Editar animal
                val intent = Intent(this, EditAnimalActivity::class.java)
                intent.putExtra("ANIMAL_ID", animal.id)
                startActivityForResult(intent, REQUEST_EDIT_ANIMAL)
                true
            }
            R.id.action_delete_animal -> {
                // Eliminar animal
                confirmarEliminarAnimal()
                true
            }
            android.R.id.home -> {
                // Botón de navegación hacia atrás
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cargarDatosAnimal(animalId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()
                if (response.isSuccessful) {
                    val animales = response.body() ?: emptyList()
                    val animalEncontrado = animales.find { it.id == animalId }

                    if (animalEncontrado != null) {
                        animal = animalEncontrado
                        mostrarDatosAnimal()
                        mostrarInfoGeneral() // Mostrar pestaña por defecto
                    } else {
                        Toast.makeText(this@AnimalDetailActivity, "Animal no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AnimalDetailActivity, "Error cargando datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDatosAnimal() {
        supportActionBar?.title = "${animal.chapeta} - ${animal.nombre ?: "Sin nombre"}"

        // Mostrar foto o placeholder
        binding.imageAnimal.setImageResource(R.drawable.cow_image)

        // Información básica en el header
        binding.textChapeta.text = animal.chapeta ?: "N/A"
        binding.textNombre.text = animal.nombre ?: "Sin nombre"
        binding.textRaza.text = animal.raza ?: "N/A"
    }

    private fun mostrarInfoGeneral() {
        binding.contentContainer.removeAllViews()

        val infoView = layoutInflater.inflate(R.layout.fragment_animal_info, binding.contentContainer, false)

        // Llenar datos
        infoView.findViewById<android.widget.TextView>(R.id.textSexo).text =
            formatearSexo(animal.sexo)
        infoView.findViewById<android.widget.TextView>(R.id.textFechaNacimiento).text =
            formatearFecha(animal.fecha_nacimiento)
        infoView.findViewById<android.widget.TextView>(R.id.textEstadoReproductivo).text =
            animal.estado_reproductivo ?: "No definido"
        infoView.findViewById<android.widget.TextView>(R.id.textEstadoProductivo).text =
            animal.estado_productivo ?: "No definido"
        infoView.findViewById<android.widget.TextView>(R.id.textPeso).text =
            if (animal.peso_actual != null) "${animal.peso_actual} kg" else "No registrado"
        infoView.findViewById<android.widget.TextView>(R.id.textUbicacion).text =
            animal.ubicacion_actual ?: "No definida"
        infoView.findViewById<android.widget.TextView>(R.id.textNotas).text =
            animal.notas ?: "Sin observaciones"

        binding.contentContainer.addView(infoView)
    }

    private fun cargarIncidencias() {
        binding.contentContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val response = apiService.getIncidencias()
                if (response.isSuccessful) {
                    val todasIncidencias = response.body() ?: emptyList()
                    val incidenciasAnimal = todasIncidencias.filter { it.animal == animal.id }

                    mostrarIncidencias(incidenciasAnimal)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AnimalDetailActivity, "Error cargando incidencias: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarTratamientos() {
        binding.contentContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val response = apiService.getTratamientos()
                if (response.isSuccessful) {
                    val todosTratamientos = response.body() ?: emptyList()
                    val tratamientosAnimal = todosTratamientos.filter { it.animal == animal.id }

                    mostrarTratamientos(tratamientosAnimal)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AnimalDetailActivity, "Error cargando tratamientos: ${e.message}", Toast.LENGTH_SHORT).show()
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
            }
            linearLayout.addView(textView)
        } else {
            incidencias.forEach { incidencia ->
                val cardView = crearCardIncidencia(incidencia)
                linearLayout.addView(cardView)
            }
        }

        scrollView.addView(linearLayout)
        binding.contentContainer.addView(scrollView)
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
            }
            linearLayout.addView(textView)
        } else {
            tratamientos.forEach { tratamiento ->
                val cardView = crearCardTratamiento(tratamiento)
                linearLayout.addView(cardView)
            }
        }

        scrollView.addView(linearLayout)
        binding.contentContainer.addView(scrollView)
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
            text = incidencia.tipo
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
            text = "Detectada: ${formatearFecha(incidencia.fecha_deteccion)}"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }

        linearLayout.addView(headerLayout)
        linearLayout.addView(descripcionText)
        linearLayout.addView(fechaText)

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
            text = "Dosis: ${tratamiento.dosis} - Duración: ${tratamiento.duracion}"
            textSize = 14f
            setPadding(0, 4, 0, 4)
        }

        // Fecha
        val fechaText = android.widget.TextView(this).apply {
            text = "Fecha: ${formatearFecha(tratamiento.fecha)}"
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }

        // Observaciones si existen
        if (!tratamiento.observaciones.isNullOrEmpty()) {
            val observacionesText = android.widget.TextView(this).apply {
                text = "Observaciones: ${tratamiento.observaciones}"
                textSize = 12f
                setPadding(0, 8, 0, 0)
                setTextColor(android.graphics.Color.DKGRAY)
            }
            linearLayout.addView(observacionesText)
        }

        linearLayout.addView(medicamentoText)
        linearLayout.addView(dosisText)
        linearLayout.addView(fechaText)

        cardView.addView(linearLayout)
        return cardView
    }

    private fun confirmarEliminarAnimal() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar Animal")
            .setMessage("¿Estás seguro de que quieres eliminar el animal '${animal.chapeta}'?\n\nEsta acción no se puede deshacer y eliminará también todas sus incidencias y tratamientos.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarAnimal()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAnimal() {
        lifecycleScope.launch {
            try {
                val response = apiService.eliminarAnimal(animal.id!!)
                if (response.isSuccessful) {
                    Toast.makeText(this@AnimalDetailActivity, "Animal eliminado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@AnimalDetailActivity, "Error al eliminar animal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AnimalDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_EDIT_ANIMAL && resultCode == RESULT_OK) {
            // Recargar datos del animal después de editar
            cargarDatosAnimal(animal.id!!)
        }
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
}