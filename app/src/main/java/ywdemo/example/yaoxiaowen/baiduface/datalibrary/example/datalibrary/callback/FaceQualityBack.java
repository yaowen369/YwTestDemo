package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.LivenessModel;

//import com.example.datalibrary.model.LivenessModel;

public class FaceQualityBack implements FaceDetectCallBack{
    Context context;
    public FaceQualityBack(Context context){
        this.context = context;
    }
    @Override
    public void onFaceDetectCallback(LivenessModel livenessModel) {

    }

    @Override
    public void onTip(int code, String msg) {
        // 安全地显示 Toast，检查 Activity 是否已销毁
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
        try {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // 忽略 Toast 显示异常，避免崩溃
        }
    }

    @Override
    public void onFaceDetectDarwCallback(LivenessModel livenessModel) {

    }
}
