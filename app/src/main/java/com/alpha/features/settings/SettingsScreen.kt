package com.alpha.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpha.BuildConfig
import com.alpha.ui.theme.Exo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    var btNameDraft by remember(state.btDeviceName) { mutableStateOf(state.btDeviceName) }

    // FIX: no longer keyed on state.stabilityFrames to prevent recomposition-driven reset
    var sliderValue by remember { mutableFloatStateOf(state.stabilityFrames.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    // FIX: only sync from ViewModel when user is not actively dragging
    LaunchedEffect(state.stabilityFrames) {
        if (!isDragging) sliderValue = state.stabilityFrames.toFloat()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── APPEARANCE ────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Follow system theme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Automatically match your device's dark/light mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.useSystemTheme,
                        onCheckedChange = { vm.setUseSystemTheme(it) }
                    )
                }

                if (!state.useSystemTheme) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = state.darkMode,
                            onCheckedChange = { vm.setDarkMode(it) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── SBR CONTROL ───────────────────────────────────────────────
            SettingsSectionHeader("SBR Control")

            SettingsCard {
                Text("Bluetooth device name", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Name of the HC-05 as it appears in your paired devices list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = btNameDraft,
                    onValueChange = { btNameDraft = it },
                    label = { Text("Device name", style = TextStyle(fontFamily = Exo)) },
                    placeholder = { Text(AppSettings.DEFAULT_BT_NAME, style = TextStyle(fontFamily = Exo)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = Exo),
                    trailingIcon = {
                        if (btNameDraft != state.btDeviceName) {
                            TextButton(onClick = { vm.setBtDeviceName(btNameDraft) }) {
                                Text("Save")
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text("Gesture stability frames", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "How many identical frames required to confirm a gesture (${sliderValue.toInt()}). " +
                            "Higher = more stable but slower response.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            isDragging = true
                            sliderValue = it
                        },
                        // FIX: set isDragging = false before saving so LaunchedEffect
                        // doesn't fight the final committed value
                        onValueChangeFinished = {
                            isDragging = false
                            vm.setStabilityFrames(sliderValue.toInt().coerceIn(1, 20))
                        },
                        valueRange = 1f..20f,
                        steps = 18,
                        modifier = Modifier.weight(1f)
                    )
                    Text("20", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── ABOUT ─────────────────────────────────────────────────────
            SettingsSectionHeader("About")

            SettingsCard {
                AboutRow("App", "Alpha")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutRow("Version", BuildConfig.VERSION_NAME)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutRow("Developer", "Aaradhya")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutRow("Platform", "Android (Kotlin + Jetpack Compose)")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutRow("AI Backend", "Gemini 2.5 Flash")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutRow("Robot Firmware", "SBR V7")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
