package com.example.genetics

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Animals
import com.example.genetics.api.Evento
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAddEventBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEventBinding
    private val apiService = RetrofitClient.getApiService()
    private val animalesList = mutableListOf<Animals>()
    private var selectedAnimalId: Int? = null
    private var selectedDate: String? = null
    private var duplicateEventId: Int? = null

    // Calendarios para fecha y hora
    private val calendarInicio = Calendar.getInstance()
    private val calendarFin = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del intent
        selectedDate = intent.getStringExtra("SELECTED_DATE")
        duplicateEventId = intent.getIntExtra("DUPLICATE_EVENT_ID", -1).takeIf { it != -1 }

        setupUI()
        cargarAnimales()

        // Si hay una fecha seleccionada, configurarla
        selectedDate?.let { configurarFechaInicial(it) }

        // Si es duplicación, cargar datos del evento
        duplicateEventId?.let { cargarEventoParaDuplicar(it) }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (duplicateEventId != null) "Duplicar Evento" else "Nuevo Evento"

        // Configurar listeners de fecha y hora
        binding.editTextFechaInicio.setOnClickListener { mostrarDatePicker(true) }
        binding.editTextHoraInicio.setOnClickListener { mostrarTimePicker(true) }
        binding.editTextFechaFin.setOnClickListener { mostrarDatePicker(false) }
        binding.editTextHoraFin.setOnClickListener { mostrarTimePicker(false) }

        // Configurar botones
        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarEvento()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        // Configurar fecha y hora por defecto
        val ahora = Calendar.getInstance()

        // Si no hay fecha seleccionada, usar hoy
        if (selectedDate == null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(ahora.time)
        }

        // Configurar hora por defecto (próxima hora)
        ahora.add(Calendar.HOUR_OF_DAY, 1)
        ahora.set(Calendar.MINUTE, 0)
        calendarInicio.time = ahora.time

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.editTextHoraInicio.setText(timeFormat.format(ahora.time))
    }

    private fun configurarFechaInicial(fecha: String) {
        binding.editTextFechaInicio.setText(formatearFechaParaMostrar(fecha))

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(fecha)
            date?.let {
                calendarInicio.time = it
                calendarFin.time = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    Toast.makeText(this@AddEventActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEventActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun cargarEventoParaDuplicar(eventoId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.getEventos()
                if (response.isSuccessful && response.body() != null) {
                    val eventos = response.body()!!
                    val evento = eventos.find { it.id == eventoId }

                    evento?.let { duplicarDatosEvento(it) }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEventActivity, "Error cargando evento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun duplicarDatosEvento(evento: Evento) {
        binding.editTextTitulo.setText("${evento.titulo} (Copia)")
        binding.editTextDescripcion.setText(evento.descripcion)
        binding.switchRecurrente.isChecked = evento.recurrente

        // Configurar tipo de evento
        val tipoArray = resources.getStringArray(R.array.tipo_evento_options)
        val tipoIndex = tipoArray.indexOfFirst { it.contains(evento.tipo, ignoreCase = true) }
        if (tipoIndex >= 0) {
            binding.spinnerTipoEvento.setSelection(tipoIndex)
        }

        // Configurar animal si existe
        if (evento.animal != null) {
            val animalIndex = animalesList.indexOfFirst { it.id == evento.animal } + 1
            if (animalIndex > 0) {
                binding.spinnerAnimal.setSelection(animalIndex)
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

    private fun guardarEvento() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val fechaInicio = construirFechaHora(true)
        val fechaFin = construirFechaHora(false)

        val nuevoEvento = Evento(
            titulo = binding.editTextTitulo.text.toString().trim(),
            descripcion = binding.editTextDescripcion.text.toString().trim().ifEmpty { null },
            fecha_inicio = fechaInicio,
            fecha_fin = fechaFin,
            animal = selectedAnimalId,
            tipo = binding.spinnerTipoEvento.selectedItem.toString(),
            recurrente = binding.switchRecurrente.isChecked
        )

        lifecycleScope.launch {
            try {
                val response = apiService.crearEvento(nuevoEvento)

                if (response.isSuccessful) {
                    Toast.makeText(this@AddEventActivity, "Evento creado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@AddEventActivity, "Error al crear evento: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEventActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Crear Evento"
            }
        }
    }

    private fun construirFechaHora(esInicio: Boolean): String {
        val calendar = if (esInicio) calendarInicio else calendarFin
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
                dateFormat.format(calendar.time)
            }
        } catch (e: Exception) {
            // Fallback a formato básico
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            dateFormat.format(Date())
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
