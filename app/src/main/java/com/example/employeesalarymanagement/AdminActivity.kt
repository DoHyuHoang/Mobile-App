
package com.example.employeesalarymanagement

import android.app.Activity
import android.content.Intent

object NavigationHelper {

    fun goBack(activity: Activity) {
        activity.finish()
    }

    fun logout(activity: Activity) {
        SessionManager(activity).logout()

        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        activity.startActivity(intent)
        activity.finish()
    }
}
