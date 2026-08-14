package com.subspy.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subspy.app.R
import com.subspy.app.data.model.Subscription
import com.subspy.app.data.model.SubscriptionCategory
import com.subspy.app.data.model.UsageStatus
import com.subspy.app.ui.theme.ForgottenRed
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.viewmodel.SubscriptionUiState
import com.subspy.app.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    subscriptionViewModel: SubscriptionViewModel,
    onSubscriptionClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onAddSourcesClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by subscriptionViewModel.uiState.collectAsState()
    val subscriptions by subscriptionViewModel.subscriptions.collectAsState()
    val totalMonthlySpend by subscriptionViewModel.totalMonthlySpend.collectAsState()
    val isPremium by subscriptionViewModel.isPremium.collectAsState()
    var selectedCategory by remember { mutableStateOf<SubscriptionCategory?>(null) }

    val usedCategories = SubscriptionCategory.entries.filter { category ->
        subscriptions.any { it.category == category }
    }
    val visibleSubscriptions = subscriptions.filter {
        selectedCategory == null || it.category == selectedCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (!isPremium) {
                        IconButton(onClick = onPremiumClick) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(R.string.upgrade_to_premium),
                                tint = GreenAccent
                            )
                        }
                    }
                    IconButton(onClick = onAnalyticsClick) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.analytics_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.notification_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { subscriptionViewModel.refreshSubscriptions() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSourcesClick,
                containerColor = GreenAccent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_subscription)) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Monthly Spend Card
            item {
                TotalSpendCard(totalMonthlySpend)
            }

            // Category filter
            if (usedCategories.size > 1) {
                item {
                    CategoryFilterRow(
                        categories = usedCategories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }
            }

            // Premium Banner (if not premium)
            if (!isPremium && subscriptions.size >= 3) {
                item {
                    PremiumBanner(onClick = onPremiumClick)
                }
            }

            // Content based on state
            when (uiState) {
                is SubscriptionUiState.Loading,
                is SubscriptionUiState.Scanning -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = GreenAccent)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.scanning_emails),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                is SubscriptionUiState.Empty -> {
                    item {
                        EmptyState(onScan = { subscriptionViewModel.scanEmails() })
                    }
                }
                is SubscriptionUiState.Error -> {
                    item {
                        ErrorState(
                            message = (uiState as SubscriptionUiState.Error).message,
                            onRetry = { subscriptionViewModel.refreshSubscriptions() }
                        )
                    }
                }
                is SubscriptionUiState.Success -> {
                    // Subscriptions the user marked as unused — worth cancelling.
                    val unused = visibleSubscriptions.filter { it.usage == UsageStatus.NOT_USED }
                    if (unused.isNotEmpty()) {
                        item {
                            CancelSuggestionCard(
                                count = unused.size,
                                yearlyWaste = unused.sumOf { it.monthlyAmount } * 12
                            )
                        }
                        items(unused) { subscription ->
                            SubscriptionItem(
                                subscription = subscription,
                                onClick = { onSubscriptionClick(subscription.id) },
                                isForgotten = true
                            )
                        }
                    }

                    // Forgotten subscriptions section
                    val forgotten = visibleSubscriptions.filter {
                        it.isForgotten && it.usage != UsageStatus.NOT_USED
                    }
                    if (forgotten.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.forgotten_subscriptions),
                                style = MaterialTheme.typography.titleMedium,
                                color = ForgottenRed,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(forgotten) { subscription ->
                            SubscriptionItem(
                                subscription = subscription,
                                onClick = { onSubscriptionClick(subscription.id) },
                                isForgotten = true
                            )
                        }
                    }

                    // Active subscriptions section
                    val active = visibleSubscriptions.filter {
                        !it.isForgotten && it.usage != UsageStatus.NOT_USED
                    }
                    if (active.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.active_subscriptions),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(active) { subscription ->
                            SubscriptionItem(
                                subscription = subscription,
                                onClick = { onSubscriptionClick(subscription.id) },
                                isForgotten = false
                            )
                        }
                    }

                    if (visibleSubscriptions.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_subscriptions_in_category),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TotalSpendCard(totalMonthlySpend: Double) {
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
            Text(
                text = stringResource(R.string.total_monthly_spend),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.monthly_spend,
                    formatCurrency(totalMonthlySpend)
                ),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 42.sp),
                color = GreenAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SubscriptionItem(
    subscription: Subscription,
    onClick: () -> Unit,
    isForgotten: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isForgotten) {
                ForgottenRed.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isForgotten) ForgottenRed.copy(alpha = 0.2f)
                        else GreenAccent.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subscription.serviceName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isForgotten) ForgottenRed else GreenAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subscription.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isForgotten) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = ForgottenRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.next_billing, subscription.nextBillingDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = categoryLabel(subscription.category),
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor(subscription.category)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(subscription.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isForgotten) ForgottenRed else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (subscription.frequency == com.subspy.app.data.model.BillingFrequency.MONTHLY)
                        stringResource(R.string.per_month)
                    else stringResource(R.string.per_year),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    categories: List<SubscriptionCategory>,
    selected: SubscriptionCategory?,
    onSelect: (SubscriptionCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.category_all)) }
        )
        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                label = { Text(categoryLabel(category)) }
            )
        }
    }
}

@Composable
private fun CancelSuggestionCard(count: Int, yearlyWaste: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ForgottenRed.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ForgottenRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.suggested_to_cancel),
                    style = MaterialTheme.typography.titleSmall,
                    color = ForgottenRed,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.suggested_to_cancel_desc,
                        count,
                        formatCurrency(yearlyWaste)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = GreenAccent.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = GreenAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.upgrade_to_premium),
                    style = MaterialTheme.typography.titleSmall,
                    color = GreenAccent
                )
                Text(
                    text = stringResource(R.string.free_limit_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_subscriptions_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(
            onClick = onScan,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = GreenAccent
            )
        ) {
            Text(stringResource(R.string.refresh), color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}
