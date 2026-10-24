
package com.example.employeesalarymanagement

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class EmployeeManagementActivity : Activity() {

    private lateinit var db: DatabaseHelper
    private lateinit var listView: ListView
    private lateinit var searchInput: EditText

    private val employees = mutableListOf<String>()
    private val employeeIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = DatabaseHelper(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        root.addView(TextView(this).apply {
            text = "QUẢN LÝ NHÂN VIÊN"
            textSize = 22f
            setPadding(0, 0, 0, 16)
        })

        searchInput = EditText(this).apply {
            hint = "Tìm theo mã, tên hoặc email"
            setSingleLine(true)
        }
        root.addView(searchInput)

        val searchButton = Button(this).apply {
            text = "Tìm kiếm"
            isAllCaps = false
        }

        val addButton = Button(this).apply {
            text = "Thêm nhân viên"
            isAllCaps = false
        }

        val refreshButton = Button(this).apply {
            text = "Làm mới danh sách"
            isAllCaps = false
        }

        root.addView(searchButton)
        root.addView(addButton)
        root.addView(refreshButton)

        listView = ListView(this)
        root.addView(
            listView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        searchButton.setOnClickListener {
            loadEmployees(searchInput.text.toString().trim())
        }

        searchInput.setOnEditorActionListener { _, _, _ ->
            loadEmployees(searchInput.text.toString().trim())
            true
        }

        refreshButton.setOnClickListener {
            searchInput.setText("")
            loadEmployees()
        }

        addButton.setOnClickListener {
            showEmployeeDialog(null)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in employeeIds.indices) {
                showEmployeeOptions(employeeIds[position])
            }
        }

        loadEmployees()
    }

    private fun loadEmployees(keyword: String = "") {
        employees.clear()
        employeeIds.clear()

        val cursor = db.readableDatabase.rawQuery(
            """
            SELECT id, code, fullname, email, phone, hourlyRate
            FROM Employee
            WHERE code LIKE ? OR fullname LIKE ? OR email LIKE ?
            ORDER BY fullname
            """.trimIndent(),
            arrayOf("%$keyword%", "%$keyword%", "%$keyword%")
        )

        cursor.use {
            while (it.moveToNext()) {
                employeeIds.add(it.getInt(0))

                employees.add(
                    "Mã: ${it.getString(1)}\n" +
                            "Tên: ${it.getString(2)}\n" +
                            "Email: ${it.getString(3) ?: ""}\n" +
                            "Điện thoại: ${it.getString(4) ?: ""}\n" +
                            "Lương/giờ: ${it.getDouble(5)}"
                )
            }
        }

        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            employees
        )
    }

    private fun showEmployeeOptions(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Quản lý nhân viên")
            .setItems(arrayOf("Sửa thông tin", "Xóa nhân viên")) { _, choice ->
                when (choice) {
                    0 -> showEmployeeDialog(id)
                    1 -> confirmDelete(id)
                }
            }
            .show()
    }

    private fun showEmployeeDialog(id: Int?) {
        val fields = listOf(
            "Mã nhân viên",
            "Họ và tên",
            "Email",
            "Số điện thoại",
            "Lương theo giờ"
        )

        val inputs = fields.mapIndexed { index, hint ->
            EditText(this).apply {
                this.hint = hint
                setSingleLine(true)

                inputType = when (index) {
                    3 -> InputType.TYPE_CLASS_PHONE
                    4 -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL
                    else -> InputType.TYPE_CLASS_TEXT
                }
            }
        }

        if (id != null) {
            val cursor = db.readableDatabase.rawQuery(
                """
                SELECT code, fullname, email, phone, hourlyRate
                FROM Employee
                WHERE id = ?
                """.trimIndent(),
                arrayOf(id.toString())
            )

            cursor.use {
                if (it.moveToFirst()) {
                    for (i in inputs.indices) {
                        inputs[i].setText(
                            if (i == 4) {
                                it.getDouble(i).toString()
                            } else {
                                it.getString(i) ?: ""
                            }
                        )
                    }
                }
            }
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
            inputs.forEach { addView(it) }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (id == null) "Thêm nhân viên" else "Sửa nhân viên")
            .setView(container)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val code = inputs[0].text.toString().trim()
                    val name = inputs[1].text.toString().trim()
                    val email = inputs[2].text.toString().trim()
                    val phone = inputs[3].text.toString().trim()
                    val rate = inputs[4].text.toString().toDoubleOrNull()

                    if (code.isBlank() || name.isBlank() ||
                        rate == null || !rate.isFinite() || rate < 0
                    ) {
                        toast("Vui lòng nhập mã, tên và mức lương hợp lệ")
                        return@setOnClickListener
                    }

                    val values = ContentValues().apply {
                        put("code", code)
                        put("fullname", name)
                        put("email", email)
                        put("phone", phone)
                        put("hourlyRate", rate)
                    }

                    try {
                        val database = db.writableDatabase
                        database.beginTransaction()

                        try {
                            if (id == null) {
                                val account = ContentValues().apply {
                                    put("username", code)
                                    put("password", "123456")
                                    put("role", "Employee")
                                }

                                val accountId = database.insertOrThrow(
                                    "Account",
                                    null,
                                    account
                                )

                                values.put("accountId", accountId)

                                database.insertOrThrow(
                                    "Employee",
                                    null,
                                    values
                                )

                                database.setTransactionSuccessful()

                                toast(
                                    "Đã thêm nhân viên.\n" +
                                            "Tài khoản: $code\nMật khẩu: 123456"
                                )
                            } else {
                                val updated = database.update(
                                    "Employee",
                                    values,
                                    "id = ?",
                                    arrayOf(id.toString())
                                )

                                if (updated == 0) {
                                    throw IllegalStateException(
                                        "Không tìm thấy nhân viên"
                                    )
                                }

                                database.setTransactionSuccessful()
                                toast("Đã cập nhật nhân viên")
                            }
                        } finally {
                            database.endTransaction()
                        }

                        loadEmployees(searchInput.text.toString().trim())
                        dialog.dismiss()
                    } catch (e: SQLiteConstraintException) {
                        toast("Mã nhân viên hoặc tên tài khoản đã tồn tại")
                    } catch (e: Exception) {
                        toast("Không thể lưu nhân viên: ${e.message ?: "Lỗi dữ liệu"}")
                    }
                }
        }

        dialog.show()
    }

    private fun confirmDelete(id: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xóa nhân viên")
            .setMessage(
                "Bạn có chắc muốn xóa nhân viên này? " +
                        "Nếu có dữ liệu chấm công hoặc lương liên quan, " +
                        "cơ sở dữ liệu có thể từ chối thao tác."
            )
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                val database = db.writableDatabase

                try {
                    database.beginTransaction()

                    try {
                        var accountId: Long? = null

                        val cursor = database.rawQuery(
                            "SELECT accountId FROM Employee WHERE id = ?",
                            arrayOf(id.toString())
                        )

                        cursor.use {
                            if (it.moveToFirst() && !it.isNull(0)) {
                                accountId = it.getLong(0)
                            }
                        }

                        val deleted = database.delete(
                            "Employee",
                            "id = ?",
                            arrayOf(id.toString())
                        )

                        if (deleted == 0) {
                            throw IllegalStateException(
                                "Không tìm thấy nhân viên"
                            )
                        }

                        if (accountId != null) {
                            database.delete(
                                "Account",
                                "id = ?",
                                arrayOf(accountId.toString())
                            )
                        }

                        database.setTransactionSuccessful()
                    } finally {
                        database.endTransaction()
                    }

                    toast("Đã xóa nhân viên")
                    loadEmployees(searchInput.text.toString().trim())
                } catch (e: Exception) {
                    toast(
                        "Không thể xóa nhân viên. Có thể đang có dữ liệu " +
                                "chấm công hoặc lương liên quan."
                    )
                }
            }
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
