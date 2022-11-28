package com.tzuhsien.pinpisode.ext

import android.app.Activity
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.factory.ViewModelFactory

fun Activity.getVmFactory(): ViewModelFactory {
    val repository = (applicationContext as MyApplication).repository
    return ViewModelFactory(repository)
}