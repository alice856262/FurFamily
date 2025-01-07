package com.example.furfamily.profile

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.furfamily.ViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSetting(viewModel: ViewModel, userId: String) {
    val userProfile by viewModel.userProfile.observeAsState()

    if (userProfile != null) {
        var firstName by remember { mutableStateOf(userProfile!!.firstName) }
        var lastName by remember { mutableStateOf(userProfile!!.lastName) }
        var phone by remember { mutableStateOf(userProfile!!.phone) }
        val gender = listOf("Male", "Female", "Other")
        var isExpanded by remember { mutableStateOf(false) }
        var selectedGender by remember { mutableStateOf(userProfile!!.selectedGender) }
        val birthDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(userProfile!!.birthDate)
        val email = userProfile!!.email

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Email (read-only)
            Text(text = "Email: ", style = MaterialTheme.typography.titleMedium)
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // First Name
            Text(text = "First Name: ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Last Name
            Text(text = "Last Name: ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Phone Number
            Text(text = "Phone Number: ", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gender Selection
            Text(text = "Gender: ", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
                TextField(
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .focusProperties {
                            canFocus = false
                        }
                        .padding(bottom = 8.dp),
                    readOnly = true,
                    value = selectedGender,
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    gender.forEach { selectionOption ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedGender = selectionOption
                                isExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Birthdate (read-only)
            Text(text = "Birth Date: ", style = MaterialTheme.typography.titleMedium)
            Text(
                text = birthDate,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Changes Button
            Button(
                onClick = {
                    val updatedProfile = userProfile!!.copy(
                        firstName = firstName,
                        lastName = lastName,
                        phone = phone,
                        selectedGender = selectedGender
                    )
                    viewModel.updateUser(updatedProfile) {
                        Log.d("ProfileUpdate", "Profile updated successfully.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    } else {
        Text(
            text = "Loading user profile...",
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}