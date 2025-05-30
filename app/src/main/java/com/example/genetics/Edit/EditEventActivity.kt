package com.example.genetics.Edit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.R
import com.example.genetics.api.Animals
import com.example.genetics.api.Evento
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityEditEventBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditEventBinding
    private val apiService = RetrofitClient.getApiService()
    private val animalesList = mutableListOf<Animals>()
    private var selectedAnimalId: Int? = null
    private var eventoId: Int = -1
    private var eventoOriginal: Evento? = null

    // Calendarios para fecha y hora
    private val calendarInicio = Calendar.getInstance()
    private val calendarFin = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener ID del evento desde el intent
        eventoId = intent.getIntExtra("EVENT_ID", -1)
        if (eventoId == -1) {
            Toast.makeText(this, "Error: Evento no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarAnimales()
        cargarDatosEvento()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✏️ Editar Evento"

        // Configurar listeners de fecha y hora
        binding.editTextFechaInicio.setOnClickListener { mostrarDatePicker(true) }
        binding.editTextHoraInicio.setOnClickListener { mostrarTimePicker(true) }
        binding.editTextFechaFin.setOnClickListener { mostrarDatePicker(false) }
        binding.editTextHoraFin.setOnClickListener { mostrarTimePicker(false) }

        // Configurar botones
        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarCambios()
            }
        }

        binding.buttonCancel.setOnClickListener {
            confirmarSalida()
        }

        binding.buttonDelete.setOnClickListener {
            confirmarEliminarEvento()
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
                    Toast.makeText(this@EditEventActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditEventActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupSpinnerAnimales() {
        val animalNames = mutableListOf("Sin animal específico")
        animalNames.addAll(animalesList.map {
            "${it.chapeta} - ${it.nombre ?: "Sin nombre"} (${it.raza})"
        })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, animalNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAnimal.adapter = adapter

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

    private fun cargarDatosEvento() {
        lifecycleScope.launch {
            try {
                val response = apiService.getEventos()
                if (response.isSuccessful && response.body() != null) {
                    val eventos = response.body()!!
                    val evento = eventos.find { it.id == eventoId }

                    if (evento != null) {
                        eventoOriginal = evento
                        llenarFormulario(evento)
                    } else {
                        Toast.makeText(this@EditEventActivity, "Evento no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditEventActivity, "Error cargando evento: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun llenarFormulario(evento: Evento) {
        // Título y descripción
        binding.editTextTitulo.setText(evento.titulo)
        binding.editTextDescripcion.setText(evento.descripcion)

        // Recurrente
        binding.switchRecurrente.isChecked = evento.recurrente

        // Configurar tipo de evento
        val tipoArray = resources.getStringArray(R.array.tipo_evento_options)
        val tipoIndex = tipoArray.indexOfFirst { opcion ->
            // Buscar por el valor sin emoji
            val valorSinEmoji = opcion.replace(Regex("^[🩺💊🍼🥛🏥⚖️🚚🔍📊🌾✂️📋]\\s*"), "")
            valorSinEmoji.equals(evento.tipo, ignoreCase = true) ||
                    evento.tipo.contains(valorSinEmoji, ignoreCase = true)
        }
        if (tipoIndex >= 0) {
            binding.spinnerTipoEvento.setSelection(tipoIndex)
        }

        // Configurar animal si existe
        if (evento.animal != null) {
            val animalIndex = animalesList.indexOfFirst { it.id == evento.animal } + 1
            if (animalIndex > 0) {
                binding.spinnerAnimal.setSelection(animalIndex)
                selectedAnimalId = evento.animal
            }
        }

        // Configurar fechas y horas
        configurarFechaHora(evento.fecha_inicio, true)
        if (evento.fecha_fin != null) {
            configurarFechaHora(evento.fecha_fin!!, false)
        }
    }

    private fun configurarFechaHora(fechaHora: String, esInicio: Boolean) {
        try {
            val calendar = if (esInicio) calendarInicio else calendarFin

            // Intentar parsear con hora
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(fechaHora)

            if (date != null) {
                calendar.time = date

                // Configurar fecha
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaFormateada = dateFormat.format(date)

                // Configurar hora
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val horaFormateada = timeFormat.format(date)

                if (esInicio) {
                    binding.editTextFechaInicio.setText(fechaFormateada)
                    binding.editTextHoraInicio.setText(horaFormateada)
                } else {
                    binding.editTextFechaFin.setText(fechaFormateada)
                    binding.editTextHoraFin.setText(horaFormateada)
                }
            }
        } catch (e: Exception) {
            // Si falla, intentar solo con fecha
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputFormat.parse(fechaHora.substring(0, 10))

                if (date != null) {
                    val calendar = if (esInicio) calendarInicio else calendarFin
                    calendar.time = date

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaFormateada = dateFormat.format(date)

                    if (esInicio) {
                        binding.editTextFechaInicio.setText(fechaFormateada)
                        binding.editTextHoraInicio.setText("09:00")
                    } else {
                        binding.editTextFechaFin.setText(fechaFormateada)
                        binding.editTextHoraFin.setText("10:00")
                    }
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun mostrarDatePicker(esInicio: Boolean) {
        val calendar = if (esInicio) calendarInicio else calendarFin

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaFormateada = dateFormat.format(calendar.time)

                if (esInicio) {
                    binding.editTextFechaInicio.setText(fechaFormateada)
                } else {
                    binding.editTextFechaFin.setText(fechaFormateada)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun mostrarTimePicker(esInicio: Boolean) {
        val calendar = if (esInicio) calendarInicio else calendarFin

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val horaFormateada = timeFormat.format(calendar.time)

                if (esInicio) {
                    binding.editTextHoraInicio.setText(horaFormateada)
                } else {
                    binding.editTextHoraFin.setText(horaFormateada)
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )

        timePickerDialog.show()
    }

    private fun validarCampos(): Boolean {
        // Validar título
        val titulo = binding.editTextTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            binding.editTextTitulo.error = "El título es obligatorio"
            binding.editTextTitulo.requestFocus()
            return false
        }

        // Validar tipo de evento
        val tipoEvento = binding.spinnerTipoEvento.selectedItem.toString()
        if (tipoEvento == "Seleccionar tipo") {
            Toast.makeText(this, "Selecciona el tipo de evento", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar fecha de inicio
        val fechaInicio = binding.editTextFechaInicio.text.toString().trim()
        if (fechaInicio.isEmpty()) {
            binding.editTextFechaInicio.error = "La fecha de inicio es obligatoria"
            binding.editTextFechaInicio.requestFocus()
            return false
        }

        return true
    }

    private fun guardarCambios() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val fechaInicio = construirFechaHora(true)
        val fechaFin = construirFechaHora(false)

        // Obtener el tipo correcto para el backend
        val tipoOptions = resources.getStringArray(R.array.tipo_evento_options)
        val tipoValues = resources.getStringArray(R.array.tipo_evento_values)
        val selectedIndex = binding.spinnerTipoEvento.selectedItemPosition
        val tipoParaBackend = if (selectedIndex > 0 && selectedIndex < tipoValues.size) {
            tipoValues[selectedIndex]
        } else {
            "otro"
        }

        val eventoActualizado = Evento(
            id = eventoId,
            titulo = binding.editTextTitulo.text.toString().trim(),
            descripcion = binding.editTextDescripcion.text.toString().trim().ifEmpty { null },
            fecha_inicio = fechaInicio,
            fecha_fin = fechaFin,
            animal = selectedAnimalId,
            tipo = tipoParaBackend,
            recurrente = binding.switchRecurrente.isChecked
        )

        lifecycleScope.launch {
            try {
                val response = apiService.actualizarEvento(eventoId, eventoActualizado)

                if (response.isSuccessful) {
                    Toast.makeText(this@EditEventActivity, "Evento actualizado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditEventActivity, "Error al actualizar evento: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditEventActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "💾 Guardar Cambios"
            }
        }
    }

    private fun construirFechaHora(esInicio: Boolean): String {
        val fecha = if (esInicio) {
            binding.editTextFechaInicio.text.toString()
        } else {
            binding.editTextFechaFin.text.toString()
        }
        val hora = if (esInicio) {
            binding.editTextHoraInicio.text.toString()
        } else {
            binding.editTextHoraFin.text.toString()
        }

        return try {
            if (fecha.isNotEmpty()) {
                // Convertir fecha del formato dd/MM/yyyy a yyyy-MM-dd
                val inputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val outputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = inputDateFormat.parse(fecha)
                val fechaISO = outputDateFormat.format(date ?: Date())

                if (hora.isNotEmpty()) {
                    "${fechaISO}T${hora}:00"
                } else {
                    "${fechaISO}T09:00:00"
                }
            } else {
                // Fecha actual por defecto
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                dateFormat.format(Date())
            }
        } catch (e: Exception) {
            // Fallback a formato básico
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            dateFormat.format(Date())
        }
    }

    private fun confirmarEliminarEvento() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar Evento")
            .setMessage("¿Estás seguro de que quieres eliminar el evento '${eventoOriginal?.titulo}'?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarEvento()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEvento() {
        lifecycleScope.launch {
            try {
                val response = apiService.eliminarEvento(eventoId)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditEventActivity, "Evento eliminado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@EditEventActivity, "Error al eliminar evento", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditEventActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        val original = eventoOriginal ?: return false

        return binding.editTextTitulo.text.toString().trim() != original.titulo ||
                binding.editTextDescripcion.text.toString().trim() != (original.descripcion ?: "") ||
                binding.switchRecurrente.isChecked != original.recurrente ||
                selectedAnimalId != original.animal
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