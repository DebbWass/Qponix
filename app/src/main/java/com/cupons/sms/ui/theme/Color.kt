package com.cupons.sms.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand Gradient — Deep Violet → Hot Pink ───
val PrimaryGreen      = Color(0xFF7C3AED)   // Violet-600  (שם ישן — ערך חדש)
val PrimaryGreenLight = Color(0xFFA78BFA)   // Violet-400
val GradientEnd       = Color(0xFFDB2777)   // Pink-600
val BrandGradient     = listOf(PrimaryGreen, GradientEnd)

// ─── Gold — amounts ───
val SecondaryAmber    = Color(0xFFF59E0B)
val GoldText          = Color(0xFFFFFFFF)   // לבן על גרדיאנט זהב
val GoldStart         = Color(0xFFB45309)   // Amber-700
val GoldEnd           = Color(0xFFF59E0B)   // Amber-500

// ─── Status ───
val ErrorRed          = Color(0xFFEF4444)
val StatusActive      = Color(0xFF10B981)
val StatusLow         = Color(0xFFF59E0B)

// ─── Dark Backgrounds ───
val BgDeep            = Color(0xFF08081A)   // near-black עם גוון סגול
val SurfaceLight      = Color(0xFF0F0F24)   // כרטיסים
val CardBackground    = Color(0xFF111130)   // רקע כרטיס
val CardHighlight     = Color(0xFF1E1050)   // כרטיס נבחר
val BalanceChipBg     = Color(0xFF1A0D00)   // רקע עגול יתרה

// ─── Code Box Background ───
val CodeBoxBg         = Color(0xFF07071A)

// ─── Text ───
val TextPrimary       = Color(0xFFFFFFFF)
val TextSecondary     = Color(0xFF94A3B8)
val TextOnPrimary     = Color(0xFFFFFFFF)

// ─── Borders / Glow ───
val CardBorderAlpha   = 0.35f              // שקיפות מסגרת כרטיס
