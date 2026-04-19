package com.example.householdledger.ui.transaction

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.householdledger.ui.components.MoneyText
import com.example.householdledger.ui.components.MoneyTone
import com.example.householdledger.ui.theme.EyebrowCaps
import com.example.householdledger.ui.theme.MoneyHero
import com.example.householdledger.ui.theme.PillShape
import com.example.householdledger.util.ParsedTransaction
import kotlinx.coroutines.launch

/**
 * Voice-first transaction entry. Body of a bottom sheet — chrome (drag handle,
 * rounded corners) is supplied by the ModalBottomSheet in MainActivity.
 *
 * Flow:
 *   1. Permission check → request if missing.
 *   2. Start listening: animated waveform, partial transcript shown live.
 *   3. Parsing: brief spinner.
 *   4. Ready: preview card with parsed fields. User hits Confirm to save,
 *      Edit to fall back to the full AddTransactionSheet, or Try again.
 */
@Composable
fun VoiceAddSheet(
    onClose: () -> Unit,
    onSave: (ParsedTransaction) -> Unit,
    onEditManually: (ParsedTransaction?) -> Unit,
    viewModel: VoiceAddViewModel = hiltViewModel(),
    addViewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.start()
    }

    // On first composition: request if needed, otherwise start immediately.
    LaunchedEffect(Unit) {
        if (!viewModel.isAvailable()) {
            // Still show the sheet, but in an error state so the user knows why.
            return@LaunchedEffect
        }
        if (hasPermission) viewModel.start()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "VOICE ENTRY",
            style = EyebrowCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))

        when {
            !viewModel.isAvailable() -> UnavailableState(onClose)
            state.phase == VoicePhase.Listening -> ListeningState(
                partial = state.partialText,
                rms = state.rmsLevel,
                onCancel = {
                    viewModel.cancel()
                    onClose()
                }
            )
            state.phase == VoicePhase.Processing -> ProcessingState(state.partialText)
            state.phase == VoicePhase.Ready && state.parsed != null -> ReadyState(
                parsed = state.parsed!!,
                raw = state.finalText,
                onConfirm = {
                    val p = state.parsed ?: return@ReadyState
                    if (p.amount != null && p.amount > 0) {
                        scope.launch {
                            addViewModel.addTransaction(
                                amount = p.amount,
                                description = p.description,
                                type = p.type,
                                categoryId = p.categoryId,
                                date = p.date.toString()
                            )
                            onSave(p)
                        }
                    } else {
                        // Amount missing — punt to manual edit.
                        onEditManually(p)
                    }
                },
                onEdit = { onEditManually(state.parsed) },
                onRetry = { viewModel.start() }
            )
            state.phase == VoicePhase.Error -> ErrorState(
                message = state.errorMessage ?: "Recognition failed",
                onRetry = { viewModel.start() },
                onCancel = {
                    viewModel.cancel()
                    onClose()
                }
            )
            else -> IdleState()
        }
    }
}

// ─── States ───

@Composable
private fun IdleState() {
    Spacer(Modifier.height(12.dp))
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        MicBadge(active = false, rms = 0f)
    }
    Spacer(Modifier.height(12.dp))
    Text("Preparing microphone…", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ListeningState(partial: String, rms: Float, onCancel: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        MicBadge(active = true, rms = rms)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = partial.ifBlank { "Listening…" },
        style = MaterialTheme.typography.titleMedium,
        color = if (partial.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Try: \"200 for milk\" or \"yesterday 400 groceries\"",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(Modifier.height(20.dp))
    TextButton(onClick = onCancel) { Text("Cancel") }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ProcessingState(partial: String) {
    Spacer(Modifier.height(12.dp))
    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        MicBadge(active = true, rms = 0.3f)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        partial.ifBlank { "Understanding…" },
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ReadyState(
    parsed: ParsedTransaction,
    raw: String,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit
) {
    Spacer(Modifier.height(8.dp))
    Text(
        "\"$raw\"",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(Modifier.height(14.dp))

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                parsed.type.uppercase(),
                style = EyebrowCaps,
                color = when (parsed.type) {
                    "income" -> MaterialTheme.colorScheme.primary
                    "transfer" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                }
            )
            Spacer(Modifier.height(6.dp))
            if (parsed.amount != null) {
                MoneyText(
                    amount = parsed.amount,
                    style = MoneyHero.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    tone = when (parsed.type) {
                        "income" -> MoneyTone.Income
                        "transfer" -> MoneyTone.Neutral
                        else -> MoneyTone.Expense
                    }
                )
            } else {
                Text(
                    "Amount missing",
                    style = MoneyHero.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!parsed.categoryName.isNullOrBlank()) {
                    Chip(parsed.categoryName, tint = MaterialTheme.colorScheme.primaryContainer)
                }
                if (!parsed.personName.isNullOrBlank()) {
                    Chip(parsed.personName, tint = MaterialTheme.colorScheme.secondaryContainer)
                }
                Chip(parsed.date.toString(), tint = MaterialTheme.colorScheme.surfaceVariant)
            }
            if (parsed.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    parsed.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.weight(1f)
        ) { Text("Try again") }
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Outlined.Edit,
                null,
                modifier = Modifier.padding(end = 6.dp).size(18.dp)
            )
            Text("Edit")
        }
    }
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = parsed.amount != null && parsed.amount > 0,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text("Confirm & save", style = MaterialTheme.typography.titleSmall)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        MicBadge(active = false, rms = 0f, errored = true)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        message,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onCancel) { Text("Close") }
        Button(onClick = onRetry) { Text("Try again") }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun UnavailableState(onClose: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(
        "Voice recognition is not available on this device.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onClose) { Text("Close") }
    Spacer(Modifier.height(12.dp))
}

// ─── Visuals ───

@Composable
private fun Chip(text: String, tint: Color) {
    Surface(shape = PillShape, color = tint) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Animated mic badge. When `active`, a halo pulses driven by RMS, and a
 * subtle "breathing" radial gradient runs continuously so it feels alive.
 */
@Composable
private fun MicBadge(active: Boolean, rms: Float, errored: Boolean = false) {
    val haloScale by animateFloatAsState(
        targetValue = if (active) 1.0f + (rms * 0.6f) else 1.0f,
        animationSpec = tween(160),
        label = "haloScale"
    )
    val pulse = rememberInfiniteTransition(label = "pulse")
    val wave by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer halo — scales with RMS
        Canvas(modifier = Modifier.size(140.dp)) {
            val baseColor = if (errored) Color(0xFFD14343) else Color(0xFFE8833A)
            // Continuous pulse ring
            drawCircle(
                color = baseColor.copy(alpha = 0.18f * (1f - wave)),
                radius = (size.minDimension / 2f) * (0.55f + wave * 0.45f)
            )
            // RMS-driven halo
            drawCircle(
                color = baseColor.copy(alpha = 0.25f),
                radius = (size.minDimension / 2f) * 0.55f * haloScale
            )
            // Soft inner glow
            drawCircle(
                color = baseColor.copy(alpha = 0.12f),
                radius = (size.minDimension / 2f) * 0.40f
            )
        }
        // Mic button
        Surface(
            shape = CircleShape,
            color = if (errored) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = null,
                    tint = if (errored) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
