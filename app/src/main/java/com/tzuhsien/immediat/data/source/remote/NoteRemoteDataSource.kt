package com.tzuhsien.immediat.data.source.remote

import android.icu.util.Calendar
import com.google.firebase.firestore.FirebaseFirestore
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.TimestampNote
import com.tzuhsien.immediat.data.model.YouTubeResult
import com.tzuhsien.immediat.data.source.DataSource
import com.tzuhsien.immediat.network.YouTubeApi
import com.tzuhsien.immediat.util.Util.getString
import com.tzuhsien.immediat.util.Util.isInternetConnected
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NoteRemoteDataSource: DataSource {

    private const val PATH_NOTES = "notes"
    private const val PATH_USERS = "users"
    private const val KEY_LAST_EDIT_TIME = "lastEditTime"

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

    override suspend fun updateYouTubeVideoInfo(videoId: String, note: Note): Result<String> = suspendCoroutine { continuation ->
        val notes = FirebaseFirestore.getInstance().collection(PATH_NOTES)
        val doc = notes.document()

        note.lastEditTime = Calendar.getInstance().timeInMillis

        doc
            .set(note)
            .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.i("[${this::class.simpleName}] note: $note ")

                continuation.resume(Result.Success(note.id))
            } else {
                task.exception?.let {
                    Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                }
            }
        }

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