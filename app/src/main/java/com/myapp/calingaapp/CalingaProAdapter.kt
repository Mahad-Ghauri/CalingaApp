package com.myapp.calingaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalingaProAdapter(private val calingaProList: List<CalingaPro>) : 
    RecyclerView.Adapter<CalingaProAdapter.CalingaProViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalingaProViewHolder {
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_calingapro, parent, false)
        return CalingaProViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CalingaProViewHolder, position: Int) {
        val currentItem = calingaProList[position]
        holder.nameTextView.text = currentItem.name
        holder.tierTextView.text = currentItem.tier
        
        // Extract city and state from address
        val addressParts = currentItem.address.split(",")
        if (addressParts.size >= 2) {
            val cityState = "${addressParts[addressParts.size - 2].trim()}, ${addressParts[addressParts.size - 1].trim()}"
            holder.locationTextView.text = cityState
        } else {
            holder.locationTextView.text = currentItem.address
        }
        
        holder.rateTextView.text = "$${currentItem.rate}/h"
    }

    override fun getItemCount() = calingaProList.size

    class CalingaProViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        val tierTextView: TextView = itemView.findViewById(R.id.textViewTier)
        val locationTextView: TextView = itemView.findViewById(R.id.textViewLocation)
        val rateTextView: TextView = itemView.findViewById(R.id.textViewRate)
    }
}
