package com.tzuhsien.immediat.data.source.local

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.MyApplication.Companion.applicationContext
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.UserInfo
import com.tzuhsien.immediat.data.source.local.UserManager.allEditableNoteList
import kotlin.coroutines.suspendCoroutine

object UserManager {

    private const val USER_ID = "UserId" // Make it a constant to prevent mis-spelling

    private val preferences: SharedPreferences =
        MyApplication.instance.getSharedPreferences(USER_ID, Context.MODE_PRIVATE)

    private val editor = preferences.edit()

    var userId: String? = null
        get() {
            return preferences.getString(USER_ID, null)
        }
        set(value) {
            field = value
            editor.putString(USER_ID, value).commit()
        }

    var user: UserInfo? = null

    var userName: String = "Shrimp"
    var userEmail: String = "shrimp@gmail.com"

    // Query user info if user info in UserManager is null
    fun isUserInfoAvailable(): Boolean {
        return userId == null
    }


    // Save all the notes of which the user is one of the authors
    var allEditableNoteList: List<Note> = listOf()

    // Save all the notes owned by the user
    var usersNoteList: List<Note> = listOf<Note>()

    // Get all tags
    var tagSet = mutableSetOf<String>()
}