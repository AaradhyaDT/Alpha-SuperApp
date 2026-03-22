package com.alpha.features.websearch

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import com.alpha.ui.theme.Exo
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSearchScreen(onBack: () -> Unit, viewModel: WebSearchViewModel = viewModel()) {
    val state   by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Camera temp file ──────────────────────────────────────────────────
    val cameraImageFile = remember {
        File(context.cacheDir, "ws_camera_capture.jpg").also { it.delete() }
    }
    val cameraImageUri: Uri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", cameraImageFile)
    }

    // ── Launchers ─────────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        viewModel.onImageAttached(bytes)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageFile.exists()) {
            viewModel.onImageAttached(cameraImageFile.readBytes())
        }
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) cameraLauncher.launch(cameraImageUri)
    }

    // ── Image source bottom sheet ─────────────────────────────────────────
    var showSourceSheet by remember { mutableStateOf(false) }

    if (showSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showSourceSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Attach image",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = {
                        showSourceSheet = false
                        if (cameraPermissionGranted) cameraLauncher.launch(cameraImageUri)
                        else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Take photo") }
                OutlinedButton(
                    onClick = {
                        showSourceSheet = false
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose from gallery") }
            }
        }
    }

    // ── Screen ────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Search & Summarize") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.result.isNotEmpty() || state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clear() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Query field ───────────────────────────────────────────────
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text(if (state.imageBytes != null) "Ask about the image…" else "What do you want to know?") },
                placeholder = { Text(if (state.imageBytes != null) "e.g. What's in this photo?" else "e.g. Latest AI news this week") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4,
                textStyle = TextStyle(fontFamily = Exo),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                }
            )

            // ── Image thumbnail (shown when attached) ─────────────────────
            state.imageBytes?.let { bytes ->
                val bitmap = remember(bytes) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                bitmap?.let {
                    Box {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        // Remove button
                        IconButton(
                            onClick = { viewModel.onImageAttached(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Remove image",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ── Action row: attach + search ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach button — badge turns primary when image is loaded
                val attachTint = if (state.imageBytes != null)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                IconButton(onClick = { showSourceSheet = true }) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "Attach image",
                        tint = attachTint
                    )
                }

                Button(
                    onClick = { viewModel.search() },
                    enabled = state.query.isNotBlank() && !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isLoading) "Thinking…" else "Search & Summarize")
                }
            }

            // ── Loading ───────────────────────────────────────────────────
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.imageBytes != null) "Analysing image…" else "Searching the web…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── Error ─────────────────────────────────────────────────────
            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "⚠ $it",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Result ────────────────────────────────────────────────────
            if (state.result.isNotEmpty()) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Summary",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(state.result, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
