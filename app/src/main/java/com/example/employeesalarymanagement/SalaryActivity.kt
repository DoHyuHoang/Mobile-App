
package com.example.employeesalarymanagement

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.content.ContentValues
import java.util.Calendar
import java.util.Locale

class SalaryActivity : Activity() {

    private lateinit var db: DatabaseHelper
    private lateinit var employeeSpinner: Spinner
    private lateinit var monthInput: EditText
    private lateinit var yearInput: EditText
    private lateinit var resultView: TextView
    private lateinit var historyView: TextView

    private val employeeIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DatabaseHelper(this)

        val calendar = Calendar.getInstance()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 16, 24, 16)
        }

        root.addView(TextView(this).apply {
            text = "QUẢN LÝ LƯƠNG NHÂN VIÊN"
            textSize = 22f
            setPadding(0, 0, 0, 16)
        })

        employeeSpinner = Spinner(this)
        root.addView(employeeSpinner)

        monthInput = EditText(this).apply {
            hint = "Tháng (1-12)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText((calendar.get(Calendar.MONTH) + 1).toString())
            setSingleLine(true)
        }
        root.addView(monthInput)

        yearInput = EditText(this).apply {
            hint = "Năm"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(calendar.get(Calendar.YEAR).toString())
            setSingleLine(true)
        }
        root.addView(yearInput)

        val calculateButton = Button(this).apply {
            text = "Tính và lưu lương"
        }
        root.addView(calculateButton)

        resultView = TextView(this).apply {
            textSize = 16f
            setPadding(0, 12, 0, 12)
        }
        root.addView(resultView)

        val historyButton = Button(this).apply {
            text = "Xem bảng lương đã lưu"
        }
        root.addView(historyButton)

        historyView = TextView(this).apply {
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }

        val scroll = ScrollView(this).apply {
            addView(
                historyView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        // Gọi object NavigationHelper, không gọi tên file .kt.
        // Nếu object hiện tại chỉ có goBack() và logout(),
        // không gọi attach() ở đây.
        loadEmployees()

        calculateButton.setOnClickListener {
            calculateAndSaveSalary()
        }

        historyButton.setOnClickListener {
            historyView.text = loadSalaryHistory()
        }

        if (SessionManager(this).getRole() == "Employee") {
            employeeSpinner.isEnabled = false
        }
    }

    private fun loadEmployees() {
        val names = mutableListOf<String>()
        employeeIds.clear()

        val session = SessionManager(this)
        val role = session.getRole()
        val accountId = session.getAccountId()

        val cursor = if (role == "Employee") {
            db.readableDatabase.rawQuery(
                """
                SELECT id, fullname
                FROM Employee
                WHERE accountId = ?
                ORDER BY fullname
                """.trimIndent(),
                arrayOf(accountId.toString())
            )
        } else {
            db.readableDatabase.rawQuery(
                "SELECT id, fullname FROM Employee ORDER BY fullname",
                null
            )
        }

        cursor.use {
            while (it.moveToNext()) {
                employeeIds.add(it.getInt(0))
                names.add(it.getString(1) ?: "")
            }
        }

        employeeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        if (employeeIds.isEmpty()) {
            resultView.text = "Chưa có nhân viên."
        }
    }

    private fun calculateAndSaveSalary() {
        if (employeeIds.isEmpty()) {
            resultView.text = "Chưa có nhân viên để tính lương."
            return
        }

        val month = monthInput.text.toString().toIntOrNull()
        val year = yearInput.text.toString().toIntOrNull()

        if (month == null || month !in 1..12 ||
            year == null || year !in 2000..2100
        ) {
            resultView.text = "Vui lòng nhập tháng và năm hợp lệ."
            return
        }

        val position = employeeSpinner.selectedItemPosition
        if (position !in employeeIds.indices) {
            resultView.text = "Vui lòng chọn nhân viên hợp lệ."
            return
        }

        val employeeId = employeeIds[position]

        try {
            var hourlyRate = 0.0

            val rateCursor = db.readableDatabase.rawQuery(
                "SELECT hourlyRate FROM Employee WHERE id = ?",
                arrayOf(employeeId.toString())
            )

            rateCursor.use {
                if (it.moveToFirst()) {
                    hourlyRate = it.getDouble(0)
                } else {
                    resultView.text = "Không tìm thấy thông tin nhân viên."
                    return
                }
            }

            val dailyHours = mutableListOf<Double>()

            val workCursor = db.readableDatabase.rawQuery(
                """
                SELECT totalHours
                FROM WorkLog
                WHERE employeeId = ?
                  AND substr(workDate, 1, 4) = ?
                  AND substr(workDate, 6, 2) = ?
                  AND checkOutTime IS NOT NULL
                """.trimIndent(),
                arrayOf(
                    employeeId.toString(),
                    year.toString().padStart(4, '0'),
                    month.toString().padStart(2, '0')
                )
            )

            workCursor.use {
                while (it.moveToNext()) {
                    dailyHours.add(it.getDouble(0))
                }
            }

            val result = SalaryCalculator.calculate(dailyHours, hourlyRate)

            val values = ContentValues().apply {
                put("employeeId", employeeId)
                put("month", month)
                put("year", year)
                put("totalHours", result.totalHours)
                put("overtimeHours", result.overtimeHours)
                put("totalSalary", result.totalSalary)
                put("status", "Pending")
            }

            val existing = db.readableDatabase.rawQuery(
                """
                SELECT id FROM Salary
                WHERE employeeId = ? AND month = ? AND year = ?
                """.trimIndent(),
                arrayOf(
                    employeeId.toString(),
                    month.toString(),
                    year.toString()
                )
            )

            var salaryId: Int? = null
            existing.use {
                if (it.moveToFirst()) {
                    salaryId = it.getInt(0)
                }
            }

            if (salaryId == null) {
                db.writableDatabase.insertOrThrow(
                    "Salary",
                    null,
                    values
                )
            } else {
                db.writableDatabase.update(
                    "Salary",
                    values,
                    "id = ?",
                    arrayOf(salaryId.toString())
                )
            }

            resultView.text = """
                Tháng: $month/$year
                Tổng giờ làm: ${"%.2f".format(Locale.US, result.totalHours)} giờ
                Giờ tăng ca: ${"%.2f".format(Locale.US, result.overtimeHours)} giờ
                Lương theo giờ: ${"%,.0f".format(Locale.US, hourlyRate)}
                Tổng lương: ${"%,.0f".format(Locale.US, result.totalSalary)}
                Trạng thái: Chưa thanh toán
            """.trimIndent()

            Toast.makeText(
                this,
                "Đã lưu bảng lương.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            resultView.text = "Không thể lưu bảng lương: ${e.message}"
        }
    }

    private fun loadSalaryHistory(): String {
        val session = SessionManager(this)
        val role = session.getRole()
        val accountId = session.getAccountId()

        val query: String
        val args: Array<String>?

        if (role == "Employee") {
            query = """
                SELECT S.month, S.year, E.fullname,
                       S.totalHours, S.overtimeHours,
                       S.totalSalary, S.status
                FROM Salary S
                JOIN Employee E ON E.id = S.employeeId
                WHERE E.accountId = ?
                ORDER BY S.year DESC, S.month DESC
            """.trimIndent()

            args = arrayOf(accountId.toString())
        } else {
            query = """
                SELECT S.month, S.year, E.fullname,
                       S.totalHours, S.overtimeHours,
                       S.totalSalary, S.status
                FROM Salary S
                JOIN Employee E ON E.id = S.employeeId
                ORDER BY S.year DESC, S.month DESC, E.fullname
            """.trimIndent()

            args = null
        }

        val output = StringBuilder()
        val cursor = db.readableDatabase.rawQuery(query, args)

        cursor.use {
            while (it.moveToNext()) {
                output.append(
                    "${it.getString(2)} — ${it.getInt(0)}/${it.getInt(1)}\n" +
                            "Giờ làm: ${it.getDouble(3)} | " +
                            "Tăng ca: ${it.getDouble(4)}\n" +
                            "Lương: ${"%,.0f".format(Locale.US, it.getDouble(5))} | " +
                            "Trạng thái: ${it.getString(6)}\n\n"
                )
            }
        }

        return if (output.isEmpty()) {
            "Chưa có bảng lương nào được lưu."
        } else {
            output.toString()
        }
    }
}
