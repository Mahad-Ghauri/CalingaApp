package com.myapp.calingaapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageUtils {
    
    companion object {
        /**
         * Show image picker dialog (Camera or Gallery)
         */
        fun showImagePickerDialog(
            context: Context,
            onCameraSelected: () -> Unit,
            onGallerySelected: () -> Unit
        ) {
            val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
            
            AlertDialog.Builder(context)
                .setTitle("Select Image")
                .setItems(options) { dialog, which ->
                    when (which) {
                        0 -> onCameraSelected()
                        1 -> onGallerySelected()
                        2 -> dialog.dismiss()
                    }
                }
                .show()
        }
        
        /**
         * Create camera intent
         */
        fun createCameraIntent(context: Context): Pair<Intent, Uri?> {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            var photoUri: Uri? = null
            
            try {
                val photoFile = createImageFile(context)
                photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            } catch (ex: IOException) {
                // Handle error
            }
            
            return Pair(takePictureIntent, photoUri)
        }
        
        /**
         * Create gallery intent
         */
        fun createGalleryIntent(): Intent {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickPhotoIntent.type = "image/*"
            return pickPhotoIntent
        }
        
        /**
         * Create temporary image file
         */
        private fun createImageFile(context: Context): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile(imageFileName, ".jpg", storageDir)
        }
        
        /**
         * Compress image
         */
        fun compressImage(context: Context, imageUri: Uri, maxWidth: Int = 1024, maxHeight: Int = 1024, quality: Int = 80): Uri? {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (originalBitmap == null) return null
                
                // Get image rotation
                val rotation = getImageRotation(context, imageUri)
                
                // Rotate bitmap if needed
                val rotatedBitmap = if (rotation != 0) {
                    rotateBitmap(originalBitmap, rotation)
                } else {
                    originalBitmap
                }
                
                // Calculate new dimensions
                val ratio = minOf(
                    maxWidth.toFloat() / rotatedBitmap.width,
                    maxHeight.toFloat() / rotatedBitmap.height
                )
                
                val newWidth = (rotatedBitmap.width * ratio).toInt()
                val newHeight = (rotatedBitmap.height * ratio).toInt()
                
                // Resize bitmap
                val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true)
                
                // Save compressed bitmap to temporary file
                val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(tempFile)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.close()
                
                // Clean up
                if (rotatedBitmap != originalBitmap) {
                    rotatedBitmap.recycle()
                }
                resizedBitmap.recycle()
                originalBitmap.recycle()
                
                return Uri.fromFile(tempFile)
                
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        
        /**
         * Get image rotation from EXIF data
         */
        private fun getImageRotation(context: Context, imageUri: Uri): Int {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val exif = inputStream?.let { ExifInterface(it) }
                inputStream?.close()
                
                return when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (e: Exception) {
                return 0
            }
        }
        
        /**
         * Rotate bitmap
         */
        private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(degrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        
        /**
         * Load image into ImageView using Glide
         */
        fun loadImage(
            context: Context,
            imageUrl: String?,
            imageView: ImageView,
            placeholder: Int = R.drawable.ic_person_placeholder
        ) {
            val requestOptions = RequestOptions()
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
            
            Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        }
        
        /**
         * Load circular image
         */
        fun loadCircularImage(
            context: Context,
            imageUrl: String?,
            imageView: ImageView,
            placeholder: Int = R.drawable.ic_person_placeholder
        ) {
            val requestOptions = RequestOptions()
                .placeholder(placeholder)
                .error(placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
            
            Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView)
        }
    }
}
