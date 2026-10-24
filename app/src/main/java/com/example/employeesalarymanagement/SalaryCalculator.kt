
package com.example.employeesalarymanagement

data class SalaryResult(
    val totalHours: Double,
    val overtimeHours: Double,
    val totalSalary: Double
)

object SalaryCalculator {

    fun calculate(
        dailyHours: List<Double>,
        hourlyRate: Double,
        overtimeRate: Double = 1.5
    ): SalaryResult {
        require(hourlyRate >= 0) {
            "Hourly rate cannot be negative"
        }
        require(overtimeRate >= 0) {
            "Overtime rate cannot be negative"
        }
        require(dailyHours.all { it >= 0 }) {
            "Working hours cannot be negative"
        }

        val totalHours = dailyHours.sum()
        val overtimeHours = dailyHours.sumOf {
            maxOf(0.0, it - 8.0)
        }

        val regularHours = totalHours - overtimeHours
        val salary = regularHours * hourlyRate +
                overtimeHours * hourlyRate * overtimeRate

        return SalaryResult(
            totalHours = totalHours,
            overtimeHours = overtimeHours,
            totalSalary = salary
        )
    }
}
