package com.tzuhsien.immediat.spotifynote

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.spotify.protocol.types.PlayerState
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.data.model.Note
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.databinding.FragmentSpotifyNoteBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.ext.parseSpotifyImageUri
import com.tzuhsien.immediat.spotifynote.SpotifyService.seekBack
import com.tzuhsien.immediat.spotifynote.SpotifyService.seekForward
import com.tzuhsien.immediat.spotifynote.SpotifyService.seekTo
import timber.log.Timber

class SpotifyNoteFragment : Fragment() {
    private val viewModel by viewModels<SpotifyNoteViewModel> {
        getVmFactory(
            SpotifyNoteFragmentArgs.fromBundle(requireArguments()).noteIdKey,
            SpotifyNoteFragmentArgs.fromBundle(requireArguments()).sourceIdKey
        )
    }
    private lateinit var binding: FragmentSpotifyNoteBinding
    private lateinit var trackProgressBar: TrackProgressBar

    override fun onDestroy() {
        super.onDestroy()
        SpotifyService.disconnect()
        Timber.d("onDestroy(): SpotifyService.disconnect()")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSpotifyNoteBinding.inflate(layoutInflater)

        viewModel.isSpotifyConnected.observe(viewLifecycleOwner) { it ->
            binding.playPauseButton.isEnabled = it
            binding.seekBackButton.isEnabled = it
            binding.seekForwardButton.isEnabled = it

            if (it) {
                SpotifyService.play(SPOTIFY_URI + viewModel.sourceId)

                SpotifyService.subscribeToPlayerState { state ->
                    updateSeekbar(state)
                    updatePlayPauseButton(state)
                    SpotifyService.getCoverArt(state.track.imageUri) {
                        binding.imgCoverArt.setImageBitmap(it)
                    }
                    binding.textSourceTitle.text = state.track.name
                    if (!state.isPaused) {
                        // Update new info after the playerState catching up with the current playing track
                        viewModel.updateNewInfo(state)
                    }
                }

            }

        }

        viewModel.getInfoFromPlayerState.observe(viewLifecycleOwner) {
            it?.let {

                viewModel.shouldCreateNewNote.observe(viewLifecycleOwner) { shouldCreateNote ->
                    SpotifyService.getCurrentTrack { track ->
                        val newSpotifyNote = Note(
                            source = Source.SPOTIFY.source,
                            sourceId = viewModel.sourceId,
                            title = track.name,
                            thumbnail = track.imageUri.raw!!.parseSpotifyImageUri(),
                            duration = track.duration.toString()
                        )

                        if (shouldCreateNote) {
                            viewModel.createNewSpotifyNote(newSpotifyNote)
                            viewModel.createNewNoteFinished()
                        }
                    }

            }
        }

        }

        binding.playPauseButton.setOnClickListener {
            when (viewModel.playingState) {
                PlayingState.STOPPED -> {
                    viewModel.playingState = PlayingState.PLAYING
                }

                PlayingState.PLAYING -> {
                    SpotifyService.pause()
                }

                PlayingState.PAUSED -> {
                    SpotifyService.resume()
                }
            }
        }

        binding.seekBackButton.setOnClickListener {
            seekBack()
        }
        binding.seekForwardButton.setOnClickListener {
            seekForward()
        }

        /**
         *  Seek bar
         * */
        binding.seekTo.apply {
            this.isEnabled = false
            progressDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
            indeterminateDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
        }
        trackProgressBar =
            TrackProgressBar(binding.seekTo) { seekToPosition: Long -> seekTo(seekToPosition) }


        return binding.root
    }

    private fun updateSeekbar(playerState: PlayerState) {
        // Update progressbar
        trackProgressBar.apply {
            if (playerState.playbackSpeed > 0) {
                unpause()
            } else {
                pause()
            }
            // Invalidate seekbar length and position
            binding.seekTo.max = playerState.track.duration.toInt()
            binding.seekTo.isEnabled = true
            setDuration(playerState.track.duration)
            update(playerState.playbackPosition)
        }
    }

    private fun updatePlayPauseButton(playerState: PlayerState) {
        if (playerState.isPaused) {
            viewModel.playingState = PlayingState.PAUSED
            binding.playPauseButton.setImageResource(R.drawable.btn_play)
        } else {
            viewModel.playingState = PlayingState.PLAYING
            binding.playPauseButton.setImageResource(R.drawable.btn_pause)
        }
    }
}