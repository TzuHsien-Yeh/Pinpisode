package com.tzuhsien.pinpisode.spotifynote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

open class TimestampReceiver(
    private val onActionListener: OnActionListener
): BroadcastReceiver() {

    companion object {
        const val ACTION_TAKE_TIMESTAMP = "ACTION_TAKE_TIMESTAMP"
        const val ACTION_CLIP_START = "ACTION_CLIP_START"
        const val ACTION_CLIP_END = "ACTION_CLIP_END"
    }

//    private var callback: ClipStatusCallback? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        Timber.d("onReceive, action: $action")

        when (action) {
            ACTION_TAKE_TIMESTAMP -> {
                onActionListener.onAction(ACTION_TAKE_TIMESTAMP)
            }
            ACTION_CLIP_START -> {
                onActionListener.onAction(ACTION_CLIP_START)
//                Intent(context, SpotifyNoteService::class.java).apply {
//                    this.action = SpotifyNoteService.ACTION_START_CLIPPING
//                    context?.startService(this)
//                }
            }
            ACTION_CLIP_END -> {
                onActionListener.onAction(ACTION_CLIP_END)

//                Intent(context, SpotifyNoteService::class.java).apply {
//                    this.action = SpotifyNoteService.ACTION_DONE_CLIPPING
//                    context?.startService(this)
//                }

            }
        }
    }

    class OnActionListener(val actionListener: (action: String) -> Unit) {
        fun onAction(action: String) = actionListener(action)
    }

//    fun registerCallback(callback: ClipStatusCallback) {
//        this.callback = callback
//    }
//    interface ClipStatusCallback {
//        fun onReceiveClipStart()
//        fun onReceiveClipEnd()
//    }

}