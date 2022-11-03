package com.tzuhsien.immediat.data.source

import androidx.lifecycle.MutableLiveData
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.YouTubeResult

class DefaultRepository(private val noteRemoteDataSource: DataSource): Repository {

    override fun getAllLiveNotes(): MutableLiveData<List<Note>> {
        return noteRemoteDataSource.getAllLiveNotes()
    }

    override fun getLiveNoteById(noteId: String):  MutableLiveData<Note?>  {
        return noteRemoteDataSource.getLiveNoteById(noteId)
    }

    override suspend fun getCoauthoringNotes() {
        TODO("Not yet implemented")
    }

    override suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult> {
        return noteRemoteDataSource.getYouTubeVideoInfoById(id)
    }

    override suspend fun updateYouTubeVideoInfo(videoId: String, note: Note): Result<String> {
        return noteRemoteDataSource.updateYouTubeVideoInfo(videoId, note)
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

    override suspend fun addUser(token: String) {
        TODO("Not yet implemented")
    }
}