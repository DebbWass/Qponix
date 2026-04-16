package com.cupons.sms.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.cupons.sms.ui.navigation.AppNavigation
import com.cupons.sms.ui.theme.CuponsSMSTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity — נקודת הכניסה של האפליקציה.
 *
 * @AndroidEntryPoint מאפשר ל-Hilt להזריק dependencies ל-Activity זה
 * ולכל ה-composables/ViewModels שלו.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // תמיכה בתצוגה edge-to-edge
        setContent {
            CuponsSMSTheme {
                // כפה RTL על כל ממשק האפליקציה — כל Row/Column/Scaffold יתיישרו ימין לשמאל
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNavigation()
                }
            }
        }
    }
}
