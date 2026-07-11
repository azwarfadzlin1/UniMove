package com.example.unimove.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.R
import com.example.unimove.ui.components.*
import com.example.unimove.ui.theme.*
import com.example.unimove.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    // Animations
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val enterTransition = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { 40 })

    fun navigateAfterAuth(requiresOnboarding: Boolean) {
        val destination = if (requiresOnboarding) "onboarding" else "home"
        navController.navigate(destination) { popUpTo(0) { inclusive = true } }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = Color(0xFF0F0F0F),
            shape = RoundedCornerShape(28.dp),
            title = { Text("Reset Password", fontWeight = FontWeight.Black, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Enter your email to receive a password reset link.", color = AuthTextSecondary)
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = authTextFieldColors(),
                        singleLine = true,
                        placeholder = { Text("email@example.com", color = Color.White.copy(alpha = 0.2f)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showForgotDialog = false
                        viewModel.resetPassword(
                            email = forgotEmail.trim(),
                            onSuccess = {
                                forgotEmail = ""
                                Toast.makeText(context, "Recovery link sent!", Toast.LENGTH_LONG).show()
                            },
                            onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuthPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SEND LINK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("CANCEL", color = AuthTextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AuthScreenBackground {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = enterTransition,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Back Button
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        IconButton(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassWhite)
                                .border(1.dp, GlassWhiteBorder, RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Header
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Welcome Back",
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp
                            )
                        )
                        Text(
                            text = "Log in to your account and continue your athletic journey.",
                            color = AuthTextSecondary,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 26.sp,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }

                    // Form
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("EMAIL ADDRESS", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = authTextFieldColors(),
                                singleLine = true,
                                placeholder = { Text("your@email.com", color = Color.White.copy(alpha = 0.15f)) }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("PASSWORD", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = if (passwordVisible) AuthPrimary else AuthTextSecondary.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                colors = authTextFieldColors(),
                                singleLine = true,
                                placeholder = { Text("••••••••", color = Color.White.copy(alpha = 0.15f)) }
                            )
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                TextButton(onClick = { showForgotDialog = true }) {
                                    Text("FORGOT PASSWORD?", color = AuthPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Action
                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.loginWithEmail(
                                email = email.trim(),
                                password = password,
                                onSuccess = { outcome -> navigateAfterAuth(outcome.requiresOnboarding) },
                                onNotVerified = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Email not verified yet. We sent another verification link. Check your inbox.")
                                    }
                                },
                                onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        },
                        enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuthPrimary,
                            disabledContainerColor = AuthPrimary.copy(alpha = 0.2f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("SIGN IN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }

                    OrDivider()

                    SocialButton(
                        text = "Continue with Google",
                        iconResId = R.drawable.ic_google,
                        onClick = {
                            viewModel.signInWithGoogle(
                                context,
                                webClientId,
                                onSuccess = { outcome -> navigateAfterAuth(outcome.requiresOnboarding) },
                                onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ready to join?", color = AuthTextSecondary, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { navController.navigate("register") }) {
                            Text("CREATE ACCOUNT", color = AuthPrimary, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
