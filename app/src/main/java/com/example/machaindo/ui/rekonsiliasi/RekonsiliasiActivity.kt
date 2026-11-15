package com.example.machaindo.ui.rekonsiliasi

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.machaindo.R

class RekonsiliasiActivity : AppCompatActivity() {

    private lateinit var tvTotalSistem: TextView
    private lateinit var tvJumlahTrx: TextView
    private lateinit var etTotalAktual: EditText
    private lateinit var btnSimpan: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rekonsiliasi)

        tvTotalSistem = findViewById(R.id.tvTotalSistem)
        tvJumlahTrx = findViewById(R.id.tvJumlahTrx)
        etTotalAktual = findViewById(R.id.etTotalAktual)
        btnSimpan = findViewById(R.id.btnSimpanRekon)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        tvTotalSistem.text = "Rp 125.000"
        tvJumlahTrx.text = "4 Transaksi"

        btnSimpan.setOnClickListener {
            val aktual = etTotalAktual.text.toString().toDoubleOrNull() ?: 0.0
            val sistem = 125000.0
            val selisih = aktual - sistem

            val msg = if (selisih == 0.0) {
                "Rekonsiliasi berhasil! Total QRIS cocok dengan catatan kasir."
            } else {
                "Rekonsiliasi tersimpan. Terdapat selisih Rp ${String.format("%,.0f", selisih).replace(',', '.')}"
            }

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
