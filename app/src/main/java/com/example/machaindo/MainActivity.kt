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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
import com.example.machaindo.data.local.TransactionRecord
import com.example.machaindo.data.local.TransactionRepository
import com.example.machaindo.data.model.DetailTransaksiRequest
import com.example.machaindo.data.model.Menu
import com.example.machaindo.data.model.TransaksiRequest
import com.example.machaindo.data.remote.RetrofitClient
import com.example.machaindo.databinding.ActivityMainBinding
import com.example.machaindo.ui.absensi.AbsensiActivity
import com.example.machaindo.ui.menu.MenuAdapter
import com.example.machaindo.ui.menu.MenuUiState
import com.example.machaindo.ui.menu.MenuViewModel
import com.example.machaindo.ui.riwayat.RiwayatActivity
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
        setupNotifications()

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
        binding.navRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        viewModel.loadMenus()
    }

    private fun setupNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Notifikasi Sistem",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        TOPICS.forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
        }
    }

    private fun unsubscribeNotifications() {
        TOPICS.forEach { topic ->
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        }
    }

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDialogTotal)
        val btnTunai = dialogView.findViewById<Button>(R.id.btnTabTunai)
        val btnQris = dialogView.findViewById<Button>(R.id.btnTabQris)
        val layoutTunai = dialogView.findViewById<LinearLayout>(R.id.layoutTunai)
        val layoutQris = dialogView.findViewById<LinearLayout>(R.id.layoutQris)
        val etUangDiterima = dialogView.findViewById<EditText>(R.id.etUangDiterima)
        val tvKembalian = dialogView.findViewById<TextView>(R.id.tvKembalian)
        val btnPas = dialogView.findViewById<Button>(R.id.btnPas)
        val btn50k = dialogView.findViewById<Button>(R.id.btn50k)
        val btn100k = dialogView.findViewById<Button>(R.id.btn100k)
        val btnSelesaikan = dialogView.findViewById<Button>(R.id.btnSelesaikanTransaksi)
        val tvQrisNominal = dialogView.findViewById<TextView>(R.id.tvQrisNominal)

        val totalAmount = binding.tvTotal.text.toString().replace("IDR ", "").replace(".", "").toDoubleOrNull() ?: 40000.0
        tvTotal.text = binding.tvTotal.text
        tvQrisNominal.text = "Nominal QRIS: ${binding.tvTotal.text}"

        var isTunai = true

        btnTunai.setOnClickListener {
            isTunai = true
            btnTunai.setBackgroundColor(ContextCompat.getColor(this, R.color.matcha_green))
            btnTunai.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnQris.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            btnQris.setTextColor(ContextCompat.getColor(this, R.color.matcha_text))
            layoutTunai.isVisible = true
            layoutQris.isVisible = false
        }

        btnQris.setOnClickListener {
            isTunai = false
            btnQris.setBackgroundColor(ContextCompat.getColor(this, R.color.matcha_green))
            btnQris.setTextColor(ContextCompat.getColor(this, R.color.white))
            btnTunai.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            btnTunai.setTextColor(ContextCompat.getColor(this, R.color.matcha_text))
            layoutTunai.isVisible = false
            layoutQris.isVisible = true
        }

        etUangDiterima.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().toDoubleOrNull() ?: 0.0
                val change = input - totalAmount
                tvKembalian.text = if (change >= 0) String.format("IDR %,.0f", change).replace(',', '.') else "Kurang"
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnPas.setOnClickListener { etUangDiterima.setText(totalAmount.toInt().toString()) }
        btn50k.setOnClickListener { etUangDiterima.setText("50000") }
        btn100k.setOnClickListener { etUangDiterima.setText("100000") }

        btnSelesaikan.setOnClickListener {
            val metode = if (isTunai) "tunai" else "qris"
            val detailTrx = "${binding.tvQty.text}x ${binding.tvCartItemName.text}"
            val trxId = "#KK-${(1000000..9999999).random()}"
            val timeNow = android.text.format.DateFormat.format("dd MMM yyyy, HH:mm", java.util.Date()).toString()
            val uangDiterimaVal = etUangDiterima.text.toString().toDoubleOrNull() ?: totalAmount

            val request = TransaksiRequest(
                idCabang = TokenManager.idCabang.toLong().let { if (it == -1L) 1L else it },
                idUser = 1L,
                totalBayar = totalAmount,
                metodeBayar = metode,
                uangDiterima = if (isTunai) uangDiterimaVal else null,
                items = listOf(
                    DetailTransaksiRequest(
                        idMenu = 1L,
                        idCup = 1L,
                        jumlah = binding.tvQty.text.toString().toIntOrNull() ?: 1,
                        hargaSatuan = totalAmount / (binding.tvQty.text.toString().toIntOrNull() ?: 1)
                    )
                )
            )

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.submitCheckout(request)
                    if (response.isSuccessful) {
                        Log.d("CHECKOUT", "Checkout berhasil ke server web")
                    }
                } catch (e: Exception) {
                    Log.e("CHECKOUT", "Gagal sync ke server: ${e.message}")
                }
            }

            TransactionRepository.addTransaction(
                TransactionRecord(trxId, detailTrx, totalAmount, if (isTunai) "Tunai (Cash)" else "QRIS Scan", timeNow)
            )

            dialog.dismiss()
            binding.layoutCart.isVisible = false

            AlertDialog.Builder(this)
                .setTitle("Pembayaran Berhasil! 🎉")
                .setMessage("Transaksi $trxId via ${if (isTunai) "Tunai (Cash)" else "QRIS Scan"} senilai ${binding.tvTotal.text} berhasil dicatat dan disinkronkan ke web.")
                .setPositiveButton("Cetak Struk", null)
                .setNegativeButton("Transaksi Baru", null)
                .show()
        }

        dialog.show()
    }

    private fun bindHeader() {
        val nama = TokenManager.nama ?: "Barista"
        val role = TokenManager.role?.replaceFirstChar { it.uppercase() } ?: "Barista"

        binding.tvAvatar.text = nama.firstOrNull()?.uppercase() ?: "?"
        binding.tvBaristaName.text = "$nama • $role"
        binding.tvBooth.text = TokenManager.namaCabang ?: "KK - Booth Senopati"
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
            unsubscribeNotifications()
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
