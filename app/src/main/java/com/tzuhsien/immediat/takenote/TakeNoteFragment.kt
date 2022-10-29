package com.tzuhsien.immediat.takenote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerSupportFragment
import com.tzuhsien.immediat.ImMediAtApplication.Companion.YT_API_KEY
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.databinding.FragmentTakeNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory

class TakeNoteFragment : Fragment() {

    private val viewModel by viewModels<TakeNoteViewModel> {
        getVmFactory(TakeNoteFragmentArgs.fromBundle(requireArguments()).videoIdKey)
    }
    private lateinit var binding: FragmentTakeNoteBinding
    private var youtubePlayer: YouTubePlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = FragmentTakeNoteBinding.inflate(layoutInflater)

        val youtubePlayerFragment = YouTubePlayerSupportFragment.newInstance()
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.yt_player, youtubePlayerFragment).commit()

        youtubePlayerFragment.initialize(YT_API_KEY, object : YouTubePlayer.OnInitializedListener {
            override fun onInitializationSuccess(
                p0: YouTubePlayer.Provider?,
                p1: YouTubePlayer?,
                p2: Boolean
            ) {
                p1?.loadVideo(viewModel.videoId)
            }

            override fun onInitializationFailure(
                p0: YouTubePlayer.Provider?,
                p1: YouTubeInitializationResult?
            ) {
            }
        })

        return binding.root
    }


}