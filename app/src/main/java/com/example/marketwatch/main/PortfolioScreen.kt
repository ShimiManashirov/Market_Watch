package com.example.marketwatch.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Composable
fun PortfolioScreen(
    portfolioViewModel: PortfolioViewModel = viewModel(),
    onStockClick: (String) -> Unit
) {
    val holdings by portfolioViewModel.holdings.collectAsState()
    val totalPortfolioValue by portfolioViewModel.totalPortfolioValue.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TotalPortfolioValue(value = totalPortfolioValue, currencySymbol = holdings.firstOrNull()?.currencySymbol ?: "$")
        if (holdings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Your portfolio is empty. Buy a stock to get started.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(holdings, key = { it.symbol }) { holding ->
                    HoldingCard(
                        holding = holding,
                        onClick = { onStockClick(holding.symbol) }
                    )
                }
            }
        }
    }
}

@Composable
fun TotalPortfolioValue(value: Double, currencySymbol: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total Portfolio Value", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${String.format(Locale.US, "%,.2f", value)} $currencySymbol",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HoldingCard(holding: Holding, onClick: () -> Unit) {
    val currentValue = holding.totalValue ?: (holding.averagePrice * holding.totalQuantity)
    val totalCost = holding.averagePrice * holding.totalQuantity
    val gainLoss = currentValue - totalCost
    val gainLossPercent = if (totalCost > 0) (gainLoss / totalCost) * 100 else 0.0
    val changeColor = if (gainLoss >= 0) Color(0xFF00C853) else Color.Red

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = holding.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = holding.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${String.format(Locale.US, "%.2f", holding.totalQuantity)} shares", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (holding.currentPrice != null) {
                        Text(
                            text = "${String.format(Locale.US, "%,.2f", currentValue)} ${holding.currencySymbol}", 
                            fontWeight = FontWeight.SemiBold, 
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", gainLoss)} (${String.format(Locale.US, "%.2f", gainLossPercent)}%)",
                            color = changeColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                     else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
