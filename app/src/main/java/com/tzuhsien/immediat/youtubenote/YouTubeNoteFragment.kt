package com.tzuhsien.immediat.youtubenote

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tzuhsien.immediat.MyApplication
import com.tzuhsien.immediat.databinding.FragmentYoutubeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.factory.YoutubeNoteViewModelFactory


class YouTubeNoteFragment : Fragment() {

    private val viewModel by viewModels<YouTubeNoteViewModel> {
        getVmFactory(YouTubeNoteFragmentArgs.fromBundle(requireArguments()).videoIdKey)
    }
    private lateinit var binding: FragmentYoutubeNoteBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentYoutubeNoteBinding.inflate(layoutInflater)

        val youTubePlayerView: YouTubePlayerView = binding.youtubePlayerView
        lifecycle.addObserver(youTubePlayerView)

        binding.textTestingTimestamp.text = "${viewModel.testTime.toInt().toString()}: 時間戳標題"


        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                val videoId = viewModel.videoId
                youTubePlayer.loadVideo(videoId, 0f)

                binding.textTestingTimestamp.setOnClickListener {
                    youTubePlayer.seekTo(viewModel.testTime.toFloat())
                }
                // TODO: recyclerview item onclick > play at the second / clip
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                super.onCurrentSecond(youTubePlayer, second)

                binding.btnTakeTimestamp.setOnClickListener {
                    viewModel.takeTimeStamp(second)
                }
            }

        })

        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.youtubePlayerView.enterFullScreen()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.youtubePlayerView.exitFullScreen()
        }
    }
}