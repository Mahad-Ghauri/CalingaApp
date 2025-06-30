package com.myapp.calingaapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DocumentsSubmissionActivity : AppCompatActivity(), DocumentUploadListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var documentRequirementAdapter: DocumentRequirementAdapter
    private lateinit var storageManager: FirebaseStorageManager
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private var photoUri: Uri? = null
    private var currentUploadPosition: Int = -1
    private var currentDocumentType: String = ""
    
    // Activity result launchers for camera and gallery
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                val compressedUri = ImageUtils.compressImage(this, uri)
                if (compressedUri != null) {
                    uploadDocument(compressedUri, currentDocumentType, currentUploadPosition)
                }
            }
        }
    }
    
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null && data.data != null) {
                val selectedUri = data.data!!
                val compressedUri = ImageUtils.compressImage(this, selectedUri)
                if (compressedUri != null) {
                    uploadDocument(compressedUri, currentDocumentType, currentUploadPosition)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents_submission)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storageManager = FirebaseStorageManager()

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Compliance Documents"

        // Find views
        recyclerView = findViewById(R.id.recyclerViewDocuments)
        spinner = findViewById(R.id.spinnerRoleType)
        progressBar = findViewById(R.id.progressBar) // Add this to your layout if not present
        
        // Set up spinner with role types
        val roles = arrayOf(
            "Select Role Type", 
            "CNA - Certified Nursing Assistant",
            "LVN - Licensed Vocational Nurse",
            "RN - Registered Nurse",
            "NP - Nurse Practitioner",
            "PT - Physical Therapist",
            "Private Caregiver",
            "HHA - Home Health Aide"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        // Set up recycler view
        recyclerView.layoutManager = LinearLayoutManager(this)
        documentRequirementAdapter = DocumentRequirementAdapter(emptyList(), this)
        recyclerView.adapter = documentRequirementAdapter
        
        // Handle spinner selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        // Nothing selected yet
                        documentRequirementAdapter.updateDocuments(emptyList())
                    }
                    1 -> {
                        // CNA
                        documentRequirementAdapter.updateDocuments(getCnaDocuments())
                    }
                    2 -> {
                        // LVN
                        documentRequirementAdapter.updateDocuments(getLvnDocuments())
                    }
                    3 -> {
                        // RN
                        documentRequirementAdapter.updateDocuments(getRnDocuments())
                    }
                    4 -> {
                        // NP
                        documentRequirementAdapter.updateDocuments(getNpDocuments())
                    }
                    5 -> {
                        // PT
                        documentRequirementAdapter.updateDocuments(getPtDocuments())
                    }
                    6 -> {
                        // Private Caregiver
                        documentRequirementAdapter.updateDocuments(getPrivateCaregiverDocuments())
                    }
                    7 -> {
                        // HHA
                        documentRequirementAdapter.updateDocuments(getHhaDocuments())
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    // DocumentUploadListener implementation
    override fun onDocumentUploadClicked(documentType: String, position: Int) {
        currentDocumentType = documentType
        currentUploadPosition = position
        showImagePickerDialog()
    }
    
    override fun onDocumentViewClicked(documentUrl: String) {
        // Open document in browser or image viewer
        if (documentUrl.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl))
            startActivity(intent)
        }
    }
    
    private fun showImagePickerDialog() {
        ImageUtils.showImagePickerDialog(
            context = this,
            onCameraSelected = {
                val (cameraIntent, uri) = ImageUtils.createCameraIntent(this)
                photoUri = uri
                cameraLauncher.launch(cameraIntent)
            },
            onGallerySelected = {
                val galleryIntent = ImageUtils.createGalleryIntent()
                galleryLauncher.launch(galleryIntent)
            }
        )
    }
    
    private fun uploadDocument(imageUri: Uri, documentType: String, position: Int) {
        progressBar.visibility = View.VISIBLE
        
        storageManager.uploadCaregiverDocument(
            imageUri = imageUri,
            documentType = documentType.replace(" ", "_").replace("-", "_"),
            onSuccess = { downloadUrl ->
                // Update the document as uploaded
                documentRequirementAdapter.markDocumentAsUploaded(position, downloadUrl)
                
                // Save document reference to user profile
                saveDocumentToUserProfile(downloadUrl, documentType)
                
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Document uploaded successfully", Toast.LENGTH_SHORT).show()
            },
            onFailure = { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to upload document: ${exception.message}", Toast.LENGTH_SHORT).show()
            },
            onProgress = { progress ->
                // You can update a progress bar here if needed
            }
        )
    }
    
    private fun saveDocumentToUserProfile(documentUrl: String, documentType: String) {
        val currentUser = auth.currentUser ?: return
        
        // Get current user profile and add document URL to the documents list
        db.collection("userProfiles").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val currentDocuments = document.get("documents") as? List<String> ?: emptyList()
                val updatedDocuments = currentDocuments.toMutableList()
                
                // Add new document URL with type prefix
                updatedDocuments.add("$documentType|$documentUrl")
                
                // Update user profile with new documents list
                db.collection("userProfiles").document(currentUser.uid)
                    .update("documents", updatedDocuments)
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save document reference: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }
    
    // Document requirement lists for each role
    private fun getCnaDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("California CNA Certificate", "Issued by CDPH (California Department of Public Health)", false),
            DocumentRequirement("CDPH License Verification", "CDPH License Lookup Tool", false),
            DocumentRequirement("Valid Government ID or Real ID", "", false),
            DocumentRequirement("Proof of Address", "", false),
            DocumentRequirement("CPR/First Aid Certification", "", false),
            DocumentRequirement("Live Scan Background Check / DOJ Clearance", "", false),
            DocumentRequirement("TB Test Results", "within 1 year", false),
            DocumentRequirement("Signed CALiNGA Independent Contractor Agreement", "", false)
        )
    }
    
    private fun getLvnDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("California LVN License", "Issued by BVNPT (Board of Vocational Nursing & Psychiatric Technicians)", false),
            DocumentRequirement("License Verification", "BVNPT License Lookup", false),
            DocumentRequirement("Valid Government ID or Real ID", "", false),
            DocumentRequirement("CPR Certification", "BLS Preferred", false),
            DocumentRequirement("Live Scan / DOJ Clearance", "", false),
            DocumentRequirement("TB Test Results", "", false),
            DocumentRequirement("Proof of Work Authorization", "e.g. SSN, Green Card", false),
            DocumentRequirement("Signed CALiNGA Independent Contractor Agreement", "", false)
        )
    }
    
    private fun getRnDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("California RN License", "Issued by BRN (Board of Registered Nursing)", false),
            DocumentRequirement("License Verification", "BRN License Lookup", false),
            DocumentRequirement("NPI (National Provider Identifier)", "optional but preferred if billing", false),
            DocumentRequirement("CPR Certification", "BLS or ACLS", false),
            DocumentRequirement("Live Scan Background Check", "", false),
            DocumentRequirement("TB Test Results", "", false),
            DocumentRequirement("Government ID / Real ID", "", false),
            DocumentRequirement("Signed CALiNGA Independent Contractor Agreement", "", false)
        )
    }
    
    private fun getNpDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("California RN License + Nurse Practitioner Certification", "", false),
            DocumentRequirement("Furnishing Number", "if prescribing medication", false),
            DocumentRequirement("NPI Number", "required", false),
            DocumentRequirement("DEA Registration", "if prescribing controlled substances", false),
            DocumentRequirement("Malpractice Insurance", "if billing or prescribing", false),
            DocumentRequirement("CPR / BLS / ACLS Certification", "", false),
            DocumentRequirement("Live Scan / DOJ Clearance", "", false),
            DocumentRequirement("Government ID / Real ID", "", false),
            DocumentRequirement("Signed CALiNGA Independent Contractor Agreement", "", false)
        )
    }
    
    private fun getPtDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("California PT License", "Verified by the Physical Therapy Board of California", false),
            DocumentRequirement("License Lookup", "PT License Search", false),
            DocumentRequirement("NPI Number", "preferred for clinical care", false),
            DocumentRequirement("CPR Certification", "", false),
            DocumentRequirement("Live Scan Background Check", "", false),
            DocumentRequirement("Government ID / Real ID", "", false),
            DocumentRequirement("Signed CALiNGA Independent Contractor Agreement", "", false)
        )
    }
    
    private fun getPrivateCaregiverDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("Driver's License or Government-issued ID", "", false),
            DocumentRequirement("Social Security Number or ITIN", "for payment/tax reporting", false),
            DocumentRequirement("CPR/First Aid Certification", "optional but recommended", false),
            DocumentRequirement("Live Scan Fingerprinting", "if working with vulnerable adults", false),
            DocumentRequirement("TB Test or Health Screening", "often required by clients or facilities", false),
            DocumentRequirement("Proof of Address", "", false),
            DocumentRequirement("Work Authorization", "if not a U.S. citizen", false)
        )
    }
    
    private fun getHhaDocuments(): List<DocumentRequirement> {
        return listOf(
            DocumentRequirement("California HHA Certificate", "", false),
            DocumentRequirement("Proof of CNA License", "most HHAs are also CNAs", false),
            DocumentRequirement("Valid Government ID or Real ID", "", false),
            DocumentRequirement("Proof of Address", "", false),
            DocumentRequirement("CPR/First Aid Certification", "", false),
            DocumentRequirement("Live Scan Background Check / DOJ Clearance", "", false),
            DocumentRequirement("TB Test Results", "within 1 year", false),
            DocumentRequirement("Signed CALiNGA Independent Contractor Agreement", "", false)
        )
    }
}

// Data class for document requirements
data class DocumentRequirement(
    val title: String,
    val description: String,
    var isUploaded: Boolean,
    var documentUrl: String? = null
)
