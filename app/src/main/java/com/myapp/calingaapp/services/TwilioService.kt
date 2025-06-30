package com.myapp.calingaapp.services

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.random.Random

class TwilioService(private val context: Context) {
    
    companion object {
        private const val TWILIO_VERIFY_URL = "https://verify.twilio.com/v2/Services"
        
        // Load these from twilio.properties file
        private var ACCOUNT_SID: String = ""
        private var AUTH_TOKEN: String = ""
        private var VERIFY_SERVICE_SID: String = ""
        
        // Rate limiting - store last request times per phone number
        private val lastRequestTimes = mutableMapOf<String, Long>()
        private const val RATE_LIMIT_INTERVAL = 60000L // 1 minute between requests
    }
    
    private val client = OkHttpClient()
    
    init {
        loadTwilioConfig()
    }
    
    private fun loadTwilioConfig() {
        try {
            val properties = Properties()
            val inputStream = context.assets.open("twilio.properties")
            properties.load(inputStream)
            
            ACCOUNT_SID = properties.getProperty("TWILIO_ACCOUNT_SID", "")
            AUTH_TOKEN = properties.getProperty("TWILIO_AUTH_TOKEN", "")
            VERIFY_SERVICE_SID = properties.getProperty("TWILIO_VERIFY_SERVICE_SID", "")
            
            inputStream.close()
            
            // Log the configuration (remove in production)
            android.util.Log.d("TwilioService", "Config loaded - Account SID: ${ACCOUNT_SID.take(10)}...")
            android.util.Log.d("TwilioService", "Verify Service SID: $VERIFY_SERVICE_SID")
            
        } catch (e: Exception) {
            android.util.Log.e("TwilioService", "Error loading Twilio credentials", e)
        }
    }
    
    fun sendOtp(phoneNumber: String, callback: (Boolean, String) -> Unit) {
        // Validate credentials first
        if (ACCOUNT_SID.isEmpty() || AUTH_TOKEN.isEmpty() || VERIFY_SERVICE_SID.isEmpty()) {
            callback(false, "Twilio configuration is incomplete. Please check your credentials.")
            return
        }
        
        // Rate limiting check
        val currentTime = System.currentTimeMillis()
        val lastRequestTime = lastRequestTimes[phoneNumber] ?: 0L
        
        if (currentTime - lastRequestTime < RATE_LIMIT_INTERVAL) {
            val remainingTime = (RATE_LIMIT_INTERVAL - (currentTime - lastRequestTime)) / 1000
            callback(false, "Please wait $remainingTime seconds before requesting another OTP")
            return
        }
        
        // Update last request time
        lastRequestTimes[phoneNumber] = currentTime
        
        sendOtpWithVerifyService(phoneNumber, callback)
    }
    
    private fun sendOtpWithVerifyService(phoneNumber: String, callback: (Boolean, String) -> Unit) {
        val url = "$TWILIO_VERIFY_URL/$VERIFY_SERVICE_SID/Verifications"
        
        android.util.Log.d("TwilioService", "Sending OTP to: $phoneNumber")
        android.util.Log.d("TwilioService", "Using URL: $url")
        
        val formBody = FormBody.Builder()
            .add("To", phoneNumber)
            .add("Channel", "sms")
            .build()
        
        val credentials = Credentials.basic(ACCOUNT_SID, AUTH_TOKEN)
        
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .header("Authorization", credentials)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("TwilioService", "Failed to send OTP", e)
                callback(false, "Failed to send OTP: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                android.util.Log.d("TwilioService", "Response code: ${response.code}")
                android.util.Log.d("TwilioService", "Response body: $responseBody")
                
                if (response.isSuccessful) {
                    callback(true, "OTP sent successfully")
                } else {
                    val errorMessage = when (response.code) {
                        400 -> "Invalid phone number format"
                        401 -> "Invalid Twilio credentials"
                        404 -> "Verify Service not found"
                        429 -> "Too many requests. Please try again later"
                        else -> "Failed to send OTP (${response.code}): ${responseBody ?: response.message}"
                    }
                    callback(false, errorMessage)
                }
            }
        })
    }
    
    fun verifyOtp(phoneNumber: String, userEnteredOtp: String, callback: (Boolean, String) -> Unit) {
        // Validate credentials first
        if (ACCOUNT_SID.isEmpty() || AUTH_TOKEN.isEmpty() || VERIFY_SERVICE_SID.isEmpty()) {
            callback(false, "Twilio configuration is incomplete. Please check your credentials.")
            return
        }
        
        verifyOtpWithVerifyService(phoneNumber, userEnteredOtp, callback)
    }
    
    private fun verifyOtpWithVerifyService(phoneNumber: String, userEnteredOtp: String, callback: (Boolean, String) -> Unit) {
        val url = "$TWILIO_VERIFY_URL/$VERIFY_SERVICE_SID/VerificationCheck"
        
        android.util.Log.d("TwilioService", "Verifying OTP for: $phoneNumber")
        android.util.Log.d("TwilioService", "Using URL: $url")
        android.util.Log.d("TwilioService", "Code: $userEnteredOtp")
        
        val formBody = FormBody.Builder()
            .add("To", phoneNumber)
            .add("Code", userEnteredOtp)
            .build()
        
        val credentials = Credentials.basic(ACCOUNT_SID, AUTH_TOKEN)
        
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .header("Authorization", credentials)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("TwilioService", "Failed to verify OTP", e)
                callback(false, "Verification failed: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                android.util.Log.d("TwilioService", "Verify response code: ${response.code}")
                android.util.Log.d("TwilioService", "Verify response body: $responseBody")
                
                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val status = jsonResponse.optString("status", "")
                        val valid = jsonResponse.optBoolean("valid", false)
                        
                        android.util.Log.d("TwilioService", "Parsed status: $status, valid: $valid")
                        
                        if (status == "approved" && valid) {
                            android.util.Log.d("TwilioService", "Verification successful!")
                            callback(true, "Phone number verified successfully")
                        } else {
                            android.util.Log.w("TwilioService", "Verification failed - status: $status, valid: $valid")
                            callback(false, "Invalid OTP or verification failed")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TwilioService", "Error parsing verification response", e)
                        callback(false, "Error processing verification response")
                    }
                } else {
                    val errorMessage = when (response.code) {
                        400 -> "Invalid verification code"
                        404 -> "Verification not found or expired"
                        429 -> "Too many verification attempts"
                        else -> "Failed to verify OTP (${response.code}): ${responseBody ?: response.message}"
                    }
                    android.util.Log.w("TwilioService", "Verification failed with code ${response.code}: $errorMessage")
                    callback(false, errorMessage)
                }
            }
        })
    }
}
