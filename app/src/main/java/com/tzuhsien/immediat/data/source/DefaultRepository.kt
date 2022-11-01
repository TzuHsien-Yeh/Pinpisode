package com.tzuhsien.immediat.data.source

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
    override suspend fun getTimeItems(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun addNewTimeItem(timeItem: TimeItem) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTimeItem(timeItem: TimeItem) {
        TODO("Not yet implemented")
    }

    override suspend fun addUser(token: String) {
        TODO("Not yet implemented")
    }
}