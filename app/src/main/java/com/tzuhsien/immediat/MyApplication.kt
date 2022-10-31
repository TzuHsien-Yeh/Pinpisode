package com.tzuhsien.immediat

import android.app.Application
import android.content.Context
import com.tzuhsien.immediat.data.source.Repository
import com.tzuhsien.immediat.network.YouTubeApiService
import com.tzuhsien.immediat.util.ServiceLocator
import kotlin.properties.Delegates

class MyApplication: Application() {

    val repository: Repository
        get() = ServiceLocator.provideTasksRepository(this)

    companion object {
        var instance: MyApplication by Delegates.notNull()

        fun applicationContext() : Context {
            return instance.applicationContext
        }


    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}