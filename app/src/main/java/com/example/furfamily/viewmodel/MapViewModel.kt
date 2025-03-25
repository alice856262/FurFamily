package com.example.furfamily.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.furfamily.map.PlaceTag
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _placeTags = MutableStateFlow<Map<LatLng, String>>(emptyMap())
    val placeTags: StateFlow<Map<LatLng, String>> = _placeTags

    private val _placeCategories = MutableStateFlow<Map<LatLng, String>>(emptyMap())
    val placeCategories: StateFlow<Map<LatLng, String>> = _placeCategories

    fun loadPlaceTags() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("loadPlaceTags", "User not authenticated.")
            return
        }

        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")
        placeRef.get().addOnSuccessListener { dataSnapshot ->
            val tagsMap = mutableMapOf<LatLng, String>()
            val categoryMap = mutableMapOf<LatLng, String>()

            dataSnapshot.children.forEach { snapshot ->
                val placeTag = snapshot.getValue(PlaceTag::class.java)
                if (placeTag != null) {
                    val latLng = LatLng(placeTag.latitude, placeTag.longitude)
                    val name = placeTag.name ?: "Unnamed Place"

                    tagsMap[latLng] = name
                    categoryMap[latLng] = placeTag.category ?: "Other"
                }
            }
            _placeTags.value = tagsMap
            _placeCategories.value = categoryMap
        }.addOnFailureListener { exception ->
            Log.e("loadPlaceTags", "Failed to load tags: ${exception.message}")
        }
    }

    fun savePlaceTag(latLng: LatLng, tag: String, address: String?, category: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("savePlaceTag", "User not authenticated.")
            return
        }

        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")
        val key = placeRef.push().key
        if (key != null) {
            val placeTag = PlaceTag(
                tagId = key,
                userId = userId,
                name = tag,
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                address = address ?: "Unknown Address",
                category = category
            )

            placeRef.child(key).setValue(placeTag)
                .addOnSuccessListener {
                    Log.d("savePlaceTag", "Tag saved successfully: $placeTag")
                    loadPlaceTags() // Reload tags after saving
                }
                .addOnFailureListener { exception ->
                    Log.e("savePlaceTag", "Failed to save tag: ${exception.message}")
                }
        } else {
            Log.e("savePlaceTag", "Failed to generate a key.")
        }
    }

    suspend fun getTagDetailsForLocation(location: LatLng): Pair<String?, String?> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Pair(null, null)
        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")

        return try {
            val snapshot = placeRef.get().await()
            val matchingChild = snapshot.children.firstOrNull { childSnapshot ->
                val latitude = childSnapshot.child("latitude").getValue(Double::class.java)
                val longitude = childSnapshot.child("longitude").getValue(Double::class.java)
                val isMatch = latitude != null && longitude != null &&
                        Math.abs(latitude - location.latitude) < 0.0001 && // Tolerance for latitude
                        Math.abs(longitude - location.longitude) < 0.0001 // Tolerance for longitude

                isMatch
            }

            if (matchingChild != null) {
                val id = matchingChild.key
                val name = matchingChild.child("name").getValue(String::class.java)
                Pair(id, name)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Log.e("getTagDetailsForLocation", "Error fetching tag details: ${e.message}")
            Pair(null, null)
        }
    }

    suspend fun getPlaceTagByLocation(location: LatLng): PlaceTag? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")

        return try {
            val snapshot = placeRef.get().await()
            snapshot.children.firstOrNull { childSnapshot ->
                val latitude = childSnapshot.child("latitude").getValue(Double::class.java)
                val longitude = childSnapshot.child("longitude").getValue(Double::class.java)

                latitude != null && longitude != null &&
                        Math.abs(latitude - location.latitude) < 0.0001 &&
                        Math.abs(longitude - location.longitude) < 0.0001
            }?.getValue(PlaceTag::class.java)
        } catch (e: Exception) {
            Log.e("getPlaceTagByLocation", "Error fetching place tag: ${e.message}")
            null
        }
    }

    fun modifyPlaceTag(tagId: String, latLng: LatLng, newTagName: String, category: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("modifyPlaceTag", "User not authenticated.")
            return
        }

        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId/$tagId")
        val updates = mapOf(
            "name" to newTagName,
            "latitude" to latLng.latitude,
            "longitude" to latLng.longitude,
            "category" to category
        )

        placeRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("modifyPlaceTag", "Tag modified successfully: $tagId with new name: $newTagName")
                loadPlaceTags() // Reload tags after modification
            }
            .addOnFailureListener { exception ->
                Log.e("modifyPlaceTag", "Failed to modify tag: ${exception.message}")
            }
    }

    fun deletePlaceTag(tagId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("deletePlaceTag", "User not authenticated.")
            return
        }

        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId/$tagId")
        placeRef.removeValue()
            .addOnSuccessListener {
                Log.d("deletePlaceTag", "Tag deleted successfully: $tagId")
                loadPlaceTags() // Reload tags after deletion
            }
            .addOnFailureListener { exception ->
                Log.e("deletePlaceTag", "Failed to delete tag: ${exception.message}")
            }
    }
}