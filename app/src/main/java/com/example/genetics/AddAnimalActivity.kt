package com.example.genetics

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAddAnimalBinding
import kotlinx.coroutines.launch

class AddAnimalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAnimalBinding
    private val apiService = RetrofitClient.getApiService()

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

        binding.buttonSave.setOnClickListener {
            if (validarCampos()) {
                guardarAnimal()
            }
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun validarCampos(): Boolean {
        val chapeta = binding.editTextChapeta.text.toString().trim()
        val sexo = binding.spinnerSexo.selectedItem.toString()
        val fechaNacimiento = binding.editTextFechaNacimiento.text.toString().trim()
        val raza = binding.editTextRaza.text.toString().trim()

        if (chapeta.isEmpty()) {
            binding.editTextChapeta.error = "La chapeta es obligatoria"
            return false
        }

        if (sexo == "Seleccionar sexo") {
            Toast.makeText(this, "Selecciona el sexo del animal", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fechaNacimiento.isEmpty()) {
            binding.editTextFechaNacimiento.error = "La fecha de nacimiento es obligatoria"
            return false
        }

        if (raza.isEmpty()) {
            binding.editTextRaza.error = "La raza es obligatoria"
            return false
        }

        return true
    }

    private fun guardarAnimal() {
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = "Guardando..."

        val nuevoAnimal = Animals(
            chapeta = binding.editTextChapeta.text.toString().trim(),
            nombre = binding.editTextNombre.text.toString().trim().ifEmpty { null },
            sexo = binding.spinnerSexo.selectedItem.toString().lowercase(),
            fecha_nacimiento = binding.editTextFechaNacimiento.text.toString().trim(),
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
                    finish()
                } else {
                    Toast.makeText(this@AddAnimalActivity, "Error al crear el animal", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddAnimalActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.buttonSave.isEnabled = true
                binding.buttonSave.text = "Guardar"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}