package com.cupons.sms.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cupons.sms.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeywordsSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: KeywordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var newKeyword   by remember { mutableStateOf("") }
    var newBlacklist by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BgDeep,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה",
                            tint = Color.White)
                    }
                    Text(
                        text       = "מילות מפתח",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── מילות מפתח לזיהוי קופון ───
            KeywordSectionCard(
                title    = "מילות זיהוי קופון",
                subtitle = "הוספת מילים שיגרמו לזיהוי SMS/WhatsApp כקופון",
                accentColor = PrimaryGreenLight
            ) {
                // שדה הוספה
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = newKeyword,
                        onValueChange = { newKeyword = it },
                        placeholder   = { Text("מילה חדשה...", color = TextSecondary) },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = PrimaryGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick  = {
                            if (newKeyword.isNotBlank()) {
                                viewModel.addKeyword(newKeyword.trim())
                                newKeyword = ""
                            }
                        },
                        modifier = Modifier
                            .background(PrimaryGreen, RoundedCornerShape(12.dp))
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "הוסף", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // רשימת chips
                if (uiState.customKeywords.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.customKeywords.forEach { word ->
                            InputChip(
                                selected = false,
                                onClick  = {},
                                label    = { Text(word, fontSize = 13.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick  = { viewModel.removeKeyword(word) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "הסר",
                                            tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor       = CardHighlight,
                                    labelColor           = TextPrimary,
                                    selectedContainerColor = CardHighlight
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text     = "אין מילות מפתח מותאמות — הוסיפי מילים כדי לשפר את הזיהוי",
                        color    = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // ─── רשימה שחורה ───
            KeywordSectionCard(
                title    = "רשימה שחורה",
                subtitle = "הודעות המכילות מילים אלו יסוננו אוטומטית",
                accentColor = ErrorRed
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value         = newBlacklist,
                        onValueChange = { newBlacklist = it },
                        placeholder   = { Text("מילה לחסימה...", color = TextSecondary) },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = ErrorRed,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = ErrorRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick  = {
                            if (newBlacklist.isNotBlank()) {
                                viewModel.addBlacklist(newBlacklist.trim())
                                newBlacklist = ""
                            }
                        },
                        modifier = Modifier
                            .background(ErrorRed, RoundedCornerShape(12.dp))
                            .size(48.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "הוסף לחסימה",
                            tint = Color.White)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (uiState.customBlacklist.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.customBlacklist.forEach { word ->
                            InputChip(
                                selected = false,
                                onClick  = {},
                                label    = { Text(word, fontSize = 13.sp) },
                                trailingIcon = {
                                    IconButton(
                                        onClick  = { viewModel.removeBlacklist(word) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "הסר",
                                            tint = TextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor        = Color(0xFF2D0A0A),
                                    labelColor            = ErrorRed,
                                    selectedContainerColor= Color(0xFF2D0A0A)
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text     = "אין מילות חסימה מותאמות",
                        color    = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KeywordSectionCard(
    title      : String,
    subtitle   : String,
    accentColor: androidx.compose.ui.graphics.Color,
    content    : @Composable ColumnScope.() -> Unit
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = accentColor, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            content()
        }
    }
}
