package com.tzuhsien.pinpisode.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Base64
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.MyApplication.Companion.applicationContext
import java.security.MessageDigest
import java.security.SecureRandom

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

    /** Spotify Auth **/

    val CODE_VERIFIER = getCodeVerifier()

    private fun getCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(
            code,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun getCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }
}
