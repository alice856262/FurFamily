package com.example.furfamily.health

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.HealthViewModel
import com.example.furfamily.viewmodel.ProfileViewModel
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(0)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHealthRecord(
    userId: String,
    petId: String,
    navController: NavController,
    recordId: String? = null
) {
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val healthViewModel: HealthViewModel = hiltViewModel()
    val pets by profileViewModel.pets.observeAsState(emptyList())
    
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var isPetDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var entryDate by remember { mutableStateOf(Date()) }
    val datePickerState = rememberDatePickerState(entryDate.time)
    var weight by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var rbc by remember { mutableStateOf("") }
    var wbc by remember { mutableStateOf("") }
    var plt by remember { mutableStateOf("") }
    var alb by remember { mutableStateOf("") }
    var ast by remember { mutableStateOf("") }
    var alt by remember { mutableStateOf("") }
    var bun by remember { mutableStateOf("") }
    var scr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Load pets when the screen is displayed
    LaunchedEffect(userId) {
        profileViewModel.fetchPets(userId)
    }

    // Set selected pet based on petId
    LaunchedEffect(petId, pets) {
        if (petId.isNotEmpty()) {
            selectedPet = pets.find { it.petId == petId }
        }
    }

    // Load existing record if editing
    LaunchedEffect(recordId) {
        if (!recordId.isNullOrEmpty()) {
            // Fetch the record from Firebase
            FirebaseDatabase.getInstance()
                .getReference("healthRecords")
                .child(userId)
                .child(recordId)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.getValue(HealthRecord::class.java)?.let { record ->
                        entryDate = record.entryDate
                        weight = record.weight?.toString() ?: ""
                        temperature = record.temperature?.toString() ?: ""
                        rbc = record.rbc?.toString() ?: ""
                        wbc = record.wbc?.toString() ?: ""
                        plt = record.plt?.toString() ?: ""
                        alb = record.alb?.toString() ?: ""
                        ast = record.ast?.toString() ?: ""
                        alt = record.alt?.toString() ?: ""
                        bun = record.bun?.toString() ?: ""
                        scr = record.scr?.toString() ?: ""
                        notes = record.notes ?: ""
                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (recordId.isNullOrEmpty()) "Create New Health Record" else "Edit Health Record") },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Choose pet
                ExposedDropdownMenuBox(
                    expanded = isPetDropdownExpanded,
                    onExpandedChange = { isPetDropdownExpanded = it }
                ) {
                    TextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        readOnly = true,
                        value = selectedPet?.name ?: "Please choose one",
                        onValueChange = {},
                        label = { Text("Pet Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPetDropdownExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isPetDropdownExpanded,
                        onDismissRequest = { isPetDropdownExpanded = false }
                    ) {
                        if (pets.isEmpty()) {
                            Text("No pets available")
                        }
                        pets.forEach { pet ->
                            DropdownMenuItem(
                                text = { Text(pet.name) },
                                onClick = {
                                    selectedPet = pet
                                    isPetDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) { Text("Enter Record Date") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(entryDate),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let {
                                        entryDate = Date(it)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Adjustable fields for health metrics
                AdjustableAttribute(
                    "Weight: ${weight.toFloatOrNull()?.let { "%.1f".format(it) } ?: "0.0"} kg",
                    weight,
                    0f..20f
                ) { weight = it.toString() }
                AdjustableAttribute(
                    "Body Temperature: ${temperature.toFloatOrNull()?.let { "%.1f".format(it) } ?: "35.0"} °C",
                    temperature,
                    35f..45f
                ) { temperature = it.toString() }
                AdjustableAttribute(
                    "RBC: ${rbc.toFloatOrNull()?.let { "%.1f".format(it) } ?: "0.0"}",
                    rbc,
                    0f..10f
                ) { rbc = it.toString() }
                AdjustableAttribute(
                    "WBC: ${wbc.toFloatOrNull()?.let { "%.1f".format(it) } ?: "0.0"}",
                    wbc,
                    0f..10f
                ) { wbc = it.toString() }
                AdjustableAttribute(
                    "PLT: ${plt.toFloatOrNull()?.let { it.toInt().toString() } ?: "0"}",
                    plt,
                    0f..300f
                ) { plt = it.toString() }
                AdjustableAttribute(
                    "Alb (Albumin): ${alb.toFloatOrNull()?.let { "%.1f".format(it) } ?: "0.0"}",
                    alb,
                    0f..10f
                ) { alb = it.toString() }
                AdjustableAttribute(
                    "AST: ${ast.toFloatOrNull()?.let { it.toInt().toString() } ?: "0"}",
                    ast,
                    0f..300f
                ) { ast = it.toString() }
                AdjustableAttribute(
                    "ALT: ${alt.toFloatOrNull()?.let { it.toInt().toString() } ?: "0"}",
                    alt,
                    0f..300f
                ) { alt = it.toString() }
                AdjustableAttribute(
                    "BUN: ${bun.toFloatOrNull()?.let { it.toInt().toString() } ?: "0"}",
                    bun,
                    0f..100f
                ) { bun = it.toString() }
                AdjustableAttribute(
                    "Scr (Serum creatinine): ${scr.toFloatOrNull()?.let { "%.1f".format(it) } ?: "0.0"}",
                    scr,
                    0f..10f
                ) { scr = it.toString() }

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        if (selectedPet == null) {
                            // Handle case where no pet is selected
                            return@Button
                        }

                        val record = HealthRecord(
                            recordId = recordId ?: "",
                            userId = userId,
                            petId = selectedPet?.petId ?: "",
                            entryDate = entryDate,
                            weight = weight.toFloatOrNull(),
                            temperature = temperature.toFloatOrNull(),
                            rbc = rbc.toFloatOrNull(),
                            wbc = wbc.toFloatOrNull(),
                            plt = plt.toFloatOrNull(),
                            alb = alb.toFloatOrNull(),
                            ast = ast.toFloatOrNull(),
                            alt = alt.toFloatOrNull(),
                            bun = bun.toFloatOrNull(),
                            scr = scr.toFloatOrNull(),
                            notes = notes
                        )

                        if (recordId.isNullOrEmpty()) {
                            healthViewModel.addHealthRecord(userId, record)
                        } else {
                            healthViewModel.updateHealthRecord(userId, record)
                        }

                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "healthRecordSaved",
                            selectedPet?.name ?: ""
                        )
                        navController.popBackStack()
                    },
                    enabled = selectedPet != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (recordId.isNullOrEmpty()) "Save Record" else "Save Changes")
                }
            }
        }
    )
}

@Composable
fun AdjustableAttribute(
    label: String,
    initialValue: String,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    var showSlider by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(initialValue) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()) {
            Switch(
                checked = showSlider,
                onCheckedChange = { showSlider = it }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
        if (showSlider) {
            Slider(
                value = value.toFloatOrNull() ?: range.start,
                onValueChange = {
                    value = it.toString()
                    onValueChange(it)
                },
                valueRange = range,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
