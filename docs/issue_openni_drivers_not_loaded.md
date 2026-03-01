# OpenNI 驱动文件无法加载问题

## 问题描述

在集成百度人脸SDK和Orbbec 3D摄像头SDK时，进入人脸识别页面`BdFaceDepthGateActivity`后，系统回调`onDeviceOpenFailed()`方法，错误信息为"Device is null after initialization"，导致深度摄像头无法正常工作。

> 在最初的提交时，系统回调`onDeviceOpenFailed()`方法， 错误信息为：open device error: Attempt to invoke virtual method 'long org.openni.Device.getHandle()' on a null object reference， 
> 后面修改了一些，错误信息才变为 "Device is null after initialization"


## 问题背景

- **项目**：YwTestDemo - 百度人脸SDK集成项目
- **参考Demo**：百度官方Demo (`Baidu_Face_Offline_SDK_Android_8.5`)
- **相关页面**：
  - 本项目：`BdFaceDepthGateActivity.kt`
  - 百方Demo：`FaceDepthGateActivity.java`
- **SDK版本**：
  - 百度人脸SDK：FaceSDK_8.5_20241205-release.aar
  - Orbbec SDK：orbbec_module-debug.aar
  - OpenNI版本：2.3.0 (Build 61)

## 问题现象

### 1. 应用行为

- 进入`BdFaceDepthGateActivity`页面后，立即回调`onDeviceOpenFailed()`方法
- 深度摄像头无法初始化
- 无法进行人脸识别功能

### 2. 关键错误日志

```
BaseOrbbecActivity: Failed to initialize OpenNI
OpenNI: Failed to get path: /data/data/ywdemo.example.yaoxiaowen/lib//libOniFile.so
OpenNI: Failed to get path: /data/data/ywdemo.example.yaoxiaowen/lib//liborbbec.so
OpenNI: Failed to get path: /data/data/ywdemo.example.yaoxiaowen/files//liborbbec.so
OpenNI: Failed to get path liborbbec.so for lib: /system/lib/liborbbec.so
OpenNI: Couldn't use file 'liborbbec.so' as a device driver
OpenNI: Found no valid drivers
```

### 3. 设备状态检查

通过adb检查设备上so文件位置：

```bash
# 检查nativeLibraryDir指向的目录
adb shell "ls -la /data/app/ywdemo.example.yaoxiaowen-1/lib/arm64/"
# 结果：目录为空！

# 检查APK中是否包含so文件
unzip -l app-debug.apk | grep -E "lib/arm64-v8a/.*\.so$"
# 结果：APK中包含了所有OpenNI的so文件
```

## 问题分析

### 1. 根本原因

**Android系统没有自动将jniLibs目录中的OpenNI so文件解压到设备的nativeLibraryDir目录**。

具体分析：
- APK中正确打包了OpenNI的so文件（`libOniFile.so`, `liborbbec.so`, `libOpenNI2.so`等）
- 但是Android安装APK后，这些so文件没有被解压到`/data/app/<package>/lib/<abi>/`目录
- OpenNI SDK尝试从多个路径加载驱动，但都失败了：
  - `/data/data/<package>/lib/`
  - `/data/data/<package>/files/`
  - `/system/lib/`
- 最终返回"Found no valid drivers"错误

### 2. 为什么百度官方Demo能正常工作？

百度官方Demo的代码结构：
```
FaceSDKAndroid/
├── app/                    # 应用模块
├── facelibrary/           # 人脸库模块
│   └── libs/
│       └── orbbec_module-debug.aar
└── aobilibrary/           # 识别模块
    └── BaseOrbbecActivity.java
```

关键差异：
- 百度Demo通过library模块依赖传递，so文件被正确解压
- 本项目直接在app模块中引用aar，可能触发了解压问题

### 3. jniLibs目录配置问题

项目build.gradle中的配置：
```gradle
sourceSets.main {
    jniLibs.srcDirs = ["src/main/jniLibs"]
}
```

这个配置导致：
- APK中so文件路径：`lib/arm64-v8a/libOniFile.so` ✓
- 设备上解压路径：`/data/app/<package>/lib/arm64/`（空目录）✗

## 解决方案

### 方案一：手动复制so文件（临时方案）

通过adb手动复制so文件到设备：
```bash
adb push app/src/main/jniLibs/arm64-v8a/*.so /data/app/<package>/lib/arm64/
```

**缺点**：每次重新安装APK都需要手动复制，不可持续。

### 方案二：运行时自动提取（最终方案）

修改`BaseOrbbecActivity.java`，在OpenNI初始化前自动从APK中提取so文件：

```java
private synchronized void extractOpenNIDrivers() {
    // 1. 检查nativeLibraryDir中是否存在驱动文件
    String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;

    // 2. 如果不存在，从APK中手动提取
    if (!driversExist(nativeLibraryDir)) {
        String apkPath = context.getPackageCodePath();
        String targetLibPath = context.getApplicationInfo().dataDir + "/lib";

        // 3. 从APK文件中提取so文件到/data/data/<package>/lib/
        extractFilesFromApk(apkPath, targetLibPath, driverFiles);
    }
}

private boolean extractFileFromApk(String apkPath, String entryPath, File targetFile) {
    ZipFile zipFile = new ZipFile(apkPath);
    ZipEntry entry = zipFile.getEntry(entryPath);
    InputStream is = zipFile.getInputStream(entry);
    // 复制到目标位置
    ...
}
```

**关键点**：
1. 检测设备ABI（arm64-v8a或armeabi-v7a）
2. 直接从APK文件中读取so文件
3. 复制到`/data/data/<package>/lib/`目录
4. 只在首次启动时执行，避免重复操作

### 方案三：修复jniLibs配置（未采用）

尝试多种build.gradle配置：
```gradle
// 方案1：只使用jniLibs
sourceSets.main {
    jniLibs.srcDirs = ["src/main/jniLibs"]
}

// 方案2：同时包含libs
sourceSets.main {
    jniLibs.srcDirs = ["src/main/jniLibs", "libs"]
}

// 方案3：注释配置，使用默认
// sourceSets.main { ... }
```

**结果**：以上配置都无法解决so文件未解压的问题。

## 实施步骤

### 1. 创建jniLibs目录并放入so文件

```bash
# 提取orbbec_module中的so文件
mkdir -p app/src/main/jniLibs/arm64-v8a
mkdir -p app/src/main/jniLibs/armeabi-v7a

# 从orbbec_module-debug.aar中提取so文件
unzip orbbec_module-debug.aar "jni/arm64-v8a/*.so" -d temp/
cp temp/jni/arm64-v8a/*.so app/src/main/jniLibs/arm64-v8a/

# 同样处理armeabi-v7a
```

### 2. 修改build.gradle

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a"
        }
    }

    sourceSets.main {
        jniLibs.srcDirs = ["src/main/jniLibs"]
    }
}
```

### 3. 修改BaseOrbbecActivity.java

完整实现见：`app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/datalibrary/example/datalibrary/activity/BaseOrbbecActivity.java`

## 验证结果

### 成功日志

```
BaseOrbbecActivity: Starting OpenNI initialization...
BaseOrbbecActivity: Native library dir: /data/app/.../lib/arm64
BaseOrbbecActivity: Drivers not found in nativeLibraryDir, extracting from APK...
BaseOrbbecActivity: Device ABI: arm64-v8a
BaseOrbbecActivity: APK path: /data/app/.../base.apk
BaseOrbbecActivity: Extracted driver from APK: libOniFile.so
BaseOrbbecActivity: Extracted driver from APK: liborbbec.so
BaseOrbbecActivity: Extracted driver from APK: liborbbecusb2.so
BaseOrbbecActivity: Extracted driver from APK: libOpenNI2.so
BaseOrbbecActivity: Extracted driver from APK: libOpenNI2.jni.so

OpenNI: Device sensor initialized
OpenNI: Stream 'Depth' was initialized.
OpenNI: [FPS] Depth: 30.77
OpenNI: [FPS] Depth: 30.47
OpenNI: [FPS] Depth: 30.50
```

### 功能验证

- ✓ `onDeviceOpened()`回调正常触发
- ✓ 深度摄像头成功初始化
- ✓ 深度数据正常获取（30 FPS稳定帧率）
- ✓ 不再回调`onDeviceOpenFailed()`方法

## 相关文件

### 修改的文件

1. `app/build.gradle` - 添加jniLibs配置和多ABI支持
2. `app/src/main/jniLibs/arm64-v8a/` - 新增目录，包含OpenNI so文件
3. `app/src/main/jniLibs/armeabi-v7a/` - 新增目录，包含OpenNI so文件
4. `app/src/main/java/.../BaseOrbbecActivity.java` - 添加自动提取so文件逻辑

### 新增的so文件

```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── libOniFile.so
│   ├── libOpenNI2.jni.so
│   ├── libOpenNI2.so
│   ├── liborbbec.so
│   └── liborbbecusb2.so
└── armeabi-v7a/
    ├── libOniFile.so
    ├── libOpenNI2.jni.so
    ├── libOpenNI2.so
    ├── liborbbec.so
    └── liborbbecusb2.so
```

## 经验总结

### 1. Android so文件加载机制

- Android系统通常会自动将APK中`lib/<abi>/`目录的so文件解压到`/data/app/<package>/lib/<abi>/`
- 但在某些情况下（如直接引用aar），这个自动解压可能失败
- 需要手动检查并处理so文件缺失的情况

### 2. OpenNI SDK驱动查找顺序

OpenNI SDK按以下顺序查找驱动文件：
1. `/data/data/<package>/lib/`
2. `/data/data/<package>/files/`
3. `/system/lib/`
4. `/data/app/<package>/lib/<abi>/`（nativeLibraryDir）

如果上述路径都不存在驱动文件，初始化会失败。

### 3. 调试技巧

```bash
# 检查APK中包含的so文件
unzip -l app-debug.apk | grep -E "\.so$"

# 检查设备上的so文件
adb shell "ls -la /data/app/<package>/lib/<abi>/"

# 检查nativeLibraryDir路径
adb shell "run-as <package> echo \$APP_LIB"

# 实时查看日志
adb logcat | grep -E "OpenNI|BaseOrbbec"
```

## 后续问题

### 模型加载失败

虽然OpenNI驱动问题已解决，但仍存在模型加载失败的问题：

```
java.io.FileNotFoundException: face-sdk-models/detect/detect_rgb-customized-pa-192.model.float32-0.0.18.1
java.io.FileNotFoundException: face-sdk-models/align/align_rgb-customized-pa-fast.model.float32-0.7.5.5
```

这是下一个需要解决的问题，与人脸模型文件有关。

---

**文档创建时间**：2026-02-27
**问题状态**：✅ 已解决
**影响页面**：BdFaceDepthGateActivity
**SDK版本**：FaceSDK 8.5, OpenNI 2.3.0
