package com.example.machaindo.ui.menu

import android.util.Log
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
                // Gson membaca langsung ke model Menu (sesuai MenuResource Laravel)
                val menus = RetrofitClient.api.getMenus().data
                _state.update { it.copy(isLoading = false, menus = menus) }
            } catch (e: Exception) {
                Log.e("MenuVM", "Gagal memuat menu", e)
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