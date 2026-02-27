package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback;

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
        Toast.makeText(context , msg , Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFaceDetectDarwCallback(LivenessModel livenessModel) {

    }
}
