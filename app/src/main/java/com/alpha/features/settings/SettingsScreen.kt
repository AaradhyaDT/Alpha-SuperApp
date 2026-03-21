package com.alpha.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    // Local draft for text fields (committed on Done/focus-leave)
    var btNameDraft by remember(state.btDeviceName) { mutableStateOf(state.btDeviceName) }

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
                // Use system theme toggle
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

                // Manual dark mode — only shown when system theme is off
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
                // BT device name
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
                    label = { Text("Device name") },
                    placeholder = { Text(AppSettings.DEFAULT_BT_NAME) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (btNameDraft != state.btDeviceName) {
                            TextButton(onClick = { vm.setBtDeviceName(btNameDraft) }) {
                                Text("Save")
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Stability frames slider
                Text("Gesture stability frames", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "How many identical frames required to confirm a gesture (${state.stabilityFrames}). " +
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
                        value = state.stabilityFrames.toFloat(),
                        onValueChange = { vm.setStabilityFrames(it.toInt()) },
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
                AboutRow("Version", "1.0")
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
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
