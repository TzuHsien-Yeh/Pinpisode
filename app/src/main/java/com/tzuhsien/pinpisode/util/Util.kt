package com.tzuhsien.pinpisode.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.MyApplication.Companion.applicationContext

/**
 * Updated by Wayne Chen in Mar. 2019.
 */
object Util {

    /**
     * Determine and monitor the connectivity status
     *
     * https://developer.android.com/training/monitoring-device-state/connectivity-monitoring
     */
    fun isInternetConnected(): Boolean {
        val cm = MyApplication.instance
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    fun getString(resourceId: Int, value: String? = null): String {

        return applicationContext().getString(resourceId, value)
    }
}