package com.apurvpandey.expiryticker.presentation.insights

import com.apurvpandey.expiryticker.presentation.common.labelSpacing
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.apurvpandey.expiryticker.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apurvpandey.expiryticker.AppContainer
import com.apurvpandey.expiryticker.core.util.CurrencyFormatter
import com.apurvpandey.expiryticker.core.util.Insights
import com.apurvpandey.expiryticker.presentation.common.label
import com.apurvpandey.expiryticker.presentation.components.CategoryIconBadge
import com.apurvpandey.expiryticker.presentation.theme.ExpiryTheme
import java.time.YearMonth
import java.time.format.TextStyle

@Composable
fun InsightsRoute(container: AppContainer, onNavigateBack: () -> Unit) {
    val vm: InsightsViewModel = viewModel(
        factory = InsightsViewModel.factory(container.expiryItemRepository, container.renewalHistoryRepository)
    )
    val insights by vm.insights.collectAsStateWithLifecycle()
    InsightsScreen(insights = insights, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(insights: Insights?, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            insights == null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !insights.hasData -> EmptyInsights(Modifier.padding(innerPadding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigNumber(stringResource(R.string.yearly_cost), insights.annualPaise, stringResource(R.string.if_everything_keeps_renewing), Modifier.weight(1f))
                    BigNumber(stringResource(R.string.paid_last_12_months), insights.paidLast12MonthsPaise, stringResource(R.string.from_your_history), Modifier.weight(1f))
                }
                if (insights.upcoming.any { it.paise > 0 }) UpcomingChart(insights)
                if (insights.byCategory.isNotEmpty()) CategoryBreakdown(insights)
                if (insights.priceRises.isNotEmpty()) PriceRises(insights)
                if (insights.openItemsWithoutCost > 0) {
                    Text(
                        pluralStringResource(R.plurals.insights_no_cost, insights.openItemsWithoutCost, insights.openItemsWithoutCost),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInsights(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Insights, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.nothing_to_show_yet), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.add_an_expected_cost_to_your),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = labelSpacing(1.sp)
            )
            content()
        }
    }
}

@Composable
private fun BigNumber(label: String, paise: Long, caption: String, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(CurrencyFormatter.format(paise), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpcomingChart(insights: Insights) {
    // Read through Compose so labels follow a language change instead of a stale default.
    val locale = ComposeLocale.current.platformLocale
    val max = insights.upcoming.maxOf { it.paise }.coerceAtLeast(1L)
    val thisMonth = YearMonth.now()
    val total = insights.upcoming.sumOf { it.paise }
    SectionCard(stringResource(R.string.next_12_months, CurrencyFormatter.format(total))) {
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            insights.upcoming.forEach { m ->
                val fraction = m.paise.toFloat() / max
                val monthA11y = stringResource(
                    R.string.month_amount_a11y,
                    m.month.month.getDisplayName(TextStyle.FULL, locale),
                    CurrencyFormatter.format(m.paise)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { contentDescription = monthA11y },
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((88 * fraction).coerceAtLeast(if (m.paise > 0) 3f else 0f).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (m.month == thisMonth) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        m.month.month.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val busiest = insights.upcoming.maxByOrNull { it.paise }
        if (busiest != null && busiest.paise > 0) {
            Text(
                stringResource(
                    R.string.busiest_month,
                    busiest.month.month.getDisplayName(TextStyle.FULL, locale),
                    CurrencyFormatter.format(busiest.paise)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBreakdown(insights: Insights) {
    val max = insights.byCategory.maxOf { it.annualPaise }.coerceAtLeast(1L)
    SectionCard(stringResource(R.string.by_category_per_year)) {
        insights.byCategory.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryIconBadge(category = row.category, size = 36.dp, iconSize = 18.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(row.category.label(), style = MaterialTheme.typography.bodyMedium)
                        Text(CurrencyFormatter.format(row.annualPaise), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(row.annualPaise.toFloat() / max)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceRises(insights: Insights) {
    val color = ExpiryTheme.colors.overdue.accent
    SectionCard(stringResource(R.string.price_increases)) {
        insights.priceRises.forEach { rise ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(rise.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(R.string.price_from_to, CurrencyFormatter.format(rise.fromPaise), CurrencyFormatter.format(rise.toPaise)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(stringResource(R.string.price_rise, rise.percent), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
