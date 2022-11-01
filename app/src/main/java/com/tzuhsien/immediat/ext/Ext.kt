package com.tzuhsien.immediat.ext

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration


// Convert UTC to local time

fun String.utcToLocalTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val consultationDate = sdf.parse(this)?.toString() ?: ""

    return consultationDate
}

fun Float.convertDurationToDisplay(): String {
    val millis = this.toLong()
    val duration = millis.toDuration(DurationUnit.MILLISECONDS)
    val timeString =
        duration.toComponents { HH, MM, SS ->
            String.format("%02d:%02d:%02d", HH, MM, SS)
        }
    return timeString
}

fun customNoteEditView(editText: EditText, text: TextView, value: String){
    text.setOnClickListener { view ->
        view.visibility = View.GONE
        editText.visibility = View.VISIBLE
        editText.setText(value)
        editText.doAfterTextChanged{
            text.text = it.toString()
        }
    }
}