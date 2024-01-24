package git.jkl4o4.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import java.lang.IllegalStateException

@SuppressLint("SetJavaScriptEnabled")
class OptimizeWebView(
    context: Context,
    attrs: AttributeSet?
) : WebView(context, attrs) {

    private var keys: List<String>? = null

    fun setKeys(list: List<String>) {
        this.keys = list
    }

    private var lastBackPressTime: Long = 0
    val onBackPress = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            this@OptimizeWebView.let {
                val currentIndex = it.copyBackForwardList().currentIndex
                if (it.canGoBack()) {
                    if ((System.currentTimeMillis() - lastBackPressTime) < 2000) {
                        it.goBackOrForward(-currentIndex)
                    } else it.goBack()
                    lastBackPressTime = System.currentTimeMillis()
                }
            }
        }
    }

    init {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        isSaveEnabled = true
        isFocusable = true
        isFocusableInTouchMode = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setLayerType(LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        settings.apply {
            mixedContentMode = 0
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            databaseEnabled = true
            useWideViewPort = true
            allowFileAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = userAgentString.replace("; zy".decryptCaesar(3), "")
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) saveFormData = true
        }
    }

    private var permissionRequest: PermissionRequest? = null
    fun setupChromeClient(filePathCallback: (filePathCallback: ValueCallback<Array<Uri>>?, request: PermissionRequest?) -> Unit) {
        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback(filePathCallback, null)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.resources?.forEach { resource ->
                    if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        permissionRequest = request
                        val permission = android.Manifest.permission.CAMERA
                        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                            permissionRequest?.grant(permissionRequest?.resources)
                            return
                        }
                        filePathCallback(null, permissionRequest)
                    }
                }
            }
        }
    }

    fun setupClient(callback: () -> Unit) {
        if (keys.isNullOrEmpty()) throw IllegalStateException("Please set keys for this view.")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val title = view?.title
                if (title != null) {
                    run cloak@{
                        keys?.forEach { key ->
                            if (title.contains(key)) {
                                callback()
                                return@cloak
                            }
                        }
                    }
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return try {
                    val url = request?.url?.toString()
                    val intent = createIntent(url.toString())
                    if (intent != null && view?.context != null) {
                        view.context.startActivity(intent)
                        true
                    } else false
                } catch (e: Exception) {
                    true
                }
            }
        }
    }

    private fun createIntent(url: String): Intent? {
        val urlActionMappings = listOf(
            Pair("who:".decryptCaesar(3), Intent.ACTION_DIAL),
            Pair("pdlowr:".decryptCaesar(3), Intent.ACTION_SENDTO),
            Pair("kwwsv://w.ph/mrlqfkdw".decryptCaesar(3), Intent.ACTION_VIEW)
        )

        for ((urlStart, action) in urlActionMappings) {
            if (url.startsWith(urlStart)) {
                return Intent(action, Uri.parse(url))
            }
        }

        if (url.startsWith("kwws://".decryptCaesar(3)) || url.startsWith("kwwsv://".decryptCaesar(3))) return null

        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }
}