package com.myapp.calingaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CareseekerAdapter(private val careseekerList: List<Careseeker>) : 
    RecyclerView.Adapter<CareseekerAdapter.CareseekerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CareseekerViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_careseeker, parent, false)
        return CareseekerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CareseekerViewHolder, position: Int) {
        val currentItem = careseekerList[position]
        holder.nameTextView.text = currentItem.name
        holder.ageTextView.text = "${currentItem.age} years old"
        
        // Extract city and state from address
        val addressParts = currentItem.address.split(",")
        if (addressParts.size >= 2) {
            val cityState = "${addressParts[addressParts.size - 2].trim()}, ${addressParts[addressParts.size - 1].trim()}"
            holder.locationTextView.text = cityState
        } else {
            holder.locationTextView.text = currentItem.address
        }
    }

    override fun getItemCount() = careseekerList.size

    class CareseekerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        val ageTextView: TextView = itemView.findViewById(R.id.textViewAge)
        val locationTextView: TextView = itemView.findViewById(R.id.textViewLocation)
    }
}
