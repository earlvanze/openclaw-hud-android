package ai.openclaw.app

import ai.openclaw.app.ui.HudScreen
import ai.openclaw.app.ui.OpenClawTheme
import android.app.Presentation
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

internal class HudPresentation(
    private val activity: ComponentActivity,
    display: Display,
    private val viewModel: MainViewModel,
    private val onHudKeyEvent: (KeyEvent) -> Boolean,
) : Presentation(activity, display) {
    private val systemBarsHandler = Handler(Looper.getMainLooper())
    private val keepSystemBarsHidden =
        object : Runnable {
            override fun run() {
                applyHudWindowFlags()
                systemBarsHandler.postDelayed(this, SYSTEM_BARS_HIDE_INTERVAL_MS)
            }
        }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        applyHudWindowFlags()

        val composeView =
            ComposeView(activity).apply {
                setViewTreeLifecycleOwner(activity)
                setViewTreeViewModelStoreOwner(activity)
                setViewTreeSavedStateRegistryOwner(activity)
                setContent {
                    CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {
                        OpenClawTheme {
                            Surface(modifier = Modifier, color = ComposeColor.Black) {
                                HudScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        setContentView(composeView)
        window?.decorView?.setOnSystemUiVisibilityChangeListener {
            applyHudWindowFlags()
        }
    }

    override fun onStart() {
        super.onStart()
        applyHudWindowFlags()
        systemBarsHandler.removeCallbacks(keepSystemBarsHidden)
        systemBarsHandler.post(keepSystemBarsHidden)
    }

    override fun onStop() {
        systemBarsHandler.removeCallbacks(keepSystemBarsHidden)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        applyHudWindowFlags()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (onHudKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION")
    private fun applyHudWindowFlags() {
        val presentationWindow = window ?: return
        presentationWindow.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        presentationWindow.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )
        presentationWindow.statusBarColor = Color.BLACK
        presentationWindow.navigationBarColor = Color.BLACK
        WindowCompat.setDecorFitsSystemWindows(presentationWindow, false)
        presentationWindow.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        presentationWindow.attributes =
            presentationWindow.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        WindowInsetsControllerCompat(presentationWindow, presentationWindow.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        private const val SYSTEM_BARS_HIDE_INTERVAL_MS = 1_000L
    }
}
