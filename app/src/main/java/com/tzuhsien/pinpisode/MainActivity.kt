package com.tzuhsien.pinpisode

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tzuhsien.pinpisode.data.source.local.UserManager
import com.tzuhsien.pinpisode.databinding.ActivityMainBinding
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.ext.extractYoutubeVideoId
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.notelist.NoteListFragmentDirections
import com.tzuhsien.pinpisode.signin.SignInFragmentDirections
import com.tzuhsien.pinpisode.spotifynote.SpotifyNoteFragmentDirections
import com.tzuhsien.pinpisode.youtubenote.YouTubeNoteFragmentDirections
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel> { getVmFactory() }
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        /** Read intent data **/

        Timber.d("onNewIntent CALLED")
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        intent?.data?.let {
            if (it.toString().contains("http://pinpisode/")) {
                navController.handleDeepLink(intent)
                Timber.d("[onNewIntent] handleDeepLink called")
            }
        }

        intent?.extras?.let {
            handleIntent(it)
            Timber.d("[onNewIntent] handleIntent called: extra = ${it.toString()}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize timber
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        navController = findNavController(R.id.nav_host_fragment_activity_main)

        /** Read intent data if the activity has not been created when getting intent from other app **/
        Timber.d("onCreate CALLED")
        Timber.d("intent.data: ${intent.data}, ${intent.data?.host}, ${intent.data?.query}")
        Timber.d("intent.extra: ${intent.extras?.getString(Intent.EXTRA_TEXT)}")

        if (null != intent) {
            intent.data?.let {
                if (it.toString().contains("http://pinpisode/")) {
                    navController.handleDeepLink(intent)
                    Timber.d("[onCreate] handleDeepLink called")
                }
            }

            intent.extras?.let {
                handleIntent(it)
                Timber.d("[onCreate] handleIntent called")
            }
        }

        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.navigationIcon = null
        binding.toolbarText.text = getString(R.string.pin_your_episodes)

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {

                R.id.noteListFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.navigationIcon = null
                    binding.toolbarText.text = getString(R.string.pin_your_episodes)
                }

                R.id.searchFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.find_your_episodes)
                }

                R.id.profileFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.profile)
                }

                R.id.youTubeNoteFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.youtube_note)
                }

                R.id.spotifyNoteFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.spotify_note)
                }

                R.id.signInFragment -> {
                    binding.toolbar.visibility = View.GONE
                    binding.toolbarText.text = null
                    binding.toolbar.navigationIcon = null
                }

                R.id.notificationFragment -> {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.setNavigationIcon(R.drawable.icons_24px_back02)
                    binding.toolbar.setNavigationOnClickListener {
                        navController.navigate(NoteListFragmentDirections.actionGlobalNoteListFragment())
                    }
                    binding.toolbarText.text = getString(R.string.coauthor_invitation)
                }

            }
        }

    }

    override fun onBackPressed() {
        navController.navigateUp()
    }
    /**
     *  EditText behavior in MainActivity (exit edit mode when the user touched other places)
     * */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v: View? = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.toolbar.visibility = View.GONE
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.toolbar.visibility = View.VISIBLE
        }
    }

    private fun handleIntent(intentExtras: Bundle) {

        navController = findNavController(R.id.nav_host_fragment_activity_main)

        /**
         * Check and handle sign in status
         * */
        if (null == GoogleSignIn.getLastSignedInAccount(applicationContext)) {
            Timber.d("[${this::class.simpleName}]: null == GoogleSignIn.getLastSignedInAccount(applicationContext)")
            navController.navigate(SignInFragmentDirections.actionGlobalSignInFragment())
        } else {
            viewModel.updateLocalUserId()
        }

        if (UserManager.userId != null) {
            intentExtras.getString(Intent.EXTRA_TEXT)?.let { extra ->
                if (extra.contains("spotify")) {
                    // Handle Spotify intent

                    val sourceId =
                        intentExtras.getString(Intent.EXTRA_TEXT)?.extractSpotifySourceId()
                    sourceId?.let { it ->

                        Timber.d("HANDLE spotify INTENT FUN intent extras : $it")

                        navController.navigate(
                            SpotifyNoteFragmentDirections.actionGlobalSpotifyNoteFragment(
                                sourceIdKey = it)
                        )
                    }

                } else {
                    // Handle YouTube intent
                    val videoId = intentExtras.getString(Intent.EXTRA_TEXT)?.extractYoutubeVideoId()
                    videoId?.let {
                        Timber.d("HANDLE youtube INTENT FUN intent extras : $it")

                        navController.navigate(
                            YouTubeNoteFragmentDirections.actionGlobalYouTubeNoteFragment(videoIdKey = it)
                        )
                    }
                }

            }
        }

    }
}
