# armeabi-v7a 单架构支持 - 完整实施记录

## 文档信息
- **创建日期**: 2026-03-05
- **最后更新**: 2026-03-05
- **目标**: 修改项目仅支持 armeabi-v7a 架构，减小 APK 体积
- **当前状态**: 代码修改已完成，等待设备测试

---

## 一、背景和目标

### 1.1 原始需求
用户希望将项目从支持 `armeabi-v7a` 和 `arm64-v8a` 两种架构，改为**仅支持 armeabi-v7a**，以减小 APK 体积。

### 1.2 当前配置
- **设备**: 64 位 ARM 设备 (arm64-v8a)
- **摄像头**: Orbbec 深度摄像头 (基于 OpenNI2 框架)
- **SDK**:
  - 百度人脸 SDK: FaceSDK_8.5_20241205-release.aar
  - Orbbec SDK: orbbec_module-debug.aar

### 1.3 预期效果
- APK 体积减小约 40%（去除 arm64-v8a 库）
- 在 64 位设备上以 32 位兼容模式运行
- 摄像头和人脸识别功能正常工作

---

## 二、问题分析过程

### 2.1 第一次尝试：仅配置 build.gradle

**修改内容**:
```gradle
// app/build.gradle
ndk {
    abiFilters "armeabi-v7a"  // 从 "armeabi-v7a", "arm64-v8a" 改为仅 "armeabi-v7a"
}

packagingOptions {
    jniLibs {
        excludes += '**/arm64-v8a/**'
    }
}
```

**删除内容**:
- `app/src/main/jniLibs/arm64-v8a/` 目录

**测试结果**:
```
E OpenNI  : Found no valid drivers
E BaseOrbbecActivity: Failed to initialize OpenNI
E LogPrefix BdFaceActy: onDeviceOpenFailed, msg:Device is null after initialization
```

**结论**: ❌ 失败，摄像头无法初始化

### 2.2 第二次尝试：简化代码，参考官方 demo

**修改内容**:
将 `BaseOrbbecActivity.java` 简化为与百度官方 demo 相同的实现：
```java
public void initializeOpenNI() {
    OpenNI.setLogAndroidOutput(true);
    OpenNI.setLogMinSeverity(0);
    OpenNI.initialize();
}
```

**测试结果**: ❌ 仍然失败

**日志分析**:
```
I OpenNI  : Using '/data/app/.../base.apk!/lib/armeabi-v7a/' as driver path
E OpenNI  : Failed to get path: /data/data/ywdemo.example.yaoxiaowen/lib//libOniFile.so
E OpenNI  : Failed to get path: /data/data/ywdemo.example.yaoxiaowen/files//libOniFile.so
E OpenNI  : Failed to get path libOniFile.so for lib: /system/lib/libOniFile.so
W OpenNI  : Couldn't use file 'libOniFile.so' as a device driver
E OpenNI  : Found no valid drivers
```

**关键发现**:
1. OpenNI 识别到 APK 中的驱动路径：`base.apk!/lib/armeabi-v7a/`
2. 但它在文件系统中查找驱动的路径都不正确
3. nativeLibraryDir (`/data/app/.../lib/arm/`) 是空的

### 2.3 第三次尝试：显式加载库

**修改内容**:
添加静态初始化块显式加载 OpenNI 库：
```java
static {
    System.loadLibrary("OpenNI2");
    System.loadLibrary("OniFile");
    System.loadLibrary("orbbec");
    System.loadLibrary("orbbecusb2");
}
```

**测试结果**: ❌ 库加载成功，但 OpenNI.initialize() 仍然失败

**分析**:
- `System.loadLibrary()` 将库加载到内存中（无 UnsatisfiedLinkError）
- 但 OpenNI 的驱动查找机制需要在**文件系统**中找到库文件
- nativeLibraryDir 仍然为空

### 2.4 第四次尝试：手动提取驱动文件（最终方案）

**分析**:
从日志可以看到 OpenNI 尝试在 `/data/data/.../files/` 目录查找驱动文件。应用对这个目录有写权限，可以手动将驱动文件提取到这里。

**解决方案**:
在 `OpenNI.initialize()` 之前，从 APK 中提取驱动文件到 `/data/data/{package}/files/lib/` 目录。

---

## 三、最终解决方案

### 3.1 代码实现

**文件**: `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/datalibrary/example/datalibrary/activity/BaseOrbbecActivity.java`

```java
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
     * 初始化 OpenNI SDK
     *
     * 说明：
     * 1. 在 32 位兼容模式下运行时，OpenNI 无法自动找到驱动文件
     * 2. 需要手动将驱动文件从 APK 提取到 /data/data/.../files/ 目录
     * 3. 这样 OpenNI 才能找到并加载驱动
     */
    public void initializeOpenNI() {
        try {
            Log.d(TAG, "Starting OpenNI initialization...");

            // 首先提取驱动文件到应用可访问的目录
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
     * 将 OpenNI 驱动文件从 APK 中提取到应用数据目录
     *
     * 重要：在 32 位兼容模式下，OpenNI 在 /data/data/.../files/ 目录查找驱动
     */
    private synchronized void extractOpenNIDrivers() {
        if (driversExtracted) {
            Log.d(TAG, "Drivers already extracted, skipping...");
            return;
        }

        try {
            Context context = getApplicationContext();
            // 使用 files 目录，应用对这个目录有写权限
            File targetDir = new File(context.getFilesDir(), "lib");
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                Log.d(TAG, "Target dir created: " + created);
            }

            Log.d(TAG, "Target driver path: " + targetDir.getAbsolutePath());

            // OpenNI 需要的驱动文件列表
            String[] driverFiles = {
                "libOniFile.so",
                "liborbbec.so",
                "liborbbecusb2.so",
                "libOpenNI2.so",
                "libOpenNI2.jni.so"
            };

            int copiedCount = 0;
            String apkPath = context.getPackageCodePath();

            for (String driver : driverFiles) {
                File targetFile = new File(targetDir, driver);
                if (!targetFile.exists()) {
                    // 从 APK 中提取（仅尝试 armeabi-v7a，因为应用只支持这个架构）
                    String entryPath = "lib/armeabi-v7a/" + driver;
                    if (extractFileFromApk(apkPath, entryPath, targetFile)) {
                        Log.d(TAG, "Extracted driver: " + driver);
                        copiedCount++;
                    } else {
                        Log.e(TAG, "Failed to extract driver: " + driver);
                    }
                } else {
                    Log.d(TAG, "Driver already exists: " + driver);
                    copiedCount++;
                }
            }

            driversExtracted = true;
            Log.d(TAG, "OpenNI drivers extracted successfully (" + copiedCount + "/" + driverFiles.length + ")");
        } catch (Exception e) {
            Log.e(TAG, "Error extracting OpenNI drivers", e);
            driversExtracted = true; // 标记为已处理，避免重复尝试
        }
    }

    /**
     * 从APK文件中提取指定文件
     */
    private boolean extractFileFromApk(String apkPath, String entryPath, File targetFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkPath);
            ZipEntry entry = zipFile.getEntry(entryPath);
            if (entry == null) {
                Log.w(TAG, "Entry not found in APK: " + entryPath);
                return false;
            }

            InputStream is = zipFile.getInputStream(entry);
            try (FileOutputStream os = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
            }
            is.close();
            Log.d(TAG, "Successfully extracted: " + entryPath + " to " + targetFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting file from APK: " + entryPath, e);
            return false;
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }
}
```

### 3.2 配置文件

**app/build.gradle**:
```gradle
android {
    defaultConfig {
        // 仅支持 armeabi-v7a 架构
        ndk {
            abiFilters "armeabi-v7a"
        }
    }

    packagingOptions {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
            excludes += 'META-INF/annotations/**'
            excludes += '**/annotations/**'
        }
        jniLibs {
            // 确保 APK 中不包含 arm64-v8a 的库
            excludes += '**/arm64-v8a/**'
        }
        // 处理libc++_shared.so重复问题
        pickFirst 'lib/armeabi-v7a/libc++_shared.so'
    }
}
```

### 3.3 目录结构

```
app/src/main/
├── assets/openni/
│   ├── OpenNI.ini      # OpenNI 配置文件（由 OpenNIHelper 自动提取）
│   └── orbbec.ini      # Orbbec 驱动配置文件
├── jniLibs/
│   └── armeabi-v7a/    # 仅保留 32 位库
│       ├── libOniFile.so
│       ├── libOpenNI2.jni.so
│       ├── libOpenNI2.so
│       ├── liborbbec.so
│       └── liborbbecusb2.so
```

---

## 四、技术原理说明

### 4.1 为什么 OpenNI 找不到驱动？

1. **Android 的库提取机制**
   - Android 系统会在安装 APK 时自动提取 `.so` 文件到 `nativeLibraryDir`
   - 但这只对通过 `System.loadLibrary()` 显式加载的库有效
   - OpenNI 的驱动文件没有被 Java 代码显式加载，所以不会被自动提取

2. **32 位兼容模式的路径问题**
   - 64 位设备运行 32 位应用时，nativeLibraryDir 路径为：`/data/app/{package}/lib/arm/`
   - OpenNI 的驱动查找逻辑硬编码了查找路径，不知道兼容模式的特殊路径
   - 它尝试查找：
     - `/data/data/{package}/lib/`（不存在）
     - `/data/data/{package}/files/`（我们的目标路径）

3. **解决方案的原理**
   - 手动从 APK 中提取驱动文件
   - 存放到 OpenNI 会查找的路径：`/data/data/{package}/files/lib/`
   - 应用对这个路径有写权限

### 4.2 与官方 demo 的区别

**百度官方 demo**:
- 同时支持 `armeabi-v7a` 和 `arm64-v8a`
- 在 64 位设备上使用 arm64-v8a 库
- OpenNI 可以在正确的路径找到驱动文件

**我们的修改**:
- 仅支持 `armeabi-v7a`
- 在 64 位设备上以 32 位兼容模式运行
- 需要手动提取驱动到 OpenNI 可找到的路径

---

## 五、明天测试步骤

### 5.1 准备工作

确保以下内容：
- [ ] 设备已连接（64 位 ARM 设备）
- [ ] Orbbec 深度摄像头已连接
- [ ] 代码已编译成功

### 5.2 测试命令

```bash
# 1. 进入项目目录
cd /Users/yaowen/code/my_code/YwTestDemo

# 2. 清理并重新构建（可选）
./gradlew clean assembleDebug

# 3. 卸载旧版本（可选）
adb uninstall ywdemo.example.yaoxiaowen

# 4. 安装新版本
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. 清空日志
adb logcat -c

# 6. 启动应用
adb shell am start -n ywdemo.example.yaoxiaowen/.baiduface.BdStartActivity

# 7. 等待几秒后查看日志
sleep 5
adb logcat -d | grep -E "(BaseOrbbecActivity|OpenNI|Extracted|Device|orbbec)" | head -100
```

### 5.3 关键日志检查点

#### ✅ 成功的标志：

```
# 1. 驱动提取成功
D BaseOrbbecActivity: Target driver path: /data/data/ywdemo.example.yaoxiaowen/files/lib
D BaseOrbbecActivity: Extracted driver: libOniFile.so
D BaseOrbbecActivity: Extracted driver: liborbbec.so
D BaseOrbbecActivity: Extracted driver: liborbbecusb2.so
D BaseOrbbecActivity: Extracted driver: libOpenNI2.so
D BaseOrbbecActivity: Extracted driver: libOpenNI2.jni.so
D BaseOrbbecActivity: OpenNI drivers extracted successfully (5/5)

# 2. OpenNI 初始化成功
V OpenNI  : Loading device driver 'libOniFile.so'...
V OpenNI  : Loaded module libOniFile.so
V OpenNI  : Loading device driver 'liborbbec.so'...
V OpenNI  : Loaded module liborbbec.so
D BaseOrbbecActivity: OpenNI initialized successfully

# 3. 设备枚举成功
V OpenNIHelper: vid: 2bc5, pid: 60e
D BdFaceDepthGate: enumerateDevices: 1 devices
```

#### ❌ 失败的标志：

```
# 驱动提取失败
E BaseOrbbecActivity: Failed to extract driver: libOniFile.so
D BaseOrbbecActivity: OpenNI drivers extracted successfully (0/5) 或少于 5/5

# OpenNI 找不到驱动
E OpenNI  : Failed to get path: /data/data/.../files//libOniFile.so
W OpenNI  : Couldn't use file 'libOniFile.so' as a device driver
E OpenNI  : Found no valid drivers
E BaseOrbbecActivity: Failed to initialize OpenNI

# 设备打开失败
E LogPrefix BdFaceActy: onDeviceOpenFailed, msg:Device is null after initialization
```

### 5.4 功能验证

如果 OpenNI 初始化成功，继续验证：

1. **进入人脸识别页面**
   - 点击进入人脸识别功能
   - 检查是否正常跳转

2. **检查深度摄像头**
   - 观察摄像头预览是否正常
   - 检查深度数据是否正常显示

3. **人脸检测功能**
   - 对着摄像头进行人脸检测
   - 检查人脸框是否正确显示

4. **活体检测**
   - 测试活体检测功能是否正常
   - 检查是否可以正常识别真人

---

## 六、可能的结果和处理方案

### 结果 A：完全成功 ✅

**表现**:
- OpenNI 驱动提取成功 (5/5)
- OpenNI 初始化成功
- 摄像头正常工作
- 人脸检测和活体检测正常

**后续处理**:
1. ✅ 确认 armeabi-v7a 单架构方案可行
2. 更新 `docs/abi_configuration_summary.md` 文档
3. 进行完整的回归测试
4. 对比 APK 体积变化
5. 提交代码到版本控制

---

### 结果 B：驱动提取失败

**表现**:
```
E BaseOrbbecActivity: Failed to extract driver: libOniFile.so
```

**可能原因**:
1. APK 中没有对应的文件
2. 文件路径不正确
3. 权限问题

**处理方案**:

1. **检查 APK 内容**:
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/armeabi-v7a/.*\.so"
```

2. **检查文件权限**:
```bash
adb shell run-as ywdemo.example.yaoxiaowen ls -la files/
```

3. **检查文件是否存在**:
```bash
adb shell run-as ywdemo.example.yaoxiaowen ls -la files/lib/
```

4. **查看详细错误日志**:
```bash
adb logcat -d | grep "Error extracting file from APK"
```

---

### 结果 C：驱动提取成功，但 OpenNI 找不到

**表现**:
```
D BaseOrbbecActivity: OpenNI drivers extracted successfully (5/5)
E OpenNI  : Failed to get path: /data/data/.../files//libOniFile.so
```

**处理方案**:

1. **检查文件是否真的存在**:
```bash
adb shell run-as ywdemo.example.yaoxiaowen ls -la files/lib/
```

2. **尝试修改 OpenNI.ini 配置**:
在 `app/src/main/assets/openni/OpenNI.ini` 中添加：
```ini
[Drivers]
Repository=/data/data/ywdemo.example.yaoxiaowen/files/lib
```

3. **尝试设置环境变量**（需要在 Java 代码中）:
```java
// 在 extractOpenNIDrivers() 之后添加
String driverPath = new File(context.getFilesDir(), "lib").getAbsolutePath();
System.load(driverPath + "/libOpenNI2.so");
```

4. **检查文件权限**:
```bash
adb shell run-as ywdemo.example.yaoxiaowen ls -l files/lib/libOniFile.so
# 应该显示 -rw-rw-r-- (可读写)
```

---

### 结果 D：OpenNI 初始化成功，但摄像头无法工作

**表现**:
```
D BaseOrbbecActivity: OpenNI initialized successfully
D BdFaceDepthGate: enumerateDevices: 0 devices
```

**处理方案**:

1. **检查 USB 设备连接**:
```bash
adb shell lsusb | grep -i orbbec
# 或
adb shell dmesg | grep -i usb
```

2. **检查设备权限**:
```bash
adb shell ls -l /dev/bus/usb/
```

3. **查看完整 OpenNI 日志**:
```bash
adb logcat -d | grep -E "OpenNI|orbbec|USB|Device" | grep -v "AndroidRuntime"
```

4. **尝试恢复双架构支持**:
   如果 32 位兼容模式确实无法正常工作，可能需要恢复支持 arm64-v8a

---

## 七、调试技巧

### 7.1 常用调试命令

```bash
# 检查应用运行模式
adb shell "dumpsys package ywdemo.example.yaoxiaowen | grep primaryCpuAbi"

# 检查 nativeLibraryDir
adb shell "run-as ywdemo.example.yaoxiaowen ls -la lib/"

# 检查提取的驱动文件
adb shell "run-as ywdemo.example.yaoxiaowen ls -la files/lib/"

# 检查 APK 内容
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/.*\.so"

# 查看 OpenNI 详细日志
adb logcat -d | grep "OpenNI" | grep -v "AndroidRuntime"

# 检查设备枚举
adb logcat -d | grep "enumerateDevices"

# 检查 USB 设备
adb shell dmesg | grep -i "usb"
```

### 7.2 日志过滤

```bash
# 只看 OpenNI 相关日志
adb logcat -v time | grep "OpenNI"

# 只看驱动提取日志
adb logcat -v time | grep "BaseOrbbecActivity"

# 只看错误日志
adb logcat -v time *:E
```

---

## 八、文件清单

### 8.1 修改的文件

1. **app/build.gradle** - 修改 ABI 配置
2. **app/src/main/java/.../BaseOrbbecActivity.java** - 添加驱动提取逻辑
3. **app/src/main/assets/openni/OpenNI.ini** - OpenNI 配置文件（已存在）
4. **app/src/main/assets/openni/orbbec.ini** - Orbbec 配置文件（已存在）

### 8.2 删除的文件/目录

1. **app/src/main/jniLibs/arm64-v8a/** - 64 位库目录（已删除）

### 8.3 新建的文档

1. **docs/armeabi_v7a_only_implementation.md** - 本文档
2. **docs/armeabi_v7a_only_progress.md** - 简要进度记录

---

## 九、相关参考

### 9.1 百度官方 demo
- **路径**: `/Users/yaowen/Desktop/八维通/需求-App/pad-天津/0205-百度给的新demo/Baidu_Face_Offline_SDK_Android_8.5/Baidu_Face_Offline_SDK_Android_8.5/FaceSDKAndroid/`
- **配置**: 同时支持 armeabi-v7a 和 arm64-v8a

### 9.2 相关文档
- **ABI 配置说明**: `docs/abi_configuration_summary.md`
- **OpenNI 驱动问题记录**: `docs/issue_openni_drivers_not_loaded.md`

### 9.3 技术资料
- OpenNI 2.3.0 Android 版本
- Orbbec 深度摄像头 SDK
- Android ABI 兼容性文档

---

## 十、总结

### 10.1 关键发现

1. **OpenNI 在 32 位兼容模式下的驱动查找问题**
   - OpenNI 的驱动查找逻辑不兼容 32 位兼容模式
   - 需要手动将驱动文件提取到 `/data/data/{package}/files/lib/` 目录

2. **Android 库提取机制的局限性**
   - 只有通过 `System.loadLibrary()` 显式加载的库才会被自动提取
   - OpenNI 的驱动文件没有被 Java 代码显式加载

3. **解决方案的有效性**
   - 手动提取驱动文件到 OpenNI 可找到的路径
   - 应用对 `/data/data/{package}/files/` 目录有写权限

### 10.2 待验证内容

- [ ] 驱动文件是否能成功提取
- [ ] OpenNI 是否能找到并加载驱动
- [ ] 摄像头是否能正常工作
- [ ] 人脸检测和活体检测是否正常
- [ ] APK 体积是否明显减小

### 10.3 风险评估

**低风险**:
- 驱动提取逻辑简单直接
- 使用标准 Java API，无特殊依赖
- 文件操作有完善的异常处理

**中风险**:
- OpenNI 可能还有其他路径查找逻辑
- 某些设备可能有不同的权限配置

**高风险**:
- 如果此方案失败，可能需要恢复双架构支持
- APK 体积优化的目标无法实现

---

## 附录：完整的工作日志

### 2026-03-05 工作记录

#### 上午（10:00-12:00）
- 分析用户需求：仅支持 armeabi-v7a 架构
- 修改 build.gradle 配置
- 删除 arm64-v8a 目录
- 第一次测试：失败，摄像头无法初始化

#### 下午（14:00-16:00）
- 研究百度官方 demo 实现
- 第二次测试：简化代码，仍然失败
- 分析日志，发现 OpenNI 找不到驱动文件
- 检查 nativeLibraryDir，发现为空

#### 下午（16:00-18:00）
- 第三次尝试：显式加载库
- 库加载成功，但 OpenNI.initialize() 仍然失败
- 深入分析 OpenNI 驱动查找机制
- 确认需要在文件系统中提供驱动文件

#### 晚上（19:00-21:00）
- 第四次尝试：手动提取驱动文件
- 实现从 APK 提取驱动到 `/data/data/{package}/files/lib/`
- 代码修改完成
- 创建详细文档
- 等待明天设备测试

---

**文档结束**

创建日期: 2026-03-05
最后更新: 2026-03-05
创建人: Claude Code
状态: 等待测试
