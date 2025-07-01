package com.myapp.calingaapp

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CareseekerBookingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: CareseekerBookingAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var emptyStateSubtext: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chipGroupFilters: ChipGroup
    private lateinit var refreshButton: ImageView
    
    private val allBookingsList = ArrayList<Booking>()
    private val filteredBookingsList = ArrayList<Booking>()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_careseeker_bookings)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Initialize UI components
        initializeViews()
        setupRecyclerView()
        setupFilterChips()
        setupRefreshFunctionality()

        // Load all bookings
        loadMyBookings()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewBookings)
        progressBar = findViewById(R.id.progressBar)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        emptyStateText = findViewById(R.id.emptyStateText)
        emptyStateSubtext = findViewById(R.id.emptyStateSubtext)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        chipGroupFilters = findViewById(R.id.chipGroupFilters)
        refreshButton = findViewById(R.id.refreshButton)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        bookingAdapter = CareseekerBookingAdapter(filteredBookingsList)
        recyclerView.adapter = bookingAdapter
    }

    private fun setupFilterChips() {
        chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val checkedId = checkedIds[0]
                currentFilter = when (checkedId) {
                    R.id.chipAll -> "all"
                    R.id.chipPending -> "pending"
                    R.id.chipAccepted -> "accepted"
                    R.id.chipCompleted -> "completed"
                    R.id.chipCancelled -> "cancelled"
                    else -> "all"
                }
                applyFilter()
            }
        }
    }

    private fun setupRefreshFunctionality() {
        swipeRefreshLayout.setOnRefreshListener {
            loadMyBookings()
        }
        
        refreshButton.setOnClickListener {
            loadMyBookings()
        }
    }

    private fun loadMyBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showProgress(true)

        // Query all bookings for this careseeker, ordered by creation date descending (newest first)
        db.collection("bookings")
            .whereEqualTo("careseekerId", currentUser.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                allBookingsList.clear()

                for (document in documents) {
                    val booking = document.toObject(Booking::class.java)
                    if (booking != null) {
                        allBookingsList.add(booking)
                    }
                }

                applyFilter()
                showProgress(false)
                swipeRefreshLayout.isRefreshing = false

                // Show appropriate empty state
                updateEmptyState()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading bookings: ${exception.message}", Toast.LENGTH_SHORT).show()
                showProgress(false)
                swipeRefreshLayout.isRefreshing = false
                showEmptyState(true, "Error loading bookings", "Please try again")
            }
    }

    private fun applyFilter() {
        filteredBookingsList.clear()
        
        when (currentFilter) {
            "all" -> filteredBookingsList.addAll(allBookingsList)
            else -> {
                filteredBookingsList.addAll(
                    allBookingsList.filter { it.status.lowercase() == currentFilter.lowercase() }
                )
            }
        }
        
        bookingAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (filteredBookingsList.isEmpty()) {
            when (currentFilter) {
                "all" -> showEmptyState(true, "No bookings found", "Book your first caregiver to get started!")
                "pending" -> showEmptyState(true, "No pending bookings", "All your pending requests will appear here")
                "accepted" -> showEmptyState(true, "No accepted bookings", "Accepted bookings will appear here")
                "completed" -> showEmptyState(true, "No completed bookings", "Your completed services will appear here")
                "cancelled" -> showEmptyState(true, "No cancelled bookings", "Cancelled bookings will appear here")
                else -> showEmptyState(true, "No bookings found", "Try selecting a different filter")
            }
        } else {
            showEmptyState(false)
        }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState(show: Boolean, title: String = "No bookings found", subtitle: String = "Book your first caregiver to get started!") {
        emptyStateLayout.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        
        if (show) {
            emptyStateText.text = title
            emptyStateSubtext.text = subtitle
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
