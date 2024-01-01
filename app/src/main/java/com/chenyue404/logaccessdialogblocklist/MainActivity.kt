package com.chenyue404.logaccessdialogblocklist

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit

class MainActivity : Activity() {

    private val etContent: EditText by lazy { findViewById(R.id.etContent) }
    private val etContentAllow: EditText by lazy { findViewById(R.id.etContentAllow) }
    private val btSave: Button by lazy { findViewById(R.id.btSave) }
    private val sp by lazy {
        try {
            getSharedPreferences(
                Hook.PREF_NAME,
                Context.MODE_WORLD_READABLE
            )
        } catch (e: SecurityException) {
            // The new XSharedPreferences is not enabled or module's not loading
            null // other fallback, if any
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etContent.setText(sp?.getStringSet(Hook.KEY, emptySet())?.joinToString())
        etContentAllow.setText(sp?.getStringSet(Hook.KEY_ALLOW, emptySet())?.joinToString())
        btSave.setOnClickListener {
            val set = etContent.text.toString()
                .split(",")
                .toMutableSet().onEach { it.trim() }
            etContent.setText(set.joinToString(","))
            val allowSet = etContentAllow.text.toString()
                .split(",")
                .toMutableSet().onEach { it.trim() }
            etContentAllow.setText(allowSet.joinToString(","))
            sp?.edit {
                putStringSet(Hook.KEY, set)
                putStringSet(Hook.KEY_ALLOW, allowSet)
            }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
    }
}