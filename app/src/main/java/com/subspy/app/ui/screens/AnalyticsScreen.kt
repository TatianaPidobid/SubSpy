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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.subspy.app.R
import com.subspy.app.data.model.Subscription
import com.subspy.app.data.model.SubscriptionCategory
import com.subspy.app.ui.theme.GreenAccent
import com.subspy.app.viewmodel.SubscriptionViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val MONTHS_SHOWN = 6
private val MAX_BAR_HEIGHT: Dp = 140.dp

private data class MonthlyPoint(val label: String, val total: Double)

private data class CategorySlice(
    val category: SubscriptionCategory,
    val total: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    subscriptionViewModel: SubscriptionViewModel,
    onBack: () -> Unit
) {
    val subscriptions by subscriptionViewModel.subscriptions.collectAsState()

    val monthlyPoints = remember(subscriptions) { monthlySeries(subscriptions) }
    val categorySlices = remember(subscriptions) { categoryBreakdown(subscriptions) }
    val monthlyTotal = remember(subscriptions) { subscriptions.sumOf { it.monthlyAmount } }
    val yearlyTotal = monthlyTotal * 12

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (subscriptions.isEmpty()) {
                Text(
                    text = stringResource(R.string.analytics_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(
                        label = stringResource(R.string.per_month_total),
                        value = formatMoney(monthlyTotal),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTile(
                        label = stringResource(R.string.per_year_total),
                        value = formatMoney(yearlyTotal),
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionCard(title = stringResource(R.string.monthly_spend_trend)) {
                    MonthlyBarChart(monthlyPoints)
                }

                SectionCard(title = stringResource(R.string.spending_by_category)) {
                    val total = categorySlices.sumOf { it.total }
                    categorySlices.forEach { slice ->
                        CategoryRow(slice = slice, total = total)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = GreenAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun MonthlyBarChart(points: List<MonthlyPoint>) {
    val max = points.maxOfOrNull { it.total } ?: 0.0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        points.forEach { point ->
            val fraction = safeFraction(point.total, max)
            val barHeight = (MAX_BAR_HEIGHT * fraction).coerceAtLeast(4.dp)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatMoneyShort(point.total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(GreenAccent.copy(alpha = 0.35f + 0.65f * fraction))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice, total: Double) {
    val fraction = safeFraction(slice.total, total)
    val color = categoryColor(slice.category)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = categoryLabel(slice.category),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatMoney(slice.total)} · ${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Spend per month for the last [MONTHS_SHOWN] months. A subscription counts
 * towards a month once its first known payment happened on or before that month.
 */
private fun monthlySeries(subscriptions: List<Subscription>): List<MonthlyPoint> {
    val current = YearMonth.now()
    return (0 until MONTHS_SHOWN).map { index ->
        val month = current.minusMonths((MONTHS_SHOWN - 1 - index).toLong())
        val endOfMonth = month.atEndOfMonth()
        val total = subscriptions.filter { sub ->
            val first = try {
                LocalDate.parse(sub.firstPaymentDate)
            } catch (e: Exception) {
                null
            }
            first == null || !first.isAfter(endOfMonth)
        }.sumOf { it.monthlyAmount }
        MonthlyPoint(
            label = month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            total = total
        )
    }
}

private fun categoryBreakdown(subscriptions: List<Subscription>): List<CategorySlice> =
    subscriptions
        .groupBy { it.category }
        .map { (category, subs) ->
            CategorySlice(
                category = category,
                total = subs.sumOf { it.monthlyAmount }
            )
        }
        .sortedByDescending { it.total }

/** Ratio of [value] to [total], always a finite number in 0f..1f. */
private fun safeFraction(value: Double, total: Double): Float {
    if (total <= 0.0) return 0f
    val fraction = (value / total).toFloat()
    return if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else 0f
}

private fun formatMoney(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(amount)

private fun formatMoneyShort(amount: Double): String = when {
    amount >= 1000 -> "${"%.1f".format(amount / 1000)}k"
    else -> "%.0f".format(amount)
}
