package com.example.machaindo.ui.stok

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.machaindo.data.remote.RetrofitClient
import com.example.machaindo.databinding.ActivityStokBinding
import com.example.machaindo.ui.rekap.RekapActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class StokActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStokBinding
    private val adapter = StokAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStokBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvStok.layoutManager = LinearLayoutManager(this)
        binding.rvStok.adapter = adapter

        binding.btnBackToKasir.setOnClickListener { finish() }
        binding.btnRekapShortcut.setOnClickListener {
            startActivity(Intent(this, RekapActivity::class.java))
        }
    }

    // Dimuat tiap layar tampil, jadi stok selalu terbaru setelah kembali dari layar lain
    override fun onResume() {
        super.onResume()
        loadStok()
    }

    private fun loadStok() {
        binding.progressStok.isVisible = true
        binding.tvStokEmpty.isVisible = false

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getStok()

                if (res.isSuccessful) {
                    val list = res.body()?.data.orEmpty()
                    adapter.submitList(list)

                    val kritis = list.count { it.status == "kritis" }
                    binding.tvRingkasan.isVisible = list.isNotEmpty()
                    binding.tvRingkasan.text = "${list.size} bahan • $kritis kritis"

                    if (list.isEmpty()) showMessage("Belum ada data stok di cabang ini.")
                } else {
                    adapter.submitList(emptyList())
                    binding.tvRingkasan.isVisible = false
                    showMessage(
                        when (res.code()) {
                            401 -> "Sesi berakhir. Silakan login ulang."
                            422 -> "Akun ini belum terhubung ke cabang."
                            else -> "Gagal memuat stok (kode ${res.code()})."
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                binding.tvRingkasan.isVisible = false
                showMessage("Tidak dapat terhubung ke server.")
            } finally {
                binding.progressStok.isVisible = false
            }
        }
    }

    private fun showMessage(text: String) {
        binding.tvStokEmpty.text = text
        binding.tvStokEmpty.isVisible = true
    }
}