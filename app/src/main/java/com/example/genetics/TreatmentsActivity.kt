package com.example.genetics

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.Tratamiento
import com.example.genetics.databinding.ActivityTreatmentsBinding
import kotlinx.coroutines.launch

class TreatmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTreatmentsBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var treatmentsAdapter: TreatmentsAdapter
    private val tratamientosList = mutableListOf<Tratamiento>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTreatmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadTreatments()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "💊 Tratamientos"

        // Configurar RecyclerView
        treatmentsAdapter = TreatmentsAdapter(tratamientosList) { tratamiento ->
            Toast.makeText(this, "Tratamiento: ${tratamiento.medicamento}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewTreatments.apply {
            layoutManager = LinearLayoutManager(this@TreatmentsActivity)
            adapter = treatmentsAdapter
        }

        // Configurar FAB
        binding.fabAddTreatment.setOnClickListener {
            Toast.makeText(this, "Agregar tratamiento - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Configurar refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadTreatments()
        }
    }

    private fun loadTreatments() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getTratamientos()

                if (response.isSuccessful && response.body() != null) {
                    val treatments = response.body()!!

                    tratamientosList.clear()
                    tratamientosList.addAll(treatments.sortedByDescending { it.fecha })
                    treatmentsAdapter.notifyDataSetChanged()

                    // Actualizar UI vacía
                    if (treatments.isEmpty()) {
                        binding.textEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewTreatments.visibility = View.GONE
                    } else {
                        binding.textEmptyState.visibility = View.GONE
                        binding.recyclerViewTreatments.visibility = View.VISIBLE
                    }

                } else {
                    Toast.makeText(this@TreatmentsActivity, "Error cargando tratamientos", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@TreatmentsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
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