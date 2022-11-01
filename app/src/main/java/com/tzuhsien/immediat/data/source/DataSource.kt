package com.tzuhsien.immediat.data.source

import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.TimeItem
import com.tzuhsien.immediat.data.model.YouTubeResult

interface DataSource {

    /**
     *  Pages that show a whole list of notes
     * */
    // notes of which the user is the owner (home page)
    suspend fun getAllNotes()

    suspend fun getNoteById()

    // notes of which the user is not the owner
    suspend fun getCoauthoringNotes()

    /**
     *  For the note (single source)
     */
    suspend fun getYouTubeVideoInfoById(id: String): Result<YouTubeResult>

    suspend fun updateYouTubeVideoInfo(videoId: String, note: Note): Result<String>

//    suspend fun getSpotifyInfoById(id: String)

    suspend fun getTimeItems(id: String)

    suspend fun addNewTimeItem(timeItem: TimeItem)

    suspend fun deleteTimeItem(timeItem: TimeItem)

    /**
     *  User info (Login and Profile page method)
     * */
    suspend fun addUser(token: String)
}