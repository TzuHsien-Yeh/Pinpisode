package com.tzuhsien.immediat.data.source

import com.tzuhsien.immediat.data.Result
import com.tzuhsien.immediat.data.model.ClipNote
import com.tzuhsien.immediat.data.model.TimestampNote
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

//    suspend fun getSpotifyInfoById(id: String)

    suspend fun getTimestampNotes(id: String)

    suspend fun getClipNotes(id: String)

    suspend fun addNewTimestampNote(timestampNote: TimestampNote)

    suspend fun addNewClipNote(clipNote: ClipNote)

    suspend fun deleteTimestampNote(timestampNote: TimestampNote)

    suspend fun deleteClipNote(clipNote: ClipNote)

    /**
     *  User info (Login and Profile page method)
     * */
    suspend fun addUser(token: String)
}