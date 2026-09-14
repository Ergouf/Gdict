package io.github.gdict.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.R
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.model.Dictionary
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.DictionaryViewModel
import io.github.gdict.viewmodel.SettingsViewModel

@Composable
fun DictionariesScreen(
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val dictionaries by dictionaryViewModel.dictionaries.collectAsStateWithLifecycle(initialValue = emptyList())
    val importing by dictionaryViewModel.importing.collectAsStateWithLifecycle(initialValue = false)
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val diagnosticResult by dictionaryViewModel.diagnosticResult.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var scannedCandidates by remember { mutableStateOf<List<DictFileImporter.DictCandidate>>(emptyList()) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnBackground else GdictColors.OnBackground
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    LaunchedEffect(diagnosticResult) {
        if (diagnosticResult != null) showDiagnostics = true
    }

    Scaffold(
        containerColor = background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dictionaries),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more), tint = secondary)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.scan_import)) },
                            onClick = { showMenu = false; showAddDialog = true },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diagnostics)) },
                            onClick = { showMenu = false; dictionaryViewModel.diagnoseDictionaries() },
                            leadingIcon = { Icon(Icons.Default.BugReport, contentDescription = null) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = CircleShape,
                containerColor = GdictColors.Primary,
                contentColor = GdictColors.OnPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_dictionary))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            if (importing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = GdictColors.Primary)
                Spacer(Modifier.height(12.dp))
            }

            if (dictionaries.isNotEmpty()) {
                Text(
                    text = "${dictionaries.size} dictionaries",
                    style = MaterialTheme.typography.labelLarge,
                    color = secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dictionaries, key = { it.id }) { dictionary ->
                        DictionaryItemCard(
                            dictionary = dictionary,
                            darkMode = darkMode,
                            onToggle = { dictionaryViewModel.toggleDictionary(dictionary) },
                            onRemove = { dictionaryViewModel.removeDictionary(dictionary) }
                        )
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 64.dp)) {
                        Surface(shape = CircleShape, color = surface, modifier = Modifier.size(72.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = GdictColors.Primary, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(stringResource(R.string.no_dictionaries), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = textColor)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.tap_to_add_dictionaries), style = MaterialTheme.typography.bodyMedium, color = secondary)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDictionaryDialog(
            darkMode = darkMode,
            onDismiss = { showAddDialog = false },
            onAdd = { name, path ->
                dictionaryViewModel.addDictionary(name, path)
                showAddDialog = false
            },
            onBatchSelect = { candidates ->
                scannedCandidates = candidates
                showAddDialog = false
                showBatchDialog = true
            },
            scanDirectory = dictionaryViewModel::scanDirectory
        )
    }

    if (showBatchDialog && scannedCandidates.isNotEmpty()) {
        BatchImportDialog(
            candidates = scannedCandidates,
            darkMode = darkMode,
            onDismiss = {
                showBatchDialog = false
                scannedCandidates = emptyList()
            },
            onImport = { selected ->
                dictionaryViewModel.batchImport(selected) {
                    showBatchDialog = false
                    scannedCandidates = emptyList()
                }
            }
        )
    }

    if (showDiagnostics && diagnosticResult != null) {
        DiagnosticsDialog(
            result = diagnosticResult.orEmpty(),
            onDismiss = {
                showDiagnostics = false
                dictionaryViewModel.clearDiagnosticResult()
            }
        )
    }
}

@Composable
fun DictionaryItemCard(
    dictionary: Dictionary,
    darkMode: Boolean = false,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = if (dictionary.isEnabled) GdictColors.Primary else secondary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dictionary.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(dictionary.path, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = dictionary.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = GdictColors.Primary)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove), tint = secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AddDictionaryDialog(
    darkMode: Boolean = false,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onBatchSelect: (List<DictFileImporter.DictCandidate>) -> Unit,
    scanDirectory: (Uri) -> List<DictFileImporter.DictCandidate>
) {
    var name by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            path = uri.toString()
            var displayName = ""
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) displayName = cursor.getString(index).orEmpty()
                    }
                }
            } catch (_: Exception) {}
            if (displayName.isBlank()) displayName = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
            if (name.isBlank()) {
                name = displayName.removeSuffix(".mdx").removeSuffix(".mdd").removeSuffix(".dsl").removeSuffix(".bgl").removeSuffix(".lsa").removeSuffix(".slob")
            }
        }
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            val candidates = scanDirectory(uri)
            if (candidates.isEmpty()) scanError = context.getString(R.string.scan_error_no_dict) else onBatchSelect(candidates)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_dictionary), fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dictionary_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dictionary_path)) },
                    placeholder = { Text(stringResource(R.string.dictionary_path_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(onClick = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) }) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = stringResource(R.string.cd_select_file))
                        }
                    }
                )
                OutlinedButton(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_folder_for_dictionaries))
                }
                if (scanError != null) Text(scanError.orEmpty(), color = GdictColors.CoralAccent, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, path) },
                enabled = name.isNotBlank() && path.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GdictColors.Primary)
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun BatchImportDialog(
    candidates: List<DictFileImporter.DictCandidate>,
    darkMode: Boolean = false,
    onDismiss: () -> Unit,
    onImport: (List<DictFileImporter.DictCandidate>) -> Unit
) {
    var selected by remember { mutableStateOf(candidates.toSet()) }
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_dictionaries_to_import), fontWeight = FontWeight.SemiBold) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(candidates) { candidate ->
                    val checked = candidate in selected
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = if (checked) selected - candidate else selected + candidate
                        }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { value -> selected = if (value) selected + candidate else selected - candidate },
                            colors = CheckboxDefaults.colors(checkedColor = GdictColors.Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(candidate.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(candidate.displayName, style = MaterialTheme.typography.bodySmall, color = secondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(selected.toList()) },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = GdictColors.Primary)
            ) { Text(stringResource(R.string.import_count, selected.size)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun DiagnosticsDialog(result: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.82f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.diagnostic_result), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.diagnostic_clip_label), result))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_copy), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.copy))
                    }
                }
                HorizontalDivider()
                SelectionContainer {
                    Text(
                        text = result,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(vertical = 12.dp)
                    )
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
            }
        }
    }
}
