package io.github.dev1ck.ytloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dev1ck.ytloader.ui.theme.YTLoaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTLoaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Text(stringResource(R.string.app_name), modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
