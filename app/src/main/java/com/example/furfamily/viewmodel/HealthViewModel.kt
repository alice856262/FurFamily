package com.example.furfamily.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.furfamily.health.HealthRecord
import com.example.furfamily.profile.Pet
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
        val database = FirebaseDatabase.getInstance().getReference("healthRecords").child(userId)
        val recordsList = mutableListOf<HealthRecord>()

        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val record = snapshot.getValue(HealthRecord::class.java)
                record?.takeIf { it.petId == petId }?.let { recordsList.add(it) }
                _healthRecords.postValue(recordsList) // Update LiveData
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedRecord = snapshot.getValue(HealthRecord::class.java)
                updatedRecord?.takeIf { it.petId == petId }?.let {
                    val index = recordsList.indexOfFirst { it.entryDate == it.entryDate }
                    if (index != -1) {
                        recordsList[index] = updatedRecord
                        _healthRecords.postValue(recordsList)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedRecord = snapshot.getValue(HealthRecord::class.java)
                removedRecord?.let {
                    recordsList.remove(it)
                    _healthRecords.postValue(recordsList)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", "Failed to fetch health records: ${error.message}")
            }
        }

        database.addChildEventListener(childEventListener)
    }
}