package com.malacca.guide.api.models

import com.google.gson.annotations.SerializedName

data class HealthResponse(
    val status: String,
    val version: String
)

data class AnalyzeResponse(
    val status: String,
    @SerializedName("landmark_name") val landmarkName: String?,
    val response: String?,
    @SerializedName("session_id") val sessionId: String?,
    val confidence: String?,
    val message: String?
)

data class RestaurantResponse(
    val status: String,
    val type: String?,
    @SerializedName("restaurant_name") val restaurantName: String?,
    @SerializedName("place_id") val placeId: String?,
    val rating: Float?,
    @SerializedName("review_count") val reviewCount: Int?,
    @SerializedName("top_review") val topReview: String?,
    @SerializedName("price_level") val priceLevel: Int?,
    @SerializedName("opening_hours") val openingHours: String?,
    val cuisine: String?,
    val response: String?,
    @SerializedName("session_id") val sessionId: String?,
    val message: String?
)

data class PlaceAlternative(
    val name: String,
    val rating: Float,
    @SerializedName("price_level") val priceLevel: Int,
    @SerializedName("distance_m") val distanceM: Int,
    @SerializedName("opening_hours") val openingHours: String,
    val cuisine: String,
    @SerializedName("place_id") val placeId: String
)

data class NearbyRequest(
    val lat: Double,
    val lng: Double,
    @SerializedName("exclude_place_id") val excludePlaceId: String?,
    val language: String = "en"
)

data class NearbyResponse(
    val status: String,
    val alternatives: List<PlaceAlternative> = emptyList(),
    val response: String?,
    val message: String?
)
