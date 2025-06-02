package com.example.genetics.Activitys

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.genetics.api.Adapters.UserAnimalsAdapter
import com.example.genetics.api.Animals
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityUserAnimalsBinding
import kotlinx.coroutines.launch

class UserAnimalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserAnimalsBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var animalsAdapter: UserAnimalsAdapter
    private val animalsList = mutableListOf<Animals>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserAnimalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadAnimals()
    }

    private fun setupUI() {
        // Configurar toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🐄 Animales (Solo Lectura)"

        // ❌ NO HAY FAB - Los usuarios no pueden añadir animales
        // El FAB está oculto en el layout

        // Configurar RecyclerView con adapter de solo lectura
        animalsAdapter = UserAnimalsAdapter(
            animalsList,
            onItemClick = { animal ->
                // Solo navegar a detalles de solo lectura
                val intent = Intent(this, UserAnimalDetailActivity::class.java)
                intent.putExtra("ANIMAL_ID", animal.id)
                startActivity(intent)
            }
        )

        binding.recyclerViewAnimals.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@UserAnimalsActivity)
            adapter = animalsAdapter
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
                    Toast.makeText(this@UserAnimalsActivity, "Error cargando animales", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@UserAnimalsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
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