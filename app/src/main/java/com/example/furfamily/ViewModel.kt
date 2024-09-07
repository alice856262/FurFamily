package com.example.furfamily

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.furfamily.calendar.CalendarEvent
import com.example.furfamily.profile.UserProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import com.google.api.services.calendar.model.Event
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.model.EventDateTime
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId

class ViewModel(application: Application) : AndroidViewModel(application) {
    private val dbRef = FirebaseDatabase.getInstance().getReference()

    // Google sign-in
    private val googleSignInClient: GoogleSignInClient
    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(application.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(application, gso)
    }
    private val _googleSignInIntent = MutableLiveData<Intent>()
    val googleSignInIntent: LiveData<Intent> = _googleSignInIntent

    fun signInWithGoogle() {
        Log.d("signInWithGoogle", "signInWithGoogle")
        val signInIntent = googleSignInClient.signInIntent
        _googleSignInIntent.value = signInIntent
    }

    fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Firebase authentication succeeded")
                    onSuccess()
                } else {
                    Log.e("FirebaseAuth", "Firebase authentication failed", task.exception)
                    onFailure(task.exception!!)
                }
            }
    }

    fun fetchOrCreateUserProfile(account: GoogleSignInAccount, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val email = account.email ?: return@launch  // Exit if email is null
            val userProfilesRef = dbRef.child("userProfiles").orderByChild("email").equalTo(email)

            userProfilesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User profile exists, retrieve and pass it to onSuccess
                        snapshot.children.forEach {
                            val userProfile = it.getValue(UserProfile::class.java)
                            userProfile?.let { profile ->
                                onSuccess(profile)
                                return@forEach
                            }
                        }
                    } else {
                        // No profile exists, create a new one
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                        Log.e("ViewModel 101", "${userId}")
                        val newUserProfile = UserProfile(
                            userId = userId,
                            email = email,
                            firstName = account.givenName ?: "",
                            lastName = account.familyName ?: "",
                            password = "",  // Password is not stored for Google users
                            selectedGender = "",
                            phone = "",
                            birthDate = Date(),
                            allowLocation = false,
                            allowActivityShare = false,
                            allowHealthDataShare = false,
                            isGoogleUser = true,
                            profileImageUrl = "")
                        // Insert new user profile into Firebase and call onSuccess
                        dbRef.child("userProfiles").child(userId).setValue(newUserProfile)
                            .addOnSuccessListener {
                                onSuccess(newUserProfile)
                            }
                            .addOnFailureListener {
                                Log.e("FirebaseError", "Failed to create new user profile", it)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to fetch user profile: ${error.message}")
                }
            })
        }
    }

    // Reset password
    private val _statusMessage = MutableLiveData<String>()
    fun sendPasswordResetEmail(email: String) {
        val auth = FirebaseAuth.getInstance()
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _statusMessage.postValue("Reset link sent to your email")
                } else {
                    _statusMessage.postValue("Failed to send reset link: ${task.exception?.localizedMessage}")
                }
            }
    }

    private val _intentForUserResolution = MutableLiveData<Intent>()
    val intentForUserResolution: LiveData<Intent> = _intentForUserResolution

    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()
    private val JSON_FACTORY = GsonFactory.getDefaultInstance()

    private val _calendarEvents = MutableLiveData<List<Event>>()
    val calendarEvents: LiveData<List<Event>> = _calendarEvents

    private val _eventsDates = MutableLiveData<List<LocalDate>>()
    val eventsDates: LiveData<List<LocalDate>> = _eventsDates

    fun loadCalendarEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if the current user is signed in with Google
                val account = GoogleSignIn.getLastSignedInAccount(getApplication<Application>())
                if (account != null) {
                    // User signed in with Google, proceed with Google Calendar API
                    val credential = GoogleAccountCredential.usingOAuth2(
                        getApplication<Application>(), listOf(CalendarScopes.CALENDAR)
                    )
                    credential.selectedAccount = account.account
                    val service = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("YourAppName")
                        .build()
                    val now = DateTime(System.currentTimeMillis())
                    val eventsResponse = service.events().list("primary")
                        .setMaxResults(10)
                        .setTimeMin(now)
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()

                    _calendarEvents.postValue(eventsResponse.items)
                } else {
                    // User signed in with email/password, load events from Firebase
                    Log.e("ViewModel 190", "HERE")
                    loadEventsFromFirebase()
                }
            } catch (e: UserRecoverableAuthIOException) {
                _intentForUserResolution.postValue(e.intent)
            } catch (e: Exception) {
                Log.e("CalendarLoadError", "Failed to load calendar events", e)
            }
        }
    }

    fun loadEventsFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("events/$uid")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newEvents = mutableListOf<Event>()
                snapshot.children.forEach { event ->
                    Log.d("FirebaseDebug", "Event key: ${event.key} | Event value: ${event.value}")
                    try {
                        val eventMap = event.value as? HashMap<*, *>
                        val userId = eventMap?.get("userId") as? String ?: ""
                        val title = eventMap?.get("title") as? String ?: ""
                        val description = eventMap?.get("description") as? String
                        val location = eventMap?.get("location") as? String

                        // Handling startTime and endTime
                        val startTimeMap = eventMap?.get("startTime") as HashMap<String, Any>
                        val endTimeMap = eventMap?.get("endTime") as HashMap<String, Any>

                        // Use the function to parse start and end times
                        val startTime = parseLocalDateTimeFromMap(startTimeMap)
                        val endTime = parseLocalDateTimeFromMap(endTimeMap)

                        // Create and add the event
                        val event = CalendarEvent(userId, title, description, startTime, endTime, location)
                        newEvents.add(convertCalendarEventToEvent(event))  // Assuming this function converts CalendarEvent to Event
                    } catch (e: Exception) {
                        Log.e("DataProcessError", "Failed to process event data: ${e.message}")
                    }
                }
                _calendarEvents.postValue(newEvents)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error loading events: ${error.message}")
            }
        })
    }

    private fun parseLocalDateTimeFromMap(dateTimeMap: HashMap<String, Any>): LocalDateTime {
        val year = (dateTimeMap["year"] as Long).toInt()
        val month = Month.valueOf((dateTimeMap["month"] as String).toUpperCase())
        val day = (dateTimeMap["dayOfMonth"] as Long).toInt()
        val hour = (dateTimeMap["hour"] as Long).toInt()
        val minute = (dateTimeMap["minute"] as Long).toInt()
        val second = (dateTimeMap["second"] as Long).toInt()
        val nanoOfSecond = (dateTimeMap["nano"] as Long).toInt()

        return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond)
    }

    private fun convertCalendarEventToEvent(calendarEvent: CalendarEvent): Event {
        val event = Event()  // Initialize the Event object
        event.summary = calendarEvent.title
        event.description = calendarEvent.description

        // Create EventDateTime for start and end
        val startDateTime = DateTime(calendarEvent.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        val endDateTime = DateTime(calendarEvent.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())

        // Set EventDateTime with the DateTime
        event.start = EventDateTime().setDateTime(startDateTime)
        event.end = EventDateTime().setDateTime(endDateTime)

        event.location = calendarEvent.location

        return event
    }

    fun loadEventsForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Check if the current user is signed in with Google
                val account = GoogleSignIn.getLastSignedInAccount(getApplication<Application>())
                if (account != null) {
                    // User signed in with Google, proceed with Google Calendar API
                    val credential = GoogleAccountCredential.usingOAuth2(
                        getApplication<Application>(), listOf(CalendarScopes.CALENDAR)
                    )
                    credential.selectedAccount = account.account
                    val service = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("YourAppName")
                        .build()

                    val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                    val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

                    val eventsResponse = service.events().list("primary")
                        .setTimeMin(DateTime(startOfDay.toEpochMilli()))
                        .setTimeMax(DateTime(endOfDay.toEpochMilli()))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()

                    _calendarEvents.postValue(eventsResponse.items)
                } else {
                    // User signed in with email/password, load events from Firebase
                    loadEventsFromFirebaseForDate(date)
                }
            } catch (e: UserRecoverableAuthIOException) {
                _intentForUserResolution.postValue(e.intent)
            } catch (e: Exception) {
                Log.e("CalendarLoadError", "Failed to load calendar events", e)
            }
        }
    }

    fun loadEventsFromFirebaseForDate(date: LocalDate) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("events/$uid")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newEvents = mutableListOf<Event>()
                snapshot.children.forEach { child ->
                    try {
                        val eventMap = child.value as? HashMap<*, *>
                        val userId = eventMap?.get("userId") as? String ?: ""
                        val title = eventMap?.get("title") as? String ?: ""
                        val description = eventMap?.get("description") as? String
                        val location = eventMap?.get("location") as? String

                        // Handling startTime and endTime
                        val startTimeMap = eventMap?.get("startTime") as HashMap<String, Any>
                        val endTimeMap = eventMap?.get("endTime") as HashMap<String, Any>
                        val startTime = parseLocalDateTimeFromMap(startTimeMap)
                        val endTime = parseLocalDateTimeFromMap(endTimeMap)

                        // Check if the event's date is the same as the provided date
                        if (startTime.toLocalDate() == date || endTime.toLocalDate() == date) {
                            val event = CalendarEvent(userId, title, description, startTime, endTime, location)
                            newEvents.add(convertCalendarEventToEvent(event))
                        }
                    } catch (e: Exception) {
                        Log.e("DataProcessError", "Failed to process event data: ${e.message}")
                    }
                }
                _calendarEvents.postValue(newEvents)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to load events: ${error.message}")
            }
        })
    }

    fun createEvent(event: CalendarEvent, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

            // Check if the current user is authenticated via Google
            val account = GoogleSignIn.getLastSignedInAccount(getApplication<Application>())
            if (account != null && account.idToken != null) {
                try {
                    // User is a Google user, proceed with Google Calendar and Firebase
                    val credential = GoogleAccountCredential.usingOAuth2(
                        getApplication<Application>(), listOf(CalendarScopes.CALENDAR)
                    ).apply {
                        selectedAccount = account.account
                    }
                    val calendarService = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("YourAppName")
                        .build()

                    val googleEvent = Event().apply {
                        summary = event.title
                        description = event.description
                        start = EventDateTime().setDateTime(localDateTimeToDateTime(event.startTime))
                        end = EventDateTime().setDateTime(localDateTimeToDateTime(event.endTime))
                        if (!event.location.isNullOrEmpty()) {
                            location = event.location
                        }
                    }
                    val createdEvent = calendarService.events().insert("primary", googleEvent).execute()

                    // Also save to Firebase under the user's directory
                    saveEventToFirebase(userId, event)
                    onComplete()
                } catch (e: Exception) {
                    Log.e("ViewModel", "Error creating event on Google Calendar", e)
                    onComplete()
                }
            } else {
                // User is not a Google user, save only to Firebase
                saveEventToFirebase(userId, event)
                onComplete()
            }
        }
    }

    private fun saveEventToFirebase(userId: String, event: CalendarEvent) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("events").child(userId)
        val eventId = databaseReference.push().key ?: throw IllegalStateException("Failed to get Firebase key")
        val eventMap = mapOf(
            "userId" to event.userId,
            "title" to event.title,
            "description" to event.description,
            "startTime" to event.startTime,
            "endTime" to event.endTime,
            "location" to event.location)

        databaseReference.child(eventId).setValue(eventMap)
            .addOnSuccessListener {
                println("Event saved successfully")
            }
            .addOnFailureListener {
                println("Error saving event: ${it.message}")
            }
    }

    fun localDateTimeToDateTime(ldt: LocalDateTime): DateTime {
        val instant = ldt.atZone(ZoneId.systemDefault()).toInstant()
        return DateTime(instant.toEpochMilli())
    }

//    val allFoods: LiveData<List<Food>> = repository.allFoods.asLiveData()
//    val allPersonalNutrition: LiveData<List<PersonalNutrition>> = repository.allPersonalNutrition.asLiveData()
//    val retrofitResponse: MutableState<List<FoodAPI>> = mutableStateOf((emptyList()))

    // User profile
    val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: MutableLiveData<UserProfile?> = _userProfile

    private val _profileImageUri = MutableLiveData<Uri>()
    val profileImageUri: LiveData<Uri> = _profileImageUri

    fun uploadImageToStorage(imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().getReference("uploads/$userId/${imageUri.lastPathSegment}")
        storageRef.putFile(imageUri).addOnSuccessListener {
            it.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                saveImageUriToDatabase(userId, imageUrl)
                setImageUri(uri)
            }
        }.addOnFailureListener {
            Log.e("Upload", "Upload failed", it)
        }
    }

    fun saveImageUriToDatabase(userId: String, imageUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("userProfiles/$userId/profileImageUrl")
        databaseReference.setValue(imageUrl).addOnSuccessListener {
            Log.d("DatabaseUpdate", "Profile image URL successfully saved to database.")
        }.addOnFailureListener {
            Log.e("DatabaseError", "Failed to save profile image URL to database.", it)
        }
    }

    fun setImageUri(uri: Uri) {
        _profileImageUri.postValue(uri)
    }
//
//    // Food
//    fun insertFood(food: Food) = viewModelScope.launch(Dispatchers.IO) {
//        repository.insertFood(food)
//    }
//
//    fun insertFoods(foodList: List<Food>) {
//        viewModelScope.launch(Dispatchers.IO) {
//            for (food in foodList) {
//                repository.insertFood(food)
//                Log.d(ContentValues.TAG, "Inserted food: ${food.name}")
//            }
//        }
//    }
//    fun updateFood(food: Food) = viewModelScope.launch(Dispatchers.IO) {
//        repository.updateFood(food)
//    }
//
//    fun deleteFood(food: Food) = viewModelScope.launch(Dispatchers.IO) {
//        repository.deleteFood(food)
//    }
//
//    // Personal Nutrition
//    fun insertPersonalNutrition(personalNutrition: PersonalNutrition) = viewModelScope.launch(Dispatchers.IO) {
//        repository.insertPersonalNutrition(personalNutrition)
//    }
//
//    fun updatePersonalNutrition(personalNutrition: PersonalNutrition) = viewModelScope.launch(Dispatchers.IO) {
//        repository.updatePersonalNutrition(personalNutrition)
//    }
//    fun deletePersonalNutrition(personalNutrition: PersonalNutrition) = viewModelScope.launch(Dispatchers.IO) {
//        repository.deletePersonalNutrition(personalNutrition)
//    }
//    fun deleteAllPersonalNutrition() = viewModelScope.launch(Dispatchers.IO) {
//        repository.deleteAllPersonalNutrition()
//    }
//
//    fun getResponse(keyword:String) {
//        viewModelScope.launch  {
//            try {
//                val responseReturned = repository.getResponse(keyword)
//                Log.i("Response", "Response : $responseReturned")
//                retrofitResponse.value = responseReturned
//
//            } catch (e: Exception) {
//                Log.i("Error ", "Response failed : $e")
//            }
//        }
//    }

    // User Profile
    fun loadUserProfile(userId: String) {
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
//            try {
//                repository.updateUser(userProfile)
//                _userProfile.postValue(userProfile)
//                onSuccess()
//            } catch (e: Exception) {
//                Log.e("UpdateUser", "Failed to update user: ${e.message}")
//            }
        }
    }

    fun deleteUser(userProfile: UserProfile) = viewModelScope.launch(Dispatchers.IO) {
        userProfile.userId?.let {
            dbRef.child("userProfiles").child(it).removeValue().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("DeleteUser", "User profile deleted successfully")
                } else {
                    Log.e("DeleteUser", "Failed to delete user profile: ${it.exception?.message}")
                }
            }
        }
//        repository.deleteUser(userProfile)
    }
}