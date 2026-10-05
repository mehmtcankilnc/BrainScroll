package com.mehmtcan.brainscroll

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mehmtcan.brainscroll.account.DeepLinkInbox

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is dark-only for now, so force dark system bars (light icons)
        // instead of following the device's light/dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        // Only on a real start. After a rotation the same intent is still here, and a login code works once.
        if (savedInstanceState == null) deliverLoginLink(intent)

        setContent {
            App()
        }
    }

    // singleTop: the browser brings the player back through the login link into the running activity.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deliverLoginLink(intent)
    }

    private fun deliverLoginLink(intent: Intent?) {
        intent?.dataString?.let(DeepLinkInbox::deliver)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}