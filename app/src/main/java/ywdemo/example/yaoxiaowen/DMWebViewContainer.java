package ywdemo.example.yaoxiaowen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DMWebViewContainer extends FrameLayout {
    WebView mWebView;
    FrameLayout mTouchInterceptFrameLayout;

    public DMWebViewContainer(@NonNull Context context) {
        super(context);
        init();
    }
    public void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.dm_webview_container, this);
        mTouchInterceptFrameLayout = findViewById(R.id.mTouchInterceptFrameLayout);
    }
    public void initWebView() {
        // 实际项目中，这个 WebView从 配置中获取。这里 也是视图层引擎 接口化的一部分
        mWebView =  new WebView(getContext());

        // 重点是这一句
        mTouchInterceptFrameLayout.addView(mWebView);

        // 注入 invoke, publish 方法
        mWebView.addJavascriptInterface(new DMWebViewJSInterface(), "DMWebViewJSInterface");
    }

    // 这个方法正常不会被调用， 只有添加 地图等Native组件时才会被调用
    FrameLayout mBottomLayerFrameLayout;
    public FrameLayout getBottomFrameLayout() {
        if (mBottomLayerFrameLayout == null) {
            mBottomLayerFrameLayout = new FrameLayout(getContext());
            // -1 就是 match_parent
            mBottomLayerFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

            // Notice: 这是层级是 0, 所以相当于加在了 该 FrameLayout的 最里面.
            this.addView(mBottomLayerFrameLayout, 0);
        }

        return mBottomLayerFrameLayout;
    }
}
