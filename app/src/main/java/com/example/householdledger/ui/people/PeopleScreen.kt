package com.example.householdledger.ui.people

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.Avatar
import com.example.householdledger.ui.components.EmptyState
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.components.SectionHeader

private enum class PeopleTab(val label: String, val icon: ImageVector) {
    Staff("Staff", Icons.Outlined.People),
    Family("Family", Icons.Outlined.Groups)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    onAddServant: () -> Unit,
    onAddMember: () -> Unit,
    viewModel: PeopleViewModel = hiltViewModel()
) {
    var tab by remember { mutableStateOf(PeopleTab.Staff) }
    val servants by viewModel.servants.collectAsState()
    val members by viewModel.members.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("People", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == PeopleTab.Staff) onAddServant() else onAddMember() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (tab == PeopleTab.Staff) "Add Staff" else "Add Family") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SegmentedTabs(
                selected = tab,
                onSelect = { tab = it },
                staffCount = servants.size,
                familyCount = members.size,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            when (tab) {
                PeopleTab.Staff -> {
                    if (servants.isEmpty()) {
                        EmptyPeople(
                            title = "No staff added yet",
                            description = "Track your staff — their roles, contact details and monthly salary — all in one place.",
                            actionLabel = "Add first staff",
                            onAction = onAddServant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item { SectionHeader(title = "All Staff") }
                            items(servants, key = { it.id }) { servant ->
                                StaffRow(
                                    name = servant.name,
                                    role = servant.role.ifBlank { "Staff" },
                                    phone = servant.phoneNumber,
                                    salary = servant.salary ?: 0.0,
                                    balance = servant.balance,
                                    inviteCode = servant.inviteCode,
                                    onDelete = { viewModel.deleteServant(servant) }
                                )
                            }
                        }
                    }
                }
                PeopleTab.Family -> {
                    if (members.isEmpty()) {
                        EmptyPeople(
                            title = "No family members yet",
                            description = "Add family members so everyone can share the household budget.",
                            actionLabel = "Add family member",
                            onAction = onAddMember
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item { SectionHeader(title = "All Members") }
                            items(members, key = { it.id }) { member ->
                                FamilyRow(
                                    name = member.name,
                                    role = member.role.ifBlank { "Member" },
                                    inviteCode = member.inviteCode,
                                    onDelete = { viewModel.deleteMember(member) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(
    selected: PeopleTab,
    onSelect: (PeopleTab) -> Unit,
    staffCount: Int,
    familyCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            PeopleTab.values().forEach { option ->
                val count = if (option == PeopleTab.Staff) staffCount else familyCount
                val isSelected = option == selected
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    onClick = { onSelect(option) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(option.icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(option.label, style = MaterialTheme.typography.labelLarge, color = fg)
                        if (count > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) fg.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffRow(
    name: String,
    role: String,
    phone: String?,
    salary: Double,
    balance: Double,
    inviteCode: String?,
    onDelete: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = name, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoleChip(role)
                    if (!phone.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = phone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                if (salary > 0 || balance != 0.0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (salary > 0) {
                            Text(
                                text = "Salary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            MoneyText(
                                amount = salary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (balance != 0.0) {
                            if (salary > 0) Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            MoneyText(
                                amount = balance,
                                style = MaterialTheme.typography.labelMedium,
                                tone = if (balance > 0) MoneyTone.Income else MoneyTone.Expense,
                                showSign = true
                            )
                        }
                    }
                }
                if (!inviteCode.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    InviteCodePill(code = inviteCode)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove staff",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FamilyRow(
    name: String,
    role: String,
    inviteCode: String?,
    onDelete: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(name = name, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                RoleChip(role)
                if (!inviteCode.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    InviteCodePill(code = inviteCode)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteCodePill(code: String) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        onClick = {
            clipboard.setText(androidx.compose.ui.text.AnnotatedString(code))
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "INVITE",
                style = com.example.householdledger.ui.theme.EyebrowCaps,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.width(6.dp))
            Text(
                code,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp)
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun RoleChip(role: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = role.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyPeople(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(tonal = true) {
            EmptyState(
                icon = Icons.Outlined.Groups,
                title = title,
                description = description,
                actionLabel = actionLabel,
                onActionClick = onAction
            )
        }
    }
}
