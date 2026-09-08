package com.codecraft.contactvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.codecraft.contactvault.presentation.navigation.ContactVaultNavGraph
import com.codecraft.contactvault.ui.theme.ContactVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactVaultTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ContactVaultNavGraph()
                }
            }
        }
    }
}
