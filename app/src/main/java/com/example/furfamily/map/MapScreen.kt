package com.example.furfamily.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.furfamily.ViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.launch

@Composable
fun MapScreen(viewModel: ViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val mapView = rememberMapViewWithLifecycle()
    val placesClient = remember { Places.createClient(context) }

    var selectedPlace by remember { mutableStateOf<LatLng?>(null) }
    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf("") }
    var placeDetails by remember { mutableStateOf<Place?>(null) }
    var modifyTag by remember { mutableStateOf(false) }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val placeTags = remember { mutableStateMapOf<LatLng, String>() }
    val markerMap = remember { mutableStateMapOf<LatLng, Marker>() }

    // Load initial tags
    LaunchedEffect(Unit) {
        viewModel.loadPlaceTags()
        viewModel.placeTags.collect { tags ->
            placeTags.clear()
            placeTags.putAll(tags)
        }
    }

    // Fetch current location
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                }
            }
        } else {
            requestLocationPermission(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SearchBar { latLng, name ->
                    selectedPlace = latLng
                    placeName = name
                    modifyTag = false
                    showTagDialog = true
                }
            }

            // Google Map View
            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
            ) { mapView ->
                mapView.getMapAsync { googleMap ->
                    googleMap.uiSettings.isZoomControlsEnabled = true

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        googleMap.isMyLocationEnabled = true
                    }

                    // Move camera to current location
                    currentLocation?.let {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    }

                    // Add markers for saved tags dynamically
                    placeTags.forEach { (location, tag) ->
                        if (!markerMap.containsKey(location)) {
                            val marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .title(tag)
                            )
                            if (marker != null) {
                                markerMap[location] = marker
                            }
                        }
                    }

                    // Handle clicks on POIs
                    googleMap.setOnPoiClickListener { poi ->
                        fetchPlaceDetails(placesClient, poi.placeId) { place ->
                            placeDetails = place
                            placeName = place.name ?: "Unnamed Place"
                            selectedPlace = place.latLng
                            selectedTagId = null
                            modifyTag = false
                            showTagDialog = true
                        }
                    }

                    // Add click listener for places not saved as tags
                    googleMap.setOnMapClickListener { latLng ->
                        selectedPlace = latLng
                        placeName = "Unnamed Place"
                        showTagDialog = true
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                items(placeTags.entries.toList()) { (location, tag) ->
                    val coroutineScope = rememberCoroutineScope()

                    TaggedPlaceItem(
                        tag = tag,
                        location = location,
                        onNavigate = { navigateToLocation(context, location) },
                        onModify = {
                            coroutineScope.launch {
                                val tagId = viewModel.getTagIdForLocation(location)
                                if (tagId != null) {
                                    selectedPlace = location
                                    placeName = tag
                                    selectedTagId = tagId
                                    modifyTag = true
                                    showTagDialog = true
                                } else {
                                    Toast.makeText(context, "Failed to retrieve tag ID for modification.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                val tagId = viewModel.getTagIdForLocation(location)
                                if (tagId != null) {
                                    viewModel.deletePlaceTag(tagId)
                                    placeTags.remove(location) // Update state directly
                                    markerMap[location]?.remove() // Remove the marker from the map
                                    markerMap.remove(location) // Remove from the marker map
                                } else {
                                    Toast.makeText(context, "Failed to delete tag: Tag ID not found.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Show dialog to add or modify a tag
        if (showTagDialog) {
            AddTagDialog(
                placeName = placeName,
                placeDetails = placeDetails,
                onAddTag = { tag ->
                    selectedPlace?.let { latLng ->
                        val address = placeDetails?.address ?: "Unknown Address" // Use fetched address
                        if (modifyTag && selectedTagId != null) {
                            viewModel.modifyPlaceTag(selectedTagId!!, latLng, tag)
                            placeTags[latLng] = tag // Update state directly
                        } else {
                            viewModel.savePlaceTag(latLng, tag, address) // Pass the address
                            placeTags[latLng] = tag // Add new tag directly
                        }
                    }
                    showTagDialog = false
                    modifyTag = false
                },
                onDismiss = {
                    showTagDialog = false
                    modifyTag = false
                }
            )
        }
    }
}

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onPlaceSelected: (LatLng, String) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            place.latLng?.let { onPlaceSelected(it, place.name ?: "Unnamed Place") }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR && result.data != null) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Toast.makeText(context, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Button(
        onClick = {
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            ).build(context)
            launcher.launch(intent)
        },
        modifier = modifier
    ) {
        Text("Click Here to Search Places")
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

@Composable
fun AddTagDialog(
    placeName: String,
    placeDetails: Place?,
    onAddTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tag by remember { mutableStateOf(placeName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add or Modify Tag") },
        text = {
            Column {
                TextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag Name") }
                )
                if (placeDetails != null) {
                    Text("Address: ${placeDetails.address ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddTag(tag)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TaggedPlaceItem(
    tag: String,
    location: LatLng,
    onNavigate: () -> Unit,
    onModify: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(tag, style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onNavigate) {
                Text("Navigate")
            }
            Button(onClick = onModify) {
                Text("Edit")
            }
            Button(onClick = onDelete) {
                Text("Delete")
            }
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