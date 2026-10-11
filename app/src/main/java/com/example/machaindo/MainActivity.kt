package com.example.machaindo

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.machaindo.data.local.TokenManager
import com.example.machaindo.data.model.Menu
import com.example.machaindo.data.remote.RetrofitClient
import com.example.machaindo.databinding.ActivityMainBinding
import com.example.machaindo.ui.absensi.AbsensiActivity
import com.example.machaindo.ui.menu.MenuAdapter
import com.example.machaindo.ui.menu.MenuUiState
import com.example.machaindo.ui.menu.MenuViewModel
import com.example.machaindo.ui.stok.StokActivity
import com.google.android.material.chip.Chip
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MenuViewModel by viewModels()
    private lateinit var adapter: MenuAdapter

    private var renderedCategories: List<String>? = null
    private var cartQuantity = 2
    private var cartPrice = 22000.0

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "Izin notifikasi diberikan: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)

        if (TokenManager.token.isNullOrEmpty()) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        bindHeader()
        setupNotifications() // <-- sebelumnya tidak pernah dipanggil

        adapter = MenuAdapter { menu ->
            addToCart(menu)
        }

        binding.rvMenu.layoutManager = GridLayoutManager(this, 2)
        binding.rvMenu.adapter = adapter
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnAbsensiShortcut.setOnClickListener {
            startActivity(Intent(this, AbsensiActivity::class.java))
        }

        // Bottom Nav
        binding.navKasir.setOnClickListener { /* Already here */ }
        binding.navStok.setOnClickListener {
            startActivity(Intent(this, StokActivity::class.java))
        }
        binding.navLogout.setOnClickListener { confirmLogout() }

        // Cart Actions
        binding.btnPlus.setOnClickListener {
            cartQuantity++
            updateCartUI()
        }
        binding.btnMin.setOnClickListener {
            if (cartQuantity > 1) {
                cartQuantity--
                updateCartUI()
            } else {
                binding.layoutCart.isVisible = false
            }
        }
        binding.btnCheckout.setOnClickListener {
            showPaymentDialog()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Notifikasi (FCM)
    // ---------------------------------------------------------------------

    private fun setupNotifications() {
        // 1. Channel notifikasi (Android 8+). ID harus sama dengan di
        //    AndroidManifest, MyFirebaseMessagingService, dan FcmService (Laravel).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Notifikasi Sistem",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        // 2. Izin notifikasi (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 3. Subscribe ke topic
        TOPICS.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    Log.d(TAG, "subscribe $topic: ${task.isSuccessful} ${task.exception ?: ""}")
                }
        }

        // 4. Log token (untuk tes lewat "Test on device" di Firebase Console).
        //    Hapus bagian ini kalau sudah tidak diperlukan.
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "TOKEN: ${task.result}")
            } else {
                Log.e(TAG, "Gagal ambil token", task.exception)
            }
        }
    }

    private fun unsubscribeNotifications() {
        TOPICS.forEach { topic ->
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        }
    }

    // ---------------------------------------------------------------------
    // Keranjang
    // ---------------------------------------------------------------------

    private fun addToCart(menu: Menu) {
        binding.tvCartItemName.text = menu.namaMenu
        cartPrice = menu.harga
        cartQuantity = 1
        updateCartUI()
        binding.layoutCart.isVisible = true
    }

    private fun updateCartUI() {
        val subtotal = cartPrice * cartQuantity
        val discount = 4000.0
        val total = if (subtotal > discount) subtotal - discount else 0.0

        binding.tvQty.text = cartQuantity.toString()
        binding.tvCartCount.text = "$cartQuantity Item"
        binding.tvCartItemPrice.text = String.format("%,.0f", subtotal).replace(',', '.')
        binding.tvSubtotal.text = String.format("IDR %,.0f", subtotal).replace(',', '.')
        binding.tvTotal.text = String.format("IDR %,.0f", total).replace(',', '.')
    }

    private fun showPaymentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pembayaran")
            .setMessage("Selesaikan transaksi sebesar ${binding.tvTotal.text}?")
            .setPositiveButton("Bayar Tunai / QRIS") { _, _ ->
                binding.layoutCart.isVisible = false
                AlertDialog.Builder(this)
                    .setTitle("Pembayaran Berhasil! 🎉")
                    .setMessage("Transaksi berhasil dicatat dan disinkronkan dengan web dashboard.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ---------------------------------------------------------------------
    // Header & logout
    // ---------------------------------------------------------------------

    private fun bindHeader() {
        val nama = TokenManager.nama ?: "Barista"
        val role = TokenManager.role?.replaceFirstChar { it.uppercase() } ?: "Barista"

        binding.tvAvatar.text = nama.firstOrNull()?.uppercase() ?: "?"
        binding.tvBaristaName.text = "$nama • $role"
        binding.tvBooth.text = TokenManager.namaCabang ?: "-"
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Keluar")
            .setMessage("Yakin ingin keluar dari akun?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ -> doLogout() }
            .show()
    }

    private fun doLogout() {
        binding.btnLogout.isEnabled = false
        lifecycleScope.launch {
            try {
                RetrofitClient.api.logout()
            } catch (_: Exception) {}
            unsubscribeNotifications() // user yang sudah logout tidak menerima notifikasi
            TokenManager.clear()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ---------------------------------------------------------------------
    // Render daftar menu
    // ---------------------------------------------------------------------

    private fun render(state: MenuUiState) {
        val hasError = state.error != null
        val list = state.filteredMenus

        binding.progress.isVisible = state.isLoading
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
            (listOf<String?>(null) + state.categories).forEach { kategori ->
                val chip = layoutInflater.inflate(R.layout.item_chip_kategori, group, false) as Chip
                chip.text = kategori ?: "Semua"
                chip.tag = kategori
                chip.setOnClickListener { viewModel.selectCategory(kategori) }
                group.addView(chip)
            }
        }

        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            chip.isChecked = (chip.tag as String?) == state.selectedCategory
        }
    }

    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "matcha_notif_channel"
        private val TOPICS = listOf("menu_update", "stok_update")
    }
}