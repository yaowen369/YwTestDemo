package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.baidu.idl.main.facesdk.model.BDFaceImageInstance;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.listener.SaveImageListener;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.BDLiveConfig;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.model.LivenessModel;
import ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.utils.BitmapUtils;
import ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.model.SingleBaseConfig;
import ywdemo.example.yaoxiaowen.until.LogUtil;

public class SaveImageManager {
    public static final String TAG = "SaveImageManager";

    private static class HolderClass {
        private static final SaveImageManager SAVE_IMAGE_MANAGER = new SaveImageManager();
    }

    public static SaveImageManager getInstance() {
        return HolderClass.SAVE_IMAGE_MANAGER;
    }

    private String formatCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private ExecutorService es3 = Executors.newSingleThreadExecutor();
    private Future future3;

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 保存图片（带回调）
     */
    public void saveImage(final Context context, final LivenessModel livenessModel,
                         final BDLiveConfig bdLiveConfig, final SaveImageListener listener) {
        if (future3 != null && !future3.isDone()) {
            if (listener != null) {
                notifySaveFailed(listener, -1, "已有保存任务正在进行");
            }
            return;
        }

        future3 = es3.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    float saveThreshold = SingleBaseConfig.getBaseConfig().getSaveImageThreshold();
                    float rgbScore = livenessModel.getRgbLivenessScore();
                    float depthScore = livenessModel.getDepthLivenessScore();

                    if (rgbScore <= saveThreshold || depthScore <= saveThreshold) {
                        LogUtil.i(TAG, String.format("图片未保存：RGB(%.2f) Depth(%.2f) 阈值(%.2f)",
                                rgbScore, depthScore, saveThreshold));
                        if (listener != null) {
                            notifyConditionNotMet(listener, rgbScore, depthScore, saveThreshold);
                        }
                        return;
                    }

                    final String formattedTime = formatCurrentTime();
                    final List<String> savedFilePaths = new ArrayList<>();

                    BDFaceImageInstance rgbImage = livenessModel.getBdFaceImageInstance();
                    BDFaceImageInstance nirImage = livenessModel.getBdNirFaceImageInstance();
                    BDFaceImageInstance depthImage = livenessModel.getBdDepthFaceImageInstance();

                    int savedCount = 0;

                    // 保存NIR图像
                    if (nirImage != null) {
                        Bitmap bitmap = null;
                        try {
                            bitmap = BitmapUtils.getInstaceBmp(nirImage);
                            String suffix = livenessModel.getIrLivenessScore() > bdLiveConfig.nirLiveScore
                                    ? "_NIR_Feature"
                                    : "_NIR_Live";
                            String filePath = saveImageAndReturnPath(context, bitmap,
                                    "Save-Image" + "/" + formattedTime, formattedTime + suffix);
                            if (filePath != null) {
                                savedFilePaths.add(filePath);
                            }
                        } finally {
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                            nirImage.destory();
                        }
                        savedCount++;
                    }

                    // 保存Depth图像
                    if (depthImage != null) {
                        Bitmap bitmap = null;
                        try {
                            bitmap = BitmapUtils.getInstaceBmp(depthImage);
                            String suffix = livenessModel.getDepthLivenessScore() > bdLiveConfig.depthLiveScore
                                    ? "_Depth_Feature"
                                    : "_Depth_Live";
                            String filePath = saveImageAndReturnPath(context, bitmap,
                                    "Save-Image" + "/" + formattedTime, formattedTime + suffix);
                            if (filePath != null) {
                                savedFilePaths.add(filePath);
                            }
                        } finally {
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                            depthImage.destory();
                        }
                        savedCount++;
                    }

                    // 保存RGB图像
                    if (rgbImage != null) {
                        Bitmap bitmap = null;
                        try {
                            bitmap = BitmapUtils.getInstaceBmp(rgbImage);
                            String suffix = livenessModel.getRgbLivenessScore() > bdLiveConfig.rgbLiveScore
                                    ? "_RGB_Feature"
                                    : "_RGB_Live";
                            String filePath = saveImageAndReturnPath(context, bitmap,
                                    "Save-Image" + "/" + formattedTime, formattedTime + suffix);
                            if (filePath != null) {
                                savedFilePaths.add(filePath);
                            }
                        } finally {
                            if (bitmap != null && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                            rgbImage.destory();
                        }
                        savedCount++;
                    }

                    // 显示保存成功提示
                    final int finalSavedCount = savedCount;
                    showToast(context, String.format("图片已保存：%d张 (%s)", finalSavedCount, formattedTime));

                    // 回调通知保存成功
                    if (listener != null) {
                        notifySaveSuccess(listener, finalSavedCount, savedFilePaths, formattedTime);
                    }
                } catch (Exception e) {
                    LogUtil.e(TAG, "保存图片异常: " + e.getMessage());
                    e.printStackTrace();
                    if (listener != null) {
                        notifySaveFailed(listener, -2, "保存图片异常: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 保存图片（不带回调，保持向后兼容）
     */
    public void saveImage(Context context, LivenessModel livenessModel, BDLiveConfig bdLiveConfig) {
        saveImage(context, livenessModel, bdLiveConfig, null);
    }

    /**
     * 在主线程显示Toast
     * 使用 ApplicationContext 避免内存泄漏
     */
    private void showToast(final Context context, final String message) {
        if (context == null) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImage(Context context, Bitmap bitmap, String url, String name) {
        BitmapUtils.saveRgbBitmap(context, bitmap, url, name);
    }

    /**
     * 保存图片并返回文件路径
     */
    private String saveImageAndReturnPath(Context context, Bitmap bitmap, String dirName, String fileName) {
        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath();
            File dir = new File(path + "/" + dirName);
            dir.mkdirs();
            File pngFile = new File(dir, fileName + ".png");
            saveImage(context, bitmap, dirName, fileName);
            return pngFile.getAbsolutePath();
        } catch (Exception e) {
            LogUtil.e(TAG, "保存图片并获取路径失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void notifySaveSuccess(final SaveImageListener listener, final int savedCount,
                                   final List<String> filePaths, final String timestamp) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onSaveSuccess(savedCount, filePaths, timestamp);
            }
        });
    }

    private void notifySaveFailed(final SaveImageListener listener, final int errorCode, final String errorMsg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onSaveFailed(errorCode, errorMsg);
            }
        });
    }

    private void notifyConditionNotMet(final SaveImageListener listener, final float rgbScore,
                                       final float depthScore, final float threshold) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onConditionNotMet(rgbScore, depthScore, threshold);
            }
        });
    }
}
