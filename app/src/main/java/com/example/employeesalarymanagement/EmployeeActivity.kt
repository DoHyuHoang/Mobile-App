
package com.example.employeesalarymanagement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class EmployeeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee)

        val session = SessionManager(this)

        // Kiểm tra đăng nhập và quyền nhân viên.
        if (!session.isLoggedIn() || session.getRole() != "Employee") {
            NavigationHelper.logout(this)
            return
        }

        // Hiển thị tên tài khoản.
        findViewById<TextView>(R.id.txtWelcome).text =
            "Welcome, ${session.getUsername()}"

        // Mở màn hình chấm công.
        findViewById<Button>(R.id.btnAttendance).setOnClickListener {
            startActivity(
                Intent(this, AttendanceActivity::class.java)
            )
        }

        // Mở lịch sử chấm công.
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            startActivity(
                Intent(this, AttendanceHistoryActivity::class.java)
            )
        }

        // Mở màn hình lương.
        findViewById<Button>(R.id.btnSalary).setOnClickListener {
            startActivity(
                Intent(this, SalaryActivity::class.java)
            )
        }

        // Đăng xuất và trở về màn hình đăng nhập.
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            NavigationHelper.logout(this)
        }
    }
}
