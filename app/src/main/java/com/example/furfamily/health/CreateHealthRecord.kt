package com.example.furfamily.health

import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.furfamily.profile.Pet
import com.example.furfamily.viewmodel.ProfileViewModel
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(0)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHealthRecord(healthRecord: HealthRecord, userId: String, onSaveMetrics: (HealthRecord) -> Unit, navController: NavController) {
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val pets by profileViewModel.pets.observeAsState(emptyList())
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = healthRecord.entryDate.time)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var entryDate by rememberSaveable { mutableStateOf(healthRecord.entryDate.time) }

    // Local state for form fields
    var selectedPet by remember { mutableStateOf<Pet?>(null) } // Track selected pet
    var isPetDropdownExpanded by remember { mutableStateOf(false) }
    var weight by rememberSaveable { mutableStateOf(healthRecord.weight ?: 0f) }
    var temperature by rememberSaveable { mutableStateOf(healthRecord.temperature ?: 0f) }
    var rbc by rememberSaveable { mutableStateOf(healthRecord.rbc ?: 0f) }
    var wbc by rememberSaveable { mutableStateOf(healthRecord.wbc ?: 0f) }
    var plt by rememberSaveable { mutableStateOf(healthRecord.plt ?: 0f) }
    var alb by rememberSaveable { mutableStateOf(healthRecord.alb ?: 0f) }
    var ast by rememberSaveable { mutableStateOf(healthRecord.ast ?: 0f) }
    var alt by rememberSaveable { mutableStateOf(healthRecord.alt ?: 0f) }
    var bun by rememberSaveable { mutableStateOf(healthRecord.bun ?: 0f) }
    var scr by rememberSaveable { mutableStateOf(healthRecord.scr ?: 0f) }

    LaunchedEffect(userId) {
        profileViewModel.loadPetsProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Create New Health Record") },
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPetDropdownExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = isPetDropdownExpanded,
                        onDismissRequest = { isPetDropdownExpanded = false }
                    ) {
                        if (pets.isEmpty()) {
                            Text("No pets available") // Handle empty pets list
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
                    ) { Text(text = "Enter Record Date") }
                    Spacer(modifier = Modifier.width(12.dp))
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                    Text(
                        text = "${formatter.format(Date(entryDate))}",
                        modifier = Modifier.weight(1f)
                    )
                }
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                entryDate = datePickerState.selectedDateMillis ?: entryDate
                            }) { Text(text = "OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel") }
                        }
                    ) { DatePicker(state = datePickerState) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Adjustable fields
                AdjustableAttribute("Weight: ${"%.1f".format(weight)} kg", weight, 0f..20f) { weight = it }
                AdjustableAttribute("Body Temperature: ${"%.1f".format(temperature)} °C", temperature, 35f..45f) { temperature = it }
                AdjustableAttribute("RBC: ${"%.1f".format(rbc)}", rbc, 0f..10f) { rbc = it }
                AdjustableAttribute("WBC: ${"%.1f".format(wbc)}", wbc, 0f..10f) { wbc = it }
                AdjustableAttribute("PLT: ${plt.toInt()}", plt, 0f..300f) { plt = it }
                AdjustableAttribute("Alb (Albumin): ${"%.1f".format(alb)}", alb, 0f..10f) { alb = it }
                AdjustableAttribute("AST: ${ast.toInt()}", ast, 0f..300f) { ast = it }
                AdjustableAttribute("ALT: ${alt.toInt()}", alt, 0f..300f) { alt = it }
                AdjustableAttribute("BUN: ${bun.toInt()}", bun, 0f..100f) { bun = it }
                AdjustableAttribute("Scr (Serum creatinine): ${"%.1f".format(scr)}", scr, 0f..10f) { scr = it }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        if (selectedPet == null) {
                            // Handle case where no pet is selected
                            return@Button
                        }
                        val record = HealthRecord(
                            userId = userId,
                            petId = selectedPet?.petId ?: "",
                            entryDate = Date(entryDate),
                            weight = weight,
                            temperature = temperature,
                            rbc = rbc,
                            wbc = wbc,
                            plt = plt,
                            alb = alb,
                            ast = ast,
                            alt = alt,
                            bun = bun,
                            scr = scr
                        )

                        val userRecordsRef = FirebaseDatabase.getInstance().getReference("healthRecords/$userId")
                        val recordKey = userRecordsRef.push().key
                        recordKey?.let {
                            userRecordsRef.child(it).setValue(record)
                                .addOnSuccessListener {
                                    onSaveMetrics(record)
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("healthRecordSaved", selectedPet?.name ?: "")
                                    navController.popBackStack()
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("SaveHealthRecord", "Failed to save health record: ${exception.message}")
                                }
                        }
                    },
                    enabled = selectedPet != null, // Enable button only if a pet is selected
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Records")
                }
            }
        }
    )
}

@Composable
fun AdjustableAttribute(
    label: String,
    initialValue: Float,
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
                value = value,
                onValueChange = {
                    value = it
                    onValueChange(it)
                },
                valueRange = range,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
