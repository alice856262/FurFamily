package com.example.furfamily.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.furfamily.calendar.CalendarEvent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val application: Application,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : ViewModel() {
    private val _calendarEvents = MutableLiveData<List<Event>>()
    val calendarEvents: LiveData<List<Event>> = _calendarEvents

    private val _calendarEventDates = MutableLiveData<List<Event>>()
    val calendarEventDates: LiveData<List<Event>> = _calendarEventDates

    private val _eventsDates = MediatorLiveData<List<LocalDate>>()
    val eventsDates: LiveData<List<LocalDate>> = _eventsDates

    private val _selectedDateEvents = MutableLiveData<List<Event>>()
    val selectedDateEvents: LiveData<List<Event>> = _selectedDateEvents

    private val _intentForUserResolution = MutableLiveData<Intent>()
    val intentForUserResolution: LiveData<Intent> = _intentForUserResolution

    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    fun loadCalendarEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(application)
                if (account != null) {
                    loadGoogleCalendarEvents(account)
                } else {
                    loadFirebaseEvents()
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Failed to load calendar events", e)
            }
        }
    }

    private fun loadGoogleCalendarEvents(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                application, listOf(CalendarScopes.CALENDAR)
            )
            credential.selectedAccount = account.account
            val service = Calendar.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("YourAppName")
                .build()

            val now = com.google.api.client.util.DateTime(System.currentTimeMillis())
            val events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            _calendarEvents.postValue(events.items)
            updateEventDates(events.items)
        } catch (e: UserRecoverableAuthIOException) {
            _intentForUserResolution.postValue(e.intent)
        }
    }

    private fun loadFirebaseEvents() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val eventsRef = firebaseDatabase.reference.child("events").child(uid)

        eventsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val events = mutableListOf<Event>()
                snapshot.children.forEach { eventSnapshot ->
                    val event = convertSnapshotToEvent(eventSnapshot)
                    event?.let { events.add(it) }
                }
                _calendarEvents.postValue(events)
                updateEventDates(events)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CalendarViewModel", "Failed to load events: ${error.message}")
            }
        })
    }

    fun loadEventsForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val account = GoogleSignIn.getLastSignedInAccount(application)
                if (account != null) {
                    loadGoogleCalendarEventsForDate(date)
                } else {
                    loadFirebaseEventsForDate(date)
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Failed to load events for date", e)
            }
        }
    }

    private fun loadGoogleCalendarEventsForDate(date: LocalDate) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(application)!!
            val credential = GoogleAccountCredential.usingOAuth2(
                application, listOf(CalendarScopes.CALENDAR)
            )
            credential.selectedAccount = account.account
            val service = Calendar.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("YourAppName")
                .build()

            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val events = service.events().list("primary")
                .setTimeMin(com.google.api.client.util.DateTime(startOfDay.toEpochMilli()))
                .setTimeMax(com.google.api.client.util.DateTime(endOfDay.toEpochMilli()))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()

            _selectedDateEvents.postValue(events.items)
        } catch (e: UserRecoverableAuthIOException) {
            _intentForUserResolution.postValue(e.intent)
        }
    }

    private fun loadFirebaseEventsForDate(date: LocalDate) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val eventsRef = firebaseDatabase.reference.child("events").child(uid)

        eventsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dateEvents = mutableListOf<Event>()
                snapshot.children.forEach { eventSnapshot ->
                    val event = convertSnapshotToEvent(eventSnapshot)
                    event?.let {
                        val eventDate = getEventDate(it)
                        if (eventDate == date) {
                            dateEvents.add(it)
                        }
                    }
                }
                _selectedDateEvents.postValue(dateEvents)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CalendarViewModel", "Failed to load events for date: ${error.message}")
            }
        })
    }

    private fun convertSnapshotToEvent(snapshot: DataSnapshot): Event? {
        return try {
            val eventMap = snapshot.value as? Map<*, *> ?: return null
            val title = eventMap["title"] as? String ?: return null
            val description = eventMap["description"] as? String
            val location = eventMap["location"] as? String

            val startTimeMap = eventMap["startTime"] as? Map<*, *> ?: return null
            val endTimeMap = eventMap["endTime"] as? Map<*, *> ?: return null

            val startTime = parseLocalDateTimeFromMap(startTimeMap)
            val endTime = parseLocalDateTimeFromMap(endTimeMap)

            Event().apply {
                summary = title
                this.description = description
                this.location = location
                start = EventDateTime().setDateTime(localDateTimeToDateTime(startTime))
                end = EventDateTime().setDateTime(localDateTimeToDateTime(endTime))
            }
        } catch (e: Exception) {
            Log.e("CalendarViewModel", "Error converting event snapshot", e)
            null
        }
    }

    private fun updateEventDates(events: List<Event>) {
        val dates = events.mapNotNull { event ->
            event.start?.dateTime?.let {
                Instant.ofEpochMilli(it.value).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: event.start?.date?.let {
                LocalDate.parse(it.toString())
            }
        }.distinct()
        _eventsDates.postValue(dates)
    }

    fun createEvent(calendarEvent: CalendarEvent, function: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch

            val account = GoogleSignIn.getLastSignedInAccount(application)
            if (account != null && account.idToken != null) {
                createGoogleCalendarEvent(account, calendarEvent)
            }

            // Always save to Firebase
            saveEventToFirebase(userId, calendarEvent)
        }
    }

    private fun createGoogleCalendarEvent(
        account: GoogleSignInAccount,
        calendarEvent: CalendarEvent
    ) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                application, listOf(CalendarScopes.CALENDAR)
            ).apply { selectedAccount = account.account }

            val service = Calendar.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("YourAppName")
                .build()

            val googleEvent = Event().apply {
                summary = calendarEvent.title
                description = calendarEvent.description
                start = EventDateTime().setDateTime(localDateTimeToDateTime(calendarEvent.startTime))
                end = EventDateTime().setDateTime(localDateTimeToDateTime(calendarEvent.endTime))
                location = calendarEvent.location
            }

            service.events().insert("primary", googleEvent).execute()
        } catch (e: Exception) {
            Log.e("CalendarViewModel", "Error creating Google Calendar event", e)
        }
    }

    private fun saveEventToFirebase(userId: String, event: CalendarEvent) {
        val eventsRef = firebaseDatabase.reference.child("events").child(userId)
        val eventId = eventsRef.push().key ?: return

        val eventMap = mapOf(
            "userId" to event.userId,
            "petId" to event.petId,
            "title" to event.title,
            "description" to event.description,
            "startTime" to event.startTime,
            "endTime" to event.endTime,
            "location" to event.location
        )

        eventsRef.child(eventId).setValue(eventMap)
            .addOnSuccessListener {
                Log.d("CalendarViewModel", "Event saved successfully")
            }
            .addOnFailureListener {
                Log.e("CalendarViewModel", "Failed to save event", it)
            }
    }

    private fun parseLocalDateTimeFromMap(dateTimeMap: Map<*, *>): LocalDateTime {
        val year = (dateTimeMap["year"] as Long).toInt()
        val month = Month.valueOf((dateTimeMap["month"] as String).uppercase())
        val day = (dateTimeMap["dayOfMonth"] as Long).toInt()
        val hour = (dateTimeMap["hour"] as Long).toInt()
        val minute = (dateTimeMap["minute"] as Long).toInt()
        val second = (dateTimeMap["second"] as Long).toInt()
        val nanoOfSecond = (dateTimeMap["nano"] as Long).toInt()

        return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond)
    }

    private fun localDateTimeToDateTime(ldt: LocalDateTime): com.google.api.client.util.DateTime {
        val instant = ldt.atZone(ZoneId.systemDefault()).toInstant()
        return com.google.api.client.util.DateTime(instant.toEpochMilli())
    }

    private fun getEventDate(event: Event): LocalDate? {
        return event.start?.dateTime?.let {
            Instant.ofEpochMilli(it.value).atZone(ZoneId.systemDefault()).toLocalDate()
        } ?: event.start?.date?.let {
            LocalDate.parse(it.toString())
        }
    }

    fun clearSelectedDateEvents() {
        _selectedDateEvents.value = emptyList()
    }
}