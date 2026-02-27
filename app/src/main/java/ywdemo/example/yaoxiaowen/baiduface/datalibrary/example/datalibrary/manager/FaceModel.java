package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager;

import android.content.Context;

import com.baidu.idl.main.facesdk.FaceCrop;
import com.baidu.idl.main.facesdk.FaceDarkEnhance;
import com.baidu.idl.main.facesdk.FaceDetect;
import com.baidu.idl.main.facesdk.FaceFeature;
import com.baidu.idl.main.facesdk.FaceLive;
import com.baidu.idl.main.facesdk.FaceMouthMask;
import com.baidu.idl.main.facesdk.FaceSearch;
import com.baidu.idl.main.facesdk.model.BDFaceInstance;
import com.baidu.idl.main.facesdk.model.BDFaceSDKConfig;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.CropBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.DarkBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.DetectBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.DetectNirBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.DetectQualityBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.FeatureBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.LiveBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.MouthMaskBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.factory.specific.TrackBuilder;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
//import com.example.datalibrary.factory.specific.CropBuilder;
//import com.example.datalibrary.factory.specific.DarkBuilder;
//import com.example.datalibrary.factory.specific.DetectBuilder;
//import com.example.datalibrary.factory.specific.DetectNirBuilder;
//import com.example.datalibrary.factory.specific.DetectQualityBuilder;
//import com.example.datalibrary.factory.specific.FeatureBuilder;
//import com.example.datalibrary.factory.specific.LiveBuilder;
//import com.example.datalibrary.factory.specific.MouthMaskBuilder;
//import com.example.datalibrary.factory.specific.TrackBuilder;
//import com.example.datalibrary.listener.SdkInitListener;

public class FaceModel implements SdkInitListener {
    private TrackBuilder trackBuilder;
    private DetectBuilder detectBuilder;
    private DetectQualityBuilder detectQualityBuilder;
    private DetectNirBuilder detectNirBuilder;
    private DarkBuilder darkBuilder;
    private FeatureBuilder featureBuilder;
    private FeatureBuilder featurePersonBuilder;
    private LiveBuilder liveBuilder;
    private CropBuilder cropBuilder;
    private MouthMaskBuilder mouthMaskBuilder = null;
    private boolean isModelInit;
    private boolean checkMouthMask = false;

    public void setListener(SdkInitListener listener) {
        this.listener = listener;
    }

    private SdkInitListener listener;
    public FaceModel(boolean checkMouthMask){
        isModelInit = false;
        cropBuilder = new CropBuilder(this);
        trackBuilder = new TrackBuilder(this);
        detectBuilder = new DetectBuilder(this);
        detectQualityBuilder = new DetectQualityBuilder(this);
        detectNirBuilder = new DetectNirBuilder(this);
        darkBuilder = new DarkBuilder(this);
        liveBuilder = new LiveBuilder(this);
        featureBuilder = new FeatureBuilder(this);
        featurePersonBuilder = new FeatureBuilder(this);

        this.checkMouthMask = checkMouthMask;
        if (checkMouthMask) {
            mouthMaskBuilder = new MouthMaskBuilder(this);
        }
    }
    public void init(BDFaceSDKConfig config , Context context){
        if (isModelInit){
            return;
        }
        BDFaceInstance trackInstance = new BDFaceInstance();
        trackInstance.creatInstance();
        BDFaceInstance detectInstance = new BDFaceInstance();
        detectInstance.creatInstance();
        // 人证核验init入参
        BDFaceInstance detectQualityInstance = new BDFaceInstance();
        detectQualityInstance.creatInstance();
        BDFaceInstance cropInstance = new BDFaceInstance();
        cropInstance.creatInstance();

        trackBuilder.init(trackInstance , config);
        cropBuilder.init(cropInstance);
        detectBuilder.init(detectInstance , config);
        detectNirBuilder.init(detectInstance);
        // 认证核验检测模型预配置
        detectQualityBuilder.init(detectQualityInstance , config);
        darkBuilder.init(null);
        liveBuilder.init(null);
        // 认证核验提取特征模型预配置
        featurePersonBuilder.init(detectQualityInstance);
        // 通用特征提取模型预配置
        featureBuilder.init(null);

        if (checkMouthMask){
            mouthMaskBuilder.init(null);
        }

        if (checkMouthMask) {
            // 口罩模型初始化
            mouthMaskBuilder.initModel(context);
        }
        // 抠图模型初始化
        cropBuilder.initModel(context);
        // 跟踪模型初始化
        trackBuilder.initModel(context);
        // 检测模型初始化
        detectBuilder.initModel(context);
        // 认证核验检测模型初始化 （边导入边识别-需初始化此处）
        detectQualityBuilder.initModel(context);
        // 红外检测模型
        detectNirBuilder.initModel(context);
        // 暗光恢复模型
        darkBuilder.initModel(context);
        // 活体分数模型
        liveBuilder.initModel(context);
        // 认证特征提取模型 （边导入边识别-需初始化此处）
        featurePersonBuilder.initModel(context);
        // 通用特征提取模型
        featureBuilder.initModel(context);
    }

    public void setCheckMouthMask(boolean checkMouthMask){
       this.checkMouthMask = checkMouthMask;
    }

    @Override
    public void initStart() {

        listener.initStart();
    }

    @Override
    public void initLicenseSuccess() {

        listener.initLicenseSuccess();
    }

    @Override
    public void initLicenseFail(int errorCode, String msg) {
        listener.initLicenseFail(errorCode , msg);
    }

    @Override
    public void initModelSuccess() {
        listener.initModelSuccess();
        isModelInit = true;
    }

    @Override
    public void initModelFail(int errorCode, String msg) {
        isModelInit = false;
        listener.initModelFail(errorCode , msg);
    }
    public FaceFeature getFacePersonFeature(){
        return featurePersonBuilder.getExample();
    }
    public FaceSearch getFacePersonSearch(){
        return featurePersonBuilder.getFaceSearch();
    }
    public FaceFeature getFaceFeature(){
        return featureBuilder.getExample();
    }
    public FaceSearch getFaceSearch(){
        return featureBuilder.getFaceSearch();
    }
    public FaceDetect getFaceTrack(){
        return trackBuilder.getExample();
    }
    public FaceCrop getFaceCrop(){
        return cropBuilder.getExample();
    }
    public FaceDetect getFaceDetectPerson(){
        return detectQualityBuilder.getExample();
    }
    public FaceDetect getFaceDetect(){
        return detectBuilder.getExample();
    }
    public FaceDetect getFaceNirDetect(){
        return detectNirBuilder.getExample();
    }
    public FaceDarkEnhance getDark(){
        return darkBuilder.getExample();
    }
    public FaceLive getFaceLive(){
        return liveBuilder.getExample();
    }

    public FaceMouthMask getFaceMoutMask(){
        if (mouthMaskBuilder != null) {
            return mouthMaskBuilder.getExample();
        }
        else{
            return null;
        }
    }
}
