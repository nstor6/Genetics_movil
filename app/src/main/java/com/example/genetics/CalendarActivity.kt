package com.example.genetics

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.genetics.Add.AddEventActivity
import com.example.genetics.api.Adapters.EventsAdapter
import com.example.genetics.api.Evento
import com.example.genetics.api.RetrofitClient
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var recyclerViewEvents: androidx.recyclerview.widget.RecyclerView
    private lateinit var textEmptyState: android.widget.LinearLayout  // ← CORREGIDO: LinearLayout en lugar de ConstraintLayout
    private lateinit var textSelectedDate: android.widget.TextView
    private lateinit var textEventCount: android.widget.TextView
    private lateinit var fabAddEvent: com.google.android.material.floatingactionbutton.FloatingActionButton

    // Chips de filtro
    private lateinit var chipAllEvents: Chip
    private lateinit var chipVisitas: Chip
    private lateinit var chipTratamientos: Chip
    private lateinit var chipPartos: Chip

    private val apiService = RetrofitClient.getApiService()
    private lateinit var eventsAdapter: EventsAdapter
    private val eventosList = mutableListOf<Evento>()
    private var selectedDate: String = ""

    companion object {
        private const val REQUEST_ADD_EVENT = 4001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        initializeViews()
        setupUI()
        loadEvents()

        // Configurar fecha inicial (hoy)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = dateFormat.format(Date())
        updateSelectedDateUI()
    }

    private fun initializeViews() {
        calendarView = findViewById(R.id.calendarView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents)
        textEmptyState = findViewById(R.id.textEmptyState)  // ← Ya no hay cast problemático
        textSelectedDate = findViewById(R.id.textSelectedDate)
        textEventCount = findViewById(R.id.textEventCount)
        fabAddEvent = findViewById(R.id.fabAddEvent)

        // Chips
        chipAllEvents = findViewById(R.id.chipAllEvents)
        chipVisitas = findViewById(R.id.chipVisitas)
        chipTratamientos = findViewById(R.id.chipTratamientos)
        chipPartos = findViewById(R.id.chipPartos)
    }

    private fun setupUI() {
        // Configurar toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📅 Calendario"

        // Configurar RecyclerView
        eventsAdapter = EventsAdapter(
            eventosList,
            onItemClick = { evento -> mostrarDetallesEvento(evento) },
            onOptionsClick = { evento -> mostrarOpcionesEvento(evento) }
        )

        recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@CalendarActivity)
            adapter = eventsAdapter
        }

        // Configurar CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(calendar.time)
            updateSelectedDateUI()
            filterEventsByDate()
        }

        // Configurar FAB
        fabAddEvent.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate)
            startActivityForResult(intent, REQUEST_ADD_EVENT)
        }

        // Configurar refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadEvents()
        }

        // Configurar filtros
        setupFilters()
    }

    private fun setupFilters() {
        chipAllEvents.setOnClickListener {
            resetFilterChips()
            chipAllEvents.isChecked = true
            eventsAdapter.filterByType("")
            updateEventCount()
        }

        chipVisitas.setOnClickListener {
            resetFilterChips()
            chipVisitas.isChecked = true
            eventsAdapter.filterByType("visita")
            updateEventCount()
        }

        chipTratamientos.setOnClickListener {
            resetFilterChips()
            chipTratamientos.isChecked = true
            eventsAdapter.filterByType("tratamiento")
            updateEventCount()
        }

        chipPartos.setOnClickListener {
            resetFilterChips()
            chipPartos.isChecked = true
            eventsAdapter.filterByType("parto")
            updateEventCount()
        }
    }

    private fun resetFilterChips() {
        chipAllEvents.isChecked = false
        chipVisitas.isChecked = false
        chipTratamientos.isChecked = false
        chipPartos.isChecked = false
    }

    private fun loadEvents() {
        swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = apiService.getEventos()

                if (response.isSuccessful && response.body() != null) {
                    val events = response.body()!!

                    eventosList.clear()
                    eventosList.addAll(events.sortedBy { it.fecha_inicio })
                    eventsAdapter.updateList(eventosList)

                    filterEventsByDate()

                } else {
                    Toast.makeText(this@CalendarActivity, "Error cargando eventos", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@CalendarActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun filterEventsByDate() {
        eventsAdapter.filterByDate(selectedDate)
        updateEventCount()
    }

    private fun updateSelectedDateUI() {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
            val date = inputFormat.parse(selectedDate)

            val formattedDate = outputFormat.format(date ?: Date())
            textSelectedDate.text = "📅 Eventos del $formattedDate"
        } catch (e: Exception) {
            textSelectedDate.text = "📅 Eventos del día"
        }
    }

    private fun updateEventCount() {
        val eventCount = eventsAdapter.itemCount
        textEventCount.text = if (eventCount == 1) {
            "1 evento"
        } else {
            "$eventCount eventos"
        }

        // Mostrar/ocultar estado vacío
        if (eventCount == 0) {
            textEmptyState.visibility = View.VISIBLE
            recyclerViewEvents.visibility = View.GONE
        } else {
            textEmptyState.visibility = View.GONE
            recyclerViewEvents.visibility = View.VISIBLE
        }
    }

    private fun mostrarDetallesEvento(evento: Evento) {
        val mensaje = buildString {
            append("📅 ${evento.titulo}\n\n")
            append("🕐 Fecha: ${formatearFecha(evento.fecha_inicio)}\n")
            if (evento.fecha_fin != null) {
                append("🕕 Hasta: ${formatearFecha(evento.fecha_fin!!)}\n")
            }
            append("📋 Tipo: ${evento.tipo}\n")
            if (evento.animal != null) {
                append("🐄 Animal: ID ${evento.animal}\n")
            }
            if (!evento.descripcion.isNullOrEmpty()) {
                append("\n📝 Descripción:\n${evento.descripcion}")
            }
            if (evento.recurrente) {
                append("\n\n🔄 Este es un evento recurrente")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalles del Evento")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .setNeutralButton("Editar") { _, _ ->
                Toast.makeText(this, "Editar evento - Próximamente", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun mostrarOpcionesEvento(evento: Evento) {
        // Crear menú contextual simple sin PopupMenu por ahora
        val opciones = arrayOf("✏️ Editar evento", "📋 Duplicar evento", "🗑️ Eliminar evento")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Opciones del evento")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Editar evento - Próximamente", Toast.LENGTH_SHORT).show()
                    1 -> duplicarEvento(evento)
                    2 -> confirmarEliminarEvento(evento)
                }
            }
            .show()
    }

    private fun duplicarEvento(evento: Evento) {
        val intent = Intent(this, AddEventActivity::class.java)
        intent.putExtra("DUPLICATE_EVENT_ID", evento.id)
        intent.putExtra("SELECTED_DATE", selectedDate)
        startActivityForResult(intent, REQUEST_ADD_EVENT)
    }

    private fun confirmarEliminarEvento(evento: Evento) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Evento")
            .setMessage("¿Estás seguro de que quieres eliminar el evento '${evento.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarEvento(evento)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEvento(evento: Evento) {
        lifecycleScope.launch {
            try {
                val response = apiService.eliminarEvento(evento.id!!)
                if (response.isSuccessful) {
                    Toast.makeText(this@CalendarActivity, "Evento eliminado", Toast.LENGTH_SHORT).show()
                    loadEvents()
                } else {
                    Toast.makeText(this@CalendarActivity, "Error al eliminar evento", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CalendarActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(fecha)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = inputFormat.parse(fecha)
                outputFormat.format(date ?: Date())
            } catch (e2: Exception) {
                fecha
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_EVENT && resultCode == RESULT_OK) {
            loadEvents()
        }
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}