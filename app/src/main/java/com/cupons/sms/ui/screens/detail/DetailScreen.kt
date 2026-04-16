package com.cupons.sms.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.domain.model.UsageLog
import com.cupons.sms.ui.components.DateInputField
import com.cupons.sms.ui.components.millisToDateString
import com.cupons.sms.ui.components.parseDateToMillis
import com.cupons.sms.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DetailScreen — מסך פרטי קופון עם ניווט Swipe.
 *
 * מאפשר החלקה ימינה/שמאלה למעבר בין קופונים ברשימה הנוכחית
 * ללא צורך בחזרה למסך הרשימה.
 *
 * @param couponIds    רשימת מזהי הקופונים בטאב הנוכחי (לניווט swipe)
 * @param initialIndex האינדקס הראשוני בתוך [couponIds]
 * @param onNavigateBack פעולה לחזרה למסך הרשימה
 */
@Composable
fun DetailScreen(
    couponIds: List<Long>,
    initialIndex: Int,
    onNavigateBack: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount   = { couponIds.size }
    )

    HorizontalPager(
        state                  = pagerState,
        modifier               = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1   // טעינה מוקדמת של עמוד אחד משני הצדדים
    ) { page ->
        val couponId = couponIds[page]

        // כל עמוד מקבל ViewModel משלו באמצעות AssistedInject + key ייחודי
        val viewModel = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
            key             = "detail_$couponId",
            creationCallback = { factory -> factory.create(couponId) }
        )

        CouponDetailPage(
            viewModel      = viewModel,
            pageIndex      = page,
            totalPages     = couponIds.size,
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * CouponDetailPage — תוכן עמוד בודד ב-HorizontalPager.
 *
 * כולל את כל תוכן מסך הפרטים: כרטיס ראשי, כפתורי פעולה,
 * SMS מקורי, והיסטוריית שימושים.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CouponDetailPage(
    viewModel: DetailViewModel,
    pageIndex: Int,
    totalPages: Int,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // ניווט חזרה אחרי מחיקה/מימוש
    LaunchedEffect(uiState.isDeleted, uiState.isMarkedUsed) {
        if (uiState.isDeleted || uiState.isMarkedUsed) onNavigateBack()
    }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    // Dialog: עדכון יתרה
    var showBalanceDialog by remember { mutableStateOf(false) }
    // Dialog: אישור מימוש
    var showUsedDialog by remember { mutableStateOf(false) }

    if (showBalanceDialog) {
        UpdateBalanceDialog(
            coupon    = uiState.coupon,
            onConfirm = { newBalance, note, newExpiry ->
                viewModel.updateDetailsAndBalance(newBalance, note, newExpiry)
                showBalanceDialog = false
            },
            onDismiss = { showBalanceDialog = false }
        )
    }

    if (showUsedDialog) {
        AlertDialog(
            onDismissRequest = { showUsedDialog = false },
            title = { Text("סמן כממומש?") },
            text = { Text("הקופון יועבר לארכיון. ניתן לצפות בו בארכיון.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.markAsUsed(); showUsedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) { Text("מימשתי") }
            },
            dismissButton = {
                TextButton(onClick = { showUsedDialog = false }) { Text("ביטול") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(
                modifier = Modifier.background(
                    Brush.horizontalGradient(BrandGradient)
                )
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.coupon?.merchantName
                                    ?: uiState.coupon?.sender
                                    ?: "פרטי קופון",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 18.sp,
                                color      = Color.White
                            )
                            // כשיש מספר קופונים — מציג מיקום + רמז להחלקה
                            if (totalPages > 1) {
                                Text(
                                    text  = "קופון ${pageIndex + 1} מתוך $totalPages  •  החלק לניווט",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.70f)
                                )
                            } else {
                                Text(
                                    text  = "פרטי קופון",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "חזרה",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.deleteCoupon() }) {
                            Icon(Icons.Default.Delete, contentDescription = "מחק", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = BgDeep
    ) { padding ->
        if (uiState.isLoading || uiState.coupon == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
            return@Scaffold
        }

        val coupon = uiState.coupon!!

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // ─── כרטיס ראשי ───
            item { MainInfoCard(coupon, clipboardManager, context) }

            // ─── כפתורי פעולה ───
            item {
                ActionButtons(
                    coupon        = coupon,
                    onUpdateBalance = { showBalanceDialog = true },
                    onMarkUsed    = { showUsedDialog = true },
                    onOpenUrl     = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }

            // ─── גוף ה-SMS המקורי ───
            if (coupon.rawSmsBody != null) {
                item { OriginalSmsSection(coupon.rawSmsBody) }
            }

            // ─── היסטוריית שימושים ───
            if (uiState.usageLog.isNotEmpty()) {
                item {
                    Text(
                        text       = "היסטוריית שימושים",
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.usageLog) { log ->
                    UsageLogItem(log, coupon.currency)
                }
            }
        }
    }
}

@Composable
private fun MainInfoCard(
    coupon: Coupon,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    val accentColor = if (coupon.isExpired) ErrorRed else PrimaryGreen
    val cardShape   = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(cardShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.5f), accentColor.copy(alpha = 0.05f))
                ),
                shape = cardShape
            )
            .background(CardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ─── קוד הקופון — Hero ───
            Text(text = "קוד קופון", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CodeBoxBg)
                    .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text  = coupon.couponCode,
                        style = TextStyle(
                            brush         = Brush.horizontalGradient(BrandGradient),
                            fontWeight    = FontWeight.ExtraBold,
                            fontSize      = 22.sp,
                            letterSpacing = 3.sp
                        )
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(coupon.couponCode))
                            android.widget.Toast.makeText(context, "הקוד הועתק ✓", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "העתק", tint = PrimaryGreen)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.07f))
            Spacer(Modifier.height(16.dp))

            // ─── סכום ויתרה ───
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (coupon.originalAmount != null) {
                    InfoChip(label = "סכום מקורי", value = coupon.formatAmount(coupon.originalAmount))
                }
                if (coupon.remainingBalance != null) {
                    InfoChip(label = "יתרה", value = coupon.formatAmount(coupon.remainingBalance), isHighlighted = true)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── פרטים ───
            InfoRow(icon = Icons.Default.Person,    label = "שולח",  value = coupon.sender)

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("he", "IL"))
            InfoRow(icon = Icons.Default.DateRange, label = "התקבל", value = sdf.format(Date(coupon.receivedAt)))

            if (coupon.expiresAt != null) {
                InfoRow(
                    icon       = Icons.Default.Timer,
                    label      = "תוקף עד",
                    value      = sdf.format(Date(coupon.expiresAt)),
                    valueColor = if (coupon.isExpired) ErrorRed else TextPrimary
                )
            }

            if (!coupon.notes.isNullOrBlank()) {
                InfoRow(icon = Icons.AutoMirrored.Filled.Notes, label = "הערות", value = coupon.notes)
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, isHighlighted: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Surface(
            shape  = RoundedCornerShape(20.dp),
            color  = if (isHighlighted) BalanceChipBg else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text       = value,
                fontWeight = FontWeight.Bold,
                color      = if (isHighlighted) PrimaryGreen else TextPrimary,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "$label: ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionButtons(
    coupon: Coupon,
    onUpdateBalance: () -> Unit,
    onMarkUsed: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ─── עדכון יתרה ───
        OutlinedButton(
            onClick  = onUpdateBalance,
            modifier = Modifier.weight(1f).height(48.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp, PrimaryGreen.copy(alpha = 0.5f)
            )
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
            Spacer(Modifier.width(4.dp))
            Text("עדכן יתרה", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
        }

        // ─── מימוש — gradient button ───
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(BrandGradient))
                .then(Modifier.padding(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick  = onMarkUsed,
                modifier = Modifier.fillMaxSize(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("מימשתי", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    // ─── פתיחת אתר ───
    if (!coupon.websiteUrl.isNullOrBlank()) {
        OutlinedButton(
            onClick  = { onOpenUrl(coupon.websiteUrl) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(48.dp),
            shape  = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
            Spacer(Modifier.width(4.dp))
            Text("פתח אתר מימוש", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OriginalSmsSection(body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "הודעת SMS מקורית",
                    fontWeight = FontWeight.SemiBold,
                    color      = PrimaryGreenLight,
                    fontSize   = 13.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun UsageLogItem(log: UsageLog, currency: String) {
    val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale("he", "IL"))
    ListItem(
        headlineContent   = {
            Text(
                text       = "נוצל: $currency${"%.0f".format(log.amountUsed)}",
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
        },
        supportingContent = {
            Column {
                Text(
                    text  = "יתרה לאחר מכן: $currency${"%.0f".format(log.balanceAfter)}  •  ${sdf.format(Date(log.usedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (!log.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreenLight.copy(alpha = 0.7f))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text  = log.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGreenLight
                        )
                    }
                }
            }
        },
        leadingContent = {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = SecondaryAmber)
        },
        colors = ListItemDefaults.colors(containerColor = CardBackground)
    )
}

/**
 * Dialog לעדכון יתרה + תאריך תפוגה.
 *
 * @param onConfirm (newBalance, note, newExpiry in ms)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateBalanceDialog(
    coupon: Coupon?,
    onConfirm: (Double, String?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var balanceText by remember {
        mutableStateOf(coupon?.remainingBalance?.toLong()?.toString() ?: "")
    }
    var expiryText by remember {
        mutableStateOf(millisToDateString(coupon?.expiresAt))
    }
    var note  by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text       = "עדכון פרטים",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ─── יתרה ───
                OutlinedTextField(
                    value           = balanceText,
                    onValueChange   = { balanceText = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                    label           = { Text("יתרה חדשה (${coupon?.currency ?: "₪"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError         = error != null,
                    supportingText  = { error?.let { Text(it, color = ErrorRed) } },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    leadingIcon     = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, tint = PrimaryGreen)
                    }
                )

                // ─── תאריך תפוגה עם תאריכון ───
                DateInputField(
                    value         = expiryText,
                    onValueChange = { expiryText = it },
                    label         = "תאריך תפוגה",
                    modifier      = Modifier.fillMaxWidth()
                )

                // ─── הערה ───
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("הערה (תופיע בהיסטוריית השימושים)") },
                    modifier      = Modifier.fillMaxWidth(),
                    maxLines      = 3,
                    leadingIcon   = {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = PrimaryGreen)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balance = balanceText.toDoubleOrNull()
                    if (balance == null || balance < 0) {
                        error = "יש להזין סכום תקין"
                    } else {
                        val newExpiry = parseDateToMillis(expiryText)
                            ?: coupon?.expiresAt  // אם לא שונה — שמור ישן
                        onConfirm(balance, note.ifBlank { null }, newExpiry)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) { Text("שמור", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול", color = TextSecondary) }
        }
    )
}
