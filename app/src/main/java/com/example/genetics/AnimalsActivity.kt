package com.example.genetics

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityAnimalsBinding
import kotlinx.coroutines.launch

class AnimalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnimalsBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var animalsAdapter: AnimalsAdapter
    private val animalsList = mutableListOf<Animals>()

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

        // Configurar RecyclerView
        animalsAdapter = AnimalsAdapter(animalsList) { animal ->
            // Click en animal - Ver detalles
            Toast.makeText(this, "Ver detalles de ${animal.chapeta}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewAnimals.apply {
            layoutManager = LinearLayoutManager(this@AnimalsActivity)
            adapter = animalsAdapter
        }

        // Configurar FAB
        binding.fabAddAnimal.setOnClickListener {
            // Agregar nuevo animal
            Toast.makeText(this, "Agregar nuevo animal - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Configurar refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadAnimals()
        }
    }

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}