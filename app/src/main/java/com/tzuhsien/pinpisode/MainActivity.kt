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
import com.tzuhsien.pinpisode.data.model.Source
import com.tzuhsien.pinpisode.databinding.ActivityMainBinding
import com.tzuhsien.pinpisode.ext.extractSpotifySourceId
import com.tzuhsien.pinpisode.ext.extractYoutubeVideoId
import com.tzuhsien.pinpisode.ext.getVmFactory
import com.tzuhsien.pinpisode.util.DYNAMIC_LINK_PREFIX
import timber.log.Timber

private const val ICON_ALPHA = 130

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
            if (it.toString().contains(DYNAMIC_LINK_PREFIX)) {
                navController.handleDeepLink(intent)
                Timber.d("[onNewIntent] handleDeepLink called")
            }
        }

        intent?.extras?.let {
            handleIntent(it)
            Timber.d("[onNewIntent] handleIntent called: extra = $it")
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
                if (it.toString().contains(DYNAMIC_LINK_PREFIX)) {
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
        supportActionBar?.setDisplayShowTitleEnabled(false)

        navController.addOnDestinationChangedListener { controller, destination, _ ->

            when (destination.id) {
                R.id.noteListFragment -> {
                    setUpToolbarUi(
                        isToolbarVisible = true,
                        toolbarText = getString(R.string.pin_your_episodes),
                        isHelpIconVisible = true,
                        onHelpIconClick = {
                            if (navController.currentDestination?.id != R.id.noteListGuideFragment) {
                                controller.navigate(NavGraphDirections.actionGlobalNoteListGuideFragment())
                            }
                        },
                        isNavIconVisible = false,
                    )
                }

                R.id.searchFragment -> {
                    setUpToolbarUi(
                        isToolbarVisible = true,
                        toolbarText = getString(R.string.find_your_episodes),
                    )
                }

                R.id.profileFragment -> {
                    setUpToolbarUi(
                        isToolbarVisible = true,
                        toolbarText = getString(R.string.profile),
                    )
                }

                R.id.youTubeNoteFragment -> {
                    setUpToolbarUi(
                        isToolbarVisible = true,
                        toolbarText = getString(R.string.spotify_note),
                        platformLogo = R.drawable.logo_youtube,
                        onNavIconClick = {
                            controller.navigate(NavGraphDirections.actionGlobalNoteListFragment())
                        },
//                        isHelpIconVisible = true
                    )
                }

                R.id.spotifyNoteFragment -> {
                    setUpToolbarUi(
                        isToolbarVisible = true,
                        toolbarText = getString(R.string.spotify_note),
                        platformLogo = R.drawable.spotify_logo_rgb_black,
                        onNavIconClick = {
                            controller.navigate(NavGraphDirections.actionGlobalNoteListFragment())
                        },
//                        isHelpIconVisible = true
                    )
                }

                R.id.signInFragment -> {
                    setUpToolbarUi()
                }

                R.id.notificationFragment -> {
                    setUpToolbarUi(
                        isToolbarVisible = true,
                        toolbarText = getString(R.string.coauthor_invitation),
                        isNavIconVisible = true
                    )
                }
            }
        }
    }

    private fun setUpToolbarUi(
        isToolbarVisible: Boolean = false,
        platformLogo: Int? = null,
        toolbarText: String? = null,
        isNavIconVisible: Boolean = true,
        onNavIconClick: View.OnClickListener = View.OnClickListener { onBackPressed() },
        isHelpIconVisible: Boolean = false,
        onHelpIconClick: View.OnClickListener? = null,
    ) {
        binding.toolbar.visibility = if (isToolbarVisible) View.VISIBLE else View.GONE
        binding.toolbarText.text = toolbarText
        binding.logoPlatform.apply {
            visibility = if (null != platformLogo) {
                setImageResource(platformLogo)
                View.VISIBLE
            } else {
                View.GONE
            }
        }



        if (isNavIconVisible) {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back)
            binding.toolbar.navigationIcon?.apply {
                alpha = ICON_ALPHA
                setTint(getColor(R.color.back_icon_color_to_theme))
            }
        } else {
            binding.toolbar.navigationIcon = null
        }
        binding.toolbar.setNavigationOnClickListener(onNavIconClick)

        binding.helperIcon.visibility = if (isHelpIconVisible) View.VISIBLE else View.GONE
        binding.helperIcon.setOnClickListener(onHelpIconClick)
    }

    override fun onBackPressed() {
        navController.navigateUp()
    }

    /**
     *  EditText behavior in MainActivity (exit edit mode when the user touched other places)
     * */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view: View? = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    view.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
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

        intentExtras.getString(Intent.EXTRA_TEXT)?.let { extra ->

            if (extra.contains(Constants.SPOTIFY)) {
                // Handle Spotify intent
                val sourceId = intentExtras.getString(Intent.EXTRA_TEXT)?.extractSpotifySourceId()

                sourceId?.let { it ->
                    Timber.d("intentExtras.getString(Intent.EXTRA_TEXT)sourceId = $sourceId")
                    /** Check and handle sign in status **/
                    if (null == viewModel.getSignedInUser()) {
                        navController.navigate(NavGraphDirections.actionGlobalSignInFragment(
                            source = Source.SPOTIFY.source,
                            sourceId = it
                        ))
                    } else {
                        navController.navigate(
                            NavGraphDirections.actionGlobalSpotifyNoteFragment(
                                sourceIdKey = it)
                        )
                    }
                }

            } else {
                // Handle YouTube intent
                val videoId = intentExtras.getString(Intent.EXTRA_TEXT)?.extractYoutubeVideoId()

                videoId?.let {
                    Timber.d("HANDLE youtube INTENT FUN intent extras : $it")
                    /** Check and handle sign in status **/
                    if (null == viewModel.getSignedInUser()) {
                        navController.navigate(NavGraphDirections.actionGlobalSignInFragment(
                            source = Source.YOUTUBE.source,
                            sourceId = it
                        ))
                    } else {
                        navController.navigate(
                            NavGraphDirections.actionGlobalYouTubeNoteFragment(
                                videoIdKey = it)
                        )
                    }
                }
            }
        }
    }
}
