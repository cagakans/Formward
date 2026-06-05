package com.example.mis49mproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mis49mproject.navigation.AppNavigation
import com.example.mis49mproject.ui.theme.MIS49MProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MIS49MProjectTheme {
                AppNavigation()
            }
        }
    }
}