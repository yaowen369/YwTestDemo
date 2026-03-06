# 项目仅支持 armeabi-v7a 架构修改指南

## 文档信息
- **修改日期**: 2026-03-06
- **项目**: YwTestDemo (百度人脸SDK集成)
- **目标**: 将项目从支持多架构(arm64-v8a + armeabi-v7a)改为仅支持 armeabi-v7a
- **参考项目**: FaceSDKAndroid (百度官方Demo)

---

## 一、问题背景

### 1.1 为什么需要修改
- 参考项目 FaceSDKAndroid 已修改为仅支持 armeabi-v7a
- 需要保持当前项目与参考项目架构一致
- 统一架构可以减少APK体积

### 1.2 初始状态
- 当前项目同时支持 arm64-v8a 和 armeabi-v7a 架构
- 测试设备为 arm64-v8a 架构
- 目标是让项目仅使用 armeabi-v7a 库在 arm64-v8a 设备上正常运行

---

## 二、修改过程

### 2.1 第一次尝试（失败）

**修改内容**：
```gradle
// build.gradle
ndk {
    abiFilters "armeabi-v7a"
}
```

**结果**：❌ 失败
```
错误: enumerateDevices: 0 devices
原因: Native库没有被正确安装到设备
```

### 2.2 第二次尝试（失败）

**修改内容**：
```gradle
packagingOptions {
    exclude 'lib/arm64-v8a/**'
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
}

buildTypes {
    debug {
        jniDebuggable true
    }
}
```

**结果**：❌ 失败
```
错误: /data/app/.../lib/arm/ 目录为空
原因: 即使排除了 arm64-v8a，库仍然无法安装
```

### 2.3 第三次尝试（部分成功）

**修改内容**：
```gradle
// 降低 compileSdk
compileSdk 29
targetSdk 29

// 降低 androidx 库版本
implementation 'androidx.core:core:1.3.2'
implementation 'androidx.appcompat:appcompat:1.2.0'
```

**结果**：⚠️ 部分成功
```
问题: 库可以安装，但OpenNI驱动加载失败
```

### 2.4 最终方案（✅ 成功）

**关键修改**：

1. **build.gradle**
```gradle
android {
    compileSdk 29
    targetSdk 29

    defaultConfig {
        ndk {
            abiFilters "armeabi-v7a"
        }
    }

    buildTypes {
        debug {
            jniDebuggable true
        }
    }

    packagingOptions {
        exclude 'lib/arm64-v8a/**'
        pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    }
}

dependencies {
    // 使用与 compileSdk 29 兼容的 androidx 库版本
    implementation 'androidx.core:core:1.3.2'
    implementation 'androidx.lifecycle:lifecycle-runtime:2.2.0'
    implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'com.google.android.material:material:1.2.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
}
```

2. **AndroidManifest.xml** ⚠️ **最关键**
```xml
<application
    android:extractNativeLibs="true"
    ...>
```

**结果**：✅ 完全成功
```
验证:
- APK只包含 armeabi-v7a 库
- 库正确安装到 /data/app/.../lib/arm/
- OpenNI成功加载并连接摄像头
```

---

## 三、关键修改点详解

### 3.1 AndroidManifest.xml - android:extractNativeLibs

**这是最关键的配置！**

```xml
android:extractNativeLibs="true"
```

**作用**：
- 明确告诉系统将 native 库从 APK 中提取到文件系统
- 在 arm64-v8a 设备上运行 armeabi-v7a 应用时必须设置

**为什么重要**：
- Android 10+ 默认行为是不提取库以节省空间
- 在 arm64 设备上运行 armeabi-v7a 应用时，系统可能不会自动安装库
- 显式设置 `true` 可以强制提取并安装库

### 3.2 build.gradle - packagingOptions

```gradle
packagingOptions {
    // 排除所有 arm64-v8a 架构的库
    exclude 'lib/arm64-v8a/**'
    // 处理重复库
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
}
```

**作用**：
- 确保最终 APK 不包含任何 arm64-v8a 的 .so 文件
- 避免打包时因重复文件导致的错误

### 3.3 build.gradle - jniDebuggable

```gradle
buildTypes {
    debug {
        jniDebuggable true
    }
}
```

**作用**：
- 启用 JNI 调试支持
- 有助于 native 库的加载和调试

### 3.4 降低 compileSdk 到 29

```gradle
compileSdk 29
targetSdk 29
```

**原因**：
- Android 10 (API 29) 对 armeabi-v7a 的支持更稳定
- 参考项目使用的是 API 29
- 避免高版本 Android 系统的兼容性问题

---

## 四、坑点总结

### 4.1 ⚠️ 坑点一：只设置 abiFilters 无效

**错误做法**：
```gradle
ndk {
    abiFilters "armeabi-v7a"
}
```

**问题**：
- 虽然编译通过，但 .so 库不会安装到设备
- 运行时会报错：`enumerateDevices: 0 devices`

**正确做法**：
必须配合 `android:extractNativeLibs="true"` 使用

### 4.2 ⚠️ 坑点二：exclude lib/arm64-v8a/** 还不够

**错误做法**：
```gradle
packagingOptions {
    exclude 'lib/arm64-v8a/**'
}
```

**问题**：
- 即使排除了 arm64-v8a，armeabi-v7a 库仍然无法安装
- 原因是缺少 `android:extractNativeLibs="true"`

### 4.3 ⚠️ 坑点三：高版本 compileSdk 的兼容性问题

**问题**：
- 使用 compileSdk 33 时，androidx 库要求 API 31+
- 降低了 compileSdk 后需要同步降低 androidx 库版本

**解决方案**：
```gradle
compileSdk 29
// 使用兼容的 androidx 版本
implementation 'androidx.core:core:1.3.2'
```

### 4.4 ⚠️ 坑点四：设备架构不匹配导致的问题

**现象**：
- 设备是 arm64-v8a 架构
- APK 只包含 armeabi-v7a 库
- OpenNI 查找库时路径错误（查找 /system/lib 而非 /system/lib64）

**解决**：
- 设置 `android:extractNativeLibs="true"` 强制提取
- 降低 compileSdk 到 29 提高兼容性

### 4.5 ⚠️ 坑点五：Toast 在 Activity 销毁后崩溃

**错误**：
```java
Toast.makeText(this, "message", Toast.LENGTH_LONG).show();
```

**问题**：
- 用户按 Home 键后 Activity 销毁
- Toast 尝试显示时抛出 BadTokenException

**解决方案**：
```java
private void showToast(final String message) {
    if (isFinishing() || isDestroyed()) {
        return;
    }
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }
    });
}
```

---

## 五、验证方法

### 5.1 验证 APK 架构

```bash
# 检查 APK 包含哪些架构的库
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/"
```

**期望结果**：
- 只看到 `lib/armeabi-v7a/` 目录
- 没有任何 `lib/arm64-v8a/` 目录

### 5.2 验证库是否正确安装

```bash
# 检查设备上的库是否安装
adb shell "run-as ywdemo.example.yaoxiaowen ls -la lib/"
```

**期望结果**：
- 看到多个 .so 文件
- 文件数量应该在 20+ 个

### 5.3 验证 OpenNI 驱动加载

```bash
# 查看日志
adb logcat -s "OpenNI:*"
```

**期望结果**：
```
Device connected: Orbbec Astra (2bc5/060e@2/4)
```

### 5.4 验证设备架构

```bash
# 查看设备 CPU 架构
adb shell getprop ro.product.cpu.abi
```

---

## 六、注意事项

### 6.1 开发注意事项

1. **确保所有配置同步修改**
   - build.gradle
   - AndroidManifest.xml
   - androidx 库版本

2. **测试设备选择**
   - 优先在 arm64-v8a 设备上测试
   - 确保兼容性

3. **调试技巧**
   - 启用 `jniDebuggable true`
   - 关注 OpenNI 日志
   - 检查库文件是否正确安装

### 6.2 性能考虑

**优点**：
- APK 体积减少约 50%
- 减少内存占用

**缺点**：
- 在 arm64-v8a 设备上通过兼容模式运行，性能可能有轻微下降
- 32位地址空间限制（一般不影响）

### 6.3 后续维护

1. **添加新依赖时注意**
   - 确保新库支持 armeabi-v7a
   - 检查是否需要额外的配置

2. **升级 SDK 版本时**
   - 先在测试设备验证
   - 注意 androidx 库版本兼容性

3. **代码规范**
   - 使用 ToastUtils 统一显示 Toast
   - 在显示 UI 前检查 Activity 状态

---

## 七、完整配置示例

### 7.1 app/build.gradle (关键部分)

```gradle
android {
    namespace 'ywdemo.example.yaoxiaowen'
    compileSdk 29

    defaultConfig {
        applicationId "ywdemo.example.yaoxiaowen"
        minSdk 24
        targetSdk 29

        ndk {
            abiFilters "armeabi-v7a"
        }
    }

    buildTypes {
        debug {
            jniDebuggable true
        }
        release {
            minifyEnabled false
        }
    }

    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
            excludes += 'META-INF/annotations/**'
            excludes += '**/annotations/**'
        }
        // 排除 arm64-v8a 架构，仅保留 armeabi-v7a
        exclude 'lib/arm64-v8a/**'
        pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    }
}

dependencies {
    // 使用与 API 29 兼容的 androidx 库版本
    implementation 'androidx.core:core:1.3.2'
    implementation 'androidx.lifecycle:lifecycle-runtime:2.2.0'
    implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'com.google.android.material:material:1.2.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
}
```

### 7.2 AndroidManifest.xml (关键部分)

```xml
<application
    android:name=".DemoApplication"
    android:allowBackup="true"
    android:extractNativeLibs="true"  <!-- 关键配置 -->
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name">
    ...
</application>
```

---

## 八、常见问题 FAQ

### Q1: 为什么必须设置 android:extractNativeLibs="true"？

**A**: 在 arm64-v8a 设备上运行 armeabi-v7a 应用时，Android 系统默认不会将 32 位的 .so 库提取到文件系统。显式设置 `true` 可以强制提取，确保 OpenNI 等 native 库能够正确加载。

### Q2: 能否使用 compileSdk 33 或更高版本？

**A**: 理论上可以，但需要注意：
- androidx 库版本需要相应升级
- 可能遇到额外的兼容性问题
- 建议先在测试设备充分验证

### Q3: 如何确认项目已正确配置为仅支持 armeabi-v7a？

**A**: 按以下步骤验证：
1. 检查 APK 只包含 `lib/armeabi-v7a/` 目录
2. 安装到设备后，`/data/app/.../lib/arm/` 包含 .so 文件
3. 应用可以正常启动并加载 OpenNI 驱动

### Q4: 在其他 arm64-v8a 设备上会有问题吗？

**A**:
- 理论上不应该有问题
- Android 系统的 32 位兼容模式应该是一致的
- 建议在目标设备上测试验证

### Q5: 如果需要同时支持 arm64-v8a 和 armeabi-v7a 怎么办？

**A**:
- 移除 `exclude 'lib/arm64-v8a/**'`
- 移除或注释 `android:extractNativeLibs="true"`（使用默认值）
- 恢复 `abiFilters "armeabi-v7a", "arm64-v8a"`

---

## 九、相关文件清单

### 修改的文件
1. `app/build.gradle` - 构建配置
2. `app/src/main/AndroidManifest.xml` - 清单文件
3. `app/src/main/java/.../BdFaceDepthGateActivity.java` - 添加安全的 Toast 方法
4. `app/src/main/java/.../SaveImageManager.java` - 添加异常处理
5. `app/src/main/java/.../FaceQualityBack.java` - 添加状态检查
6. `app/src/main/java/.../ToastUtils.java` - 添加异常处理

### 参考文件
- `/Users/yaowen/Desktop/.../FaceSDKAndroid/` - 百度官方 Demo

---

## 十、总结

本次修改的核心在于理解 **Android 在 arm64-v8a 设备上运行 armeabi-v7a 应用时的特殊行为**。关键配置是 `android:extractNativeLibs="true"`，这个配置在大多数情况下可以省略，但在这种特定场景下必须显式设置。

**关键要点**：
1. ✅ 设置 `android:extractNativeLibs="true"`（最关键）
2. ✅ 使用 `exclude 'lib/arm64-v8a/**'` 排除其他架构
3. ✅ 降低 compileSdk 到 29 提高兼容性
4. ✅ 同步调整 androidx 库版本
5. ✅ 添加 UI 显示的安全检查

**验证通过**：
- ✅ APK 只包含 armeabi-v7a 库
- ✅ 在 arm64-v8a 设备上正常运行
- ✅ OpenNI 驱动正确加载
- ✅ 摄像头正常工作
