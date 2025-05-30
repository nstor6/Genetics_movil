package com.example.genetics.Edit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.R
import com.example.genetics.api.Animals
import com.example.genetics.api.Incidencia
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityEditIncidentBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditIncidentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditIncidentBinding
    private val apiService = RetrofitClient.getApiService()
    private var incidenciaId: Int = -1
    private var currentIncidencia: Incidencia? = null
    private val animalesList = mutableListOf<Animals>()
    private var selectedAnimalId: Int? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditIncidentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener ID de la incidencia a editar
        incidenciaId = intent.getIntExtra("INCIDENCIA_ID", -1)
        if (incidenciaId == -1) {
            Toast.makeText(this, "Error: Incidencia no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarAnimales()
        cargarDatosIncidencia()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✏️ Editar Incidencia"

        // Configurar DatePickers
        binding.editTextFechaDeteccion.setOnClickListener {
            mostrarDatePicker(true)
        }

        binding.editTextFechaResolucion.setOnClickListener {
            mostrarDatePicker(false)
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

        // Mostrar/ocultar fecha de resolución según el estado
        binding.spinnerEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val estado = binding.spinnerEstado.selectedItem.toString().lowercase()
                if (estado == "resuelto") {
                    binding.layoutFechaResolucion.visibility = View.VISIBLE
                    if (binding.editTextFechaResolucion.text.toString().isEmpty()) {
                        // Auto-completar con fecha actual
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        binding.editTextFechaResolucion.setText(dateFormat.format(Date()))
                    }
                } else {
                    binding.layoutFechaResolucion.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
                    Toast.makeText(this@EditIncidentActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditIncidentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun cargarDatosIncidencia() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = apiService.getIncidencias()
                if (response.isSuccessful && response.body() != null) {
                    val incidencias = response.body()!!
                    currentIncidencia = incidencias.find { it.id == incidenciaId }

                    if (currentIncidencia != null) {
                        llenarFormulario(currentIncidencia!!)
                    } else {
                        Toast.makeText(this@EditIncidentActivity, "Incidencia no encontrada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@EditIncidentActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditIncidentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun llenarFormulario(incidencia: Incidencia) {
        // Seleccionar animal
        val animalIndex = animalesList.indexOfFirst { it.id == incidencia.animal }
        if (animalIndex >= 0) {
            binding.spinnerAnimal.setSelection(animalIndex)
            selectedAnimalId = incidencia.animal
        }

        // Tipo de incidencia
        val tipoOptions = resources.getStringArray(R.array.tipo_incidencia_options)
        val tipoIndex = tipoOptions.indexOfFirst { it.equals(incidencia.tipo, ignoreCase = true) }
        if (tipoIndex >= 0) {
            binding.spinnerTipo.setSelection(tipoIndex)
        }

        // Descripción
        binding.editTextDescripcion.setText(incidencia.descripcion)

        // Fecha de detección
        binding.editTextFechaDeteccion.setText(formatearFechaParaMostrar(incidencia.fecha_deteccion))

        // Estado
        val estadoOptions = resources.getStringArray(R.array.estado_incidencia_options)
        val estadoIndex = estadoOptions.indexOfFirst { it.equals(incidencia.estado, ignoreCase = true) }
        if (estadoIndex >= 0) {
            binding.spinnerEstado.setSelection(estadoIndex)
        }

        // Fecha de resolución (si existe)
        if (incidencia.fecha_resolucion != null) {
            binding.editTextFechaResolucion.setText(formatearFechaParaMostrar(incidencia.fecha_resolucion!!))
            binding.layoutFechaResolucion.visibility = View.VISIBLE
        }
    }

    private fun mostrarDatePicker(esFechaDeteccion: Boolean) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaFormateada = dateFormat.format(calendar.time)

                if (esFechaDeteccion) {
                    binding.editTextFechaDeteccion.setText(fechaFormateada)
                } else {
                    binding.editTextFechaResolucion.setText(fechaFormateada)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Para fecha de detección, máximo hoy
        if (esFechaDeteccion) {
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        }

        datePickerDialog.show()
    }

    private fun validarCampos(): Boolean {
        // Validar animal seleccionado
        if (selectedAnimalId == null) {
            Toast.makeText(this, "Selecciona un animal", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar tipo
        val tipo = binding.spinnerTipo.selectedItem.toString()
        if (tipo == "Seleccionar tipo") {
            Toast.makeText(this, "Selecciona el tipo de incidencia", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar descripción
        val descripcion = binding.editTextDescripcion.text.toString().trim()
        if (descripcion.isEmpty()) {
            binding.editTextDescripcion.error = "La descripción es obligatoria"
            binding.editTextDescripcion.requestFocus()
            return false
        }

        // Validar fecha de detección
        val fechaDeteccion = binding.editTextFechaDeteccion.text.toString().trim()
        if (fechaDeteccion.isEmpty()) {
            binding.editTextFechaDeteccion.error = "La fecha de detección es obligatoria"
            binding.editTextFechaDeteccion.requestFocus()
            return false
        }

        // Validar fecha de resolución si el estado es "resuelto"
        val estado = binding.spinnerEstado.selectedItem.toString().lowercase()
        if (estado == "resuelto") {
            val fechaResolucion = binding.editTextFechaResolucion.text.toString().trim()
            if (fechaResolucion.isEmpty()) {
                binding.editTextFechaResolucion.error = "La fecha de resolución es obligatoria para incidencias resueltas"
                binding.editTextFechaResolucion.requestFocus()
                return false
            }
        }

        return true
    }

    private fun guardarCambios() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val estado = binding.spinnerEstado.selectedItem.toString().lowercase()
        val fechaResolucion = if (estado == "resuelto") {
            convertirFechaParaAPI(binding.editTextFechaResolucion.text.toString())
        } else {
            null
        }

        val incidenciaActualizada = Incidencia(
            id = incidenciaId,
            animal = selectedAnimalId!!,
            tipo = binding.spinnerTipo.selectedItem.toString(),
            descripcion = binding.editTextDescripcion.text.toString().trim(),
            fecha_deteccion = convertirFechaParaAPI(binding.editTextFechaDeteccion.text.toString()),
            estado = estado,
            fecha_resolucion = fechaResolucion
        )

        lifecycleScope.launch {
            try {
                val response = apiService.actualizarIncidencia(incidenciaId, incidenciaActualizada)

                if (response.isSuccessful) {
                    Toast.makeText(this@EditIncidentActivity, "Incidencia actualizada correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditIncidentActivity, "Error al actualizar: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditIncidentActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
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
        if (currentIncidencia == null) return false

        val estado = binding.spinnerEstado.selectedItem.toString().lowercase()
        val fechaResolucionActual = if (estado == "resuelto") {
            convertirFechaParaAPI(binding.editTextFechaResolucion.text.toString())
        } else {
            null
        }

        return selectedAnimalId != currentIncidencia?.animal ||
                binding.spinnerTipo.selectedItem.toString() != currentIncidencia?.tipo ||
                binding.editTextDescripcion.text.toString().trim() != currentIncidencia?.descripcion ||
                convertirFechaParaAPI(binding.editTextFechaDeteccion.text.toString()) != currentIncidencia?.fecha_deteccion ||
                estado != currentIncidencia?.estado ||
                fechaResolucionActual != currentIncidencia?.fecha_resolucion
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