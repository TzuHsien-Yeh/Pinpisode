package com.tzuhsien.pinpisode

object Constants {
    const val YOUTUBE_WATCH_URL = "youtube.com/watch?v="
    const val YOUTUBE_SHARE_LINK = "youtu.be/"

    const val SPOTIFY = "spotify"
    const val SPOTIFY_URI = "spotify:"
    const val SPOTIFY_URI_EPISODE = "episode:"
    const val SPOTIFY_SHARE_LINK = "https://open.spotify.com/"
    const val SPOTIFY_PACKAGE_NAME = "com.spotify.music"

    const val PATH_SEARCH = "search/"

    // Spotify Auth
    const val CLIENT_ID = "f6095c97a1ab4a7fb88b5ac5f2ba606d"
    const val REDIRECT_URI = "pinpisode://callback"
    const val SCOPE_READ_PLAYBACK_POSITION = "user-read-playback-position"
    const val SCOPE_LIBRARY_READ = "user-library-read"

    const val PARAM_CODE_CHALLENGE_METHOD = "code_challenge_method"
    const val S256 = "S256"
    const val PARAM_CODE_CHALLENGE = "code_challenge"
    const val PARAM_GRANT_TYPE = "grant_type"
    const val AUTH_CODE = "authorization_code"
    const val PARAM_CODE = "code"
    const val PARAM_CODE_VERIFIER = "code_verifier"

}