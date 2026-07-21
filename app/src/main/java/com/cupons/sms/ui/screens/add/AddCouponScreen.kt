package com.cupons.sms.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cupons.sms.ui.components.DateInputField
import com.cupons.sms.ui.theme.*

/**
 * AddCouponScreen — מסך הוספת קופון ידנית.
 *
 * Header gradient + טופס עם שדות מעוצבים.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCouponScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddCouponViewModel = hiltViewModel()
) {
    // ניווט חזרה לאחר שמירה
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) {
            viewModel.onSavedHandled()
            onNavigateBack()
        }
    }

    val gradient = Brush.horizontalGradient(BrandGradient)

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(gradient)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text       = "Qponix — הוסף קופון",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 20.sp,
                                color      = Color.White
                            )
                            Text(
                                text  = "הזן את פרטי הקופון ידנית",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "חזור",
                                tint = Color.White
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── קוד קופון (שדה חובה) — בולט ✳ ───
            SectionCard {
                Text(
                    text       = "קוד הקופון *",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = viewModel.couponCode,
                    onValueChange = viewModel::onCouponCodeChange,
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("לדוגמה: SUMMER2026") },
                    isError       = viewModel.couponCodeError != null,
                    supportingText = viewModel.couponCodeError?.let { { Text(it, color = ErrorRed) } },
                    textStyle     = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color         = PrimaryGreen
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction      = ImeAction.Next
                    ),
                    singleLine = true,
                    shape      = RoundedCornerShape(14.dp),
                    colors     = couponFieldColors()
                )
            }

            // ─── פרטי העסק ───
            SectionCard {
                SectionTitle("פרטי העסק", Icons.Default.Store)
                Spacer(Modifier.height(8.dp))
                FormField(
                    value         = viewModel.merchantName,
                    onValueChange = viewModel::onMerchantNameChange,
                    label         = "שם בית עסק",
                    placeholder   = "לדוגמה: Zara",
                    icon          = Icons.Default.ShoppingBag
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    value         = viewModel.websiteUrl,
                    onValueChange = viewModel::onWebsiteUrlChange,
                    label         = "אתר אינטרנט",
                    placeholder   = "https://...",
                    icon          = Icons.Default.Language,
                    keyboardType  = KeyboardType.Uri
                )
            }

            // ─── פרטי הקופון ───
            SectionCard {
                SectionTitle("פרטי הקופון", Icons.Default.LocalOffer)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormField(
                        value         = viewModel.amount,
                        onValueChange = viewModel::onAmountChange,
                        label         = "סכום (₪)",
                        placeholder   = "0.00",
                        icon          = Icons.Default.AttachMoney,
                        keyboardType  = KeyboardType.Decimal,
                        modifier      = Modifier.weight(1f)
                    )
                    DateInputField(
                        value         = viewModel.expiryDate,
                        onValueChange = viewModel::onExpiryDateChange,
                        label         = "תאריך תפוגה",
                        modifier      = Modifier.weight(1f),
                        colors        = couponFieldColors()
                    )
                }
            }

            // ─── הערות ───
            SectionCard {
                SectionTitle("הערות", Icons.AutoMirrored.Filled.Notes)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = viewModel.notes,
                    onValueChange = viewModel::onNotesChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    placeholder   = { Text("הערות נוספות...") },
                    maxLines      = 4,
                    shape         = RoundedCornerShape(14.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ─── כפתור שמירה — gradient ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (!viewModel.isSaving)
                            Brush.horizontalGradient(BrandGradient)
                        else
                            Brush.horizontalGradient(listOf(PrimaryGreen.copy(alpha = 0.4f), GradientEnd.copy(alpha = 0.4f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick  = viewModel::save,
                    enabled  = !viewModel.isSaving,
                    modifier = Modifier.fillMaxSize(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    if (viewModel.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(22.dp),
                            color       = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = "שמור קופון",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Helper Composables ───

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(PrimaryGreen.copy(alpha = 0.3f), GradientEnd.copy(alpha = 0.05f))
                ),
                shape = cardShape
            )
            .background(CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content
        )
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = PrimaryGreen)
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        modifier      = modifier,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = TextSecondary.copy(alpha = 0.6f)) },
        leadingIcon   = { Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp)) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        singleLine    = true,
        shape         = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun couponFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = PrimaryGreen,
    unfocusedBorderColor = PrimaryGreenLight.copy(alpha = 0.5f),
    focusedLabelColor    = PrimaryGreen
)
