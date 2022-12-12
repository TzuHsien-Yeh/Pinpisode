package com.tzuhsien.pinpisode.util

import androidx.annotation.VisibleForTesting
import com.tzuhsien.pinpisode.data.source.DefaultRepository
import com.tzuhsien.pinpisode.data.source.Repository
import com.tzuhsien.pinpisode.data.source.local.UserLocalDataSource
import com.tzuhsien.pinpisode.data.source.remote.NoteRemoteDataSource
import com.tzuhsien.pinpisode.data.source.remote.UserRemoteDataSource

object ServiceLocator {
    @Volatile
    var repository: Repository? = null
        @VisibleForTesting set

    fun provideTasksRepository(): Repository {
        synchronized(this) {
            return repository
                ?: createRepository()
        }
    }

    private fun createRepository(): Repository {
        return DefaultRepository(
            NoteRemoteDataSource,
            UserRemoteDataSource,
            UserLocalDataSource
        )
    }
}