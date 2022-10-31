package com.tzuhsien.immediat.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.MyApplication.Companion.applicationContext

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

    fun getString(resourceId: Int): String {
        return applicationContext().getString(resourceId)
    }

}
