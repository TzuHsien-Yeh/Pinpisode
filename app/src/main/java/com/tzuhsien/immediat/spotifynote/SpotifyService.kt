package com.tzuhsien.immediat.spotifynote

import android.content.Context
import android.graphics.Bitmap
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerContext
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Repeat
import timber.log.Timber


enum class PlayingState {
    PAUSED, PLAYING, STOPPED
}

private const val STEP_MS = 15000L

object SpotifyService {
    private const val CLIENT_ID = "f6095c97a1ab4a7fb88b5ac5f2ba606d"
    private const val  REDIRECT_URI = "pinpisode://callback"

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var connectionParams: ConnectionParams = ConnectionParams.Builder(CLIENT_ID)
        .setRedirectUri(REDIRECT_URI)
        .showAuthView(true)
        .build()
    private var playerStateSubscription: Subscription<PlayerState>? = null
    private var playerContextSubscription: Subscription<PlayerContext>? = null

//    suspend fun connectToAppRemote(): Result<SpotifyAppRemote> =
//        suspendCoroutine { cont ->
//            SpotifyAppRemote.connect(
//                MyApplication.applicationContext(),
//                connectionParams,
//                object : Connector.ConnectionListener {
//                    override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
//                        this@SpotifyService.spotifyAppRemote = spotifyAppRemote
//                        cont.resume(Result.Success(spotifyAppRemote))
//                    }
//
//                    override fun onFailure(error: Throwable) {
//                        cont.resume(Result.Fail(error.message!!))
//                    }
//                })
//        }

    fun connectToAppRemote(context: Context, handler: (connected: Boolean) -> Unit) {

        if (spotifyAppRemote?.isConnected == true) {
            handler(true)
            return
        }

        val connectionListener = object : Connector.ConnectionListener {
            override fun onConnected(spAppRemote: SpotifyAppRemote) {
                spotifyAppRemote = spAppRemote
                handler(true)
            }

            override fun onFailure(throwable: Throwable) {
                Timber.e("SpotifyService", throwable.message, throwable)
                handler(false)
            }
        }
        SpotifyAppRemote.connect(context, connectionParams, connectionListener)
    }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
    }

    fun play(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
        spotifyAppRemote?.playerApi?.setRepeat(Repeat.ONE)
    }

    fun resume() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun getCoverArt(imageUri: ImageUri, handler: (Bitmap) -> Unit)  {
        spotifyAppRemote?.imagesApi?.getImage(imageUri)?.setResultCallback {
            handler(it)
        }
    }

    fun seekBack() {
        spotifyAppRemote?.playerApi?.seekToRelativePosition( - STEP_MS)?.setResultCallback { Timber.d("command successful: seek back") }
    }

    fun seekForward() {
        spotifyAppRemote?.playerApi?.seekToRelativePosition(STEP_MS)?.setResultCallback { Timber.d("command successful: seek fwd") }
    }

    fun seekTo (seekToPosition: Long) {
        spotifyAppRemote?.playerApi?.seekTo(seekToPosition)
    }

    fun subscribeToPlayerState(handler: (PlayerState) -> Unit){
        val playerStateEventCallback = Subscription.EventCallback<PlayerState> { playerState ->
            handler(playerState)
        }

        playerStateSubscription = cancelAndResetSubscription(playerStateSubscription)

        spotifyAppRemote?.let {
            playerStateSubscription =
                it.playerApi.subscribeToPlayerState().setEventCallback(
            playerStateEventCallback
        )?.setLifecycleCallback(
            object : Subscription.LifecycleCallback {
                override fun onStart() {
                    Timber.d("Event: start")
                }

                override fun onStop() {
                    Timber.d("Event: end")
                }
            })
            ?.setErrorCallback { } as Subscription<PlayerState>
        }
    }

    fun subscribeToPlayerContext(handler: (PlayerContext) -> Unit){
        val playerContextEventCallback = Subscription.EventCallback<PlayerContext> { playerContext ->
            handler(playerContext)
        }

        playerContextSubscription = cancelAndResetSubscription(playerContextSubscription)

        spotifyAppRemote?.let {
            playerContextSubscription =
                it.playerApi.subscribeToPlayerContext().setEventCallback(
                    playerContextEventCallback
                )?.setLifecycleCallback(
                    object : Subscription.LifecycleCallback {
                        override fun onStart() {
                            Timber.d("Event: start")
                        }

                        override fun onStop() {
                            Timber.d("Event: end")
                        }
                    })
                    ?.setErrorCallback { } as Subscription<PlayerContext>
        }
    }

    private fun <T : Any?> cancelAndResetSubscription(subscription: Subscription<T>?): Subscription<T>? {
        return subscription?.let {
            if (!it.isCanceled) {
                it.cancel()
            }
            null
        }
    }

}