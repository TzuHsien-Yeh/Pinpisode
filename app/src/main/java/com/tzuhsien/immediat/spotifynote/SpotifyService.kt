package com.tzuhsien.immediat.spotifynote

import android.graphics.Bitmap
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.client.Subscription
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Track
import com.tzuhsien.immediat.MyApplication
import timber.log.Timber
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


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


    suspend fun connectToAppRemote(): SpotifyAppRemote =
        suspendCoroutine { cont: Continuation<SpotifyAppRemote> ->
            SpotifyAppRemote.connect(
                MyApplication.applicationContext(),
                connectionParams,
                object : Connector.ConnectionListener {
                    override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                        this@SpotifyService.spotifyAppRemote = spotifyAppRemote
                        cont.resume(spotifyAppRemote)
                    }

                    override fun onFailure(error: Throwable) {
                        cont.resumeWithException(error)
                    }
                })
        }

    fun disconnect() {
        SpotifyAppRemote.disconnect(spotifyAppRemote)
    }

    fun play(uri: String) {
        spotifyAppRemote?.playerApi?.play(uri)
    }

    fun resume() {
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun getCurrentTrackInfo(handler: (track: Track) -> Unit) {
        spotifyAppRemote?.playerApi?.playerState?.setResultCallback { result ->
            handler(result.track)
        }
    }

    fun getCoverImage(imageUri: ImageUri, handler: (Bitmap) -> Unit)  {
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

    private fun <T : Any?> cancelAndResetSubscription(subscription: Subscription<T>?): Subscription<T>? {
        return subscription?.let {
            if (!it.isCanceled) {
                it.cancel()
            }
            null
        }
    }

}