package com.alpha.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alpha.R

data class AppModule(val id: String, val title: String, val description: String, val emoji: String)

val ALL_MODULES = listOf(
    AppModule("web_search", "Web Search & Summarize", "Ask anything — Gemini searches the web and returns a clean summary.", "🔍"),
    AppModule("calculator", "Calculator", "Clean, fast calculator for everyday math.", "🧮"),
    AppModule("sbr_control", "SBR Control", "Gesture-controlled self-balancing robot via Bluetooth.", "🤖"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onModuleClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_alpha_logo),
                            contentDescription = "Alpha Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Alpha", fontWeight = FontWeight.Bold)
                            Text(
                                "Your personal AI toolkit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        val icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.Nightlight
                        Icon(
                            imageVector = icon,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(ALL_MODULES) { module ->
                Card(
                    onClick = { onModuleClick(module.id) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(module.emoji, style = MaterialTheme.typography.headlineMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(module.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                module.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
