package com.example.machaindo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.machaindo.data.local.TokenManager
import com.example.machaindo.ui.menu.MenuListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)

        // Jika belum ada token / belum login, kembali ke LoginActivity
        if (TokenManager.token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                MenuListScreen()
            }
        }
    }
}
