package com.annaorazov.screenguard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.annaorazov.screenguard.databinding.ActivitySetPasswordBinding
import com.annaorazov.screenguard.utils.PasswordManager

class SetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetPasswordBinding
    private lateinit var passwordManager: PasswordManager
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SwitchLanguageHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewBinding
        if (!MainActivity.hasAllPermissions(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivitySetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        passwordManager = PasswordManager(this)

        // Проверка на наличие установленного пароля
        if (passwordManager.isPasswordSet()) {
            binding.editTextCurrentPassword.visibility = android.view.View.VISIBLE
        } else {
            binding.editTextCurrentPassword.visibility = android.view.View.GONE
        }

        binding.buttonSetPassword.setOnClickListener {
            val currentPassword = binding.editTextCurrentPassword.text.toString()
            val newPassword = binding.editTextNewPassword.text.toString()
            val confirmPassword = binding.editTextConfirmPassword.text.toString()

            // Проверка на заполненность полей
            if (newPassword.isEmpty() || confirmPassword.isEmpty() ||
                (passwordManager.isPasswordSet() && currentPassword.isEmpty())) {
                Toast.makeText(this,
                    getString(R.string.please_fill_up_all_places), Toast.LENGTH_SHORT).show()
            }
            // Проверка текущего пароля
            else if (passwordManager.isPasswordSet() && !passwordManager.validatePassword(currentPassword)) {
                Toast.makeText(this, getString(R.string.old_password_is_wrong), Toast.LENGTH_SHORT).show()
            }
            // Проверка совпадения паролей
            else if (newPassword != confirmPassword) {
                Toast.makeText(this, getString(R.string.password_is_wrong), Toast.LENGTH_SHORT).show()
            }
            // Установка нового пароля
            else {
                passwordManager.setPassword(newPassword)
                Toast.makeText(this,
                    getString(R.string.successfully_changed_password), Toast.LENGTH_SHORT).show()

                // Переход на главное окно
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
