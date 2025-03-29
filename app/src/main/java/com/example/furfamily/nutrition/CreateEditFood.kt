package com.example.furfamily.nutrition

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.furfamily.viewmodel.NutritionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewFood(
    onDismiss: () -> Unit,
    onSave: (Food) -> Unit,
    navController: NavController
) {
    val nutritionViewModel: NutritionViewModel = hiltViewModel()

    var category by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var ingredient by remember { mutableStateOf("") }
    var caloriesPerKg by remember { mutableStateOf(0.0F) }
    var size by remember { mutableStateOf(0) }
    var proteinPercentage by remember { mutableStateOf(0.0F) }
    var fatPercentage by remember { mutableStateOf(0.0F) }
    var fiberPercentage by remember { mutableStateOf(0.0F) }
    var moisturePercentage by remember { mutableStateOf(0.0F) }
    var ashPercentage by remember { mutableStateOf(0.0F) }
    var feedingInfo by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var extractedInfo by remember { mutableStateOf("") }
    var showLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Create New Food") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.Start // Align text to the left side
                ) {
                    Text("You can manually enter the food information or upload an image to extract the details!",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Food name and calories are required!",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp))
                }

                // Image Preview Section
                imageUri?.let { uri ->
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Uploaded Image",
                        modifier = Modifier
                            .size(150.dp) // Adjust the size as needed
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp)) // Add rounded corners
                            .border(1.dp, Color.LightGray) // Add border
                    )
                }

                // Choose Image Button
                val pickImage = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { imageUri = uri }
                }
                Button(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier
                        .wrapContentWidth() // Adjust button width to fit the text
                ) {
                    Text("Choose an Image")
                }

                // Extract Information Button
                imageUri?.let { uri ->
                    Button(
                        onClick = {
                            showLoading = true
                            nutritionViewModel.uploadImageToFirebase(uri,
                                onSuccess = { imageUrl ->
                                    // After successful upload, extract text from the image
                                    nutritionViewModel.extractTextFromImage(context, uri,
                                        onSuccess = { extractedText ->
                                            // After successful text extraction, send text to OpenAI
                                            nutritionViewModel.sendTextToOpenAI(extractedText,
                                                onSuccess = { foodInfo ->
                                                    // Update UI fields with the returned Food object
                                                    name = foodInfo.name
                                                    category = foodInfo.category
                                                    ingredient = foodInfo.ingredient
                                                    caloriesPerKg = foodInfo.caloriesPerKg
                                                    size = foodInfo.size
                                                    proteinPercentage = foodInfo.proteinPercentage
                                                    fatPercentage = foodInfo.fatPercentage
                                                    fiberPercentage = foodInfo.fiberPercentage
                                                    moisturePercentage = foodInfo.moisturePercentage
                                                    ashPercentage = foodInfo.ashPercentage
                                                    feedingInfo = foodInfo.feedingInfo
                                                    extractedInfo =
                                                        "Food details retrieved successfully. Please check and update if needed."
                                                    showLoading = false // Hide loading indicator
                                                },
                                                onError = { openAIError ->
                                                    // Handle OpenAI error
                                                    extractedInfo = "OpenAI Error: $openAIError"
                                                    showLoading = false // Hide loading indicator
                                                })
                                        },
                                        onError = { extractionError ->
                                            // Handle text extraction error
                                            extractedInfo = "Text Extraction Error: $extractionError"
                                            showLoading = false // Hide loading indicator
                                        })
                                },
                                onError = { uploadError ->
                                    // Handle image upload error
                                    extractedInfo = "Image Upload Error: $uploadError"
                                    showLoading = false // Hide loading indicator
                                })
                        },
                        modifier = Modifier
                            .wrapContentWidth() // Adjust button width to fit the text
                    ) {
                        Text("Retrieve Food Details")
                    }
                }

                // Loading Indicator
                if (showLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Extracted Information
                if (extractedInfo.isNotEmpty()) {
                    Text(extractedInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                    )
                }

                // Food Fields
                Text(
                    "Food Category *",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = category == "Dry Food",
                            onClick = { category = "Dry Food" }
                        )
                        Text("Dry Food", modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = category == "Wet Food",
                            onClick = { category = "Wet Food" }
                        )
                        Text("Wet Food", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                TextField(
                    value = caloriesPerKg.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            caloriesPerKg = parsedValue
                        }
                    },
                    label = { Text("Calories (kcal/kg) *") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = ingredient,
                    onValueChange = { ingredient = it },
                    label = { Text("Ingredient") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                TextField(
                    value = size.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toIntOrNull()
                        if (parsedValue != null && parsedValue > 0) {
                            size = parsedValue
                        }
                    },
                    label = {
                        Text(
                            when (category) {
                                "Dry Food" -> "Bag Size (g)"
                                "Wet Food" -> "Can/Pouch Size (g) *"
                                else -> "Size (g)"
                            }
                        )
                    },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = proteinPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            proteinPercentage = parsedValue
                        }
                    },
                    label = { Text("Protein (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = fatPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fatPercentage = parsedValue
                        }
                    },
                    label = { Text("Fat (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = fiberPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fiberPercentage = parsedValue
                        }
                    },
                    label = { Text("Fiber (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = moisturePercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            moisturePercentage = parsedValue
                        }
                    },
                    label = { Text("Moisture (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = ashPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            ashPercentage = parsedValue
                        }
                    },
                    label = { Text("Ash (%)") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(), // Set max width for the TextField
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                TextField(
                    value = feedingInfo,
                    onValueChange = { feedingInfo = it },
                    label = { Text("Feeding Guidance") },
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth() // Set max width for the TextField
                )
                Button(
                    onClick = {
                        val food = Food(category = category,
                                        name = name,
                                        ingredient = ingredient,
                                        caloriesPerKg = caloriesPerKg,
                                        size = size,
                                        proteinPercentage = proteinPercentage,
                                        fatPercentage = fatPercentage,
                                        fiberPercentage = fiberPercentage,
                                        moisturePercentage = moisturePercentage,
                                        ashPercentage = ashPercentage,
                                        feedingInfo = feedingInfo)
                        onSave(food)
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && category.isNotBlank() && caloriesPerKg > 0 && size > 0, // Enable only when required fields are filled
                    modifier = Modifier
                        .fillMaxWidth() // Set button to max width
                        .padding(vertical = 8.dp)
                ) {
                    Text("Save")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodDialog(food: Food, onDismiss: () -> Unit, onSave: (Food) -> Unit) {
    var category by remember { mutableStateOf(food.category) }
    var name by remember { mutableStateOf(food.name) }
    var ingredient by remember { mutableStateOf(food.ingredient) }
    var caloriesPerKg by remember { mutableStateOf(food.caloriesPerKg) }
    var size by remember { mutableStateOf(food.size) }
    var proteinPercentage by remember { mutableStateOf(food.proteinPercentage) }
    var fatPercentage by remember { mutableStateOf(food.fatPercentage) }
    var fiberPercentage by remember { mutableStateOf(food.fiberPercentage) }
    var moisturePercentage by remember { mutableStateOf(food.moisturePercentage) }
    var ashPercentage by remember { mutableStateOf(food.ashPercentage) }
    var feedingInfo by remember { mutableStateOf(food.feedingInfo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Food Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Food Fields for editing
                Text(
                    "Food Category *",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = category == "Dry Food",
                        onClick = { category = "Dry Food" }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dry Food")
                    RadioButton(
                        selected = category == "Wet Food",
                        onClick = { category = "Wet Food" }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wet Food")
                }
                Spacer(modifier = Modifier.height(4.dp))

                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = caloriesPerKg.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            caloriesPerKg = parsedValue
                        }
                    },
                    label = { Text("Calories (kcal/kg) *", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = ingredient,
                    onValueChange = { ingredient = it },
                    label = { Text("Ingredient", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = size.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toIntOrNull()
                        if (parsedValue != null && parsedValue > 0) {
                            size = parsedValue
                        }
                    },
                    label = {
                        Text(
                            when (category) {
                                "Dry Food" -> "Bag Size (g)"
                                "Wet Food" -> "Can/Pouch Size (g) *"
                                else -> "Size (g)" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = proteinPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            proteinPercentage = parsedValue
                        }
                    },
                    label = { Text("Protein (%)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = fatPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fatPercentage = parsedValue
                        }
                    },
                    label = { Text("Fat (%)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = fiberPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            fiberPercentage = parsedValue
                        }
                    },
                    label = { Text("Fiber (%)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = moisturePercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            moisturePercentage = parsedValue
                        }
                    },
                    label = { Text("Moisture (%)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = ashPercentage.toString(),
                    onValueChange = { input ->
                        val parsedValue = input.toFloatOrNull()
                        if (parsedValue != null) {
                            ashPercentage = parsedValue
                        }
                    },
                    label = { Text("Ash (%)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = feedingInfo,
                    onValueChange = { feedingInfo = it },
                    label = { Text("Feeding Guidance", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val updatedFood = food.copy(
                            category = category,
                            name = name,
                            ingredient = ingredient,
                            caloriesPerKg = caloriesPerKg,
                            size = size,
                            proteinPercentage = proteinPercentage,
                            fatPercentage = fatPercentage,
                            fiberPercentage = fiberPercentage,
                            moisturePercentage = moisturePercentage,
                            ashPercentage = ashPercentage,
                            feedingInfo = feedingInfo)
                        onSave(updatedFood)
                    },
                    enabled = name.isNotBlank() && category.isNotBlank() && caloriesPerKg > 0 && size > 0 // Enable only when required fields are filled
                ) {
                    Text("Save")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}