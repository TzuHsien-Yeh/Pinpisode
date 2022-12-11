package com.tzuhsien.pinpisode.data.source.local

import android.content.Context
import android.content.SharedPreferences
import com.tzuhsien.pinpisode.MyApplication

object UserManager {
    // Keys put in the USER sharedPref
    // Make it a constant to prevent mis-spelling
    private const val USER = "USER"
    private const val USER_ID = "UserId"
    private const val USER_NAME = "UserName"
    private const val USER_EMAIL = "UserEmail"
    private const val USER_PIC = "UserPic"
    private const val USER_SPOTIFY_TOKEN = "UserSpotifyToken"
    private const val NEW_USER = "isNewUser"
    private const val SHOW_NOTE_LIST_GUIDE = "shown_note_list_guide"

    private val preferences: SharedPreferences =
        MyApplication.instance.getSharedPreferences(USER, Context.MODE_PRIVATE)

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

    var userSpotifyAuthToken: String? = null
        get() {
            return preferences.getString(USER_SPOTIFY_TOKEN, null)
        }
        set(value) {
            field = value
            editor.putString(USER_SPOTIFY_TOKEN, value).commit()
        }

    var isNewUser: Boolean = false
        get() {
            return preferences.getBoolean(NEW_USER, false)
        }
        set(value) {
            field = value
            editor.putBoolean(NEW_USER, value).commit()
        }

    var shouldShowNoteListGuide: Boolean = true
        get() {
            return preferences.getBoolean(SHOW_NOTE_LIST_GUIDE, isNewUser)
        }
        set(value) {
            field = value
            editor.putBoolean(SHOW_NOTE_LIST_GUIDE, value).commit()
        }

}