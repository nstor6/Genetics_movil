package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.api.Incidencia
import com.example.genetics.api.Adapters.IncidentsAdapter
import com.example.genetics.api.RetrofitClient
import com.example.genetics.databinding.ActivityIncidentsBinding
import kotlinx.coroutines.launch

class IncidentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidentsBinding
    private val apiService = RetrofitClient.getApiService()
    private lateinit var incidentsAdapter: IncidentsAdapter
    private val incidenciasList = mutableListOf<Incidencia>()

    companion object {
        private const val REQUEST_ADD_INCIDENT = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadIncidents()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🚨 Incidencias"

        // Configurar RecyclerView
        incidentsAdapter = IncidentsAdapter(incidenciasList) { incidencia ->
            // Ver detalles de la incidencia
            Toast.makeText(this, "Detalle de: ${incidencia.tipo}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewIncidents.apply {
            layoutManager = LinearLayoutManager(this@IncidentsActivity)
            adapter = incidentsAdapter
        }

        // Configurar FAB
        binding.fabAddIncident.setOnClickListener {
            val intent = Intent(this, AddIncidentActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_INCIDENT)
        }

        // Configurar refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadIncidents()
        }

        // Configurar filtros
        binding.chipFilterAll.setOnClickListener { filterIncidents("all") }
        binding.chipFilterPending.setOnClickListener { filterIncidents("pendiente") }
        binding.chipFilterTreatment.setOnClickListener { filterIncidents("en tratamiento") }
        binding.chipFilterResolved.setOnClickListener { filterIncidents("resuelto") }
    }

    private fun loadIncidents() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getIncidencias()

                if (response.isSuccessful && response.body() != null) {
                    val incidents = response.body()!!

                    incidenciasList.clear()
                    incidenciasList.addAll(incidents.sortedByDescending { it.fecha_deteccion })
                    incidentsAdapter.notifyDataSetChanged()

                    // Actualizar UI vacía
                    if (incidents.isEmpty()) {
                        binding.textEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewIncidents.visibility = View.GONE
                    } else {
                        binding.textEmptyState.visibility = View.GONE
                        binding.recyclerViewIncidents.visibility = View.VISIBLE
                    }

                } else {
                    Toast.makeText(this@IncidentsActivity, "Error cargando incidencias", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@IncidentsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun filterIncidents(filter: String) {
        // Resetear todos los chips
        binding.chipFilterAll.isChecked = false
        binding.chipFilterPending.isChecked = false
        binding.chipFilterTreatment.isChecked = false
        binding.chipFilterResolved.isChecked = false

        // Marcar el chip seleccionado
        when (filter) {
            "all" -> {
                binding.chipFilterAll.isChecked = true
                incidentsAdapter.updateList(incidenciasList)
            }
            "pendiente" -> {
                binding.chipFilterPending.isChecked = true
                val filtered = incidenciasList.filter { it.estado == "pendiente" }
                incidentsAdapter.updateList(filtered)
            }
            "en tratamiento" -> {
                binding.chipFilterTreatment.isChecked = true
                val filtered = incidenciasList.filter { it.estado == "en tratamiento" }
                incidentsAdapter.updateList(filtered)
            }
            "resuelto" -> {
                binding.chipFilterResolved.isChecked = true
                val filtered = incidenciasList.filter { it.estado == "resuelto" }
                incidentsAdapter.updateList(filtered)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_INCIDENT && resultCode == RESULT_OK) {
            loadIncidents()
        }
    }

    override fun onResume() {
        super.onResume()
        loadIncidents()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}