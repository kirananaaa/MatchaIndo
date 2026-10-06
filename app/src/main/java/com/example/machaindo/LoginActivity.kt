package com.example.machaindo

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.machaindo.data.local.TokenManager
import com.example.machaindo.data.model.LoginRequest
import com.example.machaindo.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : ComponentActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvToggle: TextView
    private lateinit var tvError: TextView
    private lateinit var btnLogin: Button
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(this)

        // Aplikasi dibuka baru -> wajib login lagi
        if (savedInstanceState == null) {
            TokenManager.clear()
        }

        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        tvToggle = findViewById(R.id.tvTogglePassword)
        tvError = findViewById(R.id.tvError)
        btnLogin = findViewById(R.id.btnLogin)

        tvToggle.setOnClickListener { togglePassword() }
        btnLogin.setOnClickListener { doLogin() }
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doLogin()
                true
            } else false
        }
    }

    private fun togglePassword() {
        passwordVisible = !passwordVisible
        etPassword.transformationMethod =
            if (passwordVisible) HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
        etPassword.setSelection(etPassword.text.length)
        tvToggle.text = if (passwordVisible) "Sembunyikan" else "Lihat"
    }

    private fun doLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan kata sandi wajib diisi")
            return
        }

        tvError.visibility = View.GONE
        setLoading(true)

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.login(LoginRequest(username, password))
                TokenManager.token = res.token
                TokenManager.nama = res.user.nama
                TokenManager.role = res.user.role
                TokenManager.idCabang = res.user.idCabang ?: -1

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: HttpException) {
                showError(
                    when (e.code()) {
                        401, 403, 422 -> "Username atau kata sandi salah"
                        else -> "Terjadi kesalahan server (${e.code()})"
                    }
                )
            } catch (e: IOException) {
                showError("Tidak bisa terhubung ke server")
            } catch (e: Exception) {
                showError("Login gagal: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled = !loading
        btnLogin.alpha = if (loading) 0.6f else 1f
        btnLogin.text = if (loading) "Memproses..." else "Masuk ke Aplikasi"
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}
