package com.tzuhsien.immediat.data.source

import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.ClipNote
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.TimestampNote
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.source.remote.NoteRemoteDataSource

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

    override suspend fun updateYouTubeVideoInfo(videoId: String, note: Note) {
        TODO("Not yet implemented")
    }

    override suspend fun getTimestampNotes(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getClipNotes(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun addNewTimestampNote(timestampNote: TimestampNote) {
        TODO("Not yet implemented")
    }

    override suspend fun addNewClipNote(clipNote: ClipNote) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTimestampNote(timestampNote: TimestampNote) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteClipNote(clipNote: ClipNote) {
        TODO("Not yet implemented")
    }

    override suspend fun addUser(token: String) {
        TODO("Not yet implemented")
    }
}