package com.example.furfamily.profile

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.furfamily.R
import com.example.furfamily.Routes
import com.example.furfamily.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(loginWithEmailPassword: (String, String, (String) -> Unit) -> Unit,
                navController: NavController, viewModel: ViewModel
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }

    // Google Sign-In intent handling
    val signInIntent by viewModel.googleSignInIntent.observeAsState()
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            Log.d("handleSignInResult", "handleSignInResult")
            handleSignInResult(task, viewModel, navController)
        }
    }

    // Trigger Google Sign-In
    LaunchedEffect(signInIntent) {
        signInIntent?.let {
            googleSignInLauncher.launch(it)
        }
    }

    if (showResetPasswordDialog) {
        PasswordResetDialog(
            initialEmail = email,  // Pass the current email state
            onDismiss = { showResetPasswordDialog = false },
            viewModel = viewModel
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login to Your Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (isPasswordVisible) painterResource(id = R.drawable.eye) else painterResource(id = R.drawable.hidden)
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        painter = icon,
                        contentDescription = "Show or hide password",
                        modifier = Modifier.height(22.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                loginWithEmailPassword(email, password, navController) { error -> loginError = error }
            },
            modifier = Modifier
                .height(46.dp)
                .width(190.dp)
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (loginError.isNotEmpty()) {
            Snackbar {
                Text(loginError)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Google Sign-In Button
        Button(
            onClick = { viewModel.signInWithGoogle() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.android_light_rd),
                contentDescription = "Sign in with Google",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { showResetPasswordDialog = true }) {
            Text("Forgot Password?")
        }
        Row(modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account? ")
            TextButton(onClick = { navController.navigate(Routes.Registration.value) },
            ) {
                Text("Register here!")
            }
        }
    }
}

fun loginWithEmailPassword(email: String, password: String,
                           navController: NavController,
                           onLoginError: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    Log.d("Login auth", "auth: $auth")
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("Login Success", "User email: $email")
            Log.d("NavController", "Current back stack entry: ${navController.currentBackStackEntry}")
            if (navController.currentBackStackEntry != null) {
                navController.navigate(Routes.CalendarScreen.value) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        } else {
            Log.e("Login Error", task.exception?.message ?: "Unknown Error")
            val errorMessage = "Login failed: please make sure you provide the correct email and password!"
            onLoginError(errorMessage)  // Pass the error message back to the Composable
        }
    }
}

@Composable
fun PasswordResetDialog(initialEmail: String, onDismiss: () -> Unit, viewModel: ViewModel) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Password Reset") },
        text = {
            Column {
                Text("Enter your email to reset your password:")
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    placeholder = { Text("Email address") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.sendPasswordResetEmail(email)
                    onDismiss()
                }
            ) { Text("Send Email") }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) { Text("Cancel") }
        }
    )
}

fun handleSignInResult(task: Task<GoogleSignInAccount>, viewModel: ViewModel, navController: NavController) {
    task.addOnSuccessListener { account ->
        // Success, proceed with account
        Log.d("SignIn", "Google sign-in succeeded: ${account.email}")

        account.idToken?.let { idToken ->
            // First authenticate with Firebase
            viewModel.firebaseAuthWithGoogle(idToken, account, {
                // Authentication successful, now fetch or create the user profile
                viewModel.fetchOrCreateUserProfile(account) { userProfile ->
                    // Navigation on success
                    navController.navigate(Routes.CalendarScreen.value) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }, { exception ->
                // Handle authentication failure
                Log.e("FirebaseAuth", "Authentication failed", exception)
            })
        } ?: Log.e("SignIn", "No ID token available")

//        viewModel.fetchOrCreateUserProfile(account) { userProfile ->
//            account.idToken?.let { token ->
//                viewModel.firebaseAuthWithGoogle(token, userProfile) {
//                    // Navigation on success
//                    navController.navigate(Routes.CalendarScreen.value) {
//                        popUpTo(navController.graph.startDestinationId) {
//                            inclusive = true
//                        }
//                    }
//                }
//            } ?: Log.e("SignIn", "No ID token available")
//        }
    }.addOnFailureListener { exception ->
        // Log detailed error
        if (exception is ApiException) {
            Log.e("SignIn", "Google sign-in failed: ${exception.statusCode}", exception)
        } else {
            Log.e("SignIn", "Unknown error during Google sign-in", exception)
        }
    }
}
