package com.example.householdledger.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.AppCard
import com.example.householdledger.ui.components.CategoryPill
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.util.resolveCategoryIcon

private val PALETTE = listOf(
    "#E8833A", "#D14343", "#2F8F6A", "#0F766E",
    "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899",
    "#F59E0B", "#B08448"
)

/**
 * Icon options presented in the category picker. Values match Ionicons names so
 * [resolveCategoryIcon] round-trips them into real Material icons on list/home
 * screens. Grouped for scanability.
 */
private data class IconGroup(val title: String, val icons: List<String>)

private val ICON_GROUPS = listOf(
    IconGroup(
        "Everyday",
        listOf("cart", "fast-food", "restaurant", "cafe", "milk", "gift")
    ),
    IconGroup(
        "Home",
        listOf("home", "flash", "water", "flame", "wifi", "phone-portrait")
    ),
    IconGroup(
        "Transport",
        listOf("car", "bus", "airplane", "train")
    ),
    IconGroup(
        "Health & fitness",
        listOf("medkit", "fitness", "heart")
    ),
    IconGroup(
        "Entertainment",
        listOf("musical-notes", "game-controller", "film", "book", "school")
    ),
    IconGroup(
        "Money",
        listOf("cash", "wallet", "card", "receipt")
    ),
    IconGroup(
        "Personal",
        listOf("shirt", "paw")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    categoryId: String?,
    onBack: () -> Unit,
    viewModel: AddCategoryViewModel = hiltViewModel(),
    catListVm: CategoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState by catListVm.state.collectAsState()

    LaunchedEffect(categoryId, listState.items) {
        if (!categoryId.isNullOrBlank() && state.name.isBlank()) {
            val existing = listState.items.firstOrNull { it.category.id == categoryId }
            if (existing != null) viewModel.startEditing(existing.category)
        }
    }

    LaunchedEffect(state.done) { if (state.done) onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                },
                title = {
                    Text(
                        if (!categoryId.isNullOrBlank()) "Edit category" else "New category",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            AppCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        label = { Text("Name") },
                        singleLine = true,
                        isError = state.error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.budget,
                        onValueChange = viewModel::setBudget,
                        label = { Text("Monthly budget (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.error != null) {
                        Text(
                            state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text("Color", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PALETTE.take(5).forEach { hex ->
                    ColorDot(hex, selected = hex == state.color) { viewModel.setColor(hex) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PALETTE.drop(5).forEach { hex ->
                    ColorDot(hex, selected = hex == state.color) { viewModel.setColor(hex) }
                }
            }

            Text("Icon", style = MaterialTheme.typography.titleSmall)
            val tint = runCatching { Color(android.graphics.Color.parseColor(state.color)) }
                .getOrDefault(MaterialTheme.colorScheme.primary)
            ICON_NAMES.chunked(4).forEach { rowIcons ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowIcons.forEach { iconName ->
                        IconChoice(
                            iconName = iconName,
                            selected = iconName == state.icon,
                            tint = tint,
                            onClick = { viewModel.setIcon(iconName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val fill = 4 - rowIcons.size
                    repeat(fill) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.save() },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (!categoryId.isNullOrBlank()) "Save" else "Add category")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, CircleShape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IconChoice(
    iconName: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) tint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, tint) else null,
        modifier = modifier.height(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = iconName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) tint else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
