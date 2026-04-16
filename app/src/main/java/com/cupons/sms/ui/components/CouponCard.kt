package com.cupons.sms.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CouponCard — Glass-dark design.
 *
 * מסגרת gradient, רקע כהה, קוד גדול ב-monospace + gradient text,
 * תגית זהב לסכום, פס סטטוס זוהר.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CouponCard(
    coupon: Coupon,
    onClick: () -> Unit,
    onMarkUsed: () -> Unit,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    isExpiringSoon: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    val context          = LocalContext.current
    var showMenu         by remember { mutableStateOf(false) }

    // ─── צבע מסגרת לפי סטטוס ───
    val accentColor = when {
        coupon.isExpired -> ErrorRed
        isExpiringSoon   -> SecondaryAmber
        coupon.displayBalance != null && coupon.displayBalance!! < 50.0 -> StatusLow
        else -> PrimaryGreen
    }

    val selectedScale by animateColorAsState(
        targetValue = if (isSelected) CardHighlight else CardBackground,
        label = "bg"
    )

    val cardShape = RoundedCornerShape(24.dp)

    // ─── Outer glow (animated scale on select) ───
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .graphicsLayer {
                scaleX = if (isSelected) 0.97f else 1f
                scaleY = if (isSelected) 0.97f else 1f
            }
    ) {
        // ─── Glass card ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                // Gradient border
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = if (isSelected) 0.9f else CardBorderAlpha),
                            accentColor.copy(alpha = 0.05f)
                        )
                    ),
                    shape = cardShape
                )
                .background(if (isSelected) CardHighlight else CardBackground)
                .combinedClickable(
                    onClick     = { if (isMultiSelectMode) onToggleSelect() else onClick() },
                    onLongClick = { if (!isMultiSelectMode) onLongClick() }
                )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {

                // ─── Checkbox ───
                if (isMultiSelectMode) {
                    Checkbox(
                        checked        = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier        = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 12.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryGreen,
                            checkmarkColor = Color.White
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(18.dp)
                ) {
                    // ─── שורה 1: שם עסק + סכום ───
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // שם + סטטוס
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.weight(1f)
                        ) {
                            // נקודת סטטוס
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text  = coupon.merchantName ?: coupon.sender,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textDirection = TextDirection.Content,
                                        color         = TextPrimary
                                    ),
                                    maxLines = 1
                                )
                                if (coupon.isExpired) {
                                    Text(
                                        text       = "פג תוקף",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = ErrorRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (isExpiringSoon && !coupon.isExpired) {
                                    Text(
                                        text       = "עומד לפוג בקרוב",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = SecondaryAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // תגית סכום זהב
                        if (coupon.hasAmount) {
                            Spacer(Modifier.width(8.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.horizontalGradient(listOf(GoldStart, GoldEnd))
                                        )
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text       = coupon.formatAmount(coupon.displayBalance),
                                        color      = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ─── קוד הקופון — Hero Element ───
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CodeBoxBg)
                                .border(
                                    1.dp,
                                    PrimaryGreen.copy(alpha = 0.2f),
                                    RoundedCornerShape(14.dp)
                                )
                                .combinedClickable(
                                    onClick     = { if (isMultiSelectMode) onToggleSelect() },
                                    onLongClick = {
                                        if (!isMultiSelectMode) {
                                            clipboardManager.setText(AnnotatedString(coupon.couponCode))
                                            android.widget.Toast.makeText(
                                                context, "הקוד הועתק ✓",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                // Gradient text על הקוד
                                Text(
                                    text  = coupon.couponCode,
                                    style = TextStyle(
                                        brush         = Brush.horizontalGradient(BrandGradient),
                                        fontFamily    = FontFamily.Monospace,
                                        fontSize      = 20.sp,
                                        fontWeight    = FontWeight.ExtraBold,
                                        letterSpacing = 3.sp
                                    )
                                )
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "העתק",
                                    tint     = PrimaryGreen.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ─── שורה תחתונה: תאריכים + תפריט ───
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = formatDate(coupon.receivedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (coupon.expiresAt != null) {
                                Text(
                                    text  = "עד ${formatDate(coupon.expiresAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (coupon.isExpired) ErrorRed else TextSecondary
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            if (!isMultiSelectMode) {
                                Box {
                                    IconButton(
                                        onClick  = { showMenu = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "אפשרויות",
                                            tint     = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded         = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("מימשתי ✓") },
                                            onClick = { showMenu = false; onMarkUsed() },
                                            leadingIcon = {
                                                Icon(Icons.Default.CheckCircle, null, tint = StatusActive)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(ts: Long): String =
    SimpleDateFormat("dd/MM/yy", Locale("he", "IL")).format(Date(ts))
