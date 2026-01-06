package com.example.marketwatch.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.marketwatch.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(authViewModel: AuthViewModel = viewModel()) {
    val userName by authViewModel.userName.collectAsState()
    val userEmail by authViewModel.userEmail.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End, // Changed to End
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "פרופיל", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Person, contentDescription = "Profile Icon", tint = Color(0xFF8A2BE2))
        }
        Text(
            text = "נהל את הפרטים וההעדפות האישיות שלך",
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Details Card
        Text(text = "פרטים אישיים", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(title = "שם מלא", value = userName, icon = Icons.Default.AccountCircle)
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow(title = "אימייל", value = userEmail, icon = Icons.Default.Email)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preferences Card
        Text(text = "העדפות", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FF))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PreferenceItem(title = "אזור זמן", icon = Icons.Default.Language)
                Spacer(modifier = Modifier.height(16.dp))
                PreferenceItem(title = "מטבע בסיס", icon = Icons.Default.Paid)
            }
        }
    }
}

@Composable
fun InfoRow(title: String, value: String, icon: ImageVector) {
    Column {
        Text(text = title, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(icon, contentDescription = null, tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceItem(title: String, icon: ImageVector) {
    var isExpanded by remember { mutableStateOf(false) }
    val timezones = listOf("(UTC+2) ישראל")
    var selectedTimezone by remember { mutableStateOf(timezones[0]) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (title == "אזור זמן") {
            ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
                OutlinedTextField(
                    value = selectedTimezone,
                    onValueChange = {}, // Not needed for dropdown
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                    timezones.forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = { 
                            selectedTimezone = it
                            isExpanded = false
                        })
                    }
                }
            }
        }
    }
}
