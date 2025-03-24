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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.furfamily.ViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
    var hasMovedCamera by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<LatLng?>(null) }
    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var placeName by remember { mutableStateOf("") }
    var placeDetails by remember { mutableStateOf<Place?>(null) }
    var modifyTag by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val placeTags by viewModel.placeTags.collectAsState()
    val placeCategories by viewModel.placeCategories.collectAsState()
    val markerMap = remember { mutableStateMapOf<LatLng, Marker>() }

    // Load initial tags and categories
    LaunchedEffect(Unit) {
        viewModel.loadPlaceTags()
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

    // Update map markers when place tags change or category filter changes
    LaunchedEffect(placeTags, placeCategories, categoryFilter) {
        mapView.getMapAsync { googleMap ->
            // Clear existing markers
            markerMap.values.forEach { it.remove() }
            markerMap.clear()

            // Filter places based on selected category
            val filteredPlaces = if (categoryFilter != null) {
                placeTags.filter { (location, _) ->
                    placeCategories[location] == categoryFilter
                }
            } else {
                placeTags
            }

            // Re-add markers based on current state and filter
            filteredPlaces.forEach { (location, tag) ->
                val category = placeCategories[location] ?: "Other"
                val markerColor = getMarkerColorForCategory(category)

                val markerOptions = MarkerOptions()
                    .position(location)
                    .title(tag)

                try {
                    val hue = getMarkerHueFromColor(markerColor)
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                } catch (e: Exception) {
                    Log.e("MapScreen", "Error setting marker icon: ${e.message}")
                }

                val marker = googleMap.addMarker(markerOptions)
                if (marker != null) {
                    markerMap[location] = marker
                }
            }
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
                SearchBar { latLng, name, address ->
                    selectedPlace = latLng
                    placeName = name
                    placeDetails = if (address != null) {
                        Place.builder()
                            .setLatLng(latLng)
                            .setName(name)
                            .setAddress(address)
                            .build()
                    } else {
                        null
                    }
                    modifyTag = false
                    selectedCategory = "Other" // Default category for new tags
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

                    // Move camera only once when the map first loads
                    if (!hasMovedCamera) {
                        currentLocation?.let {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                            hasMovedCamera = true
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
                            selectedCategory = "Other"
                            showTagDialog = true
                        }
                    }

                    // Add click listener for places not saved as tags
                    googleMap.setOnMapClickListener { latLng ->
                        selectedPlace = latLng
                        placeName = "Unnamed Place"
                        placeDetails = null // Reset place details
                        selectedCategory = "Other" // Default category for new tags

                        // Perform reverse geocoding to get address
                        val geocoder = Geocoder(context)
                        try {
                            @Suppress("DEPRECATION") // For backward compatibility
                            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.let { addresses ->
                                if (addresses.isNotEmpty()) {
                                    val address = addresses[0]
                                    val addressText = address.getAddressLine(0) ?: "Unknown Address"

                                    // Create a simple Place object with just the address
                                    placeDetails = Place.builder()
                                        .setLatLng(latLng)
                                        .setAddress(addressText)
                                        .build()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Error getting address: ${e.message}")
                        }

                        showTagDialog = true
                    }
                }
            }

            // Category Legend with active filter indication
            Spacer(modifier = Modifier.height(4.dp))
            CategoryLegend(
                selectedCategory = categoryFilter,
                onCategorySelected = { category ->
                    // Toggle filter: if already selected, clear filter, otherwise set it
                    categoryFilter = if (categoryFilter == category) null else category
                }
            )

            // Current filter indicator
            if (categoryFilter != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtering: $categoryFilter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )

                    TextButton(
                        onClick = { categoryFilter = null }
                    ) {
                        Text("Clear Filter")
                    }
                }
            }

            // Display tagged places with category information (filtered by selected category)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Filter the places based on selected category
                val filteredPlaces = if (categoryFilter != null) {
                    placeTags.entries.filter { (location, _) ->
                        placeCategories[location] == categoryFilter
                    }
                } else {
                    placeTags.entries
                }

                items(filteredPlaces.toList()) { (location, tag) ->
                    val coroutineScope = rememberCoroutineScope()
                    val category = placeCategories[location] ?: "Other"
                    var address by remember { mutableStateOf("Loading address...") }

                    // Load the address for this location
                    LaunchedEffect(location) {
                        coroutineScope.launch {
                            val placeTag = viewModel.getPlaceTagByLocation(location)
                            address = placeTag?.address ?: "Address not available"
                        }
                    }

                    TaggedPlaceItem(
                        tag = tag,
                        category = category,
                        location = location,
                        address = address,
                        onNavigate = { navigateToLocation(context, location) },
                        onModify = {
                            coroutineScope.launch {
                                val (tagId, tagName) = viewModel.getTagDetailsForLocation(location)
                                if (tagId != null) {
                                    selectedPlace = location
                                    placeName = tagName ?: tag
                                    selectedTagId = tagId
                                    selectedCategory = placeCategories[location] ?: "Other"
                                    modifyTag = true
                                    showTagDialog = true
                                } else {
                                    Toast.makeText(context, "Failed to retrieve tag details for modification.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                val (tagId, _) = viewModel.getTagDetailsForLocation(location)
                                if (tagId != null) {
                                    viewModel.deletePlaceTag(tagId)
                                } else {
                                    Toast.makeText(context, "Failed to delete tag: Tag ID not found.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        // Show dialog to add or modify a tag with category
        if (showTagDialog) {
            AddTagDialog(
                placeName = placeName,
                placeDetails = placeDetails,
                initialCategory = selectedCategory,
                onAddTag = { tag, category ->
                    selectedPlace?.let { latLng ->
                        val address = placeDetails?.address ?: "Unknown Address"
                        if (modifyTag && selectedTagId != null) {
                            viewModel.modifyPlaceTag(selectedTagId!!, latLng, tag, category)
                        } else {
                            viewModel.savePlaceTag(latLng, tag, address, category)
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
private fun getMarkerHueFromColor(color: Int): Float {
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

@Composable
fun SearchBar(modifier: Modifier = Modifier, onPlaceSelected: (LatLng, String, String?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            place.latLng?.let {
                onPlaceSelected(it, place.name ?: "Unnamed Place", place.address)
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR && result.data != null) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Toast.makeText(context, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Button(
        onClick = {
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTagDialog(placeName: String, placeDetails: Place?, initialCategory: String, onAddTag: (String, String) -> Unit, onDismiss: () -> Unit) {
    var tag by remember { mutableStateOf(placeName) }
    val defaultCategories = listOf("Vet Clinic", "Grooming Salon", "Pet Hotel", "Dog Park", "Pet Store", "Training Center", "Pet-Friendly Cafe", "Pharmacy", "Other")
    var selectedCategory by remember { mutableStateOf(initialCategory.ifEmpty { defaultCategories.first() }) }
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var addressText by remember { mutableStateOf(placeDetails?.address ?: "Address not available") }
    val context = LocalContext.current

    LaunchedEffect(placeDetails) {
        if (placeDetails?.address == null && placeDetails?.latLng != null) {
            // Perform reverse geocoding to get address
            val geocoder = Geocoder(context)
            try {
                @Suppress("DEPRECATION") // For backward compatibility
                val addresses = geocoder.getFromLocation(
                    placeDetails.latLng!!.latitude,
                    placeDetails.latLng!!.longitude,
                    1
                )
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    addressText = address.getAddressLine(0) ?: "Unknown Address"
                }
            } catch (e: Exception) {
                Log.e("AddTagDialog", "Error getting address: ${e.message}")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add or Modify Tag") },
        text = {
            Column {
                TextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = isCategoryExpanded, onExpandedChange = { isCategoryExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .focusProperties { canFocus = false },
                        readOnly = true,
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Category", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme. colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) }
                    )
                    ExposedDropdownMenu (
                        expanded = isCategoryExpanded,
                        onDismissRequest = { isCategoryExpanded = false }
                    ) {
                        defaultCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    isCategoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (placeDetails != null) {
                    Text("Address: ${placeDetails.address ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onAddTag(tag, selectedCategory)
                    }
                ) {
                    Text("Save")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun CategoryLegend(
    selectedCategory: String? = null,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        "Vet Clinic", "Grooming Salon", "Pet Hotel", "Dog Park", "Pet Store", "Training Center", "Pet-Friendly Cafe", "Pharmacy", "Other"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val color = getMarkerColorForCategory(category)
            val isSelected = category == selectedCategory

            // Make each category item clickable
            Box(
                modifier = Modifier
                    .clickable { onCategorySelected(category) }
                    .background(
                        color = if (isSelected) Color(color).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color(color)
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(color), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(color)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TaggedPlaceItem(tag: String, category: String, location: LatLng, address: String, onNavigate: () -> Unit, onModify: () -> Unit, onDelete: () -> Unit) {
    val categoryColor = getMarkerColorForCategory(category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = Color(categoryColor).copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color indicator circle
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(categoryColor), CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                tag,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Address: $address",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Category: $category",
            style = MaterialTheme.typography.titleMedium,
            color = Color(categoryColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Navigate")
            }

            OutlinedButton(onClick = onModify) {
                Text("Edit")
            }

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
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