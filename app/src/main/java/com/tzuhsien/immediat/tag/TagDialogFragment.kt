package com.tzuhsien.immediat.tag

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.chip.Chip
import com.tzuhsien.immediat.R
import com.tzuhsien.immediat.R.anim
import com.tzuhsien.immediat.R.layout
import com.tzuhsien.immediat.databinding.DialogTagBinding
import com.tzuhsien.immediat.ext.getVmFactory
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
        binding.layoutTags.startAnimation(AnimationUtils.loadAnimation(context, anim.anim_slide_up))

        for (t in viewModel.allTags) {
            val chip = inflater.inflate(layout.chip_tag, binding.chipGroupTags, false) as Chip
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
            viewModel.inputNewTag = it.toString()
        }
        binding.btnAddNewTag.setOnClickListener {
            if (!viewModel.inputNewTag.isNullOrEmpty()) {

                viewModel.addNewTag()

                val newChip = inflater.inflate(layout.chip_tag, binding.chipGroupTags, false) as Chip
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

        return binding.root
    }

    override fun dismiss() {
        binding.layoutTags.startAnimation(AnimationUtils.loadAnimation(context, anim.anim_slide_down))
        Handler().postDelayed({ super.dismiss() }, 200)
    }

}