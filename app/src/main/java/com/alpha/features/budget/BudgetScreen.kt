package com.alpha.features.budget

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alpha.features.budget.models.*
import com.alpha.ui.theme.Exo
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    vm: BudgetViewModel = viewModel()
) {
    val state         by vm.uiState.collectAsStateWithLifecycle()
    val importPreview by vm.importPreview.collectAsStateWithLifecycle()
    var selectedTab   by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Transactions", "Limits")
    var showAddSheet  by remember { mutableStateOf(false) }

    // XLS file picker
    val xlsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.previewXlsImport(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Import XLS button
                    IconButton(onClick = { xlsLauncher.launch("application/vnd.ms-excel") }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import eSewa XLS")
                    }
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { vm.forceDriveSync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync to Drive")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab != 2) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MonthSelector(state.selectedMonthEpochMs, vm::previousMonth, vm::nextMonth)
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(title) }
                    )
                }
            }
            // Sync error banner
            state.syncError?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        "⚠ $it",
                        modifier = Modifier.padding(12.dp),
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            }
            when (selectedTab) {
                0 -> OverviewTab(state, vm)
                1 -> TransactionsTab(state, vm)
                2 -> LimitsTab(state, vm)
            }
        }
    }

    // ── XLS import duplicate dialog ───────────────────────────────────────
    importPreview?.let { preview ->
        DuplicateImportDialog(
            preview   = preview,
            onSkip    = { vm.confirmImportSkipDuplicates() },
            onOverwrite = { vm.confirmImportOverwriteDuplicates() },
            onDismiss = { vm.dismissImportPreview() }
        )
    }

    if (showAddSheet) {
        AddTransactionSheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { amount, category, merchant, note, dateMs, photoBytes ->
                vm.addManualTransaction(amount, category, merchant, note, dateMs, photoBytes)
                showAddSheet = false
            }
        )
    }
}

// ── Duplicate Import Dialog ───────────────────────────────────────────────────

@Composable
private fun DuplicateImportDialog(
    preview: XlsImportPreview,
    onSkip: () -> Unit,
    onOverwrite: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Summary") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("✅ ${preview.newTransactions.size} new transactions found")
                if (preview.duplicates.isNotEmpty()) {
                    Text(
                        "⚠️ ${preview.duplicates.size} duplicate(s) already in your records",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (preview.skippedFailed > 0)
                    Text("⏭ ${preview.skippedFailed} failed transactions skipped",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (preview.skippedCredits > 0)
                    Text("⏭ ${preview.skippedCredits} incoming credits skipped",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (preview.duplicates.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "How should duplicates be handled?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            if (preview.duplicates.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onSkip) { Text("Skip duplicates") }
                    Button(onClick = onOverwrite) { Text("Overwrite") }
                }
            } else {
                Button(onClick = onSkip) { Text("Import") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Overview Tab ──────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(state: BudgetState, vm: BudgetViewModel) {
    val spent      = state.totalSpentThisMonth()
    val limit      = state.totalBudgetLimit()
    val remaining  = state.remainingBudget()
    val byCategory = state.spentByCategory()

    var editingNetWorth by remember { mutableStateOf(false) }
    var netWorthDraft   by remember(state.netWorthRs) { mutableStateOf(state.netWorthRs.toInt().toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("NET WORTH", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    IconButton(
                        onClick  = { editingNetWorth = !editingNetWorth },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            if (editingNetWorth) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit net worth",
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (editingNetWorth) {
                    OutlinedTextField(
                        value           = netWorthDraft,
                        onValueChange   = { netWorthDraft = it },
                        prefix          = { Text("Rs. ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        textStyle       = TextStyle(fontFamily = Exo, fontSize = 20.sp),
                        modifier        = Modifier.fillMaxWidth(),
                        trailingIcon    = {
                            TextButton(onClick = {
                                netWorthDraft.toDoubleOrNull()?.let { vm.setNetWorth(it) }
                                editingNetWorth = false
                            }) { Text("Save") }
                        }
                    )
                } else {
                    Text(
                        "Rs. ${"%,.0f".format(state.netWorthRs)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "After this month's spending: Rs. ${"%,.0f".format(state.netWorthRs - spent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryChip("Spent",     "Rs. ${"%,.0f".format(spent)}",     Modifier.weight(1f))
            SummaryChip("Remaining", "Rs. ${"%,.0f".format(remaining)}", Modifier.weight(1f),
                warn = limit > 0 && remaining < 0)
        }

        Text("BY CATEGORY", style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary)

        TransactionCategory.entries.forEach { cat ->
            val catSpent = byCategory[cat] ?: 0.0
            val catLimit = state.categoryBudgets.find { it.category == cat }?.limitRs ?: 0.0
            if (catSpent > 0 || catLimit > 0) CategoryBar(cat, catSpent, catLimit)
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, modifier: Modifier = Modifier, warn: Boolean = false) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (warn) MaterialTheme.colorScheme.errorContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryBar(cat: TransactionCategory, spent: Double, limit: Double) {
    val progress   = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val overBudget = limit > 0 && spent > limit
    val barColor   = when {
        overBudget       -> MaterialTheme.colorScheme.error
        progress > 0.75f -> MaterialTheme.colorScheme.tertiary
        else             -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${cat.emoji()} ${cat.displayName()}", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (limit > 0) "Rs. ${"%,.0f".format(spent)} / ${"%,.0f".format(limit)}"
                else           "Rs. ${"%,.0f".format(spent)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (overBudget) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (limit > 0) {
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// ── Transactions Tab ──────────────────────────────────────────────────────────

@Composable
private fun TransactionsTab(state: BudgetState, vm: BudgetViewModel) {
    val txns    = state.transactionsThisMonth().sortedByDescending { it.dateEpochMs }
    val dateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    if (txns.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(txns, key = { it.id }) { txn ->
            TransactionRow(txn, dateFmt, onDelete = { vm.deleteTransaction(txn.id) })
        }
    }
}

@Composable
private fun TransactionRow(txn: Transaction, dateFmt: SimpleDateFormat, onDelete: () -> Unit) {
    var showConfirm   by remember { mutableStateOf(false) }
    var showFullPhoto by remember { mutableStateOf(false) }

    val bitmap = remember(txn.billPhotoPath) {
        txn.billPhotoPath?.let { BitmapFactory.decodeFile(it) }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier              = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.weight(1f)
                ) {
                    Text(txn.category.emoji(), fontSize = 24.sp)
                    Column {
                        Text(
                            txn.merchantName.ifEmpty { txn.category.displayName() },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                dateFmt.format(Date(txn.dateEpochMs)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("·", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                when (txn.source) {
                                    TransactionSource.ESEWA  -> "eSewa"
                                    TransactionSource.MANUAL -> "Manual"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Rs. ${"%,.0f".format(txn.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Bill photo thumbnail
            if (bitmap != null) {
                Image(
                    bitmap             = bitmap.asImageBitmap(),
                    contentDescription = "Bill photo",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullPhoto = true }
                )
            }
        }
    }

    if (showFullPhoto && bitmap != null) {
        Dialog(
            onDismissRequest = { showFullPhoto = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier         = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { showFullPhoto = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap             = bitmap.asImageBitmap(),
                    contentDescription = "Bill photo full view",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick  = { showFullPhoto = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title            = { Text("Delete transaction?") },
            text             = { Text("This cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showConfirm = false }) { Text("Delete") }
            },
            dismissButton    = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Limits Tab ────────────────────────────────────────────────────────────────

@Composable
private fun LimitsTab(state: BudgetState, vm: BudgetViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Set monthly spending limits per category. Leave 0 for no limit.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        TransactionCategory.entries.forEach { cat ->
            val current = state.categoryBudgets.find { it.category == cat }?.limitRs ?: 0.0
            LimitRow(cat, current) { vm.setCategoryLimit(cat, it) }
        }
    }
}

@Composable
private fun LimitRow(cat: TransactionCategory, currentLimit: Double, onSave: (Double) -> Unit) {
    var draft   by remember(currentLimit) { mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "") }
    var editing by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text("${cat.emoji()} ${cat.displayName()}", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            if (editing) {
                OutlinedTextField(
                    value           = draft,
                    onValueChange   = { draft = it },
                    prefix          = { Text("Rs. ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine      = true,
                    textStyle       = TextStyle(fontFamily = Exo, fontSize = 14.sp),
                    modifier        = Modifier.width(150.dp),
                    trailingIcon    = {
                        TextButton(onClick = { onSave(draft.toDoubleOrNull() ?: 0.0); editing = false }) {
                            Text("Save")
                        }
                    }
                )
            } else {
                Text(
                    if (currentLimit > 0) "Rs. ${"%,.0f".format(currentLimit)}" else "No limit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { editing = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Add Transaction Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onConfirm: (Double, TransactionCategory, String, String, Long, ByteArray?) -> Unit
) {
    val context = LocalContext.current

    var amount       by remember { mutableStateOf("") }
    var merchant     by remember { mutableStateOf("") }
    var note         by remember { mutableStateOf("") }
    var category     by remember { mutableStateOf(TransactionCategory.OTHER) }
    var expanded     by remember { mutableStateOf(false) }
    var photoBytes   by remember { mutableStateOf<ByteArray?>(null) }
    var showSrcSheet by remember { mutableStateOf(false) }

    val cameraFile = remember { File(context.cacheDir, "budget_bill_capture.jpg").also { it.delete() } }
    val cameraUri: Uri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", cameraFile)
    }

    var cameraPermGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        photoBytes = context.contentResolver.openInputStream(uri)?.readBytes()
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraFile.exists()) photoBytes = cameraFile.readBytes()
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermGranted = granted
        if (granted) cameraLauncher.launch(cameraUri)
    }

    if (showSrcSheet) {
        ModalBottomSheet(onDismissRequest = { showSrcSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Attach bill photo", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    showSrcSheet = false
                    if (cameraPermGranted) cameraLauncher.launch(cameraUri)
                    else permLauncher.launch(Manifest.permission.CAMERA)
                }, modifier = Modifier.fillMaxWidth()) { Text("Take photo") }
                OutlinedButton(onClick = { showSrcSheet = false; galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()) { Text("Choose from gallery") }
                if (photoBytes != null) {
                    TextButton(onClick = { photoBytes = null; showSrcSheet = false },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Remove photo", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(value = amount, onValueChange = { amount = it },
                label = { Text("Amount") }, prefix = { Text("Rs. ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, textStyle = TextStyle(fontFamily = Exo),
                modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = merchant, onValueChange = { merchant = it },
                label = { Text("Merchant / Place") }, singleLine = true,
                textStyle = TextStyle(fontFamily = Exo), modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") }, singleLine = true,
                textStyle = TextStyle(fontFamily = Exo), modifier = Modifier.fillMaxWidth())

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = "${category.emoji()} ${category.displayName()}",
                    onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    TransactionCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text("${cat.emoji()} ${cat.displayName()}") },
                            onClick = { category = cat; expanded = false }
                        )
                    }
                }
            }

            val previewBitmap = remember(photoBytes) {
                photoBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            }

            if (previewBitmap != null) {
                Box {
                    Image(bitmap = previewBitmap.asImageBitmap(), contentDescription = "Bill preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(8.dp)))
                    IconButton(onClick = { photoBytes = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else {
                OutlinedButton(onClick = { showSrcSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Attach bill photo")
                }
            }

            Button(
                onClick  = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    onConfirm(amt, category, merchant, note, System.currentTimeMillis(), photoBytes)
                },
                enabled  = amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add") }
        }
    }
}

// ── Month Selector ────────────────────────────────────────────────────────────

@Composable
private fun MonthSelector(epochMs: Long, onPrev: () -> Unit, onNext: () -> Unit) {
    val label = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMs))
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}
