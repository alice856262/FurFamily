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
                        Text(text = "Sex: ${pet.selectedSex}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Date of Birth: ${SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(pet.birthDate)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(text = "Breed: ${pet.breed}", style = MaterialTheme.typography.bodyMedium)
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedPet != null
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
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Log Out") }
            }
        }
    )

    // User Profile Settings Dialog
    if (showUserProfileSettings) {
        AlertDialog(
            onDismissRequest = { showUserProfileSettings = false },
            title = { Text("User Profile Settings") },
            text = { UserProfileSetting(viewModel = viewModel, userId = userId) },
            confirmButton = {
                Button(onClick = { showUserProfileSettings = false }) {
                    Text("Close")
                }
            }
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
    var breed by remember { mutableStateOf(selectedPet?.breed ?: "") }
    var selectedSex by remember { mutableStateOf(selectedPet?.selectedSex ?: "Please choose one") }
    var birthDate by remember { mutableStateOf(selectedPet?.birthDate ?: Date()) }
    val sexOptions = listOf("Neutered male", "Spayed female", "Intact male", "Intact female")
    var isExpanded by remember { mutableStateOf(false) }
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

                Spacer(modifier = Modifier.height(16.dp))

                // Pet Name
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Pet Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pet Breed
                TextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pet Sex
                ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        readOnly = true,
                        value = selectedSex,
                        onValueChange = {},
                        label = { Text("Sex") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        sexOptions.forEach { selectionOption ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedSex = selectionOption
                                    isExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pet Birth Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text("Select Date of Birth")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(birthDate),
                        style = MaterialTheme.typography.bodyMedium
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
                        if (name.isNotBlank() && breed.isNotBlank()) {
                            val petToSave = Pet(
                                petId = newPetId,
                                name = name,
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEditing) "Save Changes" else "Add Pet")
                }
            }
        }
    )
}

//@RequiresApi(0)
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileSettingsScreen(navController: NavController, viewModel: ViewModel, userId: String) {
//    // Load the user profile when the composable enters the composition
//    LaunchedEffect(userId) {
//        Log.e("userId", "${userId}")
//        viewModel.loadUserProfile(userId)
//    }
//    // Observe UserProfile LiveData and provide a default empty UserProfile if null
//    val userProfile by viewModel.userProfile.observeAsState()
//    if (userProfile == null) {
//        return@ProfileSettingsScreen
//    }
//
//    val profileImageUri by viewModel.profileImageUri.observeAsState()
//    // Check if profile image URI is null
//    val profileImage = profileImageUri ?: userProfile?.profileImageUrl?.let { Uri.parse(it) }
//
//    var firstName by remember { mutableStateOf(userProfile!!.firstName) }
//    var editingFirstName by remember { mutableStateOf(false) }
//    var lastName by remember { mutableStateOf(userProfile!!.lastName) }
//    var editingLastName by remember { mutableStateOf(false) }
//    val gender = listOf("Male", "Female")
//    var isExpanded by rememberSaveable { mutableStateOf(false) }
//    var selectedGender by rememberSaveable { mutableStateOf(userProfile?.selectedGender ?: gender[0]) }
//    var phone by remember { mutableStateOf(userProfile!!.phone) }
//    var editingPhone by remember { mutableStateOf(false) }
//    val calendar = Calendar.getInstance()
//    val datePickerState = rememberDatePickerState(
//        initialSelectedDateMillis = Instant.now().toEpochMilli()
//    )
//    var showDatePicker by rememberSaveable { mutableStateOf(false) }
//    var birthDate by rememberSaveable { mutableStateOf(userProfile?.birthDate?.time ?: calendar.timeInMillis) }
//    var showSnackbar by rememberSaveable { mutableStateOf(false) }
//    var snackbarMessage by rememberSaveable { mutableStateOf("") }
//    val isSetFormValid = isSetFormValid(firstName, lastName, phone)
//
//    // Remember launcher for activity result to handle image picking
//    val imagePickerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent(),
//        onResult = { uri: Uri? ->
//            uri?.let {
//                viewModel.setImageUri(it)  // Set the URI in the ViewModel
//                viewModel.uploadImageToStorage(it)  // Trigger upload
//            }
//        }
//    )
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//            .verticalScroll(rememberScrollState()),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Top
//    ) {
//        Text(
//            text = "Profile Settings",
//            style = MaterialTheme.typography.titleLarge,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier
//                .padding(top = 12.dp)
//                .padding(bottom = 12.dp)
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Column {
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalAlignment = Alignment.CenterHorizontally,
//                verticalArrangement = Arrangement.Center
//            ) {
//                Box(
//                    contentAlignment = Alignment.BottomEnd,
//                    modifier = Modifier.size(120.dp)
//                ) {
//                    Image(
//                        painter = rememberImagePainter(profileImage),
//                        contentDescription = "Profile Picture",
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .background(Color.LightGray, CircleShape)
//                            .clip(CircleShape)
//                            .align(Alignment.Center)
//                    )
//                    IconButton(
//                        onClick = { imagePickerLauncher.launch("image/*") },
//                        modifier = Modifier
//                            .size(30.dp)
//                            .padding(4.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.AddCircle,
//                            contentDescription = "Pick Image"
//                        )
//                    }
//                }
//            }
//
//            Column(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(text = "First Name: ", style = MaterialTheme.typography.titleMedium)
//                if (editingFirstName) {
//                    OutlinedTextField(
//                        value = firstName,
//                        onValueChange = { firstName = it },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        trailingIcon = {
//                            IconButton(onClick = { editingFirstName = false }) {
//                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
//                            }
//                        }
//                    )
//                } else {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text(
//                            text = firstName,
//                            style = MaterialTheme.typography.titleMedium,
//                            modifier = Modifier
//                                .weight(1f)
//                                .clickable { editingFirstName = true }
//                                .padding(12.dp)
//                        )
//                        IconButton(onClick = { editingFirstName = true }) {
//                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
//                        }
//                    }
//                }
//            }
//            Column(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(
//                    text = "Last Name: ",
//                    style = MaterialTheme.typography.titleMedium
//                )
//                if (editingLastName) {
//                    OutlinedTextField(
//                        value = lastName,
//                        onValueChange = { lastName = it },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        trailingIcon = {
//                            IconButton(onClick = { editingLastName = false }) {
//                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
//                            }
//                        }
//                    )
//                } else {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text(
//                            text = lastName,
//                            style = MaterialTheme.typography.titleMedium,
//                            modifier = Modifier
//                                .weight(1f)
//                                .clickable { editingLastName = true }
//                                .padding(12.dp)
//                        )
//                        IconButton(onClick = { editingLastName = true }) {
//                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
//                        }
//                    }
//                }
//            }
//            Column(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(
//                    text = "Phone Number: ",
//                    style = MaterialTheme.typography.titleMedium
//                )
//                if (editingPhone) {
//                    OutlinedTextField(
//                        value = phone,
//                        onValueChange = { phone = it },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        trailingIcon = {
//                            IconButton(onClick = { editingPhone = false }) {
//                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
//                            }
//                        }
//                    )
//                } else {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text(
//                            text = phone,
//                            style = MaterialTheme.typography.titleMedium,
//                            modifier = Modifier
//                                .weight(1f)
//                                .clickable { editingPhone = true }
//                                .padding(12.dp)
//                        )
//                        IconButton(onClick = { editingPhone = true }) {
//                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
//                        }
//                    }
//                }
//            }
//
//            Column(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text(
//                    text = "Gender: ",
//                    style = MaterialTheme.typography.titleMedium
//                )
//                Spacer(modifier = Modifier.height(12.dp))
//                ExposedDropdownMenuBox(
//                    expanded = isExpanded,
//                    onExpandedChange = { isExpanded = it }) {
//                    TextField(
//                        modifier = Modifier
//                            .menuAnchor()
//                            .fillMaxWidth()
//                            .focusProperties {
//                                canFocus = false
//                            }
//                            .padding(bottom = 8.dp),
//                        readOnly = true,
//                        value = selectedGender,
//                        onValueChange = {},
//                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
//                        colors = TextFieldDefaults.textFieldColors(
//                            containerColor = Color.Transparent,
//                            focusedIndicatorColor = Color.Black,
//                            unfocusedIndicatorColor = Color.Black
//                        )
//                    )
//                    ExposedDropdownMenu(
//                        expanded = isExpanded,
//                        onDismissRequest = { isExpanded = false })
//                    {
//                        gender.forEach { selectionOption ->
//                            DropdownMenuItem(
//                                text = { Text(selectionOption) },
//                                onClick = {
//                                    selectedGender = selectionOption
//                                    isExpanded = false
//                                },
//                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
//                            )
//                        }
//                    }
//                }
//            }
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 14.dp)
//            ) { Text("Date of Birth:", style = MaterialTheme.typography.titleMedium) }
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                OutlinedButton(
//                    onClick = { showDatePicker = true },
//                    modifier = Modifier
//                        .weight(1f)
//                        .height(46.dp)
//                ) { Text(text = "Click here") }
//                Spacer(modifier = Modifier.width(12.dp))
//                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
//                Text(
//                    text = "${formatter.format(Date(birthDate))}",
//                    modifier = Modifier.weight(1f)
//                )
//            }
//            if (showDatePicker) {
//                DatePickerDialog(
//                    onDismissRequest = { showDatePicker = false },
//                    confirmButton = {
//                        TextButton(onClick = {
//                            showDatePicker = false
//                            birthDate = datePickerState.selectedDateMillis!!
//                        }) { Text(text = "OK") }
//                    },
//                    dismissButton = {
//                        TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel") }
//                    }
//                ) { DatePicker(state = datePickerState) }
//            }
//            Spacer(modifier = Modifier.height(12.dp))
//        }
//        Spacer(modifier = Modifier.height(8.dp))
//        Row(verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 4.dp, horizontal = 12.dp)) {
//            Switch(
//                checked = allowLocation,
//                onCheckedChange = { allowLocation = it },
//                modifier = Modifier.padding(end = 16.dp)
//            )
//            Text("Share Location Data", style = MaterialTheme.typography.titleMedium)
//        }
//        Row(verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 4.dp, horizontal = 12.dp)
//        ) {
//            Switch(
//                checked = allowActivityShare,
//                onCheckedChange = { allowActivityShare = it },
//                modifier = Modifier.padding(end = 16.dp)
//            )
//            Text("Share Activity Data", style = MaterialTheme.typography.titleMedium)
//        }
//        Row(verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 4.dp, horizontal = 12.dp)) {
//            Switch(
//                checked = allowHealthDataShare,
//                onCheckedChange = { allowHealthDataShare = it },
//                modifier = Modifier.padding(end = 16.dp)
//            )
//            Text("Share Health Data", style = MaterialTheme.typography.titleMedium)
//        }
//        Spacer(modifier = Modifier.height(24.dp))
//        Button(
//            onClick = {
//                userProfile?.let { profile ->
//                    val updatedProfile = profile.copy(
//                        firstName = firstName,
//                        lastName = lastName,
//                        phone = phone,
//                        selectedGender = selectedGender,
//                        birthDate = Date(birthDate),
//                        allowLocation = allowLocation,
//                        allowActivityShare = allowActivityShare,
//                        allowHealthDataShare = allowHealthDataShare
//                    )
//                    viewModel.updateUser(updatedProfile) {
//                        Log.d("ProfileUpdate", "Profile updated successfully.")
//                        snackbarMessage = "Profile updated successfully!"
//                        showSnackbar = true
//                    }
//                } ?: run {
//                    Log.d("ProfileUpdate", "Fail to update profile.")
//                }
//            },
//            enabled = isSetFormValid,
//            modifier = Modifier.fillMaxWidth()
//        ) { Text("Save Changes") }
//        if (showSnackbar) {
//            Snackbar(
//                action = { Button(onClick = { showSnackbar = false }) { Text("OK") } }
//            ) { Text(snackbarMessage) }
//        }
//    }
//}
//
//fun isSetFormValid(firstName: String, lastName: String, phone: String): Boolean {
//    return firstName.isNotEmpty() && lastName.isNotEmpty() && phone.isNotEmpty()
//}
