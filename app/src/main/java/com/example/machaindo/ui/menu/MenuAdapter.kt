package com.example.machaindo.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.machaindo.data.model.Menu
import com.example.machaindo.databinding.ItemMenuBinding

class MenuAdapter(
    private val onClick: (Menu) -> Unit = {}   // dipakai nanti untuk tambah ke keranjang
) : ListAdapter<Menu, MenuAdapter.VH>(Diff) {

    class VH(val b: ItemMenuBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val menu = getItem(position)
        with(holder.b) {
            tvNama.text = menu.namaMenu
            tvKategori.text = menu.kategori
            tvInitial.text = menu.namaMenu.firstOrNull()?.uppercase() ?: "?"
            tvNonaktif.isVisible = !menu.isAktif
            root.alpha = if (menu.isAktif) 1f else 0.55f
            root.isEnabled = menu.isAktif
            root.setOnClickListener { if (menu.isAktif) onClick(menu) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Menu>() {
        override fun areItemsTheSame(a: Menu, b: Menu) = a.idMenu == b.idMenu
        override fun areContentsTheSame(a: Menu, b: Menu) = a == b
    }
}
