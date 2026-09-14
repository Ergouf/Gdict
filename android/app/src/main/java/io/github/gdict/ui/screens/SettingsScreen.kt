package io.github.gdict.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero) {
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
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val background = if (darkMode) GdictColors.DarkBackground else GdictColors.Background
    val textColor = if (darkMode) GdictColors.DarkOnBackground else GdictColors.OnBackground
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        ProfileHeader(textColor = textColor, secondary = secondary, darkMode = darkMode)

        SettingsSection(stringResource(R.string.section_dictionaries), darkMode) {
            SettingsButtonItem(
                title = stringResource(R.string.dictionary_management),
                description = stringResource(R.string.dictionary_management_desc),
                icon = Icons.Outlined.MenuBook,
                darkMode = darkMode,
                onClick = onNavigateToDictionaries
            )
        }

        SettingsSection(stringResource(R.string.section_appearance), darkMode) {
            SettingsSwitchItem(
                title = stringResource(R.string.dark_mode),
                description = stringResource(R.string.dark_mode_desc),
                icon = Icons.Outlined.DarkMode,
                checked = darkMode,
                darkMode = darkMode,
                onCheckedChange = settingsViewModel::setDarkMode
            )
            SectionDivider(darkMode)
            SettingsButtonItem(
                title = stringResource(R.string.language),
                description = stringResource(R.string.language_desc),
                icon = Icons.Outlined.Language,
                darkMode = darkMode,
                onClick = { showLanguageDialog = true }
            )
        }

        SettingsSection(stringResource(R.string.section_features), darkMode) {
            SettingsSwitchItem(
                title = stringResource(R.string.scan_popup),
                description = stringResource(R.string.scan_popup_desc),
                icon = Icons.Outlined.QrCodeScanner,
                checked = scanPopup,
                darkMode = darkMode,
                onCheckedChange = settingsViewModel::setScanPopup
            )
        }

        DonationSection(darkMode)

        SettingsSection(stringResource(R.string.section_about), darkMode) {
            SettingsButtonItem(
                title = stringResource(R.string.version_info),
                description = "Gdict v${BuildConfig.VERSION_NAME}",
                icon = Icons.Outlined.Info,
                darkMode = darkMode,
                trailingText = "v${BuildConfig.VERSION_NAME}",
                onClick = {}
            )
            SectionDivider(darkMode)
            SettingsButtonItem(
                title = stringResource(R.string.project_repository),
                description = GITHUB_REPO_URL,
                icon = GitHubMark,
                darkMode = darkMode,
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL)))
                }
            )
            SectionDivider(darkMode)
            SettingsButtonItem(
                title = stringResource(R.string.clear_data),
                description = stringResource(R.string.clear_data_desc),
                icon = Icons.Outlined.DeleteOutline,
                darkMode = darkMode,
                destructive = true,
                onClick = { showClearDialog = true }
            )
        }
        Spacer(Modifier.height(32.dp))
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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear)) },
            text = { Text(stringResource(R.string.confirm_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.clearAllData()
                    showClearDialog = false
                }) { Text(stringResource(R.string.clear), color = GdictColors.CoralAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ProfileHeader(textColor: Color, secondary: Color, darkMode: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = GdictColors.Primary, modifier = Modifier.size(26.dp))
            }
        }
        Column {
            Text(
                text = stringResource(R.string.profile),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = stringResource(R.string.manage_your_settings),
                style = MaterialTheme.typography.bodyMedium,
                color = secondary
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    darkMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = secondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SectionDivider(darkMode: Boolean) {
    HorizontalDivider(
        color = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant,
        modifier = Modifier.padding(start = 68.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    darkMode: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon, darkMode)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor)
            Text(description, style = MaterialTheme.typography.bodySmall, color = secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = GdictColors.Primary)
        )
    }
}

@Composable
private fun SettingsButtonItem(
    title: String,
    description: String,
    icon: ImageVector,
    darkMode: Boolean,
    trailingText: String? = null,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val textColor = if (destructive) GdictColors.CoralAccent else if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon, darkMode, destructive)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = textColor)
            Text(description, style = MaterialTheme.typography.bodySmall, color = secondary, maxLines = 1)
        }
        if (trailingText != null) {
            Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = secondary)
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector, darkMode: Boolean, destructive: Boolean = false) {
    val tint = if (destructive) GdictColors.CoralAccent else GdictColors.Primary
    val background = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
    Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(10.dp), color = background) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onSelect(tag) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tag == currentLanguage,
                            onClick = { onSelect(tag) },
                            colors = RadioButtonDefaults.colors(selectedColor = GdictColors.Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun DonationSection(darkMode: Boolean) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    val secondary = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val surface = if (darkMode) GdictColors.DarkSurface else GdictColors.Surface
    val outline = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.OutlineVariant
    var selectedQr by remember { mutableStateOf<QrCode?>(null) }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.section_support).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = secondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = surface,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = GdictColors.CoralAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.section_support), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = textColor)
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.support_developer_desc), style = MaterialTheme.typography.bodySmall, color = secondary)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val alipay = stringResource(R.string.donation_alipay)
                    val wechat = stringResource(R.string.donation_wechat)
                    QrThumbnail(alipay, R.drawable.donation_alipay, darkMode, Modifier.weight(1f)) {
                        selectedQr = QrCode(alipay, R.drawable.donation_alipay)
                    }
                    QrThumbnail(wechat, R.drawable.donation_wechat, darkMode, Modifier.weight(1f)) {
                        selectedQr = QrCode(wechat, R.drawable.donation_wechat)
                    }
                }
            }
        }
    }

    selectedQr?.let { qr ->
        AlertDialog(
            onDismissRequest = { selectedQr = null },
            title = { Text(qr.label) },
            text = {
                Image(
                    painter = painterResource(qr.drawableRes),
                    contentDescription = qr.label,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedQr = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

private data class QrCode(val label: String, @DrawableRes val drawableRes: Int)

@Composable
private fun QrThumbnail(
    label: String,
    @DrawableRes drawableRes: Int,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = if (darkMode) GdictColors.DarkSurfaceVariant else GdictColors.SurfaceVariant
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnSurface
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(background, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(drawableRes), contentDescription = label, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}
