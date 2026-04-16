package com.cupons.sms.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cupons.sms.ui.screens.add.AddCouponScreen
import com.cupons.sms.ui.screens.detail.DetailScreen
import com.cupons.sms.ui.screens.home.HomeScreen
import com.cupons.sms.ui.screens.settings.KeywordsSettingsScreen
import com.cupons.sms.ui.screens.settings.SettingsScreen
import com.cupons.sms.ui.screens.splash.SplashScreen
import com.cupons.sms.ui.screens.statistics.StatisticsScreen

/**
 * AppNavigation — גרף הניווט של האפליקציה.
 *
 * מסלולים:
 * - "splash"                          → SplashScreen (start)
 * - "home"                            → HomeScreen
 * - "add"                             → AddCouponScreen
 * - "detail/{couponId}?couponIds=..."  → DetailScreen (עם ניווט swipe)
 *
 * פרמטר couponIds הוא רשימת מזהים מופרדת בפסיקים של כל הקופונים בטאב
 * הנוכחי. כך ה-DetailScreen יכול לאפשר החלקה בין קופונים.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = "splash"
    ) {

        composable("splash") {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onCouponClick  = { couponId, couponIds ->
                    val idsParam = couponIds.joinToString(",")
                    navController.navigate("detail/$couponId?couponIds=$idsParam")
                },
                onAddManual    = { navController.navigate("add") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack       = { navController.popBackStack() },
                onNavigateToKeywords = { navController.navigate("settings/keywords") },
                onNavigateToStats    = { navController.navigate("statistics") }
            )
        }

        composable("statistics") {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings/keywords") {
            KeywordsSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("add") {
            AddCouponScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = "detail/{couponId}?couponIds={couponIds}",
            arguments = listOf(
                navArgument("couponId") { type = NavType.LongType },
                navArgument("couponIds") {
                    type         = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val couponId    = backStackEntry.arguments?.getLong("couponId") ?: return@composable
            val couponIdsStr = backStackEntry.arguments?.getString("couponIds") ?: ""

            // פירוק רשימת המזהים; fallback לרשימה של קופון אחד
            val couponIds = couponIdsStr
                .split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .ifEmpty { listOf(couponId) }

            val initialIndex = couponIds.indexOf(couponId).coerceAtLeast(0)

            DetailScreen(
                couponIds    = couponIds,
                initialIndex = initialIndex,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
