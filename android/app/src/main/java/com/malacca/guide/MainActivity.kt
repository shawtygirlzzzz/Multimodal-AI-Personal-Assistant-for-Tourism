package com.malacca.guide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.malacca.guide.ui.navigation.AppNavGraph
import com.malacca.guide.ui.theme.HeyCyanGuideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HeyCyanGuideTheme {
                AppNavGraph()
            }
        }
    }
}
