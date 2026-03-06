# armeabi-v7a 单架构支持 - 工作进度记录

## 文档信息
- **日期**: 2026-03-05
- **目标**: 修改项目仅支持 armeabi-v7a 架构，减小 APK 体积
- **状态**: 代码修改已完成，等待设备测试
- **详细文档**: `docs/armeabi_v7a_only_implementation.md`（包含完整实施记录）

---

## 问题分析

### 核心问题
在 64 位设备上运行仅包含 armeabi-v7a 的应用时，OpenNI SDK 无法找到驱动文件。

### 根本原因

1. **32 位兼容模式运行**
   - 当 APK 只包含 armeabi-v7a 时，64 位设备会以 32 位兼容模式运行
   - `primaryCpuAbi=armeabi-v7a`

2. **OpenNI 驱动查找机制**
   - OpenNI 在初始化时会尝试查找驱动文件
   - 它尝试在以下路径查找：
     - `/data/data/{package}/lib/`（应用无写权限）
     - `/data/data/{package}/files/`（应用有写权限）
     - `/system/lib/`（系统库路径）

3. **nativeLibraryDir 为空**
   - Android 系统不会自动提取 OpenNI 的驱动文件到 nativeLibraryDir
   - OpenNI 的 Java 类没有静态初始化块来触发库的自动提取

---

## 解决方案

### 代码修改

**文件**: `app/src/main/java/.../BaseOrbbecActivity.java`

**修改内容**:
1. 在 `initializeOpenNI()` 之前调用 `extractOpenNIDrivers()`
2. `extractOpenNIDrivers()` 方法：
   - 在 `/data/data/{package}/files/lib/` 目录创建驱动文件存放位置
   - 从 APK 中提取 5 个必需的驱动文件：
     - libOniFile.so
     - liborbbec.so
     - liborbbecusb2.so
     - libOpenNI2.so
     - libOpenNI2.jni.so
   - 使用 `ZipFile` 直接读取 APK 内容并提取文件

**关键代码**:
```java
// 使用 files 目录，应用对这个目录有写权限
File targetDir = new File(context.getFilesDir(), "lib");

// 从 APK 中提取
String entryPath = "lib/armeabi-v7a/" + driver;
if (extractFileFromApk(apkPath, entryPath, targetFile)) {
    Log.d(TAG, "Extracted driver: " + driver);
}
```

---

## 当前配置

### app/build.gradle
```gradle
ndk {
    abiFilters "armeabi-v7a"  // 仅支持 armeabi-v7a
}

packagingOptions {
    jniLibs {
        excludes += '**/arm64-v8a/**'  // 排除 arm64-v8a
    }
    pickFirst 'lib/armeabi-v7a/libc++_shared.so'
}
```

### 目录结构
```
app/src/main/
├── assets/openni/
│   ├── OpenNI.ini
│   └── orbbec.ini
├── jniLibs/
│   └── armeabi-v7a/
│       ├── libOniFile.so
│       ├── libOpenNI2.jni.so
│       ├── libOpenNI2.so
│       ├── liborbbec.so
│       └── liborbbecusb2.so
```

---

## 明天测试步骤

### 1. 构建并安装 APK
```bash
cd /Users/yaowen/code/my_code/YwTestDemo
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. 启动应用并查看日志
```bash
adb logcat -c
adb shell am start -n ywdemo.example.yaoxiaowen/.baiduface.BdStartActivity
adb logcat | grep -E "(BaseOrbbecActivity|OpenNI|orbbec|Device|Extracted)"
```

### 3. 检查关键日志

**期望看到的成功日志**:
```
D BaseOrbbecActivity: Target driver path: /data/data/ywdemo.example.yaoxiaowen/files/lib
D BaseOrbbecActivity: Extracted driver: libOniFile.so
D BaseOrbbecActivity: Extracted driver: liborbbec.so
D BaseOrbbecActivity: Extracted driver: liborbbecusb2.so
D BaseOrbbecActivity: Extracted driver: libOpenNI2.so
D BaseOrbbecActivity: Extracted driver: libOpenNI2.jni.so
D BaseOrbbecActivity: OpenNI drivers extracted successfully (5/5)
V OpenNI  : Loading device driver 'libOniFile.so'...
V OpenNI  : Loading device driver 'liborbbec.so'...
D BaseOrbbecActivity: OpenNI initialized successfully
```

**如果失败，可能看到**:
```
E OpenNI  : Failed to get path: /data/data/.../files//libOniFile.so
W OpenNI  : Couldn't use file 'libOniFile.so' as a device driver
E BaseOrbbecActivity: Failed to initialize OpenNI
```

### 4. 验证摄像头功能
- 进入人脸识别页面
- 检查深度摄像头是否正常工作
- 检查人脸检测是否正常

---

## 可能的结果和后续处理

### 结果 A：完全成功 ✅
- OpenNI 初始化成功
- 摄像头正常工作
- **后续处理**：更新文档，确认 armeabi-v7a 单架构方案可行

### 结果 B：OpenNI 初始化失败
- **可能原因**：OpenNI 仍然无法找到驱动文件
- **后续处理**：
  1. 检查文件是否成功提取到 `/data/data/.../files/lib/`
  2. 尝试修改 OpenNI.ini 配置文件，指定驱动路径
  3. 考虑使用环境变量或其他方式指定驱动位置

### 结果 C：摄像头无法工作
- **可能原因**：Orbbec 摄像头在 32 位兼容模式下有其他问题
- **后续处理**：
  1. 查看详细错误日志
  2. 可能需要恢复双架构支持

---

## 相关文档
- **ABI 配置说明**: `docs/abi_configuration_summary.md`
- **之前的问题记录**: `docs/issue_openni_drivers_not_loaded.md`

---

## 变更记录

| 日期 | 操作 | 结果 |
|------|------|------|
| 2026-03-05 | 尝试仅支持 armeabi-v7a | OpenNI 驱动查找失败 |
| 2026-03-05 | 添加手动提取驱动逻辑 | 代码完成，待测试 |
| 2026-03-06 | （待测试） | （待定） |
