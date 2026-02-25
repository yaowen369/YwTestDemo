package ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.util;

import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.CodeDetail;
import ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.data.SituationData;

public class BdHttpUtils {
    private static final String TAG = "BdHttpUtils";
    public static SituationData requestPost(String licenseID , String deviceID){
        OutputStream outputStream = null;
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        String resultData = null;
        SituationData situationData = new SituationData();
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("deviceId", deviceID);
            jsonObject.put("key", licenseID);
            jsonObject.put("platformType", 2);
            jsonObject.put("version", 5);
            String paramStr = jsonObject.toString();
            byte[] postDataBytes = paramStr.getBytes(StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) (new URL("https://ai.baidu.com/activation/key/activate")).openConnection();
            System.setProperty("sun.net.client.defaultConnectTimeout", "8000");
            System.setProperty("sun.net.client.defaultReadTimeout", "8000");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();
            outputStream = conn.getOutputStream();
            outputStream.write(postDataBytes);
            outputStream.flush();
            outputStream.close();
            int responseCode = conn.getResponseCode();
            Log.e(TAG, "responseCode: " + responseCode);
            if (HttpURLConnection.HTTP_OK == responseCode) {
                inputStream = conn.getInputStream();
                byte[] buffer = new byte[1024];
                baos = new ByteArrayOutputStream();
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }

                byte[] b = baos.toByteArray();
                resultData = new String(b, "utf-8");
                baos.flush();
                Log.e(TAG, "resultData: " + resultData);
                situationData.setCode(CodeDetail.SUCCESS);
                situationData.setHttpResponse(resultData);
                return situationData;
            }

            situationData.setCode(CodeDetail.HTTP_ERROR_ERROR);
            situationData.setMassage("http请求失败 错误 responseCode :" + getHttpErrorManager(responseCode));
        } catch (IOException e) {
            situationData.setCode(CodeDetail.WIFI_ERROR);
            situationData.setMassage("网络链接异常，请确保设备已联网且已开启网络权限:" + e.getMessage());
        } catch (JSONException e) {
            situationData.setCode(CodeDetail.JSON_CREATE_ERROR);
            situationData.setMassage("json异常:" + e.getMessage());
        }
        return situationData;
    }

    public static String getHttpErrorManager(int code){
        if (code == HttpURLConnection.HTTP_ACCEPTED){
            return "服务器已接受请求，但尚未处理。";
        }else if (code == HttpURLConnection.HTTP_MOVED_PERM){
            return "资源已被永久移动到其他URL。";
        }else if (code == HttpURLConnection.HTTP_MOVED_TEMP){
            return "资源临时被移动到其他URL。";
        }else if (code == HttpURLConnection.HTTP_BAD_REQUEST){
            return "客户端请求的语法错误，服务器无法理解。";
        }else if (code == HttpURLConnection.HTTP_UNAUTHORIZED){
            return "请求需要验证。";
        }else if (code == HttpURLConnection.HTTP_FORBIDDEN){
            return "服务器拒绝请求。请检查权限是否开启";
        }else if (code == HttpURLConnection.HTTP_NOT_FOUND){
            return "服务器无法找到请求的资源。。";
        }else if (code == HttpURLConnection.HTTP_BAD_METHOD){
            return "请求行中指定的请求方法不能被用于请求相应的资源。";
        }else if (code == HttpURLConnection.HTTP_CLIENT_TIMEOUT){
            return "客户端没有在服务器预备等待的时间内完成请求。";
        }else if (code == HttpURLConnection.HTTP_CONFLICT){
            return "发出的请求在资源的当前状态下会导致冲突。";
        }else if (code == HttpURLConnection.HTTP_GONE){
            return "请求的资源已被永久删除。";
        }else if (code == HttpURLConnection.HTTP_UNAVAILABLE){
            return "服务器暂时无法处理请求。";
        }
        return "";
    }
}
