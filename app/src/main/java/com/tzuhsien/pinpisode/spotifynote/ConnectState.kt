package com.tzuhsien.pinpisode.spotifynote

import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R

enum class ConnectState(val msg: String?) {
    CONNECTED(null),
    NOT_INSTALLED(MyApplication.applicationContext().getString(R.string.error_msg_spotify_not_installed)),
    NOT_LOGGED_IN(MyApplication.applicationContext().getString(R.string.error_msg_spotify_not_logged_in)),
    UNKNOWN_ERROR(MyApplication.applicationContext().getString(R.string.error_connecting_to_spotify))
}