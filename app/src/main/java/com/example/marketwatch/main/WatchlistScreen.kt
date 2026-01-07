package com.example.marketwatch.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.marketwatch.auth.AuthViewModel
import java.util.Locale

@Composable
fun WatchlistScreen(
    authViewModel: AuthViewModel = viewModel(),
    onStockClick: (String) -> Unit
) {
    val watchlistViewModel: WatchlistViewModel = viewModel(factory = WatchlistViewModelFactory(authViewModel))
    val watchlistItems by watchlistViewModel.watchlistItems.collectAsState()

    if (watchlistItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your watchlist is empty. Add stocks to track them.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(watchlistItems, key = { it.symbol }) { item ->
                WatchlistItemCard(
                    item = item,
                    onClick = { onStockClick(item.symbol) }
                )
            }
        }
    }
}

@Composable
fun WatchlistItemCard(item: WatchlistItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                item.name?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (item.quote != null) {
                Column(horizontalAlignment = Alignment.End) {
                    val priceChange = item.quote.change ?: 0.0
                    val changeColor = if (priceChange >= 0) Color(0xFF00C853) else Color.Red
                    Text(
                        text = "${String.format(Locale.US, "%.2f", item.quote.currentPrice)} USD",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f", priceChange),
                        color = changeColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}
