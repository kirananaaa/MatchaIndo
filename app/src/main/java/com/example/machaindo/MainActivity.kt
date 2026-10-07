package com.example.machaindo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.machaindo.data.local.TokenManager
import com.example.machaindo.data.remote.RetrofitClient
import com.example.machaindo.databinding.ActivityMainBinding
import com.example.machaindo.ui.menu.MenuAdapter
import com.example.machaindo.ui.menu.MenuUiState
import com.example.machaindo.ui.menu.MenuViewModel
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MenuViewModel by viewModels()
    private val adapter = MenuAdapter()

    // Supaya chip tidak dibangun ulang pada setiap perubahan state
    private var renderedCategories: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)

        // Jika belum ada token / belum login, kembali ke LoginActivity
        if (TokenManager.token.isNullOrEmpty()) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        bindHeader()

        binding.rvMenu.layoutManager = GridLayoutManager(this, 2)
        binding.rvMenu.adapter = adapter
        binding.btnRetry.setOnClickListener { viewModel.loadMenus() }
        binding.btnLogout.setOnClickListener { confirmLogout() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    // Data header diambil dari TokenManager (disimpan saat login)
    private fun bindHeader() {
        val nama = TokenManager.nama ?: "Barista"
        val role = TokenManager.role?.replaceFirstChar { it.uppercase() } ?: "Barista"
        val cabang = TokenManager.idCabang

        binding.tvAvatar.text = nama.firstOrNull()?.uppercase() ?: "?"
        binding.tvBaristaName.text = "$nama • $role"
        binding.tvBooth.text = if (cabang != -1) "Cabang #$cabang" else "-"
    }

    // ---------- Logout ----------

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Yakin ingin keluar dari akun?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ -> doLogout() }
            .show()
    }

    private fun doLogout() {
        binding.btnLogout.isEnabled = false   // cegah ditekan dua kali

        lifecycleScope.launch {
            // Hapus token di server dulu (token masih dibutuhkan untuk request ini)
            try {
                RetrofitClient.api.logout()
            } catch (_: Exception) {
                // Server tidak terjangkau: tetap lanjut logout di HP
            }

            TokenManager.clear()
            goToLogin()
        }
    }

    // Pindah ke login dan hapus semua layar sebelumnya (tombol Back tidak bisa kembali)
    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ---------- Render menu ----------

    private fun render(state: MenuUiState) {
        val hasError = state.error != null
        val list = state.filteredMenus

        binding.progress.isVisible = state.isLoading
        binding.layoutError.isVisible = hasError && !state.isLoading
        binding.tvError.text = state.error
        binding.rvMenu.isVisible = !hasError
        binding.tvEmpty.isVisible = !state.isLoading && !hasError && list.isEmpty()

        adapter.submitList(list)
        renderChips(state)
    }

    private fun renderChips(state: MenuUiState) {
        val group = binding.chipGroup

        if (renderedCategories != state.categories) {
            renderedCategories = state.categories
            group.removeAllViews()
            // null = "Semua", lalu satu chip per kategori dari API
            (listOf<String?>(null) + state.categories).forEach { kategori ->
                val chip = layoutInflater.inflate(R.layout.item_chip_kategori, group, false) as Chip
                chip.text = kategori ?: "Semua"
                chip.tag = kategori
                chip.setOnClickListener { viewModel.selectCategory(kategori) }
                group.addView(chip)
            }
        }

        // Sinkronkan chip yang aktif dengan state
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            chip.isChecked = (chip.tag as String?) == state.selectedCategory
        }
    }
}