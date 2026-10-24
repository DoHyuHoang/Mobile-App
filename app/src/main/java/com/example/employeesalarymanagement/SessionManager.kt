

package com.example.employeesalarymanagement

import android.content.Context

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(
        "EmployeeSession",
        Context.MODE_PRIVATE
    )

    fun saveSession(id: Int, username: String, role: String) {
        preferences.edit()
            .putInt("account_id", id)
            .putString("username", username)
            .putString("role", role)
            .putBoolean("logged_in", true)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean("logged_in", false)
    }

    fun getAccountId(): Int {
        return preferences.getInt("account_id", -1)
    }

    fun getUsername(): String {
        return preferences.getString("username", "") ?: ""
    }

    fun getRole(): String {
        return preferences.getString("role", "") ?: ""
    }

    fun logout() {
        preferences.edit().clear().apply()
    }
}
