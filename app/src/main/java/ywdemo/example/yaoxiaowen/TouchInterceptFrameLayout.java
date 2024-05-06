package ywdemo.example.yaoxiaowen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class TouchInterceptFrameLayout extends FrameLayout {

    private boolean mEnable = false;
    private CopyOnWriteArrayList<TouchInterceptFrame> mFrameList;
    private boolean mTempBoolean;

    public TouchInterceptFrameLayout(Context context) {
        super(context);
        init();
    }

    public void setInterceptEnabled(boolean enable) {
        mEnable = enable;
    }

    private void init() {
        mEnable = false;
        mTempBoolean = false;
        mFrameList = new CopyOnWriteArrayList<>();

    }

    public void updateInterceptFrame(Map<String, List<TouchInterceptFrame>> frameMap) {
        mFrameList = new CopyOnWriteArrayList();
        for (List<TouchInterceptFrame> entry : frameMap.values()) {
            mFrameList.addAll(entry);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mEnable) {
            return super.onInterceptTouchEvent(ev);
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (ev.getPointerId(ev.getActionIndex()) == 0) {
                mTempBoolean = true;
                for (TouchInterceptFrame frame : mFrameList) {
                    if (frame == null) {
                        continue;
                    }
                    if (frame.x < ev.getX() && ev.getX() < (frame.x + frame.width)) {
                        if (frame.y < ev.getY() && ev.getY() < (frame.y + frame.height)) {
                            mTempBoolean = false;
                            break;
                        }
                    }
                }

                // mTempBoolean =false 代表不拦截，手势交给web-view
                // mTempBoolean =true  代表此时并没有web-view的 cover-view标签消费手势,即将拦截手势转交给native-view
                // 但如果此时手势也没有touch到native-view的区域内，则不拦截
                if (mTempBoolean) {
                    mTempBoolean = checkNativeViewCanConsume(ev);
                }
            }
        }
        if (!mTempBoolean) {  // mTempBoolean =false 代表不拦截，手势交给web-view
            return super.onInterceptTouchEvent(ev);
        } else {  // mTempBoolean =true  代表此时并没有web-view的 cover-view标签消费手势,即将拦截手势转交给native-view
            return true;
        }
    }

    /**
     * 如果手势没有落在cover-view区域内，则尝试开启二次检测手势是否落在native-view区域内
     *
     * @param ev 手势事件，此方法在action-down事件内调用，所以不会触发多次
     * @return false native-view不消费手势，true native-view(map-view) 消费手势。
     */
    private boolean checkNativeViewCanConsume(MotionEvent ev) {
        // 检测 事件是否落在 Native区域内,
    }
}
