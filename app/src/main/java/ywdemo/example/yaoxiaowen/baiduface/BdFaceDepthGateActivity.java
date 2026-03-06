package ywdemo.example.yaoxiaowen.baiduface;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;
import com.baidu.idl.main.facesdk.utils.PreferencesUtil;
import com.baidu.idl.main.facesdk.utils.PreferencesUtil;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.ImageRegistrationMode;
import org.openni.OpenNI;
import org.openni.PixelFormat;
import org.openni.SensorType;
import org.openni.VideoMode;
import org.openni.VideoStream;
import org.openni.VideoFrameRef;
import org.openni.android.OpenNIHelper;
import org.openni.android.OpenNIHelper.DeviceOpenListener;
import org.openni.android.OpenNIView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import ywdemo.example.yaoxiaowen.R;
import ywdemo.example.yaoxiaowen.baiduface.aobilibrary.idl.main.facesdk.utils.FaceUtils;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.activity.BaseOrbbecActivity;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.api.FaceApi;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback.CameraDataCallback;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.callback.FaceDetectCallBack;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gatecamera.CameraPreviewManager;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.gl.view.GlMantleSurfacView;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager.FaceSDKManager;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager.SaveImageManager;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SaveImageListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDFaceCheckConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDFaceImageConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDLiveConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.LivenessModel;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.BitmapUtils;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.FaceOnDrawTexturViewUtil;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.FileUtils;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.ToastUtils;
import ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.model.SingleBaseConfig;
import ywdemo.example.yaoxiaowen.until.LogUtil;

public class BdFaceDepthGateActivity extends BaseOrbbecActivity implements View.OnClickListener, DeviceOpenListener {

    private static final String TAG = "BdFaceActy";

    /*RGB摄像头图像宽和高*/
    private final int RGB_WIDTH = SingleBaseConfig.getBaseConfig().getRgbAndNirWidth();
    private final int RGB_HEIGHT = SingleBaseConfig.getBaseConfig().getRgbAndNirHeight();

    // Depth摄像头图像宽和高
    private int depthWidth = SingleBaseConfig.getBaseConfig().getDepthWidth();
    private int depthHeight = SingleBaseConfig.getBaseConfig().getDepthHeight();

    private Context mContext;

    // 调试页面控件
    private ImageView mFaceDetectImageView;
    private TextView mTvDetect;
    private TextView mTvLive;
    private TextView mTvLiveScore;
    private TextView mNum;

    // 深度数据显示
    private TextView mTvDepth;
    private TextView mTvDepthScore;

    private TextView mTvFeature;
    private TextView mTvAll;
    private TextView mTvAllTime;

    // 显示Depth图
    private OpenNIView mDepthGLView;

    // 设备初始化状态标记
    private boolean initOk = false;

    // 摄像头驱动
    private Device mDevice;
    private Thread thread;
    private OpenNIHelper mOpenNIHelper;
    private VideoStream mDepthStream;

    private final Object sync = new Object();

    // 循环取深度图像数据
    private boolean exit = false;

    /*当前摄像头类型*/
    private int cameraType = 0;

    private float rgbLiveScore = 0f;
    private float depthLiveScore = 0f;
    private boolean isCheck = true;  // 默认为开发模式
    private boolean isCompareCheck = true;
    // 已删除预览模式相关变量 (preText, preView, preViewRelativeLayout)
    private TextView deveLop;
    private RelativeLayout deveLopRelativeLayout;
    private TextView detectSurfaceText;
    private ImageView isRgbCheckImage;
    private ImageView isDepthCheckImage;
    // 已删除预览模式相关变量 (developView, 但保留用于开发模式)
    private View developView;
    private View view;
    private RelativeLayout layoutCompareStatus;
    private TextView textCompareStatus;
    private TextView nirSurfaceText;
    // logoText用于"百度大脑技术支持"文字显示
    private TextView logoText;
    private User mUser;
    private View saveCamera;
    private boolean isSaveImage = true;
    private View spot;
    private GlMantleSurfacView glSurfaceView;
    private BDFaceImageConfig bdFaceImageConfig;
    private BDFaceImageConfig bdDepthFaceImageConfig;
    private BDFaceCheckConfig bdFaceCheckConfig;
    private BDLiveConfig bdLiveConfig;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        initListener();
        // 人脸库已在BdStartActivity初始化，此处不再重复初始化
        // FaceSDKManager.getInstance().initDataBases(this);

        setContentView(R.layout.activity_face_depth_gate);

        exit = false;
        PreferencesUtil.initPrefs(this);
        cameraType = SingleBaseConfig.getBaseConfig().getCameraType();
        initFaceCheck();
        initView();
    }

    private void initFaceConfig(int height, int width) {
        bdFaceImageConfig = new BDFaceImageConfig(
                height, width,
                SingleBaseConfig.getBaseConfig().getRgbDetectDirection(),
                SingleBaseConfig.getBaseConfig().getMirrorDetectRGB(),
                BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_YUV_NV21
        );
    }

    private void initDepthFaceConfig(int height, int width) {
        bdDepthFaceImageConfig = new BDFaceImageConfig(
                height, width,
                SingleBaseConfig.getBaseConfig().getNirDetectDirection(),
                SingleBaseConfig.getBaseConfig().getMirrorDetectNIR(),
                BDFaceSDKCommon.BDFaceImageType.BDFACE_IMAGE_TYPE_DEPTH
        );
    }

    private void initFaceCheck() {
        bdFaceCheckConfig = FaceUtils.getInstance().getBDFaceCheckConfig();
        bdLiveConfig = FaceUtils.getInstance().getBDLiveConfig();
    }

    private void initListener() {
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_MODEL_LOAD_SUCCESS) {
            FaceSDKManager.getInstance().initModel(
                    mContext,
                    FaceUtils.getInstance().getBDFaceSDKConfig(), new SdkInitListener() {
                        @Override
                        public void initStart() {
                        }

                        @Override
                        public void initLicenseSuccess() {
                        }

                        @Override
                        public void initLicenseFail(int errorCode, String msg) {
                        }

                        @Override
                        public void initModelSuccess() {
                            FaceSDKManager.initModelSuccess = true;
                            ToastUtils.toast(mContext, "模型加载成功，欢迎使用");
                        }

                        @Override
                        public void initModelFail(int errorCode, String msg) {
                            FaceSDKManager.initModelSuccess = false;
                            LogUtil.e(TAG, "initModelFail() -> " + errorCode + "  " + msg);
                            if (errorCode != -12) {
                                ToastUtils.toast(mContext, "模型加载失败，请尝试重启应用");
                            }
                        }
                    });
        }
    }

    /**
     * 开启Debug View
     */
    private void initView() {
        depthWidth = SingleBaseConfig.getBaseConfig().getDepthWidth();
        depthHeight = SingleBaseConfig.getBaseConfig().getDepthHeight();
        // RGB 阈值
        rgbLiveScore = SingleBaseConfig.getBaseConfig().getRgbLiveScore();
        // depth 阈值
        depthLiveScore = SingleBaseConfig.getBaseConfig().getDepthLiveScore();
        logoText = findViewById(R.id.logo_text);
        logoText.setVisibility(View.VISIBLE);

        // 返回
        ImageView mButReturn = findViewById(R.id.btn_back);
        mButReturn.setOnClickListener(this);
        // 设置
        ImageView mBtSetting = findViewById(R.id.btn_setting);
        mBtSetting.setOnClickListener(this);
        // 已删除预览模式相关代码 (preText, preView, preViewRelativeLayout)
        // 开发模式（默认模式）
        deveLop = findViewById(R.id.develop_text);
        // 不再需要点击监听，因为只有开发模式
        // deveLop.setOnClickListener(this);
        deveLop.setTextColor(Color.parseColor("#ffffff"));
        deveLopRelativeLayout = findViewById(R.id.kaifa_relativeLayout);
        deveLopRelativeLayout.setVisibility(View.VISIBLE);  // 直接显示开发模式
        developView = findViewById(R.id.develop_view);
        developView.setVisibility(View.VISIBLE);  // 显示开发模式高亮
        layoutCompareStatus = findViewById(R.id.layout_compare_status);
        layoutCompareStatus.setVisibility(View.GONE);
        textCompareStatus = findViewById(R.id.text_compare_status);
        // 存图按钮
        saveCamera = findViewById(R.id.save_camera);
        saveCamera.setOnClickListener(this);
        spot = findViewById(R.id.spot);

        // ***************开发模式*************
        isRgbCheckImage = findViewById(R.id.is_check_image);
        isDepthCheckImage = findViewById(R.id.depth_is_check_image);
        // RGB 阈值
        rgbLiveScore = SingleBaseConfig.getBaseConfig().getRgbLiveScore();
        // Live 阈值
        depthLiveScore = SingleBaseConfig.getBaseConfig().getDepthLiveScore();
        // 送检RGB 图像回显
        mFaceDetectImageView = findViewById(R.id.face_detect_image_view);
        mFaceDetectImageView.setVisibility(View.VISIBLE);
        // 深度摄像头数据回显
        mDepthGLView = findViewById(R.id.depth_camera_preview_view);
        mDepthGLView.setVisibility(View.INVISIBLE);

        // 存在底库的数量
        mNum = findViewById(R.id.tv_num);
        mNum.setText(String.format(
                "底库 ： %s 个样本",
                FaceApi.getInstance().getmUserNum()
        ));
        // 检测耗时
        mTvDetect = findViewById(R.id.tv_detect_time);
        // RGB活体
        mTvLive = findViewById(R.id.tv_rgb_live_time);
        mTvLiveScore = findViewById(R.id.tv_rgb_live_score);
        // depth活体
        mTvDepth = findViewById(R.id.tv_depth_live_time);
        mTvDepthScore = findViewById(R.id.tv_depth_live_score);
        // 特征提取
        mTvFeature = findViewById(R.id.tv_feature_time);
        // 检索
        mTvAll = findViewById(R.id.tv_feature_search_time);
        // 总耗时
        mTvAllTime = findViewById(R.id.tv_all_time);


        // ***************预览模式*************
        // 已删除冗余的预览模式UI引用（activity_itme_gate 已删除）
        // textHuanying = findViewById(R.id.huanying_relative)
        // userNameLayout = findViewById(R.id.user_name_layout)
        // nameImage = findViewById(R.id.name_image)
        // nameText = findViewById(R.id.name_text)
        detectSurfaceText = findViewById(R.id.detect_surface_text);
        mFaceDetectImageView.setVisibility(View.GONE);
        saveCamera.setVisibility(View.GONE);
        detectSurfaceText.setVisibility(View.GONE);
        // 蒙层视图 - 开发模式下默认隐藏
        view = findViewById(R.id.mongolia_view);
        view.setAlpha(0.85f);
        view.setBackgroundColor(Color.parseColor("#ffffff"));
        view.setVisibility(View.GONE);  // 默认隐藏蒙层，避免界面偏白
        nirSurfaceText = findViewById(R.id.depth_surface_text);
        nirSurfaceText.setVisibility(View.GONE);


        glSurfaceView = findViewById(R.id.camera_textureview);
        // 修复：人脸框的镜像参数必须与视频预览的镜像参数一致
        // 否则会导致人脸框在水平方向上与实际人脸移动方向相反
        int mirrorVideoRGB = SingleBaseConfig.getBaseConfig().getMirrorVideoRGB();
        glSurfaceView.initSurface(
                mirrorVideoRGB == 1,  // 将 int 转换为 boolean
                mirrorVideoRGB,
                SingleBaseConfig.getBaseConfig().isOpenGl()
        );
        CameraPreviewManager.getInstance().startPreview(/*mContext, */glSurfaceView,
                SingleBaseConfig.getBaseConfig().getRgbVideoDirection(),
                RGB_WIDTH,
                RGB_HEIGHT
        );
    }

    /**
     * 在device 启动时候初始化USB 驱动
     *
     * @param device
     */
    private void initUsbDevice(UsbDevice device) {
        List<DeviceInfo> opennilist = OpenNI.enumerateDevices();
        android.util.Log.d("BdFaceDepthGate", "enumerateDevices: " + opennilist.size() + " devices");
        if (opennilist.size() <= 0) {
            showToast("openni enumerateDevices 0 devices");
            return;
        }
        // 不再显式设置为 null，保持现有值
        // Find mDevice ID
        for (int i = 0; i < opennilist.size(); i++) {
            android.util.Log.d("BdFaceDepthGate", "Device[" + i + "]: usbProductId=" + opennilist.get(i).getUsbProductId() + ", looking for " + device.getProductId());
            if (opennilist.get(i).getUsbProductId() == device.getProductId()) {
                android.util.Log.d("BdFaceDepthGate", "Found matching device, opening...");
                this.mDevice = Device.open();
                android.util.Log.d("BdFaceDepthGate", "Device.open() returned: " + mDevice);
                break;
            }
        }

        if (this.mDevice == null) {
            android.util.Log.e("BdFaceDepthGate", "mDevice is still null after initUsbDevice");
            showToast("openni open devices failed: " + device.getDeviceName());
            return;
        }
        android.util.Log.d("BdFaceDepthGate", "initUsbDevice succeeded, mDevice=" + mDevice);
    }

    /**
     * 摄像头图像预览
     */
    private void startCameraPreview() {
        // 设置USB摄像头
        if (SingleBaseConfig.getBaseConfig().getRBGCameraId() != -1) {
            CameraPreviewManager.getInstance().setCameraFacing(SingleBaseConfig.getBaseConfig().getRBGCameraId());
        } else {
            CameraPreviewManager.getInstance().setCameraFacing(CameraPreviewManager.CAMERA_USB);
        }
        int[] cameraSize = CameraPreviewManager.getInstance().initCamera();
        initFaceConfig(cameraSize[1], cameraSize[0]);

        CameraPreviewManager.getInstance().setmCameraDataCallback(new CameraDataCallback() {
            @Override
            public void onGetCameraData(byte[] data, Camera camera, int width, int height) {
                // 摄像头预览数据进行人脸检测
                dealRgb(data);
            }
        });
    }

    @Override
    public void onDeviceOpened(UsbDevice usbDevice) {
        initUsbDevice(usbDevice);
        mDepthStream = VideoStream.create(this.mDevice, SensorType.DEPTH);
        if (mDepthStream != null) {
            List<VideoMode> mVideoModes = mDepthStream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : mVideoModes) {
                int x = mode.getResolutionX();
                int y = mode.getResolutionY();
                int fps = mode.getFps();
                if (cameraType == 1) {
                    if (x == depthHeight && y == depthWidth && mode.getPixelFormat() == PixelFormat.DEPTH_1_MM) {
                        mDepthStream.setVideoMode(mode);
                        mDevice.setImageRegistrationMode(ImageRegistrationMode.DEPTH_TO_COLOR);
                        break;
                    }
                } else {
                    if (x == depthWidth && y == depthHeight && mode.getPixelFormat() == PixelFormat.DEPTH_1_MM) {
                        mDepthStream.setVideoMode(mode);
                        mDevice.setImageRegistrationMode(ImageRegistrationMode.DEPTH_TO_COLOR);
                        break;
                    }
                }
            }
            initDepthFaceConfig(depthHeight, depthWidth);
            startThread();
        }
    }

    /**
     * 开启线程接收深度数据
     */
    private void startThread() {
        initOk = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                List<VideoStream> streams = new ArrayList<>();

                streams.add(mDepthStream);
                try {
                    mDepthStream.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                while (!exit) {
                    try {
                        OpenNI.waitForAnyStream(streams, 2000);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                        continue;
                    }

                    synchronized (sync) {
                        if (mDepthStream != null) {
                            mDepthGLView.update(mDepthStream);
                            try {
                                VideoFrameRef videoFrameRef = mDepthStream.readFrame();
                                ByteBuffer depthByteBuf = videoFrameRef.getData();
                                if (depthByteBuf != null) {
                                    int depthLen = depthByteBuf.remaining();
                                    byte[] depthByte = new byte[depthLen];
                                    depthByteBuf.get(depthByte);
                                    dealDepth(depthByte);
                                }
                                videoFrameRef.release();
                            } catch (Exception e) {
                                LogUtil.e(TAG, "处理深度数据异常: " + e.getLocalizedMessage());
                            }
                        }
                    }
                }
            }
        });

        thread.start();
    }

    private void dealDepth(byte[] data) {
        // 修复: 避免非空断言，安全处理
        if (bdDepthFaceImageConfig != null) {
            bdDepthFaceImageConfig.setData(data);
            checkData();
        } else {
            LogUtil.w(TAG, "bdDepthFaceImageConfig 未初始化，跳过深度数据处理");
            return;
        }
    }

    /**
     * 处理RGB摄像头数据，进行人脸检测
     */
    private void dealRgb(byte[] data) {
        if (bdFaceImageConfig != null) {
            bdFaceImageConfig.setData(data);
            glSurfaceView.setFrame();
            checkData();
        }
    }

    private synchronized void checkData() {
        byte[] rgbData = bdFaceImageConfig != null ? bdFaceImageConfig.data : null;
        BDFaceImageConfig depthConfig = bdDepthFaceImageConfig;
        byte[] depthData = depthConfig != null ? depthConfig.data : null;

        if (rgbData != null && depthConfig != null && depthData != null) {
            android.util.Log.d(TAG, "checkData: 调用人脸检测，RGB数据长度=" + rgbData.length + ", Depth数据长度=" + depthData.length);
            FaceSDKManager.getInstance().onDetectCheck(
                    bdFaceImageConfig, null, bdDepthFaceImageConfig,
                    bdFaceCheckConfig, new FaceDetectCallBack() {
                        @Override
                        public void onFaceDetectCallback(LivenessModel livenessModel) {
                            android.util.Log.d(TAG, "onFaceDetectCallback: livenessModel=" + livenessModel);
                            // 预览模式已删除，不再调用checkCloseDebugResult
                            // checkCloseDebugResult(livenessModel);
                            // 开发模式
                            checkOpenDebugResult(livenessModel);
                            if (isSaveImage) {
                                // 检查 livenessModel 是否为 null，避免空指针异常
                                if (livenessModel != null) {
                                    SaveImageManager.getInstance().saveImage(mContext, livenessModel, bdLiveConfig,
                                            new SaveImageListener() {
                                                @Override
                                                public void onSaveSuccess(int savedCount, List<String> filePaths, String timestamp) {
                                                    // 检查Activity是否已销毁，避免内存泄漏
                                                    if (!isFinishing() && !isDestroyed()) {
                                                        LogUtil.i(TAG, "图片保存成功: 数量=" + savedCount + ", 时间戳=" + timestamp);
                                                        LogUtil.d(TAG, "保存的图片路径: " + filePaths);
                                                    }
                                                }

                                                @Override
                                                public void onSaveFailed(int errorCode, String errorMsg) {
                                                    if (!isFinishing() && !isDestroyed()) {
                                                        LogUtil.e(TAG, "图片保存失败: errorCode=" + errorCode + ", errorMsg=" + errorMsg);
                                                    }
                                                }

                                                @Override
                                                public void onConditionNotMet(float rgbScore, float depthScore, float threshold) {
                                                    if (!isFinishing() && !isDestroyed()) {
                                                        LogUtil.d(TAG, String.format("图片未保存: RGB(%.2f) Depth(%.2f) 阈值(%.2f)",
                                                                rgbScore, depthScore, threshold));
                                                    }
                                                }
                                            });
                                } else {
                                    LogUtil.w(TAG, "livenessModel 为 null，跳过保存图片");
                                }
                            }
                        }

                        @Override
                        public void onTip(int code, String msg) {
                            android.util.Log.d(TAG, "onTip: code=" + code + ", msg=" + msg);
                        }

                        @Override
                        public void onFaceDetectDarwCallback(LivenessModel livenessModel) {
                            showFrame(livenessModel);
                        }
                    });
        } else {
            android.util.Log.d(TAG, "checkData: 条件不满足，rgbData=" + (rgbData != null) + ", depthConfig=" + (depthConfig != null) + ", depthData=" + (depthData != null));
        }
    }

    // 已删除预览模式结果输出方法 checkCloseDebugResult() - 预览模式已移除

    // ***************开发模式结果输出*************
    private void checkOpenDebugResult(final LivenessModel livenessModel) {
        // 当未检测到人脸UI显示

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (livenessModel == null) {
                    layoutCompareStatus.setVisibility(View.GONE);
                    isRgbCheckImage.setVisibility(View.GONE);
                    isDepthCheckImage.setVisibility(View.GONE);
                    mFaceDetectImageView.setImageResource(R.mipmap.ic_image_video);
                    mTvDetect.setText(String.format("检测耗时 ：%s ms", 0));
                    mTvLive.setText(String.format("RGB活体检测耗时 ：%s ms", 0));
                    mTvLiveScore.setText(String.format("RGB活体得分 ：%s", 0));
                    mTvDepth.setText(String.format("Depth活体检测耗时 ：%s ms", 0));
                    mTvDepthScore.setText(String.format("Depth活体得分 ：%s", 0));
                    mTvFeature.setText(String.format("特征抽取耗时 ：%s ms", 0));
                    mTvAll.setText(String.format("特征比对耗时 ：%s ms", 0));
                    mTvAllTime.setText(String.format("总耗时 ：%s ms", 0));
                    return;
                }
                BDFaceImageInstance image = livenessModel.getBdFaceImageInstance();
                if (image != null) {
                    mFaceDetectImageView.setImageBitmap(BitmapUtils.getInstaceBmp(image));
                    image.destory();
                }

                float rgbLivenessScore = livenessModel.getRgbLivenessScore();
                if (rgbLivenessScore < rgbLiveScore) {
                    if (isCheck) {
                        isRgbCheckImage.setVisibility(View.VISIBLE);
                        isRgbCheckImage.setImageResource(R.mipmap.ic_icon_develop_fail);
                    }
                } else {
                    if (isCheck) {
                        isRgbCheckImage.setVisibility(View.VISIBLE);
                        isRgbCheckImage.setImageResource(R.mipmap.ic_icon_develop_success);
                    }
                }

                float depthLivenessScore = livenessModel.getDepthLivenessScore();
                if (depthLivenessScore < depthLiveScore) {
                    if (isCheck) {
                        isDepthCheckImage.setVisibility(View.VISIBLE);
                        isDepthCheckImage.setImageResource(R.mipmap.ic_icon_develop_fail);
                    }
                } else {
                    if (isCheck) {
                        isDepthCheckImage.setVisibility(View.VISIBLE);
                        isDepthCheckImage.setImageResource(R.mipmap.ic_icon_develop_success);
                    }
                }
                if (livenessModel.isQualityCheck()) {
                    if (isCompareCheck) {
                        layoutCompareStatus.setVisibility(View.VISIBLE);
                        textCompareStatus.setTextColor(Color.parseColor("#FFFEC133"));
                        //                                                textCompareStatus.setMaxEms(6);
                        textCompareStatus.setText("请正视摄像头");
                    }
                } else if (rgbLivenessScore < rgbLiveScore || depthLivenessScore < depthLiveScore) {
                    if (isCompareCheck) {
                        layoutCompareStatus.setVisibility(View.VISIBLE);
                        textCompareStatus.setTextColor(Color.parseColor("#FFFEC133"));

                        //                            textCompareStatus.setMaxEms(7);
                        textCompareStatus.setText("活体检测未通过");
                    }
                } else {
                    User user = livenessModel.getUser();
                    if (user == null) {
                        mUser = null;
                        if (isCompareCheck) {
                            if (livenessModel.isMultiFrame()) {
                                layoutCompareStatus.setVisibility(View.VISIBLE);
                                textCompareStatus.setTextColor(Color.parseColor("#FFFEC133"));
                                textCompareStatus.setText("识别未通过");
                            } else {
                                layoutCompareStatus.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        mUser = user;
                        if (isCompareCheck) {
                            layoutCompareStatus.setVisibility(View.VISIBLE);
                            textCompareStatus.setTextColor(Color.parseColor("#FF00BAF2"));

                            //                            textCompareStatus.setMaxEms(5);
                            textCompareStatus.setText(FileUtils.spotString(mUser.getUserName()));
                        }
                    }
                }

                mTvDetect.setText(
                        String.format("检测耗时 ：%s ms", livenessModel.getRgbDetectDuration()));
                mTvLive.setText(
                        String.format("RGB活体检测耗时 ：%s ms", livenessModel.getRgbLivenessDuration()));
                mTvLiveScore.setText(
                        String.format("RGB活体得分 ：%s", livenessModel.getRgbLivenessScore()));
                mTvDepth.setText(String.format(
                        "Depth活体检测耗时 ：%s ms",
                        livenessModel.getDepthtLivenessDuration()
                ));
                mTvDepthScore.setText(
                        String.format("Depth活体得分 ：%s", livenessModel.getDepthLivenessScore()));
                mTvFeature.setText(
                        String.format("特征抽取耗时 ：%s ms", livenessModel.getFeatureDuration()));
                mTvAll.setText(
                        String.format("特征比对耗时 ：%s ms", livenessModel.getCheckDuration()));
                mTvAllTime.setText(
                        String.format("总耗时 ：%s ms", livenessModel.getAllDetectDuration()));
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId(); // 返回
        if (id == R.id.btn_back) {
            if (!FaceSDKManager.initModelSuccess) {
                showToast("SDK正在加载模型，请稍后再试");
                return;
            }
            if (thread != null) {
                thread.interrupt();
            }
            finish();
            // 设置
        } else if (id == R.id.btn_setting) {
            if (!FaceSDKManager.initModelSuccess) {
                showToast("SDK正在加载模型，请稍后再试");
                return;
            }
            if (thread != null) {
                thread.interrupt();
            }
            // TODO 临时屏蔽，该页面 还没有 开始
//            startActivity(Intent(mContext, GateSettingActivity.class))
            finish();
        } else if (id == R.id.save_camera) {
//            isSaveImage = !isSaveImage
            // 临时屏蔽，先确认其修改
            isSaveImage = true;
            if (isSaveImage) {
                spot.setVisibility(View.VISIBLE);
                ToastUtils.toast(BdFaceDepthGateActivity.this, "存图功能已开启再次点击可关闭");
            } else {
                spot.setVisibility(View.GONE);
            }
        }
    }

    // 已删除未使用的方法: judgeFirst() 和 setFirstView()

    /**
     * 安全地显示 Toast，避免 Activity 销毁后崩溃
     */
    private void showToast(final String message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(BdFaceDepthGateActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 摄像头图像预览
        startCameraPreview();

        // 初始化 深度摄像头
        mOpenNIHelper = new OpenNIHelper(this);
        mOpenNIHelper.requestDeviceOpen(this);
    }

    /**
     * 释放深度摄像头资源
     * 修复: 提取公共方法，避免onPause和onDestroy中的代码重复
     */
    private void releaseDepthResources() {
        exit = true;
        if (initOk) {
            if (thread != null) {
                try {
                    // 先设置exit标志，再interrupt
                    exit = true;
                    thread.interrupt();
                    thread.join(1000);  // 等待最多1秒
                } catch (Exception e) {
                    LogUtil.e(TAG, "停止线程失败: " + e.getMessage());
                }
            }
            if (mDepthStream != null) {
                mDepthStream.stop();
                mDepthStream.destroy();
                mDepthStream = null;
            }
            if (mDevice != null) {
                mDevice.close();
                mDevice = null;
            }
        }
        if (mOpenNIHelper != null) {
            mOpenNIHelper.shutdown();
            mOpenNIHelper = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseDepthResources();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CameraPreviewManager.getInstance().stopPreview();
        releaseDepthResources();
    }

    @Override
    public void onDeviceOpenFailed(String msg) {
        LogUtil.e(TAG, "onDeviceOpenFailed, msg:" + msg);
        showAlertAndExit("Open Device failed: " + msg);
    }

    @Override
    public void onDeviceNotFound() {
        LogUtil.e(TAG, "onDeviceNotFound");
        showAlertAndExit("Open Device failed: NotFound");
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 权限请求回调处理，当前未实现具体逻辑
    }

    /**
     * 绘制人脸框
     */
    private void showFrame(LivenessModel model) {
        if (model == null) {
            return;
        }
        glSurfaceView.onGlDraw(
                model.getTrackFaceInfo(),
                model.getBdFaceImageInstance(),
                FaceOnDrawTexturViewUtil.drawFaceColor(mUser, model)
        );
    }

    private void showAlertAndExit(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setNeutralButton("OK", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }

    // 已删除预览模式的蒙层动画方法 objectAnimator() - 预览模式已移除
}
