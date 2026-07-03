package io.github.gdict.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gdict.BuildConfig
import io.github.gdict.R
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.util.LocaleHelper
import io.github.gdict.viewmodel.SettingsViewModel

private const val GITHUB_REPO_URL = "https://github.com/Ergouf/Gdict"

private val GitHubMark: ImageVector by lazy {
    ImageVector.Builder(
        name = "GitHubMark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 官方 GitHub Octocat mark 路径（24x24 viewBox）
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 0.297f)
            curveTo(5.37f, 0.297f, 0f, 5.67f, 0f, 12f)
            curveTo(0f, 17.303f, 3.438f, 21.8f, 8.205f, 23.385f)
            curveTo(8.805f, 23.498f, 9.025f, 23.127f, 9.025f, 22.808f)
            curveTo(9.025f, 22.523f, 9.015f, 21.768f, 9.01f, 20.768f)
            curveTo(5.672f, 21.492f, 4.968f, 19.159f, 4.968f, 19.159f)
            curveTo(4.421f, 17.77f, 3.634f, 17.4f, 3.634f, 17.4f)
            curveTo(2.546f, 16.656f, 3.718f, 16.671f, 3.718f, 16.671f)
            curveTo(4.922f, 16.756f, 5.555f, 17.908f, 5.555f, 17.908f)
            curveTo(6.626f, 19.766f, 8.303f, 19.228f, 8.964f, 18.915f)
            curveTo(9.069f, 18.135f, 9.386f, 17.597f, 9.735f, 17.29f)
            curveTo(7.062f, 16.979f, 4.247f, 15.946f, 4.247f, 11.333f)
            curveTo(4.247f, 10.018f, 4.714f, 8.941f, 5.575f, 8.093f)
            curveTo(5.455f, 7.782f, 5.044f, 6.552f, 5.687f, 4.887f)
            curveTo(5.687f, 4.887f, 6.701f, 4.562f, 8.997f, 6.13f)
            curveTo(9.953f, 5.864f, 10.98f, 5.731f, 12f, 5.726f)
            curveTo(13.02f, 5.731f, 14.047f, 5.864f, 15.003f, 6.13f)
            curveTo(17.299f, 4.562f, 18.313f, 4.887f, 18.313f, 4.887f)
            curveTo(18.956f, 6.552f, 18.545f, 7.782f, 18.425f, 8.093f)
            curveTo(19.286f, 8.941f, 19.753f, 10.018f, 19.753f, 11.333f)
            curveTo(19.753f, 15.956f, 16.933f, 16.976f, 14.253f, 17.28f)
            curveTo(14.693f, 17.66f, 15.085f, 18.41f, 15.085f, 19.547f)
            curveTo(15.085f, 21.197f, 15.07f, 22.527f, 15.07f, 22.937f)
            curveTo(15.07f, 23.252f, 15.285f, 23.627f, 15.895f, 23.512f)
            curveTo(20.566f, 21.833f, 24f, 17.33f, 24f, 12f)
            curveTo(24f, 5.67f, 18.63f, 0.297f, 12f, 0.297f)
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
    val currentLanguage by settingsViewModel.language.collectAsStateWithLifecycle()
    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.BlueBackgroundTop
    // 真实渐变：顶部明显蓝 → 中段浅蓝 → 底部接近白，对比度足够在浅色背景上能看出来
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
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHero(
            darkMode = darkMode,
            textColor = textColor,
            subtitleColor = subtitleColor
        )

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SettingsSection(
                title = stringResource(R.string.section_dictionaries),
                darkMode = darkMode,
                textColor = textColor
            ) {
                SettingsButtonItem(
                    title = stringResource(R.string.dictionary_management),
                    description = stringResource(R.string.dictionary_management_desc),
                    icon = Icons.Outlined.MenuBook,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = onNavigateToDictionaries
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(
                title = stringResource(R.string.section_appearance),
                darkMode = darkMode,
                textColor = textColor
            ) {
                SettingsSwitchItem(
                    title = stringResource(R.string.dark_mode),
                    description = stringResource(R.string.dark_mode_desc),
                    icon = Icons.Outlined.DarkMode,
                    checked = darkMode,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onCheckedChange = { settingsViewModel.setDarkMode(it) }
                )
                SettingsButtonItem(
                    title = stringResource(R.string.language),
                    description = stringResource(R.string.language_desc),
                    icon = Icons.Outlined.Language,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { showLanguageDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(
                title = stringResource(R.string.section_features),
                darkMode = darkMode,
                textColor = textColor
            ) {
                SettingsSwitchItem(
                    title = stringResource(R.string.scan_popup),
                    description = stringResource(R.string.scan_popup_desc),
                    icon = Icons.Outlined.QrCodeScanner,
                    checked = scanPopup,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onCheckedChange = { settingsViewModel.setScanPopup(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(
                title = stringResource(R.string.section_about),
                darkMode = darkMode,
                textColor = textColor
            ) {
                SettingsButtonItem(
                    title = stringResource(R.string.version_info),
                    description = "Gdict v${BuildConfig.VERSION_NAME}",
                    icon = Icons.Outlined.Info,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    badgeText = "v${BuildConfig.VERSION_NAME}",
                    onClick = { }
                )
                SettingsButtonItem(
                    title = stringResource(R.string.project_repository),
                    description = GITHUB_REPO_URL,
                    icon = GitHubMark,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL))
                        context.startActivity(intent)
                    }
                )
                var showClearDialog by remember { mutableStateOf(false) }
                SettingsButtonItem(
                    title = stringResource(R.string.clear_data),
                    description = stringResource(R.string.clear_data_desc),
                    icon = Icons.Outlined.DeleteOutline,
                    darkMode = darkMode,
                    textColor = textColor,
                    subtitleColor = subtitleColor,
                    onClick = { showClearDialog = true }
                )
                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text(stringResource(R.string.confirm_clear)) },
                        text = { Text(stringResource(R.string.confirm_clear_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                settingsViewModel.clearAllData()
                                showClearDialog = false
                            }) {
                                Text(stringResource(R.string.clear), color = GdictColors.CoralAccent)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onSelect = { tag ->
                settingsViewModel.setLanguage(tag)
                showLanguageDialog = false
                (context as? android.app.Activity)?.recreate()
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun ProfileHero(
    darkMode: Boolean,
    textColor: Color,
    subtitleColor: Color
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .border(1.dp, borderColor, CircleShape)
                .background(glassBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = GdictColors.Primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Column {
            Text(
                stringResource(R.string.profile),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                stringResource(R.string.manage_your_settings),
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        LocaleHelper.LANG_FOLLOW_SYSTEM to stringResource(R.string.lang_follow_system),
        LocaleHelper.LANG_ENGLISH to stringResource(R.string.lang_english),
        LocaleHelper.LANG_SIMPLIFIED_CHINESE to stringResource(R.string.lang_simplified_chinese),
        LocaleHelper.LANG_TRADITIONAL_CHINESE to stringResource(R.string.lang_traditional_chinese)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_language)) },
        text = {
            Column {
                options.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(tag) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tag == currentLanguage,
                            onClick = { onSelect(tag) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = GdictColors.Primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable { onSelect(tag) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    darkMode: Boolean,
    textColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .background(glassBg)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GdictColors.Primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
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
    darkMode: Boolean,
    textColor: Color,
    subtitleColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val iconContainerBg = GdictColors.Primary.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GdictColors.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsButtonItem(
    title: String,
    description: String,
    icon: ImageVector,
    darkMode: Boolean,
    textColor: Color,
    subtitleColor: Color,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val iconContainerBg = GdictColors.Primary.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconContainerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GdictColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (badgeText != null) GdictColors.Primary else subtitleColor
                )
            }
        }
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GdictColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .background(GdictColors.Primary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GdictColors.Primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = GdictColors.Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
    }
}
