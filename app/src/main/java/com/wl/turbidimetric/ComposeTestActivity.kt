package com.wl.turbidimetric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class ComposeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text("asfsdafsdfasd")
//                    RectView()
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1080, widthDp = 1920)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Text("asfsdafsdfasd")
//                    RectView()
        }
    }
}

@Composable
fun RectView(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.5f)
            .heightIn(800.dp, 800.dp)
//            .size(500.dp)
            .background(Color.Red)
            .clickable {
                println("点击了1")
            }
            .padding(100.dp)
            .background(Color.Yellow)
            .clickable {
                println("点击了2")
            }
            .padding(100.dp)
            .background(Color.Green)
            .clickable {
                println("点击了3")
            }) {

    }
}
