package com.example.furfamily.profile

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.Routes
import com.example.furfamily.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPetScreen(navController: NavController, userId: String, petId: String) {
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val pets by profileViewModel.pets.observeAsState(emptyList())
    val selectedPet = remember(petId, pets) { pets.find { it.petId == petId } }
    var profileImageUrl by remember { mutableStateOf(selectedPet?.profileImageUrl ?: "") }
    var name by remember { mutableStateOf(selectedPet?.name ?: "") }
    var type by remember { mutableStateOf(selectedPet?.type ?: "Please choose one") }
    val typeOptions = listOf("Dog", "Cat")
    var isTypeExpanded by remember { mutableStateOf(false) }
    var breed by remember { mutableStateOf(selectedPet?.breed ?: "") }
    var selectedSex by remember { mutableStateOf(selectedPet?.selectedSex ?: "Please choose one") }
    val sexOptions = listOf("Neutered male", "Spayed female", "Intact male", "Intact female")
    var isSexExpanded by remember { mutableStateOf(false) }
    var allergy by remember { mutableStateOf(selectedPet?.allergy ?: "") }
    var birthDate by remember { mutableStateOf(selectedPet?.birthDate ?: Date()) }
    val datePickerState = rememberDatePickerState(birthDate.time)
    var showDatePicker by remember { mutableStateOf(false) }

    // Update form fields when selectedPet changes
    LaunchedEffect(selectedPet) {
        selectedPet?.let {
            profileImageUrl = it.profileImageUrl
            name = it.name
            type = it.type
            breed = it.breed
            selectedSex = it.selectedSex
            allergy = it.allergy
            birthDate = it.birthDate
            datePickerState.selectedDateMillis = it.birthDate.time
        }
    }

    // Load pets when the screen is displayed
    LaunchedEffect(Unit) {
        profileViewModel.loadPetsProfile(userId)
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                profileViewModel.uploadPetImage(userId, petId, it) { uploadedImageUrl ->
                    profileImageUrl = uploadedImageUrl
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Pet Details") },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate(Routes.Profile.value) {
                            popUpTo(Routes.Profile.value) { inclusive = true }
                        }
                    }) {
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
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

                // Pet Allergy Note
                TextField(
                    value = allergy,
                    onValueChange = { allergy = it },
                    label = { Text("Allergy Note", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

                // Pet Birth Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
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
                            Button(onClick = {
                                showDatePicker = false
                                birthDate = Date(datePickerState.selectedDateMillis ?: birthDate.time)
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = {
                                showDatePicker = false
                            }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        if (name.isNotBlank() && type != "Please choose one" && selectedSex != "Please choose one" && breed.isNotBlank()) {
                            val petToSave = Pet(
                                petId = petId,
                                name = name,
                                type = type,
                                breed = breed,
                                selectedSex = selectedSex,
                                allergy = allergy,
                                birthDate = birthDate,
                                profileImageUrl = profileImageUrl
                            )

                            profileViewModel.updatePet(userId, petToSave)
                            
                            // Navigate back to profile and refresh
                            navController.navigate(Routes.Profile.value) {
                                popUpTo(Routes.Profile.value) { inclusive = true }
                            }
                            profileViewModel.loadPetsProfile(userId)
                        }
                    },
                    enabled = name.isNotBlank() && type != "Please choose one" && selectedSex != "Please choose one" && breed.isNotBlank(),
                    modifier = Modifier.height(46.dp).width(190.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    )
} 