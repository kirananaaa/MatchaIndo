package com.example.machaindo.ui.stok

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.machaindo.data.model.StokItem
import com.example.machaindo.databinding.ItemStokBinding
import java.text.NumberFormat
import java.util.Locale

class StokAdapter : ListAdapter<StokItem, StokAdapter.VH>(Diff) {

    class VH(val b: ItemStokBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStokBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        // label, warna teks, warna latar badge
        val (label, teks, latar) = when (item.status) {
            "kritis"       -> Triple("Kritis", "#B91C1C", "#FEE2E2")
            "hampir_habis" -> Triple("Hampir Habis", "#B45309", "#FEF3C7")
            else           -> Triple("Aman", "#15803D", "#DCFCE7")
        }

        with(holder.b) {
            tvNamaBahan.text = item.namaBahan
            tvBatasMinimal.text = "Batas minimal: ${angka(item.stokMinimum)} ${item.satuan}"

            tvSisaStok.text = "${angka(item.jumlahStok)} ${item.satuan}"
            tvSisaStok.setTextColor(teks.toColorInt())

            tvStatus.text = label
            tvStatus.setTextColor(teks.toColorInt())
            tvStatus.backgroundTintList = ColorStateList.valueOf(latar.toColorInt())
        }
    }

    private fun angka(n: Int): String =
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(n)

    private object Diff : DiffUtil.ItemCallback<StokItem>() {
        override fun areItemsTheSame(a: StokItem, b: StokItem) = a.idBahan == b.idBahan
        override fun areContentsTheSame(a: StokItem, b: StokItem) = a == b
    }
}