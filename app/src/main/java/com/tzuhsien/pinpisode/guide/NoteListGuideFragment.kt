package com.tzuhsien.pinpisode.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.data.model.Sort
import com.tzuhsien.pinpisode.databinding.DialogNoteListGuideBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import kotlinx.coroutines.*
import timber.log.Timber


class NoteListGuideFragment : AppCompatDialogFragment() {

    private val viewModel by viewModels<NoteListGuideViewModel> { getVmFactory() }
    private lateinit var binding: DialogNoteListGuideBinding

    private val animJob = Job()
    val coroutineScope = CoroutineScope(animJob + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.GuideDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        binding = DialogNoteListGuideBinding.inflate(layoutInflater)

        binding.root.setOnClickListener { viewModel.showNext() }

        binding.viewGroupNoteListElements.children.forEach { it.alpha = 0.05f }

        viewModel.isToShowWelcome.observe(viewLifecycleOwner) {
            binding.textStartGuideGreeting.visibility = if (it) View.VISIBLE else View.GONE
            binding.imgLogoPinpisode.visibility = if (it) View.VISIBLE else View.GONE
            binding.arrow.visibility = if (it) View.GONE else View.VISIBLE
        }

        viewModel.isToShowAddNotes.observe(viewLifecycleOwner) {
            binding.imgShareFromYt.visibility = if (it) View.VISIBLE else View.GONE
            binding.textAddNoteBySharing.visibility = if (it) View.VISIBLE else View.GONE
            binding.lottieTouchEffect.visibility =  if (it) View.VISIBLE else View.GONE

            if (it) {
                binding.lottieTouchEffect.setAnimation(R.raw.click_effect)
                binding.arrow.apply {
                    visibility = View.VISIBLE
                    setAnimation(R.raw.arrow)
                    rotation = 108f
                }
            }
        }

        viewModel.isToShowSearch.observe(viewLifecycleOwner) {
            binding.textToSearchPage.visibility = if (it) View.VISIBLE else View.GONE
            binding.btnToSearchPage.alpha = if (it) 1f else 0.1f
            if (it) pointToSearchBtn()
        }

        viewModel.isToShowHowToSort.observe(viewLifecycleOwner) {
            binding.textHowToSortNote.visibility = if (it) View.VISIBLE else View.GONE
            initializeSortAnimSetting(it)
            if (it) {
                sortAnimation()
                binding.arrow.visibility = View.GONE
            }
        }

        viewModel.isToShowCoauthorInvitation.observe(viewLifecycleOwner) {
            binding.textCoauthorInvitation.visibility = if (it) View.VISIBLE else View.GONE
            binding.btnNotificationBell.alpha = if (it) 1f else 0.1f
            if (it) {
                binding.cardSelectedTag.visibility = View.GONE
                pointToNotificationBellIcon()
            }
        }

        viewModel.isToShowQuit.observe(viewLifecycleOwner) {
            binding.arrow.visibility = View.GONE
            binding.textHowToSortNote.visibility = if (it) View.VISIBLE else View.GONE
            binding.cardNoteYoutube.alpha = if (it) 1f else 0.1f
            binding.cardNote.alpha = if (it) 1f else 0.1f
            binding.swipeLeft.visibility = if (it) View.VISIBLE else View.GONE
            binding.coverUpBuggedLottie.apply {
                alpha = if (it) 1f else 0.1f
                visibility = if (it) View.VISIBLE else View.GONE
            }
            if (it) {
                binding.textHowToSortNote.text = getString(R.string.how_to_quit_notes)
                binding.swipeLeft.apply {
                    alpha = 1f
                    setAnimation(R.raw.swipe_left)
                    loop(true)
                    playAnimation()
                }
            }
        }

        viewModel.isToShowClosure.observe(viewLifecycleOwner) {
            binding.arrow.visibility = if (it) View.GONE else View.VISIBLE
            binding.textGuideClosure.visibility = if (it) View.VISIBLE else View.GONE
            binding.textLetsGetStarted.visibility = if (it) View.VISIBLE else View.GONE

            if (it) {
                binding.imgLogoPinpisode.visibility = View.VISIBLE
                pointToHelpIconOnToolbar()
            }
        }

        viewModel.leave.observe(viewLifecycleOwner) {
            if (it) {
                dismiss()
                viewModel.doneDismissGuide()
            }
        }
        return binding.root
    }

    private fun pointToSearchBtn() {
        binding.arrow.apply {
            visibility = View.VISIBLE
            rotation = 60f

            val constraintLayout: ConstraintLayout = binding.root
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            constraintSet.connect(binding.arrow.id,
                ConstraintSet.END,
                binding.btnToSearchPage.id,
                ConstraintSet.END,
                0)
            constraintSet.connect(binding.arrow.id,
                ConstraintSet.TOP,
                binding.textToSearchPage.id,
                ConstraintSet.BOTTOM,
                0)
            constraintSet.connect(binding.arrow.id,
                ConstraintSet.BOTTOM,
                binding.btnToSearchPage.id,
                ConstraintSet.BOTTOM,
                30)
            constraintSet.connect(binding.arrow.id,
                ConstraintSet.START,
                binding.textToSearchPage.id,
                ConstraintSet.START,
                35)
            constraintSet.setHorizontalBias(binding.arrow.id, 0.92f)
            constraintSet.setVerticalBias(binding.arrow.id, 0f)
            constraintSet.applyTo(constraintLayout)
        }
    }

    private fun initializeSortAnimSetting(ready: Boolean) {
        binding.cardNoteYoutube.alpha = if (ready) 1f else 0.1f
        binding.cardNote.alpha = if (ready) 1f else 0.1f
        binding.firstTagInRecyclerview.alpha = if (ready) 1f else 0.1f
        binding.SecondTagInRecyclerview.alpha = if (ready) 1f else 0.1f
        binding.btnSwitchDirection.alpha = if (ready) 1f else 0.1f
        binding.sortAsc.alpha = if (ready) 1f else 0.1f
        binding.sortDesc.alpha = if (ready) 0.5f else 0.1f
        binding.cardSortBy.alpha = if (ready) 1f else 0.1f
        binding.textSortOptions.text = Sort.LAST_EDIT.VALUE
    }

    private fun sortAnimation() {

        Timber.d("sortAnimation, viewModel.trickNumber == ${viewModel.trickNumber}")

        coroutineScope.launch {
            delay(800)
            binding.cardSelectedTag.visibility = View.VISIBLE
            binding.SecondTagInRecyclerview.visibility = View.GONE
            binding.cardSelectedTag.alpha = 1f
            binding.cardNote.visibility = View.GONE

            if (viewModel.trickNumber != 4) {
                resetSort()
                animJob.cancel()
            }
            delay(800)
            binding.sortAsc.alpha = 0.5f
            binding.sortDesc.alpha = 1f

            if (viewModel.trickNumber != 4) {
                resetSort()
                animJob.cancel()
            }
            delay(800)
            binding.textSortOptions.text = Sort.DURATION.VALUE

            if (viewModel.trickNumber != 4) {
                animJob.cancel()
                resetSort()
            }
            delay(800)
            binding.textSortOptions.text = Sort.TIME_LEFT.VALUE

            if (viewModel.trickNumber != 4) {
                animJob.cancel()
                resetSort()
            }
            delay(800)
            resetSort()
            sortAnimation()
        }
    }

    private fun resetSort() {
        binding.cardNote.visibility = View.VISIBLE
        binding.SecondTagInRecyclerview.visibility = View.VISIBLE
        binding.cardSelectedTag.visibility = View.GONE
        binding.sortAsc.alpha = 1f
        binding.sortDesc.alpha = 0.5f
        binding.textSortOptions.text = Sort.LAST_EDIT.VALUE
    }

    private fun pointToNotificationBellIcon() {
        binding.arrow.apply {
            visibility = View.VISIBLE
            rotation = -50f
        }

        val constraintLayout: ConstraintLayout = binding.root
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.TOP,
            binding.btnNotificationBell.id,
            ConstraintSet.BOTTOM,
            0)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.BOTTOM,
            binding.textCoauthorInvitation.id,
            ConstraintSet.TOP,
            30)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            0)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            12)
        constraintSet.setHorizontalBias(binding.arrow.id, 0.88f)
        constraintSet.applyTo(constraintLayout)
    }

    private fun pointToHelpIconOnToolbar() {
        binding.arrow.apply {
            visibility = View.VISIBLE
            rotation = -50f
        }

        val constraintLayout: ConstraintLayout = binding.root
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            0)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            0)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            0)
        constraintSet.connect(binding.arrow.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            0)
        constraintSet.setHorizontalBias(binding.arrow.id, 0.92f)
        constraintSet.setVerticalBias(binding.arrow.id, 0f)
        constraintSet.applyTo(constraintLayout)
    }

    override fun dismiss() {
        Timber.d("dismissed")
        findNavController().navigate(NavGraphDirections.actionGlobalNoteListFragment())
    }

    override fun onStop() {
        super.onStop()
        animJob.cancel()
    }

}