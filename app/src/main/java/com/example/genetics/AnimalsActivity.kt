package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.Add.AddAnimalActivity
import com.example.genetics.api.Adapters.AnimalsAdapter
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAnimalsBinding
import kotlinx.coroutines.launch

class AnimalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimalsBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var animalsAdapter: AnimalsAdapter
    private val animalsList = mutableListOf<Animals>()

    companion object {
        private const val REQUEST_ADD_ANIMAL = 1001
        private const val REQUEST_EDIT_ANIMAL = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadAnimals()
    }

    private fun setupUI() {
        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🐄 Animales"

        // Configurar RecyclerView con los nuevos callbacks
        animalsAdapter = AnimalsAdapter(
            animalsList,
            onItemClick = { animal ->
                // Navegar a detalles del animal
                val intent = Intent(this, AnimalDetailActivity::class.java)
                intent.putExtra("ANIMAL_ID", animal.id)
                startActivity(intent)
            },
            onEditClick = { animal ->
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
            // Navegar a agregar nuevo animal
            val intent = Intent(this, AddAnimalActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_ANIMAL)
        }

        // Configurar refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadAnimals()
        }
    }

    // MÉTODO QUE FALTABA: loadAnimals()
    private fun loadAnimals() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getAnimales()

                if (response.isSuccessful && response.body() != null) {
                    val animals = response.body()!!

                    animalsList.clear()
                    animalsList.addAll(animals)
                    animalsAdapter.notifyDataSetChanged()

                    // Actualizar UI vacía
                    if (animals.isEmpty()) {
                        binding.textEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewAnimals.visibility = View.GONE
                    } else {
                        binding.textEmptyState.visibility = View.GONE
                        binding.recyclerViewAnimals.visibility = View.VISIBLE
                    }

                } else {
                    Toast.makeText(this@AnimalsActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@AnimalsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_ANIMAL, REQUEST_EDIT_ANIMAL -> {
                if (resultCode == RESULT_OK) {
                    // Recargar la lista cuando se agrega o edita un animal
                    loadAnimals()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar datos cuando volvemos a esta actividad
        loadAnimals()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}