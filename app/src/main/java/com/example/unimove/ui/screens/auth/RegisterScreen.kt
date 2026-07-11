package com.example.unimove.ui.screens.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.R
import com.example.unimove.ui.components.AuthScreenBackground
import com.example.unimove.ui.components.OrDivider
import com.example.unimove.ui.components.SocialButton
import com.example.unimove.ui.components.authTextFieldColors
import com.example.unimove.ui.theme.AuthInputBackground
import com.example.unimove.ui.theme.AuthPrimary
import com.example.unimove.ui.theme.AuthSecondary
import com.example.unimove.ui.theme.AuthTextSecondary
import com.example.unimove.ui.theme.GlassWhite
import com.example.unimove.ui.theme.GlassWhiteBorder
import com.example.unimove.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var verificationSentEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var verificationCheckInFlight by remember { mutableStateOf(false) }
    var silentVerificationCheckInFlight by remember { mutableStateOf(false) }

    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val enterTransition = fadeIn(animationSpec = tween(450)) + slideInVertically(initialOffsetY = { 28 })

    fun navigateAfterAuth(requiresOnboarding: Boolean) {
        val destination = if (requiresOnboarding) "onboarding" else "home"
        navController.navigate(destination) { popUpTo(0) { inclusive = true } }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun openEmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showMessage("Open your email app and look for the UniMove verification email.")
        }
    }

    fun checkVerification(showStillPendingMessage: Boolean, showCheckingUi: Boolean) {
        if (showCheckingUi) {
            if (verificationCheckInFlight) return
            verificationCheckInFlight = true
        } else {
            if (silentVerificationCheckInFlight || verificationCheckInFlight) return
            silentVerificationCheckInFlight = true
        }
        viewModel.checkEmailVerification(
            showLoading = showCheckingUi,
            onVerified = { outcome ->
                verificationCheckInFlight = false
                silentVerificationCheckInFlight = false
                showMessage("Email verified. Continuing setup.")
                navigateAfterAuth(outcome.requiresOnboarding)
            },
            onNotVerified = {
                verificationCheckInFlight = false
                silentVerificationCheckInFlight = false
                if (showStillPendingMessage) {
                    showMessage("Still not verified. Please tap the link in your email, then return here.")
                }
            },
            onError = { message ->
                verificationCheckInFlight = false
                silentVerificationCheckInFlight = false
                showMessage(message)
            }
        )
    }

    LaunchedEffect(uiState.user?.uid, uiState.user?.email, uiState.user?.isEmailVerified) {
        val user = uiState.user
        if (user != null && !user.isEmailVerified && user.providerData.any { it.providerId == "password" }) {
            verificationSentEmail = user.email.orEmpty()
        }
    }

    LaunchedEffect(verificationSentEmail) {
        if (verificationSentEmail != null) {
            while (true) {
                delay(4_000)
                checkVerification(showStillPendingMessage = false, showCheckingUi = false)
            }
        }
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
                if (verificationSentEmail == null) {
                    RegisterFormContent(
                        email = email,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = uiState.isLoading,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onEmailChange = { email = it },
                        onPasswordChange = { password = it },
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        onBack = { navController.navigateUp() },
                        onRegister = {
                            val cleanEmail = email.trim()
                            viewModel.clearError()
                            viewModel.registerWithEmail(
                                email = cleanEmail,
                                password = password,
                                onVerificationSent = {
                                    verificationSentEmail = cleanEmail
                                    showMessage("Verification email sent. Check your inbox to continue.")
                                },
                                onError = ::showMessage
                            )
                        },
                        onGoogleSignUp = {
                            viewModel.signInWithGoogle(
                                context,
                                webClientId,
                                onSuccess = { outcome -> navigateAfterAuth(outcome.requiresOnboarding) },
                                onError = ::showMessage
                            )
                        },
                        onLogin = { navController.navigate("login") }
                    )
                } else {
                    VerificationSentContent(
                        email = verificationSentEmail.orEmpty(),
                        isChecking = uiState.isLoading || verificationCheckInFlight,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onOpenEmail = ::openEmailApp,
                        onCheckVerification = {
                            checkVerification(showStillPendingMessage = true, showCheckingUi = true)
                        },
                        onResendEmail = {
                            viewModel.resendVerificationEmail(
                                onSuccess = { showMessage("Verification email sent again.") },
                                onError = ::showMessage
                            )
                        },
                        onUseAnotherEmail = {
                            viewModel.signOut()
                            verificationSentEmail = null
                            password = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterFormContent(
    email: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onGoogleSignUp: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        horizontalAlignment = Alignment.Start
    ) {
        RegisterBackButton(onBack = onBack)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "UniMove account",
                color = AuthPrimary.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Create your account",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.2).sp
                )
            )
            Text(
                text = "Use an email you can open now. We will send a verification link before you can sign in.",
                color = AuthTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 25.sp,
                modifier = Modifier.alpha(0.9f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AuthSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "After signup, keep this app open or return after verifying your email.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RegistrationTextField(
                label = "Email address",
                value = email,
                onValueChange = onEmailChange,
                placeholder = "name@example.com",
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = AuthTextSecondary.copy(alpha = 0.8f))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            RegistrationTextField(
                label = "Password",
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "At least 6 characters",
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AuthTextSecondary.copy(alpha = 0.8f))
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePassword) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = if (passwordVisible) AuthPrimary else AuthTextSecondary.copy(alpha = 0.65f)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )
        }

        Button(
            onClick = onRegister,
            enabled = !isLoading && email.isNotBlank() && password.length >= 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuthPrimary,
                disabledContainerColor = AuthPrimary.copy(alpha = 0.22f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Create account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }

        OrDivider()

        SocialButton(
            text = "Sign up with Google",
            iconResId = R.drawable.ic_google,
            enabled = !isLoading,
            onClick = onGoogleSignUp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already registered?", color = AuthTextSecondary, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onLogin, enabled = !isLoading) {
                Text("Sign in", color = AuthPrimary, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun VerificationSentContent(
    email: String,
    isChecking: Boolean,
    modifier: Modifier = Modifier,
    onOpenEmail: () -> Unit,
    onCheckVerification: () -> Unit,
    onResendEmail: () -> Unit,
    onUseAnotherEmail: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(AuthPrimary.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Check your email",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.1).sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "We sent a verification link to:",
                color = AuthTextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = email,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AuthInputBackground,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VerificationStep(
                    number = "1",
                    title = "Open your inbox",
                    body = "Look for the UniMove verification email. Check Spam or Promotions if it is not visible."
                )
                VerificationStep(
                    number = "2",
                    title = "Tap the verification link",
                    body = "The link confirms this email belongs to you."
                )
                VerificationStep(
                    number = "3",
                    title = "Return to UniMove",
                    body = "Keep this screen open. UniMove checks Firebase and continues automatically after verification."
                )
            }
        }

        Button(
            onClick = onOpenEmail,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AuthPrimary)
        ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Open email app", fontWeight = FontWeight.Black)
        }

        Button(
            onClick = onCheckVerification,
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF111827),
                disabledContainerColor = Color.White.copy(alpha = 0.20f),
                disabledContentColor = Color.White.copy(alpha = 0.65f)
            )
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Checking verification", fontWeight = FontWeight.Bold)
            } else {
                Text("I verified, continue", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onResendEmail,
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("Resend verification email", fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onUseAnotherEmail, enabled = !isChecking) {
            Text("Use another email", color = AuthTextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegistrationTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = authTextFieldColors(),
            singleLine = true,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.28f)
                )
            }
        )
    }
}

@Composable
private fun RegisterBackButton(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite)
            .border(1.dp, GlassWhiteBorder, RoundedCornerShape(16.dp))
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun VerificationStep(
    number: String,
    title: String,
    body: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AuthPrimary.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = body,
                color = AuthTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
