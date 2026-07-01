package io.github.gdict.ui.screens

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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gdict.ui.AppLanguage
import io.github.gdict.ui.strings.StringResources
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.SettingsViewModel
import io.github.gdict.api.AfdianClient

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    strings: StringResources,
    onNavigateToDictionaries: () -> Unit = {},
    onNavigateToFlashcard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val languageCode by settingsViewModel.language.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        strings.profile,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        strings.manageYourSettings,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SettingsSection(title = strings.sectionDictionaries) {
                SettingsButtonItem(
                    title = strings.dictionaryManagement,
                    description = strings.dictionaryManagementDesc,
                    icon = Icons.Outlined.MenuBook,
                    onClick = onNavigateToDictionaries
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSection(title = strings.sectionStudy) {
                SettingsButtonItem(
                    title = strings.flashcardReview,
                    description = strings.flashcardReviewDesc,
                    icon = Icons.Outlined.School,
                    onClick = onNavigateToFlashcard
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSection(title = strings.sectionAppearance) {
                SettingsButtonItem(
                    title = strings.language,
                    description = strings.languageDesc,
                    icon = Icons.Outlined.Language,
                    onClick = { showLanguageDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DonationSection(strings = strings)

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSection(title = strings.sectionAbout) {
                SettingsButtonItem(
                    title = strings.versionInfo,
                    description = "Gdict Desktop v1.0.0",
                    icon = Icons.Outlined.Info,
                    onClick = { }
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsButtonItem(
                    title = strings.projectRepository,
                    description = "https://github.com/Ergouf/Gdict",
                    icon = Icons.Outlined.Info,
                    onClick = {
                        try {
                            val desktop = java.awt.Desktop.getDesktop()
                            desktop.browse(java.net.URI("https://github.com/Ergouf/Gdict"))
                        } catch (_: Exception) {}
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                SettingsButtonItem(
                    title = strings.clearData,
                    description = strings.clearDataDesc,
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = { showClearDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                    Text(strings.clear, color = MaterialTheme.colorScheme.error)
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
                                    if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
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
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
private fun DonationSection(strings: StringResources) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GdictColors.CardStroke, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE53935), // 爱心语义色，保留
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.sectionSupport,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = strings.supportDeveloperDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { AfdianClient.openSponsorPage() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "❤ 爱发电赞助",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GdictColors.CardStroke, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsButtonItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // Fluent 列表项：hover 时 Subtle 填充反馈
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val background = if (isHovered) GdictColors.SubtleHover else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(background)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
