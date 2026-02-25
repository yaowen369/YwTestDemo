package ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.baidu.idl.main.facesdk.FaceAuth;
import com.baidu.idl.main.facesdk.FaceQueue;
import com.baidu.idl.main.facesdk.callback.Callback;
import com.baidu.idl.main.facesdk.model.BDFaceSDKCommon;
import com.baidu.idl.main.facesdk.statistic.PostDeviceInfo;
import com.baidu.idl.main.facesdk.utils.PreferencesUtil;
import com.baidu.liantian.ac.LH;
import com.baidu.vis.facecollect.license.AndroidLicenser;
import com.baidu.vis.facecollect.license.BDLicenseLocalInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.data.SituationData;
import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.util.BdFileUtils;
import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.util.BdHttpUtils;

public class BdFaceAuth {
    private static final String TAG = "BdFaceAuth";
    private FaceAuth faceAuth;
    private static String deviceID = "";
    private static String mIdFlag = "1";
    private static final int ALGORITHM_ID = 3;
    private static final String OFFLINE_KEY = "activate_offline_key";
    private static final String ONLINE_KEY = "activate_online_key";

    public BdFaceAuth() {
        faceAuth = new FaceAuth();
    }
    public void setActiveLog(BDFaceSDKCommon.BDFaceLogInfo logInfo, int isLog) {
        faceAuth.setActiveLog(logInfo, isLog);
    }

    public void setCoreConfigure(BDFaceSDKCommon.BDFaceCoreRunMode runMode, int coreNum) {
        faceAuth.setCoreConfigure(runMode, coreNum);
    }
    public String getDeviceId(Context context) {
        if ("".equals(deviceID)) {
            try {
                LH.init(context, false);
                Log.i("License-SDK", "Load liantian ac succeed");
                Pair<String, String> deviceId = LH.getId(context, mIdFlag);
                if (deviceId != null && deviceId.second != null) {
                    deviceID = ((String) deviceId.second).toUpperCase();
                }
            } catch (Exception var2) {
                var2.printStackTrace();
                Log.i("License-SDK", "Load liantian ac failed");
            }
        }

        return deviceID;
    }
    /*
    * 离线激活
    * */
    public void initLicenseOffLine(final Context context, final Callback callback) {
        if (context == null) {
            callback.onResponse(CodeDetail.CONTEXT_NOT_ERROR, "context为空");
            return;
        }
        if (TextUtils.isEmpty(getDeviceId(context))) {
            callback.onResponse(CodeDetail.DEVICES_ID_NOT_ERROR, "devicesId为空");
            return;
        }

        // 检查SD卡是否装载
        boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        String path;
        if (isSDPresent) {
            // 获取SD卡根目录
            File sdCardPath = Environment.getExternalStorageDirectory();
            assert sdCardPath != null;
            path = sdCardPath.getAbsolutePath();
            initLicenseOffLine(context, path, OFFLINE_KEY , callback);
            // 输出SD卡路径
            Log.d(TAG, "SdCard: " + path);

        } else {

            initLicenseOffLine(context, context.getCacheDir().getAbsolutePath() , OFFLINE_KEY , callback);
//            callback.onResponse(CodeDetail.FILE_NOT_ERROR, "sd卡目录不可用,请自行设置健全文件读取路径");
            Log.d(TAG, "not find SdCard");
        }

    }

    public void initLicenseOffLine(final Context context, String path, String preferencesKey , final Callback callback) {

        SituationData situationData = BdFileUtils.readAuthFile(context, path);
        if (situationData.getCode() == CodeDetail.SUCCESS) {
            initLicenseOffLine(context, situationData.getKey(), situationData.getValue(), preferencesKey , callback);
        } else {
            callback.onResponse(situationData.getCode(), situationData.getMassage());
        }
    }

    private void initLicenseOffLine(Context context, String key, String[] value, String preferencesKey, Callback callback) {
        AndroidLicenser licenser = AndroidLicenser.getInstance();

        AndroidLicenser.ErrorCode errorCode1 = licenser.authFromFile(context,
                key, "idl-license.face-android", false, ALGORITHM_ID);
        if (errorCode1 == AndroidLicenser.ErrorCode.SUCCESS){
            int status = faceAuth.createInstance();
            Log.v(TAG, "bdface_create_instance status " + status);
            PreferencesUtil.putString(preferencesKey , key);
            callback.onResponse(errorCode1.ordinal(), "");
            return;
        }

        AndroidLicenser.ErrorCode errorCode = licenser.authFromMemory(context,
                key, value, "idl-license.face-android", ALGORITHM_ID);
        if (errorCode != AndroidLicenser.ErrorCode.SUCCESS) {
            BDLicenseLocalInfo info = licenser.authGetLocalInfo(context, ALGORITHM_ID);
            if (info != null) {
                Log.i(TAG, info.toString());
            }
        }else {
            int status = faceAuth.createInstance();
            Log.v(TAG, "bdface_create_instance status " + status);
            PreferencesUtil.putString(preferencesKey , key);
        }
        String errMsgx = licenser.getErrorMsg(ALGORITHM_ID);
        callback.onResponse(errorCode.ordinal(), getErrorManager(errorCode) + " , " + errMsgx);
    }
    /*
    * 获取离线激活错误码
    * */
    private String getErrorManager(AndroidLicenser.ErrorCode errorCode){
        switch (errorCode){
            case SUCCESS:
                return "";
            case LICENSE_NOT_INIT_ERROR:
                return "license未初始化";
            case LICENSE_DECRYPT_ERROR:
                return "license数据解密失败";
            case LICENSE_INFO_FORMAT_ERROR:
                return "license数据格式错误";
            case LICENSE_KEY_CHECK_ERROR:
                return "license-key校验错误";
            case LICENSE_ALGORITHM_CHECK_ERROR:
                return "算法ID校验错误";
            case LICENSE_MD5_CHECK_ERROR:
                return "MD5校验错误";
            case LICENSE_DEVICE_ID_CHECK_ERROR:
                return "设备ID校验错误";
            case LICENSE_PACKAGE_NAME_CHECK_ERROR:
                return "包名校验错误";
            case LICENSE_EXPIRED_TIME_CHECK_ERROR:
                return "时间校验不通过";
            case LICENSE_FUNCTION_CHECK_ERROR:
                return "功能未授权";
            case LICENSE_TIME_EXPIRED:
                return "授权已过期";
            case LICENSE_LOCAL_FILE_ERROR:
                return "本地文件读取失败";
            case LICENSE_REMOTE_DATA_ERROR:
                return "远程数据拉取失败";
            case LICENSE_LOCAL_TIME_ERROR:
                return "本地时间校验错误";
            case LICENSE_PARAM_ERROR:
                return "参数错误";
            case LICENSE_KEY_FILE_ERROR:
                return "key错误";
        }
        return "其他错误";
    }

    /*
     * 在线激活
     * */
    public void initLicenseOnLine(final Context context, final String licenseID, final Callback callback) {

        Runnable runnable = new Runnable() {
            public void run() {
                if (context == null) {
                    callback.onResponse(CodeDetail.CONTEXT_NOT_ERROR, "context为空");
                    return;
                }
                if (TextUtils.isEmpty(getDeviceId(context))) {
                    callback.onResponse(CodeDetail.DEVICES_ID_NOT_ERROR, "devicesId为空");
                    return;
                }

                File iniFile = new File(context.getFilesDir() + File.separator + "license.ini");
                File keyFile = new File(context.getFilesDir() + File.separator + "license.key");
                if (iniFile.canRead() && keyFile.canRead() && keyFile.exists()
                        && iniFile.exists() && keyFile.isFile() && iniFile.isFile()) {
                    SituationData situationData = BdFileUtils.readAuthFile(context, keyFile, iniFile);
                    if (situationData.getCode() == CodeDetail.SUCCESS) {
                        if (situationData.getKey().equals(licenseID)) {
                            initLicenseOffLine(context, situationData.getKey(), situationData.getValue() , ONLINE_KEY , callback);
                            return;
                        }
                    }
                }
                iniFile.delete();
                keyFile.delete();
                SituationData situationData = BdHttpUtils.requestPost(licenseID, deviceID);
                if (situationData.getCode() == CodeDetail.SUCCESS) {
                    String response = situationData.getHttpResponse();
                    try {
                        JSONObject json = null;
                        json = new JSONObject(response);
                        int jsonErrorCode = json.optInt("error_code");
                        if (jsonErrorCode != 0) {
                            String errorMsg = json.optString("error_msg");
                            Log.i("FaceSDK", "error_msg->" + errorMsg);
                            callback.onResponse(jsonErrorCode, getHttpErrorManager(jsonErrorCode) + " , " + errorMsg);
                            return;
                        } else {
                            situationData.setCode(CodeDetail.HTTP_RESULT_ERROR);
                            JSONObject result = json.optJSONObject("result");
                            if (result == null) {
                                situationData.setMassage("http接口返回数据为空");
                                callback.onResponse(situationData.getCode(), situationData.getMassage());
                                return;
                            }
                            String license = result.optString("license");
                            if (TextUtils.isEmpty(license)) {
                                situationData.setMassage("http鉴权文件为空");
                                callback.onResponse(situationData.getCode(), situationData.getMassage());
                                return;
                            }
                            String[] licenses = license.split(",");
                            if (licenses.length != 2) {
                                situationData.setMassage("http鉴权数据长度错误");
                                callback.onResponse(situationData.getCode(), situationData.getMassage());
                                return;
                            }
                            BdFileUtils.saveAuthFile(licenseID, licenses, context.getFilesDir().getAbsolutePath());
                            initLicenseOffLine(context, licenseID, licenses, ONLINE_KEY, callback);
                            return;
                        }
                    } catch (JSONException e) {
                        situationData.setCode(CodeDetail.JSON_CREATE_ERROR);
                        situationData.setMassage("网络数据解析异常");
                    }
                }
                callback.onResponse(situationData.getCode(), situationData.getMassage());
            }
        };
        FaceQueue.getInstance().execute(runnable);
    }


    /*
     * 获取在线激活错误码
     * */
    private String getHttpErrorManager(int errorCode){
        switch (errorCode){
            case CodeLicenseDetail.LOGIC_INTERNAL_ERROR:
                return "未知错误";
            case CodeLicenseDetail.INVALID_PARAM:
                return "非法参数";
            case CodeLicenseDetail.SERVICE_NOT_SUPPORT:
                return "不支持的状态类型";
            case CodeLicenseDetail.NOT_ENOUGH_PARAM:
                return "请求参数不足";
            case CodeLicenseDetail.NO_AUTH_TO_OPERATE:
                return "无操作权限";
            case CodeLicenseDetail.KEY_GENERATE_ERROR:
                return "序列号生成错误";
            case CodeLicenseDetail.KEY_INVALID:
                return "序列号错误";
            case CodeLicenseDetail.DEVICEID_NOT_CORRECT:
                return "设备标识错误";
            case CodeLicenseDetail.LICENSE_HAS_ACTIVED_ON_OTHER_DEVICE:
                return "license已在其它设备激活";
            case CodeLicenseDetail.LICENSE_GENERATE_ERROR:
                return "license生成失败";
            case CodeLicenseDetail.LICENSE_UPDATE_TIME_ERROR:
                return "license过期时间设置错误，必须比当前过期时间短";
            case CodeLicenseDetail.LICENSE_UPDATE_FAILED:
                return "license更新失败";
            case CodeLicenseDetail.LICENSE_TIMES_HAS_REACHED_UPPER_LIMIT:
                return "license下载次数已超过上限";
            case CodeLicenseDetail.LICENSE_BIND_ON_OTHER_DEVICE:
                return "license已绑定其它设备";
            case CodeLicenseDetail.LICENSE_DEVICE_BIND_ERROR:
                return "license设备绑定失败";
            case CodeLicenseDetail.LICENSE_QUERY_ERROR:
                return "license查询失败";
        }
        return "其他错误";
    }
    /*
    * 批量激活
    * */
    public void initLicenseBatchLine(final Context context, final String licenseKey, final Callback callback){
        if (context == null) {
            callback.onResponse(CodeDetail.CONTEXT_NOT_ERROR, "context为空");
            return;
        }
        if (TextUtils.isEmpty(getDeviceId(context))) {
            callback.onResponse(CodeDetail.DEVICES_ID_NOT_ERROR, "devicesId为空");
            return;
        }
//        faceAuth.initLicenseBatchLine(context , licenseKey , callback);
        Runnable runnable = new Runnable() {
            public void run() {
                if (context == null) {
                    callback.onResponse(1, "没有初始化上下文");
                } else {
                    PreferencesUtil.initPrefs(context);
                    String statics = PreferencesUtil.getString("statics", "");
                    if (TextUtils.isEmpty(statics)) {
                        PostDeviceInfo.uploadDeviceInfo(context, new Callback() {
                            public void onResponse(int code, String response) {
                                if (code == 0) {
                                    PreferencesUtil.putString("statics", "ok");
                                }

                            }
                        });
                    }

                    if (!TextUtils.isEmpty(licenseKey) && !TextUtils.isEmpty("idl-license.face-android")) {
                        AndroidLicenser licenser = AndroidLicenser.getInstance();
                        AndroidLicenser.ErrorCode errorCode = licenser.authFromFile(context, licenseKey, "idl-license.face-android", true, ALGORITHM_ID);
                        if (errorCode != AndroidLicenser.ErrorCode.SUCCESS) {
                            BDLicenseLocalInfo info = licenser.authGetLocalInfo(context, ALGORITHM_ID);
                            if (info != null) {
                                Log.i("FaceSDK", info.toString());
                            }
                        }else {
                            faceAuth.createInstance();
                        }
                        String errMsg = licenser.getErrorMsg(ALGORITHM_ID);
                        callback.onResponse(errorCode.ordinal(), getErrorManager(errorCode) + " , " +errMsg);
                    } else {
                        callback.onResponse(2, "license 关键字为空");
                    }
                }

            }
        };
        FaceQueue.getInstance().execute(runnable);
    }
}
