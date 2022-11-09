package com.tzuhsien.immediat.data.source

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.UserInfo
import com.tzuhsien.immediat.data.model.YouTubeResult

class DefaultRepository(private val noteRemoteDataSource: DataSource): Repository {

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {
        return noteRemoteDataSource.getAllLiveNotes()
    }

    override suspend fun getNoteInfoById(noteId: String): Result<Note> {
        return noteRemoteDataSource.getNoteInfoById(noteId)
    }

    override fun getLiveNoteById(noteId: String):  MutableLiveData<Note?>  {
        return noteRemoteDataSource.getLiveNoteById(noteId)
    }

    override suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult> {
        return noteRemoteDataSource.getYouTubeVideoInfoById(id)
    }

    override suspend fun checkIfYouTubeNoteExists(videoId: String): Result<Note?> {
        return noteRemoteDataSource.checkIfYouTubeNoteExists(videoId)
    }

    override suspend fun createYouTubeVideoNote(videoId: String, note: Note): Result<Note> {
        return noteRemoteDataSource.createYouTubeVideoNote(videoId, note)
    }

    override suspend fun updateYouTubeInfo(noteId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateYouTubeInfo(noteId, note)
    }

    override fun getLiveTimeItems(noteId: String): MutableLiveData<List<TimeItem>> {
        return noteRemoteDataSource.getLiveTimeItems(noteId)
    }

    override suspend fun addNewTimeItem(noteId: String, timeItem: TimeItem): Result<String> {
        return noteRemoteDataSource.addNewTimeItem(noteId, timeItem)
    }

    override suspend fun updateTimeItem(noteId: String, timeItem: TimeItem): Result<*> {
        return noteRemoteDataSource.updateTimeItem(noteId, timeItem)
    }

    override suspend fun deleteTimeItem(noteId: String, timeItem: TimeItem): Result<*> {
        return noteRemoteDataSource.deleteTimeItem(noteId, timeItem)
    }

    override suspend fun updateNote(noteId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateNote(noteId, note)
    }

    override suspend fun updateTags(noteId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateTags(noteId, note)
    }

    override suspend fun updateUser(firebaseUser: FirebaseUser, user: UserInfo): Result<UserInfo> {
        return noteRemoteDataSource.updateUser(firebaseUser, user)
    }

    override suspend fun getCurrentUser(): Result<UserInfo?> {
        return noteRemoteDataSource.getCurrentUser()
    }
}