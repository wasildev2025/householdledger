package com.example.householdledger.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Financial Reports") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Monthly Summary", style = MaterialTheme.typography.titleLarge)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val totalIncome = transactions.filter { it.type == "income" }.sumOf { it.amount }
            val totalExpense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Income: $${String.format("%.2f", totalIncome)}", color = MaterialTheme.colorScheme.primary)
                    Text("Total Expense: $${String.format("%.2f", totalExpense)}", color = MaterialTheme.colorScheme.error)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Net Balance: $${String.format("%.2f", totalIncome - totalExpense)}", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            // TODO: Add Charts using a library like Compose-Charts or similar
        }
    }
}
