

package com.example.employeesalarymanagement

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "employee_management.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE Account (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('Admin','Employee'))
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE Employee (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL UNIQUE,
                fullname TEXT NOT NULL,
                email TEXT,
                phone TEXT,
                hourlyRate REAL NOT NULL CHECK(hourlyRate >= 0),
                accountId INTEGER NOT NULL UNIQUE,
                FOREIGN KEY(accountId) REFERENCES Account(id)
                    ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE WorkLog (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employeeId INTEGER NOT NULL,
                workDate TEXT NOT NULL,
                checkInTime TEXT NOT NULL,
                checkOutTime TEXT,
                totalHours REAL NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'Working',
                FOREIGN KEY(employeeId) REFERENCES Employee(id)
                    ON DELETE CASCADE,
                UNIQUE(employeeId, workDate)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE Salary (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                employeeId INTEGER NOT NULL,
                month INTEGER NOT NULL CHECK(month BETWEEN 1 AND 12),
                year INTEGER NOT NULL,
                totalHours REAL NOT NULL DEFAULT 0,
                overtimeHours REAL NOT NULL DEFAULT 0,
                totalSalary REAL NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'Pending',
                FOREIGN KEY(employeeId) REFERENCES Employee(id)
                    ON DELETE CASCADE,
                UNIQUE(employeeId, month, year)
            )
        """.trimIndent())

        // Tạo tài khoản Admin mặc định
        val admin = ContentValues().apply {
            put("username", "admin")
            put("password", "admin123")
            put("role", "Admin")
        }

        db.insertOrThrow("Account", null, admin)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Bổ sung migration khi thay đổi cấu trúc database.
    }

    fun login(username: String, password: String): Cursor {
        return readableDatabase.rawQuery(
            """
            SELECT id, username, role
            FROM Account
            WHERE username = ? AND password = ?
            """.trimIndent(),
            arrayOf(username, password)
        )
    }
}
