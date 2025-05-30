package com.example.genetics.Activitys

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.Create.AddTreatmentActivity
import com.example.genetics.Edit.EditTreatmentActivity
import com.example.genetics.R
import com.example.genetics.api.Adapters.TreatmentsAdapter
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

    companion object {
        private const val REQUEST_ADD_TREATMENT = 3001
        private const val REQUEST_EDIT_TREATMENT = 3002
    }

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

        // ✅ CONFIGURAR RECYCLERVIEW CON CALLBACKS DE CRUD
        treatmentsAdapter = TreatmentsAdapter(
            tratamientosList,
            onItemClick = { tratamiento -> mostrarDetallesTratamiento(tratamiento) },
            onEditClick = { tratamiento -> editarTratamiento(tratamiento) },
            onDeleteClick = { tratamiento -> confirmarEliminarTratamiento(tratamiento) }
        )

        recyclerViewTreatments.apply {
            layoutManager = LinearLayoutManager(this@TreatmentsActivity)
            adapter = treatmentsAdapter
        }

        // Configurar FAB
        fabAddTreatment.setOnClickListener {
            val intent = Intent(this, AddTreatmentActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_TREATMENT)
        }

        // Configurar refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadTreatments()
        }
    }

    private fun mostrarDetallesTratamiento(tratamiento: Tratamiento) {
        val mensaje = buildString {
            append("💊 Medicamento: ${tratamiento.medicamento}\n")
            append("📏 Dosis: ${tratamiento.dosis}\n")
            append("⏱️ Duración: ${tratamiento.duracion}\n")
            append("📅 Fecha: ${formatearFecha(tratamiento.fecha)}\n")
            append("🐄 Animal ID: ${tratamiento.animal}\n")
            if (!tratamiento.observaciones.isNullOrEmpty()) {
                append("\n📝 Observaciones:\n${tratamiento.observaciones}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles del Tratamiento")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .setNeutralButton("✏️ Editar") { _, _ ->
                editarTratamiento(tratamiento)
            }
            .setNegativeButton("🗑️ Eliminar") { _, _ ->
                confirmarEliminarTratamiento(tratamiento)
            }
            .show()
    }

    private fun editarTratamiento(tratamiento: Tratamiento) {
        val intent = Intent(this, EditTreatmentActivity::class.java)
        intent.putExtra("TRATAMIENTO_ID", tratamiento.id)
        startActivityForResult(intent, REQUEST_EDIT_TREATMENT)
    }

    private fun confirmarEliminarTratamiento(tratamiento: Tratamiento) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Eliminar Tratamiento")
            .setMessage("¿Estás seguro de que quieres eliminar el tratamiento con '${tratamiento.medicamento}' del animal ID ${tratamiento.animal}?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarTratamiento(tratamiento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarTratamiento(tratamiento: Tratamiento) {
        lifecycleScope.launch {
            try {
                android.util.Log.d("TREATMENTS_ACTIVITY", "🗑️ Eliminando tratamiento ID: ${tratamiento.id}")

                val response = apiService.eliminarTratamiento(tratamiento.id!!)

                if (response.isSuccessful) {
                    Toast.makeText(this@TreatmentsActivity, "Tratamiento eliminado correctamente", Toast.LENGTH_SHORT).show()

                    // Eliminar del adapter inmediatamente para mejor UX
                    treatmentsAdapter.removeTreatment(tratamiento)

                    // Actualizar estado vacío si es necesario
                    updateEmptyState(tratamientosList.isEmpty())

                    android.util.Log.d("TREATMENTS_ACTIVITY", "✅ Tratamiento eliminado exitosamente")
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("TREATMENTS_ACTIVITY", "❌ Error eliminando: $errorBody")
                    Toast.makeText(this@TreatmentsActivity, "Error al eliminar tratamiento", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("TREATMENTS_ACTIVITY", "❌ Exception eliminando: ${e.message}", e)
                Toast.makeText(this@TreatmentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

    private fun loadTreatments() {
        android.util.Log.d("TREATMENTS_ACTIVITY", "🔄 Iniciando carga de tratamientos...")
        swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getTratamientos()
                android.util.Log.d("TREATMENTS_ACTIVITY", "📡 Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val treatments = response.body()!!
                    android.util.Log.d("TREATMENTS_ACTIVITY", "✅ Tratamientos recibidos: ${treatments.size}")

                    // Limpiar y actualizar la lista
                    tratamientosList.clear()
                    tratamientosList.addAll(treatments.sortedByDescending { it.fecha })

                    // Notificar al adapter
                    treatmentsAdapter.notifyDataSetChanged()

                    android.util.Log.d("TREATMENTS_ACTIVITY", "📋 Lista actualizada. Items en adapter: ${treatmentsAdapter.itemCount}")

                    // Actualizar UI vacía
                    updateEmptyState(treatments.isEmpty())

                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("TREATMENTS_ACTIVITY", "❌ Error: $errorBody")
                    Toast.makeText(this@TreatmentsActivity, "Error cargando tratamientos: ${response.code()}", Toast.LENGTH_SHORT).show()
                    updateEmptyState(true)
                }

            } catch (e: Exception) {
                android.util.Log.e("TREATMENTS_ACTIVITY", "❌ Exception: ${e.message}", e)
                Toast.makeText(this@TreatmentsActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                updateEmptyState(true)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            textEmptyState.visibility = View.VISIBLE
            recyclerViewTreatments.visibility = View.GONE
            android.util.Log.d("TREATMENTS_ACTIVITY", "👻 Mostrando estado vacío")
        } else {
            textEmptyState.visibility = View.GONE
            recyclerViewTreatments.visibility = View.VISIBLE
            android.util.Log.d("TREATMENTS_ACTIVITY", "📋 Mostrando lista de tratamientos")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ADD_TREATMENT, REQUEST_EDIT_TREATMENT -> {
                if (resultCode == RESULT_OK) {
                    android.util.Log.d("TREATMENTS_ACTIVITY", "🔄 Recargando después de crear/editar tratamiento")
                    loadTreatments()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("TREATMENTS_ACTIVITY", "🔄 onResume - Recargando datos")
        loadTreatments()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}