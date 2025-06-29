package com.myapp.calingaapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class CareseekerBookingAdapter(private val bookingList: List<Booking>) : 
    RecyclerView.Adapter<CareseekerBookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_careseeker_booking, parent, false)
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
        
        // Set status color and card styling based on booking status
        when (currentItem.status.lowercase()) {
            "pending" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#FF9800"))
                holder.statusTextView.background = holder.itemView.context.getDrawable(R.drawable.status_pending_background)
            }
            "accepted" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#4CAF50"))
                holder.statusTextView.background = holder.itemView.context.getDrawable(R.drawable.status_accepted_background)
            }
            "completed" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#2196F3"))
                holder.statusTextView.background = holder.itemView.context.getDrawable(R.drawable.status_completed_background)
            }
            "cancelled" -> {
                holder.statusTextView.setTextColor(Color.parseColor("#F44336"))
                holder.statusTextView.background = holder.itemView.context.getDrawable(R.drawable.status_cancelled_background)
            }
            else -> {
                holder.statusTextView.setTextColor(Color.parseColor("#757575"))
                holder.statusTextView.background = holder.itemView.context.getDrawable(R.drawable.status_default_background)
            }
        }
    }

    override fun getItemCount() = bookingList.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTimeTextView: TextView = itemView.findViewById(R.id.tv_booking_date_time)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_booking_address)
        val statusTextView: TextView = itemView.findViewById(R.id.tv_booking_status)
        val notesTextView: TextView = itemView.findViewById(R.id.tv_booking_notes)
        val rateTextView: TextView = itemView.findViewById(R.id.tv_booking_rate)
        val tierTextView: TextView = itemView.findViewById(R.id.tv_caregiver_tier)
    }
}
