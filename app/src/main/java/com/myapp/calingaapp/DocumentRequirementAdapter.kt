package com.myapp.calingaapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DocumentRequirementAdapter(
    private var documentList: List<DocumentRequirement>
) : RecyclerView.Adapter<DocumentRequirementAdapter.DocumentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document_requirement, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documentList[position]
        
        holder.titleTextView.text = document.title
        
        if (document.description.isNotEmpty()) {
            holder.descriptionTextView.visibility = View.VISIBLE
            holder.descriptionTextView.text = document.description
        } else {
            holder.descriptionTextView.visibility = View.GONE
        }
        
        // Update UI based on upload status
        if (document.isUploaded) {
            holder.uploadButton.text = "View Document"
            holder.statusImageView.setImageResource(R.drawable.ic_check_circle)
            holder.statusImageView.visibility = View.VISIBLE
        } else {
            holder.uploadButton.text = "Upload Document"
            holder.statusImageView.visibility = View.GONE
        }
        
        // Set click listener for upload button
        holder.uploadButton.setOnClickListener {
            if (!document.isUploaded) {
                // Create intent to pick document
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*" // All file types
                holder.itemView.context.startActivity(
                    Intent.createChooser(intent, "Select a document")
                )
                
                // In a real app, you'd handle the result in the activity
                // For now, just simulate a successful upload
                document.isUploaded = true
                notifyItemChanged(position)
            } else {
                // If already uploaded, we would show the document
                // But for now, just show that it's already uploaded
                document.isUploaded = false
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount() = documentList.size
    
    fun updateDocuments(newDocuments: List<DocumentRequirement>) {
        documentList = newDocuments
        notifyDataSetChanged()
    }

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewDocumentTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDocumentDescription)
        val uploadButton: Button = itemView.findViewById(R.id.buttonUpload)
        val statusImageView: ImageView = itemView.findViewById(R.id.imageViewStatus)
    }
}
