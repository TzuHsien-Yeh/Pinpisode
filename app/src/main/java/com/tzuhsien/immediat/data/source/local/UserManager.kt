package com.tzuhsien.immediat.data.source.local

import android.content.Context
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.source.local.UserManager.allEditableNoteList

object UserManager {

    // Hardcoded mock user data
//    val userId = "TynspyKXE1kkaDu36DgA"
    val userId = "aaa"
    val userName = "Shrimp"
    val userEmail = "shrimp@gmail.com"


    // Save all the notes of which the user is one of the authors
    var allEditableNoteList: List<Note> = listOf()

    // Save all the notes owned by the user
    var usersNoteList: List<Note> = listOf<Note>()

    // Get all tags
    var tagSet = mutableSetOf<String>()

}