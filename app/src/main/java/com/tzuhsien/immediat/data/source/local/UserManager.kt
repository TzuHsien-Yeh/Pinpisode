package com.tzuhsien.immediat.data.source.local

import android.content.Context
import android.content.SharedPreferences
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.data.model.Note

object UserManager {

    // Keys put in the USER sharedPref
    // Make it a constant to prevent mis-spelling
    private const val USER_ID = "UserId"
    private const val USER_NAME = "UserName"
    private const val USER_EMAIL = "UserEmail"
    private const val USER_PIC = "UserPic"

    private val preferences: SharedPreferences =
        MyApplication.instance.getSharedPreferences("USER", Context.MODE_PRIVATE)

    private val editor = preferences.edit()

    var userId: String? = null
        get() {
            return preferences.getString(USER_ID, null)
        }
        set(value) {
            field = value
            editor.putString(USER_ID, value).commit()
        }

    var userName: String? = null
        get() {
            return preferences.getString(USER_NAME, null)
        }
        set(value) {
            field = value
            editor.putString(USER_NAME, value).commit()
        }

    var userEmail: String? = null
        get() {
            return preferences.getString(USER_EMAIL, null)
        }
        set(value) {
            field = value
            editor.putString(USER_EMAIL, value).commit()
        }

    var userPic: String? = null
        get() {
            return preferences.getString(USER_PIC, null)
        }
        set(value) {
            field = value
            editor.putString(USER_PIC, value).commit()
        }



    // Save all the notes of which the user is one of the authors
    var allEditableNoteList: List<Note> = listOf()

    // Save all the notes owned by the user
    var usersNoteList: List<Note> = listOf()

    // Get all tags
    var tagSet = mutableSetOf<String>()
}