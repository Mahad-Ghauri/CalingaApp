package com.myapp.calingaapp

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class FirebaseStorageManager {
    
    private val storage = FirebaseStorage.getInstance("gs://calingaapp.firebasestorage.app")
    private val storageRef = storage.reference
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        const val PROFILE_PHOTOS_PATH = "profile_photos"
        const val DOCUMENTS_PATH = "documents"
        const val CAREGIVER_DOCS_PATH = "caregiver_documents"
        const val PROFILE_VERIFICATION_PATH = "profile_verification"
    }
    
    /**
     * Upload profile photo
     */
    fun uploadProfilePhoto(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: (Int) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure(Exception("User not authenticated"))
            return
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "profile_${currentUser.uid}_$timestamp.jpg"
        val profilePhotoRef = storageRef.child("$PROFILE_PHOTOS_PATH/$fileName")
        
        uploadFile(imageUri, profilePhotoRef, onSuccess, onFailure, onProgress)
    }
    
    /**
     * Upload document (ID, certificates, etc.)
     */
    fun uploadDocument(
        imageUri: Uri,
        documentType: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: (Int) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure(Exception("User not authenticated"))
            return
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${documentType}_${currentUser.uid}_$timestamp.jpg"
        val documentRef = storageRef.child("$DOCUMENTS_PATH/${currentUser.uid}/$fileName")
        
        uploadFile(imageUri, documentRef, onSuccess, onFailure, onProgress)
    }
    
    /**
     * Upload caregiver verification documents
     */
    fun uploadCaregiverDocument(
        imageUri: Uri,
        documentType: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: (Int) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure(Exception("User not authenticated"))
            return
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${documentType}_${currentUser.uid}_$timestamp.jpg"
        val documentRef = storageRef.child("$CAREGIVER_DOCS_PATH/${currentUser.uid}/$fileName")
        
        uploadFile(imageUri, documentRef, onSuccess, onFailure, onProgress)
    }
    
    /**
     * Common upload function
     */
    private fun uploadFile(
        imageUri: Uri,
        storageRef: StorageReference,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val uploadTask = storageRef.putFile(imageUri)
        
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            onProgress(progress)
        }.addOnSuccessListener {
            // Upload successful, get download URL
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }.addOnFailureListener { exception ->
                onFailure(exception)
            }
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }
    
    /**
     * Delete file from storage
     */
    fun deleteFile(
        fileUrl: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (fileUrl.isEmpty()) {
            onSuccess()
            return
        }
        
        try {
            val fileRef = storage.getReferenceFromUrl(fileUrl)
            fileRef.delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { exception -> onFailure(exception) }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
    
    /**
     * Get storage reference for a specific path
     */
    fun getStorageReference(path: String): StorageReference {
        return storageRef.child(path)
    }
}
