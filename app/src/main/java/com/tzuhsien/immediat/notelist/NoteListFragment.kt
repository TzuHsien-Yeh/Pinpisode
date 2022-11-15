package com.tzuhsien.immediat.notelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tzuhsien.immediat.data.model.Sort
import com.tzuhsien.immediat.data.source.local.UserManager
import com.tzuhsien.immediat.databinding.FragmentNoteListBinding
import com.tzuhsien.immediat.ext.getVmFactory
import com.tzuhsien.immediat.ext.parseDuration
import com.tzuhsien.immediat.notification.NotificationFragmentDirections
import com.tzuhsien.immediat.signin.SignInFragmentDirections
import com.tzuhsien.immediat.spotifynote.SpotifyNoteFragmentDirections
import com.tzuhsien.immediat.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber

class NoteListFragment : Fragment() {

    private val viewModel by viewModels<NoteListViewModel> {
        getVmFactory()
    }
    private lateinit var binding: FragmentNoteListBinding

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
        val noteAdapter = NoteAdapter(onClickListener = NoteAdapter.OnNoteClickListener {
            viewModel.navigateToNotePage(it)
        })
        binding.recyclerviewNoteList.adapter = noteAdapter

        viewModel.liveNoteList.observe(viewLifecycleOwner) { list ->
            Timber.d("viewModel.liveNoteList.observe: $list")
            list?.let {
                viewModel.getAllTags(list)
                if (null != viewModel.selectedTag) {
                    noteAdapter.submitList(list.filter { it.tags.contains(viewModel.selectedTag) })

                } else {
                    noteAdapter.submitList(list)
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

        }
        // TODO: getLiveInvitationDATA and show badge on icon

        return binding.root
    }

}
