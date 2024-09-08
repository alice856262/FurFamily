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
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("Health Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("CreateHealthRecord") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add New Record")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Line chart
            if (lineEntries.isNotEmpty()) {
                val lineDataSet = LineDataSet(lineEntries, "Weight")
                lineDataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
                val lineData = LineData(lineDataSet)

                Text(
                    text = "Weight Progress",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

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
                            animateY(1500)
                            setTouchEnabled(true)
                            setPinchZoom(true)
                            axisRight.isEnabled = false
                            axisLeft.setDrawGridLines(false)
                            xAxis.setDrawGridLines(false)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Previous records section
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Previous Health Records",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                healthRecords.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(record.entryDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            record.weight?.takeIf { it != 0f }?.let {
                                Text(text = "Weight: ${"%.1f".format(it)} kg", style = MaterialTheme.typography.bodySmall)
                            }
                            record.temperature?.takeIf { it != 0f }?.let {
                                Text(text = "Body temperature: ${"%.1f".format(it)} °C", style = MaterialTheme.typography.bodySmall)
                            }
                            record.rbc?.takeIf { it != 0f }?.let {
                                Text(text = "RBC: ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.wbc?.takeIf { it != 0f }?.let {
                                Text(text = "WBC: ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.plt?.takeIf { it != 0f }?.let {
                                Text(text = "PLT: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.alb?.takeIf { it != 0f }?.let {
                                Text(text = "Alb (Albumin): ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.ast?.takeIf { it != 0f }?.let {
                                Text(text = "AST: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.alt?.takeIf { it != 0f }?.let {
                                Text(text = "ALT: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.bun?.takeIf { it != 0f }?.let {
                                Text(text = "BUN: ${it.toInt()}", style = MaterialTheme.typography.bodySmall)
                            }
                            record.scr?.takeIf { it != 0f }?.let {
                                Text(text = "Scr (Serum creatinine): ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                            }
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
                    // Update logic if necessary
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
