package com.cupons.sms.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cupons.sms.ui.theme.*
import kotlinx.coroutines.delay

/**
 * SplashScreen — animated entrance for Qponix.
 *
 * Dark radial-gradient background, pulsing glow orbs,
 * spring-animated "Q" badge, gradient + white brand text.
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // ─── Animation states ───
    var started by remember { mutableStateOf(false) }

    val badgeScale by animateFloatAsState(
        targetValue  = if (started) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    val badgeAlpha by animateFloatAsState(
        targetValue  = if (started) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label = "badge_alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(700, delayMillis = 350, easing = EaseOut),
        label = "text_alpha"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(600, delayMillis = 600, easing = EaseOut),
        label = "subtitle_alpha"
    )

    val glowScale by animateFloatAsState(
        targetValue  = if (started) 1f else 0.5f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "glow_scale"
    )

    // Pulsing glow animation (infinite)
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(2400)
        onSplashComplete()
    }

    // ─── Root: near-black + radial glow ───
    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment  = Alignment.Center
    ) {

        // ─── Background glow orbs ───
        Box(
            modifier = Modifier
                .size(500.dp)
                .scale(glowScale * pulseScale)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryGreen.copy(alpha = 0.25f),
                            GradientEnd.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 100.dp, y = 150.dp)
                .scale(glowScale * pulseScale)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GradientEnd.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // ─── Center content ───
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ─── Animated "Q" badge ───
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        scaleX = badgeScale
                        scaleY = badgeScale
                        alpha  = badgeAlpha
                    }
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    PrimaryGreen.copy(alpha = 0.4f),
                                    GradientEnd.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(16.dp)
                )

                // Main gradient circle
                Box(
                    modifier         = Modifier
                        .size(100.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(BrandGradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner dark circle for depth
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF1A0A3E),
                                        Color(0xFF0D0626)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = "Q",
                            style      = TextStyle(
                                brush      = Brush.linearGradient(BrandGradient),
                                fontSize   = 44.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Default
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ─── "Qponix" brand name — gradient first letter, white rest ───
            // חובה: LTR כדי שה-Q תופיע משמאל גם כשהאפליקציה ב-RTL
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier          = Modifier.graphicsLayer { alpha = textAlpha },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "Q",
                        style = TextStyle(
                            brush         = Brush.linearGradient(BrandGradient),
                            fontSize      = 48.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        )
                    )
                    Text(
                        text          = "ponix",
                        fontSize      = 48.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        color         = TextPrimary,
                        letterSpacing = (-1).sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── Tagline ───
            Box(
                modifier = Modifier
                    .graphicsLayer { alpha = subtitleAlpha }
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimaryGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 5.dp)
            ) {
                Text(
                    text      = "הקופונים שלך, במקום אחד",
                    style     = TextStyle(
                        brush     = Brush.horizontalGradient(
                            listOf(PrimaryGreenLight, GradientEnd.copy(alpha = 0.8f))
                        ),
                        fontSize  = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        // ─── Bottom version text ───
        Text(
            text      = "v1.0",
            color     = TextSecondary.copy(alpha = 0.4f),
            fontSize  = 11.sp,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .graphicsLayer { alpha = subtitleAlpha }
        )
    }
}
