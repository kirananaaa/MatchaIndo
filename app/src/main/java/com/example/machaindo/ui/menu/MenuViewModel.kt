package com.example.machaindo.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val categories: List<String> get() = menus.map { it.kategori }.filter { it.isNotEmpty() }.distinct().sorted()
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
                // 100% dinamis mengambil data langsung dari Web API Laravel (/api/menus)
                val element = RetrofitClient.api.getMenusJson()
                val menuList = mutableListOf<Menu>()

                val jsonArray = when {
                    element.isJsonArray -> element.asJsonArray
                    element.isJsonObject -> {
                        val obj = element.asJsonObject
                        when {
                            obj.has("data") && obj.get("data").isJsonArray -> obj.getAsJsonArray("data")
                            obj.has("menus") && obj.get("menus").isJsonArray -> obj.getAsJsonArray("menus")
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

                        val hargaVal = item.get("harga")?.asDouble 
                            ?: item.get("price")?.asDouble 
                            ?: 20000.0

                        val rawGambar = item.get("gambar")?.asString 
                            ?: item.get("image")?.asString 
                            ?: item.get("photo")?.asString

                        val gambarVal = when {
                            rawGambar.isNullOrEmpty() -> null
                            rawGambar.startsWith("http") -> rawGambar
                            else -> "http://192.168.1.9:8000/" + rawGambar.removePrefix("/")
                        }

                        menuList.add(Menu(id, nama, kat, isAktifVal, hargaVal, gambarVal))
                    } catch (_: Exception) {}
                }

                _state.update { it.copy(isLoading = false, menus = menuList) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Gagal memuat menu dari web: ${e.localizedMessage}")
                }
            }
        }
    }

    fun selectCategory(category: String?) {
        _state.update { it.copy(selectedCategory = category) }
    }
}
