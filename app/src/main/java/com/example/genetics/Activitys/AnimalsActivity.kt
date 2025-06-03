package com.example.genetics.Activitys

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Create.AddAnimalActivity
import com.example.genetics.api.Adapters.AnimalsAdapter
import com.example.genetics.api.models.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAnimalsBinding
import kotlinx.coroutines.launch

class AnimalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimalsBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var animalsAdapter: AnimalsAdapter
    private val animalsList = mutableListOf<Animals>()
    private var isLoadingData = false // ✅ AÑADIDO: Flag para evitar doble carga

    companion object {
        private const val REQUEST_ADD_ANIMAL = 1001
        private const val REQUEST_EDIT_ANIMAL = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("ANIMALS_ACTIVITY", "🚀 === INICIANDO ANIMALS ACTIVITY ===")

            binding = ActivityAnimalsBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupUI()

            // ✅ CORREGIDO: Solo cargar una vez en onCreate
            if (!isLoadingData) {
                loadAnimals()
            }

            Log.d("ANIMALS_ACTIVITY", "✅ AnimalsActivity inicializada correctamente")

        } catch (e: Exception) {
            Log.e("ANIMALS_ACTIVITY", "❌ Error crítico en onCreate: ${e.message}")
            e.printStackTrace()

            Toast.makeText(this, "Error inicializando: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupUI() {
        Log.d("ANIMALS_ACTIVITY", "🔧 Configurando UI...")

        try {
            // Configurar toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "🐄 Animales"

            // Configurar RecyclerView con los nuevos callbacks
            animalsAdapter = AnimalsAdapter(
                animalsList,
                onItemClick = { animal ->
                    Log.d("ANIMALS_ACTIVITY", "👁️ Ver detalles animal: ${animal.chapeta}")
                    // Navegar a detalles del animal
                    val intent = Intent(this, AnimalDetailActivity::class.java)
                    intent.putExtra("ANIMAL_ID", animal.id)
                    startActivity(intent)
                },
                onEditClick = { animal ->
                    Log.d("ANIMALS_ACTIVITY", "✏️ Editar animal: ${animal.chapeta}")
                    // Editar animal
                    val intent = Intent(this, EditAnimalActivity::class.java)
                    intent.putExtra("ANIMAL_ID", animal.id)
                    startActivityForResult(intent, REQUEST_EDIT_ANIMAL)
                }
            )

            binding.recyclerViewAnimals.apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@AnimalsActivity)
                adapter = animalsAdapter
            }

            // Configurar FAB
            binding.fabAddAnimal.setOnClickListener {
                Log.d("ANIMALS_ACTIVITY", "➕ Añadir nuevo animal")
                // Navegar a agregar nuevo animal
                val intent = Intent(this, AddAnimalActivity::class.java)
                startActivityForResult(intent, REQUEST_ADD_ANIMAL)
            }

            // Configurar refresh
            binding.swipeRefreshLayout.setOnRefreshListener {
                Log.d("ANIMALS_ACTIVITY", "🔄 Refresh manual")
                if (!isLoadingData) {
                    loadAnimals()
                }
            }

            Log.d("ANIMALS_ACTIVITY", "✅ UI configurada")

        } catch (e: Exception) {
            Log.e("ANIMALS_ACTIVITY", "❌ Error configurando UI: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // ✅ CORREGIDO: Método mejorado con control de carga
    private fun loadAnimals() {
        if (isLoadingData) {
            Log.w("ANIMALS_ACTIVITY", "⚠️ Ya se están cargando animales, ignorando")
            return
        }

        isLoadingData = true
        binding.swipeRefreshLayout.isRefreshing = true

        Log.d("ANIMALS_ACTIVITY", "📡 Iniciando carga de animales...")

        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()

                Log.d("ANIMALS_ACTIVITY", "📊 Response - Code: ${response.code()}, Success: ${response.isSuccessful}")

                if (response.isSuccessful && response.body() != null) {
                    val animalsResponse = response.body()!!
                    val animals = animalsResponse.results

                    Log.d("ANIMALS_ACTIVITY", "✅ Animales recibidos: ${animals.size}")

                    runOnUiThread {
                        animalsList.clear()
                        animalsList.addAll(animals)
                        animalsAdapter.notifyDataSetChanged()
                        updateEmptyState(animals.isEmpty())

                        Log.d("ANIMALS_ACTIVITY", "📋 UI actualizada - Items en adapter: ${animalsAdapter.itemCount}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ANIMALS_ACTIVITY", "❌ Error del servidor: $errorBody")

                    runOnUiThread {
                        Toast.makeText(this@AnimalsActivity, "Error cargando animales: ${response.code()}", Toast.LENGTH_SHORT).show()
                        updateEmptyState(true)
                    }
                }

            } catch (e: java.net.UnknownHostException) {
                Log.e("ANIMALS_ACTIVITY", "🌐 Error: Servidor no encontrado")
                runOnUiThread {
                    Toast.makeText(this@AnimalsActivity, "Servidor no encontrado", Toast.LENGTH_LONG).show()
                    updateEmptyState(true)
                }

            } catch (e: java.net.ConnectException) {
                Log.e("ANIMALS_ACTIVITY", "🔗 Error: Conexión rechazada")
                runOnUiThread {
                    Toast.makeText(this@AnimalsActivity, "No se puede conectar al servidor", Toast.LENGTH_LONG).show()
                    updateEmptyState(true)
                }

            } catch (e: Exception) {
                Log.e("ANIMALS_ACTIVITY", "💥 Error inesperado: ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@AnimalsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    updateEmptyState(true)
                }

            } finally {
                isLoadingData = false
                runOnUiThread {
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                Log.d("ANIMALS_ACTIVITY", "🏁 Carga de animales completada")
            }
        }
    }


    // ✅ NUEVO: Método para actualizar estado vacío
    private fun updateEmptyState(isEmpty: Boolean) {
        try {
            if (isEmpty) {
                binding.textEmptyState.visibility = View.VISIBLE
                binding.recyclerViewAnimals.visibility = View.GONE
                Log.d("ANIMALS_ACTIVITY", "👻 Mostrando estado vacío")
            } else {
                binding.textEmptyState.visibility = View.GONE
                binding.recyclerViewAnimals.visibility = View.VISIBLE
                Log.d("ANIMALS_ACTIVITY", "📋 Mostrando lista de animales")
            }
        } catch (e: Exception) {
            Log.e("ANIMALS_ACTIVITY", "❌ Error actualizando estado vacío: ${e.message}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            when (requestCode) {
                REQUEST_ADD_ANIMAL, REQUEST_EDIT_ANIMAL -> {
                    if (resultCode == RESULT_OK) {
                        Log.d("ANIMALS_ACTIVITY", "🔄 Recargando después de añadir/editar animal")
                        // Recargar la lista cuando se agrega o edita un animal
                        if (!isLoadingData) {
                            loadAnimals()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ANIMALS_ACTIVITY", "❌ Error en onActivityResult: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()

        try {
            Log.d("ANIMALS_ACTIVITY", "🔄 onResume - Verificando si recargar datos")

            // Solo recargar si no hay datos o si no se está cargando ya
            if (animalsList.isEmpty() && !isLoadingData) {
                Log.d("ANIMALS_ACTIVITY", "📡 Lista vacía en onResume - Recargando")
                loadAnimals()
            } else {
                Log.d("ANIMALS_ACTIVITY", "ℹ️ onResume - No es necesario recargar (${animalsList.size} items)")
            }

        } catch (e: Exception) {
            Log.e("ANIMALS_ACTIVITY", "❌ Error en onResume: ${e.message}")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d("ANIMALS_ACTIVITY", "🔙 Navegación hacia atrás")
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        Log.d("ANIMALS_ACTIVITY", "🔙 Back presionado")
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ANIMALS_ACTIVITY", "🗑️ AnimalsActivity destruida")
        isLoadingData = false
    }
}