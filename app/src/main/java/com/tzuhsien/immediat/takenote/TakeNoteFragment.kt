package com.tzuhsien.immediat.takenote

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.tzuhsien.immediat.databinding.FragmentTakeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory


class TakeNoteFragment : Fragment() {

    private val viewModel by viewModels<TakeNoteViewModel> {
        getVmFactory(TakeNoteFragmentArgs.fromBundle(requireArguments()).videoIdKey)
    }
    private lateinit var binding: FragmentTakeNoteBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentTakeNoteBinding.inflate(layoutInflater)

        val youTubePlayerView: YouTubePlayerView = binding.youtubePlayerView
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                val videoId = viewModel.videoId
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })

        return binding.root
    }

}