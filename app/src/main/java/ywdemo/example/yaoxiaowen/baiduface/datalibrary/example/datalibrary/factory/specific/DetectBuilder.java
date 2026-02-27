package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific;

import android.content.Context;
import android.util.Log;

import com.baidu.idl.main.facesdk.FaceDetect;
import com.baidu.idl.main.facesdk.callback.Callback;
import com.baidu.idl.main.facesdk.model.BDFaceInstance;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.model.BDFaceSDKConfig;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.builder.ModelConfigBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.GlobalSet;
//import com.example.datalibrary.factory.builder.ModelConfigBuilder;
//import com.example.datalibrary.listener.SdkInitListener;
//import com.example.datalibrary.model.GlobalSet;

public class DetectBuilder extends ModelConfigBuilder<FaceDetect> {

    private FaceDetect faceDetect;
    private SdkInitListener listener;
    public DetectBuilder(SdkInitListener listener){
        this.listener = listener;
    }
    @Override
    public void init(BDFaceInstance bdFaceInstance) {
        if (bdFaceInstance == null){
            faceDetect = new FaceDetect();
        }else {
            faceDetect = new FaceDetect(bdFaceInstance);
        }
    }

    @Override
    public void init(BDFaceInstance bdFaceInstance, BDFaceSDKConfig config) {

        if (bdFaceInstance == null){
            faceDetect = new FaceDetect();
        }else {
            faceDetect = new FaceDetect(bdFaceInstance);
        }
        if (config != null){
            faceDetect.loadConfig(config);
        }else {
            faceDetect.loadConfig(new BDFaceSDKConfig());
        }
    }

    @Override
    public void init() {
        faceDetect = new FaceDetect();
    }

    @Override
    public void initModel(Context context) {
        initFastModel(context);
        initAccurateModel(context);
        // 质量检测模型
        initQualityModel(context);
        // 属性检测模型
        initAttrbuteModel(context);
        // 最优人脸检测模型
        initBestImageModel(context);
    }

    @Override
    public FaceDetect getExample() {
        return faceDetect;
    }

    public void initFastModel(Context context) {
        faceDetect.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.ALIGN_TRACK_MODEL,
                BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST,
                new Callback() {
                    @Override
                    public void onResponse(int code, String response) {
                        Log.e("face_model" , response);
                        if (code != 0 && listener != null) {
                            listener.initModelFail(code, response);
                        }
                    }
                });
    }

    public void initAccurateModel(Context context) {
        faceDetect.initModel(context,
                GlobalSet.DETECT_VIS_MODEL,
                GlobalSet.ALIGN_RGB_MODEL, BDFaceSDKCommon.DetectType.DETECT_VIS,
                BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                new Callback() {
                    @Override
                    public void onResponse(int code, String response) {
                        Log.e("face_model" , response);
                        //  ToastUtils.toast(context, code + "  " + response);
                        if (code != 0 && listener != null) {
                            listener.initModelFail(code, response);
                        }
                    }
                });

    }
    public void initQualityModel(Context context) {
        faceDetect.initQuality(context,
                GlobalSet.BLUR_MODEL,
                GlobalSet.OCCLUSION_MODEL, new Callback() {
                    @Override
                    public void onResponse(int code, String response) {
                        Log.e("face_model" , response);
                        if (code != 0 && listener != null) {
                            listener.initModelFail(code, response);
                        }
                    }
                });
    }
    public void initAttrbuteModel(Context context) {
        faceDetect.initAttrbute(context, GlobalSet.ATTRIBUTE_MODEL, new Callback() {
            @Override
            public void onResponse(int code, String response) {
                Log.e("face_model" , response);
                if (code != 0 && listener != null) {
                    listener.initModelFail(code, response);
                }
            }
        });
    }
    public void initBestImageModel(Context context) {

        faceDetect.initBestImage(context, GlobalSet.BEST_IMAGE, new Callback() {
            @Override
            public void onResponse(int code, String response) {
                Log.e("face_model" , response);
                if (code != 0 && listener != null) {
                    listener.initModelFail(code, response);
                }
            }
        });
    }
    public FaceDetect getFaceDetect() {
        return faceDetect;
    }
}
