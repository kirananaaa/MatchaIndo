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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import com.example.machaindo.data.local.PendingTransaction
import com.example.machaindo.data.local.TokenManager
import com.example.machaindo.data.local.TransactionDatabase
import com.example.machaindo.data.local.TransactionRecord
import com.example.machaindo.data.local.TransactionRepository
import com.example.machaindo.data.local.TransactionSyncRepository
import com.example.machaindo.data.local.TransactionSyncResult
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class CheckoutCartItem(
    val menu: Menu,
    val cupId: Int,
    val unitPrice: Double,
    var quantity: Int = 1
)

private data class SyncProgressDialog(
    val dialog: AlertDialog,
    val progressBar: ProgressBar,
    val progressText: TextView
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MenuViewModel by viewModels()
    private lateinit var adapter: MenuAdapter

    private var renderedCategories: List<String>? = null
    private val cartItems = mutableListOf<CheckoutCartItem>()

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d(TAG, "Izin notifikasi diberikan: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)
        TransactionSyncRepository.init(this)

        if (TokenManager.token.isNullOrEmpty() || TokenManager.idUser <= 0) {
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
            cartItems.lastOrNull()?.let { item ->
                item.quantity++
                updateCartUI()
            }
        }
        binding.btnMin.setOnClickListener {
            cartItems.lastOrNull()?.let { item ->
                item.quantity--
                if (item.quantity <= 0) cartItems.remove(item)
                if (cartItems.isEmpty()) {
                    binding.layoutCart.isVisible = false
                } else {
                    updateCartUI()
                }
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

        binding.root.post { promptPendingTransactionRetries() }
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
        val selectedCup = menu.hargaMenus?.minByOrNull { it.harga }
        if (selectedCup == null) {
            Toast.makeText(this, "Harga menu belum tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        val existingItem = cartItems.firstOrNull {
            it.menu.idMenu == menu.idMenu && it.cupId == selectedCup.idCup
        }
        if (existingItem != null) {
            existingItem.quantity++
        } else {
            cartItems.add(CheckoutCartItem(menu, selectedCup.idCup, selectedCup.harga))
        }
        updateCartUI()
        binding.layoutCart.isVisible = true
    }

    private fun updateCartUI() {
        val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
        val discount = 4000.0
        val total = if (subtotal > discount) subtotal - discount else 0.0
        val totalQuantity = cartItems.sumOf { it.quantity }

        binding.tvCartItemName.text = cartItems.joinToString("\n") {
            "${it.quantity}x ${it.menu.namaMenu}"
        }
        binding.tvQty.text = (cartItems.lastOrNull()?.quantity ?: 0).toString()
        binding.tvCartCount.text = "$totalQuantity Item"
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
        val progressCheckout = dialogView.findViewById<ProgressBar>(R.id.progressCheckout)
        val tvCheckoutProgress = dialogView.findViewById<TextView>(R.id.tvCheckoutProgress)
        val tvQrisNominal = dialogView.findViewById<TextView>(R.id.tvQrisNominal)

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
                val currentTotal = binding.tvTotal.text.toString()
                    .replace("IDR ", "").replace(".", "").toDoubleOrNull() ?: 0.0
                val change = input - currentTotal
                tvKembalian.text = if (change >= 0) String.format("IDR %,.0f", change).replace(',', '.') else "Kurang"
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnPas.setOnClickListener {
            val currentTotal = binding.tvTotal.text.toString()
                .replace("IDR ", "").replace(".", "").toDoubleOrNull() ?: 0.0
            etUangDiterima.setText(currentTotal.toInt().toString())
        }
        btn50k.setOnClickListener { etUangDiterima.setText("50000") }
        btn100k.setOnClickListener { etUangDiterima.setText("100000") }

        var checkoutInProgress = false
        btnSelesaikan.setOnClickListener {
            if (checkoutInProgress) return@setOnClickListener

            val metode = if (isTunai) "tunai" else "qris"
            if (cartItems.isEmpty()) {
                showCheckoutValidationError("Keranjang masih kosong.")
                return@setOnClickListener
            }
            val detailTrx = cartItems.joinToString(", ") {
                "${it.quantity}x ${it.menu.namaMenu}"
            }
            val trxId = "#KK-${(1000000..9999999).random()}"
            val timeNow = android.text.format.DateFormat.format("dd MMM yyyy, HH:mm", java.util.Date()).toString()
            val totalAmount = binding.tvTotal.text.toString()
                .replace("IDR ", "").replace(".", "").toDoubleOrNull() ?: 0.0
            val uangDiterimaVal = etUangDiterima.text.toString().toDoubleOrNull() ?: totalAmount
            val idCabang = TokenManager.idCabang
            val idUser = TokenManager.idUser

            if (idCabang <= 0) {
                showCheckoutValidationError(
                    "Akun barista belum ditautkan ke cabang. Tambahkan data cabang di database, " +
                        "lalu isi pengguna.id_cabang dengan ID cabang tersebut."
                )
                return@setOnClickListener
            }
            if (idUser <= 0) {
                showCheckoutValidationError("ID akun tidak valid. Silakan login ulang.")
                return@setOnClickListener
            }

            val request = TransaksiRequest(
                idCabang = idCabang.toLong(),
                idUser = idUser.toLong(),
                totalBayar = totalAmount,
                metodeBayar = metode,
                uangDiterima = if (isTunai) uangDiterimaVal else null,
                items = cartItems.map { item ->
                    DetailTransaksiRequest(
                        idMenu = item.menu.idMenu.toLong(),
                        idCup = item.cupId.toLong(),
                        jumlah = item.quantity,
                        hargaSatuan = item.unitPrice
                    )
                }
            )

            val paymentLabel = if (isTunai) "Tunai (Cash)" else "QRIS Scan"
            val pendingTransaction = PendingTransaction(
                trxId = trxId,
                request = request,
                failedDatabases = TransactionDatabase.values().toList()
            )

            checkoutInProgress = true
            btnSelesaikan.isEnabled = false
            progressCheckout.progress = 0
            tvCheckoutProgress.text = "Menyimpan transaksi: 0%"
            progressCheckout.isVisible = true
            tvCheckoutProgress.isVisible = true
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)

            lifecycleScope.launch {
                try {
                    val result = TransactionSyncRepository.synchronize(pendingTransaction) { progress ->
                        progressCheckout.progress = progress
                        tvCheckoutProgress.text = "Menyimpan transaksi: $progress%"
                    }
                    TransactionRepository.addTransaction(
                        TransactionRecord(trxId, detailTrx, totalAmount, paymentLabel, timeNow)
                    )
                    dialog.dismiss()
                    cartItems.clear()
                    binding.layoutCart.isVisible = false
                    showTransactionSyncResult(result, paymentLabel, binding.tvTotal.text.toString())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    Log.e(TAG, "Gagal menyimpan transaksi", exception)
                    dialog.dismiss()
                    showSyncError(exception.message ?: "Terjadi kesalahan yang tidak diketahui")
                }
            }
        }

        dialog.show()
    }

    private fun showTransactionSyncResult(
        result: TransactionSyncResult,
        paymentMethod: String,
        total: String
    ) {
        if (result.failedDatabases.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Pembayaran Berhasil! 🎉")
                .setMessage(
                    "Transaksi ${result.trxId} via $paymentMethod senilai $total berhasil disimpan ke MySQL dan Firebase."
                )
                .setPositiveButton("Cetak Struk", null)
                .setNegativeButton("Transaksi Baru", null)
                .show()
            return
        }

        val failedNames = result.failedDatabases.joinToString { it.displayName }
        val succeededNames = TransactionDatabase.values()
            .filterNot { it in result.failedDatabases }
            .joinToString { it.displayName }
        val failureDetails = result.errors.entries.joinToString("\n") { (database, message) ->
            "${database.displayName}: $message"
        }
        val title = if (result.failedDatabases.size == TransactionDatabase.values().size) {
            "Transaksi gagal"
        } else {
            "Sebagian gagal"
        }
        val status = if (succeededNames.isEmpty()) {
            "Gagal menyimpan ke $failedNames."
        } else {
            "Berhasil disimpan ke $succeededNames, tetapi gagal ke $failedNames."
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(
                "Transaksi ${result.trxId}. $status\n${failureDetails}\nKegagalan sudah dicatat dan dapat dicoba lagi."
            )
            .setPositiveButton("Coba lagi") { _, _ ->
                val pending = TransactionSyncRepository.pendingTransactions()
                    .firstOrNull { it.trxId == result.trxId }
                if (pending != null) retryTransaction(pending, paymentMethod, total)
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun retryTransaction(
        pending: PendingTransaction,
        paymentMethod: String,
        total: String
    ) {
        val progressDialog = createSyncProgressDialog(
            "Mencoba ulang transaksi",
            pending.trxId
        )

        lifecycleScope.launch {
            try {
                val result = TransactionSyncRepository.synchronize(pending) { progress ->
                    updateSyncProgress(progressDialog, progress)
                }
                progressDialog.dialog.dismiss()
                showTransactionSyncResult(result, paymentMethod, total)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "Gagal mencoba ulang transaksi ${pending.trxId}", exception)
                progressDialog.dialog.dismiss()
                showSyncError(exception.message ?: "Terjadi kesalahan yang tidak diketahui")
            }
        }
    }

    private fun promptPendingTransactionRetries() {
        val pending = TransactionSyncRepository.pendingTransactions()
        if (pending.isEmpty()) return

        val databases = pending.flatMap { it.failedDatabases }.distinct()
            .joinToString { it.displayName }
        AlertDialog.Builder(this)
            .setTitle("Sinkronisasi tertunda")
            .setMessage(
                "${pending.size} transaksi belum tersimpan ke $databases. Transaksi ini sudah dicatat untuk dicoba ulang."
            )
            .setPositiveButton("Coba lagi") { _, _ -> retryPendingTransactions(pending) }
            .setNegativeButton("Nanti", null)
            .show()
    }

    private fun retryPendingTransactions(pending: List<PendingTransaction>) {
        val progressDialog = createSyncProgressDialog(
            "Mencoba ulang transaksi",
            "Mengirim ${pending.size} transaksi..."
        )

        lifecycleScope.launch {
            try {
                val results = mutableListOf<TransactionSyncResult>()
                pending.forEachIndexed { index, transaction ->
                    results += TransactionSyncRepository.synchronize(transaction) { progress ->
                        val overallProgress = (index * 100 + progress) / pending.size
                        updateSyncProgress(progressDialog, overallProgress)
                    }
                }
                progressDialog.dialog.dismiss()
                val remaining = results.mapNotNull { result ->
                    TransactionSyncRepository.pendingTransactions()
                        .firstOrNull { it.trxId == result.trxId }
                }
                val summary = results.joinToString("\n") { result ->
                    if (result.failedDatabases.isEmpty()) {
                        "${result.trxId}: tersimpan ke MySQL dan Firebase."
                    } else {
                        "${result.trxId}: gagal ke ${
                            result.failedDatabases.joinToString { it.displayName }
                        }."
                    }
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(if (remaining.isEmpty()) "Sinkronisasi selesai" else "Sebagian masih gagal")
                    .setMessage(summary)
                    .setPositiveButton(if (remaining.isEmpty()) "Tutup" else "Coba lagi") { _, _ ->
                        if (remaining.isNotEmpty()) retryPendingTransactions(remaining)
                    }
                    .setNegativeButton(if (remaining.isEmpty()) "OK" else "Nanti", null)
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "Gagal mencoba ulang transaksi tertunda", exception)
                progressDialog.dialog.dismiss()
                showSyncError(exception.message ?: "Terjadi kesalahan yang tidak diketahui")
            }
        }
    }

    private fun createSyncProgressDialog(title: String, message: String): SyncProgressDialog {
        val progressText = TextView(this).apply {
            text = "$message\n0%"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.matcha_text))
        }
        val progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressTintList = ContextCompat.getColorStateList(
                this@MainActivity,
                R.color.green_dark
            )
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding / 2)
            addView(
                progressText,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                progressBar,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (12 * resources.displayMetrics.density).toInt()
                ).apply {
                    topMargin = (12 * resources.displayMetrics.density).toInt()
                }
            )
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setCancelable(false)
            .create()
            .also { it.show() }

        return SyncProgressDialog(dialog, progressBar, progressText)
    }

    private fun updateSyncProgress(progressDialog: SyncProgressDialog, progress: Int) {
        progressDialog.progressBar.progress = progress
        progressDialog.progressText.text = "Sinkronisasi database: $progress%"
    }

    private fun showSyncError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Transaksi belum tersinkron")
            .setMessage("$message\nPeriksa kembali lalu coba lagi.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCheckoutValidationError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Data transaksi belum lengkap")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
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
