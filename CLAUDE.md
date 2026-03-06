# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

本项目是一个用于接入百度人脸SDK的Android演示项目，实现基于3D摄像头（奥比中光Orbbec）的人脸识别和活体检测功能。

**当前状态**：核心功能已实现，包括License激活、人脸库加载、活体检测和图片保存。

## 开发语言

- **代码**：使用简体中文编写注释和文档
- **变量/函数名**：保持英文命名规范

## 构建命令

### 编译Debug版本
```bash
./gradlew assembleDebug
```

### 编译Release版本
```bash
./gradlew assembleRelease
```

### 清理构建
```bash
./gradlew clean
```

### 安装到设备
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 架构说明

### SDK依赖架构
项目依赖多个第三方SDK，需要注意版本兼容性：

1. **百度人脸SDK** (`FaceSDK_8.5_20241205-release.aar`)
   - 核心人脸识别和活体检测功能
   - 包含 `libbdface_sdk.so` 和 `libbd_facecollect_unifylicense.so`

2. **Orbbec 3D摄像头SDK** (`orbbec_module-debug.aar`)
   - OpenNI框架，用于深度摄像头
   - 包含 `libOpenNI2.so`、`liborbbec.so` 等

3. **OpenCV** (`opencv.aar`)
   - 图像处理库

4. **其他SDK**
   - `ImiSDK.aar` - 蚂蚁金服视觉SDK
   - `deptrumSDK.aar` - Deptrum深度传感器SDK

### 包结构

```
ywdemo.example.yaoxiaowen/
├── baiduface/                    # 百度人脸相关功能
│   ├── BdStartActivity          # 启动页面：License激活 + 人脸库加载
│   ├── BdFaceDepthGateActivity  # 人脸识别页面：活体检测核心逻辑
│   ├── datalibrary/             # 百度SDK数据层封装
│   │   ├── api/FaceApi         # 人脸API接口
│   │   ├── manager/FaceSDKManager  # SDK管理器
│   │   ├── manager/SaveImageManager # 图片保存管理
│   │   └── db/                 # 人脸库数据库
│   ├── huajieLibrary/          # 华捷艾米相关（结构光摄像头）
│   ├── AuthLibrary/            # License认证库
│   └── idl/face/main/activity/FaceSDKManager  # 另一个SDK管理器
└── until/                       # 工具类
    └── LogUtil                  # 日志工具
```

### 活体检测流程

1. **初始化阶段** (`BdStartActivity`)
   - License在线激活（激活码：`XALX-FRXM-JYWX-7SX6`）
   - 加载配置文件（`gateFaceConfig.txt`）
   - 初始化人脸库数据库

2. **识别阶段** (`BdFaceDepthGateActivity`)
   - OpenNI初始化，连接Orbbec摄像头
   - 同时获取RGB和Depth图像流
   - 人脸检测 -> 活体判断 -> 质量检测 -> 特征提取
   - 满足条件时保存图片

3. **图片保存条件**
   - RGB活体得分 > `saveImageThreshold`（默认0.80）
   - Depth活体得分 > `saveImageThreshold`（默认0.80）
   - 保存路径：`/storage/emulated/0/Save-Image/{日期时间}/`

### 配置系统

**配置文件位置**：`/data/data/ywdemo.example.yaoxiaowen/files/Settings/gateFaceConfig.txt`

**配置类**：`BaseConfig.java`（通过`SingleBaseConfig`单例访问）

**关键配置项**：
- `rgbLiveScore` / `depthLiveScore`：活体检测阈值（默认0.80）
- `saveImageThreshold`：图片保存阈值（默认0.80）
- `livingControl`：是否开启活体检测
- `enableMediaScan`：保存的图片是否在图库显示
- `cameraType`：摄像头类型（0=奥比中光海燕/大白）

修改配置后需重启应用生效。

### 多架构支持

- **支持的ABI**：`armeabi-v7a`、`arm64-v8a`
- **多Dex支持**：`multiDexEnabled true`
- **重要**：已处理 `libc++_shared.so` 重复问题（使用 `pickFirst`）

## 常见问题

### 图片未保存
1. 检查Toast提示中的实际得分
2. 确保 RGB 和 Depth 得分都大于 `saveImageThreshold`
3. 可临时降低阈值进行测试

### 活体检测太严格
1. 降低 `rgbLiveScore` 和 `depthLiveScore`（如改为0.70）
2. 检查摄像头是否有遮挡
3. 确保光照条件良好

### 图片在图库看不到
检查 `enableMediaScan` 配置是否为 `true`

## 参考资源

- **百度官方Demo路径**：`/Users/yaowen/Desktop/八维通/需求-App/pad-天津/0205-百度给的新demo/Baidu_Face_Offline_SDK_Android_8.5/Baidu_Face_Offline_SDK_Android_8.5/FaceSDKAndroid`
- **百度SDK官方文档**：https://ai.baidu.com/ai-doc/FACE/pk37c1mqu
- **配置说明文档**：`docs/百度人脸SDK配置说明.md`
