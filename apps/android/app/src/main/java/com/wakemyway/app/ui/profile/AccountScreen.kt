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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                text = "Sign in for secure backup, account-based access and future Pro features. Your alarms remain local and keep working without an account.",
                modifier = Modifier.padding(top = WmwSpacing.Sm),
                style = MaterialTheme.typography.bodyLarge,
                color = WmwColors.LightQuietText,
            )

            if (!state.configured) {
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
                            text = "Account sign-in is not connected in this build yet.",
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
                            Text("Sign in")
                        }
                        OutlinedButton(
                            onClick = { scope.launch { manager.signUp(email, password) } },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Create account")
                        }

                        Text(
                            text = "or",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.labelMedium,
                            color = WmwColors.LightQuietText,
                        )

                        OutlinedButton(
                            onClick = { scope.launch { manager.signInWithGoogle() } },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Continue with Google")
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
