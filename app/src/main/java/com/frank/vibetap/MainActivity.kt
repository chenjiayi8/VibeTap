package com.frank.vibetap

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = getString(R.string.settings_placeholder)
            setPadding(48, 48, 48, 48)
        }

        setContentView(textView)
    }
}
