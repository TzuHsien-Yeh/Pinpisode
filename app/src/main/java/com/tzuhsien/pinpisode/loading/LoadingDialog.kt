package com.tzuhsien.pinpisode.loading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import com.tzuhsien.pinpisode.R
import com.tzuhsien.pinpisode.databinding.DialogLoadingBinding
import timber.log.Timber

class LoadingDialog : AppCompatDialogFragment() {

    private lateinit var binding: DialogLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.TagDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DialogLoadingBinding.inflate(layoutInflater)

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "dismissRequest",
         this,
        ) { _, bundle ->

            Timber.d("setFragmentResultListener bundle: ${bundle.getBoolean("doneLoading")}")
            when (bundle.getBoolean("doneLoading")) {
                true -> {
                    dismiss()
                }
                false -> {} // show pic to notify error
            }
        }

        val animation = AnimationUtils.loadAnimation(context, R.anim.ic_clipping)

//        Glide.with(binding.imgLoading)
//            .load(R.raw.pinpisode_logo_with_text)
//            .into(binding.imgLoading)

//        binding.imgLoading.apply {
//            startAnimation(animation)
//        }

        return binding.root
    }

    override fun dismiss() {
//        Handler().postDelayed({ super.dismiss() }, 500)
        super.dismiss()
        Timber.d("loading dialog dismissed")
    }

}