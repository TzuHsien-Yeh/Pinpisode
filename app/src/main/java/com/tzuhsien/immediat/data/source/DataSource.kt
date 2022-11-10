package com.tzuhsien.immediat.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.UserInfo
import com.tzuhsien.immediat.data.model.YouTubeResult

interface DataSource {

    /**
     *  Pages that show a whole list of notes
     * */
    fun getAllLiveNotes(): MutableLiveData<List<Note>>

    /**
     *  For the note (single source)
     */
    suspend fun getNoteInfoById(noteId: String): Result<Note>

    fun getLiveNoteById(noteId: String): MutableLiveData<Note?>

    suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult>

    suspend fun checkIfYouTubeNoteExists(videoId: String): Result<Note?>

    suspend fun createYouTubeVideoNote(videoId: String, note: Note): Result<Note>

    suspend fun updateYouTubeInfo(noteId: String, note: Note): Result<String>

    fun getLiveTimeItems(noteId: String): MutableLiveData<List<TimeItem>>

    suspend fun addNewTimeItem(noteId: String, timeItem: TimeItem): Result<String>

    suspend fun updateTimeItem(noteId: String, timeItem: TimeItem): Result<*>

    suspend fun deleteTimeItem(noteId: String, timeItem: TimeItem): Result<*>

    suspend fun updateNote(noteId: String, note: Note): Result<String>

    suspend fun updateTags(noteId: String, note: Note): Result<String>

    /**
     *  User info (Login and Profile page method)
     * */
    suspend fun updateUser(firebaseUser: FirebaseUser, user: UserInfo) : Result<UserInfo>

    suspend fun getCurrentUser(): Result<UserInfo?>

    suspend fun findUserByEmail(query: String): Result<UserInfo?>

    suspend fun updateNoteAuthors(noteId: String, authors: Set<String>): Result<Boolean>
}