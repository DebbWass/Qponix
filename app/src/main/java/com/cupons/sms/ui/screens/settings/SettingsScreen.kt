package com.cupons.sms.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cupons.sms.data.prefs.BackupFrequency
import com.cupons.sms.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack      : () -> Unit,
    onNavigateToKeywords: () -> Unit,
    onNavigateToStats   : () -> Unit = {},
    viewModel           : SettingsViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val context     = LocalContext.current
    val snackbarHost= remember { SnackbarHostState() }

    LaunchedEffect(backupState.backupMessage) {
        backupState.backupMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.dismissBackupMessage()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.restoreFromFile(it) } }

    Scaffold(
        containerColor = BgDeep,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF150B35), BgDeep),
                            startY = 0f, endY = 300f
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "חזרה",
                            tint = Color.White
                        )
                    }
                    Text(
                        text       = "הגדרות",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                        modifier   = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── סקשן התראות ───
            SettingsSection(title = "התראות", icon = Icons.Default.Notifications) {

                // Toggle ON/OFF
                SettingsToggleRow(
                    label   = "התראות פקיעת תוקף",
                    subtitle= "קבלי push כאשר קופון עומד לפוג",
                    checked = uiState.notificationsEnabled,
                    onToggle= viewModel::setNotificationsEnabled
                )

                if (uiState.notificationsEnabled) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    // Slider — כמה ימים מראש
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text  = "ימים לפני פקיעה",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text  = "${uiState.notificationDaysBefore} ימים",
                                color = PrimaryGreenLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Slider(
                            value         = uiState.notificationDaysBefore.toFloat(),
                            onValueChange = { viewModel.setNotificationDaysBefore(it.toInt()) },
                            valueRange    = 1f..14f,
                            steps         = 12,
                            colors        = SliderDefaults.colors(
                                thumbColor       = PrimaryGreen,
                                activeTrackColor = PrimaryGreen
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1", color = TextSecondary, fontSize = 11.sp)
                            Text("14", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // ─── סריקה מחדש של תאריכי תפוגה ───
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text     = "סרוק מחדש תאריכי תפוגה",
                        color    = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text     = "מעדכן קופונים שיובאו לפני שיפור זיהוי התאריכים",
                        color    = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    OutlinedButton(
                        onClick  = { viewModel.rescanExpiryDates() },
                        enabled  = !backupState.isBackingUp,
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreenLight),
                        border   = BorderStroke(1.dp, Brush.linearGradient(BrandGradient))
                    ) {
                        if (backupState.isBackingUp) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color       = PrimaryGreenLight
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("סרוק מחדש", fontSize = 13.sp)
                    }
                }
            }

            // ─── סקשן גיבוי ───
            SettingsSection(title = "גיבוי ושחזור", icon = Icons.Default.Backup) {

                // תדירות גיבוי אוטומטי
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text     = "גיבוי אוטומטי ל-Google Drive",
                        color    = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text     = "שמירת כל הקופונים בענן — מאפשר שחזור בהחלפת מכשיר",
                        color    = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BackupFrequency.entries.forEach { freq ->
                            val selected = uiState.backupFrequency == freq
                            FilterChip(
                                selected = selected,
                                onClick  = { viewModel.setBackupFrequency(freq) },
                                label    = { Text(freq.label, fontSize = 12.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen,
                                    selectedLabelColor     = Color.White,
                                    containerColor         = CardBackground,
                                    labelColor             = TextSecondary
                                )
                            )
                        }
                    }
                }

                if (uiState.lastBackupTimestamp > 0L) {
                    val dateStr = remember(uiState.lastBackupTimestamp) {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(uiState.lastBackupTimestamp))
                    }
                    Text(
                        text     = "גיבוי אחרון: $dateStr",
                        color    = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                // כפתורי גיבוי ידני
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = { viewModel.backupNow() },
                        enabled  = !backupState.isBackingUp,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreenLight),
                        border   = BorderStroke(1.dp, Brush.linearGradient(BrandGradient))
                    ) {
                        if (backupState.isBackingUp) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color       = PrimaryGreenLight
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("גבה עכשיו", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick  = { restoreLauncher.launch("application/json") },
                        enabled  = !backupState.isBackingUp,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border   = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("שחזר", fontSize = 13.sp)
                    }
                }
            }

            // ─── סקשן מילות מפתח ───
            SettingsSection(title = "מילות מפתח", icon = Icons.Default.Tune) {
                SettingsActionRow(
                    label    = "ניהול מילות מפתח",
                    subtitle = "הוספה/הסרה של מילים לזיהוי קופונים",
                    onClick  = onNavigateToKeywords
                )
            }

            // ─── סקשן סטטיסטיקות ───
            SettingsSection(title = "סטטיסטיקות", icon = Icons.Default.BarChart) {
                SettingsActionRow(
                    label    = "צפייה בסטטיסטיקות",
                    subtitle = "חיסכון, קופונים שפגו, בתי עסק פופולריים",
                    onClick  = onNavigateToStats
                )
            }

            // ─── סקשן WhatsApp ───
            SettingsSection(title = "WhatsApp", icon = Icons.AutoMirrored.Filled.Chat) {
                SettingsToggleRow(
                    label    = "קריאת קופונים מ-WhatsApp",
                    subtitle = "יזהה קופונים בהתראות WhatsApp אוטומטית",
                    checked  = uiState.whatsappEnabled,
                    onToggle = { enabled ->
                        viewModel.setWhatsappEnabled(enabled)
                        if (enabled) {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
                if (uiState.whatsappEnabled) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Column(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                        Text(
                            text  = "כיצד להפעיל:",
                            color = PrimaryGreenLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            "1. בחר «שירותי נגישות» (Accessibility services)",
                            "2. חפש «Qponix» ברשימה",
                            "3. הפעל את השירות ואשר «אישור»",
                            "4. חזור לאפליקציה — כל קופון שיגיע ב-WhatsApp יזוהה אוטומטית"
                        ).forEach { step ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(step, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── רכיבי עזר ───

@Composable
private fun SettingsSection(
    title   : String,
    icon    : ImageVector,
    content : @Composable ColumnScope.() -> Unit
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(icon, contentDescription = null,
                    tint = PrimaryGreenLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = title,
                    color      = PrimaryGreenLight,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label   : String,
    subtitle: String,
    checked : Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier             = Modifier.fillMaxWidth(),
        horizontalArrangement= Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor      = Color.White,
                checkedTrackColor      = PrimaryGreen,
                uncheckedTrackColor    = SurfaceLight
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    label   : String,
    subtitle: String,
    onClick : () -> Unit
) {
    Row(
        modifier             = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement= Arrangement.SpaceBetween,
        verticalAlignment    = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "כניסה",
                tint = TextSecondary
            )
        }
    }
}
