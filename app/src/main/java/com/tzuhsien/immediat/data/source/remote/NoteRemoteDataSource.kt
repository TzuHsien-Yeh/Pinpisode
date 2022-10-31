package com.tzuhsien.immediat.data.source.remote

import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.ClipNote
import com.tzuhsien.immediat.data.model.TimestampNote
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.source.DataSource
import com.tzuhsien.immediat.network.YouTubeApi
import com.tzuhsien.immediat.util.Util.getString
import com.tzuhsien.immediat.util.Util.isInternetConnected
import timber.log.Timber

object NoteRemoteDataSource: DataSource {
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
        if (!isInternetConnected()) {
            return Result.Fail(getString(R.string.internet_not_connected))
        }

        return try {
            // this will run on a thread managed by Retrofit
            val listResult = YouTubeApi.retrofitService.getVideoInfo(id)

            listResult.error?.let {
                return Result.Fail(it)
            }
            Result.Success(listResult)
        } catch (e: Exception) {
            Timber.w("[${this::class.simpleName}: getYouTubeVideoInfoById] exception=${e.message}")
            Result.Error(e)
        }
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