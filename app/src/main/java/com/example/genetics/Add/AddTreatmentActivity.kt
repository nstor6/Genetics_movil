package com.example.genetics.Add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.Tratamiento
import com.example.genetics.databinding.ActivityAddTreatmentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTreatmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTreatmentBinding
    private val apiService = RetrofitClient.getApiService()
    private val calendar = Calendar.getInstance()
    private val animalesList = mutableListOf<Animals>()
    private var selectedAnimalId: Int? = null
    private var preselectedAnimalId: Int? = null
    private var preselectedAnimalName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTreatmentBinding.inflate(layoutInflater)
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
        supportActionBar?.title = "Nuevo Tratamiento"

        // Configurar fecha por defecto (hoy)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.editTextFecha.setText(dateFormat.format(Date()))

        // Configurar DatePicker
        binding.editTextFecha.setOnClickListener {
            showDatePicker()
        }

        // Configurar botones
        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarTratamiento()
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
                    Toast.makeText(this@AddTreatmentActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddTreatmentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSpinnerAnimales() {
        val animalNames = mutableListOf("Seleccionar animal")
        animalNames.addAll(animalesList.map {
            "${it.chapeta} - ${it.nombre ?: "Sin nombre"} (${it.raza})"
        })

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
                binding.editTextFecha.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Permitir fechas pasadas y futuras (para tratamientos programados)
        datePickerDialog.show()
    }

    private fun validarCampos(): Boolean {
        // Validar animal seleccionado
        if (selectedAnimalId == null) {
            Toast.makeText(this, "Selecciona un animal", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar medicamento
        val medicamento = binding.editTextMedicamento.text.toString().trim()
        if (medicamento.isEmpty()) {
            binding.editTextMedicamento.error = "El medicamento es obligatorio"
            binding.editTextMedicamento.requestFocus()
            return false
        }

        // Validar dosis
        val dosis = binding.editTextDosis.text.toString().trim()
        if (dosis.isEmpty()) {
            binding.editTextDosis.error = "La dosis es obligatoria"
            binding.editTextDosis.requestFocus()
            return false
        }

        // Validar duración
        val duracion = binding.editTextDuracion.text.toString().trim()
        if (duracion.isEmpty()) {
            binding.editTextDuracion.error = "La duración es obligatoria"
            binding.editTextDuracion.requestFocus()
            return false
        }

        // Validar fecha
        val fecha = binding.editTextFecha.text.toString().trim()
        if (fecha.isEmpty()) {
            binding.editTextFecha.error = "La fecha es obligatoria"
            binding.editTextFecha.requestFocus()
            return false
        }

        return true
    }

    private fun guardarTratamiento() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val nuevoTratamiento = Tratamiento(
            animal = selectedAnimalId!!,
            fecha = binding.editTextFecha.text.toString().trim(),
            medicamento = binding.editTextMedicamento.text.toString().trim(),
            dosis = binding.editTextDosis.text.toString().trim(),
            duracion = binding.editTextDuracion.text.toString().trim(),
            administrado_por = null, // El backend lo asignará automáticamente
            observaciones = binding.editTextObservaciones.text.toString().trim().ifEmpty { null }
        )

        lifecycleScope.launch {
            try {
                val response = apiService.crearTratamiento(nuevoTratamiento)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddTreatmentActivity, "Tratamiento registrado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@AddTreatmentActivity, "Error al registrar tratamiento: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddTreatmentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Registrar Tratamiento"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}