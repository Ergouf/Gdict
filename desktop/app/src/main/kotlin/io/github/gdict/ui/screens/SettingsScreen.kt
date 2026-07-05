@file:OptIn(ExperimentalComposeUiApi::class)

package io.github.gdict.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gdict.ui.AppLanguage
import io.github.gdict.ui.components.acrylicAmbientBackground
import io.github.gdict.ui.strings.StringResources
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    strings: StringResources,
    onNavigateToDictionaries: () -> Unit = {},
    onNavigateToFlashcard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val darkMode by settingsViewModel.darkMode.collectAsState()
    val languageCode by settingsViewModel.language.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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

    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidthPx = with(density) { windowInfo.containerSize.width.toFloat() }
    val screenHeightPx = with(density) { windowInfo.containerSize.height.toFloat() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .acrylicAmbientBackground(darkMode, screenWidthPx, screenHeightPx)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ProfileHero(
                darkMode = darkMode,
                strings = strings
            )

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                SettingsSection(
                    title = strings.sectionDictionaries,
                    darkMode = darkMode
                ) {
                    SettingsButtonItem(
                        title = strings.dictionaryManagement,
                        description = strings.dictionaryManagementDesc,
                        icon = Icons.Outlined.MenuBook,
                        darkMode = darkMode,
                        onClick = onNavigateToDictionaries
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsSection(
                    title = strings.sectionStudy,
                    darkMode = darkMode
                ) {
                    SettingsButtonItem(
                        title = strings.flashcardReview,
                        description = strings.flashcardReviewDesc,
                        icon = Icons.Outlined.School,
                        darkMode = darkMode,
                        onClick = onNavigateToFlashcard
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsSection(
                    title = strings.sectionAppearance,
                    darkMode = darkMode
                ) {
                    SettingsSwitchItem(
                        title = strings.darkMode,
                        description = strings.darkModeDesc,
                        icon = Icons.Outlined.DarkMode,
                        checked = darkMode,
                        darkMode = darkMode,
                        onCheckedChange = { settingsViewModel.setDarkMode(it) }
                    )
                    SettingsButtonItem(
                        title = strings.language,
                        description = strings.languageDesc,
                        icon = Icons.Outlined.Language,
                        darkMode = darkMode,
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                DonationSection(strings = strings, darkMode = darkMode)

                Spacer(modifier = Modifier.height(20.dp))

                SettingsSection(
                    title = strings.sectionAbout,
                    darkMode = darkMode
                ) {
                    SettingsButtonItem(
                        title = strings.versionInfo,
                        description = "Gdict Desktop v1.0.0",
                        icon = Icons.Outlined.Info,
                        darkMode = darkMode,
                        onClick = { }
                    )
                    SettingsButtonItem(
                        title = strings.projectRepository,
                        description = "https://github.com/Ergouf/Gdict",
                        icon = Icons.Outlined.Info,
                        darkMode = darkMode,
                        onClick = {
                            try {
                                val desktop = java.awt.Desktop.getDesktop()
                                desktop.browse(java.net.URI("https://github.com/Ergouf/Gdict"))
                            } catch (_: Exception) {}
                        }
                    )
                    SettingsButtonItem(
                        title = strings.clearData,
                        description = strings.clearDataDesc,
                        icon = Icons.Outlined.DeleteOutline,
                        darkMode = darkMode,
                        onClick = { showClearDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(8.dp),
            title = { Text(strings.confirmClear) },
            text = { Text(strings.confirmClearMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.clearAllData()
                        showClearDialog = false
                    }
                ) {
                    Text(strings.clear, color = GdictColors.CoralAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(8.dp),
            title = { Text(strings.selectLanguage) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = languageCode == lang.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) Modifier.background(GdictColors.PrimaryContainer)
                                    else Modifier
                                )
                                .clickable {
                                    settingsViewModel.setLanguage(lang.code)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (lang) {
                                    AppLanguage.English -> strings.langEnglish
                                    AppLanguage.SimplifiedChinese -> strings.langSimplifiedChinese
                                    AppLanguage.TraditionalChinese -> strings.langTraditionalChinese
                                    AppLanguage.FollowSystem -> strings.langFollowSystem
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GdictColors.Primary else GdictColors.OnSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun ProfileHero(
    darkMode: Boolean,
    strings: StringResources
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
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
                strings.profile,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                strings.manageYourSettings,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun DonationSection(
    strings: StringResources,
    darkMode: Boolean
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val cardBg = if (darkMode) GdictColors.DarkSurfaceVariant.copy(alpha = 0.4f) else GdictColors.SurfaceVariant.copy(alpha = 0.4f)
    val cardBorder = if (darkMode) GdictColors.DarkOutlineVariant.copy(alpha = 0.5f) else GdictColors.BlueHighlightBorder.copy(alpha = 0.8f)

    var selectedQr by remember { mutableStateOf<QrCode?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .background(glassBg)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.sectionSupport,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Text(
                text = strings.supportDeveloperDesc,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QrThumbnail(
                    label = strings.donationAlipay,
                    path = "donation/alipay_qr.jpg",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textColor = textColor,
                    onClick = { selectedQr = QrCode(strings.donationAlipay, "donation/alipay_qr.jpg") }
                )
                QrThumbnail(
                    label = strings.donationWechat,
                    path = "donation/wechat_qr.png",
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textColor = textColor,
                    onClick = { selectedQr = QrCode(strings.donationWechat, "donation/wechat_qr.png") }
                )
            }
        }
    }

    selectedQr?.let { qr ->
        AlertDialog(
            onDismissRequest = { selectedQr = null },
            shape = RoundedCornerShape(8.dp),
            title = { Text(qr.label) },
            text = {
                Image(
                    painter = painterResource(qr.path),
                    contentDescription = qr.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedQr = null }) {
                    Text(strings.close)
                }
            }
        )
    }
}

private data class QrCode(val label: String, val path: String)

@Composable
private fun QrThumbnail(
    label: String,
    path: String,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(path),
            contentDescription = label,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    darkMode: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassBg = if (darkMode) GdictColors.BlueSurfaceGlassDark else GdictColors.BlueSurfaceGlass
    val borderColor = if (darkMode) GdictColors.DarkOutlineVariant else GdictColors.BlueHighlightBorder
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground

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
    onCheckedChange: (Boolean) -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
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
    onClick: () -> Unit
) {
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.OnBackground
    val subtitleColor = if (darkMode) GdictColors.DarkOnSurfaceVariant else GdictColors.OnSurfaceVariant
    val iconContainerBg = GdictColors.Primary.copy(alpha = 0.12f)

    // Desktop hover feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverBackground = if (isHovered) {
        if (darkMode) GdictColors.DarkSubtleHover else GdictColors.SubtleHover
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(hoverBackground)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = GdictColors.Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
    }
}
