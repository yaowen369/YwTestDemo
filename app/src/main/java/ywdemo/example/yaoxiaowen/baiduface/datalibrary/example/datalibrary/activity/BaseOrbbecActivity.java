package ywdemo.example.yaoxiaowen.baiduface.datalibrary.example.datalibrary.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.openni.OpenNI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BaseOrbbecActivity extends BaseActivity {

    private static final String TAG = "BaseOrbbecActivity";
    private boolean isFirstOpenOrbbecSDK = true;
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
            Log.d(TAG, "Starting OpenNI initialization...");

            // 首先提取驱动文件（必须在 OpenNI.initialize() 之前完成）
            extractOpenNIDrivers();

            // 设置SDK Log 日志是否输出
            OpenNI.setLogAndroidOutput(true);
            // 设置Log日志输出级别
            OpenNI.setLogMinSeverity(0);
            // 初始化SDK
            OpenNI.initialize();
            Log.d(TAG, "OpenNI initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OpenNI", e);
        }
    }

    /**
     * 将 OpenNI 驱动文件从 assets 中提取到应用数据目录
     * 因为 Android 系统可能不会自动解压 jniLibs 中的 so 文件
     */
    private synchronized void extractOpenNIDrivers() {
        if (driversExtracted) {
            Log.d(TAG, "Drivers already extracted, skipping...");
            return;
        }

        try {
            Context context = getApplicationContext();
            // APK .so 文件解压后的位置（可能为空）
            String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
            // OpenNI SDK 查找驱动的位置（需要手动复制）
            String dataDirPath = context.getApplicationInfo().dataDir;
            String targetLibPath = dataDirPath + "/lib";

            Log.d(TAG, "Native library dir: " + nativeLibraryDir);
            Log.d(TAG, "Target lib path: " + targetLibPath);

            File targetDir = new File(targetLibPath);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                Log.d(TAG, "Target dir created: " + created);
            }

            // OpenNI 需要的驱动文件列表
            String[] driverFiles = {
                "libOniFile.so",
                "liborbbec.so",
                "liborbbecusb2.so",
                "libOpenNI2.so",
                "libOpenNI2.jni.so"
            };

            // 首先尝试从 nativeLibraryDir 复制
            int copiedCount = 0;
            boolean needExtractFromAssets = false;

            for (String driver : driverFiles) {
                File targetFile = new File(targetLibPath, driver);

                if (!targetFile.exists()) {
                    // 尝试从 nativeLibraryDir 复制
                    File sourceFile = new File(nativeLibraryDir, driver);
                    if (sourceFile.exists()) {
                        copyFile(sourceFile, targetFile);
                        Log.d(TAG, "Copied driver: " + driver + " from " + nativeLibraryDir);
                        copiedCount++;
                    } else {
                        needExtractFromAssets = true;
                        break;
                    }
                } else {
                    Log.d(TAG, "Driver already exists: " + driver);
                    copiedCount++;
                }
            }

            // 如果 nativeLibraryDir 中没有文件，尝试从 assets 中提取
            if (needExtractFromAssets) {
                Log.d(TAG, "Drivers not found in nativeLibraryDir, extracting from APK...");
                copyDriversFromApk(context, targetLibPath, driverFiles);
            }

            driversExtracted = true;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting OpenNI drivers", e);
            driversExtracted = true; // 标记为已处理，避免重复尝试
        }
    }

    /**
     * 从 APK 中复制驱动文件
     */
    private void copyDriversFromApk(Context context, String targetLibPath, String[] driverFiles) {
        try {
            // 获取设备ABI
            String abi = android.os.Build.SUPPORTED_ABIS[0];
            Log.d(TAG, "Device ABI: " + abi);

            // 确定 so 文件的 ABI 子目录
            String abiSubDir;
            if (abi.contains("arm64-v8a")) {
                abiSubDir = "arm64-v8a";
            } else if (abi.contains("armeabi-v7a")) {
                abiSubDir = "armeabi-v7a";
            } else {
                Log.e(TAG, "Unsupported ABI: " + abi);
                return;
            }

            Log.d(TAG, "Using ABI subdirectory: " + abiSubDir);

            // 获取APK路径
            String apkPath = context.getPackageCodePath();
            Log.d(TAG, "APK path: " + apkPath);

            int copiedCount = 0;
            for (String driver : driverFiles) {
                File targetFile = new File(targetLibPath, driver);
                if (!targetFile.exists()) {
                    // 直接从APK中提取so文件
                    String entryPath = "lib/" + abiSubDir + "/" + driver;
                    if (extractFileFromApk(apkPath, entryPath, targetFile)) {
                        Log.d(TAG, "Extracted driver from APK: " + driver);
                        copiedCount++;
                    } else {
                        Log.w(TAG, "Failed to extract " + driver + " from APK");
                    }
                } else {
                    Log.d(TAG, "Driver already exists: " + driver);
                    copiedCount++;
                }
            }

            Log.d(TAG, "Extracted " + copiedCount + "/" + driverFiles.length + " OpenNI drivers from APK");
        } catch (Exception e) {
            Log.e(TAG, "Error extracting drivers from APK", e);
        }
    }

    /**
     * 从APK文件中提取指定文件
     */
    private boolean extractFileFromApk(String apkPath, String entryPath, File targetFile) {
        java.util.zip.ZipFile zipFile = null;
        try {
            zipFile = new java.util.zip.ZipFile(apkPath);
            java.util.zip.ZipEntry entry = zipFile.getEntry(entryPath);
            if (entry == null) {
                Log.w(TAG, "Entry not found in APK: " + entryPath);
                return false;
            }

            InputStream is = zipFile.getInputStream(entry);
            copyStream(is, targetFile);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting file from APK: " + entryPath, e);
            return false;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * 复制文件
     */
    private void copyFile(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    /**
     * 复制流
     */
    private void copyStream(InputStream is, File targetFile) throws IOException {
        try (OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        } finally {
            is.close();
        }
    }
}
