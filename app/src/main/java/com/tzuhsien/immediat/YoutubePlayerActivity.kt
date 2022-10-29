package com.tzuhsien.immediat

import android.R
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerSupportFragment
import com.tzuhsien.immediat.ImMediAtApplication.Companion.YT_API_KEY
import com.tzuhsien.immediat.databinding.ActivityYoutubePlayerBinding


class YoutubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val playerView : YouTubePlayerSupportFragment =
//            supportFragmentManager.findFragmentById(R.id.fragment_ytplayer) as YouTubePlayerSupportFragment
//        playerView.initialize(YT_API_KEY, object : YouTubePlayer.OnInitializedListener {
//            override fun onInitializationSuccess(
//                p0: YouTubePlayer.Provider?,
//                p1: YouTubePlayer?,
//                p2: Boolean
//            ) { }
//
//            override fun onInitializationFailure(
//                p0: YouTubePlayer.Provider?,
//                p1: YouTubeInitializationResult?
//            ) { }
//        })

    }
}