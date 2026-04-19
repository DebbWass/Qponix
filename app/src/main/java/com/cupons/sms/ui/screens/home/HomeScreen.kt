package com.cupons.sms.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cupons.sms.domain.model.Coupon
import com.cupons.sms.ui.components.CouponCard
import com.cupons.sms.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCouponClick : (couponId: Long, couponIds: List<Long>) -> Unit,
    onAddManual   : () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel     : HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query   by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) viewModel.importFromSms()
    }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(uiState.importMessage) {
        uiState.importMessage?.let { snackbarHost.showSnackbar(it); viewModel.dismissImportMessage() }
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.selectedTab.ordinal,
        pageCount   = { CouponTab.entries.size }
    )
    // סוויפ → עדכון הטאב ב-ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onTabChange(CouponTab.entries[pagerState.currentPage])
    }
    // לחיצה על טאב → גלילה ל-Pager
    LaunchedEffect(uiState.selectedTab) {
        if (pagerState.currentPage != uiState.selectedTab.ordinal) {
            pagerState.animateScrollToPage(uiState.selectedTab.ordinal)
        }
    }

    Scaffold(
        containerColor = BgDeep,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            // ─── Custom gradient header ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF150B35), BgDeep),
                            startY = 0f,
                            endY   = 400f
                        )
                    )
            ) {
                // ─── Brand bar ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Logo + שם
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Q badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(BrandGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = "Q",
                                color      = Color.White,
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text       = "Qponix",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                    }

                    // פעולות ימניות
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "הגדרות",
                                tint = Color.White.copy(alpha = 0.7f))
                        }
                        if (uiState.selectedTab != CouponTab.DELETED) {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "מיון",
                                    tint = Color.White.copy(alpha = 0.7f))
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text    = { Text(order.displayName) },
                                        onClick = { viewModel.onSortOrderChange(order); showSortMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // ─── Tab row ───
                TabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor   = Color.Transparent,
                    contentColor     = Color.White,
                    divider          = {},
                    indicator        = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal])
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(Brush.horizontalGradient(BrandGradient))
                        )
                    }
                ) {
                    CouponTab.entries.forEachIndexed { index, tab ->
                        val count = when (tab) {
                            CouponTab.ACTIVE        -> uiState.activeCoupons.size
                            CouponTab.EXPIRING_SOON -> uiState.expiringSoonCoupons.size
                            CouponTab.EXPIRED       -> uiState.expiredCoupons.size
                            CouponTab.ARCHIVED      -> uiState.archivedCoupons.size
                            CouponTab.DELETED       -> uiState.deletedCoupons.size
                        }
                        val isSelected = uiState.selectedTab.ordinal == index
                        Tab(
                            selected  = isSelected,
                            onClick   = { viewModel.onTabChange(tab) },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text       = tab.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize   = 12.sp,
                                        color      = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                                    )
                                    if (count > 0) {
                                        Text(
                                            text     = "$count",
                                            fontSize = 10.sp,
                                            color    = if (isSelected) PrimaryGreenLight else Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (uiState.isMultiSelectMode) {
                MultiSelectBottomBar(
                    selectedCount = uiState.selectedIds.size,
                    isAllSelected = uiState.isAllSelected,
                    onSelectAll   = viewModel::selectAll,
                    onDelete      = viewModel::deleteSelected,
                    onCancel      = viewModel::clearSelection
                )
            }
        },
        floatingActionButton = {
            if ((uiState.selectedTab == CouponTab.ACTIVE || uiState.selectedTab == CouponTab.EXPIRING_SOON) && !uiState.isMultiSelectMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // FAB ידני — מינימליסטי
                    SmallFloatingActionButton(
                        onClick          = onAddManual,
                        modifier         = Modifier.padding(bottom = 10.dp),
                        containerColor   = CardBackground,
                        contentColor     = PrimaryGreen
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "הוסף ידנית")
                    }
                    // FAB ראשי — gradient
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(BrandGradient))
                            .clickable {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.READ_SMS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) viewModel.importFromSms()
                                else permissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(26.dp),
                                color       = Color.White,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(Icons.Default.Sms, contentDescription = "סרוק SMS",
                                tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── Search ───
            if (uiState.selectedTab != CouponTab.DELETED) {
                DarkSearchBar(query = query, onQueryChange = viewModel::onSearchQueryChange)
            }

            // ─── ממתינים לאישור ───
            if (uiState.selectedTab == CouponTab.ACTIVE) {
                AnimatedVisibility(visible = uiState.pendingCoupons.isNotEmpty()) {
                    PendingCouponsSection(
                        coupons    = uiState.pendingCoupons,
                        onConfirm  = viewModel::confirmPendingCoupon,
                        onDismiss  = viewModel::dismissPendingCoupon,
                        onCloseAll = viewModel::dismissAllPending
                    )
                }
            }

            // ─── רשימה עם סוויפ בין טאבים ───
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else {
                HorizontalPager(
                    state    = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val tab = CouponTab.entries[page]
                    val pageCoupons = when (tab) {
                        CouponTab.ACTIVE        -> uiState.activeCoupons
                        CouponTab.EXPIRING_SOON -> uiState.expiringSoonCoupons
                        CouponTab.EXPIRED       -> uiState.expiredCoupons
                        CouponTab.ARCHIVED      -> uiState.archivedCoupons
                        CouponTab.DELETED       -> uiState.deletedCoupons
                    }
                    if (pageCoupons.isEmpty()) {
                        EmptyState(tab)
                    } else {
                        LazyColumn(
                            modifier       = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
                        ) {
                            items(pageCoupons, key = { it.id }) { coupon ->
                                when (tab) {
                                    CouponTab.DELETED -> DeletedCouponCard(
                                        coupon    = coupon,
                                        onClick   = { onCouponClick(coupon.id, pageCoupons.map { it.id }) },
                                        onRestore = { viewModel.restoreCoupon(coupon.id) }
                                    )
                                    else -> CouponCard(
                                        coupon            = coupon,
                                        onClick           = { onCouponClick(coupon.id, pageCoupons.map { it.id }) },
                                        onMarkUsed        = { viewModel.markAsUsed(coupon.id) },
                                        isMultiSelectMode = uiState.isMultiSelectMode,
                                        isSelected        = coupon.id in uiState.selectedIds,
                                        onLongClick       = { viewModel.enterMultiSelectMode(coupon.id) },
                                        onToggleSelect    = { viewModel.toggleSelection(coupon.id) },
                                        isExpiringSoon    = coupon.id in uiState.expiringSoonIds
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

// ─── Dark Search Bar ───

@Composable
private fun DarkSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        modifier      = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder   = { Text("חפש לפי קוד, שם, טקסט...", color = TextSecondary) },
        leadingIcon   = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
        trailingIcon  = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null, tint = TextSecondary)
                }
            }
        },
        singleLine = true,
        shape      = RoundedCornerShape(16.dp),
        colors     = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = PrimaryGreen,
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            cursorColor          = PrimaryGreen,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary
        )
    )
}

// ─── Multi-Select Bottom Bar ───

@Composable
private fun MultiSelectBottomBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onSelectAll:   () -> Unit,
    onDelete:      () -> Unit,
    onCancel:      () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0D0D25)))
            )
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Surface(
            shape  = RoundedCornerShape(20.dp),
            color  = CardBackground,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("ביטול", color = TextSecondary)
                }
                Text(
                    "$selectedCount נבחרו",
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Row {
                    TextButton(onClick = onSelectAll) {
                        Text(
                            if (isAllSelected) "בטל הכל" else "הכל",
                            color = PrimaryGreenLight
                        )
                    }
                    if (selectedCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(ErrorRed, Color(0xFFFF6B6B))))
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, null, tint = Color.White,
                                    modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("מחק ($selectedCount)", color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Deleted Coupon Card ───

@Composable
private fun DeletedCouponCard(coupon: Coupon, onClick: () -> Unit, onRestore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    coupon.merchantName ?: coupon.sender,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextSecondary
                )
                Text(
                    coupon.couponCode,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryGreen.copy(alpha = 0.15f))
                    .clickable(onClick = onRestore)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("שחזר", color = PrimaryGreenLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

// ─── Pending Coupons ───

@Composable
private fun PendingCouponsSection(
    coupons: List<Coupon>,
    onConfirm: (Coupon) -> Unit,
    onDismiss: (Coupon) -> Unit,
    onCloseAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("${coupons.size} פריטים לאישור",
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onCloseAll, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.padding(vertical = 6.dp))
            coupons.forEach { coupon ->
                PendingCouponRow(
                    coupon    = coupon,
                    onConfirm = { onConfirm(coupon) },
                    onDismiss = { onDismiss(coupon) }
                )
            }
        }
    }
}

@Composable
private fun PendingCouponRow(coupon: Coupon, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Column {
                    Text(coupon.merchantName ?: coupon.sender,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("קוד: ${coupon.couponCode}",
                        style = MaterialTheme.typography.bodySmall, color = PrimaryGreenLight)
                    // Confidence bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(coupon.confidence)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            coupon.confidence >= 0.7f -> StatusActive
                                            coupon.confidence >= 0.5f -> SecondaryAmber
                                            else -> ErrorRed
                                        }
                                    )
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${(coupon.confidence * 100).toInt()}%",
                            color = TextSecondary, fontSize = 10.sp
                        )
                    }
                }
            }
            Row {
                TextButton(onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusActive)) { Text("אשר ✓") }
                TextButton(onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)) { Text("דחה") }
            }
        }
        if (expanded && !coupon.rawSmsBody.isNullOrBlank()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CodeBoxBg)
                .padding(10.dp)
            ) {
                Text(coupon.rawSmsBody, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
    }
}

// ─── Empty State ───

@Composable
private fun EmptyState(tab: CouponTab) {
    val (icon, title, sub) = when (tab) {
        CouponTab.ACTIVE        -> Triple(Icons.Default.CardGiftcard, "אין קופונים פעילים", "לחצי על כפתור ה-SMS לסריקה")
        CouponTab.EXPIRING_SOON -> Triple(Icons.Default.NotificationsActive, "אין קופונים עומדים לפוג", "כל הקופונים שלך עדיין רחוקים מפקיעה")
        CouponTab.EXPIRED       -> Triple(Icons.Default.HourglassEmpty, "אין קופונים פגי תוקף", "כל הקופונים שלך עדיין בתוקף")
        CouponTab.ARCHIVED      -> Triple(Icons.Default.Archive, "הארכיון ריק", "קופונים ממומשים יגיעו לכאן")
        CouponTab.DELETED       -> Triple(Icons.Default.DeleteOutline, "לא נמחקו קופונים", "מחיקות תופענה כאן")
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(tab) { visible = true }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "empty_alpha"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        ),
        label = "empty_scale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(PrimaryGreen.copy(alpha = 0.2f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryGreen, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = TextPrimary, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp))
            }
        }
    }
}

private val SortOrder.displayName: String get() = when (this) {
    SortOrder.DATE_DESC   -> "תאריך (חדש לישן)"
    SortOrder.DATE_ASC    -> "תאריך (ישן לחדש)"
    SortOrder.AMOUNT_DESC -> "סכום (גדול לקטן)"
    SortOrder.AMOUNT_ASC  -> "סכום (קטן לגדול)"
    SortOrder.MERCHANT    -> "שם בית עסק"
}
