package com.example.furfamily
//
//import android.app.Application
//import android.content.Context
//import android.content.Intent
//import android.graphics.ImageDecoder
//import android.location.Geocoder
//import android.net.Uri
//import android.os.Build
//import android.provider.MediaStore
//import android.util.Log
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MediatorLiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.viewModelScope
//import com.example.furfamily.calendar.CalendarEvent
//import com.example.furfamily.health.HealthRecord
//import com.example.furfamily.map.PlaceTag
//import com.example.furfamily.network.ChatRequest
//import com.example.furfamily.network.Message
//import com.example.furfamily.network.RetrofitInstance
//import com.example.furfamily.nutrition.Feeding
//import com.example.furfamily.nutrition.Food
//import com.example.furfamily.profile.UserProfile
//import com.google.android.gms.auth.api.signin.GoogleSignIn
//import com.google.android.gms.auth.api.signin.GoogleSignInAccount
//import com.google.android.gms.auth.api.signin.GoogleSignInClient
//import com.google.android.gms.auth.api.signin.GoogleSignInOptions
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
//import com.google.api.client.util.DateTime
//import com.google.api.services.calendar.Calendar
//import com.google.api.services.calendar.CalendarScopes
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.GoogleAuthProvider
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ValueEventListener
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.util.Date
//import com.google.api.services.calendar.model.Event
//import com.google.api.client.extensions.android.http.AndroidHttp
//import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
//import com.google.api.client.json.gson.GsonFactory
//import com.google.api.services.calendar.model.EventDateTime
//import com.google.firebase.storage.FirebaseStorage
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import kotlinx.coroutines.GlobalScope
//import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.Month
//import java.time.ZoneId
//import com.example.furfamily.profile.Pet
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.libraries.places.api.model.Place
//import com.google.android.libraries.places.api.net.FetchPlaceRequest
//import com.google.android.libraries.places.api.net.PlacesClient
//import com.google.firebase.database.ChildEventListener
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.tasks.await
//import org.json.JSONException
//import org.json.JSONObject
//import java.text.SimpleDateFormat
//import java.time.Instant
//import java.time.ZoneOffset
//import java.util.Locale
//
//class ViewModel(application: Application) : AndroidViewModel(application) {
//    private val dbRef = FirebaseDatabase.getInstance().getReference()
//
//    // Google sign-in
//    private val googleSignInClient: GoogleSignInClient
//    init {
//        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestIdToken(application.getString(R.string.default_web_client_id))
//            .requestEmail()
//            .build()
//        googleSignInClient = GoogleSignIn.getClient(application, gso)
//    }
//    private val _googleSignInIntent = MutableLiveData<Intent>()
//    val googleSignInIntent: LiveData<Intent> = _googleSignInIntent
//
//    fun signInWithGoogle() {
//        Log.d("signInWithGoogle", "signInWithGoogle")
//        val signInIntent = googleSignInClient.signInIntent
//        _googleSignInIntent.value = signInIntent
//    }
//
//    fun firebaseAuthWithGoogle(idToken: String, account: GoogleSignInAccount, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
//        val credential = GoogleAuthProvider.getCredential(idToken, null)
//        FirebaseAuth.getInstance().signInWithCredential(credential)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    Log.d("FirebaseAuth", "Firebase authentication succeeded")
//                    onSuccess()
//                } else {
//                    Log.e("FirebaseAuth", "Firebase authentication failed", task.exception)
//                    onFailure(task.exception!!)
//                }
//            }
//    }
//
//    fun fetchOrCreateUserProfile(account: GoogleSignInAccount, onSuccess: (UserProfile) -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val email = account.email ?: return@launch  // Exit if email is null
//            val userProfilesRef = dbRef.child("userProfiles").orderByChild("email").equalTo(email)
//
//            userProfilesRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists()) {
//                        // User profile exists, retrieve and pass it to onSuccess
//                        snapshot.children.forEach {
//                            val userProfile = it.getValue(UserProfile::class.java)
//                            userProfile?.let { profile ->
//                                onSuccess(profile)
//                                return@forEach
//                            }
//                        }
//                    } else {
//                        // No profile exists, create a new one
//                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//                        Log.e("ViewModel 101", "${userId}")
//                        val newUserProfile = UserProfile(
//                            userId = userId,
//                            email = email,
//                            firstName = account.givenName ?: "",
//                            lastName = account.familyName ?: "",
//                            password = "",  // Password is not stored for Google users
//                            selectedGender = "",
//                            phone = "",
//                            birthDate = Date(),
//                            isGoogleUser = true,
//                            profileImageUrl = "")
//                        // Insert new user profile into Firebase and call onSuccess
//                        dbRef.child("userProfiles").child(userId).setValue(newUserProfile)
//                            .addOnSuccessListener {
//                                onSuccess(newUserProfile)
//                            }
//                            .addOnFailureListener {
//                                Log.e("FirebaseError", "Failed to create new user profile", it)
//                            }
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("FirebaseError", "Failed to fetch user profile: ${error.message}")
//                }
//            })
//        }
//    }
//
//    // Reset password
//    private val _statusMessage = MutableLiveData<String>()
//    fun sendPasswordResetEmail(email: String) {
//        val auth = FirebaseAuth.getInstance()
//        auth.sendPasswordResetEmail(email)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    _statusMessage.postValue("Reset link sent to your email")
//                } else {
//                    _statusMessage.postValue("Failed to send reset link: ${task.exception?.localizedMessage}")
//                }
//            }
//    }
//
//    private val _intentForUserResolution = MutableLiveData<Intent>()
//    val intentForUserResolution: LiveData<Intent> = _intentForUserResolution
//
//    private val HTTP_TRANSPORT = AndroidHttp.newCompatibleTransport()
//    private val JSON_FACTORY = GsonFactory.getDefaultInstance()
//
//    private val _calendarEvents = MutableLiveData<List<Event>>()
//    val calendarEvents: LiveData<List<Event>> = _calendarEvents
//
//    private val _calendarEventDates = MutableLiveData<List<Event>>()
//    val calendarEventDates: LiveData<List<Event>> = _calendarEventDates
//
//    private val _eventsDates = MediatorLiveData<List<LocalDate>>()
//    val eventsDates: LiveData<List<LocalDate>> = _eventsDates
//
//    init {
//        _eventsDates.addSource(_calendarEvents) { events ->
//            val localDates = events.mapNotNull { event ->
//                event.start?.dateTime?.let {
//                    Instant.ofEpochMilli(it.value).atZone(ZoneId.systemDefault()).toLocalDate()
//                } ?: event.start?.date?.let {
//                    LocalDate.parse(it.toString())
//                }
//            }.distinct()
//            _eventsDates.postValue(localDates)
//        }
//    }
//
//    fun loadCalendarEvents() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // Check if the current user is signed in with Google
//                val account = GoogleSignIn.getLastSignedInAccount(getApplication<Application>())
//                if (account != null) {
//                    // User signed in with Google, proceed with Google Calendar API
//                    val credential = GoogleAccountCredential.usingOAuth2(
//                        getApplication<Application>(), listOf(CalendarScopes.CALENDAR)
//                    )
//                    credential.selectedAccount = account.account
//                    val service = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                        .setApplicationName("YourAppName")
//                        .build()
//                    val now = DateTime(System.currentTimeMillis())
//                    val eventsResponse = service.events().list("primary")
//                        .setMaxResults(10)
//                        .setTimeMin(now)
//                        .setOrderBy("startTime")
//                        .setSingleEvents(true)
//                        .execute()
//
//                    _calendarEvents.postValue(eventsResponse.items)
//                } else {
//                    // User signed in with email/password, load events from Firebase
//                    loadEventsFromFirebase()
//                }
//            } catch (e: UserRecoverableAuthIOException) {
//                _intentForUserResolution.postValue(e.intent)
//            } catch (e: Exception) {
//                Log.e("CalendarLoadError", "Failed to load calendar events", e)
//            }
//        }
//    }
//
//    fun loadEventsFromFirebase() {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val databaseReference = FirebaseDatabase.getInstance().getReference("events/$uid")
//        databaseReference.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val newEvents = mutableListOf<Event>()
//                snapshot.children.forEach { event ->
//                    try {
//                        val eventMap = event.value as? HashMap<*, *>
//                        val userId = eventMap?.get("userId") as? String ?: ""
//                        val petId = eventMap?.get("petId") as? String ?: "" // Parse petId
//                        val title = eventMap?.get("title") as? String ?: ""
//                        val description = eventMap?.get("description") as? String
//                        val location = eventMap?.get("location") as? String
//
//                        val startTimeMap = eventMap?.get("startTime") as HashMap<String, Any>
//                        val endTimeMap = eventMap?.get("endTime") as HashMap<String, Any>
//                        val startTime = parseLocalDateTimeFromMap(startTimeMap)
//                        val endTime = parseLocalDateTimeFromMap(endTimeMap)
//
//                        val event = CalendarEvent(userId, petId, title, description, startTime, endTime, location)
//                        newEvents.add(convertCalendarEventToEvent(event)) // Convert to Event
//                        snapshot.children.forEach { event ->
//                            Log.d("Debug", "Processing Firebase Event: ${event.value}")
//                        }
//                    } catch (e: Exception) {
//                        Log.e("DataProcessError", "Failed to process event data: ${e.message}")
//                    }
//                }
//                _calendarEvents.postValue(newEvents)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//            }
//        })
//    }
//
//    private fun parseLocalDateTimeFromMap(dateTimeMap: HashMap<String, Any>): LocalDateTime {
//        val year = (dateTimeMap["year"] as Long).toInt()
//        val month = Month.valueOf((dateTimeMap["month"] as String).toUpperCase())
//        val day = (dateTimeMap["dayOfMonth"] as Long).toInt()
//        val hour = (dateTimeMap["hour"] as Long).toInt()
//        val minute = (dateTimeMap["minute"] as Long).toInt()
//        val second = (dateTimeMap["second"] as Long).toInt()
//        val nanoOfSecond = (dateTimeMap["nano"] as Long).toInt()
//
//        return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond)
//    }
//
//    private fun convertCalendarEventToEvent(calendarEvent: CalendarEvent): Event {
//        val event = Event()  // Initialize the Event object
//        event.summary = calendarEvent.title
//        event.description = calendarEvent.description
//
//        // Create EventDateTime for start and end
//        val startDateTime = DateTime(calendarEvent.startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
//        val endDateTime = DateTime(calendarEvent.endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
//
//        // Set EventDateTime with the DateTime
//        event.start = EventDateTime().setDateTime(startDateTime)
//        event.end = EventDateTime().setDateTime(endDateTime)
//
//        event.location = calendarEvent.location
//
//        return event
//    }
//
//    fun loadEventsForDate(date: LocalDate) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // Check if the current user is signed in with Google
//                val account = GoogleSignIn.getLastSignedInAccount(getApplication<Application>())
//                if (account != null) {
//                    // User signed in with Google, proceed with Google Calendar API
//                    val credential = GoogleAccountCredential.usingOAuth2(
//                        getApplication<Application>(), listOf(CalendarScopes.CALENDAR)
//                    )
//                    credential.selectedAccount = account.account
//                    val service = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                        .setApplicationName("YourAppName")
//                        .build()
//
//                    val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
//                    val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
//
//                    val eventsResponse = service.events().list("primary")
//                        .setTimeMin(DateTime(startOfDay.toEpochMilli()))
//                        .setTimeMax(DateTime(endOfDay.toEpochMilli()))
//                        .setOrderBy("startTime")
//                        .setSingleEvents(true)
//                        .execute()
//
//                    _calendarEventDates.postValue(eventsResponse.items)
//                } else {
//                    // User signed in with email/password, load events from Firebase
//                    loadEventsFromFirebaseForDate(date)
//                }
//            } catch (e: UserRecoverableAuthIOException) {
//                _intentForUserResolution.postValue(e.intent)
//            } catch (e: Exception) {
//                Log.e("CalendarLoadError", "Failed to load calendar events", e)
//            }
//        }
//    }
//
//    fun loadEventsFromFirebaseForDate(date: LocalDate) {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val databaseReference = FirebaseDatabase.getInstance().getReference("events/$uid")
//        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val newEvents = mutableListOf<Event>()
//                snapshot.children.forEach { child ->
//                    try {
//                        val eventMap = child.value as? HashMap<*, *>
//                        val userId = eventMap?.get("userId") as? String ?: ""
//                        val petId = eventMap?.get("petId") as? String ?: "" // Parse petId
//                        val title = eventMap?.get("title") as? String ?: ""
//                        val description = eventMap?.get("description") as? String
//                        val location = eventMap?.get("location") as? String
//
//                        val startTimeMap = eventMap?.get("startTime") as HashMap<String, Any>
//                        val endTimeMap = eventMap?.get("endTime") as HashMap<String, Any>
//                        val startTime = parseLocalDateTimeFromMap(startTimeMap)
//                        val endTime = parseLocalDateTimeFromMap(endTimeMap)
//
//                        // Check if the event's date matches the provided date
//                        if (startTime.toLocalDate() == date || endTime.toLocalDate() == date) {
//                            val event = CalendarEvent(userId, petId, title, description, startTime, endTime, location)
//                            newEvents.add(convertCalendarEventToEvent(event)) // Convert to Event
//                        }
//                    } catch (e: Exception) {
//                        Log.e("DataProcessError", "Failed to process event data: ${e.message}")
//                    }
//                }
//                _calendarEventDates.postValue(newEvents)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("FirebaseError", "Failed to load events: ${error.message}")
//            }
//        })
//    }
//
//    fun createEvent(event: CalendarEvent, onComplete: () -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
//
//            // Check if the current user is authenticated via Google
//            val account = GoogleSignIn.getLastSignedInAccount(getApplication<Application>())
//            if (account != null && account.idToken != null) {
//                try {
//                    // User is a Google user, proceed with Google Calendar and Firebase
//                    val credential = GoogleAccountCredential.usingOAuth2(
//                        getApplication<Application>(), listOf(CalendarScopes.CALENDAR)
//                    ).apply {
//                        selectedAccount = account.account
//                    }
//                    val calendarService = Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//                        .setApplicationName("YourAppName")
//                        .build()
//
//                    val googleEvent = Event().apply {
//                        summary = event.title
//                        description = event.description
//                        start = EventDateTime().setDateTime(localDateTimeToDateTime(event.startTime))
//                        end = EventDateTime().setDateTime(localDateTimeToDateTime(event.endTime))
//                        if (!event.location.isNullOrEmpty()) {
//                            location = event.location
//                        }
//                    }
//                    val createdEvent = calendarService.events().insert("primary", googleEvent).execute()
//
//                    // Also save to Firebase under the user's directory
//                    saveEventToFirebase(userId, event)
//                    onComplete()
//                } catch (e: Exception) {
//                    Log.e("ViewModel", "Error creating event on Google Calendar", e)
//                    onComplete()
//                }
//            } else {
//                // User is not a Google user, save only to Firebase
//                saveEventToFirebase(userId, event)
//                onComplete()
//            }
//        }
//    }
//
//    private fun saveEventToFirebase(userId: String, event: CalendarEvent) {
//        val databaseReference = FirebaseDatabase.getInstance().getReference("events").child(userId)
//        val eventId = databaseReference.push().key ?: throw IllegalStateException("Failed to get Firebase key")
//        val eventMap = mapOf(
//            "userId" to event.userId,
//            "title" to event.title,
//            "description" to event.description,
//            "startTime" to event.startTime,
//            "endTime" to event.endTime,
//            "location" to event.location)
//
//        databaseReference.child(eventId).setValue(eventMap)
//            .addOnSuccessListener {
//                println("Event saved successfully")
//            }
//            .addOnFailureListener {
//                println("Error saving event: ${it.message}")
//            }
//    }
//
//    fun localDateTimeToDateTime(ldt: LocalDateTime): DateTime {
//        val instant = ldt.atZone(ZoneId.systemDefault()).toInstant()
//        return DateTime(instant.toEpochMilli())
//    }
//
//    fun clearSelectedDateEvents() {
//        _calendarEventDates.value = emptyList()
//        _feedingEvents.value = emptyList()
//    }
//
//    // Health Record
//    private val _pets = MutableLiveData<List<Pet>>()
//    val pets: LiveData<List<Pet>> = _pets
//
//    private val _healthRecords = MutableLiveData<List<HealthRecord>>()
//    val healthRecords: LiveData<List<HealthRecord>> = _healthRecords
//
//    // Fetch Pets
//    fun fetchPets(userId: String) {
//        val database = FirebaseDatabase.getInstance().getReference("pets/$userId")
//        val petsList = mutableListOf<Pet>()
//
//        val childEventListener = object : ChildEventListener {
//            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                val pet = snapshot.getValue(Pet::class.java)
//                pet?.let { petsList.add(it) }
//                _pets.postValue(petsList) // Update LiveData
//            }
//
//            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                val updatedPet = snapshot.getValue(Pet::class.java)
//                updatedPet?.let {
//                    val index = petsList.indexOfFirst { it.petId == it.petId }
//                    if (index != -1) {
//                        petsList[index] = updatedPet
//                        _pets.postValue(petsList)
//                    }
//                }
//            }
//
//            override fun onChildRemoved(snapshot: DataSnapshot) {
//                val removedPet = snapshot.getValue(Pet::class.java)
//                removedPet?.let {
//                    petsList.remove(it)
//                    _pets.postValue(petsList)
//                }
//            }
//
//            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("DatabaseError", "Failed to fetch pets: ${error.message}")
//            }
//        }
//
//        database.addChildEventListener(childEventListener)
//    }
//
//    // Fetch Health Records
//    fun fetchHealthRecords(userId: String, petId: String?) {
//        val database = FirebaseDatabase.getInstance().getReference("healthRecords").child(userId)
//        val recordsList = mutableListOf<HealthRecord>()
//
//        val childEventListener = object : ChildEventListener {
//            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                val record = snapshot.getValue(HealthRecord::class.java)
//                record?.takeIf { it.petId == petId }?.let { recordsList.add(it) }
//                _healthRecords.postValue(recordsList) // Update LiveData
//            }
//
//            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                val updatedRecord = snapshot.getValue(HealthRecord::class.java)
//                updatedRecord?.takeIf { it.petId == petId }?.let {
//                    val index = recordsList.indexOfFirst { it.entryDate == it.entryDate }
//                    if (index != -1) {
//                        recordsList[index] = updatedRecord
//                        _healthRecords.postValue(recordsList)
//                    }
//                }
//            }
//
//            override fun onChildRemoved(snapshot: DataSnapshot) {
//                val removedRecord = snapshot.getValue(HealthRecord::class.java)
//                removedRecord?.let {
//                    recordsList.remove(it)
//                    _healthRecords.postValue(recordsList)
//                }
//            }
//
//            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("DatabaseError", "Failed to fetch health records: ${error.message}")
//            }
//        }
//
//        database.addChildEventListener(childEventListener)
//    }
//
//    // Nutrition
//    private val _foodList = MutableLiveData<List<Food>>()
//    val foodList: LiveData<List<Food>> = _foodList
//
//    private val _latestWeight = MutableLiveData<Float?>()
//    val latestWeight: LiveData<Float?> = _latestWeight
//
//    private val _feedingEvents = MutableLiveData<List<Feeding>>()
//    val feedingEvents: LiveData<List<Feeding>> = _feedingEvents
//
//    init {
//        fetchLatestWeight()
//        loadFoodList()
//    }
//
//    // Function to load the food list from Firebase
//    fun loadFoodList() {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$uid")
//
//        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val foodListFromDb = mutableListOf<Food>()
//                for (foodSnapshot in snapshot.children) {
//                    val food = foodSnapshot.getValue(Food::class.java)
//                    val foodId = foodSnapshot.key // Get the key as foodId
//                    food?.let {
//                        it.foodId = foodId ?: ""  // Assign the foodId
//                        foodListFromDb.add(it)
//                    }
//                }
//                _foodList.postValue(foodListFromDb)
//            }
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("ViewModel", "Error loading food list: ${error.message}")
//            }
//        })
//    }
//
//    // Fetch Food Detail by ID
//    fun getFoodById(foodId: String, onResult: (Food?) -> Unit) {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$uid/$foodId")
//
//        foodRef.get()
//            .addOnSuccessListener { snapshot ->
//                if (snapshot.exists()) {
//                    val food = snapshot.getValue(Food::class.java)
//                    onResult(food)
//                } else {
//                    onResult(null) // No food found
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firebase", "Error fetching food: ${exception.message}")
//                onResult(null)
//            }
//    }
//
//    // Function to add a new food
//    fun addNewFood(food: Food) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        // Reference to the user's food node in Firebase
//        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$userId")
//        val foodId = foodRef.push().key ?: throw IllegalStateException("Failed to get Firebase key")
//        // Create a map to save the food data
//        val foodMap = mapOf(
//            "name" to food.name,
//            "category" to food.category,
//            "ingredient" to food.ingredient,
//            "caloriesPerKg" to food.caloriesPerKg,
//            "size" to food.size,
//            "proteinPercentage" to food.proteinPercentage,
//            "fatPercentage" to food.fatPercentage,
//            "fiberPercentage" to food.fiberPercentage,
//            "moisturePercentage" to food.moisturePercentage,
//            "ashPercentage" to food.ashPercentage,
//            "feedingInfo" to food.feedingInfo
//        )
//        // Save the new food to Firebase
//        foodRef.child(foodId).setValue(foodMap)
//            .addOnSuccessListener {
//                Log.d("Firebase", "Food saved successfully")
//                loadFoodList()  // Reload the food list from Firebase after saving
//            }
//            .addOnFailureListener {
//                Log.e("FirebaseError", "Error saving food: ${it.message}")
//            }
//    }
//
//    fun updateFood(food: Food) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val foodId = food.foodId
//        if (foodId.isBlank()) {
//            Log.e("ViewModel", "Invalid food ID")
//            return
//        }
//
//        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$userId/$foodId")
//        val foodUpdates = mapOf(
//            "name" to food.name,
//            "category" to food.category,
//            "ingredient" to food.ingredient,
//            "caloriesPerKg" to food.caloriesPerKg,
//            "size" to food.size,
//            "proteinPercentage" to food.proteinPercentage,
//            "fatPercentage" to food.fatPercentage,
//            "fiberPercentage" to food.fiberPercentage,
//            "moisturePercentage" to food.moisturePercentage,
//            "ashPercentage" to food.ashPercentage,
//            "feedingInfo" to food.feedingInfo
//        )
//        // Update the food item in Firebase
//        foodRef.updateChildren(foodUpdates)
//            .addOnSuccessListener {
//                Log.d("Firebase", "Food updated successfully")
//                loadFoodList() // Reload the food list after updating
//            }
//            .addOnFailureListener {
//                Log.e("FirebaseError", "Error updating food: ${it.message}")
//            }
//    }
//
//    fun deleteFood(food: Food) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val foodId = food.foodId
//        if (foodId.isBlank()) {
//            Log.e("ViewModel", "Invalid food ID")
//            return
//        }
//
//        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$userId/$foodId")
//        foodRef.removeValue()
//            .addOnSuccessListener {
//                Log.d("Firebase", "Food deleted successfully")
//                loadFoodList() // Reload the food list after deletion
//            }
//            .addOnFailureListener { exception ->
//                Log.e("FirebaseError", "Error deleting food: ${exception.message}")
//            }
//    }
//
//    // Function to fetch the latest weight record
//    fun fetchLatestWeight() {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        // Reference to the healthRecords node for the current user
//        val healthRecordsRef = FirebaseDatabase.getInstance().getReference("healthRecords/$userId")
//
//        // Query to get the latest health record based on `entryDate.time`
//        healthRecordsRef.orderByChild("entryDate/time").limitToLast(1)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists()) {
//                        Log.d("ViewModel", "Health records found: ${snapshot.childrenCount}")
//
//                        // Fetch the latest health record based on time
//                        snapshot.children.forEach { child ->
//                            val healthRecord = child.getValue(HealthRecord::class.java)
//                            if (healthRecord != null) {
//                                Log.d("ViewModel", "Latest health record: ${healthRecord.entryDate} - Weight: ${healthRecord.weight}")
//                                _latestWeight.postValue(healthRecord.weight)
//                            } else {
//                                Log.e("ViewModel", "Error parsing health record")
//                            }
//                        }
//                    } else {
//                        Log.d("ViewModel", "No health records found")
//                        _latestWeight.postValue(null)  // Handle the case where no weight is found
//                    }
//                }
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("ViewModel", "Error fetching latest weight: ${error.message}")
//                }
//            })
//    }
//
//    fun uploadImageToFirebase(uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
//            onError("User not logged in")
//            return
//        }
//        val storageRef = FirebaseStorage.getInstance()
//            .getReference("uploads/$userId/${uri.lastPathSegment}")
//        storageRef.putFile(uri)
//            .addOnSuccessListener { taskSnapshot ->
//                taskSnapshot.metadata?.reference?.downloadUrl
//                    ?.addOnSuccessListener { downloadUri ->
//                        val imageUrl = downloadUri.toString()
//                        val databaseReference = FirebaseDatabase.getInstance().getReference("userProfiles/$userId/profileImageUrl")
//                        databaseReference.setValue(imageUrl).addOnSuccessListener {
//                            Log.d("DatabaseUpdate", "Profile image URL successfully saved to database.")
//                        }.addOnFailureListener {
//                            Log.e("DatabaseError", "Failed to save profile image URL to database.", it)
//                        }
//
//                        // Return the image URL on success
//                        onSuccess(imageUrl)
//                    }
//                    ?.addOnFailureListener { exception ->
//                        onError("Failed to get download URL: ${exception.message}")
//                    }
//            }
//            .addOnFailureListener { exception ->
//                onError("Failed to upload image: ${exception.message}")
//            }
//    }
//
//    fun extractTextFromImage(context: Context, uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
//        try {
//            // Convert URI to Bitmap
//            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                val source = ImageDecoder.createSource(context.contentResolver, uri)
//                ImageDecoder.decodeBitmap(source)
//            } else {
//                @Suppress("DEPRECATION")
//                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
//            }
//            // Log the bitmap information
//            Log.d("extractTextFromImage", "Bitmap obtained with size: ${bitmap.width}x${bitmap.height}")
//            // Create an InputImage from the bitmap
//            val image = InputImage.fromBitmap(bitmap, 0)
//            // Initialize TextRecognizer
//            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//            // Process the image for text recognition
//            recognizer.process(image)
//                .addOnSuccessListener { visionText ->
//                    Log.d("extractTextFromImage", "Text extraction successful. Text found: ${visionText.text}")
//                    // If there is text, pass it to the onSuccess callback
//                    if (visionText.text.isNotEmpty()) {
//                        onSuccess(visionText.text)
//                    } else {
//                        onSuccess("No text found in the image.")
//                    }
//                }
//                .addOnFailureListener { e ->
//                    Log.e("extractTextFromImage", "Failed to process image for text: ${e.message}")
//                    onError("Failed to process image for text: ${e.message}")
//                }
//        } catch (e: Exception) {
//            Log.e("extractTextFromImage", "Failed to load image: ${e.message}")
//            onError("Failed to load image: ${e.message}")
//        }
//    }
//
//    fun sendTextToOpenAI(text: String, onSuccess: (Food) -> Unit, onError: (String) -> Unit) {
//        val requestBody = ChatRequest(
//            model = "gpt-4o", // or any other model you prefer
//            messages = listOf(
//                Message(role = "system",
//                    content = """
//                                  Please extract the following information from the provided text in JSON format:
//                                  {
//                                    "name": "string",
//                                    "ingredient": "string",
//                                    "caloriesPerKg": float,
//                                    "feedingInfo": "string",
//                                    "proteinPercentage": float,
//                                    "fatPercentage": float,
//                                    "fiberPercentage": float,
//                                    "moisturePercentage": float,
//                                    "ashPercentage": float
//                                  }
//                                  Default for string is an empty string. Default for float is 0.0.
//                                  Respond only with the JSON object.
//                              """.trimIndent()
//                ),
//                Message(role = "user", content = text)),
//            max_tokens = 500 // Adjust max tokens as needed
//        )
//        GlobalScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitInstance.api.getChatResponse(requestBody)
//                val result = response.choices.firstOrNull()?.message?.content ?: "No response"
//
//                // Clean the response to remove any non-JSON content
//                val cleanedResult = cleanJsonResponse(result)
//
//                // Attempt to parse the cleaned result into a Food object
//                val food = parseFoodFromResponse(cleanedResult)
//                if (food != null) {
//                    onSuccess(food)
//                } else {
//                    onError("Failed to parse food information from response.")
//                }
//            } catch (e: Exception) {
//                onError("Failed to get response from OpenAI: ${e.message}")
//            }
//        }
//    }
//
//    // Clean the JSON response by removing backticks and any non-JSON text
//    fun cleanJsonResponse(response: String): String {
//        return response.trim()
//            .replace("```json", "") // Remove ```json if it exists
//            .replace("```", "") // Remove closing ```
//            .replace("\n", "") // Remove newlines if necessary
//    }
//
//    // Parse the JSON response and create a Food object
//    fun parseFoodFromResponse(response: String): Food? {
//        return try {
//            val cleanedResponse = cleanJsonResponse(response)
//            Log.d("parseFoodFromResponse", "Cleaned Response: $cleanedResponse")
//
//            // Assuming the response is a JSON object in the format specified
//            val jsonObject = JSONObject(cleanedResponse)
//            val name = jsonObject.optString("name", "")
//            val ingredients = jsonObject.optString("ingredient", "") // Check the exact key here
//            val caloriesPerKg = jsonObject.optDouble("caloriesPerKg", 0.0).toFloat()
//            val feedingInfo = jsonObject.optString("feedingInfo", "")
//            val proteinPercentage = jsonObject.optDouble("proteinPercentage", 0.0).toFloat()
//            val fatPercentage = jsonObject.optDouble("fatPercentage", 0.0).toFloat()
//            val fiberPercentage = jsonObject.optDouble("fiberPercentage", 0.0).toFloat()
//            val moisturePercentage = jsonObject.optDouble("moisturePercentage", 0.0).toFloat()
//            val ashPercentage = jsonObject.optDouble("ashPercentage", 0.0).toFloat()
//
//            // Log the parsed ingredient to see if it's correct
//            Log.d("parseFoodFromResponse", "Parsed Ingredient: $ingredients")
//
//            Food(
//                name = name,
//                ingredient = ingredients,
//                caloriesPerKg = caloriesPerKg,
//                feedingInfo = feedingInfo,
//                proteinPercentage = proteinPercentage,
//                fatPercentage = fatPercentage,
//                fiberPercentage = fiberPercentage,
//                moisturePercentage = moisturePercentage,
//                ashPercentage = ashPercentage
//            )
//        } catch (e: JSONException) {
//            Log.e("parseFoodFromResponse", "Failed to parse response: ${e.message}")
//            null
//        }
//    }
//
//    // Function to fetch feeding details
//    fun loadFeedingEventsForDate(date: LocalDate) {
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$uid")
//        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
//        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
//
//        feedingRef.orderByChild("entryDate/time")
//            .startAt(startOfDay.toDouble())
//            .endAt(endOfDay.toDouble())
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val feedingEvents = mutableListOf<Feeding>()
//
//                    for (childSnapshot in snapshot.children) {
//                        val feedingMap = childSnapshot.value as? Map<*, *> ?: continue
//
//                        val mealTime = when (val mealTimeData = feedingMap["mealTime"]) {
//                            is Map<*, *> -> { // If stored as a Map, reconstruct LocalDateTime
//                                try {
//                                    LocalDateTime.of(
//                                        (mealTimeData["year"] as? Number)?.toInt() ?: 2000,
//                                        (mealTimeData["monthValue"] as? Number)?.toInt() ?: 1, // ✅ Use monthValue directly
//                                        (mealTimeData["dayOfMonth"] as? Number)?.toInt() ?: 1, // ✅ Correct day field
//                                        (mealTimeData["hour"] as? Number)?.toInt() ?: 0,
//                                        (mealTimeData["minute"] as? Number)?.toInt() ?: 0,
//                                        (mealTimeData["second"] as? Number)?.toInt() ?: 0
//                                    )
//                                } catch (e: Exception) {
//                                    Log.e("Firebase", "Error parsing mealTime Map: ${e.message}")
//                                    LocalDateTime.of(2000, 1, 1, 0, 0) // Default value
//                                }
//                            }
//                            is Number -> { // If stored as a timestamp, convert it
//                                Instant.ofEpochMilli(mealTimeData.toLong())
//                                    .atZone(ZoneId.systemDefault())
//                                    .toLocalDateTime()
//                            }
//                            is String -> { // If stored as a string (possible edge case)
//                                try {
//                                    LocalDateTime.parse(mealTimeData)
//                                } catch (e: Exception) {
//                                    Log.e("Firebase", "Error parsing mealTime String: $mealTimeData")
//                                    LocalDateTime.of(2000, 1, 1, 0, 0) // Default
//                                }
//                            }
//                            else -> {
//                                Log.e("Firebase", "Unknown mealTime format: $mealTimeData")
//                                LocalDateTime.of(2000, 1, 1, 0, 0) // Default
//                            }
//                        }
//
//                        val feeding = Feeding(
//                            petId = feedingMap["petId"] as? String ?: "",
//                            foodId = feedingMap["foodId"] as? String ?: "",
//                            entryDate = Date(feedingMap["entryDate"] as? Long ?: System.currentTimeMillis()),
//                            amount = (feedingMap["amount"] as? Number)?.toFloat() ?: 0.0F,
//                            mealTime = mealTime, // ✅ Correctly parsed LocalDateTime
//                            mealType = feedingMap["mealType"] as? String ?: "",
//                            notes = feedingMap["notes"] as? String ?: ""
//                        )
//
//                        feedingEvents.add(feeding)
//                    }
//                    _feedingEvents.postValue(feedingEvents)
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("FirebaseError", "Failed to load feeding events: ${error.message}")
//                }
//            })
//    }
//
//    // Function to save feeding info to Firebase
//    fun saveFeeding(pet: Pet, food: Food, amount: Float, mealTime: LocalDateTime, mealType: String, notes: String) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$userId")
//        val feedingId = feedingRef.push().key ?: throw IllegalStateException("Failed to get Firebase key")
//        // Create a Feeding object using the updated data class
//        val feeding = Feeding(
//            petId = pet.petId,
//            foodId = food.foodId,
//            entryDate = Date(),
//            amount = amount,
//            mealTime = mealTime,
//            mealType = mealType,
//            notes = notes
//        )
//        // Save the Feeding object to Firebase
//        feedingRef.child(feedingId).setValue(feeding)
//            .addOnSuccessListener {
//                Log.d("Firebase", "Feeding saved successfully")
//            }
//            .addOnFailureListener {
//                Log.e("FirebaseError", "Error saving feeding: ${it.message}")
//            }
//    }
//
//    // Map and Place Tag
//    private val _placeTags = MutableStateFlow<Map<LatLng, String>>(emptyMap())
//    val placeTags: StateFlow<Map<LatLng, String>> = _placeTags
//
//    private val _placeCategories = MutableStateFlow<Map<LatLng, String>>(emptyMap())
//    val placeCategories: StateFlow<Map<LatLng, String>> = _placeCategories
//
//    fun loadPlaceTags() {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Log.e("loadPlaceTags", "User not authenticated.")
//            return
//        }
//
//        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")
//        placeRef.get().addOnSuccessListener { dataSnapshot ->
//            val tagsMap = mutableMapOf<LatLng, String>()
//            val categoryMap = mutableMapOf<LatLng, String>()
//
//            dataSnapshot.children.forEach { snapshot ->
//                val placeTag = snapshot.getValue(PlaceTag::class.java)
//                if (placeTag != null) {
//                    val latLng = LatLng(placeTag.latitude, placeTag.longitude)
//                    val name = placeTag.name ?: "Unnamed Place"
//
//                    tagsMap[latLng] = name
//                    categoryMap[latLng] = placeTag.category ?: "Other"
//                }
//            }
//            _placeTags.value = tagsMap
//            _placeCategories.value = categoryMap
//        }.addOnFailureListener { exception ->
//            Log.e("loadPlaceTags", "Failed to load tags: ${exception.message}")
//        }
//    }
//
//    fun savePlaceTag(latLng: LatLng, tag: String, address: String?, category: String) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Log.e("savePlaceTag", "User not authenticated.")
//            return
//        }
//
//        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")
//        val key = placeRef.push().key
//        if (key != null) {
//            val placeTag = PlaceTag(
//                tagId = key,
//                userId = userId,
//                name = tag,
//                latitude = latLng.latitude,
//                longitude = latLng.longitude,
//                address = address ?: "Unknown Address",
//                category = category
//            )
//
//            placeRef.child(key).setValue(placeTag)
//                .addOnSuccessListener {
//                    Log.d("savePlaceTag", "Tag saved successfully: $placeTag")
//                    loadPlaceTags() // Reload tags after saving
//                }
//                .addOnFailureListener { exception ->
//                    Log.e("savePlaceTag", "Failed to save tag: ${exception.message}")
//                }
//        } else {
//            Log.e("savePlaceTag", "Failed to generate a key.")
//        }
//    }
//
//    suspend fun getTagDetailsForLocation(location: LatLng): Pair<String?, String?> {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Pair(null, null)
//        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")
//
//        return try {
//            val snapshot = placeRef.get().await()
//            val matchingChild = snapshot.children.firstOrNull { childSnapshot ->
//                val latitude = childSnapshot.child("latitude").getValue(Double::class.java)
//                val longitude = childSnapshot.child("longitude").getValue(Double::class.java)
//                val isMatch = latitude != null && longitude != null &&
//                        Math.abs(latitude - location.latitude) < 0.0001 && // Tolerance for latitude
//                        Math.abs(longitude - location.longitude) < 0.0001 // Tolerance for longitude
//
//                isMatch
//            }
//
//            if (matchingChild != null) {
//                val id = matchingChild.key
//                val name = matchingChild.child("name").getValue(String::class.java)
//                Pair(id, name)
//            } else {
//                Pair(null, null)
//            }
//        } catch (e: Exception) {
//            Log.e("getTagDetailsForLocation", "Error fetching tag details: ${e.message}")
//            Pair(null, null)
//        }
//    }
//
//    suspend fun getPlaceTagByLocation(location: LatLng): PlaceTag? {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
//        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId")
//
//        return try {
//            val snapshot = placeRef.get().await()
//            snapshot.children.firstOrNull { childSnapshot ->
//                val latitude = childSnapshot.child("latitude").getValue(Double::class.java)
//                val longitude = childSnapshot.child("longitude").getValue(Double::class.java)
//
//                latitude != null && longitude != null &&
//                        Math.abs(latitude - location.latitude) < 0.0001 &&
//                        Math.abs(longitude - location.longitude) < 0.0001
//            }?.getValue(PlaceTag::class.java)
//        } catch (e: Exception) {
//            Log.e("getPlaceTagByLocation", "Error fetching place tag: ${e.message}")
//            null
//        }
//    }
//
//    fun modifyPlaceTag(tagId: String, latLng: LatLng, newTagName: String, category: String) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Log.e("modifyPlaceTag", "User not authenticated.")
//            return
//        }
//
//        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId/$tagId")
//        val updates = mapOf(
//            "name" to newTagName,
//            "latitude" to latLng.latitude,
//            "longitude" to latLng.longitude,
//            "category" to category
//        )
//
//        placeRef.updateChildren(updates)
//            .addOnSuccessListener {
//                Log.d("modifyPlaceTag", "Tag modified successfully: $tagId with new name: $newTagName")
//                loadPlaceTags() // Reload tags after modification
//            }
//            .addOnFailureListener { exception ->
//                Log.e("modifyPlaceTag", "Failed to modify tag: ${exception.message}")
//            }
//    }
//
//    fun deletePlaceTag(tagId: String) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid
//        if (userId == null) {
//            Log.e("deletePlaceTag", "User not authenticated.")
//            return
//        }
//
//        val placeRef = FirebaseDatabase.getInstance().getReference("placeTags/$userId/$tagId")
//        placeRef.removeValue()
//            .addOnSuccessListener {
//                Log.d("deletePlaceTag", "Tag deleted successfully: $tagId")
//                loadPlaceTags() // Reload tags after deletion
//            }
//            .addOnFailureListener { exception ->
//                Log.e("deletePlaceTag", "Failed to delete tag: ${exception.message}")
//            }
//    }
//
//    // Pet and User profile
//    private val _petProfileImages = MutableLiveData<Map<String, Uri>>()
//    val petProfileImages: LiveData<Map<String, Uri>> = _petProfileImages
//
//    private val _selectedPet = MutableLiveData<Pet?>()
//    val selectedPet: LiveData<Pet?> = _selectedPet
//
//    val _userProfile = MutableLiveData<UserProfile?>()
//    val userProfile: MutableLiveData<UserProfile?> = _userProfile
//
//    private val _userProfileImageUri = MutableLiveData<Uri>()
//    val userProfileImageUri: LiveData<Uri> = _userProfileImageUri
//
//    fun loadPetsProfile(userId: String) {
//        val petsRef = FirebaseDatabase.getInstance().getReference("pets/$userId")
//        Log.d("Debug", "Current User ID: $userId")
//
//        petsRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(datasnapshot: DataSnapshot) {
//                val petsList = datasnapshot.children.mapNotNull { it.getValue(Pet::class.java) }
//                _pets.postValue(petsList)
//
//                // Create a map of petId to profileImageUri
//                val petImagesMap = petsList.associate { pet ->
//                    pet.petId to Uri.parse(pet.profileImageUrl)
//                }
//                _petProfileImages.postValue(petImagesMap)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Log.e("loadPetsProfile", "Failed to load pets: ${error.message}")
//            }
//        })
//    }
//
//    fun uploadPetImage(userId: String, petId: String, uri: Uri, onImageUploaded: (String) -> Unit) {
//        uploadImageToFirebase(
//            uri = uri,
//            onSuccess = { imageUrl ->
//                // Update the pet's profile image URL in Firebase Realtime Database
//                val databaseReference = FirebaseDatabase.getInstance()
//                    .getReference("pets/$userId/$petId/profileImageUrl")
//                databaseReference.setValue(imageUrl)
//                    .addOnSuccessListener {
//                        Log.d("PetImageUpload", "Pet profile image URL successfully saved for petId: $petId")
//                        onImageUploaded(imageUrl) // Pass the image URL back to the composable
//                    }
//                    .addOnFailureListener { exception ->
//                        Log.e("PetImageUpload", "Failed to save pet image URL for petId: $petId", exception)
//                    }
//            },
//            onError = { error ->
//                Log.e("PetImageUpload", "Error uploading pet image for petId: $petId: $error")
//            }
//        )
//    }
//
//    fun addPet(userId: String, pet: Pet) {
//        val petsRef = FirebaseDatabase.getInstance().getReference("pets/$userId")
//        val petId = pet.petId
//        val petWithId = pet.copy(petId = petId)  // Ensure `petId` is set in the pet object
//
//        petsRef.child(petId).setValue(petWithId).addOnSuccessListener {
//            Log.d("AddPet", "Pet successfully added: ${petWithId.petId}")
//        }.addOnFailureListener { exception ->
//            Log.e("AddPet", "Failed to add pet", exception)
//        }
//    }
//
//    fun updatePet(userId: String, pet: Pet) {
//        val petsRef = FirebaseDatabase.getInstance().getReference("pets/$userId")
//        petsRef.child(pet.petId).setValue(pet)
//    }
//
//    fun setSelectedPet(pet: Pet?) {
//        _selectedPet.value = pet
//    }
//
//    fun askPetAdvice(
//        userId: String,
//        pet: Pet,
//        question: String,
//        onSuccess: (String) -> Unit,
//        onError: (String) -> Unit
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                // --- 1. Manually Fetch and Deserialize Feeding Records ---
//                val feedingSnapshot = FirebaseDatabase.getInstance()
//                    .getReference("feedings/$userId")
//                    .get().await()
//
//                val feeding = feedingSnapshot.children.mapNotNull { child ->
//                    val map = child.value as? Map<*, *> ?: return@mapNotNull null
//                    if (map["petId"] != pet.petId) return@mapNotNull null
//
//                    val mealTime = when (val mealTimeData = map["mealTime"]) {
//                        is Map<*, *> -> {
//                            try {
//                                LocalDateTime.of(
//                                    (mealTimeData["year"] as? Number)?.toInt() ?: 2000,
//                                    (mealTimeData["monthValue"] as? Number)?.toInt() ?: 1,
//                                    (mealTimeData["dayOfMonth"] as? Number)?.toInt() ?: 1,
//                                    (mealTimeData["hour"] as? Number)?.toInt() ?: 0,
//                                    (mealTimeData["minute"] as? Number)?.toInt() ?: 0,
//                                    (mealTimeData["second"] as? Number)?.toInt() ?: 0
//                                )
//                            } catch (e: Exception) {
//                                LocalDateTime.of(2000, 1, 1, 0, 0)
//                            }
//                        }
//                        is Number -> Instant.ofEpochMilli(mealTimeData.toLong())
//                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
//                        is String -> try {
//                            LocalDateTime.parse(mealTimeData)
//                        } catch (e: Exception) {
//                            LocalDateTime.of(2000, 1, 1, 0, 0)
//                        }
//                        else -> LocalDateTime.of(2000, 1, 1, 0, 0)
//                    }
//
//                    Feeding(
//                        petId = map["petId"] as? String ?: "",
//                        foodId = map["foodId"] as? String ?: "",
//                        amount = (map["amount"] as? Number)?.toFloat() ?: 0f,
//                        mealTime = mealTime,
//                        mealType = map["mealType"] as? String ?: "",
//                        notes = map["notes"] as? String ?: ""
//                    )
//                }
//
//                // --- 2. Manually Fetch and Deserialize Health Records ---
//                val healthSnapshot = FirebaseDatabase.getInstance()
//                    .getReference("healthRecords/$userId")
//                    .get().await()
//
//                val health = healthSnapshot.children.mapNotNull { child ->
//                    val map = child.value as? Map<*, *> ?: return@mapNotNull null
//                    if (map["petId"] != pet.petId) return@mapNotNull null
//
//                    // Extract the nested entryDate.time
//                    val entryDateMap = map["entryDate"] as? Map<*, *>
//                    val entryDateTime = (entryDateMap?.get("time") as? Number)?.toLong() ?: System.currentTimeMillis()
//
//                    HealthRecord(
//                        petId = map["petId"] as? String ?: "",
//                        entryDate = Date(entryDateTime),
//                        weight = (map["weight"] as? Number)?.toFloat() ?: 0f
//                    )
//                }
//
//                // --- 3. Fetch Related Events ---
//                val calendarSnapshot = FirebaseDatabase.getInstance()
//                    .getReference("events/$userId")
//                    .get().await()
//
//                val events = calendarSnapshot.children.mapNotNull { snap ->
//                    val map = snap.value as? Map<*, *> ?: return@mapNotNull null
//                    val title = map["title"] as? String ?: return@mapNotNull null
//                    val description = map["description"] as? String
//                    if (description?.contains(pet.petId) == true || title.contains(pet.name)) {
//                        title to description
//                    } else null
//                }
//
//                // --- 4. Format all info ---
//                val feedingInfo = feeding.joinToString("\n") {
//                    "- ${it.mealTime.toLocalDate()} ${it.mealType}: ${it.amount}g, notes: ${it.notes}"
//                }
//
//                val healthInfo = health.joinToString("\n") {
//                    "- ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.entryDate)}: weight = ${it.weight}"
//                }
//
//                val eventInfo = events.joinToString("\n") { "- ${it.first}: ${it.second}" }
//
//                // --- 5. Create OpenAI Prompt ---
//                val prompt = """
//                    Pet Profile:
//                    Name: ${pet.name}
//                    Type: ${pet.type}
//                    Breed: ${pet.breed}
//                    Sex: ${pet.selectedSex}
//                    Allergy: ${pet.allergy}
//                    Birth Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pet.birthDate)}
//
//                    Recent Feeding Records:
//                    $feedingInfo
//
//                    Health Records:
//                    $healthInfo
//
//                    Events:
//                    $eventInfo
//
//                    User Question: $question
//
//                    Provide a helpful, friendly response based on all the pet's data.
//                """.trimIndent()
//
//                Log.d("askPetAdvice", "Prompt sent to OpenAI:\n$prompt")
//
//                // --- 6. Call OpenAI ---
//                val requestBody = ChatRequest(
//                    model = "gpt-4o",
//                    messages = listOf(
//                        Message(role = "system", content = "You are a helpful and smart assistant."),
//                        Message(role = "user", content = prompt)
//                    ),
//                    max_tokens = 800
//                )
//
//                val response = RetrofitInstance.api.getChatResponse(requestBody)
//                val result = response.choices.firstOrNull()?.message?.content ?: "No response"
//                onSuccess(result.trim())
//
//            } catch (e: Exception) {
//                Log.e("askPetAdvice", "Error: ${e.message}", e)
//                onError("Failed to get pet advice: ${e.message}")
//            }
//        }
//    }
//
//    fun loadUserProfile(userId: String) {
//        val userRef = dbRef.child("userProfiles").child(userId)
//
//        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                val userProfile = dataSnapshot.getValue(UserProfile::class.java)
//                _userProfile.postValue(userProfile)
//
////                // Check if profile image URL exists and set it to profileImageUri
////                userProfile?.profileImageUrl?.let { imageUrl ->
////                    setImageUri(Uri.parse(imageUrl))
////                }
//            }
//
//            override fun onCancelled(databaseError: DatabaseError) {
//                Log.e("loadUserProfile", "Database error: ${databaseError.message}")
//            }
//        })
//    }
//
//    fun insertUser(userProfile: UserProfile) = viewModelScope.launch(Dispatchers.IO) {
//        userProfile.userId?.let { userId ->
//            dbRef.child("userProfiles").child(userId).setValue(userProfile)
//                .addOnSuccessListener {
//                    // Handle success
//                    Log.d("InsertUser", "User profile inserted successfully")
//                    // Update UI or notify user via LiveData or some other mechanism
//                }
//                .addOnFailureListener { exception ->
//                    // Handle failure
//                    Log.e("InsertUser", "Failed to insert user profile: ${exception.message}")
//                    // Optionally update UI to reflect failure
//                }
//        }
//    }
//
//    fun updateUser(userProfile: UserProfile, onSuccess: () -> Unit) {
//        viewModelScope.launch {
//            userProfile.userId?.let {
//                dbRef.child("userProfiles").child(it).setValue(userProfile).addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        onSuccess()
//                    } else {
//                        Log.e("UpdateUser", "Failed to update user: ${task.exception?.message}")
//                    }
//                }
//            }
////            try {
////                repository.updateUser(userProfile)
////                _userProfile.postValue(userProfile)
////                onSuccess()
////            } catch (e: Exception) {
////                Log.e("UpdateUser", "Failed to update user: ${e.message}")
////            }
//        }
//    }
//
//    fun deleteUser(userProfile: UserProfile) = viewModelScope.launch(Dispatchers.IO) {
//        userProfile.userId?.let {
//            dbRef.child("userProfiles").child(it).removeValue().addOnCompleteListener {
//                if (it.isSuccessful) {
//                    Log.d("DeleteUser", "User profile deleted successfully")
//                } else {
//                    Log.e("DeleteUser", "Failed to delete user profile: ${it.exception?.message}")
//                }
//            }
//        }
//    }
//}