package com.cupons.sms.ui.screens.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cupons.sms.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה",
                                tint = Color.White)
                        }
                        Text(
                            text       = "סטטיסטיקות",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            modifier   = Modifier.padding(start = 4.dp)
                        )
                    }
                    IconButton(onClick = viewModel::loadStatistics) {
                        Icon(Icons.Default.Refresh, contentDescription = "רענן",
                            tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── כרטיסי סיכום ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title   = "חיסכון כולל",
                    value   = "₪${"%.0f".format(uiState.totalSaved)}",
                    icon    = Icons.Default.Savings,
                    color   = StatusActive,
                    modifier= Modifier.weight(1f)
                )
                StatCard(
                    title   = "ערך שנאבד",
                    value   = "₪${"%.0f".format(uiState.totalExpiredLost)}",
                    icon    = Icons.Default.MoneyOff,
                    color   = ErrorRed,
                    modifier= Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title   = "קופונים פעילים",
                    value   = "${uiState.activeCoupons}",
                    icon    = Icons.Default.CardGiftcard,
                    color   = PrimaryGreenLight,
                    modifier= Modifier.weight(1f)
                )
                StatCard(
                    title   = "קופונים שמומשו",
                    value   = "${uiState.usedCoupons}",
                    icon    = Icons.Default.CheckCircle,
                    color   = SecondaryAmber,
                    modifier= Modifier.weight(1f)
                )
            }

            // ─── Top Merchants ───
            if (uiState.topMerchants.isNotEmpty()) {
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null,
                                tint = PrimaryGreenLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("בתי עסק פופולריים", color = PrimaryGreenLight,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        val maxCount = uiState.topMerchants.maxOf { it.usageCount }.toFloat()
                        uiState.topMerchants.forEach { merchant ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(merchant.merchant, color = TextPrimary, fontSize = 13.sp)
                                    Text("${merchant.usageCount} פעמים",
                                        color = TextSecondary, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(merchant.usageCount / maxCount)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                Brush.horizontalGradient(BrandGradient)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── שימוש חודשי ───
            if (uiState.monthlyUsage.isNotEmpty()) {
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null,
                                tint = PrimaryGreenLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("שימוש חודשי", color = PrimaryGreenLight,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        val maxAmount = uiState.monthlyUsage.maxOf { it.total }.toFloat()
                            .takeIf { it > 0 } ?: 1f

                        uiState.monthlyUsage.take(6).forEach { month ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    month.month,
                                    color    = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(50.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((month.total / maxAmount).toFloat())
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Brush.horizontalGradient(BrandGradient))
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "₪${"%.0f".format(month.total)}",
                                    color    = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(50.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.topMerchants.isEmpty() && uiState.monthlyUsage.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BarChart, null,
                                tint = PrimaryGreen, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("אין עדיין נתונים", color = TextPrimary,
                            fontWeight = FontWeight.SemiBold)
                        Text("מש/מים קופונים כדי לראות סטטיסטיקות",
                            color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    title   : String,
    value   : String,
    icon    : ImageVector,
    color   : Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = modifier
    ) {
        Column(
            modifier             = Modifier.padding(14.dp),
            horizontalAlignment  = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(title, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
