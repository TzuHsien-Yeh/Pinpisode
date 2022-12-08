package com.tzuhsien.pinpisode.tag

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.tzuhsien.pinpisode.NavGraphDirections
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.DialogTagBinding
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.loading.BUNDLE_KEY_DONE_LOADING
import com.tzuhsien.pinpisode.loading.REQUEST_KEY_DISMISS
import com.tzuhsien.pinpisode.network.LoadApiStatus
import timber.log.Timber


class TagDialogFragment : AppCompatDialogFragment() {

    private val viewModel by viewModels<TagViewModel> { getVmFactory(TagDialogFragmentArgs.fromBundle(requireArguments()).noteKey) }
    private lateinit var binding: DialogTagBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.TagDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): ConstraintLayout {

        binding = DialogTagBinding.inflate(inflater, container, false)
        binding.layoutTags.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_slide_up))

        for (t in viewModel.allTags) {
            val chip = inflater.inflate(R.layout.chip_tag, binding.chipGroupTags, false) as Chip
            chip.isClickable = true
            chip.isCheckable = true
            chip.isChecked = viewModel.tagsOfCurrentNote.contains(t)
            chip.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateTagSet(t, isChecked)
            }
            chip.text = (t)
            binding.chipGroupTags.addView(chip)
        }

        binding.editAddNewTag.doAfterTextChanged {
            viewModel.inputNewTag = it.toString().trim()
        }
        binding.btnAddNewTag.setOnClickListener {
            if (!viewModel.inputNewTag.isNullOrEmpty()) {

                viewModel.addNewTag()

                val newChip = inflater.inflate(R.layout.chip_tag, binding.chipGroupTags, false) as Chip
                newChip.isClickable = true
                newChip.isCheckable = true
                newChip.isChecked = true
                newChip.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.updateTagSet(viewModel.inputNewTag!!, isChecked)
                }
                newChip.text = (viewModel.inputNewTag)
                binding.chipGroupTags.addView(newChip)
            }

        }

        binding.buttonCloseTagDialog.setOnClickListener {
            viewModel.saveChanges()
        }
        binding.windowBackground.setOnClickListener {
            Timber.d("binding.windowBackground.setOnClickListener")
            viewModel.saveChanges()
        }

        viewModel.leave.observe(viewLifecycleOwner, Observer {
            if (it){
                dismiss()
                viewModel.onLeaveCompleted()
            }
        })

        /** Loading status **/
        viewModel.status.observe(viewLifecycleOwner) {
            when(it) {
                LoadApiStatus.LOADING -> {
                    if (findNavController().currentDestination?.id != R.id.loadingDialog) {
                        findNavController().navigate(NavGraphDirections.actionGlobalLoadingDialog())
                    }
                }
                LoadApiStatus.DONE -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_KEY_DISMISS,
                        bundleOf(BUNDLE_KEY_DONE_LOADING to true))
                }
                LoadApiStatus.ERROR -> {
                    requireActivity().supportFragmentManager.setFragmentResult(REQUEST_KEY_DISMISS,
                        bundleOf(BUNDLE_KEY_DONE_LOADING to false))
                }
            }
        }

        return binding.root
    }

    override fun dismiss() {
        binding.layoutTags.startAnimation(AnimationUtils.loadAnimation(context, R.anim.anim_slide_down))
        Handler().postDelayed({ super.dismiss() }, 200)
    }

}