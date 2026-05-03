package com.annaorazov.screenguard.utils

import android.content.Context
import android.content.SharedPreferences

class PasswordManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "PasswordPrefs"
        private const val KEY_PASSWORD = "app_password"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Save the password
    fun setPassword(password: String) {
        sharedPreferences.edit().putString(KEY_PASSWORD, password).apply()
    }

    // Check if a password is set
    fun isPasswordSet(): Boolean {
        return sharedPreferences.contains(KEY_PASSWORD)
    }

    // Validate the entered password
    fun validatePassword(inputPassword: String): Boolean {
        val savedPassword = sharedPreferences.getString(KEY_PASSWORD, null)
        return inputPassword == savedPassword
    }

    // Clear the password (optional, for resetting)
    fun clearPassword() {
        sharedPreferences.edit().remove(KEY_PASSWORD).apply()
    }
}