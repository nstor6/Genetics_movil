package com.example.genetics

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityEditAnimalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditAnimalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAnimalBinding
    private val apiService = RetrofitClient.getApiService()
    private var animalId: Int = -1
    private var currentAnimal: Animals? = null
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAnimalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener ID del animal a editar
        animalId = intent.getIntExtra("ANIMAL_ID", -1)
        if (animalId == -1) {
            Toast.makeText(this, "Error: Animal no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        cargarDatosAnimal()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "✏️ Editar Animal"

        // Configurar DatePicker para fecha de nacimiento
        binding.editTextFechaNacimiento.setOnClickListener {
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

        // Configurar botón de cambiar foto
        binding.buttonCambiarFoto.setOnClickListener {
            Toast.makeText(this, "Función de cámara - Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatosAnimal() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()
                if (response.isSuccessful && response.body() != null) {
                    val animales = response.body()!!
                    currentAnimal = animales.find { it.id == animalId }

                    if (currentAnimal != null) {
                        llenarFormulario(currentAnimal!!)
                    } else {
                        Toast.makeText(this@EditAnimalActivity, "Animal no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@EditAnimalActivity, "Error cargando datos", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditAnimalActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun llenarFormulario(animal: Animals) {
        // Información básica
        binding.editTextChapeta.setText(animal.chapeta)
        binding.editTextNombre.setText(animal.nombre)
        binding.editTextRaza.setText(animal.raza)

        // Fecha de nacimiento
        animal.fecha_nacimiento?.let { fecha ->
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(fecha)
                binding.editTextFechaNacimiento.setText(outputFormat.format(date ?: Date()))
                calendar.time = date ?: Date()
            } catch (e: Exception) {
                binding.editTextFechaNacimiento.setText(fecha)
            }
        }

        // Sexo
        val sexoOptions = resources.getStringArray(R.array.sexo_options)
        val sexoIndex = when (animal.sexo?.lowercase()) {
            "hembra" -> 1
            "macho" -> 2
            else -> 0
        }
        binding.spinnerSexo.setSelection(sexoIndex)

        // Estados
        val estadoReproductivoOptions = resources.getStringArray(R.array.estado_reproductivo_options)
        val reproIndex = estadoReproductivoOptions.indexOfFirst {
            it.equals(animal.estado_reproductivo, ignoreCase = true)
        }.takeIf { it >= 0 } ?: 0
        binding.spinnerEstadoReproductivo.setSelection(reproIndex)

        val estadoProductivoOptions = resources.getStringArray(R.array.estado_productivo_options)
        val prodIndex = estadoProductivoOptions.indexOfFirst {
            it.equals(animal.estado_productivo, ignoreCase = true)
        }.takeIf { it >= 0 } ?: 0
        binding.spinnerEstadoProductivo.setSelection(prodIndex)

        // Peso y ubicación
        animal.peso_actual?.let { peso ->
            binding.editTextPeso.setText(peso.toString())
        }
        binding.editTextUbicacion.setText(animal.ubicacion_actual)
        binding.editTextNotas.setText(animal.notas)

        // TODO: Cargar imagen si existe
        // animal.foto_perfil_url?.let { url ->
        //     Glide.with(this).load(url).into(binding.imageAnimalPreview)
        // }
    }

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

        // Establecer fecha máxima como hoy
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validarCampos(): Boolean {
        // Validar chapeta
        val chapeta = binding.editTextChapeta.text.toString().trim()
        if (chapeta.isEmpty()) {
            binding.editTextChapeta.error = "La chapeta es obligatoria"
            binding.editTextChapeta.requestFocus()
            return false
        }

        // Validar sexo
        val sexo = binding.spinnerSexo.selectedItem.toString()
        if (sexo == "Seleccionar sexo") {
            Toast.makeText(this, "Selecciona el sexo del animal", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar fecha de nacimiento
        val fechaNacimiento = binding.editTextFechaNacimiento.text.toString().trim()
        if (fechaNacimiento.isEmpty()) {
            binding.editTextFechaNacimiento.error = "La fecha de nacimiento es obligatoria"
            binding.editTextFechaNacimiento.requestFocus()
            return false
        }

        // Validar raza
        val raza = binding.editTextRaza.text.toString().trim()
        if (raza.isEmpty()) {
            binding.editTextRaza.error = "La raza es obligatoria"
            binding.editTextRaza.requestFocus()
            return false
        }

        return true
    }

    private fun guardarCambios() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        // Construir objeto con los datos actualizados
        val animalActualizado = Animals(
            id = animalId,
            chapeta = binding.editTextChapeta.text.toString().trim(),
            nombre = binding.editTextNombre.text.toString().trim().ifEmpty { null },
            sexo = binding.spinnerSexo.selectedItem.toString().lowercase(),
            fecha_nacimiento = convertirFecha(binding.editTextFechaNacimiento.text.toString()),
            raza = binding.editTextRaza.text.toString().trim(),
            estado_reproductivo = binding.spinnerEstadoReproductivo.selectedItem.toString().lowercase(),
            estado_productivo = binding.spinnerEstadoProductivo.selectedItem.toString().lowercase(),
            peso_actual = binding.editTextPeso.text.toString().toDoubleOrNull(),
            ubicacion_actual = binding.editTextUbicacion.text.toString().trim().ifEmpty { null },
            notas = binding.editTextNotas.text.toString().trim().ifEmpty { null },
            // Mantener campos existentes
            foto_perfil_url = currentAnimal?.foto_perfil_url,
            salud = currentAnimal?.salud,
            produccion = currentAnimal?.produccion,
            historial_movimientos = currentAnimal?.historial_movimientos,
            descendencia = currentAnimal?.descendencia,
            fecha_alta_sistema = currentAnimal?.fecha_alta_sistema,
            fecha_baja_sistema = currentAnimal?.fecha_baja_sistema,
            creado_por = currentAnimal?.creado_por,
            modificado_por = currentAnimal?.modificado_por
        )

        lifecycleScope.launch {
            try {
                val response = apiService.actualizarAnimal(animalId, animalActualizado)

                if (response.isSuccessful) {
                    Toast.makeText(this@EditAnimalActivity, "Animal actualizado correctamente", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditAnimalActivity, "Error al actualizar: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditAnimalActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "💾 Guardar Cambios"
            }
        }
    }

    private fun convertirFecha(fechaTexto: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(fechaTexto)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            fechaTexto
        }
    }

    private fun confirmarSalida() {
        // Verificar si hay cambios sin guardar
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
        // Comparar valores actuales con los originales
        if (currentAnimal == null) return false

        return binding.editTextChapeta.text.toString().trim() != (currentAnimal?.chapeta ?: "") ||
                binding.editTextNombre.text.toString().trim() != (currentAnimal?.nombre ?: "") ||
                binding.editTextRaza.text.toString().trim() != (currentAnimal?.raza ?: "") ||
                binding.editTextUbicacion.text.toString().trim() != (currentAnimal?.ubicacion_actual ?: "") ||
                binding.editTextNotas.text.toString().trim() != (currentAnimal?.notas ?: "")
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