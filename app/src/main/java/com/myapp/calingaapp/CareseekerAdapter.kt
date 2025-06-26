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
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return CareseekerViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CareseekerViewHolder, position: Int) {
        val currentItem = careseekerList[position]
        holder.nameTextView.text = currentItem.name
        holder.ageTextView.text = "${currentItem.age} years old - ${currentItem.address}"
        
        holder.itemView.setOnClickListener {
            // Handle item click if needed
        }
    }

    override fun getItemCount() = careseekerList.size

    class CareseekerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(android.R.id.text1)
        val ageTextView: TextView = itemView.findViewById(android.R.id.text2)
    }
}

// Data class for Careseeker items (kept for compatibility)
data class Careseeker(
    val name: String,
    val age: Int,
    val address: String
)
