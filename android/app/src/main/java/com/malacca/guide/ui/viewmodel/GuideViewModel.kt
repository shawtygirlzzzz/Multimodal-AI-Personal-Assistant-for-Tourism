package com.malacca.guide.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malacca.guide.api.ApiClient
import com.malacca.guide.api.models.AnalyzeResponse
import com.malacca.guide.api.models.NearbyRequest
import com.malacca.guide.api.models.NearbyResponse
import com.malacca.guide.api.models.RestaurantResponse
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

enum class AppMode { LANDMARK, RESTAURANT }

private const val TAG = "GuideViewModel"

class GuideViewModel : ViewModel() {

    var selectedLanguage by mutableStateOf("EN")
        private set
    var appMode by mutableStateOf(AppMode.LANDMARK)
        private set
    var transcript by mutableStateOf("")
        private set
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var isAnalyzing by mutableStateOf(false)
        private set

    // Landmark state
    var analyzeResult by mutableStateOf<AnalyzeResponse?>(null)
        private set
    var analyzeError by mutableStateOf<String?>(null)
        private set
    private var landmarkContext = ""
    var isFollowUp by mutableStateOf(false)
        private set

    // Restaurant state
    var restaurantResult by mutableStateOf<RestaurantResponse?>(null)
        private set
    var restaurantError by mutableStateOf<String?>(null)
        private set
    var nearbyResult by mutableStateOf<NearbyResponse?>(null)
        private set
    var nearbyError by mutableStateOf<String?>(null)
        private set
    var isSearchingNearby by mutableStateOf(false)
        private set
    var currentLat by mutableStateOf(0.0)
        private set
    var currentLng by mutableStateOf(0.0)
        private set

    fun setLanguage(lang: String) { selectedLanguage = lang }
    fun setMode(mode: AppMode) { appMode = mode }
    fun updateTranscript(text: String) { transcript = text }
    fun storeBitmap(bitmap: Bitmap) { capturedBitmap = bitmap }
    fun updateLocation(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng
    }

    fun clearForNewSession() {
        analyzeResult = null
        analyzeError = null
        restaurantResult = null
        restaurantError = null
        nearbyResult = null
        nearbyError = null
        capturedBitmap = null
        transcript = ""
        landmarkContext = ""
        isFollowUp = false
    }

    fun clearResultForFollowUp() {
        landmarkContext = analyzeResult?.landmarkName ?: ""
        isFollowUp = true
        analyzeResult = null
        analyzeError = null
        transcript = ""
    }

    private fun compressBitmap(): ByteArray? {
        val bitmap = capturedBitmap ?: return null
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }

    private fun langCode() = when (selectedLanguage) {
        "ZH" -> "zh"
        "MS" -> "ms"
        else -> "en"
    }

    fun analyze() {
        val bytes = compressBitmap() ?: run {
            Log.e(TAG, "analyze: capturedBitmap is null — no image to send")
            analyzeError = "No image captured"
            return
        }
        Log.d(TAG, "analyze: starting, bitmap size=${bytes.size}, transcript='$transcript'")
        viewModelScope.launch {
            isAnalyzing = true
            analyzeResult = null
            analyzeError = null
            try {
                val imagePart = MultipartBody.Part.createFormData(
                    "image", "photo.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaType())
                )
                val query = transcript.ifBlank { "What is this building? Tell me about it." }
                val response = ApiClient.apiService.analyze(
                    imagePart,
                    query.toRequestBody("text/plain".toMediaType()),
                    langCode().toRequestBody("text/plain".toMediaType()),
                    landmarkContext.toRequestBody("text/plain".toMediaType())
                )
                Log.d(TAG, "analyze: response code=${response.code()}, successful=${response.isSuccessful}")
                if (response.isSuccessful) {
                    analyzeResult = response.body()
                    Log.d(TAG, "analyze: result status=${analyzeResult?.status}, response=${analyzeResult?.response?.take(80)}")
                } else {
                    val err = "Server error ${response.code()}"
                    Log.e(TAG, "analyze: $err — body=${response.errorBody()?.string()}")
                    analyzeError = err
                }
            } catch (e: Exception) {
                Log.e(TAG, "analyze: exception ${e::class.simpleName}: ${e.message}", e)
                analyzeError = e.message ?: "Network error"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun analyzeRestaurant() {
        val bytes = compressBitmap() ?: run {
            Log.e(TAG, "analyzeRestaurant: capturedBitmap is null — no image to send")
            restaurantError = "No image captured"
            return
        }
        Log.d(TAG, "analyzeRestaurant: starting, bitmap size=${bytes.size}, lat=$currentLat, lng=$currentLng")
        viewModelScope.launch {
            isAnalyzing = true
            restaurantResult = null
            restaurantError = null
            try {
                val imagePart = MultipartBody.Part.createFormData(
                    "image", "photo.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaType())
                )
                val response = ApiClient.apiService.analyzeRestaurant(
                    imagePart,
                    currentLat.toString().toRequestBody("text/plain".toMediaType()),
                    currentLng.toString().toRequestBody("text/plain".toMediaType()),
                    langCode().toRequestBody("text/plain".toMediaType())
                )
                Log.d(TAG, "analyzeRestaurant: response code=${response.code()}, successful=${response.isSuccessful}")
                if (response.isSuccessful) {
                    restaurantResult = response.body()
                    Log.d(TAG, "analyzeRestaurant: result status=${restaurantResult?.status}, name=${restaurantResult?.restaurantName}")
                } else {
                    val err = "Server error ${response.code()}"
                    Log.e(TAG, "analyzeRestaurant: $err — body=${response.errorBody()?.string()}")
                    restaurantError = err
                }
            } catch (e: Exception) {
                Log.e(TAG, "analyzeRestaurant: exception ${e::class.simpleName}: ${e.message}", e)
                restaurantError = e.message ?: "Network error"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun findNearby(excludePlaceId: String) {
        viewModelScope.launch {
            isSearchingNearby = true
            nearbyResult = null
            nearbyError = null
            try {
                val response = ApiClient.apiService.nearbyRestaurants(
                    NearbyRequest(
                        lat = currentLat,
                        lng = currentLng,
                        excludePlaceId = excludePlaceId,
                        language = langCode()
                    )
                )
                if (response.isSuccessful) nearbyResult = response.body()
                else nearbyError = "Server error ${response.code()}"
            } catch (e: Exception) {
                nearbyError = e.message ?: "Network error"
            } finally {
                isSearchingNearby = false
            }
        }
    }
}
