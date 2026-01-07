package com.example.marketwatch.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val portfolioItems by portfolioViewModel.portfolioItems.collectAsState()

    if (portfolioItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your portfolio is empty. Add a stock to get started.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(portfolioItems, key = { it.stock.id }) { item ->
                StockCard(
                    item = item, 
                    onRemove = { portfolioViewModel.removeStock(item.stock.id) },
                    onClick = { onStockClick(item.stock.symbol) } 
                )
            }
        }
    }
}

@Composable
fun StockCard(item: PortfolioItem, onRemove: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.stock.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(text = item.stock.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (item.quote != null) {
                Column(horizontalAlignment = Alignment.End) {
                    val priceChange = item.quote.change ?: 0.0
                    val percentChange = item.quote.percentChange ?: 0.0
                    val changeColor = if (priceChange >= 0) Color(0xFF00C853) else Color.Red

                    Text(
                        text = "${String.format(Locale.US, "%.2f", item.quote.currentPrice)} USD", 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", priceChange)} (${String.format(Locale.US, "%.2f", percentChange)}%)",
                        color = changeColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Stock")
            }
        }
    }
}
