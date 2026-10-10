package com.example.machaindo.ui.stok

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.machaindo.R
import com.example.machaindo.ui.rekap.RekapActivity

class StokActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stok)

        val btnBack = findViewById<Button>(R.id.btnBackToKasir)
        val btnRekap = findViewById<ImageView>(R.id.btnRekapShortcut)

        btnBack.setOnClickListener {
            finish()
        }

        btnRekap.setOnClickListener {
            startActivity(Intent(this, RekapActivity::class.java))
        }
    }
}
