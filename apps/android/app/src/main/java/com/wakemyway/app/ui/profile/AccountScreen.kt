package com.wakemyway.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.wakemyway.app.product.account.WakeAccountManager
import com.wakemyway.app.product.account.WakeAccountRole
import com.wakemyway.app.ui.components.WmwCard
import com.wakemyway.app.ui.components.WmwCircadianStage
import com.wakemyway.app.ui.components.WmwCircadianSurface
import com.wakemyway.app.ui.components.WmwSectionLabel
import com.wakemyway.app.ui.theme.WmwColors
import com.wakemyway.app.ui.theme.WmwSpacing
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    manager: WakeAccountManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by manager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val hasSignInMethod = manager.googleSignInConfigured || manager.emailPasswordSignInConfigured
    var showEmailForm by remember(
        manager.googleSignInConfigured,
        manager.emailPasswordSignInConfigured,
    ) {
        mutableStateOf(
            manager.emailPasswordSignInConfigured && !manager.googleSignInConfigured,
        )
    }

    LaunchedEffect(manager) {
        manager.refresh()
    }

    WmwCircadianSurface(WmwCircadianStage.PLANNING, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WmwSpacing.Lg)
                .padding(top = WmwSpacing.Md, bottom = WmwSpacing.Xl),
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Back")
            }

            Text(
                text = "Your WakeMyWay account",
                modifier = Modifier.padding(top = WmwSpacing.Md),
                style = MaterialTheme.typography.headlineLarge,
                color = WmwColors.Midnight,
            )
            Text(
                text = when {
                    manager.googleSignInConfigured && manager.emailPasswordSignInConfigured ->
                        "Use Google for the quickest sign-in, or use email and password instead. Your alarms remain local and keep working without an account."
                    manager.googleSignInConfigured ->
                        "Sign in securely with Google. Your alarms remain local and keep working without an account."
                    else ->
                        "Your alarms remain local and keep working without an account."
                },
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.LightQuietText,
            )

            if (!state.configured || !hasSignInMethod) {
                WmwSectionLabel(
                    text = "Account setup",
                    modifier = Modifier.padding(top = WmwSpacing.Xl),
                )
                WmwCard(
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                    onLightSurface = true,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Sm)) {
                        Text(
                            text = if (!state.configured) {
                                "Account sign-in is not connected in this build yet."
                            } else {
                                "No production sign-in method is enabled in this build."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = WmwColors.Midnight,
                        )
                        Text(
                            text = "Nothing is blocked. WakeMyWay stays fully usable offline while account infrastructure is being configured.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WmwColors.LightQuietText,
                        )
                    }
                }
                return@WmwCircadianSurface
            }

            state.account?.let { account ->
                WmwSectionLabel(
                    text = "Signed in",
                    modifier = Modifier.padding(top = WmwSpacing.Xl),
                )
                WmwCard(
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                    onLightSurface = true,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = account.email ?: "WakeMyWay account",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WmwColors.Midnight,
                                )
                                Text(
                                    text = if (account.roleVerified) {
                                        "Account permissions verified"
                                    } else {
                                        "Signed in · permissions will refresh when the Wake service is reachable"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WmwColors.LightQuietText,
                                )
                            }
                            if (account.roleVerified && account.role == WakeAccountRole.ADMIN) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = WmwColors.Sunrise.copy(alpha = 0.18f),
                                ) {
                                    Text(
                                        text = "Admin",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WmwColors.Midnight,
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { scope.launch { manager.refresh() } },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Refresh account")
                        }
                        TextButton(
                            onClick = { scope.launch { manager.signOut() } },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Sign out")
                        }
                    }
                }
            } ?: run {
                WmwSectionLabel(
                    text = "Sign in",
                    modifier = Modifier.padding(top = WmwSpacing.Xl),
                )
                WmwCard(
                    modifier = Modifier.padding(top = WmwSpacing.Sm),
                    onLightSurface = true,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(WmwSpacing.Md)) {
                        if (manager.googleSignInConfigured) {
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleMedium,
                                color = WmwColors.Midnight,
                            )
                            Text(
                                text = "Secure sign-in through Google and Neon Auth. No separate WakeMyWay password is needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WmwColors.LightQuietText,
                            )
                            Button(
                                onClick = { scope.launch { manager.signInWithGoogle(context) } },
                                enabled = !state.loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Continue with Google")
                            }
                        }

                        if (manager.emailPasswordSignInConfigured && manager.googleSignInConfigured) {
                            TextButton(
                                onClick = { showEmailForm = !showEmailForm },
                                enabled = !state.loading,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text(if (showEmailForm) "Hide email sign-in" else "Use email instead")
                            }
                        }

                        if (manager.emailPasswordSignInConfigured && showEmailForm) {
                            Text(
                                text = "Email & password",
                                style = MaterialTheme.typography.titleSmall,
                                color = WmwColors.Midnight,
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it.trimStart().take(MAX_EMAIL_LENGTH) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Email") },
                                singleLine = true,
                                enabled = !state.loading,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it.take(MAX_PASSWORD_LENGTH) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") },
                                singleLine = true,
                                enabled = !state.loading,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                            )
                            Button(
                                onClick = { scope.launch { manager.signIn(email, password) } },
                                enabled = !state.loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign in with email")
                            }
                            OutlinedButton(
                                onClick = { scope.launch { manager.signUp(email, password) } },
                                enabled = !state.loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Create email account")
                            }
                        }
                    }
                }
            }

            if (state.loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WmwSpacing.Md),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.notice?.let { notice ->
                AccountMessage(
                    text = notice,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                )
            }
            state.error?.let { error ->
                AccountMessage(
                    text = error,
                    modifier = Modifier.padding(top = WmwSpacing.Md),
                )
            }

            Text(
                text = "Signing in never becomes wake authority. Exact alarm scheduling, playback, Stop and Snooze remain local Android responsibilities.",
                modifier = Modifier.padding(top = WmwSpacing.Xl),
                style = MaterialTheme.typography.bodySmall,
                color = WmwColors.LightQuietText,
            )
        }
    }
}

@Composable
private fun AccountMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = WmwColors.LightSurfaceMuted,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(WmwSpacing.Md),
            style = MaterialTheme.typography.bodyMedium,
            color = WmwColors.Midnight,
        )
    }
}

private const val MAX_EMAIL_LENGTH = 320
private const val MAX_PASSWORD_LENGTH = 128
