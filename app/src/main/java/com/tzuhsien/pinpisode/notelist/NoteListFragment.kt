package com.tzuhsien.pinpisode.notelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tzuhsien.pinpisode.MyApplication
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Sort
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.databinding.FragmentNoteListBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.ext.parseDuration
import com.tzuhsien.pinpisode.loading.LoadingDialogDirections
import com.tzuhsien.pinpisode.network.LoadApiStatus
import com.tzuhsien.pinpisode.notification.NotificationFragmentDirections
import com.tzuhsien.pinpisode.signin.SignInFragmentDirections
import com.tzuhsien.pinpisode.spotifynote.SpotifyNoteFragmentDirections
import com.tzuhsien.pinpisode.util.SwipeHelper
import com.tzuhsien.pinpisode.youtubenote.YouTubeNoteFragmentDirections
import kotlinx.coroutines.*
import timber.log.Timber

class NoteListFragment : Fragment() {

    private val viewModel by viewModels<NoteListViewModel> {
        getVmFactory()
    }
    private lateinit var binding: FragmentNoteListBinding
    private val scrollJob = Job()
    val coroutineScope = CoroutineScope(scrollJob + Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("Timber[${this::class.simpleName}] NoteListFragment[onCreateView]: ${UserManager.userId}")

        /**
         * Check and handle sign in status
         * */
        if (null == GoogleSignIn.getLastSignedInAccount(requireContext())) {
            findNavController().navigate(SignInFragmentDirections.actionGlobalSignInFragment())
        } else {
            viewModel.updateLocalUserId()
        }

        binding = FragmentNoteListBinding.inflate(layoutInflater)

        /**
         *  Navigation Buttons
         * */
        Glide.with(binding.imgPicToProfile)
            .load(UserManager.userPic)
            .into(binding.imgPicToProfile)
        binding.textUserName.text = UserManager.userName
        binding.imgPicToProfile.setOnClickListener {
            findNavController().navigate(NoteListFragmentDirections.actionNoteListFragmentToProfileFragment())
        }
        binding.textUserName.setOnClickListener {
            findNavController().navigate(NoteListFragmentDirections.actionNoteListFragmentToProfileFragment())
        }

        binding.btnToSearchPage.setOnClickListener {
            findNavController().navigate(NoteListFragmentDirections.actionNoteListFragmentToSearchFragment())
        }

        /**
         * Tag list
         * */
        val tagAdapter = TagAdapter(onClickListener = TagAdapter.OnTagClickListener {
            binding.textSelectedTag.text = it
            binding.cardSelectedTag.visibility = View.VISIBLE
            viewModel.tagSelected(it)
        })
        binding.recyclerviewTag.adapter = tagAdapter
        viewModel.tagSet.observe(viewLifecycleOwner, Observer { set ->
            if (null != set) {
                tagAdapter.submitList(set.filter { it != viewModel.selectedTag }.sorted().toList())
            } else {
                tagAdapter.submitList(set)
            }
        })
        binding.cardSelectedTag.setOnClickListener {
            it.visibility = View.GONE
            viewModel.tagSelected(null)
        }

        /**
         * Sorting and ordering
         * */
        var sortState = 0

        binding.textSortOptions.text = Sort.LAST_EDIT.VALUE
        binding.sortAsc.alpha = 1F
        binding.sortDesc.alpha = 0.5F
        binding.cardSortBy.setOnClickListener {
            when (sortState) {
                0 -> {
                    binding.textSortOptions.text = Sort.DURATION.VALUE
                    binding.sortAsc.alpha = 1F
                    binding.sortDesc.alpha = 0.5F
                    viewModel.isAscending = true
                    viewModel.sortNotes(Sort.DURATION)
                    sortState = 1
                }
                1 -> {
                    binding.textSortOptions.text = Sort.TIME_LEFT.VALUE
                    binding.sortAsc.alpha = 0.5F
                    binding.sortDesc.alpha = 1F
                    viewModel.isAscending = false
                    viewModel.sortNotes(Sort.TIME_LEFT)
                    sortState = 2
                }
                2 -> {
                    binding.textSortOptions.text = Sort.LAST_EDIT.VALUE
                    binding.sortAsc.alpha = 1F
                    binding.sortDesc.alpha = 0.5F
                    viewModel.isAscending = true
                    viewModel.sortNotes(Sort.LAST_EDIT)
                    sortState = 0
                }
            }
        }
        binding.btnSwitchDirection.setOnClickListener {
            if (binding.sortAsc.alpha != 1F) {
                //change to DESC
                viewModel.isAscending = false
                viewModel.changeOrderDirection()
                binding.sortAsc.alpha = 1F
                binding.sortDesc.alpha = 0.5F
            } else {
                //change to ASC
                viewModel.isAscending = true
                viewModel.changeOrderDirection()
                binding.sortAsc.alpha = 0.5F
                binding.sortDesc.alpha = 1F
            }
        }

        /**
         * Note list
         * */
        val noteAdapter = NoteAdapter(uiState = viewModel.uiState)
        binding.recyclerviewNoteList.adapter = noteAdapter

        // Swipe to delete
        binding.recyclerviewNoteList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(
            binding.recyclerviewNoteList,
            swipeOutListener = OnSwipeOutListener {
                Timber.d("swipeOutListener = OnSwipeOutListener position = $it")
                viewModel.deleteOrQuitCoauthoringNote(it)
            }
        ) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                val deleteButton = deleteButton(position)
                return listOf(deleteButton)
            }

        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerviewNoteList)

        viewModel.liveNoteList.observe(viewLifecycleOwner) { list ->
            Timber.d("viewModel.liveNoteList.observe: $list")
            list?.let {
                viewModel.getAllTags(list)
                if (null != viewModel.selectedTag) {
                    noteAdapter.submitList(list.filter { it.tags.contains(viewModel.selectedTag) })
                    coroutineScope.launch {
                        delay(200L)
                        binding.recyclerviewNoteList.scrollToPosition(0)
                    }

                } else {
                    noteAdapter.submitList(list)
                    coroutineScope.launch {
                        delay(200L)
                        binding.recyclerviewNoteList.scrollToPosition(0)
                    }
                }

                for (note in list) {
                    if (note.duration.parseDuration() == 0L) {
                        viewModel.updateInfoFromYouTube(note.id, note)
                    }
                }
            }
        }

        viewModel.navigateToYoutubeNote.observe(viewLifecycleOwner, Observer {
            it?.let {
                findNavController().navigate(
                    YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(
                        noteIdKey = it.id,
                        videoIdKey = it.sourceId
                    )
                )
                viewModel.doneNavigationToNote()
            }
        })

        viewModel.navigateToSpotifyNote.observe(viewLifecycleOwner) {
            it?.let {
                findNavController().navigate(
                    SpotifyNoteFragmentDirections.actionGlobalSpotifyNoteFragment(
                        noteIdKey = it.id,
                        sourceIdKey = it.sourceId
                    )
                )
                viewModel.doneNavigationToNote()
            }
        }

        /**
         * Notification
         * */
        binding.btnNotificationBell.setOnClickListener {
            findNavController().navigate(NotificationFragmentDirections.actionGlobalNotificationFragment())
        }

        viewModel.invitationList.observe(viewLifecycleOwner) {
            binding.badgeNotificationNotEmpty.visibility = if (it.isNotEmpty()) View.VISIBLE else View.GONE
        }

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when(it) {
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

    override fun onStop() {
        super.onStop()
        scrollJob.cancel()
    }

    fun deleteButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            MyApplication.applicationContext(),
            getString(R.string.quit),
            16.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    viewModel.deleteOrQuitCoauthoringNote(position)
                }
            })
    }


}
