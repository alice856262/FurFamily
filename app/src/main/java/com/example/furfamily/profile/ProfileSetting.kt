package com.example.furfamily.profile

import android.os.Build
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.furfamily.R
import com.example.furfamily.Routes
import com.example.furfamily.viewmodel.ProfileViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 10.dp)
                        )
                        Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                },
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
                        Text(text = "Allergy: ${if (pet.allergy.isNullOrEmpty()) "no" else pet.allergy}", style = MaterialTheme.typography.bodyMedium)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.chatbot),
                                    contentDescription = "AI Assistant Icon",
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "FurFamily Smart Care Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

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
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "FurFamily Smart Care Assistant says:",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
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
