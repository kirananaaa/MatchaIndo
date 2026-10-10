package com.example.machaindo.ui.absensi

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.machaindo.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class AbsensiActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvStatus: TextView
    private lateinit var btnPresensi: Button

    // Koordinat Booth (Contoh: Cabang Senopati)
    private val boothLat = -7.8227
    private val boothLon = 112.0372
    private val radiusMeters = 50.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absensi) // Buat layout XML dengan ID sesuai

        tvStatus = findViewById(R.id.tvStatusLokasi)
        btnPresensi = findViewById(R.id.btnPresensi)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnPresensi.setOnClickListener {
            cekLokasiDanAbsen()
        }
    }

    private fun cekLokasiDanAbsen() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, boothLat, boothLon, results)
                val distanceInMeters = results[0]

                if (distanceInMeters <= radiusMeters) {
                    tvStatus.text = "Berada di area cabang ($distanceInMeters m). Presensi Berhasil!"
                    tvStatus.setTextColor(resources.getColor(R.color.green_dark))
                } else {
                    tvStatus.text = "Di luar jangkauan ($distanceInMeters m). Tidak bisa presensi."
                    tvStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                }
            } else {
                Toast.makeText(this, "Nyalakan GPS Anda", Toast.LENGTH_SHORT).show()
            }
        }
    }
}