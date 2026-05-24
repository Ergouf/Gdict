package io.github.gdict.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import io.github.gdict.data.Dictionary
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun DictionariesScreen(
    viewModel: AppViewModel = viewModel()
) {
    val dictionaries by viewModel.dictionaries.collectAsStateWithLifecycle(initialValue = emptyList())
    val importing by viewModel.importing.collectAsStateWithLifecycle(initialValue = false)
    var visible by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var scannedCandidates by remember { mutableStateOf<List<DictFileImporter.DictCandidate>>(emptyList()) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val vmDiagnosticResult by viewModel.diagnosticResult.collectAsStateWithLifecycle()
    LaunchedEffect(vmDiagnosticResult) {
        if (vmDiagnosticResult != null) {
            showDiagnostics = true
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = spring(dampingRatio = 0.8f)
                ) + fadeIn(animationSpec = tween(300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GdictColors.NavyBlue)
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Dictionaries",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Scan Import") },
                                    onClick = {
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Diagnostics") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.diagnoseDictionaries()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.BugReport, contentDescription = null)
                                    }
                                )
                            }
                        }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = 0.8f)
                ) + fadeIn(animationSpec = tween(300))
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GdictColors.TealAccent,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加词典",
                        tint = Color.White
                    )
                }
            }
        },
        containerColor = GdictColors.LightGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            AnimatedVisibility(
                visible = visible && dictionaries.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = GdictColors.NavyBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GdictColors.NavyBlue.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = GdictColors.NavyBlue,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text(
                            "${dictionaries.size} dictionaries added",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GdictColors.NavyBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dictionaries.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dictionaries, key = { it.id }) { dictionary ->
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                                initialOffsetY = { 30 },
                                animationSpec = spring(dampingRatio = 0.8f)
                            )
                        ) {
                            DictionaryItemCard(
                                dictionary = dictionary,
                                onToggle = { viewModel.toggleDictionary(dictionary) },
                                onRemove = { viewModel.removeDictionary(dictionary) }
                            )
                        }
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(400)) + scaleIn(
                        initialScale = 0.9f,
                        animationSpec = spring(dampingRatio = 0.8f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GdictColors.NavyBlue.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryBooks,
                                    contentDescription = null,
                                    tint = GdictColors.NavyBlue,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(24.dp)
                                )
                            }
                            Text(
                                "No dictionaries",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = GdictColors.DarkGray
                            )
                            Text(
                                "Tap the button to add dictionaries",
                                style = MaterialTheme.typography.bodyLarge,
                                color = GdictColors.MediumGray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDictionaryDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, path ->
                viewModel.addDictionary(name, path)
                showAddDialog = false
            },
            onBatchSelect = { candidates ->
                scannedCandidates = candidates
                showAddDialog = false
                showBatchDialog = true
            },
            scanDirectory = { uri -> viewModel.scanDirectory(uri) }
        )
    }

    if (showBatchDialog && scannedCandidates.isNotEmpty()) {
        BatchImportDialog(
            candidates = scannedCandidates,
            onDismiss = {
                showBatchDialog = false
                scannedCandidates = emptyList()
            },
            onImport = { selected ->
                viewModel.batchImport(selected) {
                    showBatchDialog = false
                    scannedCandidates = emptyList()
                }
            }
        )
    }

    if (showDiagnostics && vmDiagnosticResult != null) {
        val scrollState = rememberScrollState()
        val context = LocalContext.current

        Dialog(
            onDismissRequest = {
                showDiagnostics = false
                viewModel.clearDiagnosticResult()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "词典诊断结果",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("诊断结果", vmDiagnosticResult)
                                clipboard.setPrimaryClip(clip)
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "复制",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        SelectionContainer {
                            Text(
                                text = vmDiagnosticResult ?: "",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showDiagnostics = false
                            viewModel.clearDiagnosticResult()
                        }) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DictionaryItemCard(
    dictionary: Dictionary,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    LaunchedEffect(Unit) {
        scale = 0.95f
        delay(50)
        scale = 1f
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (dictionary.isEnabled)
                        GdictColors.NavyBlue.copy(alpha = 0.1f)
                    else
                        GdictColors.LightGray
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = if (dictionary.isEnabled)
                            GdictColors.NavyBlue
                        else
                            GdictColors.MediumGray,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dictionary.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = GdictColors.DarkGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dictionary.path.take(40),
                        style = MaterialTheme.typography.labelSmall,
                        color = GdictColors.MediumGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = dictionary.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = GdictColors.TealAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = GdictColors.LightGray
                    )
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "移除",
                        tint = GdictColors.MediumGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddDictionaryDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onBatchSelect: (List<DictFileImporter.DictCandidate>) -> Unit,
    scanDirectory: (Uri) -> List<DictFileImporter.DictCandidate>
) {
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            path = uri.toString()
            var displayName = ""
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (colIndex >= 0) {
                            displayName = cursor.getString(colIndex) ?: ""
                        }
                    }
                }
            } catch (_: Exception) {}
            if (displayName.isBlank()) {
                displayName = uri.lastPathSegment?.substringAfterLast('/') ?: ""
            }
            if (name.isEmpty()) {
                name = displayName.removeSuffix(".mdx").removeSuffix(".mdd")
                    .removeSuffix(".dsl").removeSuffix(".bgl")
                    .removeSuffix(".lsa").removeSuffix(".slob")
            }
        }
    }

    val scanFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val candidates = scanDirectory(uri)
            if (candidates.isNotEmpty()) {
                onBatchSelect(candidates)
            } else {
                scanError = "未在此目录中发现支持的词典文件（.mdx/.dsl/.bgl等）"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Add Dictionary",
                fontWeight = FontWeight.Bold,
                color = GdictColors.DarkGray
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dictionary Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GdictColors.NavyBlue,
                        unfocusedBorderColor = GdictColors.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = GdictColors.NavyBlue
                    )
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dictionary Path") },
                    placeholder = { Text("Select file or scan folder...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GdictColors.NavyBlue,
                        unfocusedBorderColor = GdictColors.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = GdictColors.NavyBlue
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        }) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = "选择文件",
                                tint = GdictColors.NavyBlue
                            )
                        }
                    }
                )

                OutlinedButton(
                    onClick = { scanFolderLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GdictColors.NavyBlue
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan folder for dictionaries")
                }

                if (scanError != null) {
                    Text(
                        text = scanError ?: "",
                        color = GdictColors.CoralAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && path.isNotEmpty()) {
                        onAdd(name, path)
                    }
                },
                enabled = name.isNotEmpty() && path.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GdictColors.TealAccent,
                    disabledContainerColor = GdictColors.TealAccent.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    "Add",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = GdictColors.MediumGray
                )
            }
        }
    )
}

@Composable
fun BatchImportDialog(
    candidates: List<DictFileImporter.DictCandidate>,
    onDismiss: () -> Unit,
    onImport: (List<DictFileImporter.DictCandidate>) -> Unit
) {
    var selected by remember { mutableStateOf(candidates.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Select dictionaries to import",
                fontWeight = FontWeight.Bold,
                color = GdictColors.DarkGray
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(candidates) { candidate ->
                    val isChecked = candidate in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selected = if (isChecked) selected - candidate
                                else selected + candidate
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + candidate
                                else selected - candidate
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GdictColors.TealAccent,
                                uncheckedColor = GdictColors.MediumGray
                            )
                        )
                        Column {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = GdictColors.DarkGray
                            )
                            Text(
                                text = candidate.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = GdictColors.MediumGray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(selected.toList()) },
                enabled = selected.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GdictColors.TealAccent,
                    disabledContainerColor = GdictColors.TealAccent.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    "Import ${selected.size}",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = GdictColors.MediumGray
                )
            }
        }
    )
}
