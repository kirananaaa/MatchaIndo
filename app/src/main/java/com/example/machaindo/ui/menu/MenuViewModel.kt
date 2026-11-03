package com.example.machaindo.ui.menu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.machaindo.data.model.HargaCup
import com.example.machaindo.data.model.Menu
import com.example.machaindo.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MenuUiState(
    val isLoading: Boolean = false,
    val menus: List<Menu> = emptyList(),
    val error: String? = null,
    val selectedCategory: String? = null   // null = semua
) {
    val categories: List<String>
        get() = menus.map { it.kategori }.filter { it.isNotEmpty() }.distinct().sorted()

    val filteredMenus: List<Menu>
        get() = if (selectedCategory == null) menus else menus.filter { it.kategori == selectedCategory }
}

class MenuViewModel : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    init { loadMenus() }

    fun loadMenus() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val element = RetrofitClient.api.getMenusJson()
                Log.d("MenuVM", "Raw menus response: $element")
                val menuList = mutableListOf<Menu>()

                val jsonArray = when {
                    element.isJsonArray -> element.asJsonArray
                    element.isJsonObject -> {
                        val obj = element.asJsonObject
                        when {
                            obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                            obj.has("menus") && obj.get("menus").isJsonArray -> obj.getAsJsonArray("menus")
                            obj.has("data") && obj.get("data").isJsonObject -> {
                                val inner = obj.getAsJsonObject("data")
                                if (inner.has("data") && inner.get("data").isJsonArray) inner.getAsJsonArray("data") else null
                            }
                            else -> null
                        }
                    }
                    else -> null
                }

                jsonArray?.forEach { jsonElement ->
                    try {
                        val item = jsonElement.asJsonObject
                        val id = item.get("id")?.asInt ?: item.get("id_menu")?.asInt ?: 1
                        val nama = item.get("nama_menu")?.asString ?: item.get("name")?.asString ?: "Menu"
                        val kat = item.get("kategori")?.asString ?: item.get("category")?.asString ?: "Umum"
                        
                        val isAktifVal = item.get("is_aktif")?.asBoolean 
                            ?: item.get("is_active")?.asBoolean 
                            ?: (item.get("status")?.asString?.lowercase() in listOf("aktif", "active", "1", "true"))

                        val hargaElement = item.get("harga")
                        var hargaVal = 20000.0
                        val hargaMenusList = mutableListOf<HargaCup>()

                        if (hargaElement != null) {
                            if (hargaElement.isJsonPrimitive && hargaElement.asJsonPrimitive.isNumber) {
                                hargaVal = hargaElement.asDouble
                            } else if (hargaElement.isJsonArray) {
                                val cupArray = hargaElement.asJsonArray
                                var minHarga = Double.MAX_VALUE
                                cupArray.forEach { cupElement ->
                                    try {
                                        val cupObj = cupElement.asJsonObject
                                        val cupId = cupObj.get("id_cup")?.asInt ?: 1
                                        val cupName = cupObj.get("nama_cup")?.asString ?: "Regular"
                                        val vol = cupObj.get("volume_ml")?.asInt ?: 0
                                        val h = cupObj.get("harga")?.asDouble ?: 15000.0
                                        if (h < minHarga) minHarga = h
                                        hargaMenusList.add(HargaCup(cupId, cupName, vol, h))
                                    } catch (_: Exception) {}
                                }
                                if (minHarga != Double.MAX_VALUE) {
                                    hargaVal = minHarga
                                }
                            }
                        }

                        val rawGambar = item.get("gambar")?.asString 
                            ?: item.get("image")?.asString 
                            ?: item.get("photo")?.asString

                        val gambarVal = when {
                            rawGambar.isNullOrEmpty() -> null
                            rawGambar.startsWith("http") -> rawGambar
                            else -> "http://192.168.1.8:8000/" + rawGambar.removePrefix("/")
                        }

                        menuList.add(Menu(idMenu = id, namaMenu = nama, kategori = kat, isAktif = isAktifVal, harga = hargaVal, hargaMenus = hargaMenusList, gambar = gambarVal))
                    } catch (e: Exception) {
                        Log.e("MenuVM", "Error parsing menu item", e)
                    }
                }

                _state.update { it.copy(isLoading = false, menus = menuList) }
            } catch (e: Exception) {
                Log.e("MenuVM", "Gagal memuat menu dari web", e)
                _state.update {
                    it.copy(isLoading = false, error = "Gagal memuat menu: ${e.localizedMessage}")
                }
            }
        }
    }

    fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }
}
