package com.example.furfamily.profile

import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.Routes
import com.example.furfamily.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(0)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(navController: NavController, viewModel: ViewModel, userId: String) {
    // Load the user profile when the composable enters the composition
    LaunchedEffect(userId) {
        Log.e("userId", "${userId}")
        viewModel.loadUserProfile(userId)
    }
    // Observe UserProfile LiveData and provide a default empty UserProfile if null
    val userProfile by viewModel.userProfile.observeAsState()
    if (userProfile == null) {
        return@ProfileSettingsScreen
    }

    val profileImageUri by viewModel.profileImageUri.observeAsState()

    var firstName by remember { mutableStateOf(userProfile!!.firstName) }
    var editingFirstName by remember { mutableStateOf(false) }
    var lastName by remember { mutableStateOf(userProfile!!.lastName) }
    var editingLastName by remember { mutableStateOf(false) }
    val gender = listOf("Male", "Female")
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedGender by rememberSaveable { mutableStateOf(userProfile?.selectedGender ?: gender[0]) }
    var phone by remember { mutableStateOf(userProfile!!.phone) }
    var editingPhone by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var birthDate by rememberSaveable { mutableStateOf(userProfile?.birthDate?.time ?: calendar.timeInMillis) }
    var allowLocation by remember { mutableStateOf(userProfile!!.allowLocation) }
    var allowActivityShare by remember { mutableStateOf(userProfile!!.allowActivityShare) }
    var allowHealthDataShare by remember { mutableStateOf(userProfile!!.allowHealthDataShare) }
    var showSnackbar by rememberSaveable { mutableStateOf(false) }
    var snackbarMessage by rememberSaveable { mutableStateOf("") }
    val isSetFormValid = isSetFormValid(firstName, lastName, phone)

    // Remember launcher for activity result to handle image picking
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.setImageUri(it)  // Set the URI in the ViewModel
                viewModel.uploadImageToStorage(it)  // Trigger upload
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Profile Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(bottom = 12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.size(120.dp)
                ) {
                    Image(
                        painter = rememberImagePainter(profileImageUri),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.LightGray, CircleShape)
                            .clip(CircleShape)
                            .align(Alignment.Center)
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

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "First Name: ", style = MaterialTheme.typography.titleMedium)
                if (editingFirstName) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { editingFirstName = false }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = firstName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingFirstName = true }
                                .padding(12.dp)
                        )
                        IconButton(onClick = { editingFirstName = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Last Name: ",
                    style = MaterialTheme.typography.titleMedium
                )
                if (editingLastName) {
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { editingLastName = false }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingLastName = true }
                                .padding(12.dp)
                        )
                        IconButton(onClick = { editingLastName = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Phone Number: ",
                    style = MaterialTheme.typography.titleMedium
                )
                if (editingPhone) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { editingPhone = false }) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editingPhone = true }
                                .padding(12.dp)
                        )
                        IconButton(onClick = { editingPhone = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Gender: ",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it }) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .focusProperties {
                                canFocus = false
                            }
                            .padding(bottom = 8.dp),
                        readOnly = true,
                        value = selectedGender,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Black,
                            unfocusedIndicatorColor = Color.Black
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false })
                    {
                        gender.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedGender = selectionOption
                                    isExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) { Text("Date of Birth:", style = MaterialTheme.typography.titleMedium) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) { Text(text = "Click here") }
                Spacer(modifier = Modifier.width(12.dp))
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
                Text(
                    text = "${formatter.format(Date(birthDate))}",
                    modifier = Modifier.weight(1f)
                )
            }
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            birthDate = datePickerState.selectedDateMillis!!
                        }) { Text(text = "OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel") }
                    }
                ) { DatePicker(state = datePickerState) }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp)) {
            Switch(
                checked = allowLocation,
                onCheckedChange = { allowLocation = it },
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("Share Location Data", style = MaterialTheme.typography.titleMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp)
        ) {
            Switch(
                checked = allowActivityShare,
                onCheckedChange = { allowActivityShare = it },
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("Share Activity Data", style = MaterialTheme.typography.titleMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 12.dp)) {
            Switch(
                checked = allowHealthDataShare,
                onCheckedChange = { allowHealthDataShare = it },
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("Share Health Data", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                userProfile?.let { profile ->
                    val updatedProfile = profile.copy(
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        selectedGender = selectedGender,
                        birthDate = Date(birthDate),
                        allowLocation = allowLocation,
                        allowActivityShare = allowActivityShare,
                        allowHealthDataShare = allowHealthDataShare
                    )
                    viewModel.updateUser(updatedProfile) {
                        Log.d("ProfileUpdate", "Profile updated successfully.")
                        snackbarMessage = "Profile updated successfully!"
                        showSnackbar = true
                    }
                } ?: run {
                    Log.d("ProfileUpdate", "Fail to update profile.")
                }
            },
            enabled = isSetFormValid,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Changes") }
        if (showSnackbar) {
            Snackbar(
                action = { Button(onClick = { showSnackbar = false }) { Text("OK") } }
            ) { Text(snackbarMessage) }
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

fun isSetFormValid(firstName: String, lastName: String, phone: String): Boolean {
    return firstName.isNotEmpty() && lastName.isNotEmpty() && phone.isNotEmpty()
}
