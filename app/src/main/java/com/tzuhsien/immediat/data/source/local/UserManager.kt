package com.tzuhsien.immediat.data.source.local

import androidx.lifecycle.MutableLiveData
import com.tzuhsien.immediat.data.model.Note

object UserManager {

    // Hardcoded mock user data
//    val userId = "TynspyKXE1kkaDu36DgA"
    val userId = "sssss"
    val userName = "Shrimp"
    val userEmail = "shrimp@gmail.com"

    // Save all the notes owned by the user
//    val usersNoteList = MutableLiveData<List<Note>>(listOf())
    var usersNoteList: List<Note> = listOf<Note>()
}