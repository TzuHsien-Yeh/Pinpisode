package com.tzuhsien.pinpisode.spotifynote

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.spotify.protocol.types.PlayerState
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.coauthor.CoauthorDialogFragmentDirections
import com.tzuhsien.pinpisode.data.model.Note
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.data.model.TimeItemDisplay
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.databinding.FragmentSpotifyNoteBinding
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.ext.formatDuration
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.parseSpotifyImageUri
import com.tzuhsien.pinpisode.loading.LoadingDialogDirections
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.pause
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.resume
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.seekBack
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.seekForward
import com.tzuhsien.pinpisode.spotifynote.SpotifyService.seekTo
import com.tzuhsien.pinpisode.tag.TagDialogFragmentDirections
import com.tzuhsien.pinpisode.util.SharingLinkGenerator
import com.tzuhsien.pinpisode.util.SwipeHelper
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom


class SpotifyNoteFragment : Fragment() {
    private val viewModel by viewModels<SpotifyNoteViewModel> {
        getVmFactory(
            SpotifyNoteFragmentArgs.fromBundle(requireArguments()).noteIdKey,
            SpotifyNoteFragmentArgs.fromBundle(requireArguments()).sourceIdKey
        )
    }
    private lateinit var binding: FragmentSpotifyNoteBinding
    private lateinit var trackProgressBar: TrackProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSpotifyNoteBinding.inflate(layoutInflater)

        viewModel.isSpotifyNeedLogin.observe(viewLifecycleOwner) {
            if (it) {
                showLoginActivityCode.launch(getLoginActivityCodeIntent())
            }
        }

        viewModel.isSpotifyConnected.observe(viewLifecycleOwner) { it ->
            Timber.d("viewModel.isSpotifyConnected.observe: $it")
            viewModel.clearConnectionErrorMsg()
            binding.playPauseButton.isEnabled = it
            binding.seekBackButton.isEnabled = it
            binding.seekForwardButton.isEnabled = it
            binding.btnClip.isEnabled = it
            binding.btnTakeTimestamp.isEnabled = it

            if (it) {
                SpotifyService.play(SPOTIFY_URI + viewModel.sourceId)
                Timber.d("SpotifyService.play(${SPOTIFY_URI + viewModel.sourceId})")

                SpotifyService.subscribeToPlayerState { state ->
                    Timber.d("SpotifyService.subscribeToPlayerState:  $state ")
                    updateSeekbar(state)
                    updatePlayPauseButton(state)

                    viewModel.updateCurrentPosition(state.playbackPosition)
                    viewModel.startTrackingPosition()

                    SpotifyService.getCoverArt(state.track.imageUri) {
                        binding.imgCoverArt.setImageBitmap(it)
                    }
                    binding.textSourceTitle.text = state.track.name
                    binding.textPublisher.text = if (state.track.isPodcast) {
                        state.track.album.name
                    } else {
                        state.track.artist.name
                    }

                    binding.textTotalTime.text = state.track.duration.formatDuration()

                    if (state.track.uri.extractSpotifySourceId() == viewModel.sourceId) {
                        // Update new info when the player is playing the assigned uri
                        viewModel.newSpotifyNote = Note(
                            source = Source.SPOTIFY.source,
                            sourceId = viewModel.sourceId,
                            tags = listOf(Source.SPOTIFY.source),
                            title = state.track.name,
                            thumbnail = state.track.imageUri.raw!!.parseSpotifyImageUri(),
                            duration = state.track.duration.toString()
                        )
                        viewModel.invokeCreateNewNoteLiveData()
                    }

                    if (state.isPaused) {
                        // stop the timer from keep updating time
                        viewModel.pauseTrackingPosition()
                    } else {
                        // track current playing position in real time
                        viewModel.unpauseTrackingPosition()
                    }

                }


                Timber.d("viewModel.isViewerCanEdit = ${viewModel.isViewerCanEdit}")
                if (viewModel.isViewerCanEdit) {
                    Intent(context, SpotifyNoteService::class.java).apply {
                        action = SpotifyNoteService.ACTION_START
                        context?.startService(this)
                    }
                    registerTimestampReceiver()
                } else {

                    Timber.d(" viewModel.isSpotifyConnected.observe, ACTION_STOP")
                    Intent(context, SpotifyNoteService::class.java).apply {
                        action = SpotifyNoteService.ACTION_STOP
                        context?.startService(this)
                    }
                }
            }
        }

        viewModel.shouldCreateNewNote.observe(viewLifecycleOwner) { shouldCreateNote ->
            Timber.d("viewModel.shouldCreateNewNote.observe: $shouldCreateNote, note: ${viewModel.newSpotifyNote}")
            if (viewModel.newSpotifyNote.sourceId == viewModel.sourceId) {
                if (shouldCreateNote) {
                    viewModel.createNewSpotifyNote(viewModel.newSpotifyNote)
                    viewModel.hasUploaded = true
                }
            }
        }

        /**
         * Bind player control
         * **/
        binding.playPauseButton.setOnClickListener {
            Timber.d("binding.playPauseButton.setOnClickListener, ${viewModel.playingState}")
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

        /**  Seek bar  **/
        binding.seekTo.apply {
            this.isEnabled = false
            progressDrawable.setColorFilter(Color.parseColor("#ff4664"), PorterDuff.Mode.SRC_ATOP)
            indeterminateDrawable.setColorFilter(Color.parseColor("#ff4664"),
                PorterDuff.Mode.SRC_ATOP)
        }
        trackProgressBar =
            TrackProgressBar(binding.seekTo) { seekToPosition: Long -> seekTo(seekToPosition) }

        /**
         *  Take timestamps / clips buttons
         * */
        binding.btnTakeTimestamp.setOnClickListener {
            Timber.d("binding.btnTakeTimestamp.setOnClickListener clicked")
            takeTimestamp()
        }

        binding.btnClip.setOnClickListener {
            when (viewModel.startOrStopToggle) {
                0 -> {
                    startClipping()
                    Timber.d("btnClip first time clicked, viewModel.startAt = ${viewModel.startAt}")
                }
                1 -> {
                    endClipping()
                    Timber.d("btnClip second time clicked, viewModel.endAt = ${viewModel.endAt}")
                }
            }
        }

        /**
         *  Play the time items
         * **/
        viewModel.playStart.observe(viewLifecycleOwner) { startAt ->
            startAt?.let {
                seekTo(startAt)
                resume()
            }
        }

        viewModel.currentPosition.observe(viewLifecycleOwner) { currentSec ->
            // Update current position text
            binding.textCurrentSecond.text = currentSec.formatDuration()

            viewModel.playEnd?.let {
                if (it <= currentSec) {
                    pause()
                    viewModel.clearPlayingMomentStart()
                    viewModel.clearPlayingMomentEnd()
                }
            }
        }

        /**
         * Digest of the video (editText)
         * */
        viewModel.liveNoteDataReassigned.observe(viewLifecycleOwner) {
            if (it) {
                viewModel.liveNoteData.observe(viewLifecycleOwner) { note ->
                    note?.let {
                        binding.editDigest.setText(note.digest)
                        viewModel.noteToBeUpdated = note
                    }
                }
                binding.editDigest.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        viewModel.noteToBeUpdated?.digest = binding.editDigest.text.toString()
                        viewModel.updateNote()
                    }
                }
            }
        }

        /**
         *  RecyclerView views
         * */
        // Swipe to delete
        binding.recyclerViewTimeItems.addItemDecoration(DividerItemDecoration(context,
            DividerItemDecoration.VERTICAL))
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(
            binding.recyclerViewTimeItems,
            swipeOutListener = OnSwipeOutListener {
                viewModel.deleteTimeItem(it)
            }
        ) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val deleteButton = deleteButton(position)
                return listOf(deleteButton)
            }
        })

        val adapter = SpotifyTimeItemAdapter(
            uiState = viewModel.uiState
        )
        binding.recyclerViewTimeItems.adapter = adapter

        viewModel.timeItemLiveDataReassigned.observe(viewLifecycleOwner) { timeItemLiveDataAssigned ->
            if (timeItemLiveDataAssigned == true) {
                viewModel.liveTimeItemList.observe(viewLifecycleOwner) { list ->
                    list?.let {
                        when (viewModel.displayState) {
                            TimeItemDisplay.ALL -> {
                                adapter.submitList(list)
                            }
                            TimeItemDisplay.TIMESTAMP -> {
                                adapter.submitList(list.filter { it.endAt == null })
                            }
                            TimeItemDisplay.CLIP -> {
                                adapter.submitList(list.filter { it.endAt != null })
                            }
                        }
                    }

                }
            }
        }

        /**
         *  Edit or read only mode
         * */
        viewModel.canEdit.observe(viewLifecycleOwner) {
            binding.editDigest.isEnabled = it
            binding.editDigest.hint = if (it) getString(R.string.input_video_summary) else null
            binding.icAddTag.isEnabled = it
            binding.btnClip.visibility = if (it) View.VISIBLE else View.GONE
            binding.btnTakeTimestamp.visibility = if (it) View.VISIBLE else View.GONE
            if (it) {
                // attach swipe to delete helper
                itemTouchHelper.attachToRecyclerView(binding.recyclerViewTimeItems)
            }
        }

        /**
         *  Buttons on the bottom of the page: Toggle the display of timeItems
         * */
        binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_all)
        binding.icTimeItemDisplayOptions.setOnClickListener {
            when (viewModel.displayState) {
                TimeItemDisplay.ALL -> {
                    // to display only timestamps
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_timestamps)
                    viewModel.displayState = TimeItemDisplay.TIMESTAMP
                    viewModel.notifyDisplayChange()
                }
                TimeItemDisplay.TIMESTAMP -> {
                    // to display only clips
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_clips)
                    viewModel.displayState = TimeItemDisplay.CLIP
                    viewModel.notifyDisplayChange()
                }
                TimeItemDisplay.CLIP -> {
                    binding.icTimeItemDisplayOptions.setImageResource(R.drawable.ic_view_all)
                    viewModel.displayState = TimeItemDisplay.ALL
                    viewModel.notifyDisplayChange()
                }
            }
        }

        /**
         *  Buttons on the bottom of the page: Navigate to tag fragment
         * */
        binding.icAddTag.setOnClickListener {
            findNavController().navigate(TagDialogFragmentDirections.actionGlobalTagDialogFragment(
                viewModel.noteToBeUpdated!!))
        }

        /**
         *  Buttons on the bottom of the page: Coauthoring
         * */
        binding.icCoauthoring.setOnClickListener {
            findNavController().navigate(
                CoauthorDialogFragmentDirections.actionGlobalCoauthorDialogFragment(
                    viewModel.noteToBeUpdated!!
                )
            )
        }

        /**
         *  Buttons on the bottom of the page: Share this note
         * */
        binding.icShare.setOnClickListener {
            shareNoteLink()
        }

        /**
         * Toast to warn about Spotify error
         * */
        viewModel.connectErrorMsg.observe(viewLifecycleOwner) {
            if (null != it) {
                binding.errorMsg.text = it
                binding.errorMsg.visibility = View.VISIBLE
            } else {
                binding.errorMsg.visibility = View.GONE
            }

//            it?.let {
//                val customSnack= Snackbar.make(requireView(), "",Snackbar.LENGTH_LONG)
//                val layout = customSnack.view as Snackbar.SnackbarLayout
//                val bind = CustomSnackBarBinding.inflate(layoutInflater)
//                bind.textSnackBar.text = it
//                layout.addView(bind.root)
//                customSnack.view.layoutParams = (customSnack.view.layoutParams as FrameLayout.LayoutParams)
//                    .apply {
//                        gravity = Gravity.TOP
//                        gravity = Gravity.CENTER_HORIZONTAL
//                        width = ViewGroup.LayoutParams.WRAP_CONTENT
//                        topMargin = 180
//                    }
//                layout.setBackgroundColor(Color.TRANSPARENT)
//                customSnack.show()
//            }
        }

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(LoadingDialogDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    requireActivity().supportFragmentManager.setFragmentResult("dismissRequest",
                        bundleOf("doneLoading" to true))
                }
                LoadApiStatus.ERROR -> {
                    requireActivity().supportFragmentManager.setFragmentResult("dismissRequest",
                        bundleOf("doneLoading" to false))
                }
            }
        }

        return binding.root
    }

    private fun shareNoteLink() {
        SharingLinkGenerator.generateSharingLink(
            deepLink = "${SharingLinkGenerator.PREFIX}/spotify_note/${viewModel.noteId}/${viewModel.sourceId}".toUri(),
            previewImageLink = viewModel.noteToBeUpdated?.thumbnail?.toUri()
        ) { generatedLink ->
            Timber.d("generatedLink = $generatedLink")
            shareDeepLink(generatedLink)
        }
    }

    private fun shareDeepLink(deepLink: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                getString(R.string.share_msg_subject,
                    "Spotify podcast",
                    viewModel.noteToBeUpdated?.title)
            )
            putExtra(Intent.EXTRA_TEXT, deepLink)
        }

        startActivity(Intent.createChooser(intent, null))
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

    fun deleteButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            MyApplication.applicationContext(),
            getString(R.string.delete),
            15.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    viewModel.deleteTimeItem(position)
                }
            })
    }

    // Receive action on notification
    private val timestampReceiver = TimestampReceiver(TimestampReceiver.OnActionListener {
        when (it) {
            TimestampReceiver.ACTION_TAKE_TIMESTAMP -> takeTimestamp()
            TimestampReceiver.ACTION_CLIP_START -> startClipping() // save clipStartSec
            TimestampReceiver.ACTION_CLIP_END -> endClipping() // createTimeItem
        }
    })

    private fun registerTimestampReceiver() {
        val filter = IntentFilter().apply {
            addAction(TimestampReceiver.ACTION_TAKE_TIMESTAMP)
            addAction(TimestampReceiver.ACTION_CLIP_START)
            addAction(TimestampReceiver.ACTION_CLIP_END)
        }
        context?.registerReceiver(timestampReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        SpotifyService.disconnect()
        Timber.d("onDestroy, ACTION_STOP")
        Intent(context, SpotifyNoteService::class.java).apply {
                action = SpotifyNoteService.ACTION_STOP
                context?.startService(this)
        }

        try {
            context?.unregisterReceiver(timestampReceiver)
        } catch (e: IllegalArgumentException) {
            // Do nothing if the receiver not registered
        }

        Timber.d("onDestroy() CALLED")
    }

    /**
     *  Take timestamp / clip
     * */
    private fun takeTimestamp() {
        Timber.d("takeTimestamp")
        viewModel.createTimeItem((viewModel.currentSecond / 1000).toFloat(), null)
    }

    private fun startClipping() {
        Timber.d("clipStart")
        viewModel.startAt = (viewModel.currentSecond / 1000).toFloat()

        val animation = AnimationUtils.loadAnimation(context, R.anim.ic_clipping)

        binding.btnClip.apply {
            startAnimation(animation)
        }
        viewModel.startOrStopToggle = 1

        Intent(context, SpotifyNoteService::class.java).apply {
            this.action = SpotifyNoteService.ACTION_START_CLIPPING
            context?.startService(this)
        }
    }

    private fun endClipping() {
        Timber.d("clipEnd")
        if ((viewModel.currentSecond / 1000).toFloat() < viewModel.startAt) {
            Toast.makeText(context,
                getString(R.string.clip_end_before_start_warning),
                Toast.LENGTH_SHORT).show()
        } else {
            viewModel.endAt = (viewModel.currentSecond / 1000).toFloat()
            binding.btnClip.animation = null
            viewModel.createTimeItem(viewModel.startAt, viewModel.endAt)
            viewModel.startOrStopToggle = 0

            Intent(context, SpotifyNoteService::class.java).apply {
                this.action = SpotifyNoteService.ACTION_DONE_CLIPPING
                context?.startService(this)
            }
        }
    }

    /**
     *  Spotify Auth flow
     * */
    companion object {
        const val CLIENT_ID = "f6095c97a1ab4a7fb88b5ac5f2ba606d"
        const val REDIRECT_URI = "pinpisode://callback"

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

    private fun getLoginActivityCodeIntent(): Intent =
        AuthorizationClient.createLoginActivityIntent(
            activity,
            AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.CODE, REDIRECT_URI)
                .setScopes(
                    arrayOf(
//                        "user-read-currently-playing",
//                        "app-remote-control",
//                        "user-follow-read",
                        "user-read-playback-position",
                        "user-library-read",
                    )
                )
                .setCustomParam("code_challenge_method", "S256")
                .setCustomParam("code_challenge", getCodeChallenge(CODE_VERIFIER))
                .build()
        )

    private val showLoginActivityCode = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

        val authorizationResponse = AuthorizationClient.getResponse(result.resultCode, result.data)

        when (authorizationResponse.type) {
            AuthorizationResponse.Type.CODE -> {
                // Here You will get the authorization code which you
                // can get with authorizationResponse.code

                showLoginActivityToken.launch(getLoginActivityTokenIntent(authorizationResponse.code))
            }
            AuthorizationResponse.Type.ERROR -> {
                Timber.d("AuthorizationResponse.Type.ERROR")
            }
            // Handle the Error

            else -> {}
            // Probably interruption
        }
    }

    private fun getLoginActivityTokenIntent(code: String): Intent =
        AuthorizationClient.createLoginActivityIntent(
            activity,
            AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setCustomParam("grant_type", "authorization_code")
                .setCustomParam("code", code)
                .setCustomParam("code_verifier", CODE_VERIFIER)
                .build()
        )

    private val showLoginActivityToken = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->

        val authorizationResponse = AuthorizationClient.getResponse(result.resultCode, result.data)

        when (authorizationResponse.type) {
            AuthorizationResponse.Type.TOKEN -> {
                // Here You can get access to the authorization token
                // with authorizationResponse.token

                Timber.d("showLoginActivityToken authorizationResponse.expiresIn: ${authorizationResponse.expiresIn}")
                Timber.d("authorizationResponse.accessToken = ${authorizationResponse.accessToken}")
                UserManager.userSpotifyAuthToken = authorizationResponse.accessToken
            }
            AuthorizationResponse.Type.ERROR -> {
                Timber.d("showLoginActivityToken : AuthorizationResponse.Type.ERROR")
            }
            // Handle Error
            else -> {}
            // Probably interruption
        }
    }

}