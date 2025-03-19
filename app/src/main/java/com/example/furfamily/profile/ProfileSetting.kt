package com.example.furfamily.profile

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.Routes
import com.example.furfamily.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    navController: NavController,
    viewModel: ViewModel,
    userId: String
) {
    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
        viewModel.loadPetsProfile(userId)
    }

    // Observing LiveData from ViewModel
    val pets by viewModel.pets.observeAsState(emptyList())
    var selectedPet by remember { mutableStateOf<Pet?>(null) }  // Nullable Pet for initial state
    var showUserProfileSettings by remember { mutableStateOf(false) }

    // Automatically select the first pet when the pets list is loaded
    LaunchedEffect(pets) {
        if (selectedPet == null && pets.isNotEmpty()) {
            selectedPet = pets.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showUserProfileSettings = true }) {
                        Icon(Icons.Default.Person, contentDescription = "User Profile Settings")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pet Carousel
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pets) { pet ->
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(if (selectedPet?.petId == pet.petId) Color.Gray else Color.LightGray)
                                .clickable { selectedPet = pet },
                            contentAlignment = Alignment.Center
                        ) {
                            if (pet.profileImageUrl.isNotEmpty()) {
                                Image(
                                    painter = rememberImagePainter(pet.profileImageUrl),
                                    contentDescription = pet.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = pet.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Selected Pet Details
                selectedPet?.let { pet ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Pet Name: ${pet.name}", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Type: ${pet.type}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Breed: ${pet.breed}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Sex: ${pet.selectedSex}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Date of Birth: ${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(pet.birthDate)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } ?: Text(text = "No pet selected", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(24.dp))

                // Edit Pet Button
                Button(
                    onClick = {
                        selectedPet?.let {
                            viewModel.setSelectedPet(it) // Set the selected pet in the ViewModel
                            navController.navigate(Routes.AddEditPet.value) // Navigate with Edit functionality
                        }
                    },
                    enabled = selectedPet != null,
                    modifier = Modifier.height(46.dp).width(190.dp)
                ) {
                    Text("Edit Pet Details")
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Add New Pet Button
                Button(
                    onClick = {
                        viewModel.setSelectedPet(null) // Clear selected pet
                        navController.navigate(Routes.AddEditPet.value) // Navigate with Add functionality
                    },
                    modifier = Modifier.height(46.dp).width(190.dp)
                ) {
                    Text("Add New Pet")
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Sign out
                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
                        val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)
                        googleSignInClient.signOut().addOnCompleteListener {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(Routes.Login.value) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(46.dp).width(190.dp)
                ) { Text("Log Out") }
            }
        }
    )

    if (showUserProfileSettings) {
        UserProfileSetting(
            viewModel = viewModel,
            onDismiss = { showUserProfileSettings = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPetScreen(navController: NavController, viewModel: ViewModel, userId: String) {
    val selectedPet by viewModel.selectedPet.observeAsState()
    val isEditing = selectedPet != null

    // Form fields
    var profileImageUrl by remember { mutableStateOf(selectedPet?.profileImageUrl ?: "") }
    var name by remember { mutableStateOf(selectedPet?.name ?: "") }
    var type by remember { mutableStateOf(selectedPet?.type ?: "Please choose one") }
    val typeOptions = listOf("Dog", "Cat")
    var isTypeExpanded by remember { mutableStateOf(false) }
    var breed by remember { mutableStateOf(selectedPet?.breed ?: "") }
    var selectedSex by remember { mutableStateOf(selectedPet?.selectedSex ?: "Please choose one") }
    val sexOptions = listOf("Neutered male", "Spayed female", "Intact male", "Intact female")
    var isSexExpanded by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf(selectedPet?.birthDate ?: Date()) }
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(birthDate.time)
    var showDatePicker by remember { mutableStateOf(false) }

    // Generate a new petId if not editing
    val newPetId = selectedPet?.petId ?: FirebaseDatabase.getInstance().getReference("pets/$userId").push().key!!

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.uploadPetImage(userId, newPetId, it) { uploadedImageUrl ->
                    profileImageUrl = uploadedImageUrl // Update the profileImageUrl after uploading
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Pet" else "Add Pet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pet Profile Image
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier.size(150.dp)
                    ) {
                        Image(
                            painter = rememberImagePainter(profileImageUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray, CircleShape)
                                .clip(CircleShape)
                        )
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(30.dp)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircle,
                                contentDescription = "Pick Image"
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Pet Name
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Pet Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Pet Type
                ExposedDropdownMenuBox(expanded = isTypeExpanded, onExpandedChange = { isTypeExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = type,
                        onValueChange = {},
                        label = { Text("Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isTypeExpanded,
                        onDismissRequest = { isTypeExpanded = false }
                    ) {
                        typeOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    type = selectionOption
                                    isTypeExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Pet Breed
                TextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Pet Sex
                ExposedDropdownMenuBox(expanded = isSexExpanded, onExpandedChange = { isSexExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = selectedSex,
                        onValueChange = {},
                        label = { Text("Sex", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSexExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isSexExpanded,
                        onDismissRequest = { isSexExpanded = false }
                    ) {
                        sexOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedSex = selectionOption
                                    isSexExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Pet Birth Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight (1f)
                            .height(46.dp)
                    ) { Text("Enter Date of Birth") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(birthDate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                birthDate = Date(datePickerState.selectedDateMillis ?: birthDate.time)
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        if (name.isNotBlank() && type.isNotBlank() && breed.isNotBlank()) {
                            val petToSave = Pet(
                                petId = newPetId,
                                name = name,
                                type = type,
                                breed = breed,
                                selectedSex = selectedSex,
                                birthDate = birthDate,
                                profileImageUrl = profileImageUrl
                            )

                            if (isEditing) {
                                viewModel.updatePet(userId, petToSave)
                            } else {
                                viewModel.addPet(userId, petToSave)
                            }

                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.height(46.dp).width(190.dp)
                ) {
                    Text(if (isEditing) "Save Changes" else "Save New Pet")
                }
            }
        }
    )
}