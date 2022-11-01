package com.tzuhsien.immediat.data.source.remote

import android.icu.util.Calendar
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.*
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
    private const val PATH_TIME_ITEMS = "timeItems"
    private const val KEY_START_AT = "startAt" // for orderBy()
    private const val KEY_LAST_EDIT_TIME = "lastEditTime" // for orderby()

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
        val doc = notes.document(videoId)

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
                    continuation.resume(Result.Error(it))
                    return@addOnCompleteListener
                }
                continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
            }
        }

    }

    override fun getLiveTimeItems(videoId: String): MutableLiveData<List<TimeItem>> {
        val liveData = MutableLiveData<List<TimeItem>>()

        FirebaseFirestore.getInstance()
            .collection(PATH_NOTES)
            .document(videoId)
            .collection(PATH_TIME_ITEMS)
            .orderBy(KEY_START_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                Timber.i("addSnapshotListener detect")

                error?.let {
                    Timber.w("[${this::class.simpleName}] Error getting documents. ${it.message}")
                }

                val list = mutableListOf<TimeItem>()
                for (doc in snapshot!!) {
                    Timber.d(doc.id + " => " + doc.data)

                    val timeItem = doc.toObject(TimeItem::class.java)
                    list.add(timeItem)
                }

                liveData.value = list
            }
        return liveData
    }

    override suspend fun addNewTimeItem(videoId: String, timeItem: TimeItem): Result<*> = suspendCoroutine { continuation ->
        val doc = FirebaseFirestore.getInstance()
            .collection(PATH_NOTES)
            .document(videoId)
            .collection(PATH_TIME_ITEMS)
            .document()

        doc
            .set(timeItem)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Timber.i("[${this::class.simpleName}] time items: $timeItem ")
                    continuation.resume(Result.Success(1))
                } else {
                    task.exception?.let {
                        Timber.w("[${this::class.simpleName}] Error adding documents. ${it.message}\"")
                        continuation.resume(Result.Error(it))
                        return@addOnCompleteListener
                    }
                    continuation.resume(Result.Fail(MyApplication.instance.getString(R.string.unknown_error)))
                }
            }

    }

    override suspend fun deleteTimeItem(timeItem: TimeItem) {
        TODO("Not yet implemented")
    }

    override suspend fun addUser(token: String) {
        TODO("Not yet implemented")
    }

}