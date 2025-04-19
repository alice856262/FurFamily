package com.example.furfamily.profile

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.furfamily.R
import com.example.furfamily.Routes
import com.example.furfamily.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginWithEmailPassword: (String, String, (String) -> Unit) -> Unit,
    navController: NavController
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf("") }
    val showSnackbar = remember { mutableStateOf(false) }

    // Google Sign-In intent handling
    val signInIntent by authViewModel.googleSignInIntent.observeAsState()
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            Log.d("handleSignInResult", "handleSignInResult")
            handleSignInResult(task, authViewModel, navController)
        }
    }

    // Trigger Google Sign-In
    LaunchedEffect(signInIntent) {
        signInIntent?.let {
            googleSignInLauncher.launch(it)
        }
    }

    // Show snackbar when error appears
    LaunchedEffect(loginError) {
        if (loginError.isNotEmpty()) {
            showSnackbar.value = true
        }
    }

    if (showResetPasswordDialog) {
        PasswordResetDialog(
            initialEmail = email,
            onDismiss = { showResetPasswordDialog = false },
            authViewModel = authViewModel
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.7f))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Login Here",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Image(
                    painter = painterResource(id = R.drawable.paws),
                    contentDescription = "Paws Icon",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Email Input Field
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = TextFieldDefaults.textFieldColors(
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Input Field
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (isPasswordVisible) painterResource(id = R.drawable.eye) else painterResource(id = R.drawable.hidden)
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            painter = icon,
                            contentDescription = "Show or hide password",
                            modifier = Modifier.height(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Error Message
            if (showSnackbar.value && loginError.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .background(MaterialTheme.colorScheme.error),
                    action = {
                        TextButton(onClick = {
                            showSnackbar.value = false
                            loginError = ""
                        }) {
                            Text("DISMISS", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                ) {
                    Text(loginError, color = MaterialTheme.colorScheme.onError)
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Login Button
            Button(
                onClick = {
                    when {
                        email.isBlank() && password.isBlank() -> {
                            loginError = "Please enter both email and password"
                        }
                        email.isBlank() -> {
                            loginError = "Please enter your email"
                        }
                        password.isBlank() -> {
                            loginError = "Please enter your password"
                        }
                        else -> {
                            loginWithEmailPassword(email, password) { error -> loginError = error }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .height(46.dp)
                    .width(190.dp)
            ) {
                Text("Login", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-In Button
            Button(
                onClick = { authViewModel.signInWithGoogle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Transparent
                ),
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

            // Forgot Password
            TextButton(onClick = { showResetPasswordDialog = true }) {
                Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = { navController.navigate(Routes.Registration.value) }
                ) {
                    Text("Register here!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

fun loginWithEmailPassword(email: String, password: String, navController: NavController, onLoginError: (String) -> Unit) {
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
            val errorMessage = "Login failed:\nEmail or password is incorrect"
            onLoginError(errorMessage)  // Pass the error message back to the Composable
        }
    }
}

@Composable
fun PasswordResetDialog(initialEmail: String, onDismiss: () -> Unit, authViewModel: AuthViewModel) {
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
                    authViewModel.sendPasswordResetEmail(email)
                    onDismiss()
                }
            ) { Text("Send Email") }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) { Text("Cancel") }
        }
    )
}

fun handleSignInResult(task: Task<GoogleSignInAccount>, authViewModel: AuthViewModel, navController: NavController) {
    task.addOnSuccessListener { account ->
        // Success, proceed with account
        Log.d("SignIn", "Google sign-in succeeded: ${account.email}")

        account.idToken?.let { idToken ->
            // First authenticate with Firebase
            authViewModel.firebaseAuthWithGoogle(idToken, account, {
                // Authentication successful, now fetch or create the user profile
                authViewModel.fetchOrCreateUserProfile(account) { userProfile ->
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
    }.addOnFailureListener { exception ->
        // Log detailed error
        if (exception is ApiException) {
            Log.e("SignIn", "Google sign-in failed: ${exception.statusCode}", exception)
        } else {
            Log.e("SignIn", "Unknown error during Google sign-in", exception)
        }
    }
}
