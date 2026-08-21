package com.salmanlaghari.pulsemusicplayerai.presentation.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.salmanlaghari.pulsemusicplayerai.R
import com.salmanlaghari.pulsemusicplayerai.utils.CrashLogger

class CrashDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        val textView = TextView(this).apply {
            setTextColor(0xFFFF4444.toInt())
            setBackgroundColor(0xFF000000.toInt())
            textSize = 10f
            setPadding(32, 32, 32, 32)
        }
        setContentView(textView)

        val crashMessage = intent.getStringExtra(EXTRA_CRASH_MESSAGE) ?: "Unknown crash"
        val crashClass = intent.getStringExtra(EXTRA_CRASH_CLASS) ?: "Unknown"
        val logContent = CrashLogger.readLog(this)

        textView.text = buildString {
            appendLine("CRASH DETECTED")
            appendLine("================================")
            appendLine("Type: $crashClass")
            appendLine("Message: $crashMessage")
            appendLine()
            appendLine("Full Crash Log:")
            appendLine("================================")
            appendLine()
            append(logContent)
        }
    }

    companion object {
        const val EXTRA_CRASH_MESSAGE = "extra_crash_message"
        const val EXTRA_CRASH_CLASS = "extra_crash_class"
    }
}
