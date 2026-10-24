
package com.example.employeesalarymanagement

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 24, 24, 24)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "QUẢN LÝ LƯƠNG NHÂN VIÊN"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 28)
        }
        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addMenuButton(root, "Quản lý nhân viên") {
            openActivity(EmployeeManagementActivity::class.java)
        }

        addMenuButton(root, "Chấm công") {
            openActivity(AttendanceActivity::class.java)
        }

        addMenuButton(root, "Lịch sử chấm công") {
            openActivity(AttendanceHistoryActivity::class.java)
        }

        addMenuButton(root, "Quản lý lương") {
            openActivity(SalaryActivity::class.java)
        }

        addMenuButton(root, "Đăng xuất") {
            NavigationHelper.logout(this)
        }

        setContentView(root)
    }

    private fun addMenuButton(
        root: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

        root.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        )
    }

    private fun openActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }
}
