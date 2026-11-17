package dev.master.lumio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.master.lumio.navigation.LumioApp
import dev.master.lumio.ui.theme.LumioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LumioTheme { LumioApp((application as LumioApplication).container) } }
    }
}
