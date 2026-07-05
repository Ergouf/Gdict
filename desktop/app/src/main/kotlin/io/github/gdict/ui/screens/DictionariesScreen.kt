@file:OptIn(ExperimentalComposeUiApi::class)

package io.github.gdict.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.gdict.core.DictFileImporter
import io.github.gdict.core.model.Dictionary
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.strings.StringResources
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.DictionaryViewModel
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun DictionariesScreen(
    dictionaryViewModel: DictionaryViewModel,
    settingsViewModel: SettingsViewModel,
    strings: StringResources,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dictionaries by dictionaryViewModel.dictionaries.collectAsState()
    val importing by dictionaryViewModel.importing.collectAsState()
    val errorMessage by dictionaryViewModel.errorMessage.collectAsState()
    val diagnosticResult by dictionaryViewModel.diagnosticResult.collectAsState()
    val darkMode by settingsViewModel.darkMode.collectAsState()

    var visible by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var scannedCandidates by remember { mutableStateOf<List<DictFileImporter.DictCandidate>>(emptyList()) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var dictToDelete by remember { mutableStateOf<Dictionary?>(null) }

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

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { windowInfo.containerSize.width.toFloat() }
    val screenHeightPx = with(density) { windowInfo.containerSize.height.toFloat() }

    LaunchedEffect(diagnosticResult) {
        if (diagnosticResult != null) {
            showDiagnostics = true
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    errorMessage?.let { msg ->
        LaunchedEffect(msg) {
            delay(3000)
            dictionaryViewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strings.dictionaries,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
                )
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GdictColors.Primary.copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = strings.addDictionary,
                        tint = GdictColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                AnimatedVisibility(
                    visible = visible && dictionaries.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(dampingRatio = 0.8f)
                    )
                ) {
                    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
                    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .background(glassBg)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GdictColors.Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = GdictColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                strings.dictionariesAdded(dictionaries.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
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
                            DictionaryItemCard(
                                dictionary = dictionary,
                                darkMode = darkMode,
                                onToggle = { dictionaryViewModel.toggleDictionary(dictionary) },
                                onRemove = { dictToDelete = dictionary }
                            )
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
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(GdictColors.PrimarySoft.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LibraryBooks,
                                        contentDescription = null,
                                        tint = GdictColors.PrimarySoft.copy(alpha = 0.6f),
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                                Text(
                                    strings.noDictionariesYet,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
                                )
                                Text(
                                    strings.tapToAddMdxHint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDictionaryDialog(
            darkMode = darkMode,
            strings = strings,
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
            scanDirectory = { dirPath -> dictionaryViewModel.scanDirectory(dirPath) }
        )
    }

    if (showBatchDialog && scannedCandidates.isNotEmpty()) {
        BatchImportDialog(
            strings = strings,
            darkMode = darkMode,
            candidates = scannedCandidates,
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
        val scrollState = rememberScrollState()
        val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
        val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder

        Dialog(
            onDismissRequest = {
                showDiagnostics = false
                dictionaryViewModel.clearDiagnosticResult()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 600.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                    .background(glassBg)
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
                            strings.diagnosticResult,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
                        )
                        TextButton(
                            onClick = {
                                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                clipboard.setContents(StringSelection(diagnosticResult ?: ""), null)
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = strings.copy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.copy)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.CardStroke)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        SelectionContainer {
                            Text(
                                text = diagnosticResult ?: "",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.CardStroke)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showDiagnostics = false
                            dictionaryViewModel.clearDiagnosticResult()
                        }) {
                            Text(strings.close)
                        }
                    }
                }
            }
        }
    }

    dictToDelete?.let { dict ->
        AlertDialog(
            onDismissRequest = { dictToDelete = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text(strings.removeDictionary, fontWeight = FontWeight.Bold) },
            text = { Text(strings.confirmRemoveDictionary(dict.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dictionaryViewModel.removeDictionary(dict)
                        dictToDelete = null
                    }
                ) {
                    Text(strings.remove, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { dictToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun DictionaryItemCard(
    dictionary: Dictionary,
    darkMode: Boolean = false,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface

    val hoverBg = if (darkMode) GdictColors.DarkSubtleHover else Color.White.copy(alpha = 0.85f)
    val containerColor = if (isHovered) hoverBg else glassBg

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(containerColor)
            .hoverable(interactionSource)
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (dictionary.isEnabled)
                                GdictColors.PrimarySoft.copy(alpha = 0.1f)
                            else
                                if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = if (dictionary.isEnabled)
                            GdictColors.PrimarySoft
                        else
                            if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
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
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = dictionary.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GdictColors.OnPrimary,
                        checkedTrackColor = GdictColors.PrimarySoft,
                        uncheckedThumbColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnPrimary,
                        uncheckedTrackColor = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddDictionaryDialog(
    darkMode: Boolean = false,
    strings: StringResources,
    onDismiss: () -> Unit,
    onAdd: (name: String, path: String) -> Unit,
    onBatchSelect: (List<DictFileImporter.DictCandidate>) -> Unit,
    scanDirectory: (String) -> List<DictFileImporter.DictCandidate>
) {
    var selectedTab by remember { mutableStateOf(0) }
    var dictName by remember { mutableStateOf("") }
    var dictPath by remember { mutableStateOf("") }
    var folderPath by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf("") }

    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val titleColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 500.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                .background(glassBg)
                .padding(28.dp)
        ) {
            Column {
                Text(
                    strings.addDictionary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabButton(
                        text = strings.selectFile,
                        icon = Icons.Default.MenuBook,
                        selected = selectedTab == 0,
                        darkMode = darkMode,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = strings.scanImport,
                        icon = Icons.Default.FolderOpen,
                        selected = selectedTab == 1,
                        darkMode = darkMode,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                when (selectedTab) {
                    0 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = dictName,
                                onValueChange = { dictName = it },
                                label = { Text(strings.dictionaryName) },
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GdictColors.Primary,
                                    unfocusedBorderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedLabelColor = GdictColors.Primary,
                                    cursorColor = GdictColors.Primary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = dictPath,
                                    onValueChange = { dictPath = it },
                                    label = { Text(strings.dictionaryPath) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GdictColors.Primary,
                                        unfocusedBorderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedLabelColor = GdictColors.Primary,
                                        cursorColor = GdictColors.Primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val chooser = javax.swing.JFileChooser()
                                    chooser.dialogTitle = strings.selectMdxFile
                                    chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("MDX Dictionary Files", "mdx")
                                    chooser.isMultiSelectionEnabled = false
                                    val result = chooser.showOpenDialog(null)
                                    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                        val selectedFile = chooser.selectedFile
                                        dictPath = selectedFile.absolutePath
                                        if (dictName.isBlank()) {
                                            dictName = selectedFile.nameWithoutExtension
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = strings.selectFile)
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                strings.selectFolderHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = subtitleColor
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = folderPath,
                                    onValueChange = { folderPath = it },
                                    label = { Text(strings.dictionaryFolderPath) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GdictColors.Primary,
                                        unfocusedBorderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedLabelColor = GdictColors.Primary,
                                        cursorColor = GdictColors.Primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    val chooser = javax.swing.JFileChooser()
                                    chooser.dialogTitle = strings.selectDictionaryFolder
                                    chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                                    val result = chooser.showOpenDialog(null)
                                    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                        folderPath = chooser.selectedFile.absolutePath
                                        scanError = ""
                                    }
                                }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = strings.selectDictionaryFolder)
                                }
                            }
                            if (scanError.isNotBlank()) {
                                Text(
                                    scanError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scan Folder button — glass capsule style
                if (selectedTab == 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(glassBg)
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                            .clickable {
                                if (folderPath.isNotBlank()) {
                                    val candidates = scanDirectory(folderPath)
                                    if (candidates.isNotEmpty()) {
                                        onBatchSelect(candidates)
                                    } else {
                                        scanError = strings.noMdxInFolder
                                    }
                                }
                            },
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
                                strings.scanAndImport,
                                color = GdictColors.Primary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    when (selectedTab) {
                        0 -> {
                            TextButton(
                                onClick = {
                                    if (dictName.isNotBlank() && dictPath.isNotBlank()) {
                                        onAdd(dictName, dictPath)
                                    }
                                },
                                enabled = dictName.isNotBlank() && dictPath.isNotBlank()
                            ) {
                                Text(strings.add)
                            }
                        }
                        1 -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    darkMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) GdictColors.Primary
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) GdictColors.OnPrimary else if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) GdictColors.OnPrimary else if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun BatchImportDialog(
    strings: StringResources,
    darkMode: Boolean = false,
    candidates: List<DictFileImporter.DictCandidate>,
    onDismiss: () -> Unit,
    onImport: (List<DictFileImporter.DictCandidate>) -> Unit
) {
    val selected = remember { mutableStateOf(candidates.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface,
        title = { Text(strings.batchImport, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    strings.foundDictsSelectImport(candidates.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates) { candidate ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selected.value = if (candidate in selected.value) {
                                        selected.value - candidate
                                    } else {
                                        selected.value + candidate
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = candidate in selected.value,
                                onCheckedChange = { checked ->
                                    selected.value = if (checked) selected.value + candidate else selected.value - candidate
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GdictColors.Primary,
                                    uncheckedColor = GdictColors.OutlineVariant,
                                    checkmarkColor = GdictColors.OnPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    candidate.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    candidate.filePath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(selected.value.toList()) },
                enabled = selected.value.isNotEmpty()
            ) {
                Text(strings.importCount(selected.value.size))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
