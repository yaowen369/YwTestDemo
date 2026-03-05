# 百度人脸SDK激活问题 - 工作记录

> **文档创建时间**：2025-01-30
> **工作分支**：feature_baiduFaceSdk
> **问题描述**：修改百度人脸SDK激活逻辑，使用设备指纹进行激活

---

## 一、问题背景

### 1.1 原始问题
项目中集成百度人脸SDK后，使用硬编码的激活码进行激活。根据百度官方文档说明，SDK激活是绑定到Android设备本身的，而不是单个应用。设备厂商已经使用激活码激活了设备，因此第三方应用应该使用设备指纹读取已存在的授权文件，而不是使用激活码。

### 1.2 官方文档说明
参考文档：https://ai.baidu.com/ai-doc/FACE/Zk37c1nnn

**激活方式说明**：
- **离线激活 (initLicenseOffLine)**：SDK会自动从设备读取授权文件，无需传入激活码
- **在线激活 (initLicenseOnLine)**：需要在线激活序列号
- **应用激活 (initLicenseBatchLine)**：需要应用激活序列号

对于厂商已激活的设备，应该优先使用离线激活方式。

---

## 二、已完成的工作

### 2.1 修改激活策略

**文件**：`app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/idl/face/main/activity/FaceSDKManager.java`

**修改内容**：完全重写了 `init()` 方法，实现三层激活策略：

```java
/**
 * 激活策略：
 * 1. 优先使用离线激活（适用于厂商已激活的设备）
 * 2. 如果离线激活失败，再尝试在线激活（如果有配置在线激活码）
 * 3. 最后尝试应用激活（如果有配置应用激活码）
 */
public void init(final Context context, final SdkInitListener listener) {
    // 先获取设备指纹（用于日志记录）
    String deviceFingerprint = new FaceAuth().getDeviceId(context);
    LogUtil.i(TAG, "设备指纹: " + deviceFingerprint);

    // 开始离线激活流程
    initLicenseOffLine(context, listener);
}
```

**关键改进**：
1. 移除了硬编码的激活码
2. 优先使用离线激活（SDK自动读取设备授权文件）
3. 离线激活失败后，尝试读取SharedPreferences中的在线激活码
4. 最后尝试应用激活码
5. 添加了详细的日志输出，便于调试

---

### 2.2 解决Native库缺失问题

#### 问题1：授权文件缺失
**现象**：
```
离线激活结果, code:1005, response=未找到授权文件,请将文件放到/storage/emulated/0目录
license.ini 存在: false
license.key 存在: false
License.zip 存在: false
```

**原因**：设备上没有百度人脸SDK的授权文件（license.ini/license.key/License.zip）

**授权文件位置**：
- 应该在：`/storage/emulated/0/` 目录下
- 或者：应用内部存储的 `context.getCacheDir()` 目录

---

#### 问题2：Native库缺失（主要问题）

**错误日志**：
```
java.lang.UnsatisfiedLinkError: dalvik.system.PathClassLoader[...] couldn't find "libbd_unifylicense.so"
    at com.baidu.idl.main.facesdk.FaceAuth.<init>(FaceAuth.java:44)
    at ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.BdFaceAuth.<init>(BdFaceAuth.java:38)
```

**原因分析**：
1. 检查 `FaceSDK_8.5_20241205-release.aar` 内容：
   - arm64-v8a: 只有 `libbd_facecollect_unifylicense.so` (383KB)
   - armeabi-v7a: 只有 `libbd_facecollect_unifylicense.so` (239KB)
   - x86: 有 `libbd_unifylicense.so` (1.1MB) ✓

2. 但是 `FaceAuth.java` 中尝试加载 `libbd_unifylicense.so`

**解决方案**：
将 `libbd_facecollect_unifylicense.so` 复制为 `libbd_unifylicense.so`

```bash
# 复制 arm64-v8a 架构
cp app/src/main/jniLibs/arm64-v8a/libbd_facecollect_unifylicense.so \
   app/src/main/jniLibs/arm64-v8a/libbd_unifylicense.so

# 复制 armeabi-v7a 架构
cp app/src/main/jniLibs/armeabi-v7a/libbd_facecollect_unifylicense.so \
   app/src/main/jniLibs/armeabi-v7a/libbd_unifylicense.so
```

**验证**：
```bash
ls -lh app/src/main/jniLibs/arm64-v8a/libbd_unifylicense.so
ls -lh app/src/main/jniLibs/armeabi-v7a/libbd_unifylicense.so
```

---

## 三、当前状态

### 3.1 已完成
- [x] 修改激活逻辑，使用设备指纹优先策略
- [x] 修复 libbd_unifylicense.so 缺失问题
- [x] 添加详细的日志输出

### 3.2 待确认
- [ ] 应用是否已经重新安装到设备上
- [ ] UnsatisfiedLinkError 是否已解决
- [ ] 授权文件是否存在

### 3.3 可能的后续问题
如果设备上确实没有授权文件，可能需要：
1. 联系设备厂商获取授权文件（license.ini + license.key）
2. 或配置在线激活码（通过 SharedPreferences 设置 "activate_online_key"）

---

## 四、相关文件清单

### 4.1 修改的文件
| 文件路径 | 说明 |
|---------|------|
| `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/idl/face/main/activity/FaceSDKManager.java` | SDK管理器，激活逻辑 |
| `app/src/main/jniLibs/arm64-v8a/libbd_unifylicense.so` | 复制的native库 |
| `app/src/main/jniLibs/armeabi-v7a/libbd_unifylicense.so` | 复制的native库 |

### 4.2 参考的文件
| 文件路径 | 说明 |
|---------|------|
| `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/AuthLibrary/example/authlibrary/BdFaceAuth.java` | 激活实现类 |
| `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/AuthLibrary/example/authlibrary/util/BdFileUtils.java` | 授权文件读取工具 |
| `baiduDocs/百度人脸离线识别SDK_Android 8.1.2_API接口文档.pdf` | 百度官方文档 |
| `docs/百度人脸SDK配置说明.md` | SDK配置说明文档 |

---

## 五、技术细节

### 5.1 激活流程图
```
开始
  ↓
获取设备指纹
  ↓
检查 /storage/emulated/0/ 下的授权文件
  ↓
┌─────────────────────────┐
│ 离线激活 (initLicenseOffLine) │
│ - license.ini            │
│ - license.key            │
│ - License.zip            │
└─────────────────────────┘
  ↓ 失败
┌─────────────────────────┐
│ 在线激活 (initLicenseOnLine)  │
│ - 需要激活序列号          │
│ - 从SharedPreferences读取  │
└─────────────────────────┘
  ↓ 失败
┌─────────────────────────┐
│ 应用激活 (initLicenseBatchLine)│
│ - 需要激活序列号          │
│ - 从SharedPreferences读取  │
└─────────────────────────┘
  ↓ 失败
激活失败，提示用户联系设备供应商
```

### 5.2 授权文件格式
**license.key**：单行字符串，包含授权key
```
example-key-content-here
```

**license.ini**：两行字符串，包含授权数据
```
first-line-data
second-line-data
```

### 5.3 SharedPreferences配置
| Key | 说明 | 示例 |
|-----|------|------|
| activate_offline_key | 离线激活key | 自动从授权文件读取 |
| activate_online_key | 在线激活码 | 需要手动配置 |
| activate_batchline_key | 应用激活码 | 需要手动配置 |

---

## 六、调试命令

### 6.1 构建和安装
```bash
# 清理构建
./gradlew clean

# 编译debug版本
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6.2 查看日志
```bash
# 查看激活相关日志
adb logcat | grep -E "FaceSDKManager|BdFaceAuth|FaceAuth"

# 查看错误日志
adb logcat | grep -E "Error|Exception|UnsatisfiedLinkError"
```

### 6.3 检查授权文件
```bash
# 检查外部存储
adb shell ls -l /storage/emulated/0/license.*
adb shell ls -l /storage/emulated/0/License.zip

# 查看应用内部存储
adb shell ls -l /data/data/ywdemo.example.yaoxiaowen/files/license.*
```

---

## 七、下一步计划

### 7.1 立即任务
1. **重新构建并安装应用**，验证native库问题是否解决
2. **查看日志**，确认激活流程是否正常
3. **检查授权文件状态**：
   - 如果文件存在：激活应该成功
   - 如果文件不存在：联系设备厂商或配置在线激活码

### 7.2 可能需要的后续工作
1. **获取授权文件**：
   - 联系设备厂商获取 license.ini 和 license.key
   - 将文件放到设备的 `/storage/emulated/0/` 目录

2. **配置在线激活**（如果没有授权文件）：
   ```java
   // 通过SharedPreferences设置在线激活码
   PreferencesUtil.putString("activate_online_key", "你的在线激活码");
   ```

3. **添加UI界面**（可选）：
   - 添加授权文件上传功能
   - 添加激活码配置界面

---

## 八、重要日志示例

### 8.1 成功的激活日志（预期）
```
FaceSDKManager: 设备指纹: XXXXXXXX
FaceSDKManager: 开始离线激活流程（SDK会自动读取设备授权文件）
FaceSDKManager: 外部存储路径: /storage/emulated/0
FaceSDKManager: license.ini 存在: true
FaceSDKManager: license.key 存在: true
FaceSDKManager: License.zip 存在: false
BdFaceAuth: SdCard: /storage/emulated/0
BdFaceAuth: bdface_create_instance status 0
FaceSDKManager: 离线激活结果, code:0, response=
FaceSDKManager: 离线激活成功
```

### 8.2 失败的激活日志（需要解决）
```
FaceSDKManager: 设备指纹: XXXXXXXX
FaceSDKManager: 开始离线激活流程
FaceSDKManager: license.ini 存在: false
FaceSDKManager: license.key 存在: false
FaceSDKManager: License.zip 存在: false
FaceSDKManager: 离线激活结果, code:1005, response=未找到授权文件
FaceSDKManager: 离线激活失败，尝试在线激活
FaceSDKManager: 未配置在线激活码，在线激活流程跳过
FaceSDKManager: 未配置应用激活码，所有激活方式均失败
```

---

## 九、参考资料

### 9.1 百度官方文档
- **激活文档**：https://ai.baidu.com/ai-doc/FACE/Zk37c1nnn
- **API文档**：`baiduDocs/百度人脸离线识别SDK_Android 8.1.2_API接口文档.pdf`

### 9.2 项目文档
- **SDK配置说明**：`docs/百度人脸SDK配置说明.md`
- **项目CLAUDE.md**：`CLAUDE.md`

---

## 十、联系方式备注
如果需要联系设备厂商获取授权文件，需要提供：
1. 设备指纹（Device ID）
2. 应用包名：`ywdemo.example.yaoxiaowen`
3. SDK版本：FaceSDK 8.5 (20241205)

可以通过以下代码获取设备指纹：
```java
String deviceFingerprint = new FaceAuth().getDeviceId(context);
Log.i(TAG, "设备指纹: " + deviceFingerprint);
```

---

> **文档结束** - 请根据实际进展更新本文档
