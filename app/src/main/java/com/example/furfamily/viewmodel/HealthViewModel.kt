package com.example.furfamily.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.furfamily.health.HealthRecord
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {
    private val _healthRecords = MutableLiveData<List<HealthRecord>>()
    val healthRecords: LiveData<List<HealthRecord>> = _healthRecords

    // Fetch Health Records
    fun fetchHealthRecords(userId: String, petId: String?) {
        Log.d("HealthViewModel", "Fetching health records for userId: $userId, petId: $petId")
        val database = FirebaseDatabase.getInstance().getReference("healthRecords").child(userId)
        val recordsList = mutableListOf<HealthRecord>()

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val record = snapshot.getValue(HealthRecord::class.java)
                Log.d("HealthViewModel", "Child added - Record: $record, Key: ${snapshot.key}")
                record?.let { 
                    if (petId == null || it.petId == petId) {
                        // Use the snapshot key as the record ID
                        val recordWithId = it.copy(recordId = snapshot.key ?: "")
                        Log.d("HealthViewModel", "Adding record to list - RecordId: ${recordWithId.recordId}, PetId: ${recordWithId.petId}")
                        recordsList.add(recordWithId)
                        _healthRecords.postValue(recordsList.toList())
                        Log.d("HealthViewModel", "Current records list size: ${recordsList.size}")
                    } else {
                        Log.d("HealthViewModel", "Skipping record - PetId mismatch: ${it.petId} != $petId")
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedRecord = snapshot.getValue(HealthRecord::class.java)
                Log.d("HealthViewModel", "Child changed - Record: $updatedRecord, Key: ${snapshot.key}")
                updatedRecord?.let {
                    if (petId == null || it.petId == petId) {
                        val recordWithId = it.copy(recordId = snapshot.key ?: "")
                        val index = recordsList.indexOfFirst { existing -> existing.recordId == recordWithId.recordId }
                        if (index != -1) {
                            Log.d("HealthViewModel", "Updating record at index: $index")
                            recordsList[index] = recordWithId
                            _healthRecords.postValue(recordsList.toList())
                        } else {
                            Log.d("HealthViewModel", "Record not found in list for update")
                        }
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedRecord = snapshot.getValue(HealthRecord::class.java)
                Log.d("HealthViewModel", "Child removed - Record: $removedRecord, Key: ${snapshot.key}")
                removedRecord?.let {
                    if (petId == null || it.petId == petId) {
                        val removed = recordsList.removeIf { existing -> existing.recordId == snapshot.key }
                        Log.d("HealthViewModel", "Record removed from list: $removed")
                        _healthRecords.postValue(recordsList.toList())
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("HealthViewModel", "Failed to fetch health records: ${error.message}")
            }
        }

        database.addChildEventListener(childEventListener)
        Log.d("HealthViewModel", "ChildEventListener added to database reference")
    }

    // Add new health record
    fun addHealthRecord(userId: String, record: HealthRecord) {
        val recordId = FirebaseDatabase.getInstance().getReference("healthRecords").child(userId).push().key!!
        val recordWithId = record.copy(recordId = recordId)
        val database = FirebaseDatabase.getInstance().getReference("healthRecords")
            .child(userId)
            .child(recordId)

        database.setValue(recordWithId)
            .addOnSuccessListener {
                Log.d("HealthViewModel", "Health record added successfully with ID: $recordId")
            }
            .addOnFailureListener { e ->
                Log.e("HealthViewModel", "Error adding health record", e)
            }
    }

    // Update health record
    fun updateHealthRecord(userId: String, record: HealthRecord) {
        val database = FirebaseDatabase.getInstance().getReference("healthRecords")
            .child(userId)
            .child(record.recordId)
        
        database.setValue(record)
            .addOnSuccessListener {
                Log.d("HealthViewModel", "Health record updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HealthViewModel", "Error updating health record", e)
            }
    }

    // Delete health record
    fun deleteHealthRecord(userId: String, recordId: String) {
        val database = FirebaseDatabase.getInstance().getReference("healthRecords")
            .child(userId)
            .child(recordId)
        
        database.removeValue()
            .addOnSuccessListener {
                Log.d("HealthViewModel", "Health record deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HealthViewModel", "Error deleting health record", e)
            }
    }
}