# YwTestDemo 三方 AAR 和 SO 文件说明

## 概述

本项目是一个集成百度人脸识别 SDK 的 Android 应用，使用到了多个第三方 AAR 库和原生 SO 库。本文档详细说明这些文件的作用和初始化流程。

---

## 一、AAR 库文件

项目位于 `app/libs/` 目录下，包含以下 5 个 AAR 文件：

### 1. FaceSDK_8.5_20241205-release.aar (6.1 MB)

**作用**：百度人脸识别核心 SDK

**功能模块**：
- 人脸检测（FaceDetect）
- 人脸识别（FaceFeature）
- 活体检测（RGB/Depth/NIR）
- 人脸比对（FaceSearch）
- 口罩检测（FaceMouthMask）
- 图片裁剪（FaceCrop）
- 光照检测（ImageIllum）
- License 授权认证

**对应 SO 库**：
- `libbdface_sdk.so` - 人脸识别算法核心库
- `libbd_facecollect_unifylicense.so` - License 授权库
- `libaikl_calc_arm.so` - AI 计算加速库
- `libliantian.so` - 联天活体检测库

### 2. orbbec_module-debug.aar (1.3 MB)

**作用**：奥比中光（Orbbec）3D 摄像头驱动模块

**功能模块**：
- OpenNI SDK 封装
- USB 设备管理
- 深度摄像头数据采集
- RGB+Depth 深度图同步

**对应 SO 库**：
- `libOpenNI2.so` - OpenNI 核心库
- `libOpenNI2.jni.so` - OpenNI JNI 接口
- `libOniFile.so` - ONI 文件格式支持
- `liborbbec.so` - Orbbec 摄像头驱动
- `liborbbecusb2.so` - USB 通信库

### 3. opencv.aar (49 MB)

**作用**：OpenCV 计算机视觉库

**功能**：
- 图像处理基础功能
- 图像格式转换（YUV、RGB、Depth）
- 图像预处理

**对应 SO 库**：
- 隐式包含，主要通过 FaceSDK 调用

### 4. ImiSDK.aar (5.2 MB)

**作用**：Imi 深度摄像头 SDK（备用）

**功能**：支持 Imi 品牌的深度摄像头

**对应 SO 库**：未在当前项目中使用

### 5. deptrumSDK.aar (3.6 MB)

**作用**：Deptrum 深度摄像头 SDK（备用）

**功能**：支持 Deptrum 品牌的深度摄像头

**对应 SO 库**：未在当前项目中使用

---

## 二、SO 库文件

项目位于 `app/src/main/jniLibs/` 目录下，支持两个 CPU 架构：

### 支持的架构
- `arm64-v8a` - 64位 ARM
- `armeabi-v7a` - 32位 ARM（含额外的 `libaikl_cluster_arm.so`）

### SO 库列表及作用

| SO 文件名 | 大小估算 | 作用说明 | 所属 SDK |
|-----------|---------|---------|----------|
| **libbdface_sdk.so** | 较大 | 人脸识别核心算法库 | FaceSDK |
| **libbd_facecollect_unifylicense.so** | 中等 | License 授权验证 | FaceSDK |
| **libaikl_calc_arm.so** | 小 | AI 计算加速（ARM NEON 优化） | FaceSDK |
| **libaikl_cluster_arm.so** | 小 | AI 聚类算法（仅 v7a） | FaceSDK |
| **libliantian.so** | 中等 | 联天活体检测算法 | FaceSDK |
| **libOpenNI2.so** | 中等 | OpenNI 深度传感器框架 | Orbbec |
| **libOpenNI2.jni.so** | 小 | OpenNI Java 接口 | Orbbec |
| **libOniFile.so** | 小 | ONI 录像文件支持 | Orbbec |
| **liborbbec.so** | 中等 | Orbbec 设备驱动 | Orbbec |
| **liborbbecusb2.so** | 小 | USB 通信 | Orbbec |
| **libc++_shared.so** | 中等 | C++ 标准库（公共依赖） | 系统库 |

---

## 三、初始化流程

### 初始化流程图

```
BdStartActivity (应用启动)
    │
    ├─► 配置加载
    │   ├─ GateConfigUtils.initConfig()      # 门禁配置
    │   └─ RegisterConfigUtils.initConfig()   # 注册配置
    │
    └─► License 激活 (FaceSDKManager.init)
        │
        ├─► 方式1: 离线激活 (initLicenseOffLine)
        │   └─ 检查本地授权文件
        │
        ├─► 方式2: 设备指纹激活 (deviceFingerActive)
        │   └─ 使用设备ID进行在线激活
        │
        └─► 方式3: 激活码激活 (onlineActive) ✓ 当前使用
            └─ 使用激活码 "XALX-FRXM-JYWX-7SX6"

BdHomeActivity (主页)
    │
    └─► 人脸库初始化
        └─ FaceSDKManager.initDataBases()
            ├─ DBManager.getInstance().init()  # 数据库初始化
            └─ FaceApi.initPush()              # 加载用户特征到内存

BdFaceDepthGateActivity (人脸识别页)
    │
    ├─► 模型加载 (initListener)
    │   └─ FaceSDKManager.initModel()
    │       ├─ FaceDetect    # 人脸检测模型
    │       ├─ FaceLive      # 活体检测模型
    │       └─ FaceSearch    # 人脸搜索模型
    │
    ├─► RGB 摄像头初始化 (startCameraPreview)
    │   └─ CameraPreviewManager.startPreview()
    │
    └─► Depth 摄像头初始化 (onResume)
        └─ OpenNIHelper.requestDeviceOpen()
            ├─ USB 权限请求
            ├─ 设备打开 (Device.open)
            └─ 深度流创建 (VideoStream.create)
```

### 关键初始化代码位置

#### 1. License 激活
**文件**：`app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/idl/face/main/activity/FaceSDKManager.java`

```java
// 行 109-128: 离线激活
bdFaceAuth.initLicenseOffLine(context, new Callback() {
    @Override
    public void onResponse(int code, String response) {
        if (code == 0) {
            initStatus = SDK_INIT_SUCCESS;
            listener.initLicenseSuccess();
        } else {
            deviceFingerActive(context, deviceFingerprint, listener);
        }
    }
});

// 行 133-152: 设备指纹激活
bdFaceAuth.initLicenseOnLine(context, deviceFinger, new Callback() {...});

// 行 155-175: 激活码激活
bdFaceAuth.initLicenseOnLine(context, activeCode, new Callback() {...});
```

#### 2. 模型加载
**文件**：`app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/datalibrary/example/datalibrary/manager/FaceSDKManager.java`

```java
// 行 136-161: 模型初始化
public void initModel(final Context context, BDFaceSDKConfig config, final SdkInitListener listener) {
    // 曝光检测
    if (imageIllum == null) {
        imageIllum = new ImageIllum();
    }
    // 人脸模型初始化
    if (faceModel == null) {
        faceModel = new FaceModel(checkMouthMask);
    }
    faceModel.init(config, context);
}
```

#### 3. OpenNI 初始化
**文件**：`app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/BdFaceDepthGateActivity.kt`

```kotlin
// 行 814-822: onResume 中初始化 OpenNI
override fun onResume() {
    super.onResume()
    startCameraPreview()  // RGB 摄像头

    mOpenNIHelper = OpenNIHelper(this)
    mOpenNIHelper!!.requestDeviceOpen(this)  // Depth 摄像头
}

// 行 380-414: 设备打开回调
override fun onDeviceOpened(usbDevice: UsbDevice) {
    initUsbDevice(usbDevice)
    mDepthStream = VideoStream.create(this.mDevice, SensorType.DEPTH)
    // ...
    startThread()  // 开始深度数据采集
}
```

---

## 四、依赖关系

```
YwTestDemo (App)
    │
    ├─ FaceSDK (百度人脸SDK)
    │   ├─ libbdface_sdk.so
    │   ├─ libbd_facecollect_unifylicense.so
    │   ├─ libaikl_calc_arm.so
    │   └─ libliantian.so
    │
    ├─ OpenNI (Orbbec深度摄像头)
    │   ├─ libOpenNI2.so
    │   ├─ libOpenNI2.jni.so
    │   ├─ libOniFile.so
    │   ├─ liborbbec.so
    │   └─ liborbbecusb2.so
    │
    └─ OpenCV (图像处理)
        └─ (隐式依赖，通过 FaceSDK 调用)

公共依赖:
    └─ libc++_shared.so (C++ 运行时)
```

---

## 五、build.gradle 配置说明

**文件**：`app/build.gradle`

```gradle
// 行 28-30: 支持的 CPU 架构
ndk {
    abiFilters "armeabi-v7a", "arm64-v8a"
}

// 行 44-46: jniLibs 源目录配置
sourceSets.main {
    jniLibs.srcDirs = ["src/main/jniLibs"]
}

// 行 61-63: 处理 libc++_shared.so 重复问题
packagingOptions {
    pickFirst 'lib/arm64-v8a/libc++_shared.so'
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
}

// 行 86-92: AAR 依赖配置
api files('libs/orbbec_module-debug.aar')
api fileTree(include: ['*.jar'], dir: 'libs')
api files('libs/ImiSDK.aar')
api files('libs/deptrumSDK.aar')
api files('libs/opencv.aar')
api files('libs/FaceSDK_8.5_20241205-release.aar')
```

---

## 六、注意事项

### 1. SO 库加载顺序
系统自动按需加载，不需要手动调用 `System.loadLibrary()`

### 2. 激活码配置
当前使用激活码：`XALX-FRXM-JYWX-7SX6`

### 3. 架构兼容性
- 同时支持 32 位 (armeabi-v7a) 和 64 位 (arm64-v8a)
- `libaikl_cluster_arm.so` 仅存在于 v7a 架构

### 4. 深度摄像头初始化顺序
1. 先初始化 RGB 摄像头
2. 再请求 USB 权限并打开 Depth 设备
3. 创建深度流并启动数据采集线程

---

## 七、参考文档

- [百度人脸SDK官方文档](https://ai.baidu.com/ai-doc/FACE/pk37c1mqu)
- 百度官方Demo：`/Users/yaowen/Desktop/八维通/需求-App/pad-天津/0205-百度给的新demo/Baidu_Face_Offline_SDK_Android_8.5/`
