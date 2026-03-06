# ABI 架构配置说明文档

## 文档信息

- **创建日期**: 2026-03-05
- **问题背景**: 项目从支持 armeabi-v7a + arm64-v8a 改为仅支持 armeabi-v7a，导致摄像头无法工作
- **最终结论**: 必须同时支持两种架构

---

## 问题描述

### 原始需求
用户希望将项目从支持 `armeabi-v7a` 和 `arm64-v8a` 两种架构，改为**仅支持 armeabi-v7a**，以减小 APK 体积。

### 错误现象
修改后，在实际运行时出现以下错误：

```
onDeviceOpenFailed, msg: Device is null after initialization
```

深度摄像头无法正常初始化，导致人脸识别功能无法使用。

---

## 测试设备信息

- **设备架构**: 64 位 ARM (arm64-v8a)
- **摄像头**: Orbbec 深度摄像头 (基于 OpenNI2 框架)
- **SDK**:
  - 百度人脸 SDK: FaceSDK_8.5_20241205-release.aar
  - Orbbec SDK: orbbec_module-debug.aar

---

## 修改过程记录

### 第一次修改：仅支持 armeabi-v7a

#### 修改内容

**app/build.gradle**:

```gradle
// 修改前
ndk {
    abiFilters "armeabi-v7a", "arm64-v8a"
}

// 修改后
ndk {
    abiFilters "armeabi-v7a"
}
```

```gradle
// packagingOptions 添加排除规则
packagingOptions {
    jniLibs {
        // 排除 arm64-v8a 架构，仅打包 armeabi-v7a
        excludes += '**/arm64-v8a/**'
    }
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
}
```

**jniLibs 目录**:
- 删除 `app/src/main/jniLibs/arm64-v8a/` 目录

#### 测试结果

❌ **失败** - 摄像头无法初始化，报错 "Device is null after initialization"

---

### 第二次修改：恢复双架构支持

#### 修改内容

**app/build.gradle**:

```gradle
// 恢复支持两种架构
ndk {
    abiFilters "armeabi-v7a", "arm64-v8a"
}
```

```gradle
// 恢复 packagingOptions 配置
packagingOptions {
    resources {
        excludes += '/META-INF/{AL2.0,LGPL2.1}'
        excludes += 'META-INF/annotations/**'
        excludes += '**/annotations/**'
    }
    // 处理libc++_shared.so重复问题（opencv和FaceSDK都包含）
    pickFirst 'lib/arm64-v8a/libc++_shared.so'
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
}
```

**jniLibs 目录**:
- 恢复 `app/src/main/jniLibs/arm64-v8a/` 目录
- 从 `orbbec_module-debug.aar` 中提取 OpenNI 相关的 5 个 so 文件：
  - libOniFile.so
  - libOpenNI2.jni.so
  - libOpenNI2.so
  - liborbbec.so
  - liborbbecusb2.so

#### 测试结果

✅ **成功** - 摄像头正常工作，深度数据正常获取

---

## 问题原因分析

### 根本原因

在 **64 位 ARM 设备**上，**OpenNI 驱动必须使用对应架构（arm64-v8a）的 so 库**，无法通过兼容模式运行 32 位（armeabi-v7a）的库。

### 技术细节

1. **OpenNI2 框架限制**
   - OpenNI 驱动在初始化时会严格检查架构匹配
   - 如果设备是 arm64-v8a，必须加载 arm64-v8a 的驱动库
   - 32 位库无法在 64 位进程中以兼容模式正常工作

2. **百度官方 demo 配置**
   - 参考文档：`/Users/yaowen/Desktop/八维通/需求-App/pad-天津/0205-百度给的新demo/Baidu_Face_Offline_SDK_Android_8.5/Baidu_Face_Offline_SDK_Android_8.5/FaceSDKAndroid/app/build.gradle`
   - 官方 demo 同时支持 armeabi-v7a 和 arm64-v8a
   - 官方文档明确说明 arm64-v8a 是"推荐使用"的架构

3. **Android 系统行为**
   - Android 安装 APK 时，会根据设备架构选择对应 ABI 的 so 库
   - 64 位设备优先使用 arm64-v8a，如果不存在才会降级到 armeabi-v7a
   - 但 OpenNI 驱动不支持这种降级机制

---

## 对比总结

| 配置方案 | APK 大小 | 32 位设备 | 64 位设备 | 摄像头功能 |
|---------|---------|----------|----------|-----------|
| armeabi-v7a + arm64-v8a | 较大 | ✅ 正常 | ✅ 正常 | ✅ 正常 |
| 仅 armeabi-v7a | 较小 | ✅ 正常 | ❌ 无法工作 | ❌ 不可用 |

---

## 最终配置

### app/build.gradle

```gradle
android {
    defaultConfig {
        // 参考百度官方demo，支持 armeabi-v7a 和 arm64-v8a 架构
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a"
        }
    }

    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
            excludes += 'META-INF/annotations/**'
            excludes += '**/annotations/**'
        }
        // 处理libc++_shared.so重复问题（opencv和FaceSDK都包含）
        pickFirst 'lib/arm64-v8a/libc++_shared.so'
        pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    }
}
```

### jniLibs 目录结构

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

---

## 相关文档

- **OpenNI 驱动问题记录**: `docs/issue_openni_drivers_not_loaded.md`
- **百度官方 demo**: `/Users/yaowen/Desktop/八维通/需求-App/pad-天津/0205-百度给的新demo/Baidu_Face_Offline_SDK_Android_8.5/`

---

## 重要提醒

⚠️ **切勿修改为仅支持 armeabi-v7a**

如果需要减小 APK 体积，可以考虑以下方案：

1. **APK Splits** - 为不同架构生成独立的 APK
2. **按需下载** - 动态下载对应架构的库（需要额外的架构适配代码）

但在当前的技术条件下（OpenNI 驱动限制），**必须同时包含两种架构的 so 库**。

---

## 变更记录

| 日期 | 操作 | 结果 |
|------|------|------|
| 2026-03-05 | 尝试仅支持 armeabi-v7a | ❌ 失败 |
| 2026-03-05 | 恢复双架构支持 | ✅ 成功 |
