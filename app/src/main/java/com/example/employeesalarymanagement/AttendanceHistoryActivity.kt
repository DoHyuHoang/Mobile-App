
package com.example.employeesalarymanagement

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class AttendanceHistoryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_history)

        val helper = DatabaseHelper(this)
        val session = SessionManager(this)

        val edtMonth = findViewById<EditText>(R.id.edtMonth)
        val edtYear = findViewById<EditText>(R.id.edtYear)
        val txtTotal = findViewById<TextView>(R.id.txtMonthlyTotal)
        val list = findViewById<ListView>(R.id.listAttendance)
        val btnSearch = findViewById<Button>(R.id.btnSearchHistory)

        val calendar = Calendar.getInstance()
        edtMonth.setText((calendar.get(Calendar.MONTH) + 1).toString())
        edtYear.setText(calendar.get(Calendar.YEAR).toString())

        btnSearch.setOnClickListener {
            val month = edtMonth.text.toString().toIntOrNull()
            val year = edtYear.text.toString().toIntOrNull()

            if (month == null || month !in 1..12 ||
                year == null || year !in 2000..2100
            ) {
                Toast.makeText(
                    this,
                    "Tháng hoặc năm không hợp lệ",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val cursor = helper.readableDatabase.rawQuery(
                """
                SELECT w.workDate, w.checkInTime,
                       w.checkOutTime, w.totalHours
                FROM WorkLog w
                JOIN Employee e ON e.id = w.employeeId
                WHERE e.accountId = ?
                  AND substr(w.workDate, 6, 2) = ?
                  AND substr(w.workDate, 1, 4) = ?
                ORDER BY w.workDate DESC
                """.trimIndent(),
                arrayOf(
                    session.getAccountId().toString(),
                    String.format(Locale.US, "%02d", month),
                    year.toString()
                )
            )

            val rows = mutableListOf<String>()
            var totalHours = 0.0

            cursor.use {
                while (it.moveToNext()) {
                    val date = it.getString(0)
                    val checkIn = it.getString(1) ?: "--:--:--"
                    val checkOut = it.getString(2) ?: "Chưa check-out"
                    val hours = it.getDouble(3)

                    totalHours += hours

                    rows.add(
                        "$date\n" +
                                "Vào: $checkIn | Ra: $checkOut\n" +
                                "Số giờ: ${String.format(Locale.US, "%.2f", hours)}"
                    )
                }
            }

            txtTotal.text = String.format(
                Locale.US,
                "Tổng giờ làm: %.2f",
                totalHours
            )

            list.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                rows
            )

            if (rows.isEmpty()) {
                Toast.makeText(
                    this,
                    "Không có dữ liệu chấm công trong tháng này",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Tự động tìm dữ liệu của tháng hiện tại.
        btnSearch.performClick()
    }
}
