package com.example.machaindo.ui.riwayat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.machaindo.R
import com.example.machaindo.data.local.TransactionRecord
import com.example.machaindo.data.local.TransactionRepository
import com.example.machaindo.databinding.ItemRiwayatBinding

class RiwayatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat)

        val btnBack = findViewById<Button>(R.id.btnBackToKasir)
        val rvRiwayat = findViewById<RecyclerView>(R.id.rvRiwayat)

        btnBack.setOnClickListener { finish() }

        rvRiwayat.layoutManager = LinearLayoutManager(this)
        rvRiwayat.adapter = RiwayatAdapter(TransactionRepository.history)
    }
}

class RiwayatAdapter(private val list: List<TransactionRecord>) :
    RecyclerView.Adapter<RiwayatAdapter.VH>() {

    class VH(val b: ItemRiwayatBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        with(holder.b) {
            tvTrxId.text = item.trxId
            tvMetode.text = item.metode
            tvTrxDetail.text = item.detail
            tvTrxTime.text = item.time
            tvTrxTotal.text = String.format("IDR %,.0f", item.total).replace(',', '.')
        }
    }

    override fun getItemCount() = list.size
}
