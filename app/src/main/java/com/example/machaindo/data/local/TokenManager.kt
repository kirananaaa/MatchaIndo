package com.example.machaindo.data.local

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences("auth_pref", Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(v) = prefs.edit().putString("token", v).apply()

    var nama: String?
        get() = prefs.getString("nama", null)
        set(v) = prefs.edit().putString("nama", v).apply()

    var role: String?
        get() = prefs.getString("role", null)
        set(v) = prefs.edit().putString("role", v).apply()

    var idCabang: Int
        get() = prefs.getInt("id_cabang", -1)
        set(v) = prefs.edit().putInt("id_cabang", v).apply()

    var namaCabang: String?
        get() = prefs.getString("nama_cabang", null)
        set(v)= prefs.edit().putString("nama_cabang", v).apply()

    fun clear() = prefs.edit().clear().apply()
}