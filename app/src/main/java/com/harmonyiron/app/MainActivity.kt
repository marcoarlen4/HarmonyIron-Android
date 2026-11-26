package com.harmonyiron.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.harmonyiron.app.ui.navigation.AppNavGraph
import com.harmonyiron.app.ui.theme.HarmonyIronTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HarmonyIronTheme {
                AppNavGraph()
            }
        }
    }
}
