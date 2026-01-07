package com.example.marketwatch.main

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.marketwatch.auth.AuthViewModel
import com.example.marketwatch.network.CompanyNews
import com.example.marketwatch.network.FinnhubCompanyProfile
import com.example.marketwatch.network.FinnhubQuote
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stockSymbol: String, 
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    viewModel: StockDetailViewModel = viewModel(factory = StockDetailViewModelFactory(stockSymbol))
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBuyDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val watchlist by authViewModel.watchlist.collectAsState()
    val isWatchlisted = watchlist.contains(stockSymbol)

    if (showBuyDialog) {
        (uiState as? StockDetailUiState.Success)?.let {
            AddTransactionDialog(
                stockName = it.profile.name ?: "",
                currentPrice = it.quote.currentPrice ?: 0.0,
                onConfirm = { quantity, price ->
                    viewModel.addTransaction(it.profile.ticker ?: stockSymbol, it.profile.name ?: "", quantity, price) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) {
                            showBuyDialog = false
                            onNavigateBack()
                        }
                    }
                },
                onDismiss = { showBuyDialog = false }
            )
        }
    }

    if (showSellDialog) {
        (uiState as? StockDetailUiState.Success)?.let {
            AddTransactionDialog(
                stockName = it.profile.name ?: "",
                currentPrice = it.quote.currentPrice ?: 0.0,
                isBuy = false,
                onConfirm = { quantity, price ->
                    viewModel.addTransaction(it.profile.ticker ?: stockSymbol, it.profile.name ?: "", -quantity, price) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) {
                            showSellDialog = false
                            onNavigateBack()
                        }
                    }
                },
                onDismiss = { showSellDialog = false }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stockSymbol.uppercase()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        authViewModel.toggleWatchlist(stockSymbol) { success, added ->
                            if(success) {
                                val message = if(added) "Added to watchlist" else "Removed from watchlist"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Toggle Watchlist",
                            tint = if (isWatchlisted) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            ActionButtons(
                onBuyClick = { showBuyDialog = true },
                onSellClick = { showSellDialog = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is StockDetailUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is StockDetailUiState.Success -> {
                    StockDetailsContent(state.quote, state.profile, state.news)
                }
                is StockDetailUiState.Error -> {
                    Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun StockDetailsContent(quote: FinnhubQuote, profile: FinnhubCompanyProfile, news: List<CompanyNews>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val priceChange = quote.change ?: 0.0
        val percentChange = quote.percentChange ?: 0.0
        val changeColor = if (priceChange >= 0) Color(0xFF00C853) else Color.Red

        StockHeader(profile)
        PriceDetails(
            price = "${String.format(Locale.US, "%.2f", quote.currentPrice)} USD",
            change = "${String.format(Locale.US, "%.2f", priceChange)} (${String.format(Locale.US, "%.2f", percentChange)}%)",
            changeColor = changeColor
        )
        CompanyProfile(profile)
        CompanyNewsFeed(news)
    }
}

@Composable
private fun StockHeader(profile: FinnhubCompanyProfile) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(profile.logo)
                .crossfade(true)
                .build(),
            contentDescription = "${profile.name} logo",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = profile.name ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(text = profile.ticker ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PriceDetails(price: String, change: String, changeColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Price", style = MaterialTheme.typography.labelMedium)
            Text(price, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Day's Change", style = MaterialTheme.typography.labelMedium)
            Text(change, style = MaterialTheme.typography.bodyLarge, color = changeColor)
        }
    }
}

@Composable
private fun CompanyProfile(profile: FinnhubCompanyProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Company Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ProfileRow("Industry", profile.finnhubIndustry)
            ProfileRow("Market Cap", "${String.format(Locale.US, "%.2f", profile.marketCapitalization)}M")
            ProfileRow("IPO Date", profile.ipo)
            ProfileRow("Website", profile.weburl)
        }
    }
}

@Composable
private fun CompanyNewsFeed(news: List<CompanyNews>) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Latest News", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (news.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                news.take(5).forEach { newsItem ->
                    NewsItem(news = newsItem, onClick = {
                        newsItem.url?.let {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            context.startActivity(intent)
                        }
                    })
                }
            }
        } else {
            Text(
                text = "No recent news available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun NewsItem(news: CompanyNews, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = news.image,
                contentDescription = "News article image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp), // Match image size
                verticalArrangement = Arrangement.SpaceBetween // Push content to top and bottom
            ) {
                Text(
                    text = news.headline ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = news.source ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    val date = news.datetime?.let { Date(it * 1000) }
                    val formattedDate = date?.let { SimpleDateFormat("MMM dd, yyyy", Locale.US).format(it) }
                    if (formattedDate != null) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(text = "$label: ", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.4f))
            Text(text = value, modifier = Modifier.weight(0.6f))
        }
    }
}

@Composable
fun ActionButtons(onBuyClick: () -> Unit, onSellClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = onBuyClick, modifier = Modifier.weight(1f)) {
            Text("Buy")
        }
        OutlinedButton(onClick = onSellClick, modifier = Modifier.weight(1f)) {
            Text("Sell")
        }
    }
}

@Composable
fun AddTransactionDialog(
    stockName: String,
    currentPrice: Double,
    isBuy: Boolean = true,
    onConfirm: (quantity: Double, price: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf(String.format(Locale.US, "%.2f", currentPrice)) }
    val title = if (isBuy) "Buy $stockName" else "Sell $stockName"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price per Share") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quantityValue = quantity.toDoubleOrNull() ?: 0.0
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    if (quantityValue > 0 && priceValue > 0) {
                        onConfirm(quantityValue, priceValue)
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
