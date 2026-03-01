# 百度人脸SDK文件缺失问题解决方案

## 问题描述

### 问题现象

在 `BdFaceDepthGateActivity` 页面中，虽然成功回调了 `onDeviceOpened()`，但点击"预览模式"或"开始模式"后：
- **预览模式**：页面显示白色，无法显示摄像头内容
- **开始模式**：页面显示黑色，无法显示摄像头内容

表现效果就像摄像头完全没有打开一样，无法展示摄像头录制到的实时内容。

### 期望效果

打开摄像头后，屏幕上应该动态展示摄像头录制到的内容，并能够进行人脸识别。

---

## 问题背景

### 项目信息

- **项目名称**：YwTestDemo - 百度人脸SDK集成项目
- **参考Demo**：百度官方Demo `Baidu_Face_Offline_SDK_Android_8.5/FaceSDKAndroid`
- **当前页面**：`BdFaceDepthGateActivity`（人脸识别深度门页面）
- **SDK版本**：
  - 百度人脸SDK：FaceSDK_8.5_20241205-release.aar
  - Orbbec 3D摄像头SDK：orbbec_module-debug.aar
  - OpenNI版本：2.3.0 (Build 61)

### 项目架构差异

| 项目 | 架构类型 | 模块组成 |
|------|---------|---------|
| **百度官方Demo** | 多模块架构 | app + facelibrary + aobilibrary + datalibrary 等7个模块 |
| **本项目** | 单模块架构 | 只有 app 模块 |

---

## 关键日志分析

### 1. OpenNI驱动加载成功（✅ 已解决）

```
BaseOrbbecActivity      Extracted 5/5 OpenNI drivers from APK
OpenNI                  Device sensor initialized
OpenNI                  Device connected: Orbbec Astra (2bc5/060e@2/4)
```

### 2. 百度人脸库加载失败（❌ 核心问题）

```
System.err              java.lang.UnsatisfiedLinkError: dalvik.system.PathClassLoader[...] couldn't find "libbd_unifylicense.so"
System.err                  at java.lang.Runtime.loadLibrary0(Runtime.java:984)
System.err                  at com.baidu.idl.main.facesdk.FaceAuth.<init>(FaceAuth.java:44)
```

### 3. 模型文件加载失败（❌ 核心问题）

```
System.err              java.io.FileNotFoundException: face-sdk-models/detect/detect_rgb-customized-pa-192.model.float32-0.0.18.1
System.err              java.io.FileNotFoundException: face-sdk-models/align/align_rgb-customized-pa-fast.model.float32-0.7.5.5
System.err              java.io.FileNotFoundException: face-sdk-models/blur/blur-customized-pa-addcloud_quant_e19.model.float32-3.0.13.3
System.err              java.io.FileNotFoundException: face-sdk-models/feature/feature_live-mnasnet-pa-attention_v4.model.int8-2.0.239.1
...
face_model              跟踪能力加载失败
face_model              质量模型加载失败
face_model              属性模型加载失败
```

### 4. 摄像头初始化失败（❌ 导致无显示）

```
camera_preview          开启预览模式
camera_preview          initCamera---open camera
Camera                  app passed NULL surface
```

---

## 根本原因分析

### 原因一：项目架构差异导致文件路径不同

#### 百度官方Demo的多模块架构

```
FaceSDKAndroid/
├── app/                                    # 主应用模块
│   └── src/main/assets/
│       └── license.ini                    # license文件
│
├── facelibrary/                           # 🔑 关键：独立的library模块
│   ├── src/main/
│   │   └── assets/
│   │       └── face-sdk-models/           # ⭐ 模型文件在这里！
│   │           ├── align/
│   │           ├── detect/
│   │           ├── feature/
│   │           ├── silent_live/
│   │           └── ... (17个子目录)
│   └── libs/
│       ├── FaceSDK_8.5_20241205-release.aar
│       ├── orbbec_module-debug.aar
│       └── opencv.aar
│
├── aobilibrary/                           # Orbbec摄像头库
├── datalibrary/                          # 数据库库
└── settings.gradle
    include ':app', ':facelibrary', ':aobilibrary', ...
```

**关键点**：`facelibrary` 是一个独立的 Android Library 模块，拥有自己的 `src/main/assets/` 和 `src/main/jniLibs/` 目录。

#### 本项目的单模块架构

```
YwTestDemo/
└── app/                                   # 只有一个模块
    ├── libs/
    │   └── FaceSDK_8.5_20241205-release.aar   # 只有AAR文件
    └── src/main/
        ├── jniLibs/
        │   ├── arm64-v8a/
        │   │   ├── libOniFile.so
        │   │   ├── liborbbec.so            # 只有Orbbec的so
        │   │   └── ...
        │   └── armeabi-v7a/
        └── assets/                        # ❌ 空目录！
```

### 原因二：FaceSDK AAR的内容限制

通过解压 `FaceSDK_8.5_20241205-release.aar` 分析其内容：

```
FaceSDK_8.5_20241205-release.aar
├── AndroidManifest.xml
├── classes.jar                            # Java/Kotlin编译后的代码
├── R.txt                                  # 资源索引
├── res/                                   # Android资源文件
│   └── values/values.xml
├── jni/                                   # ✅ 包含native库
│   ├── arm64-v8a/
│   │   ├── libbdface_sdk.so               # 百度人脸SDK核心库
│   │   ├── libbd_facecollect_unifylicense.so  # license库
│   │   ├── libaikl_calc_arm.so
│   │   ├── libliantian.so
│   │   └── libc++_shared.so
│   └── armeabi-v7a/
│       └── ... (对应的so文件)
└── libs/                                  # 辅助jar包
    ├── bd_facecollect_unifylicense.jar
    └── liantian.jar
```

**⚠️ 重要发现**：
- ✅ **包含**：jni/ 目录中的 .so 文件
- ✅ **包含**：classes.jar Java代码
- ❌ **不包含**：assets/ 目录
- ❌ **不包含**：face-sdk-models/ 模型文件

### 原因三：百度官方是如何组织的

**百度官方Demo并非让AAR自动提供所有文件，而是手动维护了额外文件**：

```
facelibrary/                               # 这是百度手动创建和维护的模块
├── src/main/assets/face-sdk-models/       # ⭐ 这些文件是百度手动添加的！
│   ├── detect/detect_rgb-customized-pa-192.model.float32-0.0.18.1
│   ├── align/align_rgb-customized-pa-fast.model.float32-0.7.5.5
│   ├── feature/feature_live-mnasnet-pa-attention_v4.model.int8-2.0.239.1
│   └── ... (约300+ MB的模型文件)
│
└── libs/FaceSDK_8.5_20241205-release.aar  # AAR只提供代码和so库
```

当 app 模块依赖 facelibrary 时，Android 构建系统会自动合并：
```
最终APK包含：
├── app的assets (license.ini)
└── facelibrary的assets (face-sdk-models/)    ← 自动合并进APK
```

---

## 为什么百度Demo不需要这些变动？

### 详细对比说明

#### 场景A：百度官方Demo

**项目结构**：
```gradle
// settings.gradle
include ':app', ':facelibrary'

// app/build.gradle
dependencies {
    implementation project(':facelibrary')    // 依赖facelibrary模块
}
```

**文件来源**：
```
facelibrary模块提供：
├── src/main/assets/face-sdk-models/    ← 百度手动维护的模型文件
└── libs/FaceSDK_8.5_xxx.aar            ← AAR提供so库和Java代码
```

**构建过程**：
```
1. 编译 facelibrary 模块
   ├── 合并 assets/ → face-sdk-models/
   └── 解压 AAR → jni/*.so

2. 编译 app 模块
   └── 依赖 facelibrary
       └── 自动继承 facelibrary 的 assets 和 jniLibs

3. 生成 APK
   ├── assets/
   │   ├── license.ini (来自app)
   │   └── face-sdk-models/ (来自facelibrary) ✅
   └── lib/arm64-v8a/
       ├── libbdface_sdk.so (来自AAR) ✅
       └── libbd_facecollect_unifylicense.so ✅
```

#### 场景B：本项目（单模块）

**项目结构**：
```gradle
// settings.gradle (没有其他模块)
rootProject.name = "YwTestDemo"

// app/build.gradle
dependencies {
    api files('libs/FaceSDK_8.5_20241205-release.aar')  // 直接依赖AAR
}
```

**文件来源**：
```
app模块只有：
├── libs/FaceSDK_8.5_xxx.aar    ← AAR只包含so库，不包含模型
└── src/main/
    ├── assets/                 ← 空的！没有模型文件
    └── jniLibs/               ← 只有Orbbec的so，没有百度的so
```

**构建过程（修复前）**：
```
1. 编译 app 模块
   └── 解压 AAR → jni/*.so

2. 生成 APK
   ├── assets/                    ← 空的！❌
   │   └── (缺少 face-sdk-models/)
   └── lib/arm64-v8a/
       └── (缺少 libbd_facecollect_unifylicense.so) ❌
```

### 核心差异总结

| 对比项 | 百度官方Demo | 本项目（修复前） |
|--------|-------------|-----------------|
| **模块数量** | 7个模块 | 1个模块 |
| **FaceSDK集成方式** | 通过 facelibrary 模块 | 直接依赖 AAR |
| **模型文件位置** | facelibrary/src/main/assets/ | 缺失 ❌ |
| **模型文件来源** | 百度手动维护 | AAR不包含，所以缺失 |
| **license库** | AAR自动提供 | 之前没有从AAR提取 ❌ |
| **构建系统合并** | 自动合并 facelibrary 的 assets | 无可合并内容 |

### 为什么百度需要多模块架构？

1. **代码组织**：将人脸识别相关代码独立成 facelibrary，便于复用
2. **资源管理**：大型模型文件（300+ MB）与代码分离管理
3. **版本控制**：facelibrary 可以独立版本迭代
4. **AAR的限制**：AAR 不适合包含大量 assets 文件

---

## 解决方案

### 修复步骤一：复制模型文件到 assets 目录

**操作**：
```bash
# 从百度Demo复制模型文件
cp -r FaceSDKAndroid/facelibrary/src/main/assets/face-sdk-models \
      YwTestDemo/app/src/main/assets/
```

**结果**：
```
app/src/main/assets/
├── face-sdk-models/          # ✅ 新增
│   ├── align/
│   ├── detect/
│   ├── feature/
│   ├── silent_live/
│   └── ... (17个子目录)
└── license.ini               # ✅ 新增
```

**解决错误**：
```
❌ java.io.FileNotFoundException: face-sdk-models/detect/...
✅ 模型文件成功加载
```

### 修复步骤二：提取并复制 FaceSDK 的 so 库

**操作**：
```bash
# 从 FaceSDK AAR 中提取 so 文件
unzip FaceSDK_8.5_20241205-release.aar "jni/arm64-v8a/*.so"

# 复制到项目jniLibs
cp jni/arm64-v8a/*.so app/src/main/jniLibs/arm64-v8a/
cp jni/armeabi-v7a/*.so app/src/main/jniLibs/armeabi-v7a/
```

**结果**：
```
app/src/main/jniLibs/arm64-v8a/
├── libbd_facecollect_unifylicense.so  # ✅ 新增 - 解决license库缺失
├── libbdface_sdk.so                    # ✅ 新增 - 百度人脸SDK核心库
├── libaikl_calc_arm.so                 # ✅ 新增
├── libliantian.so                      # ✅ 新增
├── libc++_shared.so                    # ✅ 已配置 pickFirst 避免冲突
├── libOniFile.so                       # OpenNI (已有)
├── libOpenNI2.so                       # OpenNI (已有)
└── liborbbec.so                        # Orbbec (已有)
```

**解决错误**：
```
❌ java.lang.UnsatisfiedLinkError: ... couldn't find "libbd_unifylicense.so"
✅ native库成功加载
```

### 修复步骤三：添加 license.ini 文件

**操作**：
```bash
cp FaceSDKAndroid/app/src/main/assets/license.ini \
   YwTestDemo/app/src/main/assets/
```

---

## 验证结果

### 构建结果

```bash
./gradlew assembleDebug

BUILD SUCCESSFUL in 2s
36 actionable tasks: 12 executed, 24 up-to-date
```

### 运行结果

- ✅ 模型文件成功加载
- ✅ license库成功加载
- ✅ 摄像头正常预览
- ✅ 人脸检测功能正常
- ✅ 页面能够显示摄像头实时内容

---

## 关键经验总结

### 1. AAR 文件的内容限制

AAR（Android Archive）文件虽然可以包含：
- ✅ 编译后的代码（classes.jar）
- ✅ Native库（jni/*.so）
- ✅ Android资源（res/）
- ✅ 清单文件（AndroidManifest.xml）

但**不适合包含大量 assets 文件**，百度官方选择将模型文件放在模块的 assets 目录而非 AAR 中。

### 2. 多模块 vs 单模块架构

| 特性 | 多模块架构（百度Demo） | 单模块架构（本项目） |
|------|---------------------|-------------------|
| **优点** | 代码复用、职责分离、维护方便 | 结构简单、构建快速 |
| **缺点** | 构建复杂、配置繁琐 | 所有内容混在一起 |
| **适用场景** | 大型项目、SDK开发 | 小型应用、快速原型 |

### 3. 依赖传递的自动合并机制

当模块A依赖模块B时：
```
模块B的 src/main/assets/  →  自动合并到 模块A的 assets/
模块B的 src/main/jniLibs/ →  自动合并到 模块A的 jniLibs/
```

**这就是为什么百度Demo不需要额外配置**：facelibrary 的内容自动合并到了 app 模块。

### 4. 调试技巧

```bash
# 检查APK中包含的so文件
unzip -l app-debug.apk | grep -E "\\.so$"

# 检查APK中包含的assets
unzip -l app-debug.apk | grep -E "assets/"

# 检查AAR文件内容
unzip -l library.aar

# 实时查看日志
adb logcat | grep -E "FaceSDK|OpenNI|camera_preview"
```

---

## 相关文件清单

### 修改的文件

1. **app/src/main/assets/** (新建目录)
   - face-sdk-models/ (从Demo复制)
   - license.ini (从Demo复制)

2. **app/src/main/jniLibs/arm64-v8a/** (新增so文件)
   - libbd_facecollect_unifylicense.so
   - libbdface_sdk.so
   - libaikl_calc_arm.so
   - libliantian.so

3. **app/src/main/jniLibs/armeabi-v7a/** (新增so文件)
   - libbd_facecollect_unifylicense.so
   - libbdface_sdk.so
   - libaikl_calc_arm.so
   - libaikl_cluster_arm.so
   - libliantian.so

### 无需修改的文件

- build.gradle (已有 pickFirst 配置处理 libc++_shared.so 冲突)
- BdFaceDepthGateActivity.kt (代码逻辑无需修改)

---

## 附录：文件来源映射表

| 文件/目录 | 来源 | 用途 |
|----------|------|------|
| face-sdk-models/ | Demo: facelibrary/src/main/assets/ | 百度人脸识别模型文件 |
| license.ini | Demo: app/src/main/assets/ | 百度SDK授权文件 |
| libbd_facecollect_unifylicense.so | Demo: FaceSDK AAR → jni/arm64-v8a/ | License验证库 |
| libbdface_sdk.so | Demo: FaceSDK AAR → jni/arm64-v8a/ | 百度人脸SDK核心库 |
| libaikl_calc_arm.so | Demo: FaceSDK AAR → jni/arm64-v8a/ | AI计算库 |
| libliantian.so | Demo: FaceSDK AAR → jni/arm64-v8a/ | 联天SDK库 |
| libOniFile.so | Demo: orbbec_module AAR | OpenNI驱动 |
| liborbbec.so | Demo: orbbec_module AAR | Orbbec摄像头驱动 |

---

**文档创建时间**：2026-02-27
**问题状态**：✅ 已解决
**影响页面**：BdFaceDepthGateActivity
**SDK版本**：FaceSDK 8.5, OpenNI 2.3.0
**解决方案**：从百度Demo复制缺失的模型文件和so库到单模块项目中
