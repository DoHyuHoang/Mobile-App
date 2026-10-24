

package com.example.employeesalarymanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LoginActivity : Activity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        databaseHelper = DatabaseHelper(this)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            openDashboard(sessionManager.getRole())
            return
        }

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter username and password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            databaseHelper.login(username, password).use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("id")
                    )
                    val savedUsername = cursor.getString(
                        cursor.getColumnIndexOrThrow("username")
                    )
                    val role = cursor.getString(
                        cursor.getColumnIndexOrThrow("role")
                    )

                    sessionManager.saveSession(
                        id, savedUsername, role
                    )

                    openDashboard(role)
                } else {
                    Toast.makeText(
                        this,
                        "Invalid username or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openDashboard(role: String) {
        val intent = when (role) {
            "Admin" -> Intent(this, AdminActivity::class.java)
            "Employee" -> Intent(this, EmployeeActivity::class.java)
            else -> {
                sessionManager.logout()
                return
            }
        }

        startActivity(intent)
        finish()
    }
}
