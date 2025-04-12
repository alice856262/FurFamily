package com.example.furfamily.viewmodel

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.furfamily.health.HealthRecord
import com.example.furfamily.network.ChatRequest
import com.example.furfamily.network.Message
import com.example.furfamily.network.RetrofitInstance
import com.example.furfamily.nutrition.Feeding
import com.example.furfamily.nutrition.Food
import com.example.furfamily.profile.Pet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _foodList = MutableLiveData<List<Food>>()
    val foodList: LiveData<List<Food>> = _foodList

    private val _petWeights = MutableLiveData<Map<String, Float>>()
    val petWeights: LiveData<Map<String, Float>> = _petWeights

    private val _feedingEvents = MutableLiveData<List<Feeding>>()
    val feedingEvents: LiveData<List<Feeding>> = _feedingEvents

    private val _allFeedingEvents = MutableLiveData<List<Feeding>>()
    val allFeedingEvents: LiveData<List<Feeding>> = _allFeedingEvents

    init {
        fetchLatestWeight()
        loadFoodList()
    }

    // Function to load the food list from Firebase
    fun loadFoodList() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$uid")

        foodRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val foodListFromDb = mutableListOf<Food>()
                for (foodSnapshot in snapshot.children) {
                    val food = foodSnapshot.getValue(Food::class.java)
                    val foodId = foodSnapshot.key // Get the key as foodId
                    food?.let {
                        it.foodId = foodId ?: ""  // Assign the foodId
                        foodListFromDb.add(it)
                    }
                }
                _foodList.postValue(foodListFromDb)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ViewModel", "Error loading food list: ${error.message}")
            }
        })
    }

    // Fetch Food Detail by ID
    fun getFoodById(foodId: String, onResult: (Food?) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$uid/$foodId")

        foodRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val food = snapshot.getValue(Food::class.java)
                    food?.foodId = foodId
                    onResult(food)
                } else {
                    onResult(null) // No food found
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error fetching food: ${exception.message}")
                onResult(null)
            }
    }

    // Function to add a new food
    fun addNewFood(food: Food) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Reference to the user's food node in Firebase
        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$userId")
        val foodId = foodRef.push().key ?: throw IllegalStateException("Failed to get Firebase key")
        // Create a map to save the food data
        val foodMap = mapOf(
            "name" to food.name,
            "category" to food.category,
            "ingredient" to food.ingredient,
            "caloriesPerKg" to food.caloriesPerKg,
            "size" to food.size,
            "proteinPercentage" to food.proteinPercentage,
            "fatPercentage" to food.fatPercentage,
            "fiberPercentage" to food.fiberPercentage,
            "moisturePercentage" to food.moisturePercentage,
            "ashPercentage" to food.ashPercentage,
            "feedingInfo" to food.feedingInfo
        )
        // Save the new food to Firebase
        foodRef.child(foodId).setValue(foodMap)
            .addOnSuccessListener {
                Log.d("Firebase", "Food saved successfully")
                loadFoodList()  // Reload the food list from Firebase after saving
            }
            .addOnFailureListener {
                Log.e("FirebaseError", "Error saving food: ${it.message}")
            }
    }

    fun updateFood(food: Food) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val foodId = food.foodId
        if (foodId.isBlank()) {
            Log.e("ViewModel", "Invalid food ID")
            return
        }

        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$userId/$foodId")
        val foodUpdates = mapOf(
            "name" to food.name,
            "category" to food.category,
            "ingredient" to food.ingredient,
            "caloriesPerKg" to food.caloriesPerKg,
            "size" to food.size,
            "proteinPercentage" to food.proteinPercentage,
            "fatPercentage" to food.fatPercentage,
            "fiberPercentage" to food.fiberPercentage,
            "moisturePercentage" to food.moisturePercentage,
            "ashPercentage" to food.ashPercentage,
            "feedingInfo" to food.feedingInfo
        )
        // Update the food item in Firebase
        foodRef.updateChildren(foodUpdates)
            .addOnSuccessListener {
                Log.d("Firebase", "Food updated successfully")
                loadFoodList() // Reload the food list after updating
            }
            .addOnFailureListener {
                Log.e("FirebaseError", "Error updating food: ${it.message}")
            }
    }

    fun deleteFood(food: Food) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val foodId = food.foodId
        if (foodId.isBlank()) {
            Log.e("ViewModel", "Invalid food ID")
            return
        }

        val foodRef = FirebaseDatabase.getInstance().getReference("foods/$userId/$foodId")
        foodRef.removeValue()
            .addOnSuccessListener {
                Log.d("Firebase", "Food deleted successfully")
                loadFoodList() // Reload the food list after deletion
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Error deleting food: ${exception.message}")
            }
    }

    // Function to fetch the latest weight record for each pet
    fun fetchLatestWeight() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Reference to the healthRecords node for the current user
        val healthRecordsRef = FirebaseDatabase.getInstance().getReference("healthRecords/$userId")

        // Query to get all health records ordered by entryDate.time
        healthRecordsRef.orderByChild("entryDate/time")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.d("ViewModel", "Health records found: ${snapshot.childrenCount}")

                        // Create a map to store the latest weight for each pet
                        val latestWeights = mutableMapOf<String, Float>()

                        // Process each health record
                        snapshot.children.forEach { child ->
                            val healthRecord = child.getValue(HealthRecord::class.java)
                            if (healthRecord != null && healthRecord.petId != null) {
                                val weight = healthRecord.weight
                                if (weight != null) {
                                    // Only update the weight if it's more recent than what we have for this pet
                                    val currentLatestWeight = latestWeights[healthRecord.petId]
                                    if (currentLatestWeight == null || 
                                        healthRecord.entryDate.after(Date(currentLatestWeight.toLong()))) {
                                        latestWeights[healthRecord.petId] = weight
                                    }
                                }
                            }
                        }

                        // Update the LiveData with the latest weights for each pet
                        _petWeights.postValue(latestWeights)
                    } else {
                        Log.d("ViewModel", "No health records found")
                        _petWeights.postValue(emptyMap())
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ViewModel", "Error fetching latest weights: ${error.message}")
                    _petWeights.postValue(emptyMap())
                }
            })
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

    fun extractTextFromImage(context: Context, uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        try {
            // Convert URI to Bitmap
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            // Log the bitmap information
            Log.d("extractTextFromImage", "Bitmap obtained with size: ${bitmap.width}x${bitmap.height}")
            // Create an InputImage from the bitmap
            val image = InputImage.fromBitmap(bitmap, 0)
            // Initialize TextRecognizer
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            // Process the image for text recognition
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    Log.d("extractTextFromImage", "Text extraction successful. Text found: ${visionText.text}")
                    // If there is text, pass it to the onSuccess callback
                    if (visionText.text.isNotEmpty()) {
                        onSuccess(visionText.text)
                    } else {
                        onSuccess("No text found in the image.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("extractTextFromImage", "Failed to process image for text: ${e.message}")
                    onError("Failed to process image for text: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("extractTextFromImage", "Failed to load image: ${e.message}")
            onError("Failed to load image: ${e.message}")
        }
    }

    fun sendTextToOpenAI(text: String, onSuccess: (Food) -> Unit, onError: (String) -> Unit) {
        val requestBody = ChatRequest(
            model = "gpt-4o", // or any other model you prefer
            messages = listOf(
                Message(role = "system",
                    content = """
                                  Please extract the following information from the provided text in JSON format: 
                                  {
                                    "name": "string",
                                    "ingredient": "string",
                                    "caloriesPerKg": float,
                                    "feedingInfo": "string",
                                    "proteinPercentage": float,
                                    "fatPercentage": float,
                                    "fiberPercentage": float,
                                    "moisturePercentage": float,
                                    "ashPercentage": float
                                  }
                                  Default for string is an empty string. Default for float is 0.0. 
                                  Respond only with the JSON object.
                              """.trimIndent()
                ),
                Message(role = "user", content = text)
            ),
            max_tokens = 500 // Adjust max tokens as needed
        )
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getChatResponse(requestBody)
                val result = response.choices.firstOrNull()?.message?.content ?: "No response"

                // Clean the response to remove any non-JSON content
                val cleanedResult = cleanJsonResponse(result)

                // Attempt to parse the cleaned result into a Food object
                val food = parseFoodFromResponse(cleanedResult)
                if (food != null) {
                    onSuccess(food)
                } else {
                    onError("Failed to parse food information from response.")
                }
            } catch (e: Exception) {
                onError("Failed to get response from OpenAI: ${e.message}")
            }
        }
    }

    // Clean the JSON response by removing backticks and any non-JSON text
    fun cleanJsonResponse(response: String): String {
        return response.trim()
            .replace("```json", "") // Remove ```json if it exists
            .replace("```", "") // Remove closing ```
            .replace("\n", "") // Remove newlines if necessary
    }

    // Parse the JSON response and create a Food object
    fun parseFoodFromResponse(response: String): Food? {
        return try {
            val cleanedResponse = cleanJsonResponse(response)
            Log.d("parseFoodFromResponse", "Cleaned Response: $cleanedResponse")

            // Assuming the response is a JSON object in the format specified
            val jsonObject = JSONObject(cleanedResponse)
            val name = jsonObject.optString("name", "")
            val ingredients = jsonObject.optString("ingredient", "") // Check the exact key here
            val caloriesPerKg = jsonObject.optDouble("caloriesPerKg", 0.0).toFloat()
            val feedingInfo = jsonObject.optString("feedingInfo", "")
            val proteinPercentage = jsonObject.optDouble("proteinPercentage", 0.0).toFloat()
            val fatPercentage = jsonObject.optDouble("fatPercentage", 0.0).toFloat()
            val fiberPercentage = jsonObject.optDouble("fiberPercentage", 0.0).toFloat()
            val moisturePercentage = jsonObject.optDouble("moisturePercentage", 0.0).toFloat()
            val ashPercentage = jsonObject.optDouble("ashPercentage", 0.0).toFloat()

            // Log the parsed ingredient to see if it's correct
            Log.d("parseFoodFromResponse", "Parsed Ingredient: $ingredients")

            Food(
                name = name,
                ingredient = ingredients,
                caloriesPerKg = caloriesPerKg,
                feedingInfo = feedingInfo,
                proteinPercentage = proteinPercentage,
                fatPercentage = fatPercentage,
                fiberPercentage = fiberPercentage,
                moisturePercentage = moisturePercentage,
                ashPercentage = ashPercentage
            )
        } catch (e: JSONException) {
            Log.e("parseFoodFromResponse", "Failed to parse response: ${e.message}")
            null
        }
    }

    // Function to fetch feeding details
    fun loadFeedingEventsForDate(date: LocalDate) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$uid")
        val startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        feedingRef.orderByChild("entryDate/time")
            .startAt(startOfDay.toDouble())
            .endAt(endOfDay.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val feedingEvents = mutableListOf<Feeding>()

                    for (childSnapshot in snapshot.children) {
                        val feedingMap = childSnapshot.value as? Map<*, *> ?: continue

                        val mealTime = when (val mealTimeData = feedingMap["mealTime"]) {
                            is Map<*, *> -> { // If stored as a Map, reconstruct LocalDateTime
                                try {
                                    LocalDateTime.of(
                                        (mealTimeData["year"] as? Number)?.toInt() ?: 2000,
                                        (mealTimeData["monthValue"] as? Number)?.toInt() ?: 1, // ✅ Use monthValue directly
                                        (mealTimeData["dayOfMonth"] as? Number)?.toInt() ?: 1, // ✅ Correct day field
                                        (mealTimeData["hour"] as? Number)?.toInt() ?: 0,
                                        (mealTimeData["minute"] as? Number)?.toInt() ?: 0,
                                        (mealTimeData["second"] as? Number)?.toInt() ?: 0
                                    )
                                } catch (e: Exception) {
                                    Log.e("Firebase", "Error parsing mealTime Map: ${e.message}")
                                    LocalDateTime.of(2000, 1, 1, 0, 0) // Default value
                                }
                            }
                            is Number -> { // If stored as a timestamp, convert it
                                Instant.ofEpochMilli(mealTimeData.toLong())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime()
                            }
                            is String -> { // If stored as a string (possible edge case)
                                try {
                                    LocalDateTime.parse(mealTimeData)
                                } catch (e: Exception) {
                                    Log.e("Firebase", "Error parsing mealTime String: $mealTimeData")
                                    LocalDateTime.of(2000, 1, 1, 0, 0) // Default
                                }
                            }
                            else -> {
                                Log.e("Firebase", "Unknown mealTime format: $mealTimeData")
                                LocalDateTime.of(2000, 1, 1, 0, 0) // Default
                            }
                        }

                        val feeding = Feeding(
                            petId = feedingMap["petId"] as? String ?: "",
                            foodId = feedingMap["foodId"] as? String ?: "",
                            entryDate = Date(feedingMap["entryDate"] as? Long ?: System.currentTimeMillis()),
                            amount = (feedingMap["amount"] as? Number)?.toFloat() ?: 0.0F,
                            mealTime = mealTime,
                            mealType = feedingMap["mealType"] as? String ?: "",
                            notes = feedingMap["notes"] as? String ?: ""
                        )

                        feedingEvents.add(feeding)
                    }
                    _feedingEvents.postValue(feedingEvents)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Failed to load feeding events: ${error.message}")
                }
            })
    }

    // Function to save feeding info to Firebase
    fun saveFeeding(pet: Pet, food: Food, amount: Float, mealTime: LocalDateTime, mealType: String, notes: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$userId")
        val feedingId = feedingRef.push().key ?: throw IllegalStateException("Failed to get Firebase key")
        // Create a Feeding object using the updated data class
        val feeding = Feeding(
            feedingId = feedingId,
            petId = pet.petId,
            foodId = food.foodId,
            entryDate = Date(),
            amount = amount,
            mealTime = mealTime,
            mealType = mealType,
            notes = notes
        )
        // Save the Feeding object to Firebase
        feedingRef.child(feedingId).setValue(feeding)
            .addOnSuccessListener {
                Log.d("Firebase", "Feeding saved successfully")
            }
            .addOnFailureListener {
                Log.e("FirebaseError", "Error saving feeding: ${it.message}")
            }
    }

    // Function to update an existing feeding record
    fun updateFeeding(feeding: Feeding) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val feedingId = feeding.feedingId
        if (feedingId.isBlank()) {
            Log.e("ViewModel", "Invalid feeding ID")
            return
        }

        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$userId/$feedingId")
        feedingRef.setValue(feeding)
            .addOnSuccessListener {
                Log.d("Firebase", "Feeding updated successfully")
                loadFeedingEventsForDate(feeding.mealTime.toLocalDate())
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Error updating feeding: ${exception.message}")
            }
    }

    // Function to delete a feeding record
    fun deleteFeeding(feeding: Feeding) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val feedingId = feeding.feedingId
        if (feedingId.isBlank()) {
            Log.e("ViewModel", "Invalid feeding ID")
            return
        }

        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$userId/$feedingId")
        feedingRef.removeValue()
            .addOnSuccessListener {
                Log.d("Firebase", "Feeding deleted successfully")
                loadFeedingEventsForDate(feeding.mealTime.toLocalDate())
                // Also reload all feeding events
                loadAllFeedingEvents()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Error deleting feeding: ${exception.message}")
            }
    }

    // Function to load all feeding records for the current user
    fun loadAllFeedingEvents() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val feedingRef = FirebaseDatabase.getInstance().getReference("feedings/$uid")

        feedingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allFeedings = mutableListOf<Feeding>()

                for (childSnapshot in snapshot.children) {
                    val feedingMap = childSnapshot.value as? Map<*, *> ?: continue
                    val feedingId = childSnapshot.key ?: continue

                    val mealTime = when (val mealTimeData = feedingMap["mealTime"]) {
                        is Map<*, *> -> { // If stored as a Map, reconstruct LocalDateTime
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
                                Log.e("Firebase", "Error parsing mealTime Map: ${e.message}")
                                LocalDateTime.of(2000, 1, 1, 0, 0) // Default value
                            }
                        }
                        is Number -> { // If stored as a timestamp, convert it
                            Instant.ofEpochMilli(mealTimeData.toLong())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                        }
                        is String -> { // If stored as a string (possible edge case)
                            try {
                                LocalDateTime.parse(mealTimeData)
                            } catch (e: Exception) {
                                Log.e("Firebase", "Error parsing mealTime String: $mealTimeData")
                                LocalDateTime.of(2000, 1, 1, 0, 0) // Default
                            }
                        }
                        else -> {
                            Log.e("Firebase", "Unknown mealTime format: $mealTimeData")
                            LocalDateTime.of(2000, 1, 1, 0, 0) // Default
                        }
                    }

                    val feeding = Feeding(
                        feedingId = feedingId,
                        petId = feedingMap["petId"] as? String ?: "",
                        foodId = feedingMap["foodId"] as? String ?: "",
                        entryDate = Date(feedingMap["entryDate"] as? Long ?: System.currentTimeMillis()),
                        amount = (feedingMap["amount"] as? Number)?.toFloat() ?: 0.0F,
                        mealTime = mealTime,
                        mealType = feedingMap["mealType"] as? String ?: "",
                        notes = feedingMap["notes"] as? String ?: ""
                    )

                    allFeedings.add(feeding)
                }
                
                // Sort feedings by meal time (newest first)
                allFeedings.sortByDescending { it.mealTime }
                _allFeedingEvents.postValue(allFeedings)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Failed to load all feeding events: ${error.message}")
            }
        })
    }
}