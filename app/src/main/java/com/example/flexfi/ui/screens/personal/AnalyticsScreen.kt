package com.example.flexfi.ui.screens.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.FlexFiTopBar
import com.example.flexfi.ui.theme.*
import com.example.flexfi.utils.CurrencyProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit
) {
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val dailyTrend by viewModel.dailyTrend.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FlexFiTopBar(
                title = "Analytics",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Monthly Total Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = FlexFiLightBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "This Month's Spending",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FlexFiDarkText.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            CurrencyProvider.formatAmount(monthlyTotal),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = FlexFiBlue
                        )
                    }
                }
            }

            // Line Graph (Daily Trends)
            if (dailyTrend.isNotEmpty() && monthlyTotal > 0) {
                item {
                    Text("Daily Spending Trend", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            DailyTrendGraph(dailyTrend)
                        }
                    }
                }
            } else if (monthlyTotal == 0.0) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                           Icon(Icons.Default.Insights, null, tint = FlexFiLightText, modifier = Modifier.size(48.dp))
                           Spacer(Modifier.height(8.dp))
                           Text("No data mathematically available", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = FlexFiDarkText)
                        }
                    }
                }
            }

            // Pie Chart (Category Breakdown)
            if (categoryBreakdown.isNotEmpty() && monthlyTotal > 0) {
                item {
                    Text("Category Breakdown", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                                PieChart(categoryBreakdown)
                            }
                            Spacer(Modifier.height(24.dp))
                            
                            // Legend
                            categoryBreakdown.forEach { item ->
                                val color = getCategoryColor(item.category)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(color))
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.category, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${"%.1f".format(item.percentage * 100)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTrendGraph(data: List<DailySum>) {
    val maxAmount = data.maxOfOrNull { it.totalAmount } ?: 1.0
    val graphColor = FlexFiBlue

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        val path = Path()
        
        data.forEachIndexed { index, dailySum ->
            val x = index * stepX
            val y = height - ((dailySum.totalAmount / maxAmount) * height).toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = graphColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun PieChart(data: List<CategorySum>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        data.forEach { item ->
            val sweepAngle = item.percentage * 360f
            val color = getCategoryColor(item.category)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt),
                size = Size(size.width - 30.dp.toPx(), size.height - 30.dp.toPx()),
                topLeft = Offset(15.dp.toPx(), 15.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food" -> CategoryFood
        "transport" -> CategoryTransport
        "shopping" -> CategoryShopping
        "entertainment" -> CategoryEntertainment
        "health" -> CategoryHealth
        "utilities" -> CategoryUtilities
        "rent" -> CategoryRent
        else -> CategoryOther
    }
}
