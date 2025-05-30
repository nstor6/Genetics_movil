package com.example.genetics.Edit

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
import com.example.genetics.databinding.ActivityEditTreatmentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditTreatmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTreatmentBinding
    private val apiService = RetrofitClient.getApiService()
    private var tratamientoId: Int = -1
    private var currentTratamiento: Tratamiento? = null
    private val animalesList = mutableListOf<Animals>()
    private var selectedAnimalId: Int? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTreatmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener ID del tratamiento a editar
        tratamientoId = intent.getIntExtra("TRATAMIENTO_ID", -1)
        if (tratamientoId == -1) {
            Toast.makeText(this, "Error: Tratamiento no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarAnimales()
        cargarDatosTratamiento()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✏️ Editar Tratamiento"

        // Configurar DatePicker
        binding.editTextFecha.setOnClickListener {
            mostrarDatePicker()
        }

        // Configurar botones
        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarCambios()
            }
        }

        binding.buttonCancel.setOnClickListener {
            confirmarSalida()
        }
    }

    private fun cargarAnimales() {
        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()
                if (response.isSuccessful && response.body() != null) {
                    animalesList.clear()
                    animalesList.addAll(response.body()!!)
                    setupSpinnerAnimales()
                } else {
                    Toast.makeText(this@EditTreatmentActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditTreatmentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinnerAnimales() {
        val animalNames = mutableListOf<String>()
        animalNames.addAll(animalesList.map { "${it.chapeta} - ${it.nombre ?: "Sin nombre"}" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animalNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimal.adapter = adapter

        binding.spinnerAnimal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAnimalId = if (position >= 0 && position < animalesList.size) {
                    animalesList[position].id
                } else {
                    null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedAnimalId = null
            }
        }
    }

    private fun cargarDatosTratamiento() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = apiService.getTratamientos()
                if (response.isSuccessful && response.body() != null) {
                    val tratamientos = response.body()!!
                    currentTratamiento = tratamientos.find { it.id == tratamientoId }

                    if (currentTratamiento != null) {
                        llenarFormulario(currentTratamiento!!)
                    } else {
                        Toast.makeText(this@EditTreatmentActivity, "Tratamiento no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@EditTreatmentActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditTreatmentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun llenarFormulario(tratamiento: Tratamiento) {
        // Seleccionar animal
        val animalIndex = animalesList.indexOfFirst { it.id == tratamiento.animal }
        if (animalIndex >= 0) {
            binding.spinnerAnimal.setSelection(animalIndex)
            selectedAnimalId = tratamiento.animal
        }

        // Medicamento
        binding.editTextMedicamento.setText(tratamiento.medicamento)

        // Dosis
        binding.editTextDosis.setText(tratamiento.dosis)

        // Duración
        binding.editTextDuracion.setText(tratamiento.duracion)

        // Fecha
        binding.editTextFecha.setText(formatearFechaParaMostrar(tratamiento.fecha))

        // Observaciones
        binding.editTextObservaciones.setText(tratamiento.observaciones)
    }

    private fun mostrarDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

    private fun guardarCambios() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val tratamientoActualizado = Tratamiento(
            id = tratamientoId,
            animal = selectedAnimalId!!,
            fecha = convertirFechaParaAPI(binding.editTextFecha.text.toString()),
            medicamento = binding.editTextMedicamento.text.toString().trim(),
            dosis = binding.editTextDosis.text.toString().trim(),
            duracion = binding.editTextDuracion.text.toString().trim(),
            administrado_por = currentTratamiento?.administrado_por, // Mantener el usuario original
            observaciones = binding.editTextObservaciones.text.toString().trim().ifEmpty { null }
        )

        lifecycleScope.launch {
            try {
                val response = apiService.actualizarTratamiento(tratamientoId, tratamientoActualizado)

                if (response.isSuccessful) {
                    Toast.makeText(this@EditTreatmentActivity, "Tratamiento actualizado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditTreatmentActivity, "Error al actualizar: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditTreatmentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "💾 Guardar Cambios"
            }
        }
    }

    private fun formatearFechaParaMostrar(fecha: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(fecha)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            fecha
        }
    }

    private fun convertirFechaParaAPI(fechaMostrada: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(fechaMostrada)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            fechaMostrada
        }
    }

    private fun confirmarSalida() {
        if (hayCambiosSinGuardar()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Cambios sin guardar")
                .setMessage("Tienes cambios sin guardar. ¿Estás seguro de que quieres salir?")
                .setPositiveButton("Salir sin guardar") { _, _ ->
                    finish()
                }
                .setNegativeButton("Continuar editando", null)
                .setNeutralButton("Guardar y salir") { _, _ ->
                    if (validarCampos()) {
                        guardarCambios()
                    }
                }
                .show()
        } else {
            finish()
        }
    }

    private fun hayCambiosSinGuardar(): Boolean {
        if (currentTratamiento == null) return false

        return selectedAnimalId != currentTratamiento?.animal ||
                binding.editTextMedicamento.text.toString().trim() != currentTratamiento?.medicamento ||
                binding.editTextDosis.text.toString().trim() != currentTratamiento?.dosis ||
                binding.editTextDuracion.text.toString().trim() != currentTratamiento?.duracion ||
                convertirFechaParaAPI(binding.editTextFecha.text.toString()) != currentTratamiento?.fecha ||
                binding.editTextObservaciones.text.toString().trim() != (currentTratamiento?.observaciones ?: "")
    }

    override fun onSupportNavigateUp(): Boolean {
        confirmarSalida()
        return true
    }

    override fun onBackPressed() {
        confirmarSalida()
        super.onBackPressed()
    }
}