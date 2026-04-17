package com.example.householdledger.ui.dairy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.data.model.DairyLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DairyTrackerScreen(
    onBack: () -> Unit,
    viewModel: DairyViewModel = hiltViewModel()
) {
    val logs by viewModel.dairyLogs.collectAsState()
    val household by viewModel.household.collectAsState()

    var milkQty by remember { mutableStateOf("") }
    var yogurtQty by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dairy Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Monthly Summary", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val totalBill = logs.sumOf { it.totalBill }
                    Text("Total Bill: $${String.format("%.2f", totalBill)}", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Entry Form
            Text("Log New Entry", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = milkQty,
                    onValueChange = { milkQty = it },
                    label = { Text("Milk (L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yogurtQty,
                    onValueChange = { yogurtQty = it },
                    label = { Text("Yogurt (Kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addEntry(
                        milkQty.toDoubleOrNull() ?: 0.0,
                        yogurtQty.toDoubleOrNull() ?: 0.0
                    )
                    milkQty = ""
                    yogurtQty = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Entry")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History
            Text("Recent Logs", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    DairyLogItem(log, onDelete = { viewModel.deleteEntry(log) })
                }
            }
        }
    }
}

@Composable
fun DairyLogItem(log: DairyLog, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = log.date.substring(0, 10), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Milk: ${log.milkQty}L, Yogurt: ${log.yogurtQty}Kg",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$${String.format("%.2f", log.totalBill)}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
