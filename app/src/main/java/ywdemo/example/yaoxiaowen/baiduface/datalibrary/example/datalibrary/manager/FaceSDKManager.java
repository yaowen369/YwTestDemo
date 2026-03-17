package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager;

//import static com.example.datalibrary.model.GlobalSet.FEATURE_SIZE;

import static ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.GlobalSet.FEATURE_SIZE;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.idl.main.facesdk.FaceAuth;
import com.baidu.idl.main.facesdk.FaceCrop;
import com.baidu.idl.main.facesdk.FaceDetect;
import com.baidu.idl.main.facesdk.FaceFeature;
import com.baidu.idl.main.facesdk.FaceInfo;
import com.baidu.idl.main.facesdk.FaceMouthMask;
import com.baidu.idl.main.facesdk.ImageIllum;
import com.baidu.idl.main.facesdk.model.BDFaceDetectListConf;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;
import com.baidu.idl.main.facesdk.model.BDFaceOcclusion;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.model.BDFaceSDKConfig;
import com.baidu.idl.main.facesdk.model.Feature;
//import com.example.datalibrary.api.FaceApi;
//import com.example.datalibrary.callback.FaceDetectCallBack;
//import com.example.datalibrary.callback.FaceQualityBack;
//import com.example.datalibrary.db.DBManager;
//import com.example.datalibrary.listener.DetectListener;
//import com.example.datalibrary.listener.QualityListener;
//import com.example.datalibrary.listener.SdkInitListener;
//import com.example.datalibrary.model.BDFaceCheckConfig;
//import com.example.datalibrary.model.BDFaceImageConfig;
//import com.example.datalibrary.model.BDLiveConfig;
//import com.example.datalibrary.model.BDQualityConfig;
//import com.example.datalibrary.model.LivenessModel;
//import com.example.datalibrary.model.User;
//import com.example.datalibrary.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.api.FaceApi;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback.FaceDetectCallBack;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback.FaceQualityBack;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.db.DBManager;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.DetectListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.QualityListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDFaceCheckConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDFaceImageConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDLiveConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDQualityConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.LivenessModel;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.ToastUtils;

public class FaceSDKManager {

    public static final int SDK_MODEL_LOAD_SUCCESS = 0;
    public static final int SDK_UNACTIVATION = 1;
    private static final String TAG = "FaceSDKManager";

    private List<Boolean> mRgbLiveList = new ArrayList<>();
    private List<Boolean> mNirLiveList = new ArrayList<>();
    private int mLastFaceId;

    private float threholdScore;

    public static volatile int initStatus = SDK_UNACTIVATION;
    public static volatile boolean initModelSuccess = false;
    private FaceAuth faceAuth;

    private ExecutorService es = Executors.newSingleThreadExecutor();
    private Future future;
    private ExecutorService es2 = Executors.newSingleThreadExecutor();
    private Future future2;
    private ExecutorService es3 = Executors.newSingleThreadExecutor();
    private Future future3;

    private float[] scores;
    private ImageIllum imageIllum;
    private long startInitModelTime;

    private static int failNumber = 0;
    private static int faceId = 0;
    private static int lastFaceId = 0;
    private static LivenessModel faceAdoptModel;
    private boolean isFail = false;
    private long trackTime;
    private FaceModel faceModel;

    private boolean checkMouthMask = false;
    private boolean isMultiIdentify = false;

    // 存储最近一次质量检测的失败信息
    private String lastQualityDetectFail = "";
    private String lastQualityOcclusionFail = "";

    private FaceSDKManager() {
        faceAuth = new FaceAuth();
        faceAuth.setCoreConfigure(BDFaceSDKCommon.BDFaceCoreRunMode.BDFACE_LITE_POWER_NO_BIND, 2);
    }

    public void setActiveLog(boolean isLog) {
        if (faceAuth != null) {
            if (isLog) {
                faceAuth.setActiveLog(BDFaceSDKCommon.BDFaceLogInfo.BDFACE_LOG_TYPE_ALL, 1);
            } else {
                faceAuth.setActiveLog(BDFaceSDKCommon.BDFaceLogInfo.BDFACE_LOG_TYPE_ALL, 0);
            }
        }
    }

    public void setCheckMouthMask(boolean checkMouthMask) {
        this.checkMouthMask = checkMouthMask;
    }

    public void setMultiIdentify(boolean isMultiFaceIdentify) {
        this.isMultiIdentify = isMultiFaceIdentify;
    }

    /**
     * 获取最近一次质量检测的遮挡失败信息
     */
    public String getLastQualityOcclusionFail() {
        return lastQualityOcclusionFail;
    }

    /**
     * 获取最近一次质量检测的其他失败信息（模糊、光照、姿态等）
     */
    public String getLastQualityDetectFail() {
        return lastQualityDetectFail;
    }

    private static class HolderClass {
        private static final FaceSDKManager INSTANCE = new FaceSDKManager();
    }

    public static FaceSDKManager getInstance() {
        return HolderClass.INSTANCE;
    }

    public ImageIllum getImageIllum() {
        return imageIllum;
    }

    public void initModel(
        final Context context, BDFaceSDKConfig config, boolean isLog, final SdkInitListener listener) {
        setActiveLog(isLog);
        initModel(context, config, listener);
    }

    /**
     * 初始化模型，目前包含检查，活体，识别模型；因为初始化是顺序执行，可以在最好初始化回掉中返回状态结果
     *
     * @param context
     */
    public void initModel(final Context context, BDFaceSDKConfig config, final SdkInitListener listener) {
        // 曝光
        if (imageIllum == null) {
            imageIllum = new ImageIllum();
        }
        // 其他模型初始化
        if (faceModel == null) {
            faceModel = new FaceModel(checkMouthMask);
        }
        faceModel.setListener(listener);

        faceModel.init(config, context);

        startInitModelTime = System.currentTimeMillis();
    }

    public FaceCrop getFaceCrop() {
        return faceModel.getFaceCrop();
    }

    public FaceDetect getFaceDetectPerson() {
        return faceModel.getFaceDetectPerson();
    }

    public FaceFeature getFacePersonFeature() {
        return faceModel.getFacePersonFeature();
    }

    public FaceMouthMask getFaceMouthMask() {
        return faceModel.getFaceMoutMask();
    }

    public void initDataBases(Context context) {
        if (FaceApi.getInstance().getmUserNum() != 0) {
            ToastUtils.toast(context, "人脸库加载中");
        }
        emptyFrame();
        // 初始化数据库
        DBManager.getInstance().init(context);
        // 数据变化，更新内存
        initPush(context);
    }

    /**
     * 数据库发现变化时候，重新把数据库中的人脸信息添加到内存中，id+feature
     */
    public void initPush(final Context context) {

        if (future3 != null && !future3.isDone()) {
            future3.cancel(true);
        }

        future3 =
            es3.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        faceModel.getFaceSearch().featureClear();
                        synchronized (faceModel.getFaceSearch()) {
                            List<User> users = FaceApi.getInstance().getAllUserList();
                            for (int i = 0; i < users.size(); i++) {
                                User user = users.get(i);
                                faceModel.getFaceSearch().pushPersonById(user.getId(), user.getFeature());
                            }
                            if (FaceApi.getInstance().getmUserNum() != 0) {
                                ToastUtils.toast(context, "人脸库加载成功");
                            }
                        }
                    }
                });
    }

    private void setFail(LivenessModel livenessModel) {
        Log.e("faceId", livenessModel.getFaceInfo().faceID + "");
        if (failNumber >= 2) {
            faceId = livenessModel.getFaceInfo().faceID;
            faceAdoptModel = livenessModel;
            trackTime = System.currentTimeMillis();
            isFail = false;
            faceAdoptModel.setMultiFrame(true);
        } else {
            failNumber += 1;
            faceId = 0;
            faceAdoptModel = null;
            isFail = true;
            livenessModel.setMultiFrame(true);
        }
    }

    public void emptyFrame() {
        failNumber = 0;
        faceId = 0;
        isFail = false;
        trackTime = 0;
        faceAdoptModel = null;
    }

    private FaceInfo[] getTrackCheck(BDFaceImageInstance rgbInstance) {

        // 快速检测获取人脸信息，仅用于绘制人脸框，详细人脸数据后续获取
        FaceInfo[] faceInfos =
            faceModel
                .getFaceTrack()
                .track(
                    BDFaceSDKCommon.DetectType.DETECT_VIS,
                    BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_FAST,
                    rgbInstance);
        return faceInfos;
    }


    private FaceInfo[] getDetectCheck(BDFaceImageInstance rgbInstance) {

        // 快速检测获取人脸信息，仅用于绘制人脸框，详细人脸数据后续获取
        FaceInfo[] faceInfos = faceModel.getFaceDetect().detect(BDFaceSDKCommon.DetectType.DETECT_VIS, rgbInstance);
        return faceInfos;
    }

    private BDFaceImageInstance getBdImage(BDFaceImageConfig bdFaceImageConfig, boolean darkEnhance) {
        BDFaceImageInstance rgbInstance =
            new BDFaceImageInstance(
                bdFaceImageConfig.data,
                bdFaceImageConfig.srcHeight,
                bdFaceImageConfig.srcWidth,
                bdFaceImageConfig.bdFaceImageType,
                bdFaceImageConfig.direction,
                bdFaceImageConfig.mirror);
        BDFaceImageInstance rgbInstanceOne;
        // 判断暗光恢复
        if (darkEnhance) {
            rgbInstanceOne = faceModel.getDark().faceDarkEnhance(rgbInstance);
            rgbInstance.destory();
        } else {
            rgbInstanceOne = rgbInstance;
        }
        return rgbInstanceOne;
    }

    private boolean frameSelect(FaceInfo faceInfo) {
        if (lastFaceId != faceInfo.faceID) {
            lastFaceId = faceInfo.faceID;
        }

        if (System.currentTimeMillis() - trackTime < 0 && faceId == faceInfo.faceID) {
            faceAdoptModel.setMultiFrame(true);

            return false;
        }
        if (faceAdoptModel != null) {
            faceAdoptModel.setMultiFrame(false);
        }
        faceId = 0;
        faceAdoptModel = null;
        if (!isFail /*&& failNumber != 0*/) {
            failNumber = 0;
        }
        return true;
    }

    public BDFaceImageInstance getCopeFace(Bitmap bitmap, float[] landmarks, int initialValue) {
        if (faceModel == null || faceModel.getFaceCrop() == null) {
            return null;
        }
        BDFaceImageInstance imageInstance = new BDFaceImageInstance(bitmap);
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        AtomicInteger isOutoBoundary = new AtomicInteger();
        BDFaceImageInstance cropInstance =
            faceModel.getFaceCrop().cropFaceByLandmark(imageInstance, landmarks, 2.0f, false, isOutoBoundary);
        return cropInstance;
    }

    /**
     * 检测-活体-特征-人脸检索流程
     *
     * @param bdFaceImageConfig      可见光YUV 数据流
     * @param bdNirFaceImageConfig   红外YUV 数据流
     * @param bdDepthFaceImageConfig 深度depth 数据流
     * @param bdFaceCheckConfig      识别参数
     * @param faceDetectCallBack
     */
    public void onDetectCheck(
        final BDFaceImageConfig bdFaceImageConfig,
        final BDFaceImageConfig bdNirFaceImageConfig,
        final BDFaceImageConfig bdDepthFaceImageConfig,
        final BDFaceCheckConfig bdFaceCheckConfig,
        final FaceDetectCallBack faceDetectCallBack) {
        if (!FaceSDKManager.initModelSuccess) {
            return;
        }
        long startTime = System.currentTimeMillis();
        // 创建检测结果存储数据
        LivenessModel livenessModel = new LivenessModel();
        // 创建检测对象，如果原始数据YUV，转为算法检测的图片BGR
        // TODO: 用户调整旋转角度和是否镜像，手机和开发版需要动态适配
        BDFaceImageInstance rgbInstance = getBdImage(bdFaceImageConfig, bdFaceCheckConfig.darkEnhance);
        livenessModel.setTestBDFaceImageInstanceDuration(System.currentTimeMillis() - startTime);
        onTrack(
            rgbInstance,
            livenessModel,
            new DetectListener() {
                @Override
                public void onDetectSuccess(FaceInfo[] faceInfos, BDFaceImageInstance rgbInstance) {
                    // 多帧判断
                    if (!frameSelect(faceInfos[0])) {
                        livenessModel.setBdFaceImageInstance(rgbInstance.getImage());
                        if (faceDetectCallBack != null && faceAdoptModel != null) {
                            faceDetectCallBack.onFaceDetectDarwCallback(livenessModel);
                            faceDetectCallBack.onFaceDetectCallback(faceAdoptModel);
                        }
                        rgbInstance.destory();

                        return;
                    }
                    // 保存人脸特征点
                    livenessModel.setLandmarks(faceInfos[0].landmarks);
                    // 保存人脸图片
                    livenessModel.setBdFaceImageInstance(rgbInstance.getImage());
                    // 口罩检测数据
                    if (checkMouthMask) {
                        FaceMouthMask mouthMask = getFaceMouthMask();
                        if (mouthMask != null) {
                            float[] maskScores = mouthMask.checkMask(rgbInstance, faceInfos);
                            if (maskScores != null && maskScores.length > 0) {
                                float maskResult = maskScores[0];
                                Log.d("mouth_mask", "mask_score:" + maskResult);
                                if (livenessModel != null) {
                                    livenessModel.setMouthMaskArray(maskScores);
                                }
                            }
                        }
                    }

                    // 调用绘制人脸框接口
                    if (faceDetectCallBack != null) {
                        faceDetectCallBack.onFaceDetectDarwCallback(livenessModel);
                    }

                    // 送检识别
                    onLivenessCheck(
                        rgbInstance,
                        bdNirFaceImageConfig,
                        bdDepthFaceImageConfig,
                        bdFaceCheckConfig,
                        livenessModel,
                        startTime,
                        faceDetectCallBack,
                        faceInfos);
                }

                @Override
                public void onDetectFail() {

                    emptyFrame();
                    // 流程结束销毁图片，开始下一帧图片检测，否着内存泄露
                    if (faceDetectCallBack != null) {
                        faceDetectCallBack.onFaceDetectCallback(null);
                        livenessModel.setBdFaceImageInstance(rgbInstance.getImage());

//                        SaveImageManager.getInstance().saveImage(livenessModel, bdFaceCheckConfig.bdLiveConfig);
                        faceDetectCallBack.onFaceDetectDarwCallback(livenessModel);
                        faceDetectCallBack.onTip(0, "未检测到人脸");
                    }
                    rgbInstance.destory();
                }
            });
    }

    private float silentLive(
        BDFaceImageInstance rgbInstance,
        BDFaceSDKCommon.LiveType type,
        float[] landmarks,
        List<Boolean> list,
        float liveScore) {
        float score = 0;
        if (landmarks != null) {
            synchronized (faceModel.getFaceLive()) {
                Log.e("test_camera", rgbInstance.getImage().data.length + "开始");
                score = faceModel.getFaceLive().silentLive(type, rgbInstance, landmarks, liveScore);
                Log.e("test_camera", "活体结束");
            }
            list.add(score > liveScore);
        }
        while (list.size() > 6) {
            list.remove(0);
        }
        if (list.size() > 2) {
            int rgbSum = 0;
            for (Boolean b : list) {
                if (b) {
                    rgbSum++;
                }
            }
            if (1.0 * rgbSum / list.size() > 0.6) {
                if (score < liveScore) {
                    score = liveScore + (1 - liveScore) * new Random().nextFloat();
                }
            } else {
                if (score > liveScore) {
                    score = new Random().nextFloat() * liveScore;
                }
            }
        }
        return score;
    }

    /**
     *  活体检测
     * @param rgbInstance
     * @param type
     * @param faceInfos
     * @param list
     * @param liveScore
     * @return
     */
    private float[] silentLives(
        BDFaceImageInstance rgbInstance,
        BDFaceSDKCommon.LiveType type,
        FaceInfo[] faceInfos,
        List<Boolean> list,
        float liveScore) {
        float[] scores = {0, 0, 0}; // 最多检测3个人脸
        if (faceInfos != null) {
            synchronized (faceModel.getFaceLive()) {
                Log.e("test_camera", rgbInstance.getImage().data.length + "开始");
                int size = faceInfos.length;

                for (int i = 0; i < size; i++) {
                    scores[i] =
                        faceModel.getFaceLive().silentLive(type, rgbInstance, faceInfos[i].landmarks, liveScore);
                }
            }
        }
        return scores;
    }

    private void onDetect(
        BDFaceCheckConfig bdFaceCheckConfig,
        BDFaceImageInstance rgbInstance,
        FaceInfo[] fastFaceInfos,
        LivenessModel livenessModel,
        DetectListener detectListener) {

        long accurateTime = System.currentTimeMillis();
        FaceInfo[] faceInfos;

        if (bdFaceCheckConfig != null) {
            bdFaceCheckConfig.bdFaceDetectListConfig.usingQuality = true;
            faceInfos =
                faceModel
                    .getFaceDetect()
                    .detect(
                        BDFaceSDKCommon.DetectType.DETECT_VIS,
                        BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                        rgbInstance,
                        fastFaceInfos,
                        bdFaceCheckConfig.bdFaceDetectListConfig);

        } else {
            faceInfos = faceModel.getFaceDetect().detect(BDFaceSDKCommon.DetectType.DETECT_VIS, rgbInstance);
        }

        livenessModel.setAccurateTime(System.currentTimeMillis() - accurateTime);

        if (faceInfos == null || faceInfos.length <= 0) {
            rgbInstance.destory();
            detectListener.onDetectFail();
            return;
        }

        // 重新赋予详细人脸信息
        if (livenessModel.getFaceInfo() != null) {
            faceInfos[0].faceID = livenessModel.getFaceInfo().faceID;
        }

        livenessModel.setFaceInfos(faceInfos);
        livenessModel.setFaceInfo(faceInfos[0]);
        livenessModel.setTrackStatus(2);
        // 保存人脸关键点
        livenessModel.setLandmarks(faceInfos[0].landmarks);
        detectListener.onDetectSuccess(faceInfos, rgbInstance);
    }

    /**
     * 活体-特征-人脸检索全流程
     *
     * @param rgbInstance            可见光底层送检对象
     * @param nirBDFaceImageConfig   红外YUV 数据流
     * @param depthBDFaceImageConfig 深度depth 数据流
     * @param livenessModel          检测结果数据集合
     * @param startTime              开始检测时间
     * @param faceDetectCallBack
     */
    public void onLivenessCheck(
        final BDFaceImageInstance rgbInstance,
        final BDFaceImageConfig nirBDFaceImageConfig,
        final BDFaceImageConfig depthBDFaceImageConfig,
        final BDFaceCheckConfig bdFaceCheckConfig,
        final LivenessModel livenessModel,
        final long startTime,
        final FaceDetectCallBack faceDetectCallBack,
        final FaceInfo[] fastFaceInfos) {

        if (future2 != null && !future2.isDone()) {
            // 流程结束销毁图片，开始下一帧图片检测，否着内存泄露
            rgbInstance.destory();
            return;
        }

        future2 =
            es2.submit(
                new Runnable() {

                    @Override
                    public void run() {

                        // 获取BDFaceCheckConfig配置信息
                        if (bdFaceCheckConfig == null) {
                            rgbInstance.destory();

                            return;
                        }

                        onDetect(
                            bdFaceCheckConfig,
                            rgbInstance,
                            fastFaceInfos,
                            livenessModel,
                            new DetectListener() {

                                @Override
                                public void onDetectSuccess(FaceInfo[] faceInfos, BDFaceImageInstance rgbInstance) {

                                    // 人脸id赋值
                                    if (mLastFaceId != fastFaceInfos[0].faceID) {
                                        mLastFaceId = fastFaceInfos[0].faceID;
                                        mRgbLiveList.clear();
                                        mNirLiveList.clear();
                                    }

                                    if (bdFaceCheckConfig == null) {
                                        rgbInstance.destory();
                                        livenessModel.clearIdentifyResults();
                                        if (faceDetectCallBack != null) {
                                            faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                        }
                                        return;
                                    }

                                    // 最优人脸控制
                                    if (!onBestImageCheck(livenessModel, bdFaceCheckConfig, faceDetectCallBack)) {
                                        livenessModel.setQualityCheck(true);
                                        livenessModel.clearIdentifyResults();
                                        rgbInstance.destory();
                                        if (faceDetectCallBack != null) {
                                            faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                        }
                                        return;
                                    }

                                    // 质量检测未通过,销毁BDFaceImageInstance，结束函数

                                    if (!onQualityCheck(
                                            faceInfos, bdFaceCheckConfig.bdQualityConfig, faceDetectCallBack)) {
                                        // 将遮挡检测信息设置到 livenessModel
                                        livenessModel.setQualityOcclusion(lastQualityOcclusionFail);
                                        livenessModel.setQualityDetect(lastQualityDetectFail);
                                        livenessModel.setQualityCheck(true);
                                        livenessModel.clearIdentifyResults();
                                        rgbInstance.destory();
                                        if (faceDetectCallBack != null) {
                                            faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                        }

                                        return;
                                    }

                                    livenessModel.setQualityCheck(false);
                                    // 获取LivenessConfig liveCheckMode 配置选项：【不使用活体：0】；【RGB活体：1】；【RGB+NIR活体：2】；【RGB+Depth活体：3】；【RGB+NIR+Depth活体：4】
                                    // TODO 活体检测

                                    float[] rgbScores = {-1};
                                    BDLiveConfig bdLiveConfig = bdFaceCheckConfig.bdLiveConfig;
                                    boolean isLiveCheck = bdFaceCheckConfig.bdLiveConfig != null;

                                    if (isLiveCheck) {
                                        long startRgbTime = System.currentTimeMillis();
                                        rgbScores =
                                            silentLives(
                                                rgbInstance,
                                                BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
                                                faceInfos,
                                                mRgbLiveList,
                                                bdLiveConfig.rgbLiveScore);
                                        /* if (rgbScores != null){
                                            int size = rgbScores.length;
                                            Log.d("Attend", "score size:" + size);
                                            for (int i = 0; i < size; i++) {
                                                Log.d("Attend", "score:" + rgbScores[i]);
                                            }
                                        } */

                                        if (faceInfos.length == 1) {
                                            livenessModel.setRgbLivenessScore(rgbScores[0]);
                                        }
                                        livenessModel.setRgbLivenessScores(rgbScores);
                                        livenessModel.setRgbLivenessDuration(System.currentTimeMillis() - startRgbTime);
                                    }

                                    // TODO nir活体检测
                                    float nirScore = -1;
                                    FaceInfo[] faceInfosIr = null;
                                    BDFaceImageInstance nirInstance = null;
                                    boolean isHaveNirImage = nirBDFaceImageConfig != null && isLiveCheck;
                                    if (isHaveNirImage) {
                                        // 创建检测对象，如果原始数据YUV-IR，转为算法检测的图片BGR
                                        // TODO: 用户调整旋转角度和是否镜像，手机和开发版需要动态适配
                                        long nirInstanceTime = System.currentTimeMillis();
                                        nirInstance = getBdImage(nirBDFaceImageConfig, false);

                                        livenessModel.setBdNirFaceImageInstance(nirInstance.getImage());
                                        livenessModel.setNirInstanceTime(System.currentTimeMillis() - nirInstanceTime);

                                        // 避免RGB检测关键点在IR对齐活体稳定，增加红外检测
                                        long startIrDetectTime = System.currentTimeMillis();
                                        BDFaceDetectListConf bdFaceDetectListConf = new BDFaceDetectListConf();
                                        bdFaceDetectListConf.usingDetect = true;
                                        faceInfosIr =
                                            faceModel
                                                .getFaceNirDetect()
                                                .detect(
                                                    BDFaceSDKCommon.DetectType.DETECT_NIR,
                                                    BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_NIR_ACCURATE,
                                                    nirInstance,
                                                    null,
                                                    bdFaceDetectListConf);
                                        livenessModel.setIrLivenessDuration(
                                            System.currentTimeMillis() - startIrDetectTime);
                                        //                    LogUtils.e(TIME_TAG, "detect ir time = " + livenessModel.getIrLivenessDuration());
                                        if (faceInfosIr != null && faceInfosIr.length > 0) {
                                            FaceInfo faceInfoIr = faceInfosIr[0];
                                            nirScore =
                                                silentLive(
                                                    nirInstance,
                                                    BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_NIR,
                                                    faceInfoIr.landmarks,
                                                    mNirLiveList,
                                                    bdLiveConfig.nirLiveScore);
                                            livenessModel.setIrLivenessScore(nirScore);
                                            //                        LogUtils.e(TIME_TAG, "live ir time = " + livenessModel.getIrLivenessDuration());
                                        }
                                    }

                                    // TODO depth活体检测
                                    float depthScore = -1;
                                    boolean isHaveDepthImage = depthBDFaceImageConfig != null && isLiveCheck;
                                    if (depthBDFaceImageConfig != null) {
                                        // TODO: 用户调整旋转角度和是否镜像，适配Atlas 镜头，目前宽和高400*640，其他摄像头需要动态调整,人脸72 个关键点x 坐标向左移动80个像素点
                                        float[] depthLandmark = new float[faceInfos[0].landmarks.length];
                                        BDFaceImageInstance depthInstance;
                                        if (bdFaceCheckConfig.cameraType == 1) {
                                            System.arraycopy(
                                                faceInfos[0].landmarks,
                                                0,
                                                depthLandmark,
                                                0,
                                                faceInfos[0].landmarks.length);
                                            for (int i = 0; i < 144; i = i + 2) {
                                                depthLandmark[i] -= 80;
                                            }
                                        } else {
                                            depthLandmark = faceInfos[0].landmarks;
                                        }
                                        depthInstance = getBdImage(depthBDFaceImageConfig, false);
                                        livenessModel.setBdDepthFaceImageInstance(depthInstance.getImage());
                                        // 创建检测对象，如果原始数据Depth
                                        long startDepthTime = System.currentTimeMillis();
                                        depthScore =
                                            faceModel
                                                .getFaceLive()
                                                .silentLive(
                                                    BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_DEPTH,
                                                    depthInstance,
                                                    depthLandmark);
                                        livenessModel.setDepthLivenessScore(depthScore);
                                        livenessModel.setDepthtLivenessDuration(
                                            System.currentTimeMillis() - startDepthTime);
                                        //                    LogUtils.e(TIME_TAG, "live depth time = " + livenessModel.getDepthtLivenessDuration());
                                        depthInstance.destory();
                                    }

                                    boolean isRgbScoreCheck = false;
                                    boolean isNirScoreCheck = false;
                                    boolean isDepthScoreCheck = false;
                                    if (isLiveCheck) {
                                        int size = rgbScores.length;
                                        for (int i = 0; i < size; i++) {
                                            if (rgbScores[i] > bdLiveConfig.rgbLiveScore) {
                                                isRgbScoreCheck = true;
                                            }
                                        }
                                        // isRgbScoreCheck = true; // liujinhui for test
                                        //   isRgbScoreCheck = (rgbScores[0] > bdLiveConfig.rgbLiveScore);
                                        isNirScoreCheck =
                                            (isHaveNirImage ? nirScore > bdLiveConfig.nirLiveScore : true);
                                        isDepthScoreCheck =
                                            (isHaveDepthImage ? depthScore > bdLiveConfig.depthLiveScore : true);
                                    }

                                    // 如果设置为不进行活体检测
                                    int liveCheckMode = livenessModel.getLiveType();
                                    if (liveCheckMode == 0){
                                        isRgbScoreCheck = true;
                                    }
                                    // TODO 特征提取+人脸检索
                                    if (!isLiveCheck || (isRgbScoreCheck && isNirScoreCheck && isDepthScoreCheck)) {
                                        if (livenessModel != null) {
                                            livenessModel.clearIdentifyResults();
                                            livenessModel.setUser(null);
                                        }

                                        synchronized (faceModel.getFaceSearch()) {
                                            if (faceInfos != null) {
                                                int size = faceInfos.length;

                                                for (int i = 0; i < size; i++) {
                                                    // 特征提取
                                                    // 模糊结果过滤,戴口罩时候，不进行过滤
                                                    if (!checkMouthMask) {
                                                        float blur = faceInfos[i].bluriness;
                                                        BDFaceOcclusion occlusion = faceInfos[i].occlusion;
                                                        float leftEye = occlusion.leftEye;
                                                        // "左眼遮挡"
                                                        float rightEye = occlusion.rightEye;
                                                        // "右眼遮挡"
                                                        float nose = occlusion.nose;
                                                        // "鼻子遮挡置信度"
                                                        float mouth = occlusion.mouth;
                                                        // "嘴巴遮挡置信度"
                                                        float leftCheek = occlusion.leftCheek;
                                                        // "左脸遮挡"
                                                        float rightCheek = occlusion.rightCheek;
                                                        // "右脸遮挡"
                                                        float chin = occlusion.chin;
                                                        // 动态底库限制
                                                        faceModel
                                                                .getFaceSearch()
                                                                .setNeedJoinDB(
                                                                        selectQuality(
                                                                                blur,
                                                                                leftEye,
                                                                                rightEye,
                                                                                nose,
                                                                                mouth,
                                                                                leftCheek,
                                                                                rightCheek,
                                                                                chin));
                                                    }

                                                    if (bdLiveConfig != null){
                                                        onFeatureChecks(
                                                                i,
                                                                rgbInstance,
                                                                bdFaceCheckConfig,
                                                                faceInfos,
                                                                faceInfosIr,
                                                                nirInstance,
                                                                livenessModel,
                                                                bdFaceCheckConfig.secondFeature,
                                                                bdFaceCheckConfig.featureCheckMode,
                                                                bdFaceCheckConfig.activeModel,
                                                                rgbScores,
                                                                bdLiveConfig.rgbLiveScore);
                                                    }
                                                    else{
                                                        onFeatureChecks(
                                                                i,
                                                                rgbInstance,
                                                                bdFaceCheckConfig,
                                                                faceInfos,
                                                                faceInfosIr,
                                                                nirInstance,
                                                                livenessModel,
                                                                bdFaceCheckConfig.secondFeature,
                                                                bdFaceCheckConfig.featureCheckMode,
                                                                bdFaceCheckConfig.activeModel,
                                                                rgbScores,
                                                                -1);
                                                    }

                                                }
                                            }
                                        }
                                    }

                                    // 流程结束,记录最终时间
                                    livenessModel.setAllDetectDuration(System.currentTimeMillis() - startTime);
                                    //                LogUtils.e(TIME_TAG, "all process time = " + livenessModel.getAllDetectDuration());
                                    // 流程结束销毁图片，开始下一帧图片检测，否着内存泄露
                                    rgbInstance.destory();
                                    if (nirInstance != null) {
                                        nirInstance.destory();
                                    }
                                    // 显示最终结果提示
                                    if (faceDetectCallBack != null) {
                                        faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                    }
                                }

                                @Override
                                public void onDetectFail() {

                                    if (faceDetectCallBack != null) {
                                        faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                    }
                                }
                            });
                    }
                });
    }

    /**
     * 最优人脸控制
     *
     * @param livenessModel
     * @param faceDetectCallBack
     * @return
     */
    public boolean onBestImageCheck(
        LivenessModel livenessModel, BDFaceCheckConfig bdFaceCheckConfig, FaceDetectCallBack faceDetectCallBack) {
        boolean isBestImageCheck = false;
        if (livenessModel != null && livenessModel.getFaceInfos() != null) {
            FaceInfo[] faceInfos = livenessModel.getFaceInfos();
            int size = faceInfos.length;
            for (int i = 0; i < size; i++) {
                float bestImageScore = faceInfos[i].bestImageScore;
                if (bestImageScore > 0.5) {
                    isBestImageCheck = true;
                }
            }
        }
        return isBestImageCheck;
    }

    /**
     * 特征提取-人脸识别比对
     *
     * @param rgbInstance      可见光底层送检对象
     * @param landmark         检测眼睛，嘴巴，鼻子，72个关键点
     * @param faceInfos        nir人脸数据
     * @param nirInstance      nir 图像句柄
     * @param livenessModel    检测结果数据集合
     * @param featureCheckMode 特征抽取模式【不提取特征：1】；【提取特征：2】；【提取特征+1：N检索：3】；
     * @param featureType      特征抽取模态执行 【生活照：1】；【证件照：2】；【混合模态：3】；
     */
    private void onFeatureCheck(
        BDFaceImageInstance rgbInstance,
        BDFaceCheckConfig bdFaceCheckConfig,
        float[] landmark,
        FaceInfo[] faceInfos,
        BDFaceImageInstance nirInstance,
        LivenessModel livenessModel,
        byte[] secondFeature,
        final int featureCheckMode,
        final int featureType) {
        // 如果不抽取特征，直接返回
        if (featureCheckMode == 1) {
            return;
        }
        byte[] feature = new byte[512];
        if (featureType == 3) {
            // todo: 混合模态使用方式是根据图片的曝光来选择需要使用的type，光照的取值范围为：0~1之间
            AtomicInteger atomicInteger = new AtomicInteger();
            FaceSDKManager.getInstance().getImageIllum().imageIllum(rgbInstance, atomicInteger);
            int illumScore = atomicInteger.get();
            BDQualityConfig bdQualityConfig = bdFaceCheckConfig.bdQualityConfig;
            boolean isIllum = bdQualityConfig != null ? illumScore < bdQualityConfig.illum : false;
            BDFaceSDKCommon.FeatureType type =
                isIllum
                    ? BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_NIR
                    : BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO;
            BDFaceImageInstance bdFaceImageInstance = isIllum ? nirInstance : rgbInstance;
            float[] landmarks = isIllum ? faceInfos[0].landmarks : landmark;

            long startFeatureTime = System.currentTimeMillis();
            float featureSize = faceModel.getFaceFeature().feature(type, bdFaceImageInstance, landmarks, feature);
            livenessModel.setFeatureDuration(System.currentTimeMillis() - startFeatureTime);
            livenessModel.setFeature(feature);
            // 人脸检索
            featureSearch(
                featureCheckMode,
                livenessModel,
                bdFaceCheckConfig,
                feature,
                secondFeature,
                featureSize,
                BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO);
        } else {
            // 生活照检索
            long startFeatureTime = System.currentTimeMillis();
            float featureSize =
                faceModel
                    .getFaceFeature()
                    .feature(
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, rgbInstance, landmark, feature);
            livenessModel.setFeatureDuration(System.currentTimeMillis() - startFeatureTime);
            livenessModel.setFeature(feature);
            livenessModel.setFeatureDuration(System.currentTimeMillis() - startFeatureTime);
            // 人脸检索
            featureSearch(
                featureCheckMode,
                livenessModel,
                bdFaceCheckConfig,
                feature,
                secondFeature,
                featureSize,
                BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO);
        }
    }

    /**
     * 特征提取-人脸识别比对
     *
     * @param rgbInstance      可见光底层送检对象
     * @param rgbFaceInfos     rgb人脸数据
     * @param faceInfos        nir人脸数据
     * @param nirInstance      nir 图像句柄
     * @param livenessModel    检测结果数据集合
     * @param featureCheckMode 特征抽取模式【不提取特征：1】；【提取特征：2】；【提取特征+1：N检索：3】；
     * @param featureType      特征抽取模态执行 【生活照：1】；【证件照：2】；【混合模态：3】；
     * @param rgbScores 可见光得分
     * @param liveScore 可见光活体阈值
     */
    private void onFeatureChecks(
        int index,
        BDFaceImageInstance rgbInstance,
        BDFaceCheckConfig bdFaceCheckConfig,
        FaceInfo[] rgbFaceInfos,
        FaceInfo[] faceInfos,
        BDFaceImageInstance nirInstance,
        LivenessModel livenessModel,
        byte[] secondFeature,
        final int featureCheckMode,
        final int featureType,
        float[] rgbScores,
        float liveScore) {
        // 如果不抽取特征，直接返回

        if (featureCheckMode == 1) {

            return;
        }
        byte[] feature = new byte[512];

        if (featureType == 3) {
            // todo: 混合模态使用方式是根据图片的曝光来选择需要使用的type，光照的取值范围为：0~1之间
            AtomicInteger atomicInteger = new AtomicInteger();
            FaceSDKManager.getInstance().getImageIllum().imageIllum(rgbInstance, atomicInteger);
            int illumScore = atomicInteger.get();
            BDQualityConfig bdQualityConfig = bdFaceCheckConfig.bdQualityConfig;
            boolean isIllum = bdQualityConfig != null ? illumScore < bdQualityConfig.illum : false;
            BDFaceSDKCommon.FeatureType type =
                isIllum
                    ? BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_NIR
                    : BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO;
            BDFaceImageInstance bdFaceImageInstance = isIllum ? nirInstance : rgbInstance;

            if (rgbFaceInfos == null) {
                return;
            }
            int size = faceInfos.length;

            if (rgbFaceInfos[index].landmarks == null) {
                return;
            }

            if (rgbScores != null) {
                float score = rgbScores[index];
                if (score < liveScore) {
                    return;
                }
            }

            float[] landmarks = rgbFaceInfos[index].landmarks;

            long startFeatureTime = System.currentTimeMillis();
            float featureSize = faceModel.getFaceFeature().feature(type, bdFaceImageInstance, landmarks, feature);
            livenessModel.setFeatureDuration(System.currentTimeMillis() - startFeatureTime);
            livenessModel.setFeature(feature);
            // 人脸检索
            featureSearchs(
                index,
                featureCheckMode,
                livenessModel,
                bdFaceCheckConfig,
                feature,
                secondFeature,
                featureSize,
                BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO);

        } else {
            // 生活照检索
            long startFeatureTime = System.currentTimeMillis();
            if (rgbFaceInfos == null) {

                return;
            }

            int size = rgbFaceInfos.length;

            if (rgbFaceInfos[index].landmarks == null) {
                return;
            }

            if (rgbScores != null) {
                float score = rgbScores[index];
                if (score < liveScore) {
                    return;
                }
            }

            float featureSize =
                faceModel
                    .getFaceFeature()
                    .feature(
                        BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                        rgbInstance,
                        rgbFaceInfos[index].landmarks,
                        feature);

            livenessModel.setFeatureDuration(System.currentTimeMillis() - startFeatureTime);
            livenessModel.setFeature(feature);
            livenessModel.setFeatureDuration(System.currentTimeMillis() - startFeatureTime);
            // 人脸检索

            featureSearchs(
                index,
                featureCheckMode,
                livenessModel,
                bdFaceCheckConfig,
                feature,
                secondFeature,
                featureSize,
                BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO);
        }
    }

    /**
     * 人脸库检索
     *
     * @param featureCheckMode 特征抽取模式【不提取特征：1】；【提取特征：2】；【提取特征+1：N检索：3】；
     * @param livenessModel    检测结果数据集合
     * @param feature          特征点
     * @param secondFeature    1:1 特征点
     * @param featureSize      特征点的size
     * @param type             特征提取类型
     */
    private void featureSearch(
        final int featureCheckMode,
        LivenessModel livenessModel,
        BDFaceCheckConfig bdFaceCheckConfig,
        byte[] feature,
        byte[] secondFeature,
        float featureSize,
        BDFaceSDKCommon.FeatureType type) {

        // 如果只提去特征，不做检索，此处返回
        if (featureCheckMode == 2) {
            livenessModel.setFeatureCode(featureSize);
            return;
        }
        // 如果提取特征+检索，调用search 方法
        if (featureSize == FEATURE_SIZE / 4) {
            long startFeature = System.currentTimeMillis();
            // 特征提取成功
            // TODO 阈值可以根据不同模型调整
            if (featureCheckMode == 3) {
                List<? extends Feature> featureResult =
                    faceModel.getFaceSearch().search(type, bdFaceCheckConfig.scoreThreshold, 1, feature, false);

                // TODO 返回top num = 1 个数据集合，此处可以任意设置，会返回比对从大到小排序的num 个数据集合
                if (featureResult != null && featureResult.size() > 0) {

                    // 获取第一个数据
                    Feature topFeature = featureResult.get(0);
                    // 判断第一个阈值是否大于设定阈值，如果大于，检索成功
                    threholdScore = bdFaceCheckConfig.scoreThreshold;
                    if (topFeature != null && topFeature.getScore() > threholdScore) {
                        // 当前featureEntity 只有id+feature 索引，在数据库中查到完整信息
                        User user = FaceApi.getInstance().getUserListById(topFeature.getId());
                        if (user != null) {
                            livenessModel.setUser(user);
                            livenessModel.setFeatureScore(topFeature.getScore());
                            /*faceId = livenessModel.getFaceInfo().faceID;
                            trackTime = System.currentTimeMillis();
                            faceAdoptModel = livenessModel;
                            failNumber = 0;
                            isFail = false;*/
                            setFail(livenessModel);
                        } else {
                            setFail(livenessModel);
                        }
                    } else {
                        setFail(livenessModel);
                    }
                } else {
                    setFail(livenessModel);
                }
            } else if (featureCheckMode == 4) {
                // 目前仅支持
                float score =
                    faceModel
                        .getFaceSearch()
                        .compare(
                            BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_ID_PHOTO,
                            livenessModel.getFeature(),
                            secondFeature,
                            true);
                livenessModel.setScore(score);
                if (score > threholdScore) {
                    /*faceId = livenessModel.getFaceInfo().faceID;
                    trackTime = System.currentTimeMillis();
                    faceAdoptModel = livenessModel;
                    failNumber = 0;
                    isFail = false;*/
                    setFail(livenessModel);
                } else {
                    setFail(livenessModel);
                }
            }
            livenessModel.setCheckDuration(System.currentTimeMillis() - startFeature);
        }
    }

    /**
     * 人脸库检索
     *
     * @param featureCheckMode 特征抽取模式【不提取特征：1】；【提取特征：2】；【提取特征+1：N检索：3】；
     * @param livenessModel    检测结果数据集合
     * @param feature          特征点
     * @param secondFeature    1:1 特征点
     * @param featureSize      特征点的size
     * @param type             特征提取类型
     */
    private void featureSearchs(
        final int index,
        final int featureCheckMode,
        LivenessModel livenessModel,
        BDFaceCheckConfig bdFaceCheckConfig,
        byte[] feature,
        byte[] secondFeature,
        float featureSize,
        BDFaceSDKCommon.FeatureType type) {

        // 如果只提去特征，不做检索，此处返回

        if (featureCheckMode == 2) {
            livenessModel.setFeatureCode(featureSize);
            return;
        }

        // 如果提取特征+检索，调用search 方法
        if (featureSize == FEATURE_SIZE / 4) {
            long startFeature = System.currentTimeMillis();
            // 特征提取成功

            // TODO 阈值可以根据不同模型调整
            if (featureCheckMode == 3) {

                List<? extends Feature> featureResult =
                    faceModel.getFaceSearch().search(type, 80, 1, feature, true);

                Log.e("aaaaaaaaaaaaaaaa" , "数量 : " + faceModel.getFaceSearch().getSize());
                // TODO 返回top num = 1 个数据集合，此处可以任意设置，会返回比对从大到小排序的num 个数据集合
                if (featureResult != null && featureResult.size() > 0) {

                    // 获取第一个数据
                    Feature topFeature = featureResult.get(0);
                    // 判断第一个阈值是否大于设定阈值，如果大于，检索成功
                    threholdScore = bdFaceCheckConfig.scoreThreshold;
                    if (topFeature != null && topFeature.getScore() > threholdScore) {
                        // 当前featureEntity 只有id+feature 索引，在数据库中查到完整信息
                        Log.e("aaaaaaaaaaaaaaaa" , "" + topFeature.getScore());
                        User user = FaceApi.getInstance().getUserListById(topFeature.getId());
                        if (user != null) {
                            IdentifyResult idResult = new IdentifyResult(user, index, topFeature.getScore());
                            // Log.d("Attend", "add user:" + user.getUserInfo() + " index:" + index);
                            livenessModel.addIdentifyResult(idResult);
                            livenessModel.setUser(user);
                            livenessModel.setFeatureScore(topFeature.getScore());

                            setFail(livenessModel);
                        } else {
                            IdentifyResult idResult = new IdentifyResult(user, index, topFeature.getScore());
                            // Log.d("Attend", "add user222:" + user.getUserInfo() + " index:" + index);
                            setFail(livenessModel);
                        }
                    } else {
                        setFail(livenessModel);
                    }
                } else {
                    // Log.d("Attend", "featureSearchs 333333：" + index + " featureResult:0");
                    setFail(livenessModel);
                }
            } else if (featureCheckMode == 4) {
                // 目前仅支持
                float score =
                    faceModel
                        .getFaceSearch()
                        .compare(
                            BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_ID_PHOTO,
                            livenessModel.getFeature(),
                            secondFeature,
                            true);
                livenessModel.setScore(score);
                if (score > threholdScore) {
                    /*faceId = livenessModel.getFaceInfo().faceID;
                    trackTime = System.currentTimeMillis();现在就感觉当年我又nb又sb
                    faceAdoptModel = livenessModel;
                    failNumber = 0;
                    isFail = false;*/
                    setFail(livenessModel);
                } else {
                    setFail(livenessModel);
                }
            }
            livenessModel.setCheckDuration(System.currentTimeMillis() - startFeature);
        }
    }

    /**
     * 金融活检-检测-活体
     *
     * @param bdFaceImageConfig      可见光YUV 数据流
     * @param bdNirFaceImageConfig   红外YUV 数据流
     * @param bdDepthFaceImageConfig 深度depth 数据流
     * @param bdFaceCheckConfig      识别参数
     * @param faceDetectCallBack
     */
    public void onDetectSilentLiveCheck(
        final BDFaceImageConfig bdFaceImageConfig,
        final BDFaceImageConfig bdNirFaceImageConfig,
        final BDFaceImageConfig bdDepthFaceImageConfig,
        final BDFaceCheckConfig bdFaceCheckConfig,
        final FaceDetectCallBack faceDetectCallBack) {
        long startTime = System.currentTimeMillis();
        // 创建检测结果存储数据
        LivenessModel livenessModel = new LivenessModel();
        // 创建检测对象，如果原始数据YUV，转为算法检测的图片BGR
        // TODO: 用户调整旋转角度和是否镜像，手机和开发版需要动态适配
        BDFaceImageInstance rgbInstance = getBdImage(bdFaceImageConfig, bdFaceCheckConfig.darkEnhance);
        livenessModel.setTestBDFaceImageInstanceDuration(System.currentTimeMillis() - startTime);
        onTrack(
            rgbInstance,
            livenessModel,
            new DetectListener() {
                @Override
                public void onDetectSuccess(FaceInfo[] faceInfos, BDFaceImageInstance rgbInstance) {

                    // 保存人脸特征点
                    livenessModel.setLandmarks(faceInfos[0].landmarks);
                    // 保存人脸图片
                    livenessModel.setBdFaceImageInstance(rgbInstance.getImage());
                    // 调用绘制人脸框接口
                    if (faceDetectCallBack != null) {
                        faceDetectCallBack.onFaceDetectDarwCallback(livenessModel);
                    }
                    // 送检识别
                    onSilentLivenessCheck(
                        rgbInstance,
                        bdNirFaceImageConfig,
                        bdDepthFaceImageConfig,
                        bdFaceCheckConfig,
                        livenessModel,
                        startTime,
                        faceDetectCallBack,
                        faceInfos);
                }

                @Override
                public void onDetectFail() {
                    // 流程结束销毁图片，开始下一帧图片检测，否着内存泄露
                    if (faceDetectCallBack != null) {
                        faceDetectCallBack.onFaceDetectCallback(null);
                        livenessModel.setBdFaceImageInstance(rgbInstance.getImage());
                        faceDetectCallBack.onFaceDetectDarwCallback(livenessModel);
                        faceDetectCallBack.onTip(0, "未检测到人脸");
                    }
                    rgbInstance.destory();
                }
            });
    }

    /**
     * 金融活检-活体
     *
     * @param rgbInstance            可见光底层送检对象
     * @param nirBDFaceImageConfig   红外YUV 数据流
     * @param depthBDFaceImageConfig 深度depth 数据流
     * @param livenessModel          检测结果数据集合
     * @param startTime              开始检测时间
     * @param faceDetectCallBack
     */
    public void onSilentLivenessCheck(
        final BDFaceImageInstance rgbInstance,
        final BDFaceImageConfig nirBDFaceImageConfig,
        final BDFaceImageConfig depthBDFaceImageConfig,
        final BDFaceCheckConfig bdFaceCheckConfig,
        final LivenessModel livenessModel,
        final long startTime,
        final FaceDetectCallBack faceDetectCallBack,
        final FaceInfo[] fastFaceInfos) {

        if (future2 != null && !future2.isDone()) {
            // 流程结束销毁图片，开始下一帧图片检测，否着内存泄露
            rgbInstance.destory();
            return;
        }

        future2 =
            es2.submit(
                new Runnable() {

                    @Override
                    public void run() {
                        onDetect(
                            bdFaceCheckConfig,
                            rgbInstance,
                            fastFaceInfos,
                            livenessModel,
                            new DetectListener() {
                                @Override
                                public void onDetectSuccess(FaceInfo[] faceInfos, BDFaceImageInstance rgbInstance) {
                                    // 人脸id赋值
                                    if (mLastFaceId != fastFaceInfos[0].faceID) {
                                        mLastFaceId = fastFaceInfos[0].faceID;
                                        mRgbLiveList.clear();
                                        mNirLiveList.clear();
                                    }
                                    if (bdFaceCheckConfig == null) {
                                        rgbInstance.destory();
                                        if (faceDetectCallBack != null) {
                                            faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                        }
                                        return;
                                    }
                                    // 最优人脸控制
                                    if (!onBestImageCheck(livenessModel, bdFaceCheckConfig, faceDetectCallBack)) {
                                        livenessModel.setQualityCheck(true);
                                        rgbInstance.destory();
                                        if (faceDetectCallBack != null) {
                                            faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                        }
                                        return;
                                    }
                                    onQualityCheck(
                                        faceInfos,
                                        bdFaceCheckConfig.bdQualityConfig,
                                        faceDetectCallBack,
                                        new QualityListener() {
                                            @Override
                                            public void onQualitySuccess() {
                                                livenessModel.setQualityCheck(false);
                                                // 获取LivenessConfig liveCheckMode 配置选项：【不使用活体：0】；【RGB活体：1】；【RGB+NIR活体：2】；【RGB+Depth活体：3】；【RGB+NIR+Depth活体：4】
                                                // TODO 活体检测
                                                BDLiveConfig bdLiveConfig = bdFaceCheckConfig.bdLiveConfig;
                                                boolean isLiveCheck = bdFaceCheckConfig.bdLiveConfig != null;
                                                if (isLiveCheck) {
                                                    long startRgbTime = System.currentTimeMillis();
                                                    boolean rgbLiveStatus =
                                                        faceModel
                                                            .getFaceLive()
                                                            .strategySilentLive(
                                                                BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_RGB,
                                                                rgbInstance,
                                                                faceInfos[0],
                                                                bdLiveConfig.framesThreshold,
                                                                bdLiveConfig.rgbLiveScore);
                                                    livenessModel.setRGBLiveStatus(rgbLiveStatus);
                                                    livenessModel.setRgbLivenessDuration(
                                                        System.currentTimeMillis() - startRgbTime);
                                                }
                                                // TODO nir活体检测
                                                BDFaceImageInstance nirInstance = null;
                                                boolean isHaveNirImage = nirBDFaceImageConfig != null && isLiveCheck;
                                                if (isHaveNirImage) {

                                                    // 创建检测对象，如果原始数据YUV-IR，转为算法检测的图片BGR
                                                    // TODO: 用户调整旋转角度和是否镜像，手机和开发版需要动态适配
                                                    nirInstance = getBdImage(nirBDFaceImageConfig, false);
                                                    livenessModel.setBdNirFaceImageInstance(nirInstance.getImage());

                                                    // 避免RGB检测关键点在IR对齐活体稳定，增加红外检测
                                                    long startIrDetectTime = System.currentTimeMillis();
                                                    BDFaceDetectListConf bdFaceDetectListConf =
                                                        new BDFaceDetectListConf();
                                                    bdFaceDetectListConf.usingDetect = true;
                                                    FaceInfo[] faceInfosIr =
                                                        faceModel
                                                            .getFaceNirDetect()
                                                            .detect(
                                                                BDFaceSDKCommon.DetectType.DETECT_NIR,
                                                                BDFaceSDKCommon.AlignType
                                                                    .BDFACE_ALIGN_TYPE_NIR_ACCURATE,
                                                                nirInstance,
                                                                null,
                                                                bdFaceDetectListConf);
                                                    bdFaceDetectListConf.usingDetect = false;
                                                    livenessModel.setIrLivenessDuration(
                                                        System.currentTimeMillis() - startIrDetectTime);
                                                    //                    LogUtils.e(TIME_TAG, "detect ir time = " + livenessModel.getIrLivenessDuration());

                                                    if (faceInfosIr != null && faceInfosIr.length > 0) {
                                                        FaceInfo faceInfoIr = faceInfosIr[0];
                                                        long startNirTime = System.currentTimeMillis();
                                                        boolean nirLiveStatus =
                                                            faceModel
                                                                .getFaceLive()
                                                                .strategySilentLive(
                                                                    BDFaceSDKCommon.LiveType
                                                                        .BDFACE_SILENT_LIVE_TYPE_NIR,
                                                                    nirInstance,
                                                                    faceInfoIr,
                                                                    bdLiveConfig.framesThreshold,
                                                                    bdLiveConfig.nirLiveScore);
                                                        livenessModel.setNIRLiveStatus(nirLiveStatus);
                                                        livenessModel.setIrLivenessDuration(
                                                            System.currentTimeMillis() - startNirTime);
                                                    }

                                                    nirInstance.destory();
                                                }
                                                // TODO depth活体检测
                                                if (depthBDFaceImageConfig != null) {
                                                    fastFaceInfos[0].landmarks = faceInfos[0].landmarks;
                                                    // TODO: 用户调整旋转角度和是否镜像，适配Atlas 镜头，目前宽和高400*640，其他摄像头需要动态调整,人脸72 个关键点x 坐标向左移动80个像素点
                                                    float[] depthLandmark = new float[faceInfos[0].landmarks.length];
                                                    BDFaceImageInstance depthInstance;
                                                    if (bdFaceCheckConfig.cameraType == 1) {
                                                        System.arraycopy(
                                                            faceInfos[0].landmarks,
                                                            0,
                                                            depthLandmark,
                                                            0,
                                                            faceInfos[0].landmarks.length);
                                                        for (int i = 0; i < 144; i = i + 2) {
                                                            depthLandmark[i] -= 80;
                                                        }
                                                        fastFaceInfos[0].landmarks = depthLandmark;
                                                    }

                                                    depthInstance = getBdImage(depthBDFaceImageConfig, false);
                                                    livenessModel.setBdDepthFaceImageInstance(depthInstance.getImage());
                                                    // 创建检测对象，如果原始数据Depth
                                                    long startDepthTime = System.currentTimeMillis();
                                                    boolean depthLiveStatus =
                                                        faceModel
                                                            .getFaceLive()
                                                            .strategySilentLive(
                                                                BDFaceSDKCommon.LiveType.BDFACE_SILENT_LIVE_TYPE_DEPTH,
                                                                depthInstance,
                                                                fastFaceInfos[0],
                                                                bdLiveConfig.framesThreshold,
                                                                bdLiveConfig.nirLiveScore);
                                                    livenessModel.setDepthLiveStatus(depthLiveStatus);
                                                    livenessModel.setDepthtLivenessDuration(
                                                        System.currentTimeMillis() - startDepthTime);
                                                    depthInstance.destory();
                                                }
                                                // 流程结束,记录最终时间
                                                livenessModel.setAllDetectDuration(
                                                    System.currentTimeMillis() - startTime);
                                                //                LogUtils.e(TIME_TAG, "all process time = " + livenessModel.getAllDetectDuration());
                                                // 流程结束销毁图片，开始下一帧图片检测，否着内存泄露
                                                rgbInstance.destory();
                                                // 显示最终结果提示
                                                if (faceDetectCallBack != null) {
                                                    faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                                }
                                            }

                                            @Override
                                            public void onQualityFail(String detectFail, String occlusionFail) {
                                                livenessModel.setQualityOcclusion(occlusionFail);
                                                livenessModel.setQualityDetect(detectFail);
                                                livenessModel.setQualityCheck(true);
                                                rgbInstance.destory();
                                                if (faceDetectCallBack != null) {
                                                    faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                                }
                                            }
                                        });
                                }

                                @Override
                                public void onDetectFail() {

                                    if (faceDetectCallBack != null) {
                                        faceDetectCallBack.onFaceDetectCallback(livenessModel);
                                    }
                                }
                            });
                    }
                });
    }

    private void onTrack(BDFaceImageInstance rgbInstance, LivenessModel livenessModel, DetectListener detectListener) {

        long startDetectTime = System.currentTimeMillis();

        livenessModel.setRgbDetectDuration(System.currentTimeMillis() - startDetectTime);
        // track
        FaceInfo[] faceInfos = null;
        // 多人采用检测，不能跟踪
        if (isMultiIdentify){
            faceInfos = getDetectCheck(rgbInstance);
        }
        else{
            faceInfos = getTrackCheck(rgbInstance);
        }

        // 检测结果判断
        if (faceInfos == null || faceInfos.length == 0) {
            detectListener.onDetectFail();
            return;
        }
        livenessModel.setTrackFaceInfo(faceInfos);
        //  livenessModel.setFaceInfo(faceInfos[0]);
        // 修改为返回多人脸框
        //  livenessModel.setFaceInfos(faceInfos);
        livenessModel.setFaceInfos(faceInfos);
        livenessModel.setTrackLandmarks(faceInfos[0].landmarks);

        livenessModel.setTrackStatus(1);

        //  detectListener.onDetectSuccess(faceInfos, rgbInstance);
        detectListener.onDetectSuccess(faceInfos, rgbInstance);

        /*  if (size > 0) {
            byte[] featureArr1 = new byte[512];
            byte[] featureArr2 = new byte[512];

            float featureSize = faceModel.getFaceFeature().feature(
                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, rgbInstance, new_faceInfos[0].landmarks, featureArr1);

            Log.d("Attend", "compare_score1111:" + featureSize);
            featureSize = faceModel.getFaceFeature().feature(
                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO, rgbInstance, new_faceInfos[0].landmarks, featureArr1);

            Log.d("Attend", "compare_score2222:" + featureSize);
            FaceSearch faceSearch = new FaceSearch();
            float featureScore = faceSearch.compare(
                    BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                    featureArr1,
                    featureArr2,
                    true);
            Log.d("Attend", "compare_score:" + featureScore);
        } */

    }

    /**
     * 质量检测结果过滤，如果需要质量检测，
     * 需要调用 SingleBaseConfig.getBaseConfig().setQualityControl(true);设置为true，
     * 再调用  FaceSDKManager.getInstance().initConfig() 加载到底层配置项中
     *
     * @param faceInfos
     * @param faceInfos
     * @param faceDetectCallBack
     * @return
     */
    public void onQualityCheck(
        final FaceInfo[] faceInfos,
        final BDQualityConfig bdQualityConfig,
        final FaceDetectCallBack faceDetectCallBack,
        final QualityListener qualityListener) {

        if (bdQualityConfig == null) {
            qualityListener.onQualitySuccess();
            return;
        }
        // 检测结果过滤
        // 角度过滤

        boolean checkQualityOk = true;
        String detectFail = "";
        String occlusionFail = "";
        if (faceInfos != null) {
            int size = faceInfos.length;

            for (int i = 0; i < size; i++) {
                StringBuffer stringBufferDetected = new StringBuffer();
                StringBuffer stringBufferOcclusion = new StringBuffer();

                FaceInfo faceInfo = faceInfos[i];

                // 角度过滤
                if (Math.abs(faceInfo.yaw) > bdQualityConfig.gesture) {
                    faceDetectCallBack.onTip(-1, "人脸左右偏转角超出限制");
                    stringBufferDetected.append("人脸左右偏转角超出限制");
                } else if (Math.abs(faceInfo.roll) > bdQualityConfig.gesture) {
                    faceDetectCallBack.onTip(-1, "人脸平行平面内的头部旋转角超出限制");
                    stringBufferDetected.append("人脸平行平面内的头部旋转角超出限制");
                } else if (Math.abs(faceInfo.pitch) > bdQualityConfig.gesture) {
                    faceDetectCallBack.onTip(-1, "人脸上下偏转角超出限制");
                    stringBufferDetected.append("人脸上下偏转角超出限制");
                }

                // 模糊结果过滤
                float blur = faceInfo.bluriness;
                if (blur > bdQualityConfig.blur) {
                    faceDetectCallBack.onTip(-1, "图片模糊");
                    stringBufferDetected.append("图片模糊");
                }

                // 光照结果过滤
                float illum = faceInfo.illum;
                Log.e("illum", "illum = " + illum);
                if (illum < bdQualityConfig.illum) {
                    faceDetectCallBack.onTip(-1, "图片光照不通过");
                    stringBufferDetected.append("图片光照不通过");
                }

                if (!checkMouthMask) {
                    // 遮挡结果过滤
                    if (faceInfo.occlusion != null) {
                        BDFaceOcclusion occlusion = faceInfo.occlusion;

                        if (occlusion.leftEye > bdQualityConfig.leftEye) {
                            // 左眼遮挡置信度
                            faceDetectCallBack.onTip(-1, "左眼遮挡");
                            stringBufferOcclusion.append("左眼遮挡");
                        } else if (occlusion.rightEye > bdQualityConfig.rightEye) {
                            // 右眼遮挡置信度
                            faceDetectCallBack.onTip(-1, "右眼遮挡");
                            stringBufferOcclusion.append("右眼遮挡");
                        } else if (occlusion.nose > bdQualityConfig.nose) {
                            // 鼻子遮挡置信度
                            faceDetectCallBack.onTip(-1, "鼻子遮挡");
                            stringBufferOcclusion.append("鼻子遮挡");
                        } else if (occlusion.mouth > bdQualityConfig.mouth) {
                            // 嘴巴遮挡置信度
                            faceDetectCallBack.onTip(-1, "嘴巴遮挡");
                            stringBufferOcclusion.append("嘴巴遮挡");
                        } else if (occlusion.leftCheek > bdQualityConfig.leftCheek) {
                            // 左脸遮挡置信度
                            faceDetectCallBack.onTip(-1, "左脸遮挡");
                            stringBufferOcclusion.append("左脸遮挡");
                        } else if (occlusion.rightCheek > bdQualityConfig.rightCheek) {
                            // 右脸遮挡置信度
                            faceDetectCallBack.onTip(-1, "右脸遮挡");
                            stringBufferOcclusion.append("右脸遮挡");
                        } else if (occlusion.chin > bdQualityConfig.chinContour) {
                            // 下巴遮挡置信度
                            faceDetectCallBack.onTip(-1, "下巴遮挡");
                            stringBufferOcclusion.append("下巴遮挡");
                        }
                    }
                }
                if (!TextUtils.isEmpty(stringBufferDetected.toString())
                    || !TextUtils.isEmpty(stringBufferOcclusion.toString())) {
                    if (!TextUtils.isEmpty(stringBufferDetected.toString())) {
                        detectFail = stringBufferDetected.toString();
                    }
                    if (!TextUtils.isEmpty(stringBufferOcclusion.toString())) {
                        occlusionFail = stringBufferOcclusion.toString();
                    }
                    checkQualityOk = false;
                }
            }
        }
        if (checkQualityOk) {
            qualityListener.onQualitySuccess();
            return;
        }
        qualityListener.onQualityFail(detectFail, occlusionFail);
    }
    /**
     * 质量检测结果过滤，如果需要质量检测，
     * 需要调用 SingleBaseConfig.getBaseConfig().setQualityControl(true);设置为true，
     * 再调用  FaceSDKManager.getInstance().initConfig() 加载到底层配置项中
     *
     * @param faceInfos
     * @param faceInfos
     * @param faceDetectCallBack
     * @return
     */
    public boolean onQualityCheck(
        final FaceInfo[] faceInfos,
        final BDQualityConfig bdQualityConfig,
        final FaceDetectCallBack faceDetectCallBack) {

        if (bdQualityConfig == null) {
            lastQualityDetectFail = "";
            lastQualityOcclusionFail = "";
            return true;
        }
        boolean qualityCheck = false;
        // 用于存储检测失败信息
        StringBuffer detectFailBuffer = new StringBuffer();
        StringBuffer occlusionFailBuffer = new StringBuffer();

        if (faceInfos != null) {
            int size = faceInfos.length;
            for (int i = 0; i < size; ++i) {
                FaceInfo faceInfo = faceInfos[i];
                boolean checkItem = true;
                // 角度过滤
                if (Math.abs(faceInfo.yaw) > bdQualityConfig.gesture) {
                    faceDetectCallBack.onTip(-1, "人脸左右偏转角超出限制");
                    detectFailBuffer.append("人脸左右偏转角超出限制");
                    checkItem = false;
                } else if (Math.abs(faceInfo.roll) > bdQualityConfig.gesture) {
                    faceDetectCallBack.onTip(-1, "人脸平行平面内的头部旋转角超出限制");
                    detectFailBuffer.append("人脸平行平面内的头部旋转角超出限制");
                    checkItem = false;
                } else if (Math.abs(faceInfo.pitch) > bdQualityConfig.gesture) {
                    faceDetectCallBack.onTip(-1, "人脸上下偏转角超出限制");
                    detectFailBuffer.append("人脸上下偏转角超出限制");
                    checkItem = false;
                }

                // 模糊结果过滤
                float blur = faceInfo.bluriness;
                if (blur > bdQualityConfig.blur) {
                    faceDetectCallBack.onTip(-1, "图片模糊");
                    detectFailBuffer.append("图片模糊");
                    checkItem = false;
                }

                // 光照结果过滤
                float illum = faceInfo.illum;
                Log.e("illum", "illum = " + illum);
                if (illum < bdQualityConfig.illum) {
                    faceDetectCallBack.onTip(-1, "图片光照不通过");
                    detectFailBuffer.append("图片光照不通过");
                    checkItem = false;
                }

                // 口罩识别不进行质量判断
                if (!checkMouthMask) {
                    // 遮挡结果过滤
                    if (faceInfo.occlusion != null) {
                        BDFaceOcclusion occlusion = faceInfo.occlusion;

                        if (occlusion.leftEye > bdQualityConfig.leftEye) {
                            // 左眼遮挡置信度
                            faceDetectCallBack.onTip(-1, "左眼遮挡");
                            occlusionFailBuffer.append("左眼遮挡");
                            checkItem = false;
                        } else if (occlusion.rightEye > bdQualityConfig.rightEye) {
                            // 右眼遮挡置信度
                            faceDetectCallBack.onTip(-1, "右眼遮挡");
                            occlusionFailBuffer.append("右眼遮挡");
                            checkItem = false;
                        } else if (occlusion.nose > bdQualityConfig.nose) {
                            // 鼻子遮挡置信度
                            faceDetectCallBack.onTip(-1, "鼻子遮挡");
                            occlusionFailBuffer.append("鼻子遮挡");
                            checkItem = false;
                        } else if (occlusion.mouth > bdQualityConfig.mouth) {
                            // 嘴巴遮挡置信度
                            faceDetectCallBack.onTip(-1, "嘴巴遮挡");
                            occlusionFailBuffer.append("嘴巴遮挡");
                            checkItem = false;
                        } else if (occlusion.leftCheek > bdQualityConfig.leftCheek) {
                            // 左脸遮挡置信度
                            faceDetectCallBack.onTip(-1, "左脸遮挡");
                            occlusionFailBuffer.append("左脸遮挡");
                            checkItem = false;
                        } else if (occlusion.rightCheek > bdQualityConfig.rightCheek) {
                            // 右脸遮挡置信度
                            faceDetectCallBack.onTip(-1, "右脸遮挡");
                            occlusionFailBuffer.append("右脸遮挡");
                            checkItem = false;
                        } else if (occlusion.chin > bdQualityConfig.chinContour) {
                            // 下巴遮挡置信度
                            faceDetectCallBack.onTip(-1, "下巴遮挡");
                            occlusionFailBuffer.append("下巴遮挡");
                            checkItem = false;
                        }
                    }
                }
                else{
                    lastQualityDetectFail = "";
                    lastQualityOcclusionFail = "";
                    return true;
                }

                if (checkItem) {
                    lastQualityDetectFail = "";
                    lastQualityOcclusionFail = "";
                    qualityCheck = true;
                    return qualityCheck;
                }
            }
        }
        // 保存失败信息到成员变量
        lastQualityDetectFail = detectFailBuffer.toString();
        lastQualityOcclusionFail = occlusionFailBuffer.toString();
        return qualityCheck;
    }

    /**
     * 质量检测结果过滤，如果需要质量检测，
     * 需要调用 SingleBaseConfig.getBaseConfig().setQualityControl(true);设置为true，
     * 再调用  FaceSDKManager.getInstance().initConfig() 加载到底层配置项中
     *
     * @param faceInfo
     * @param faceInfo
     * @param faceDetectCallBack
     * @return
     */
    public boolean onPersonQualityCheck(
        final FaceInfo faceInfo, final BDQualityConfig bdQualityConfig, final FaceDetectCallBack faceDetectCallBack) {

        if (bdQualityConfig == null) {
            return true;
        }

        if (faceInfo != null) {

            // 角度过滤
            if (Math.abs(faceInfo.yaw) > bdQualityConfig.gesture) {
                faceDetectCallBack.onTip(-1, "人脸左右偏转角超出限制");
                return false;
            } else if (Math.abs(faceInfo.roll) > bdQualityConfig.gesture) {
                faceDetectCallBack.onTip(-1, "人脸平行平面内的头部旋转角超出限制");
                return false;
            } else if (Math.abs(faceInfo.pitch) > bdQualityConfig.gesture) {
                faceDetectCallBack.onTip(-1, "人脸上下偏转角超出限制");
                return false;
            }

            // 模糊结果过滤
            float blur = faceInfo.bluriness;
            if (blur > bdQualityConfig.blur) {
                faceDetectCallBack.onTip(-1, "图片模糊");
                return false;
            }

            // 光照结果过滤
            float illum = faceInfo.illum;
            Log.e("illum", "illum = " + illum);
            if (illum < bdQualityConfig.illum) {
                faceDetectCallBack.onTip(-1, "图片光照不通过");
                return false;
            }

            // 口罩识别不进行质量判断
            if (!checkMouthMask) {
                // 遮挡结果过滤
                if (faceInfo.occlusion != null) {
                    BDFaceOcclusion occlusion = faceInfo.occlusion;

                    if (occlusion.leftEye > bdQualityConfig.leftEye) {
                        // 左眼遮挡置信度
                        faceDetectCallBack.onTip(-1, "左眼遮挡");
                    } else if (occlusion.rightEye > bdQualityConfig.rightEye) {
                        // 右眼遮挡置信度
                        faceDetectCallBack.onTip(-1, "右眼遮挡");
                    } else if (occlusion.nose > bdQualityConfig.nose) {
                        // 鼻子遮挡置信度
                        faceDetectCallBack.onTip(-1, "鼻子遮挡");
                    } else if (occlusion.mouth > bdQualityConfig.mouth) {
                        // 嘴巴遮挡置信度
                        faceDetectCallBack.onTip(-1, "嘴巴遮挡");
                    } else if (occlusion.leftCheek > bdQualityConfig.leftCheek) {
                        // 左脸遮挡置信度
                        faceDetectCallBack.onTip(-1, "左脸遮挡");
                    } else if (occlusion.rightCheek > bdQualityConfig.rightCheek) {
                        // 右脸遮挡置信度
                        faceDetectCallBack.onTip(-1, "右脸遮挡");
                    } else if (occlusion.chin > bdQualityConfig.chinContour) {
                        // 下巴遮挡置信度
                        faceDetectCallBack.onTip(-1, "下巴遮挡");
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检测-活体-特征- 全流程
     * bluriness 模糊得分
     * leftEye  左眼遮擋得分
     * rightEye 右眼遮擋得分
     * nose     鼻子遮擋得分
     * mouth    嘴巴遮擋得分
     * leftCheek 左臉眼遮擋得分
     * rightCheek 右臉遮擋得分
     */
    private boolean selectQuality(
        float bluriness,
        float leftEye,
        float rightEye,
        float nose,
        float mouth,
        float leftCheek,
        float rightCheek,
        float chin) {

        return bluriness < 0.5
            && leftEye < 0.75
            && rightEye < 0.75
            && nose < 0.75
            && mouth < 0.75
            && leftCheek < 0.75
            && rightCheek < 0.75
            && chin < 0.7;
    }

    // 人证核验特征提取
    public float personDetect(
        final Bitmap bitmap, final byte[] feature, final BDFaceCheckConfig bdFaceCheckConfig, Context context) {
        BDFaceImageInstance rgbInstance = new BDFaceImageInstance(bitmap);
        float ret = -1;
        FaceInfo[] faceInfos;
        BDQualityConfig bdQualityConfig = bdFaceCheckConfig == null ? null : bdFaceCheckConfig.bdQualityConfig;
        if (bdFaceCheckConfig != null) {
            bdFaceCheckConfig.bdFaceDetectListConfig.usingDetect = true;
            faceInfos =
                faceModel
                    .getFaceDetectPerson()
                    .detect(
                        BDFaceSDKCommon.DetectType.DETECT_VIS,
                        BDFaceSDKCommon.AlignType.BDFACE_ALIGN_TYPE_RGB_ACCURATE,
                        rgbInstance,
                        null,
                        bdFaceCheckConfig.bdFaceDetectListConfig);
        } else {
            faceInfos = faceModel.getFaceDetectPerson().detect(BDFaceSDKCommon.DetectType.DETECT_VIS, rgbInstance);
        }
        if (faceInfos != null && faceInfos.length > 0) {
            // 判断质量检测，针对模糊度、遮挡、角度
            if (onPersonQualityCheck(faceInfos[0], bdQualityConfig, new FaceQualityBack(context))) {
                ret =
                    faceModel
                        .getFacePersonFeature()
                        .feature(
                            BDFaceSDKCommon.FeatureType.BDFACE_FEATURE_TYPE_LIVE_PHOTO,
                            rgbInstance,
                            faceInfos[0].landmarks,
                            feature);
            }
        } else {
            rgbInstance.destory();
            return -1;
        }
        rgbInstance.destory();
        return ret;
    }
}
