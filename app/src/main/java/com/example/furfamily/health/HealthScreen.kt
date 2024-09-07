package com.example.furfamily.health

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(userId: String, navController: NavController) {
    val healthRecords = fetchHealthRecords(userId)
    val lineEntries = mutableListOf<Entry>()
    val dateLabels = mutableListOf<String>()

    healthRecords.forEachIndexed { index, record ->
        record.weight?.takeIf { it != 0f }?.let {
            lineEntries.add(Entry(index.toFloat(), it))
            dateLabels.add(SimpleDateFormat("dd/MM", Locale.US).format(record.entryDate))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Dashboard") },
                actions = {
                    IconButton(onClick = { navController.navigate("CreateHealthRecord") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Record")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Line chart
            if (lineEntries.isNotEmpty()) {
                val lineDataSet = LineDataSet(lineEntries, "Weight")
                lineDataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
                val lineData = LineData(lineDataSet)
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),  // Adjusting height to make graph smaller
                    factory = { context ->
                        LineChart(context).apply {
                            data = lineData
                            description.isEnabled = false
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.valueFormatter = IndexAxisValueFormatter(dateLabels)
                            animateY(4000)
                        }
                    }
                )
            }

            // Previous records
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text = "Previous Health Record", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                healthRecords.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${SimpleDateFormat("dd/MM/yyyy", Locale.US).format(record.entryDate)}")
                            record.weight?.takeIf { it != 0f }?.let { Text("Weight: ${"%.1f".format(it)} kg") }
                            record.temperature?.takeIf { it != 0f }?.let { Text("Body temperature: ${"%.1f".format(it)} °C") }
                            record.rbc?.takeIf { it != 0f }?.let { Text("RBC: ${"%.1f".format(it)}") }
                            record.wbc?.takeIf { it != 0f }?.let { Text("WBC: ${"%.1f".format(it)}") }
                            record.plt?.takeIf { it != 0f }?.let { Text("PLT: ${it.toInt()}") }
                            record.alb?.takeIf { it != 0f }?.let { Text("Alb (Albumin): ${"%.1f".format(it)}") }
                            record.ast?.takeIf { it != 0f }?.let { Text("AST: ${it.toInt()}") }
                            record.alt?.takeIf { it != 0f }?.let { Text("ALT: ${it.toInt()}") }
                            record.bun?.takeIf { it != 0f }?.let { Text("BUN: ${it.toInt()}") }
                            record.scr?.takeIf { it != 0f }?.let { Text("Scr (Serum creatinine): ${"%.1f".format(it)}") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun fetchHealthRecords(userId: String): SnapshotStateList<HealthRecord> {
    val records = remember { mutableStateListOf<HealthRecord>() }
    val database = FirebaseDatabase.getInstance().getReference("healthRecords").child(userId)

    DisposableEffect(userId) {
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val record = snapshot.getValue(HealthRecord::class.java)
                record?.let { records.add(it) }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedRecord = snapshot.getValue(HealthRecord::class.java)
                updatedRecord?.let {
//                    val index = records.indexOfFirst { record -> record.recordId == it.recordId }
//                    if (index != -1) {
//                        records[index] = it
//                    }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val record = snapshot.getValue(HealthRecord::class.java)
                record?.let { records.remove(it) }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                //
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", "Failed to fetch health records: ${error.message}")
            }
        }
        database.addChildEventListener(childEventListener)
        onDispose {
            database.removeEventListener(childEventListener)
        }
    }
    return records
}
