package com.subspy.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.subspy.app.R
import com.subspy.app.data.model.BillingFrequency
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(
    subscriptionViewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val currencies = listOf("USD", "EUR", "GBP", "INR", "RUB", "UAH", "PLN")

    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var currencyExpanded by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(BillingFrequency.MONTHLY) }
    var nextBillingDate by remember { mutableStateOf("") }

    val amount = amountText.replace(",", ".").toDoubleOrNull()
    val isValid = name.isNotBlank() && amount != null && amount > 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.source_manual_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_service_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.field_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it }
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_currency)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    currencies.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                currency = option
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.billing_frequency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = frequency == BillingFrequency.MONTHLY,
                    onClick = { frequency = BillingFrequency.MONTHLY },
                    label = { Text(stringResource(R.string.monthly)) }
                )
                FilterChip(
                    selected = frequency == BillingFrequency.YEARLY,
                    onClick = { frequency = BillingFrequency.YEARLY },
                    label = { Text(stringResource(R.string.yearly)) }
                )
            }

            OutlinedTextField(
                value = nextBillingDate,
                onValueChange = { nextBillingDate = it },
                label = { Text(stringResource(R.string.field_next_billing_optional)) },
                placeholder = { Text("2026-07-01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    subscriptionViewModel.addManualSubscription(
                        serviceName = name,
                        amount = amount ?: 0.0,
                        currency = currency,
                        frequency = frequency,
                        nextBillingDate = nextBillingDate.trim()
                    )
                    onBack()
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
