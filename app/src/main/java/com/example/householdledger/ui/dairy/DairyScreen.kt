package com.example.householdledger.ui.dairy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DairyScreen(
    onNavigateBack: () -> Unit,
    viewModel: DairyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    var milkQty by remember { mutableStateOf("") }
    var yogurtQty by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dairy Tracker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            // Summary Card
            AppCard(contentPadding = PaddingValues(20.dp)) {
                Column {
                    Text("Total Monthly Bill", style = MaterialTheme.typography.labelLarge)
                    MoneyText(
                        amount = state.totalMonthlyBill,
                        style = MaterialTheme.typography.headlineLarge,
                        tone = MoneyTone.Expense
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Add Log Entry
            Text("Add Today's Entry", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = milkQty,
                    onValueChange = { milkQty = it },
                    label = { Text("Milk (Liters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yogurtQty,
                    onValueChange = { yogurtQty = it },
                    label = { Text("Yogurt (Kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val mQty = milkQty.toDoubleOrNull() ?: 0.0
                    val yQty = yogurtQty.toDoubleOrNull() ?: 0.0
                    if (mQty > 0 || yQty > 0) {
                        viewModel.addLog(mQty, yQty)
                        milkQty = ""
                        yogurtQty = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Entry")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Logs", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Logs List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.logs, key = { it.id }) { log ->
                    AppCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(log.date, style = MaterialTheme.typography.labelMedium)
                                Text("Milk: ${log.milkQty}L, Yogurt: ${log.yogurtQty}Kg", style = MaterialTheme.typography.bodySmall)
                            }
                            MoneyText(amount = log.totalBill, tone = MoneyTone.Expense)
                        }
                    }
                }
            }
        }
    }
}
