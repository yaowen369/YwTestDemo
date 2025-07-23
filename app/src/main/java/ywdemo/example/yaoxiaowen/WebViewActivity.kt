package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        // 从布局文件中获取WebView组件
        webView = findViewById(R.id.webView)

        // 配置WebView
        setupWebView()

        // 加载指定的URL
        loadUrl()
    }

    private fun setupWebView() {
        // 获取WebView的设置
        val settings = webView.settings

        // 启用JavaScript支持
        settings.javaScriptEnabled = true

        // 支持缩放
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        // 启用DOM存储
        settings.domStorageEnabled = true

        // 设置WebViewClient，使页面在WebView中打开而不是在浏览器中
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    view?.loadUrl(it)
                }
                return true
            }
        }
    }

    private fun loadUrl() {
        // 这里加载指定的URL
        // 实际使用时请替换为你要加载的URL
        webView.loadUrl("http://192.168.20.191:3000/#/mapRouteView?cityId=3202&cityName=%E6%97%A0%E9%94%A1%E5%B8%82&showBack=1&donotreplace=1&startName=%E7%99%BE%E4%B9%90%E5%B9%BF%E5%9C%BA&startLat=31.612797&startLng=120.248972&endName=%E6%A1%83%E8%8A%B1%E5%B1%8B%E5%B0%8F%E9%85%92%E9%A6%86(%E5%8D%97%E9%95%BF%E8%A1%97%E5%BA%97)&endLat=31.565035&endLng=120.320517")
//        webView.loadUrl("https://www.baidu.com/")
    }

    // 处理返回键，使WebView能够返回上一页
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}