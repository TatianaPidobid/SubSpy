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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subspy.app.R
import com.subspy.app.data.model.BillingFrequency
import com.subspy.app.data.model.Subscription
import com.subspy.app.data.model.UsageStatus
import com.subspy.app.ui.theme.ForgottenRed
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.ui.theme.TextOnGreen
import com.subspy.app.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    subscriptionId: String,
    subscriptionViewModel: SubscriptionViewModel,
    onCancelClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val subscriptions by subscriptionViewModel.subscriptions.collectAsState()
    val subscription = subscriptions.find { it.id == subscriptionId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.subscription_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (subscription == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.error_occurred),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            DetailBody(
                subscription = subscription,
                subscriptionViewModel = subscriptionViewModel,
                onCancelClick = { onCancelClick(subscription.id) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun DetailBody(
    subscription: Subscription,
    subscriptionViewModel: SubscriptionViewModel,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
            // Service header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (subscription.isForgotten) ForgottenRed.copy(alpha = 0.2f)
                                else GreenAccent.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subscription.serviceName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (subscription.isForgotten) ForgottenRed else GreenAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = subscription.serviceName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AssistChip(
                        onClick = {},
                        label = { Text(categoryLabel(subscription.category)) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = categoryColor(subscription.category)
                        )
                    )

                    if (subscription.isForgotten) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.last_used_over_6_months),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForgottenRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Details cards
            DetailInfoCard(
                label = stringResource(R.string.monthly_amount),
                value = formatCurrency(subscription.monthlyAmount),
                icon = Icons.Default.Payments
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailInfoCard(
                label = stringResource(R.string.total_spent),
                value = formatCurrency(subscription.totalSpent),
                icon = Icons.Default.Payments
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailInfoCard(
                label = stringResource(R.string.next_billing_date),
                value = subscription.nextBillingDate,
                icon = Icons.Default.CalendarMonth
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailInfoCard(
                label = stringResource(R.string.billing_frequency),
                value = if (subscription.frequency == BillingFrequency.MONTHLY)
                    stringResource(R.string.monthly) else stringResource(R.string.yearly),
                icon = Icons.Default.CalendarMonth
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailInfoCard(
                label = stringResource(R.string.first_payment),
                value = subscription.firstPaymentDate,
                icon = Icons.Default.CalendarMonth
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Usage rating: lets SubSpy suggest what is worth cancelling.
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.usage_question),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = subscription.usage == UsageStatus.ACTIVELY_USED,
                            onClick = {
                                subscriptionViewModel.setUsage(
                                    subscription.id,
                                    UsageStatus.ACTIVELY_USED
                                )
                            },
                            label = { Text(stringResource(R.string.usage_active)) }
                        )
                        FilterChip(
                            selected = subscription.usage == UsageStatus.NOT_USED,
                            onClick = {
                                subscriptionViewModel.setUsage(
                                    subscription.id,
                                    UsageStatus.NOT_USED
                                )
                            },
                            label = { Text(stringResource(R.string.usage_not_used)) }
                        )
                    }
                    if (subscription.usage == UsageStatus.NOT_USED) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                R.string.usage_cancel_advice,
                                formatCurrency(subscription.monthlyAmount * 12)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForgottenRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // The single, unmistakable action: start the guided cancellation.
            Button(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = stringResource(R.string.cancel_subscription),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextOnGreen
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DetailInfoCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}
