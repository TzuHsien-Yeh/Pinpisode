package com.tzuhsien.immediat.ext

import android.app.Activity
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.factory.ViewModelFactory

fun Activity.getVmFactory(): ViewModelFactory {
    val repository = (applicationContext as MyApplication).repository
    return ViewModelFactory(repository)
}