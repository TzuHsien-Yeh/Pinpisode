package com.tzuhsien.immediat.data.source

import androidx.lifecycle.MutableLiveData
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.TimestampNote
import com.tzuhsien.immediat.data.model.YouTubeResult

class DefaultRepository(private val noteRemoteDataSource: DataSource): Repository {
    override suspend fun getAllNotes() {
        TODO("Not yet implemented")
    }

    override suspend fun getNoteById() {
        TODO("Not yet implemented")
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
    override fun getLiveTimeItems(videoId: String): MutableLiveData<List<TimeItem>> {
        return noteRemoteDataSource.getLiveTimeItems(videoId)
    }

    override suspend fun addNewTimeItem(videoId: String, timeItem: TimeItem): Result<*> {
        return noteRemoteDataSource.addNewTimeItem(videoId, timeItem)
    }

    override suspend fun deleteTimeItem(timeItem: TimeItem) {
        TODO("Not yet implemented")
    }

    override suspend fun addUser(token: String) {
        TODO("Not yet implemented")
    }
}