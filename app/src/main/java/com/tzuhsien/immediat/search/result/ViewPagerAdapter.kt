package com.tzuhsien.immediat.search.result

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.tzuhsien.immediat.data.model.Source
import com.tzuhsien.immediat.search.SearchFragment

class ViewPagerAdapter(fragmentActivity: SearchFragment, private var totalCount: Int) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return totalCount
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> newInstance(Source.YOUTUBE)
            1 -> newInstance(Source.SPOTIFY)
            else -> throw IllegalArgumentException("Unknown Fragment")
        }
    }

    private fun newInstance(source: Source): Fragment {
        val fragment = SearchResultFragment()
        val args = Bundle()
        args.putSerializable("sourceKey", source)
        fragment.arguments = args
        return fragment
    }
}