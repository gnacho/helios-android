package club.cloudless.helios.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import club.cloudless.helios.config.ConfigStore
import club.cloudless.helios.widget.WidgetRefreshWorker

/**
 * Entry point. It opens the site in a Trusted Web Activity, which needs the
 * domain to publish `/.well-known/assetlinks.json` with this app's signing
 * certificate. Without that file Chrome shows the address bar instead of the
 * full screen app.
 *
 * There is no Kotlin UI here on purpose: the web app is the UI. Settings live in
 * their own activity, reachable from the launcher shortcut and from the widget.
 */
class MainActivity : Activity() {

    private var connection: CustomTabsServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cfg = ConfigStore.create(this)
        if (!cfg.isConfigured()) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }
        WidgetRefreshWorker.schedule(this)
        open(cfg.baseUrl)
    }

    private fun open(url: String) {
        val uri = Uri.parse(url)
        // A Trusted Web Activity is Chrome-only in practice: the browser must
        // implement the TWA protocol on top of Custom Tabs, and a browser that
        // only speaks Custom Tabs shows a toolbar with the URL and a close
        // button (Fennec does exactly that). Asking for "the default browser"
        // therefore picks the wrong one, so this asks for a known TWA-capable
        // package first and only then falls back.
        val provider = CustomTabsClient.getPackageName(this, TWA_BROWSERS)
        if (provider == null) {
            fallback(uri)
            return
        }
        val conn = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
                val session = runCatching { client.newSession(null) }.getOrNull()
                if (session == null) {
                    fallback(uri)
                    return
                }
                runOnUiThread {
                    try {
                        TrustedWebActivityIntentBuilder(uri).build(session).launchTrustedWebActivity(this@MainActivity)
                        finish()
                    } catch (e: ActivityNotFoundException) {
                        fallback(uri)
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                fallback(uri)
            }
        }
        connection = conn
        if (!CustomTabsClient.bindCustomTabsService(this, provider, conn)) {
            fallback(uri)
        }
    }

    private fun fallback(uri: Uri) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        finish()
    }

    private companion object {
        /** Browsers that implement Trusted Web Activity, Chrome first. */
        val TWA_BROWSERS = listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.vivaldi.browser",
        )
    }

    override fun onDestroy() {
        connection?.let { runCatching { unbindService(it) } }
        connection = null
        super.onDestroy()
    }
}
