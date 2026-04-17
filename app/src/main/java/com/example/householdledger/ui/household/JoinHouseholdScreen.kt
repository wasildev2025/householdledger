package com.example.householdledger.ui.household

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.MainViewModel

@Composable
fun JoinHouseholdScreen(
    onSuccess: () -> Unit,
    mainViewModel: MainViewModel,
    viewModel: JoinHouseholdViewModel = hiltViewModel()
) {
    var step by remember { mutableStateOf("choice") }
    var inviteCode by remember { mutableStateOf("") }
    var householdName by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is JoinState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            "choice" -> {
                Text("Get Started", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { step = "join" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Join Existing Household")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { step = "create" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create New Household")
                }
            }
            "join" -> {
                Text("Join Household", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("Invite Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (state is JoinState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = { viewModel.join(inviteCode) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Join")
                    }
                    TextButton(onClick = { step = "choice" }) {
                        Text("Back")
                    }
                }
            }
            "create" -> {
                Text("Create Household", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = householdName,
                    onValueChange = { householdName = it },
                    label = { Text("Household Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (state is JoinState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = { viewModel.create(householdName) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Create")
                    }
                    TextButton(onClick = { step = "choice" }) {
                        Text("Back")
                    }
                }
            }
        }
        
        if (state is JoinState.Error) {
            Text(
                text = (state as JoinState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = { mainViewModel.signOut() }) {
            Text("Sign Out", color = MaterialTheme.colorScheme.error)
        }
    }
}
