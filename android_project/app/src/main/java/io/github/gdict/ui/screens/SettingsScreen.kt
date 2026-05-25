package io.github.gdict.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.BuildConfig
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.SettingsViewModel

private const val GITHUB_REPO_URL = "https://github.com/iuroc/gdict"

private val GitHubMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "GitHubMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 0.297f)
            curveToRelative(-6.63f, 0f, -12f, 5.373f, -12f, 12f)
            curveToRelative(0f, 5.303f, 3.438f, 9.8f, 8.205f, 11.385f)
            curveToRelative(0.6f, 0.113f, 0.82f, -0.258f, 0.82f, -0.577f)
            curveToRelative(0f, -0.285f, -0.01f, -1.04f, -0.015f, -2.04f)
            curveToRelative(-3.338f, 0.724f, -4.042f, -1.61f, -4.042f, -1.61f)
            curveTo(4.422f, 18.07f, 3.633f, 17.7f, 3.633f, 17.7f)
            curveToRelative(-1.087f, -0.744f, 0.084f, -0.729f, 0.084f, -0.729f)
            curveToRelative(1.205f, 0.084f, 1.838f, 1.236f, 1.838f, 1.236f)
            curveToRelative(1.07f, 1.835f, 2.809f, 1.305f, 3.495f, 0.998f)
            curveToRelative(0.108f, -0.776f, 0.417f, -1.305f, 0.76f, -1.605f)
            curveToRelative(-2.665f, -0.3f, -5.466f, -1.332f, -5.466f, -5.93f)
            curveToRelative(0f, -1.31f, 0.465f, -2.38f, 1.235f, -3.22f)
            curveToRelative(-0.135f, -0.303f, -0.54f, -1.523f, 0.105f, -3.176f)
            curveToRelative(0f, 0f, 1.005f, -0.322f, 3.3f, 1.23f)
            curveToRelative(0.96f, -0.267f, 1.98f, -0.399f, 3f, -0.405f)
            curveToRelative(1.02f, 0.006f, 2.04f, 0.138f, 3f, 0.405f)
            curveToRelative(2.28f, -1.552f, 3.285f, -1.23f, 3.285f, -1.23f)
            curveToRelative(0.645f, 1.653f, 0.24f, 2.873f, 0.12f, 3.176f)
            curveToRelative(0.765f, 0.84f, 1.23f, 1.91f, 1.23f, 3.22f)
            curveToRelative(0f, 4.61f, -2.805f, 5.625f, -5.475f, 5.92f)
            curveToRelative(0.42f, 0.36f, 0.81f, 1.096f, 0.81f, 2.22f)
            curveToRelative(0f, 1.605f, -0.015f, 2.905f, -0.015f, 3.3f)
            curveToRelative(0f, 0.315f, 0.21f, 0.69f, 0.825f, 0.57f)
            curveTo(20.565f, 21.795f, 24f, 17.295f, 24f, 12f)
            curveToRelative(0f, -6.627f, -5.373f, -11.703f, -12f, -11.703f)
            close()
        }
    }.build()
}

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateToDictionaries: () -> Unit = {}
) {
    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle()
    val scanPopup by settingsViewModel.scanPopup.collectAsStateWithLifecycle()
    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.LightGray
    val cardColor = if (darkMode) GdictColors.DarkSurface else Color.White
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.DarkGray
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GdictColors.NavyBlue)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Manage your settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SettingsSection(title = "Dictionaries", cardColor = cardColor, textColor = textColor) {
                SettingsButtonItem(
                    title = "Dictionary Management",
                    description = "Add, remove and manage dictionaries",
                    icon = Icons.Outlined.MenuBook,
                    textColor = textColor,
                    onClick = onNavigateToDictionaries
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSection(title = "Appearance", cardColor = cardColor, textColor = textColor) {
                SettingsSwitchItem(
                    title = "Dark Mode",
                    description = "Toggle dark/light theme",
                    icon = Icons.Outlined.DarkMode,
                    checked = darkMode,
                    textColor = textColor,
                    onCheckedChange = { settingsViewModel.setDarkMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSection(title = "Features", cardColor = cardColor, textColor = textColor) {
                SettingsSwitchItem(
                    title = "Scan Popup",
                    description = "Enable scan popup feature",
                    icon = Icons.Outlined.QrCodeScanner,
                    checked = scanPopup,
                    textColor = textColor,
                    onCheckedChange = { settingsViewModel.setScanPopup(it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSection(title = "About", cardColor = cardColor, textColor = textColor) {
                SettingsButtonItem(
                    title = "Version Info",
                    description = "Gdict v${BuildConfig.VERSION_NAME}",
                    icon = Icons.Outlined.Info,
                    textColor = textColor,
                    onClick = { }
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsButtonItem(
                    title = "Project Repository",
                    description = GITHUB_REPO_URL,
                    icon = GitHubMark,
                    textColor = textColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                var showClearDialog by remember { mutableStateOf(false) }
                SettingsButtonItem(
                    title = "Clear Data",
                    description = "Clear all history and bookmarks",
                    icon = Icons.Outlined.DeleteOutline,
                    textColor = textColor,
                    onClick = { showClearDialog = true }
                )
                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text("Confirm Clear") },
                        text = { Text("Are you sure you want to clear all history and bookmarks?") },
                        confirmButton = {
                            TextButton(onClick = {
                                settingsViewModel.clearAllData()
                                showClearDialog = false
                            }) {
                                Text("Clear", color = GdictColors.CoralAccent)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    cardColor: Color,
    textColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    textColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) GdictColors.TealAccent else GdictColors.MediumGray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GdictColors.MediumGray
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GdictColors.TealAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = GdictColors.LightGray
            )
        )
    }
}

@Composable
private fun SettingsButtonItem(
    title: String,
    description: String,
    icon: ImageVector,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GdictColors.MediumGray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GdictColors.MediumGray
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = GdictColors.MediumGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
