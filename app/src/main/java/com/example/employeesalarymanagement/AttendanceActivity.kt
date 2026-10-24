
package com.example.employeesalarymanagement

import android.app.Activity
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceActivity : Activity() {

    private lateinit var helper: DatabaseHelper
    private lateinit var txtStatus: TextView
    private lateinit var btnCheckIn: Button
    private lateinit var btnCheckOut: Button

    private var employeeId = -1

    private val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        helper = DatabaseHelper(this)
        txtStatus = findViewById(R.id.txtAttendanceStatus)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)

        findViewById<TextView>(R.id.txtDate).text = "Date: $today"

        val session = SessionManager(this)
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT id FROM Employee WHERE accountId = ?",
            arrayOf(session.getAccountId().toString())
        )

        cursor.use {
            if (!it.moveToFirst()) {
                toast("Employee profile not found")
                finish()
                return
            }

            employeeId = it.getInt(0)
        }

        btnCheckIn.setOnClickListener { checkIn() }
        btnCheckOut.setOnClickListener { checkOut() }

        refreshStatus()
    }

    private fun checkIn() {
        val values = ContentValues().apply {
            put("employeeId", employeeId)
            put("workDate", today)
            put("checkInTime", currentTime())
            put("totalHours", 0.0)
            put("status", "Working")
        }

        try {
            val result = helper.writableDatabase.insertOrThrow(
                "WorkLog",
                null,
                values
            )

            if (result != -1L) {
                toast("Check-in successful")
            }
        } catch (e: SQLiteConstraintException) {
            toast("You have already checked in today")
        }

        refreshStatus()
    }

    private fun checkOut() {
        val db = helper.writableDatabase

        val cursor = db.rawQuery(
            """
            SELECT id, checkInTime FROM WorkLog
            WHERE employeeId = ? AND workDate = ?
            AND checkOutTime IS NULL
            """.trimIndent(),
            arrayOf(employeeId.toString(), today)
        )

        var logId = -1
        var checkInTime = ""

        cursor.use {
            if (it.moveToFirst()) {
                logId = it.getInt(0)
                checkInTime = it.getString(1)
            }
        }

        if (logId == -1) {
            toast("No pending check-in found")
            return
        }

        val checkOutTime = currentTime()
        val format = SimpleDateFormat("HH:mm:ss", Locale.US)

        val start = format.parse(checkInTime)
        val end = format.parse(checkOutTime)

        if (start == null || end == null || end <= start) {
            toast("Invalid check-out time")
            return
        }

        val hours = (end.time - start.time) / 3_600_000.0

        val values = ContentValues().apply {
            put("checkOutTime", checkOutTime)
            put("totalHours", hours)
            put("status", "Completed")
        }

        val updated = db.update(
            "WorkLog",
            values,
            "id = ? AND checkOutTime IS NULL",
            arrayOf(logId.toString())
        )

        if (updated == 1) {
            toast("Worked %.2f hours".format(Locale.US, hours))
        } else {
            toast("Attendance record was already updated")
        }

        refreshStatus()
    }

    private fun refreshStatus() {
        val cursor = helper.readableDatabase.rawQuery(
            """
            SELECT checkInTime, checkOutTime, totalHours
            FROM WorkLog
            WHERE employeeId = ? AND workDate = ?
            """.trimIndent(),
            arrayOf(employeeId.toString(), today)
        )

        cursor.use {
            if (it.moveToFirst()) {
                val checkIn = it.getString(0)
                val checkOut = it.getString(1)

                if (checkOut == null) {
                    txtStatus.text = "Checked in at: $checkIn"
                    btnCheckIn.isEnabled = false
                    btnCheckOut.isEnabled = true
                } else {
                    txtStatus.text =
                        "In: $checkIn\nOut: $checkOut\n" +
                                "Total hours: %.2f".format(
                                    Locale.US,
                                    it.getDouble(2)
                                )

                    btnCheckIn.isEnabled = false
                    btnCheckOut.isEnabled = false
                }
            } else {
                txtStatus.text = "Not checked in"
                btnCheckIn.isEnabled = true
                btnCheckOut.isEnabled = false
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
