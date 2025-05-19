package com.myapp.calingaapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
        
        // Set the caregiver photo
        holder.photoImageView.setImageResource(currentItem.photoResId)
        
        // Extract city and state from address
        val addressParts = currentItem.address.split(",")
        if (addressParts.size >= 2) {
            val cityState = "${addressParts[addressParts.size - 2].trim()}, ${addressParts[addressParts.size - 1].trim()}"
            holder.locationTextView.text = cityState
        } else {
            holder.locationTextView.text = currentItem.address
        }
        
        holder.rateTextView.text = "$${currentItem.rate}/h"
        
        // Set click listener to open the caregiver profile
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CalingaProActivity::class.java).apply {
                putExtra(EXTRA_CAREGIVER_NAME, currentItem.name)
                putExtra(EXTRA_CAREGIVER_TIER, currentItem.tier)
                putExtra(EXTRA_CAREGIVER_ADDRESS, currentItem.address)
                putExtra(EXTRA_CAREGIVER_RATE, currentItem.rate)
                putExtra(EXTRA_CAREGIVER_PHOTO_RES_ID, currentItem.photoResId)
                putExtra(EXTRA_CAREGIVER_EXPERIENCE, currentItem.experience)
                putExtra(EXTRA_CAREGIVER_PATIENTS, currentItem.patients)
                putExtra(EXTRA_CAREGIVER_BLOOD_TYPE, currentItem.bloodType)
                putExtra(EXTRA_CAREGIVER_HEIGHT, currentItem.height)
                putExtra(EXTRA_CAREGIVER_ABOUT, currentItem.about)
                putExtra(EXTRA_CAREGIVER_EMAIL, currentItem.email)
                putExtra(EXTRA_CAREGIVER_PHONE, currentItem.phone)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = calingaProList.size

    class CalingaProViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoImageView: ImageView = itemView.findViewById(R.id.imageViewProfilePic)
        val nameTextView: TextView = itemView.findViewById(R.id.textViewName)
        val tierTextView: TextView = itemView.findViewById(R.id.textViewTier)
        val locationTextView: TextView = itemView.findViewById(R.id.textViewLocation)
        val rateTextView: TextView = itemView.findViewById(R.id.textViewRate)
    }
    
    companion object {
        const val EXTRA_CAREGIVER_NAME = "extra_caregiver_name"
        const val EXTRA_CAREGIVER_TIER = "extra_caregiver_tier"
        const val EXTRA_CAREGIVER_ADDRESS = "extra_caregiver_address"
        const val EXTRA_CAREGIVER_RATE = "extra_caregiver_rate"
        const val EXTRA_CAREGIVER_PHOTO_RES_ID = "extra_caregiver_photo_res_id"
        const val EXTRA_CAREGIVER_EXPERIENCE = "extra_caregiver_experience"
        const val EXTRA_CAREGIVER_PATIENTS = "extra_caregiver_patients"
        const val EXTRA_CAREGIVER_BLOOD_TYPE = "extra_caregiver_blood_type"
        const val EXTRA_CAREGIVER_HEIGHT = "extra_caregiver_height"
        const val EXTRA_CAREGIVER_ABOUT = "extra_caregiver_about"
        const val EXTRA_CAREGIVER_EMAIL = "extra_caregiver_email"
        const val EXTRA_CAREGIVER_PHONE = "extra_caregiver_phone"
    }
}
