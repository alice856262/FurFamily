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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(0)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHealthRecord(
    HealthRecord: HealthRecord,
    userId: String,
    onSaveMetrics: (HealthRecord) -> Unit,
    navController: NavController
) {
    val database = FirebaseDatabase.getInstance().reference  // Get database reference

    // Local state for form fields
    val calendar = Calendar.getInstance()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var entryDate by rememberSaveable { mutableStateOf(calendar.timeInMillis) }

    // Initialise states with a default value if null
    var weight by rememberSaveable { mutableStateOf(HealthRecord.weight?: 0f) }
    var temperature by rememberSaveable { mutableStateOf(HealthRecord.temperature?: 0f) }
    var rbc by rememberSaveable { mutableStateOf(HealthRecord.rbc?: 0f) }
    var wbc by rememberSaveable { mutableStateOf(HealthRecord.wbc?: 0f) }
    var plt by rememberSaveable { mutableStateOf(HealthRecord.plt?: 0f) }
    var alb by rememberSaveable { mutableStateOf(HealthRecord.alb?: 0f) }
    var ast by rememberSaveable { mutableStateOf(HealthRecord.ast?: 0f) }
    var alt by rememberSaveable { mutableStateOf(HealthRecord.alt?: 0f) }
    var bun by rememberSaveable { mutableStateOf(HealthRecord.bun?: 0f) }
    var scr by rememberSaveable { mutableStateOf(HealthRecord.scr?: 0f) }

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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(50.dp))
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
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
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
                            entryDate = datePickerState.selectedDateMillis!!
                        }) { Text(text = "OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text(text = "Cancel") }
                    }
                ) { DatePicker(state = datePickerState) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Turn on the switch to record health metrics, or leave it off to skip.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            AdjustableAttribute(
                label = "Weight: ${"%.1f".format(weight)} kg",
                initialValue = weight,
                range = 0f..20f,
                onValueChange = { weight = it }
            )
            AdjustableAttribute(
                label = "Body Temperature: ${"%.1f".format(temperature)} °C",
                initialValue = temperature,
                range = 35f..45f,
                onValueChange = { temperature = it }
            )
            AdjustableAttribute(
                label = "RBC: ${"%.1f".format(rbc)}",
                initialValue = rbc,
                range = 0f..10f,
                onValueChange = { rbc = it }
            )
            AdjustableAttribute(
                label = "WBC: ${"%.1f".format(wbc)}",
                initialValue = wbc,
                range = 0f..10f,
                onValueChange = { wbc = it }
            )
            AdjustableAttribute(
                label = "PLT: ${plt.toInt()}",
                initialValue = plt,
                range = 0f..300f,
                onValueChange = { plt = it }
            )
            AdjustableAttribute(
                label = "Alb (Albumin): ${"%.1f".format(alb)}",
                initialValue = alb,
                range = 0f..10f,
                onValueChange = { alb = it }
            )
            AdjustableAttribute(
                label = "AST: ${ast.toInt()}",
                initialValue = ast,
                range = 0f..300f,
                onValueChange = { ast = it }
            )
            AdjustableAttribute(
                label = "ALT: ${alt.toInt()}",
                initialValue = alt,
                range = 0f..300f,
                onValueChange = { alt = it }
            )
            AdjustableAttribute(
                label = "BUN: ${bun.toInt()}",
                initialValue = bun,
                range = 0f..100f,
                onValueChange = { bun = it }
            )
            AdjustableAttribute(
                label = "Scr (Serum creatinine): ${"%.1f".format(scr)}",
                initialValue = scr,
                range = 0f..10f,
                onValueChange = { scr = it }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button
                    val updatedRecord = HealthRecord(userId, Date(entryDate), weight, temperature, rbc, wbc, plt, alb, ast, alt, bun, scr)

                    // Reference to the user-specific directory in the database
                    val userRecordsRef = FirebaseDatabase.getInstance().getReference("healthRecords").child(userId)

                    // Generate a unique key for the new record under the specific user's directory
                    val recordKey = userRecordsRef.push().key
                    recordKey?.let {
                        // Save the record under the user's directory with the generated key
                        userRecordsRef.child(it).setValue(updatedRecord)
                            .addOnSuccessListener {
                                onSaveMetrics(updatedRecord)  // Notify successful save
                                navController.popBackStack()  // Navigate back
                            }
                            .addOnFailureListener { exception ->
                                // Handle failure (e.g., show an error message)
                                Log.e("SaveHealthRecord", "Failed to save health record: ${exception.message}")
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Records")
            }
        }
    }
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
