package com.example.machaindo.ui.absensi

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.machaindo.R
import com.example.machaindo.data.model.AbsensiRequest
import com.example.machaindo.data.model.AbsensiStatus
import com.example.machaindo.data.model.ApiError
import com.example.machaindo.data.remote.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AbsensiActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvStatus: TextView
    private lateinit var btnPresensi: Button

    // Status absensi hari ini dari server (null = belum berhasil dimuat)
    private var status: AbsensiStatus? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) kirimAbsen()
            else tampilkan("Izin lokasi diperlukan untuk absensi.", error = true)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absensi)

        tvStatus = findViewById(R.id.tvStatusLokasi)
        btnPresensi = findViewById(R.id.btnPresensi)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        btnPresensi.setOnClickListener {
            if (status == null) muatStatus() else mintaIzinLaluAbsen()
        }

        muatStatus()
    }

    // ---------- Ambil status absensi hari ini ----------
    private fun muatStatus() {
        setLoading(true)
        tampilkan("Memuat status absensi...", error = false)
        lifecycleScope.launch {
            try {
                status = RetrofitClient.api.getAbsensiHariIni().data
                tampilkanStatus()
            } catch (e: Exception) {
                status = null
                tampilkan(pesanError(e), error = true)
            } finally {
                setLoading(false)
            }
        }
    }

    // ---------- Absen masuk / pulang ----------
    private fun mintaIzinLaluAbsen() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) kirimAbsen()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private fun kirimAbsen() {
        val s = status ?: return
        setLoading(true)
        tampilkan("Mencari lokasi...", error = false)

        // getCurrentLocation memberi posisi terbaru (lastLocation bisa kosong / basi)
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc == null) {
                    tampilkan("Lokasi tidak ditemukan. Nyalakan GPS lalu coba lagi.", error = true)
                    setLoading(false)
                    return@addOnSuccessListener
                }
                if (lokasiPalsu(loc)) {
                    tampilkan("Terdeteksi lokasi palsu (mock location). Absensi ditolak.", error = true)
                    setLoading(false)
                    return@addOnSuccessListener
                }

                lifecycleScope.launch {
                    try {
                        val body = AbsensiRequest(loc.latitude, loc.longitude)
                        // Jarak dihitung & divalidasi oleh SERVER, bukan di aplikasi
                        val res = if (!s.sudahMasuk) RetrofitClient.api.absenMasuk(body)
                        else RetrofitClient.api.absenPulang(body)
                        status = res.data
                        tampilkanStatus(res.message)
                    } catch (e: Exception) {
                        tampilkan(pesanError(e), error = true)
                    } finally {
                        setLoading(false)
                    }
                }
            }
            .addOnFailureListener {
                tampilkan("Gagal mengambil lokasi.", error = true)
                setLoading(false)
            }
    }

    private fun lokasiPalsu(loc: Location): Boolean =
        if (Build.VERSION.SDK_INT >= 31) loc.isMock
        else @Suppress("DEPRECATION") loc.isFromMockProvider

    // ---------- Tampilan ----------
    private fun tampilkanStatus(pesan: String? = null) {
        val s = status ?: return
        val shift = s.shift?.let { "Shift ${it.namaShift} (${it.jamMulai}–${it.jamSelesai})" }
            ?: "Tidak ada jadwal shift hari ini"

        val teks = when {
            !s.sudahMasuk -> "Belum absen masuk.\n$shift"
            !s.sudahPulang -> {
                val telat = if (s.status == "terlambat") "Terlambat ${s.menitTelat} menit" else "Tepat waktu"
                "Masuk pukul ${s.jamMasuk} ($telat).\n$shift"
            }
            else -> "Masuk ${s.jamMasuk} • Pulang ${s.jamPulang}.\nAbsensi hari ini selesai."
        }
        tampilkan(if (pesan != null) "$pesan\n\n$teks" else teks, error = false)
    }

    private fun tampilkan(teks: String, error: Boolean) {
        tvStatus.text = teks
        tvStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (error) android.R.color.holo_red_dark else R.color.green_dark
            )
        )
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            btnPresensi.isEnabled = false
            btnPresensi.alpha = 0.6f
        } else {
            btnPresensi.alpha = 1f
            updateTombol()
        }
    }

    private fun updateTombol() {
        val s = status
        when {
            s == null -> { btnPresensi.text = "Muat Ulang"; btnPresensi.isEnabled = true }
            !s.sudahMasuk -> { btnPresensi.text = "Absen Masuk"; btnPresensi.isEnabled = true }
            !s.sudahPulang -> { btnPresensi.text = "Absen Pulang"; btnPresensi.isEnabled = true }
            else -> { btnPresensi.text = "Absensi Hari Ini Selesai"; btnPresensi.isEnabled = false }
        }
    }

    private fun pesanError(e: Exception): String = when (e) {
        is HttpException -> {
            val body = e.response()?.errorBody()?.string()
            runCatching { Gson().fromJson(body, ApiError::class.java).message }.getOrNull()
                ?: "Terjadi kesalahan server (${e.code()})"
        }
        is IOException -> "Tidak bisa terhubung ke server"
        else -> "Gagal: ${e.message}"
    }
}
