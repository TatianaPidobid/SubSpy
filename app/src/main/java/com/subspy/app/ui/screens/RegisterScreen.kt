package com.subspy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.subspy.app.R
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.viewmodel.AuthState
import com.subspy.app.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess()
        }
    }

    val loading = authState is AuthState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    authViewModel.clearError()
                    onBackToLogin()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.create_account),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(28.dp))

                AuthTextField(
                    value = name,
                    onValueChange = { name = it; authViewModel.clearError() },
                    label = stringResource(R.string.field_name),
                    enabled = !loading
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthTextField(
                    value = email,
                    onValueChange = { email = it; authViewModel.clearError() },
                    label = stringResource(R.string.field_email),
                    isEmail = true,
                    enabled = !loading
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthTextField(
                    value = password,
                    onValueChange = { password = it; authViewModel.clearError() },
                    label = stringResource(R.string.field_password),
                    isPassword = true,
                    enabled = !loading
                )
                Spacer(modifier = Modifier.height(14.dp))
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; authViewModel.clearError() },
                    label = stringResource(R.string.field_confirm_password),
                    isPassword = true,
                    enabled = !loading
                )

                if (authState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryButton(
                    text = stringResource(R.string.sign_up),
                    enabled = !loading,
                    onClick = {
                        authViewModel.registerWithEmail(name, email, password, confirmPassword)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (loading) {
                    CircularProgressIndicator(color = GreenAccent, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.have_account),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            authViewModel.clearError()
                            onBackToLogin()
                        },
                        enabled = !loading
                    ) {
                        Text(stringResource(R.string.sign_in), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
