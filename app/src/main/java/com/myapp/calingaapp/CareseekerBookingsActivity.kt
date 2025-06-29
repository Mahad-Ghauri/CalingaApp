package com.myapp.calingaapp

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CareseekerBookingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var bookingAdapter: CareseekerBookingAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val bookingList = ArrayList<Booking>()

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
        supportActionBar?.title = "My Bookings"

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewBookings)
        progressBar = findViewById(R.id.progressBar)
        emptyStateText = findViewById(R.id.emptyStateText)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        bookingAdapter = CareseekerBookingAdapter(bookingList)
        recyclerView.adapter = bookingAdapter

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadMyBookings()
        }

        // Load all bookings
        loadMyBookings()
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
                bookingList.clear()

                for (document in documents) {
                    val booking = document.toObject(Booking::class.java)
                    if (booking != null) {
                        bookingList.add(booking)
                    }
                }

                bookingAdapter.notifyDataSetChanged()
                showProgress(false)
                swipeRefreshLayout.isRefreshing = false

                // Show empty state if no bookings
                if (bookingList.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading bookings: ${exception.message}", Toast.LENGTH_SHORT).show()
                showProgress(false)
                swipeRefreshLayout.isRefreshing = false
                showEmptyState(true)
            }
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        emptyStateText.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
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
