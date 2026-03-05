package ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity;

import android.content.Context;
import android.text.TextUtils;

import com.baidu.idl.main.facesdk.FaceAuth;
import com.baidu.idl.main.facesdk.callback.Callback;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.utils.PreferencesUtil;

import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.BdFaceAuth;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SdkInitListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.ToastUtils;
import ywdemo.example.yaoxiaowen.until.LogUtil;


public class FaceSDKManager {

    private static final String TAG = "FaceSDKManager";

    public static final int SDK_MODEL_LOAD_SUCCESS = 0;
    public static final int SDK_UNACTIVATION = 1;
    public static final int SDK_UNINIT = 2;
    public static final int SDK_INITING = 3;
    public static final int SDK_INITED = 4;
    public static final int SDK_INIT_FAIL = 5;
    public static final int SDK_INIT_SUCCESS = 6;

    public static volatile int initStatus = SDK_UNACTIVATION;
    private BdFaceAuth bdFaceAuth;

    // 激活码 (杭州 这台设备的激活码)
      String activateCode = "XFRK-FQKW-AJMQ-RFHK";

    // 这是天津那台设备的激活码
//    String activateCode = "XALX-FRXM-JYWX-7SX6";



    private FaceSDKManager() {
        bdFaceAuth = new BdFaceAuth();
        bdFaceAuth.setActiveLog(BDFaceSDKCommon.BDFaceLogInfo.BDFACE_LOG_TYPE_ALL, 1);
        bdFaceAuth.setCoreConfigure(BDFaceSDKCommon.BDFaceCoreRunMode.BDFACE_LITE_POWER_NO_BIND, 2);

    }

    private static class HolderClass {
        private static final FaceSDKManager instance = new FaceSDKManager();
    }

    public static FaceSDKManager getInstance() {
        return HolderClass.instance;
    }

    /**
     * 初始化鉴权，如果鉴权通过，直接初始化模型
     *
     * @param context
     * @param listener
     */
    public void init(final Context context, final SdkInitListener listener) {

        PreferencesUtil.initPrefs(context.getApplicationContext());

        /**
         * 设备指纹：因为该台pad上，已经被其他App激活了，所以另一个App可以使用已经激活的设备指纹
         * 参考： https://ai.baidu.com/ai-doc/FACE/Zk37c1nnn#33-%E7%9B%B8%E5%90%8C%E8%AE%BE%E5%A4%87%E4%B8%8D%E5%90%8C%E5%BA%94%E7%94%A8%E8%83%BD%E5%90%A6%E4%BD%BF%E7%94%A8%E7%9B%B8%E5%90%8C%E6%BF%80%E6%B4%BB%E7%A0%81
         * eg：15475FF1E9E07FF582C3568089751322A8
         */
        String deviceFingerprint = new FaceAuth().getDeviceId(context);


        /**
         * 对于离线激活，实际测试，不管是 设备指纹，还是激活码，发现都无法激活。
         * 报错: code:1005, response=未找到授权文件,请将文件放到/storage/emulated/0目录
         */
        final String licenseOfflineKey = PreferencesUtil.getString("activate_offline_key", deviceFingerprint);

        /**
         * 对于 在线激活，实际验证发现，使用 激活码是可以的。
         * 但是 使用设备指纹，则报错提示: 290002  序列号错误 , key invalid
         * 所以我们 先使用 激活码 来进行下一步的操作.
         * （虽然这样不符合实际业务场景，因为我们作为三方App, 不可能直接写入激活码, 所以应该使用设备指纹进行激活）
         */
        final String licenseOnlineKey = PreferencesUtil.getString("activate_online_key", activateCode);

        final String licenseBatchlineKey = PreferencesUtil.getString("activate_batchline_key", "");


        LogUtil.i(TAG, "licenseOnlineKey: " + licenseOnlineKey + ", licenseOfflineKey:" + licenseOfflineKey + ", licenseBatchlineKey:" + licenseBatchlineKey);


        // 如果licenseKey 不存在提示授权码为空，并跳转授权页面授权
//        if (TextUtils.isEmpty(licenseOfflineKey) && TextUtils.isEmpty(licenseOnlineKey)
//                && TextUtils.isEmpty(licenseBatchlineKey)) {
//            LogUtil.i(TAG, "三个内容都为null，直接回调失败 ");
//            ToastUtils.toast(context, "未授权设备，请完成授权激活");
//            if (listener != null) {
//                listener.initLicenseFail(-1, "授权码不存在，请重新输入！");
//            }
//            return;
//        }
//        // todo 增加判空处理
//        if (listener != null) {
//            listener.initStart();
//        }


        bdFaceAuth.initLicenseOffLine(context, new Callback() {
            @Override
            public void onResponse(int code, String response) {
                LogUtil.i(TAG, "离线激活流程, code:" + code + ", response=" + response);
                if (code == 0) {
                    initStatus = SDK_INIT_SUCCESS;
                    if (listener != null) {
                        listener.initLicenseSuccess();
                    }
//                        initModel(context, listener);
                    return;
                } else {
//                    listener.initLicenseFail(code, response);
                    LogUtil.i(TAG, "离线激活流程 失败, 走下一个流程");

                    deviceFingerActive(context, deviceFingerprint, listener);

                }
            }
        });

    }


    private void deviceFingerActive(final Context context, String deviceFinger, final SdkInitListener listener) {
        // 在线激活 设备指纹
        bdFaceAuth.initLicenseOnLine(context, deviceFinger, new Callback() {
            @Override
            public void onResponse(int code, String response) {
                LogUtil.i(TAG, "在线激活流程, 设备指纹， code:" + code + ", response=" + response);
                if (code == 0) {
                    initStatus = SDK_INIT_SUCCESS;
                    if (listener != null) {
                        listener.initLicenseSuccess();
                    }
//                        initModel(context, listener);
                    return;
                } else {
                    LogUtil.i(TAG, "在线激活流程, 设备指纹 激活失败，走下一个流程 ");
//                    listener.initLicenseFail(code, response);
                    onlineActive(context, activateCode, listener);
                }
            }
        });
    }

    private void onlineActive(final Context context, String activeCode, final SdkInitListener listener) {
        LogUtil.i(TAG, "在线激活流程, 激活码为:" + activeCode);
        // 在线激活
        bdFaceAuth.initLicenseOnLine(context, activeCode, new Callback() {
            @Override
            public void onResponse(int code, String response) {
                LogUtil.i(TAG, "在线激活流程, 激活码， code:" + code + ", response=" + response);
                if (code == 0) {
                    LogUtil.i(TAG, "在线激活流程, >>>>> 激活成功， activeCode=" + activeCode);
                    initStatus = SDK_INIT_SUCCESS;
                    if (listener != null) {
                        listener.initLicenseSuccess();
                    }
//                        initModel(context, listener);
                    return;
                } else {
                    LogUtil.i(TAG, "在线激活流程, 激活失败， activeCode=" + activeCode);
                    listener.initLicenseFail(code, response);
                }
            }
        });
    }
}