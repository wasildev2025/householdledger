package com.example.householdledger.ui.people

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    onAddServant: () -> Unit,
    onAddMember: () -> Unit,
    viewModel: PeopleViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val servants by viewModel.servants.collectAsState()
    val members by viewModel.members.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("People") })
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Staff") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Family") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (selectedTab == 0) onAddServant() else onAddMember()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Person")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedTab == 0) {
                items(servants) { servant ->
                    PersonItem(
                        name = servant.name,
                        role = servant.role,
                        onDelete = { viewModel.deleteServant(servant) }
                    )
                }
            } else {
                items(members) { member ->
                    PersonItem(
                        name = member.name,
                        role = "Family Member",
                        onDelete = { viewModel.deleteMember(member) }
                    )
                }
            }
        }
    }
}

@Composable
fun PersonItem(name: String, role: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(text = role, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
