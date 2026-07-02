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
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import io.github.gdict.R
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.DictionaryManager
import io.github.gdict.core.model.Dictionary
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.DictionaryViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun DictionariesScreen(
    dictionaryViewModel: DictionaryViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val dictionaries by dictionaryViewModel.dictionaries.collectAsStateWithLifecycle(initialValue = emptyList())
    val importing by dictionaryViewModel.importing.collectAsStateWithLifecycle(initialValue = false)
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val dialogBg = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val cardBg = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    var visible by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var scannedCandidates by remember { mutableStateOf<List<DictFileImporter.DictCandidate>>(emptyList()) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val vmDiagnosticResult by dictionaryViewModel.diagnosticResult.collectAsStateWithLifecycle()
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
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.dictionaries),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
                    )
                    Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GdictColors.SurfaceVariant)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.cd_more),
                                    tint = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (darkMode) GdictColors.DarkSurface else GdictColors.Surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.scan_import)) },
                                    onClick = {
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.diagnostics)) },
                                    onClick = {
                                        showMenu = false
                                        dictionaryViewModel.diagnoseDictionaries()
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
                    containerColor = GdictColors.PrimarySoft,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_dictionary),
                        tint = GdictColors.OnPrimary
                    )
                }
            }
        },
        containerColor = if (darkMode) GdictColors.DarkBackground else GdictColors.BlueBackgroundTop
    ) { paddingValues ->
        val bgGradient = if (darkMode) {
            Brush.verticalGradient(
                0.0f to GdictColors.DarkBackground,
                1.0f to GdictColors.DarkSurfaceVariant
            )
        } else {
            Brush.verticalGradient(
                0.0f to Color(0xFFDCEBFF),
                0.6f to Color(0xFFEDF4FF),
                1.0f to Color(0xFFFFFFFF)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
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
                        containerColor = GdictColors.PrimarySoft.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GdictColors.PrimarySoft.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = GdictColors.PrimarySoft,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text(
                            "${dictionaries.size} dictionaries added",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GdictColors.PrimarySoft
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
                                darkMode = darkMode,
                                onToggle = { dictionaryViewModel.toggleDictionary(dictionary) },
                            onRemove = { dictionaryViewModel.removeDictionary(dictionary) }
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
                                color = GdictColors.PrimarySoft.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryBooks,
                                    contentDescription = null,
                                    tint = GdictColors.PrimarySoft,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(24.dp)
                                )
                            }
                            Text(
                                stringResource(R.string.no_dictionaries),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
                            )
                            Text(
                                stringResource(R.string.tap_to_add_dictionaries),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
                            )
                        }
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
            scanDirectory = { uri -> dictionaryViewModel.scanDirectory(uri) }
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

    if (showDiagnostics && vmDiagnosticResult != null) {
        val scrollState = rememberScrollState()
        val context = LocalContext.current

        Dialog(
            onDismissRequest = {
                showDiagnostics = false
                dictionaryViewModel.clearDiagnosticResult()
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
                            stringResource(R.string.diagnostic_result),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipLabel = context.getString(R.string.diagnostic_clip_label)
                                val clip = ClipData.newPlainText(clipLabel, vmDiagnosticResult)
                                clipboard.setPrimaryClip(clip)
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.cd_copy),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.copy))
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
                            dictionaryViewModel.clearDiagnosticResult()
                        }) {
                            Text(stringResource(R.string.close))
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
    darkMode: Boolean = false,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    val cardBg = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val surfaceBg = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
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
                        GdictColors.PrimarySoft.copy(alpha = 0.1f)
                    else
                        GdictColors.SurfaceVariant
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = if (dictionary.isEnabled)
                            GdictColors.PrimarySoft
                        else
                            GdictColors.OnSurfaceVariant,
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
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dictionary.path.take(40),
                        style = MaterialTheme.typography.labelSmall,
                        color = GdictColors.OnSurfaceVariant,
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
                        checkedThumbColor = GdictColors.OnPrimary,
                        checkedTrackColor = GdictColors.PrimarySoft,
                        uncheckedThumbColor = GdictColors.OnPrimary,
                        uncheckedTrackColor = GdictColors.SurfaceVariant
                    )
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove),
                        tint = GdictColors.OnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
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

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val titleColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val fieldBg = if (darkMode) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f)
    val fieldBorderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

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
                scanError = context.getString(R.string.scan_error_no_dict)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(12.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                .background(glassBg)
                .padding(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GdictColors.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryAdd,
                                contentDescription = null,
                                tint = GdictColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            stringResource(R.string.add_dictionary),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (darkMode) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = GdictColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dictionary_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GdictColors.Primary,
                        unfocusedBorderColor = fieldBorderColor,
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        cursorColor = GdictColors.Primary,
                        focusedLabelColor = GdictColors.Primary,
                        unfocusedLabelColor = subtitleColor
                    )
                )

                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dictionary_path)) },
                    placeholder = { Text(stringResource(R.string.dictionary_path_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GdictColors.Primary,
                        unfocusedBorderColor = fieldBorderColor,
                        focusedContainerColor = fieldBg,
                        unfocusedContainerColor = fieldBg,
                        cursorColor = GdictColors.Primary,
                        focusedLabelColor = GdictColors.Primary,
                        unfocusedLabelColor = subtitleColor
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GdictColors.Primary.copy(alpha = 0.10f))
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = stringResource(R.string.cd_select_file),
                                tint = GdictColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GdictColors.Primary.copy(alpha = 0.10f))
                        .border(1.dp, GdictColors.Primary.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
                        .clickable { scanFolderLauncher.launch(null) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = GdictColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.scan_folder_for_dictionaries),
                            color = GdictColors.Primary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                if (scanError != null) {
                    Text(
                        text = scanError ?: "",
                        color = GdictColors.CoralAccent,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (darkMode) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.60f))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            color = subtitleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val canSubmit = name.isNotEmpty() && path.isNotEmpty()
                    Button(
                        onClick = { onAdd(name, path) },
                        enabled = canSubmit,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GdictColors.Primary,
                            contentColor = GdictColors.OnPrimary,
                            disabledContainerColor = GdictColors.Primary.copy(alpha = 0.35f),
                            disabledContentColor = GdictColors.OnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        Text(
                            stringResource(R.string.add),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchImportDialog(
    candidates: List<DictFileImporter.DictCandidate>,
    darkMode: Boolean = false,
    onDismiss: () -> Unit,
    onImport: (List<DictFileImporter.DictCandidate>) -> Unit
) {
    var selected by remember { mutableStateOf(candidates.toSet()) }
    val dialogBg = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val titleColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Text(
                    stringResource(R.string.select_dictionaries_to_import),
                fontWeight = FontWeight.Bold,
                color = titleColor
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
                                checkedColor = GdictColors.PrimarySoft,
                                uncheckedColor = GdictColors.OnSurfaceVariant
                            )
                        )
                        Column {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
                            )
                            Text(
                                text = candidate.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = GdictColors.OnSurfaceVariant
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
                    containerColor = GdictColors.PrimarySoft,
                    disabledContainerColor = GdictColors.PrimarySoft.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    stringResource(R.string.import_count, selected.size),
                    color = GdictColors.OnPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel),
                    color = GdictColors.OnSurfaceVariant
                )
            }
        }
    )
}
