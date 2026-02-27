package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.openni.OpenNI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class BaseOrbbecActivity extends BaseActivity {

    private boolean isFirstOpenOrbbecSDK = true;
    private static final String TAG = "BaseOrbbecActivity";
    private static boolean driversExtracted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFirstOpenOrbbecSDK) {
            initializeOpenNI();
            isFirstOpenOrbbecSDK = false;
        }
    }

    /**
     * 只需要初始化一次即可
     */
    public void initializeOpenNI() {
        try {
            // 提取 OpenNI 驱动文件到应用数据目录
            extractOpenNIDrivers();

            // 设置SDK Log 日志是否输出
            OpenNI.setLogAndroidOutput(true);
            // 设置Log日志输出级别
            OpenNI.setLogMinSeverity(0);
            // 初始化SDK
            OpenNI.initialize();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OpenNI", e);
        }
    }

    /**
     * 将 OpenNI 驱动文件从 APK 解压目录复制到应用数据目录
     * OpenNI SDK 从 /data/data/<package>/lib/ 查找驱动
     */
    private synchronized void extractOpenNIDrivers() {
        if (driversExtracted) {
            return;
        }

        try {
            Context context = getApplicationContext();
            // APK .so 文件解压后的位置
            String sourceLibPath = context.getApplicationInfo().nativeLibraryDir;
            // OpenNI SDK 查找驱动的位置
            String dataDirPath = context.getApplicationInfo().dataDir;
            String targetLibPath = dataDirPath + "/lib";

            Log.d(TAG, "Source lib path: " + sourceLibPath);
            Log.d(TAG, "Target lib path: " + targetLibPath);

            File targetDir = new File(targetLibPath);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // OpenNI 需要的驱动文件列表
            String[] driverFiles = {
                "libOniFile.so",
                "liborbbec.so",
                "liborbbecusb2.so",
                "libOpenNI2.so",
                "libOpenNI2.jni.so"
            };

            boolean allCopied = true;
            for (String driver : driverFiles) {
                File sourceFile = new File(sourceLibPath, driver);
                File targetFile = new File(targetLibPath, driver);

                if (!targetFile.exists()) {
                    if (sourceFile.exists()) {
                        copyFile(sourceFile, targetFile);
                        Log.d(TAG, "Copied driver: " + driver);
                    } else {
                        Log.w(TAG, "Source driver not found: " + sourceFile.getAbsolutePath());
                        allCopied = false;
                    }
                } else {
                    Log.d(TAG, "Driver already exists: " + driver);
                }
            }

            if (allCopied) {
                Log.d(TAG, "All OpenNI drivers extracted successfully");
                driversExtracted = true;
            } else {
                Log.w(TAG, "Some OpenNI drivers are missing");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting OpenNI drivers", e);
        }
    }

    /**
     * 复制文件
     */
    private void copyFile(File source, File target) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            targetChannel = new FileOutputStream(target).getChannel();
            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } finally {
            if (sourceChannel != null) {
                sourceChannel.close();
            }
            if (targetChannel != null) {
                targetChannel.close();
            }
        }
    }
}
