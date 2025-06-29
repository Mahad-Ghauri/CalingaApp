package com.myapp.calingaapp

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class BookingAdapter(private val bookingList: MutableList<Booking>, private val context: Context) : 
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_booking_caregiver, parent, false)
        return BookingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val currentItem = bookingList[position]
        
        // Set booking details
        holder.dateTimeTextView.text = "${currentItem.date} at ${currentItem.time}"
        holder.addressTextView.text = currentItem.address
        holder.statusTextView.text = currentItem.status.uppercase()
        holder.notesTextView.text = if (currentItem.notes.isNotEmpty()) currentItem.notes else "No special notes"
        holder.rateTextView.text = "$${currentItem.ratePerHour}/hr"
        holder.tierTextView.text = currentItem.caregiverTier
        
        // Set status color and button visibility based on booking status
        when (currentItem.status.lowercase()) {
            "pending" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#FF9800"))
                holder.completeButton.visibility = View.GONE
                holder.acceptButton.visibility = View.VISIBLE
            }
            "accepted" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#4CAF50"))
                holder.completeButton.visibility = View.VISIBLE
                holder.acceptButton.visibility = View.GONE
            }
            "completed" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#2196F3"))
                holder.completeButton.visibility = View.GONE
                holder.acceptButton.visibility = View.GONE
            }
            "cancelled" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#F44336"))
                holder.completeButton.visibility = View.GONE
                holder.acceptButton.visibility = View.GONE
            }
            else -> {
                holder.statusTextView.setTextColor(Color.parseColor("#757575"))
                holder.completeButton.visibility = View.GONE
                holder.acceptButton.visibility = View.GONE
            }
        }

        // Handle accept booking
        holder.acceptButton.setOnClickListener {
            updateBookingStatus(currentItem, "accepted", position)
        }

        // Handle complete service
        holder.completeButton.setOnClickListener {
            showCompleteServiceDialog(currentItem, position)
        }
    }

    private fun updateBookingStatus(booking: Booking, newStatus: String, position: Int) {
        firestore.collection("bookings")
            .document(booking.bookingId)
            .update("status", newStatus)
            .addOnSuccessListener {
                // Update the local list
                bookingList[position] = booking.copy(status = newStatus)
                notifyItemChanged(position)
                
                val statusMessage = when (newStatus) {
                    "accepted" -> "Booking accepted successfully!"
                    "completed" -> "Service completed successfully!"
                    else -> "Booking updated!"
                }
                Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update booking: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCompleteServiceDialog(booking: Booking, position: Int) {
        // Create a modern dialog for service completion
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_complete_service, null)
        
        val notesLayout = dialogView.findViewById<TextInputLayout>(R.id.til_completion_notes)
        val notesEditText = dialogView.findViewById<TextInputEditText>(R.id.et_completion_notes)
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialogView.findViewById<Button>(R.id.btn_complete_service).setOnClickListener {
            val completionNotes = notesEditText.text.toString().trim()
            
            if (completionNotes.isEmpty()) {
                notesLayout.error = "Please add completion notes"
                return@setOnClickListener
            }
            
            notesLayout.error = null
            
            // Update booking with completion notes and status
            val updates = mapOf(
                "status" to "completed",
                "completionNotes" to completionNotes,
                "completedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore.collection("bookings")
                .document(booking.bookingId)
                .update(updates)
                .addOnSuccessListener {
                    // Update local list
                    bookingList[position] = booking.copy(status = "completed")
                    notifyItemChanged(position)
                    dialog.dismiss()
                    Toast.makeText(context, "Service completed successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    notesLayout.error = "Failed to complete service: ${e.message}"
                }
        }
        
        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun getItemCount() = bookingList.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTimeTextView: TextView = itemView.findViewById(R.id.tv_booking_date_time)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_booking_address)
        val statusTextView: TextView = itemView.findViewById(R.id.tv_booking_status)
        val notesTextView: TextView = itemView.findViewById(R.id.tv_booking_notes)
        val rateTextView: TextView = itemView.findViewById(R.id.tv_booking_rate)
        val tierTextView: TextView = itemView.findViewById(R.id.tv_caregiver_tier)
        val acceptButton: Button = itemView.findViewById(R.id.btn_accept_booking)
        val completeButton: Button = itemView.findViewById(R.id.btn_complete_service)
    }
}
