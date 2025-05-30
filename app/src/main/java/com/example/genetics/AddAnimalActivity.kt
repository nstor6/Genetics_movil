package com.example.genetics

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAddAnimalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddAnimalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAnimalBinding
    private val apiService = RetrofitClient.getApiService()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAnimalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Nuevo Animal"

        // CORREGIDO: Configurar DatePicker para fecha de nacimiento
        binding.editTextFechaNacimiento.setOnClickListener {
            mostrarDatePicker()
        }

        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarAnimal()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    // NUEVO: Método para mostrar el DatePicker
    private fun mostrarDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.editTextFechaNacimiento.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Establecer fecha máxima como hoy (no puede nacer en el futuro)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Establecer fecha mínima hace 20 años (límite razonable para animales)
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.YEAR, -20)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    // NUEVO: Método para convertir fecha del formato mostrado al formato de API
    private fun convertirFechaParaAPI(fechaMostrada: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(fechaMostrada)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // Si hay error, usar fecha actual
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.format(Date())
        }
    }

    private fun validarCampos(): Boolean {
        val chapeta = binding.editTextChapeta.text.toString().trim()
        val sexo = binding.spinnerSexo.selectedItem.toString()
        val fechaNacimiento = binding.editTextFechaNacimiento.text.toString().trim()
        val raza = binding.editTextRaza.text.toString().trim()

        if (chapeta.isEmpty()) {
            binding.editTextChapeta.error = "La chapeta es obligatoria"
            binding.editTextChapeta.requestFocus()
            return false
        }

        if (sexo == "Seleccionar sexo") {
            Toast.makeText(this, "Selecciona el sexo del animal", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fechaNacimiento.isEmpty()) {
            binding.editTextFechaNacimiento.error = "La fecha de nacimiento es obligatoria"
            binding.editTextFechaNacimiento.requestFocus()
            Toast.makeText(this, "Toca el campo de fecha para seleccionar", Toast.LENGTH_SHORT).show()
            return false
        }

        if (raza.isEmpty()) {
            binding.editTextRaza.error = "La raza es obligatoria"
            binding.editTextRaza.requestFocus()
            return false
        }

        return true
    }

    private fun guardarAnimal() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        // CORREGIDO: Convertir fecha al formato correcto para la API
        val fechaParaAPI = convertirFechaParaAPI(binding.editTextFechaNacimiento.text.toString().trim())

        val nuevoAnimal = Animals(
            chapeta = binding.editTextChapeta.text.toString().trim(),
            nombre = binding.editTextNombre.text.toString().trim().ifEmpty { null },
            sexo = binding.spinnerSexo.selectedItem.toString().lowercase(),
            fecha_nacimiento = fechaParaAPI, // Usar la fecha convertida
            raza = binding.editTextRaza.text.toString().trim(),
            estado_reproductivo = binding.spinnerEstadoReproductivo.selectedItem.toString().lowercase(),
            estado_productivo = binding.spinnerEstadoProductivo.selectedItem.toString().lowercase(),
            peso_actual = binding.editTextPeso.text.toString().toDoubleOrNull(),
            ubicacion_actual = binding.editTextUbicacion.text.toString().trim().ifEmpty { null },
            notas = binding.editTextNotas.text.toString().trim().ifEmpty { null }
        )

        lifecycleScope.launch {
            try {
                val response = apiService.crearAnimal(nuevoAnimal)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddAnimalActivity, "Animal creado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@AddAnimalActivity, "Error al crear el animal: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddAnimalActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Guardar Animal"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}