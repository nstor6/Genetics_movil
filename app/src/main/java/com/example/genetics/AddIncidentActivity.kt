package com.example.genetics

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Animals
import com.example.genetics.api.Incidencia
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAddIncidentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddIncidentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddIncidentBinding
    private val apiService = RetrofitClient.getApiService()
    private val calendar = Calendar.getInstance()
    private val animalesList = mutableListOf<Animals>()
    private var selectedAnimalId: Int? = null
    private var preselectedAnimalId: Int? = null
    private var preselectedAnimalName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddIncidentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener animal preseleccionado si viene desde detalles
        preselectedAnimalId = intent.getIntExtra("PRESELECTED_ANIMAL_ID", -1).takeIf { it != -1 }
        preselectedAnimalName = intent.getStringExtra("PRESELECTED_ANIMAL_NAME")

        setupUI()
        cargarAnimales()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Nueva Incidencia"

        // Configurar fecha por defecto (hoy)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.editTextFechaDeteccion.setText(dateFormat.format(Date()))

        // Configurar DatePicker
        binding.editTextFechaDeteccion.setOnClickListener {
            showDatePicker()
        }

        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarIncidencia()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun cargarAnimales() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()
                if (response.isSuccessful && response.body() != null) {
                    animalesList.clear()
                    animalesList.addAll(response.body()!!)
                    setupSpinnerAnimales()
                } else {
                    Toast.makeText(this@AddIncidentActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddIncidentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSpinnerAnimales() {
        val animalNames = mutableListOf("Seleccionar animal")
        animalNames.addAll(animalesList.map { "${it.chapeta} - ${it.nombre ?: "Sin nombre"}" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animalNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimal.adapter = adapter

        // Preseleccionar animal si viene desde detalles
        if (preselectedAnimalId != null) {
            val index = animalesList.indexOfFirst { it.id == preselectedAnimalId } + 1
            if (index > 0) {
                binding.spinnerAnimal.setSelection(index)
                selectedAnimalId = preselectedAnimalId
            }
        }

        binding.spinnerAnimal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAnimalId = if (position > 0) {
                    animalesList[position - 1].id
                } else {
                    null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedAnimalId = null
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.editTextFechaDeteccion.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Establecer fecha máxima como hoy
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validarCampos(): Boolean {
        if (selectedAnimalId == null) {
            Toast.makeText(this, "Selecciona un animal", Toast.LENGTH_SHORT).show()
            return false
        }

        val tipo = binding.spinnerTipo.selectedItem.toString()
        if (tipo == "Seleccionar tipo") {
            Toast.makeText(this, "Selecciona el tipo de incidencia", Toast.LENGTH_SHORT).show()
            return false
        }

        val descripcion = binding.editTextDescripcion.text.toString().trim()
        if (descripcion.isEmpty()) {
            binding.editTextDescripcion.error = "La descripción es obligatoria"
            binding.editTextDescripcion.requestFocus()
            return false
        }

        val fecha = binding.editTextFechaDeteccion.text.toString().trim()
        if (fecha.isEmpty()) {
            binding.editTextFechaDeteccion.error = "La fecha es obligatoria"
            binding.editTextFechaDeteccion.requestFocus()
            return false
        }

        return true
    }

    private fun guardarIncidencia() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val nuevaIncidencia = Incidencia(
            animal = selectedAnimalId!!,
            tipo = binding.spinnerTipo.selectedItem.toString(),
            descripcion = binding.editTextDescripcion.text.toString().trim(),
            fecha_deteccion = binding.editTextFechaDeteccion.text.toString().trim(),
            estado = binding.spinnerEstado.selectedItem.toString().lowercase(),
            fecha_resolucion = null // Se establecerá cuando se resuelva
        )

        lifecycleScope.launch {
            try {
                val response = apiService.crearIncidencia(nuevaIncidencia)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddIncidentActivity, "Incidencia registrada correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@AddIncidentActivity, "Error al registrar incidencia: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddIncidentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Registrar Incidencia"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}