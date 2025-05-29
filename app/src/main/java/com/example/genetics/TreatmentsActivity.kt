package com.example.genetics

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.api.RetrofitClient
import com.example.genetics.api.Tratamiento
import kotlinx.coroutines.launch

class TreatmentsActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var recyclerViewTreatments: androidx.recyclerview.widget.RecyclerView
    private lateinit var textEmptyState: android.widget.LinearLayout
    private lateinit var fabAddTreatment: com.google.android.material.floatingactionbutton.FloatingActionButton

    private val apiService = RetrofitClient.getApiService()
    private lateinit var treatmentsAdapter: TreatmentsAdapter
    private val tratamientosList = mutableListOf<Tratamiento>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatments)

        initializeViews()
        setupUI()
        loadTreatments()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerViewTreatments = findViewById(R.id.recyclerViewTreatments)
        textEmptyState = findViewById(R.id.textEmptyState)
        fabAddTreatment = findViewById(R.id.fabAddTreatment)
    }

    private fun setupUI() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "💊 Tratamientos"

        // Configurar RecyclerView
        treatmentsAdapter = TreatmentsAdapter(tratamientosList) { tratamiento ->
            Toast.makeText(this, "Tratamiento: ${tratamiento.medicamento}", Toast.LENGTH_SHORT).show()
        }

        recyclerViewTreatments.apply {
            layoutManager = LinearLayoutManager(this@TreatmentsActivity)
            adapter = treatmentsAdapter
        }

        // Configurar FAB
        fabAddTreatment.setOnClickListener {
            Toast.makeText(this, "Agregar tratamiento - Próximamente", Toast.LENGTH_SHORT).show()
        }

        // Configurar refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadTreatments()
        }
    }

    private fun loadTreatments() {
        swipeRefreshLayout.isRefreshing = true

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
                        textEmptyState.visibility = View.VISIBLE
                        recyclerViewTreatments.visibility = View.GONE
                    } else {
                        textEmptyState.visibility = View.GONE
                        recyclerViewTreatments.visibility = View.VISIBLE
                    }

                } else {
                    Toast.makeText(this@TreatmentsActivity, "Error cargando tratamientos", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@TreatmentsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}