package com.google.android.youtube.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.youtube.player.internal.ab
import com.tzuhsien.immediat.ImMediAtApplication
import java.util.*

class YouTubePlayerSupportFragment : Fragment(), YouTubePlayer.Provider {
    private val a = ViewBundle()
    private var b: Bundle? = null
    private var c: YouTubePlayerView? = null
    private var d: String? = null
    private var e: YouTubePlayer.OnInitializedListener? = null
    override fun initialize(var1: String, var2: YouTubePlayer.OnInitializedListener) {
        d = ab.a(var1, ImMediAtApplication.YT_API_KEY)
        e = var2
        a()
    }

    private fun a() {
        if (c != null && e != null) {
            c?.a(this.activity, this, d, e, b)
            b = null
            e = null
        }
    }

    override fun onCreate(var1: Bundle?) {
        super.onCreate(var1)
        b = var1?.getBundle("YouTubePlayerSupportFragment.KEY_PLAYER_VIEW_STATE")
    }

    override fun onCreateView(var1: LayoutInflater, var2: ViewGroup?, var3: Bundle?): android.view.View? {
        c = YouTubePlayerView(Objects.requireNonNull(this.activity), null, 0, a) // and this line compiles but gives red warning
        a()
        return c
    }

    override fun onStart() {
        super.onStart()
        c?.a()
    }

    override fun onResume() {
        super.onResume()
        c?.b()
    }

    override fun onPause() {
        c?.c()
        super.onPause()
    }

    override fun onSaveInstanceState(var1: Bundle) {
        super.onSaveInstanceState(var1)
        (if (c != null) c?.e() else b)?.let { var2 ->
            var1.putBundle("YouTubePlayerSupportFragment.KEY_PLAYER_VIEW_STATE", var2)
        }
    }

    override fun onStop() {
        c?.d()
        super.onStop()
    }

    override fun onDestroyView() {
        this.activity?.let { c?.c(it.isFinishing)  }
        c = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (c != null) {
            val var1 = this.activity
            c?.b(var1 == null || var1.isFinishing)
        }
        super.onDestroy()
    }

    private inner class ViewBundle : YouTubePlayerView.b {
        override fun a(var1: YouTubePlayerView, var2: String, var3: YouTubePlayer.OnInitializedListener) {
            e?.let { initialize(var2, it) }
        }

        override fun a(var1: YouTubePlayerView) {}
    }

    companion object {
        fun newInstance(): YouTubePlayerSupportFragment {
            return YouTubePlayerSupportFragment()
        }
    }
}