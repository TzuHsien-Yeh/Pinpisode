package com.tzuhsien.immediat.ext

import androidx.fragment.app.Fragment
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.factory.ViewModelFactory
import com.tzuhsien.immediat.factory.YoutubeNoteViewModelFactory


fun Fragment.getVmFactory(): ViewModelFactory {
    val repository = (requireContext().applicationContext as MyApplication).repository
    return ViewModelFactory(repository)
}

fun Fragment.getVmFactory(noteId: String, videoId: String): YoutubeNoteViewModelFactory {
    val repository = (requireContext().applicationContext as MyApplication).repository
    return YoutubeNoteViewModelFactory(repository, noteId, videoId)
}