package com.example.marketwatch.main

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketwatch.auth.AuthViewModel

data class Preference(val title: String, val key: String, val icon: ImageVector, val options: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel
) {
    val userName by authViewModel.userName.collectAsState()
    val userEmail by authViewModel.userEmail.collectAsState()
    val userPreferences by authViewModel.userPreferences.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val preferences = listOf(
        Preference("Timezone", "timezone", Icons.Default.Language, listOf("(UTC+3) Israel", "(UTC-4) New York", "(UTC+1) London")),    
        Preference("Base Currency", "currency", Icons.Default.Paid, listOf(
            "USD - $ (US Dollar)", 
            "ILS - ₪ (Israeli Shekel)",
            "EUR - € (Euro)",
            "GBP - £ (British Pound)",
            "JPY - ¥ (Japanese Yen)"
        ))
    )

    if (showPasswordDialog) {
        ChangePasswordDialog(
            authViewModel = authViewModel,
            onDismiss = { showPasswordDialog = false }
        )
    }
    
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                authViewModel.deleteUser { success, message ->
                    if (!success) {
                        Toast.makeText(context, "Failed to delete account: $message", Toast.LENGTH_LONG).show()
                    }
                    // Navigation is now handled by the AuthStateListener
                }
                showDeleteDialog = false // Dismiss the dialog after action
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = userName,
            onConfirm = { newName ->
                authViewModel.updateUserPreference("name", newName)
                showEditNameDialog = false
            },
            onDismiss = { showEditNameDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Header()

        Spacer(modifier = Modifier.height(20.dp))

        PersonalDetailsCard(
            userName = userName, 
            userEmail = userEmail, 
            onChangePasswordClick = { showPasswordDialog = true },
            onEditNameClick = { showEditNameDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))
        
        PreferencesCard(preferences, userPreferences, authViewModel)

        Spacer(modifier = Modifier.height(20.dp))
        
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Account")
        }
    }
}

@Composable
private fun EditNameDialog(currentName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Full Name") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newName) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun Header() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile", 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.Person, contentDescription = "Profile Icon", tint = Color(0xFF8A2BE2))
        }
        Text(
            text = "Manage your personal details and preferences",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PersonalDetailsCard(userName: String, userEmail: String, onChangePasswordClick: () -> Unit, onEditNameClick: () -> Unit) {
    Text(text = "Personal Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Full Name", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = userName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onEditNameClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
             Column {
                Text(text = "Email", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = userEmail, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onChangePasswordClick, modifier = Modifier.align(Alignment.End)) {
                Text("Change Password")
            }
        }
    }
}

@Composable
private fun PreferencesCard(preferences: List<Preference>, userPreferences: Map<String, Any>, authViewModel: AuthViewModel) {
    Text(text = "Preferences", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            preferences.forEachIndexed { index, preference ->
                PreferenceItem(preference, (userPreferences[preference.key] as? String) ?: preference.options[0], authViewModel)
                if (index < preferences.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceItem(preference: Preference, initialValue: String, authViewModel: AuthViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedOption by remember(initialValue) { mutableStateOf(initialValue) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(preference.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = preference.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                preference.options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { 
                        selectedOption = option
                        isExpanded = false
                        authViewModel.updateUserPreference(preference.key, option)
                    })
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(authViewModel: AuthViewModel, onDismiss: () -> Unit) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (newPassword.isNotEmpty() && newPassword == confirmPassword) {
                    authViewModel.changeUserPassword(newPassword) { success, message ->
                        if (success) {
                            Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to update password: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Passwords do not match or are empty", Toast.LENGTH_LONG).show()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
