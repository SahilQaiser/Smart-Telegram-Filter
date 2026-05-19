package com.invictus.smarttelegramfilter.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.drinkless.tdlib.TdApi

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val error     by viewModel.error.collectAsStateWithLifecycle()
    val loading   by viewModel.isLoading.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        if (error != null) {
            snackbar.showSnackbar(error!!)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Smart Filter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Sign in to your Telegram account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))

            when (authState) {
                is TdApi.AuthorizationStateWaitPhoneNumber ->
                    PhoneStep(loading = loading, onSubmit = viewModel::submitPhone)

                is TdApi.AuthorizationStateWaitCode ->
                    CodeStep(loading = loading, onSubmit = viewModel::submitCode)

                is TdApi.AuthorizationStateWaitPassword ->
                    PasswordStep(loading = loading, onSubmit = viewModel::submitPassword)

                null,
                is TdApi.AuthorizationStateWaitTdlibParameters ->
                    CircularProgressIndicator()

                else -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PhoneStep(loading: Boolean, onSubmit: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    AuthForm(
        label = "Phone number",
        placeholder = "+1 415 555 2671",
        value = phone,
        onValueChange = { phone = it },
        keyboardType = KeyboardType.Phone,
        buttonText = "Send code",
        loading = loading,
        onSubmit = { onSubmit(phone) },
    )
}

@Composable
private fun CodeStep(loading: Boolean, onSubmit: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AuthForm(
        label = "Verification code",
        placeholder = "12345",
        value = code,
        onValueChange = { code = it },
        keyboardType = KeyboardType.Number,
        buttonText = "Verify",
        loading = loading,
        onSubmit = { onSubmit(code) },
    )
}

@Composable
private fun PasswordStep(loading: Boolean, onSubmit: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    AuthForm(
        label = "Two-step verification password",
        placeholder = "••••••••",
        value = pw,
        onValueChange = { pw = it },
        keyboardType = KeyboardType.Password,
        visualTransformation = true,
        buttonText = "Confirm",
        loading = loading,
        onSubmit = { onSubmit(pw) },
    )
}

@Composable
private fun AuthForm(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    buttonText: String,
    loading: Boolean,
    onSubmit: () -> Unit,
    visualTransformation: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            visualTransformation = if (visualTransformation) PasswordVisualTransformation()
                                   else androidx.compose.ui.text.input.VisualTransformation.None,
        )
        Button(
            onClick = onSubmit,
            enabled = value.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            else Text(buttonText, style = MaterialTheme.typography.labelLarge)
        }
    }
}
