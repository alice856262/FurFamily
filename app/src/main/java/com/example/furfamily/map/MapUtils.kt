package com.example.furfamily.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient

// Helper function to get a color based on category
fun getMarkerColorForCategory(category: String): Int {
    return when (category) {
        "Vet Clinic" -> Color(0xFFD32F2F).toArgb()          // Deep red - urgency, medical
        "Grooming Salon" -> Color(0xFF64B5F6).toArgb()      // Soft blue - clean, fresh
        "Pet Hotel" -> Color(0xFFBA68C8).toArgb()           // Lavender - calm, cozy
        "Dog Park" -> Color(0xFF81C784).toArgb()            // Soft green - nature
        "Pet Store" -> Color(0xFFFFD54F).toArgb()           // Amber - shopping/friendly
        "Training Center" -> Color(0xFFFF8A65).toArgb()     // Coral - action, motivation
        "Pet-Friendly Cafe" -> Color(0xFF4DD0E1).toArgb()   // Aqua - relaxing, cool
        "Pharmacy" -> Color(0xFF0D47A1).toArgb()            // Blue - clinical, calm
        else -> Color(0xFFF33A6A).toArgb()                  // Default: light grey-blue
    }
}

// Helper function to convert our RGB colors to Google Maps marker hues
fun getMarkerHueFromColor(color: Int): Float {
    return when (color) {
        Color(0xFFD32F2F).toArgb() -> BitmapDescriptorFactory.HUE_RED         // Vet Clinic
        Color(0xFF64B5F6).toArgb() -> BitmapDescriptorFactory.HUE_AZURE       // Grooming Salon
        Color(0xFFBA68C8).toArgb() -> BitmapDescriptorFactory.HUE_VIOLET      // Pet Hotel
        Color(0xFF81C784).toArgb() -> BitmapDescriptorFactory.HUE_GREEN       // Dog Park
        Color(0xFFFFD54F).toArgb() -> BitmapDescriptorFactory.HUE_YELLOW      // Pet Store
        Color(0xFFFF8A65).toArgb() -> BitmapDescriptorFactory.HUE_ORANGE      // Training Center
        Color(0xFF4DD0E1).toArgb() -> BitmapDescriptorFactory.HUE_CYAN        // Pet-Friendly Cafe
        Color(0xFF0D47A1).toArgb() -> BitmapDescriptorFactory.HUE_BLUE        // Pharmacy
        else -> BitmapDescriptorFactory.HUE_ROSE                                    // Default
    }
}

fun fetchPlaceDetails(placesClient: PlacesClient, placeId: String, callback: (Place) -> Unit) {
    val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
    val request = FetchPlaceRequest.newInstance(placeId, placeFields)

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->
            callback(response.place)
        }
        .addOnFailureListener { exception ->
            if (exception is com.google.android.gms.common.api.ApiException) {
                println("Place not found: ${exception.message}")
            }
        }
}

fun navigateToLocation(context: Context, destination: LatLng) {
    val uri = "google.navigation:q=${destination.latitude},${destination.longitude}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
    intent.setPackage("com.google.android.apps.maps")
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
    }
}

fun requestLocationPermission(context: Context) {
    ActivityCompat.requestPermissions(
        (context as android.app.Activity),
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        1000
    )
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    return mapView
}