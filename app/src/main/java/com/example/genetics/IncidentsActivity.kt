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
    private val incidenciasOriginales = mutableListOf<Incidencia>() // Lista completa sin filtrar

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
            mostrarDetallesIncidencia(incidencia)
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

        // Configurar filtros - CORREGIDO
        setupFilters()
    }

    private fun setupFilters() {
        // Configurar el chip "Todas" como seleccionado por defecto
        binding.chipFilterAll.isChecked = true

        binding.chipFilterAll.setOnClickListener {
            resetChips()
            binding.chipFilterAll.isChecked = true
            filterIncidents("all")
        }

        binding.chipFilterPending.setOnClickListener {
            resetChips()
            binding.chipFilterPending.isChecked = true
            filterIncidents("pendiente")
        }

        binding.chipFilterTreatment.setOnClickListener {
            resetChips()
            binding.chipFilterTreatment.isChecked = true
            filterIncidents("en tratamiento")
        }

        binding.chipFilterResolved.setOnClickListener {
            resetChips()
            binding.chipFilterResolved.isChecked = true
            filterIncidents("resuelto")
        }
    }

    private fun resetChips() {
        binding.chipFilterAll.isChecked = false
        binding.chipFilterPending.isChecked = false
        binding.chipFilterTreatment.isChecked = false
        binding.chipFilterResolved.isChecked = false
    }

    private fun loadIncidents() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getIncidencias()

                if (response.isSuccessful && response.body() != null) {
                    val incidents = response.body()!!

                    // Guardar la lista completa sin filtrar
                    incidenciasOriginales.clear()
                    incidenciasOriginales.addAll(incidents.sortedByDescending { it.fecha_deteccion })

                    // Mostrar todas las incidencias inicialmente usando el mismo método que los filtros
                    filterIncidents("all")

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
        when (filter) {
            "all" -> {
                // Mostrar todas las incidencias
                incidentsAdapter.updateList(incidenciasOriginales)
            }
            "pendiente" -> {
                val filtered = incidenciasOriginales.filter { it.estado == "pendiente" }
                incidentsAdapter.updateList(filtered)
            }
            "en tratamiento" -> {
                val filtered = incidenciasOriginales.filter { it.estado == "en tratamiento" }
                incidentsAdapter.updateList(filtered)
            }
            "resuelto" -> {
                val filtered = incidenciasOriginales.filter { it.estado == "resuelto" }
                incidentsAdapter.updateList(filtered)
            }
        }

        // Actualizar estado vacío después del filtrado
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val hasItems = incidentsAdapter.itemCount > 0

        if (hasItems) {
            binding.textEmptyState.visibility = View.GONE
            binding.recyclerViewIncidents.visibility = View.VISIBLE
        } else {
            binding.textEmptyState.visibility = View.VISIBLE
            binding.recyclerViewIncidents.visibility = View.GONE
        }
    }

    private fun mostrarDetallesIncidencia(incidencia: Incidencia) {
        val mensaje = buildString {
            append("🚨 Tipo: ${incidencia.tipo}\n\n")
            append("📝 Descripción:\n${incidencia.descripcion}\n\n")
            append("🐄 Animal: ID ${incidencia.animal}\n")
            append("📅 Fecha detección: ${formatearFecha(incidencia.fecha_deteccion)}\n")
            append("📊 Estado: ${formatearEstado(incidencia.estado)}\n")

            if (incidencia.fecha_resolucion != null) {
                append("✅ Fecha resolución: ${formatearFecha(incidencia.fecha_resolucion!!)}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles de la Incidencia")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .setNeutralButton("Editar") { _, _ ->
                Toast.makeText(this, "Editar incidencia - Próximamente", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun formatearEstado(estado: String): String {
        return when (estado) {
            "pendiente" -> "⏳ Pendiente"
            "en tratamiento" -> "🔄 En Tratamiento"
            "resuelto" -> "✅ Resuelto"
            else -> estado.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(fecha)
            outputFormat.format(date ?: java.util.Date())
        } catch (e: Exception) {
            fecha
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