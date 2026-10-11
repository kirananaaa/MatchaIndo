package com.example.machaindo.ui.rekap

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.machaindo.R
import com.example.machaindo.data.local.TokenManager
import java.util.Calendar

class RekapActivity : AppCompatActivity() {

    private lateinit var btnPilihTanggal: Button
    private lateinit var btnClosing: Button
    private lateinit var tvLaporan: TextView
    private var selectedDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rekap) // Buat layout XML terkait

        btnPilihTanggal = findViewById(R.id.btnPilihTanggal)
        btnClosing = findViewById(R.id.btnClosing)
        tvLaporan = findViewById(R.id.tvLaporan)

        btnPilihTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                selectedDate = "$year-${month + 1}-$dayOfMonth"
                btnPilihTanggal.text = "Tanggal: $selectedDate"
                fetchLaporan(selectedDate)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    // Menggunakan Pustaka Volley sesuai syarat UTS
    private fun fetchLaporan(tanggal: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.9:8000/api/rekap?tanggal=$tanggal"

        val jsonObjectRequest = object : JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                tvLaporan.text = "Total Penjualan: Rp " + response.getString("total_omset")
            },
            { error ->
                Toast.makeText(this, "Gagal memuat rekap: ${error.message}", Toast.LENGTH_SHORT).show()
            })
        {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${TokenManager.token}"
                return headers
            }
        }
        queue.add(jsonObjectRequest)
    }
}