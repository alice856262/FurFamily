package com.example.furfamily.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furfamily.health.HealthRecord
import com.example.furfamily.network.ChatRequest
import com.example.furfamily.network.Message
import com.example.furfamily.network.RetrofitInstance
import com.example.furfamily.nutrition.Feeding
import com.example.furfamily.profile.Pet
import com.example.furfamily.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _pets = MutableLiveData<List<Pet>>()
    val pets: LiveData<List<Pet>> = _pets

    private val _petProfileImages = MutableLiveData<Map<String, Uri>>()
    val petProfileImages: LiveData<Map<String, Uri>> = _petProfileImages

    private val _selectedPet = MutableLiveData<Pet?>()
    val selectedPet: LiveData<Pet?> = _selectedPet

    val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: MutableLiveData<UserProfile?> = _userProfile

    private val _userProfileImageUri = MutableLiveData<Uri>()
    val userProfileImageUri: LiveData<Uri> = _userProfileImageUri

    fun loadPetsProfile(userId: String) {
        val petsRef = FirebaseDatabase.getInstance().getReference("pets/$userId")
        Log.d("Debug", "Current User ID: $userId")

        petsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(datasnapshot: DataSnapshot) {
                val petsList = datasnapshot.children.mapNotNull { it.getValue(Pet::class.java) }
                _pets.postValue(petsList)

                // Create a map of petId to profileImageUri
                val petImagesMap = petsList.associate { pet ->
                    pet.petId to Uri.parse(pet.profileImageUrl)
                }
                _petProfileImages.postValue(petImagesMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("loadPetsProfile", "Failed to load pets: ${error.message}")
            }
        })
    }

    // Fetch Pets
    fun fetchPets(userId: String) {
        val database = FirebaseDatabase.getInstance().getReference("pets/$userId")
        val petsList = mutableListOf<Pet>()

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val pet = snapshot.getValue(Pet::class.java)
                pet?.let { petsList.add(it) }
                _pets.postValue(petsList) // Update LiveData
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedPet = snapshot.getValue(Pet::class.java)
                updatedPet?.let {
                    val index = petsList.indexOfFirst { it.petId == it.petId }
                    if (index != -1) {
                        petsList[index] = updatedPet
                        _pets.postValue(petsList)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedPet = snapshot.getValue(Pet::class.java)
                removedPet?.let {
                    petsList.remove(it)
                    _pets.postValue(petsList)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", "Failed to fetch pets: ${error.message}")
            }
        }

        database.addChildEventListener(childEventListener)
    }

    fun uploadImageToFirebase(uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onError("User not logged in")
            return
        }
        val storageRef = FirebaseStorage.getInstance()
            .getReference("uploads/$userId/${uri.lastPathSegment}")
        storageRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl
                    ?.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()
                        val databaseReference = FirebaseDatabase.getInstance().getReference("userProfiles/$userId/profileImageUrl")
                        databaseReference.setValue(imageUrl).addOnSuccessListener {
                            Log.d("DatabaseUpdate", "Profile image URL successfully saved to database.")
                        }.addOnFailureListener {
                            Log.e("DatabaseError", "Failed to save profile image URL to database.", it)
                        }

                        // Return the image URL on success
                        onSuccess(imageUrl)
                    }
                    ?.addOnFailureListener { exception ->
                        onError("Failed to get download URL: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                onError("Failed to upload image: ${exception.message}")
            }
    }

    fun uploadPetImage(userId: String, petId: String, uri: Uri, onImageUploaded: (String) -> Unit) {
        uploadImageToFirebase(
            uri = uri,
            onSuccess = { imageUrl ->
                // Update the pet's profile image URL in Firebase Realtime Database
                val databaseReference = FirebaseDatabase.getInstance()
                    .getReference("pets/$userId/$petId/profileImageUrl")
                databaseReference.setValue(imageUrl)
                    .addOnSuccessListener {
                        Log.d("PetImageUpload", "Pet profile image URL successfully saved for petId: $petId")
                        onImageUploaded(imageUrl) // Pass the image URL back to the composable
                    }
                    .addOnFailureListener { exception ->
                        Log.e("PetImageUpload", "Failed to save pet image URL for petId: $petId", exception)
                    }
            },
            onError = { error ->
                Log.e("PetImageUpload", "Error uploading pet image for petId: $petId: $error")
            }
        )
    }

    fun addPet(userId: String, pet: Pet) {
        val petsRef = FirebaseDatabase.getInstance().getReference("pets/$userId")
        val petId = pet.petId
        val petWithId = pet.copy(petId = petId)  // Ensure `petId` is set in the pet object

        petsRef.child(petId).setValue(petWithId).addOnSuccessListener {
            Log.d("AddPet", "Pet successfully added: ${petWithId.petId}")
        }.addOnFailureListener { exception ->
            Log.e("AddPet", "Failed to add pet", exception)
        }
    }

    fun updatePet(userId: String, pet: Pet) {
        val petsRef = FirebaseDatabase.getInstance().getReference("pets/$userId")
        petsRef.child(pet.petId).setValue(pet)
    }

    fun setSelectedPet(pet: Pet?) {
        _selectedPet.value = pet
    }

    fun askPetAdvice(
        userId: String,
        pet: Pet,
        question: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // --- 1. Manually Fetch and Deserialize Feeding Records ---
                val feedingSnapshot = FirebaseDatabase.getInstance()
                    .getReference("feedings/$userId")
                    .get().await()

                val feeding = feedingSnapshot.children.mapNotNull { child ->
                    val map = child.value as? Map<*, *> ?: return@mapNotNull null
                    if (map["petId"] != pet.petId) return@mapNotNull null

                    val mealTime = when (val mealTimeData = map["mealTime"]) {
                        is Map<*, *> -> {
                            try {
                                LocalDateTime.of(
                                    (mealTimeData["year"] as? Number)?.toInt() ?: 2000,
                                    (mealTimeData["monthValue"] as? Number)?.toInt() ?: 1,
                                    (mealTimeData["dayOfMonth"] as? Number)?.toInt() ?: 1,
                                    (mealTimeData["hour"] as? Number)?.toInt() ?: 0,
                                    (mealTimeData["minute"] as? Number)?.toInt() ?: 0,
                                    (mealTimeData["second"] as? Number)?.toInt() ?: 0
                                )
                            } catch (e: Exception) {
                                LocalDateTime.of(2000, 1, 1, 0, 0)
                            }
                        }
                        is Number -> Instant.ofEpochMilli(mealTimeData.toLong())
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                        is String -> try {
                            LocalDateTime.parse(mealTimeData)
                        } catch (e: Exception) {
                            LocalDateTime.of(2000, 1, 1, 0, 0)
                        }
                        else -> LocalDateTime.of(2000, 1, 1, 0, 0)
                    }

                    Feeding(
                        petId = map["petId"] as? String ?: "",
                        foodId = map["foodId"] as? String ?: "",
                        amount = (map["amount"] as? Number)?.toFloat() ?: 0f,
                        mealTime = mealTime,
                        mealType = map["mealType"] as? String ?: "",
                        notes = map["notes"] as? String ?: ""
                    )
                }

                // --- 2. Manually Fetch and Deserialize Health Records ---
                val healthSnapshot = FirebaseDatabase.getInstance()
                    .getReference("healthRecords/$userId")
                    .get().await()

                val health = healthSnapshot.children.mapNotNull { child ->
                    val map = child.value as? Map<*, *> ?: return@mapNotNull null
                    if (map["petId"] != pet.petId) return@mapNotNull null

                    // Extract the nested entryDate.time
                    val entryDateMap = map["entryDate"] as? Map<*, *>
                    val entryDateTime = (entryDateMap?.get("time") as? Number)?.toLong() ?: System.currentTimeMillis()

                    HealthRecord(
                        petId = map["petId"] as? String ?: "",
                        entryDate = Date(entryDateTime),
                        weight = (map["weight"] as? Number)?.toFloat() ?: 0f
                    )
                }

                // --- 3. Fetch Related Events ---
                val calendarSnapshot = FirebaseDatabase.getInstance()
                    .getReference("events/$userId")
                    .get().await()

                val events = calendarSnapshot.children.mapNotNull { snap ->
                    val map = snap.value as? Map<*, *> ?: return@mapNotNull null
                    val title = map["title"] as? String ?: return@mapNotNull null
                    val description = map["description"] as? String
                    if (description?.contains(pet.petId) == true || title.contains(pet.name)) {
                        title to description
                    } else null
                }

                // --- 4. Format all info ---
                val feedingInfo = feeding.joinToString("\n") {
                    "- ${it.mealTime.toLocalDate()} ${it.mealType}: ${it.amount}g, notes: ${it.notes}"
                }

                val healthInfo = health.joinToString("\n") {
                    "- ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.entryDate)}: weight = ${it.weight}"
                }

                val eventInfo = events.joinToString("\n") { "- ${it.first}: ${it.second}" }

                // --- 5. Create OpenAI Prompt ---
                val prompt = """
                    Pet Profile:
                    Name: ${pet.name}
                    Type: ${pet.type}
                    Breed: ${pet.breed}
                    Sex: ${pet.selectedSex}
                    Allergy: ${pet.allergy}
                    Birth Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pet.birthDate)}
                    
                    Recent Feeding Records:
                    $feedingInfo
                    
                    Health Records:
                    $healthInfo
                    
                    Events:
                    $eventInfo
                    
                    User Question: $question
                    
                    Provide a helpful, friendly response based on all the pet's data.
                """.trimIndent()

                Log.d("askPetAdvice", "Prompt sent to OpenAI:\n$prompt")

                // --- 6. Call OpenAI ---
                val requestBody = ChatRequest(
                    model = "gpt-4o",
                    messages = listOf(
                        Message(role = "system", content = "You are a helpful and smart assistant."),
                        Message(role = "user", content = prompt)
                    ),
                    max_tokens = 800
                )

                val response = RetrofitInstance.api.getChatResponse(requestBody)
                val result = response.choices.firstOrNull()?.message?.content ?: "No response"
                onSuccess(result.trim())

            } catch (e: Exception) {
                Log.e("askPetAdvice", "Error: ${e.message}", e)
                onError("Failed to get pet advice: ${e.message}")
            }
        }
    }

    fun loadUserProfile(userId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference()
        val userRef = dbRef.child("userProfiles").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userProfile = dataSnapshot.getValue(UserProfile::class.java)
                _userProfile.postValue(userProfile)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("loadUserProfile", "Database error: ${databaseError.message}")
            }
        })
    }

    fun insertUser(userProfile: UserProfile) = viewModelScope.launch(Dispatchers.IO) {
        val dbRef = FirebaseDatabase.getInstance().getReference()
        userProfile.userId?.let { userId ->
            dbRef.child("userProfiles").child(userId).setValue(userProfile)
                .addOnSuccessListener {
                    // Handle success
                    Log.d("InsertUser", "User profile inserted successfully")
                    // Update UI or notify user via LiveData or some other mechanism
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                    Log.e("InsertUser", "Failed to insert user profile: ${exception.message}")
                    // Optionally update UI to reflect failure
                }
        }
    }

    fun updateUser(userProfile: UserProfile, onSuccess: () -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference()
        viewModelScope.launch {
            userProfile.userId?.let {
                dbRef.child("userProfiles").child(it).setValue(userProfile).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        Log.e("UpdateUser", "Failed to update user: ${task.exception?.message}")
                    }
                }
            }
        }
    }

    fun deleteUser(userProfile: UserProfile) = viewModelScope.launch(Dispatchers.IO) {
        val dbRef = FirebaseDatabase.getInstance().getReference()
        userProfile.userId?.let {
            dbRef.child("userProfiles").child(it).removeValue().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("DeleteUser", "User profile deleted successfully")
                } else {
                    Log.e("DeleteUser", "Failed to delete user profile: ${it.exception?.message}")
                }
            }
        }
    }
}