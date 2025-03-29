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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.Routes
import com.example.furfamily.viewmodel.ProfileViewModel
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
fun ProfileSettingsScreen(navController: NavController, userId: String) {
    val profileViewModel: ProfileViewModel = hiltViewModel()

    LaunchedEffect(userId) {
        profileViewModel.loadUserProfile(userId)
        profileViewModel.loadPetsProfile(userId)
    }

    val pets by profileViewModel.pets.observeAsState(emptyList())
    var selectedPet by remember { mutableStateOf<Pet?>(null) }  // Nullable Pet for initial state
    var showUserProfileSettings by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Automatically select the first pet when the pets list is loaded
    LaunchedEffect(pets) {
        if (selectedPet == null && pets.isNotEmpty()) {
            selectedPet = pets.first()
        } else if (selectedPet != null) {
            // Update selected pet if it exists in the updated list
            selectedPet = pets.find { it.petId == selectedPet?.petId } ?: selectedPet
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
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
                        Text("Pet Profile", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Name: ${pet.name}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Type: ${pet.type}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Breed: ${pet.breed}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Sex: ${pet.selectedSex}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Date of Birth: ${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(pet.birthDate)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Smart Care Assistant
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "FurFamily Smart Care Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextField(
                                value = question,
                                onValueChange = { question = it },
                                label = { Text("Ask a question about ${pet.name}") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    isLoading = true
                                    aiResponse = ""
                                    errorMessage = ""

                                    val userIdFromAuth = FirebaseAuth.getInstance().currentUser?.uid
                                    if (userIdFromAuth != null) {
                                        profileViewModel.askPetAdvice(
                                            userId = userIdFromAuth,
                                            pet = pet,
                                            question = question,
                                            onSuccess = { response ->
                                                aiResponse = response
                                                isLoading = false
                                            },
                                            onError = { error ->
                                                errorMessage = error
                                                isLoading = false
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = question.isNotBlank() && !isLoading
                            ) {
                                Text(if (isLoading) "Thinking..." else "Click Here to Ask")
                            }

                            if (errorMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color.Red,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (aiResponse.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "FurFamily Smart Care Assistant says:",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 100.dp, max = 250.dp)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = aiResponse,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                } ?: Text(text = "No pet selected", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(24.dp))

                // Edit/Add Pet Button
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            selectedPet?.let {
                                navController.navigate("${Routes.EditPet.value}/${it.petId}")
                            }
                        },
                        enabled = selectedPet != null,
                        modifier = Modifier.height(46.dp).width(190.dp)
                    ) {
                        Text("Edit Pet Details")
                    }

                    Button(
                        onClick = {
                            navController.navigate(Routes.AddPet.value)
                        },
                        modifier = Modifier.height(46.dp).width(190.dp)
                    ) {
                        Text("Add New Pet")
                    }
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
            profileViewModel = profileViewModel,
            onDismiss = { showUserProfileSettings = false }
        )
    }
}

//@RequiresApi(Build.VERSION_CODES.O)
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddEditPetScreen(navController: NavController, userId: String) {
//    val profileViewModel: ProfileViewModel = hiltViewModel()
//    val pets by profileViewModel.pets.observeAsState(emptyList())
//
//    // Get petId from navigation arguments
//    val petId = navController.currentBackStackEntry?.arguments?.getString("petId")
//    val selectedPet = remember(petId, pets) { pets.find { it.petId == petId } }
//    val isEditing = selectedPet != null
//
//    var profileImageUrl by remember { mutableStateOf(selectedPet?.profileImageUrl ?: "") }
//    var name by remember { mutableStateOf(selectedPet?.name ?: "") }
//    var type by remember { mutableStateOf(selectedPet?.type ?: "Please choose one") }
//    val typeOptions = listOf("Dog", "Cat")
//    var isTypeExpanded by remember { mutableStateOf(false) }
//    var breed by remember { mutableStateOf(selectedPet?.breed ?: "") }
//    var selectedSex by remember { mutableStateOf(selectedPet?.selectedSex ?: "Please choose one") }
//    val sexOptions = listOf("Neutered male", "Spayed female", "Intact male", "Intact female")
//    var isSexExpanded by remember { mutableStateOf(false) }
//    var birthDate by remember { mutableStateOf(selectedPet?.birthDate ?: Date()) }
//    val calendar = Calendar.getInstance()
//    val datePickerState = rememberDatePickerState(birthDate.time)
//    var showDatePicker by remember { mutableStateOf(false) }
//
//    // Generate a new petId if not editing
//    val newPetId = selectedPet?.petId ?: FirebaseDatabase.getInstance().getReference("pets/$userId").push().key!!
//
//    // Update form fields when selectedPet changes
//    LaunchedEffect(selectedPet) {
//        selectedPet?.let {
//            profileImageUrl = it.profileImageUrl
//            name = it.name
//            type = it.type
//            breed = it.breed
//            selectedSex = it.selectedSex
//            birthDate = it.birthDate
//            datePickerState.selectedDateMillis = it.birthDate.time
//        }
//    }
//
//    // Load pets when the screen is displayed
//    LaunchedEffect(Unit) {
//        profileViewModel.loadPetsProfile(userId)
//    }
//
//    // Image picker launcher
//    val imagePickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent(),
//        onResult = { uri: Uri? ->
//            uri?.let {
//                profileViewModel.uploadPetImage(userId, newPetId, it) { uploadedImageUrl ->
//                    profileImageUrl = uploadedImageUrl // Update the profileImageUrl after uploading
//                }
//            }
//        }
//    )
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(if (isEditing) "Edit Pet" else "Add Pet") },
//                navigationIcon = {
//                    IconButton(onClick = {
//                        navController.navigate(Routes.Profile.value) {
//                            popUpTo(Routes.Profile.value) { inclusive = true }
//                        }
//                    }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        content = { paddingValues ->
//            Column(
//                modifier = Modifier
//                    .padding(paddingValues)
//                    .padding(16.dp)
//                    .fillMaxSize(),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                // Pet Profile Image
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Box(
//                        contentAlignment = Alignment.BottomEnd,
//                        modifier = Modifier.size(150.dp)
//                    ) {
//                        Image(
//                            painter = rememberImagePainter(profileImageUrl),
//                            contentDescription = "Profile Picture",
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .background(Color.LightGray, CircleShape)
//                                .clip(CircleShape)
//                        )
//                        IconButton(
//                            onClick = { imagePickerLauncher.launch("image/*") },
//                            modifier = Modifier
//                                .size(30.dp)
//                                .padding(4.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Filled.AddCircle,
//                                contentDescription = "Pick Image"
//                            )
//                        }
//                    }
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Pet Name
//                TextField(
//                    value = name,
//                    onValueChange = { name = it },
//                    label = { Text("Pet Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
//                    colors = TextFieldDefaults.textFieldColors(
//                        focusedLabelColor = MaterialTheme.colorScheme.primary,
//                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
//                        containerColor = MaterialTheme.colorScheme.surface,
//                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
//                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
//                    ),
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Pet Type
//                ExposedDropdownMenuBox(expanded = isTypeExpanded, onExpandedChange = { isTypeExpanded = it }) {
//                    TextField(
//                        modifier = Modifier
//                            .menuAnchor()
//                            .fillMaxWidth(),
//                        readOnly = true,
//                        value = type,
//                        onValueChange = {},
//                        label = { Text("Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
//                        colors = TextFieldDefaults.textFieldColors(
//                            focusedLabelColor = MaterialTheme.colorScheme.primary,
//                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
//                            containerColor = MaterialTheme.colorScheme.surface,
//                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
//                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        ),
//                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeExpanded) }
//                    )
//                    ExposedDropdownMenu(
//                        expanded = isTypeExpanded,
//                        onDismissRequest = { isTypeExpanded = false }
//                    ) {
//                        typeOptions.forEach { selectionOption ->
//                            DropdownMenuItem(
//                                text = { Text(selectionOption) },
//                                onClick = {
//                                    type = selectionOption
//                                    isTypeExpanded = false
//                                },
//                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
//                            )
//                        }
//                    }
//                }
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Pet Breed
//                TextField(
//                    value = breed,
//                    onValueChange = { breed = it },
//                    label = { Text("Breed", color = MaterialTheme.colorScheme.onSurfaceVariant) },
//                    colors = TextFieldDefaults.textFieldColors(
//                        focusedLabelColor = MaterialTheme.colorScheme.primary,
//                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
//                        containerColor = MaterialTheme.colorScheme.surface,
//                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
//                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
//                    ),
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Pet Sex
//                ExposedDropdownMenuBox(expanded = isSexExpanded, onExpandedChange = { isSexExpanded = it }) {
//                    TextField(
//                        modifier = Modifier
//                            .menuAnchor()
//                            .fillMaxWidth(),
//                        readOnly = true,
//                        value = selectedSex,
//                        onValueChange = {},
//                        label = { Text("Sex", color = MaterialTheme.colorScheme.onSurfaceVariant) },
//                        colors = TextFieldDefaults.textFieldColors(
//                            focusedLabelColor = MaterialTheme.colorScheme.primary,
//                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
//                            containerColor = MaterialTheme.colorScheme.surface,
//                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
//                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        ),
//                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSexExpanded) }
//                    )
//                    ExposedDropdownMenu(
//                        expanded = isSexExpanded,
//                        onDismissRequest = { isSexExpanded = false }
//                    ) {
//                        sexOptions.forEach { selectionOption ->
//                            DropdownMenuItem(
//                                text = { Text(selectionOption) },
//                                onClick = {
//                                    selectedSex = selectionOption
//                                    isSexExpanded = false
//                                },
//                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
//                            )
//                        }
//                    }
//                }
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Pet Birth Date
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    OutlinedButton(onClick = { showDatePicker = true },
//                        modifier = Modifier
//                            .weight (1f)
//                            .height(46.dp)
//                    ) { Text("Enter Date of Birth") }
//                    Spacer(modifier = Modifier.width(16.dp))
//                    Text(
//                        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(birthDate),
//                        style = MaterialTheme.typography.bodyMedium,
//                        modifier = Modifier.weight(1f)
//                    )
//                }
//
//                if (showDatePicker) {
//                    DatePickerDialog(
//                        onDismissRequest = { showDatePicker = false },
//                        confirmButton = {
//                            TextButton(onClick = {
//                                showDatePicker = false
//                                birthDate = Date(datePickerState.selectedDateMillis ?: birthDate.time)
//                            }) { Text("OK") }
//                        },
//                        dismissButton = {
//                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
//                        }
//                    ) {
//                        DatePicker(state = datePickerState)
//                    }
//                }
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Save Button
//                Button(
//                    onClick = {
//                        if (name.isNotBlank() && type != "Please choose one" && selectedSex != "Please choose one" && breed.isNotBlank()) {
//                            val petToSave = Pet(
//                                petId = newPetId,
//                                name = name,
//                                type = type,
//                                breed = breed,
//                                selectedSex = selectedSex,
//                                birthDate = birthDate,
//                                profileImageUrl = profileImageUrl
//                            )
//
//                            if (isEditing) {
//                                profileViewModel.updatePet(userId, petToSave)
//                            } else {
//                                profileViewModel.addPet(userId, petToSave)
//                            }
//
//                            // Navigate back to profile and refresh
//                            navController.navigate(Routes.Profile.value) {
//                                popUpTo(Routes.Profile.value) { inclusive = true }
//                            }
//                            profileViewModel.loadPetsProfile(userId)
//                        }
//                    },
//                    enabled = name.isNotBlank() && type != "Please choose one" && selectedSex != "Please choose one" && breed.isNotBlank(),
//                    modifier = Modifier.height(46.dp).width(190.dp)
//                ) {
//                    Text(if (isEditing) "Save Changes" else "Save New Pet")
//                }
//            }
//        }
//    )
//}