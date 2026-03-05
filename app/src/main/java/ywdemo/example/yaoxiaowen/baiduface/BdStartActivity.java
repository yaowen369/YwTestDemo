package ywdemo.example.yaoxiaowen.baiduface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ywdemo.example.yaoxiaowen.R;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.api.FaceApi;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.DBLoadListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.User;
import ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.utils.GateConfigUtils;
import ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager;
import ywdemo.example.yaoxiaowen.baiduface.registerlibrary.idl.main.facesdk.registerlibrary.user.utils.RegisterConfigUtils;
import ywdemo.example.yaoxiaowen.until.LogUtil;

public class BdStartActivity extends Activity implements View.OnClickListener {

    // 常量定义，避免硬编码魔法数字
    private static final int SMALL_DATA_THRESHOLD = 5000;
    private static final long PROGRESS_UPDATE_DELAY = 10L;
    private static final long NAVIGATION_DELAY = 1500L;
    private static final int PROGRESS_STEP = 100;

    private static final String TAG = "BdStartActivity";

    private Context mContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService;

    // UI组件
    private Button btnStart;
    private TextView displayTv;
    private View progressGroup;
    private ProgressBar progressBar;
    private TextView progressText;

    // 状态管理
    private enum LoadingState {
        IDLE,               // 初始状态
        LICENSING,          // License激活中
        LICENSE_FAILED,     // License失败
        DB_LOADING,         // 人脸库加载中
        DB_SUCCESS,         // 人脸库成功
        DB_FAILED           // 人脸库失败
    }

    private LoadingState currentState = LoadingState.IDLE;

    // 人脸库加载状态
    private boolean isDBLoad = false;
    private String lastErrorMsg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bd_start);
        mContext = this;

        // 初始化UI组件
        btnStart = findViewById(R.id.btn_start);
        displayTv = findViewById(R.id.display_tv);
        progressGroup = findViewById(R.id.progress_group);
        progressBar = findViewById(R.id.progress_bar);
        progressText = findViewById(R.id.progress_text);

        // 初始化线程池
        executorService = Executors.newSingleThreadExecutor();

        // 初始化配置
        initConfig();

        // 设置按钮点击监听
        btnStart.setOnClickListener(this);

        // 开始License激活
        initLicense();
    }

    /**
     * 初始化配置文件
     */
    private void initConfig() {
        boolean isConfigExit = GateConfigUtils.isConfigExit(this);
        boolean isInitConfig = GateConfigUtils.initConfig();
        boolean isRegisterConfigExit = RegisterConfigUtils.isConfigExit(this);
        boolean isRegisterInitConfig = RegisterConfigUtils.initConfig();

        boolean gateConfigValid = isInitConfig && isConfigExit;
        boolean registerConfigValid = isRegisterInitConfig && isRegisterConfigExit;

        if (gateConfigValid && registerConfigValid) {
            LogUtil.i(TAG, "初始配置加载成功");
        } else {
            LogUtil.w(TAG, "初始配置失败,将重置文件内容为默认配置");
            GateConfigUtils.modityJson();
            RegisterConfigUtils.modityJson();
        }
    }

    /**
     * License激活
     */
    private void initLicense() {
        updateState(LoadingState.LICENSING);

        FaceSDKManager.getInstance().init(mContext, new SdkInitListener() {
            @Override
            public void initStart() {
                LogUtil.i(TAG, "initStart()");
            }

            @Override
            public void initLicenseSuccess() {
                LogUtil.i(TAG, "License激活成功");
                // 修复: 回调在后台线程，需要切换到主线程更新UI
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            updateState(LoadingState.DB_LOADING);
                            // 开始加载人脸库
                            loadFaceLibrary();
                        }
                    }
                });
            }

            @Override
            public void initLicenseFail(int errorCode, String msg) {
                LogUtil.e(TAG, "License激活失败: errorCode=" + errorCode + ", msg=" + msg);
                lastErrorMsg = "License激活失败\ncode: " + errorCode + "\n" + msg;
                // 修复: 回调在后台线程，需要切换到主线程更新UI
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            updateState(LoadingState.LICENSE_FAILED);
                        }
                    }
                });
            }

            @Override
            public void initModelSuccess() {
                LogUtil.i(TAG, "initModelSuccess()");
            }

            @Override
            public void initModelFail(int errorCode, String msg) {
                LogUtil.e(TAG, "initModelFail() -> errorCode:" + errorCode + ", msg:" + msg);
            }
        });
    }

    /**
     * 加载人脸库（使用线程池在后台执行）
     */
    private void loadFaceLibrary() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FaceApi.getInstance().init(new DBLoadListener() {
                        @Override
                        public void onStart(int successCount) {
                            // 小数据量时使用模拟进度
                            if (successCount < SMALL_DATA_THRESHOLD && successCount != 0) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isFinishing() && !isDestroyed()) {
                                            loadProgress(PROGRESS_STEP);
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onLoad(int finishCount, int successCount, float progress) {
                            // 大数据量时显示真实进度
                            if (successCount > SMALL_DATA_THRESHOLD || successCount == 0) {
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!isFinishing() && !isDestroyed()) {
                                            int progressInt = (int) (progress * 100);
                                            progressBar.setProgress(progressInt);
                                            progressText.setText(progressInt + "%");
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onComplete(List<User> users, int successCount) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFinishing() && !isDestroyed()) {
                                        FaceApi.getInstance().setUsers(users);
                                        isDBLoad = true;
                                        updateState(LoadingState.DB_SUCCESS);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onFail(int finishCount, int successCount, List<User> users) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFinishing() && !isDestroyed()) {
                                        FaceApi.getInstance().setUsers(users);
                                        lastErrorMsg = "人脸库加载失败\n共: " + successCount + " 条\n已加载: " + finishCount + " 条";
                                        updateState(LoadingState.DB_FAILED);
                                    }
                                }
                            });
                        }
                    }, BdStartActivity.this);
                } catch (Exception e) {
                    // 修复: LogUtil.e没有接受Throwable的重载，只记录消息
                    LogUtil.e(TAG, "人脸库加载异常: " + e.getMessage());
                    lastErrorMsg = "人脸库加载异常: " + e.getMessage();
                    updateState(LoadingState.DB_FAILED);
                }
            }
        });
    }

    /**
     * 模拟进度显示（小数据量时使用）
     */
    private void loadProgress(final int i) {
        if (currentState != LoadingState.DB_LOADING) return;

        // 修复: 使用mainHandler而不是new Handler，避免内存泄漏
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    int progress = (int) ((i / (float) SMALL_DATA_THRESHOLD) * 100);
                    progressBar.setProgress(progress);
                    progressText.setText(progress + "%");

                    if (i < SMALL_DATA_THRESHOLD) {
                        loadProgress(i + PROGRESS_STEP);
                    } else {
                        isDBLoad = true;
                        updateState(LoadingState.DB_SUCCESS);
                    }
                }
            }
        }, PROGRESS_UPDATE_DELAY);
    }

    /**
     * 更新UI状态
     */
    private void updateState(LoadingState state) {
        currentState = state;
        if (isFinishing() || isDestroyed()) return;

        switch (state) {
            case IDLE:
                displayTv.setText("准备初始化...");
                displayTv.setTextColor(Color.BLACK);
                progressGroup.setVisibility(View.GONE);
                btnStart.setVisibility(View.GONE);
                break;
            case LICENSING:
                displayTv.setText("正在激活License...");
                displayTv.setTextColor(Color.BLUE);
                progressGroup.setVisibility(View.GONE);
                btnStart.setVisibility(View.GONE);
                break;
            case LICENSE_FAILED:
                displayTv.setText(lastErrorMsg != null ? lastErrorMsg : "License激活失败");
                displayTv.setTextColor(Color.RED);
                progressGroup.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                btnStart.setText("重试激活");
                break;
            case DB_LOADING:
                displayTv.setText("正在加载人脸库...");
                displayTv.setTextColor(Color.BLUE);
                progressGroup.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                progressText.setText("0%");
                btnStart.setVisibility(View.GONE);
                break;
            case DB_SUCCESS:
                displayTv.setText("初始化完成，即将跳转...");
                displayTv.setTextColor(Color.GREEN);
                progressGroup.setVisibility(View.GONE);
                btnStart.setVisibility(View.GONE);

                // 延迟后跳转
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && !isDestroyed()) {
                            startActivity(new Intent(mContext, BdFaceDepthGateActivity.class));
                            finish();
                        }
                    }
                }, NAVIGATION_DELAY);
                break;
            case DB_FAILED:
                displayTv.setText(lastErrorMsg != null ? lastErrorMsg : "人脸库加载失败");
                displayTv.setTextColor(Color.RED);
                progressGroup.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
                btnStart.setText("重试加载");
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_start) {
            switch (currentState) {
                case LICENSE_FAILED:
                    // 重试License激活
                    initLicense();
                    break;
                case DB_FAILED:
                    // 重试人脸库加载
                    updateState(LoadingState.DB_LOADING);
                    loadFaceLibrary();
                    break;
                default:
                    LogUtil.w(TAG, "当前状态不允许重试: " + currentState);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 修复: 关闭线程池，避免内存泄漏
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        // 移除所有回调
        mainHandler.removeCallbacksAndMessages(null);
    }
}
