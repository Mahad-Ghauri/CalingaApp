package com.myapp.calingaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookingAdapter(private val bookingList: List<Booking>) : 
    RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val currentItem = bookingList[position]
        
        // Updated to use new Booking schema fields
        holder.patientEmailTextView.text = "Careseeker ID: ${currentItem.careseekerId}" // userEmail -> careseekerId
        holder.timeTextView.text = "${currentItem.date} ${currentItem.time}" // preferredTime -> date + time
        holder.addressTextView.text = currentItem.address // serviceAddress -> address
        holder.statusTextView.text = currentItem.status.uppercase() // bookingStatus -> status
        holder.notesTextView.text = if (currentItem.notes.isNotEmpty()) currentItem.notes else "No special notes"
        holder.paymentTextView.text = "Rate: $${currentItem.ratePerHour}/hr" // paymentMethod -> ratePerHour
        
        // Set status color based on booking status
        when (currentItem.status.lowercase()) { // bookingStatus -> status
            "pending" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_orange_dark))
            "accepted" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            "completed" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_blue_dark))
            "cancelled" -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            else -> holder.statusTextView.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }
    }

    override fun getItemCount() = bookingList.size

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val patientEmailTextView: TextView = itemView.findViewById(R.id.tv_patient_email)
        val timeTextView: TextView = itemView.findViewById(R.id.tv_booking_time)
        val addressTextView: TextView = itemView.findViewById(R.id.tv_service_address)
        val statusTextView: TextView = itemView.findViewById(R.id.tv_booking_status)
        val notesTextView: TextView = itemView.findViewById(R.id.tv_booking_notes)
        val paymentTextView: TextView = itemView.findViewById(R.id.tv_payment_method)
    }
}
