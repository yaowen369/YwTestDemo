# 百度人脸SDK配置说明文档

> **文档版本**：v1.0
> **更新日期**：2025-01-30
> **配置文件位置**：`/data/data/ywdemo.example.yaoxiaowen/files/Settings/gateFaceConfig.txt`
> **配置类路径**：`BaseConfig.java`

---

## 📋 目录

- [配置文件说明](#配置文件说明)
- [活体检测相关配置](#活体检测相关配置)
- [图片保存相关配置](#图片保存相关配置)
- [质量检测相关配置](#质量检测相关配置)
- [摄像头相关配置](#摄像头相关配置)
- [显示相关配置](#显示相关配置)
- [配置修改方法](#配置修改方法)

---

## 配置文件说明

配置文件采用 **JSON 格式**，在应用首次启动时自动创建。后续修改配置文件后，需要重启应用才能生效。

**配置文件路径**：
```
/data/data/ywdemo.example.yaoxiaowen/files/Settings/gateFaceConfig.txt
```

**配置类**：
```java
ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.model.BaseConfig
```

**配置工具类**：
```java
ywdemo.example.yaoxiaowen.baiduface.huajieLibrary.idl.main.huajie.utils.GateConfigUtils
```

---

## 活体检测相关配置

### 1. RGB活体阈值 (rgbLiveScore)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `rgbLiveScore` |
| **类型** | `float` |
| **默认值** | `0.80` (80分) |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:81` |

**说明**：
- RGB彩色图像的活体检测阈值
- 当活体得分 **大于** 该值时，判定为真人（`_Feature`）
- 当活体得分 **小于等于** 该值时，判定为攻击（`_Live`）

**调整建议**：
- 提高阈值（如 0.85）：检测更严格，减少误报，但可能漏检真人
- 降低阈值（如 0.75）：检测更宽松，更容易通过，但可能误判攻击

---

### 2. NIR活体阈值 (nirLiveScore)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `nirLiveScore` |
| **类型** | `float` |
| **默认值** | `0.80` (80分) |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:83` |

**说明**：
- 近红外（NIR）图像的活体检测阈值
- 当前设备（Pad）**没有NIR摄像头**，不会保存NIR图片

---

### 3. Depth活体阈值 (depthLiveScore)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `depthLiveScore` |
| **类型** | `float` |
| **默认值** | `0.80` (80分) |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:85` |

**说明**：
- 深度（Depth）图像的活体检测阈值
- Depth检测是最可靠的活体检测方式（照片/视频没有深度信息）
- **推荐优先使用Depth进行活体判断**

---

### 4. 活体检测开关 (livingControl)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `livingControl` |
| **类型** | `boolean` |
| **默认值** | `true` |
| **代码位置** | `BaseConfig.java:79` |

**说明**：
- 是否开启活体检测功能
- `true`：开启活体检测（推荐）
- `false`：关闭活体检测，所有图片都会被判定为真人

---

## 图片保存相关配置

### 1. 图片保存阈值 (saveImageThreshold)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `saveImageThreshold` |
| **类型** | `float` |
| **默认值** | `0.80` (80分) |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:87` |

**说明**：
- 控制是否保存图片的阈值
- **只有当RGB和Depth活体得分都大于该值时，才会保存图片**
- 这是一个"双保险"机制，确保保存的都是高质量真人图片

**逻辑代码** (`SaveImageManager.java:67-72`):
```java
// 只有当RGB和Depth活体得分都大于阈值时才保存图片
if (rgbScore <= saveThreshold || depthScore <= saveThreshold) {
    // 不满足保存条件，显示提示
    showToast(context, String.format("图片未保存：RGB(%.2f) Depth(%.2f) 阈值(%.2f)",
            rgbScore, depthScore, saveThreshold));
    return;
}
```

---

### 2. 媒体扫描开关 (enableMediaScan)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `enableMediaScan` |
| **类型** | `boolean` |
| **默认值** | `true` |
| **代码位置** | `BaseConfig.java:96` |

**说明**：
- 控制保存的图片是否在系统图库中可见
- `true`：保存的图片会在图库中显示（正常情况）
- `false`：保存的图片**不会**在图库中显示（仅保存在文件系统中）

**使用场景**：
- 开启(`true`)：需要用户在图库中查看保存的图片
- 关闭(`false`)：后台静默保存，不希望在图库中显示

**技术实现** (`BitmapUtils.java:539-560`):
```java
// 根据 enableMediaScan 配置决定是否通知媒体扫描器
if (context != null && jpgFile.exists() && SingleBaseConfig.getBaseConfig().isEnableMediaScan()) {
    MediaScannerConnection.scanFile(context, ...);
}
```

---

### 3. 图片保存路径和命名

**保存路径**：
```
{外部存储根目录}/Save-Image/{日期时间}/
```

**文件命名格式**：
```
{日期时间}_{图像类型}_{活体检测结果}.png
```

**示例**：
```
/storage/emulated/0/Save-Image/2025-01-30_14-25-36/
├── 2025-01-30_14-25-36_RGB_Feature.png    # RGB彩色图，通过活体检测
├── 2025-01-30_14-25-36_Depth_Feature.png   # Depth深度图，通过活体检测
```

**图像类型说明**：
| 类型 | 说明 | 人眼可见 |
|------|------|----------|
| **RGB** | 彩色可见光图像 | ✅ 是（普通照片） |
| **NIR** | 近红外图像 | ❌ 否（需红外摄像头） |
| **Depth** | 深度图像 | ✅ 是（但人眼无法理解3D数据） |

**活体检测结果说明**：
| 后缀 | 含义 | 条件 |
|------|------|------|
| **_Feature** | 通过活体检测 | 得分 > 阈值 |
| **_Live** | 未通过活体检测 | 得分 ≤ 阈值 |

---

## 质量检测相关配置

### 1. 质量检测开关 (qualityControl)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `qualityControl` |
| **类型** | `boolean` |
| **默认值** | `true` |
| **代码位置** | `BaseConfig.java:77` |

**说明**：
- 是否开启人脸质量检测
- `true`：检测人脸质量（清晰度、光照、角度等）
- `false`：跳过质量检测

---

### 2. 模糊度阈值 (blur)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `blur` |
| **类型** | `float` |
| **默认值** | `0.8` |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:31` |

**说明**：
- 图像模糊度阈值
- `0` = 最清晰，`1` = 最模糊
- 数值越大，允许的模糊程度越高

---

### 3. 光照阈值 (illum)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `illum` |
| **类型** | `float` |
| **默认值** | `0.8` |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:33` |

**说明**：
- 光照强度阈值
- 数值越大，要求的光线越强

---

### 4. 人脸角度阈值

| 配置项 | 字段名 | 默认值 | 取值范围 | 说明 |
|--------|--------|--------|----------|------|
| 俯仰角 | `pitch` | `20` | `[-90, 90]` | 上/下点头角度 |
| 平面旋转角 | `roll` | `20` | `[-180, 180]` | 左右歪头角度 |
| 左右旋转角 | `yaw` | `20` | `[-90, 90]` | 左右转头角度 |

---

### 5. 遮挡阈值

| 配置项 | 字段名 | 默认值 | 取值范围 | 说明 |
|--------|--------|--------|----------|------|
| 总体遮挡 | `occlusion` | `0.8` | `[0.0, 1.0]` | 整体遮挡阈值 |
| 左眼 | `leftEye` | `0.8` | `[0.0, 1.0]` | 左眼遮挡阈值 |
| 右眼 | `rightEye` | `0.8` | `[0.0, 1.0]` | 右眼遮挡阈值 |
| 鼻子 | `nose` | `0.8` | `[0.0, 1.0]` | 鼻子遮挡阈值 |
| 嘴巴 | `mouth` | `0.8` | `[0.0, 1.0]` | 嘴巴遮挡阈值 |
| 左脸颊 | `leftCheek` | `0.8` | `[0.0, 1.0]` | 左脸颊遮挡阈值 |
| 右脸颊 | `rightCheek` | `0.8` | `[0.0, 1.0]` | 右脸颊遮挡阈值 |
| 下巴 | `chinContour` | `0.8` | `[0.0, 1.0]` | 下巴遮挡阈值 |

---

### 6. 人脸完整度 (completeness)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `completeness` |
| **类型** | `float` |
| **默认值** | `1.0` |
| **取值范围** | `0.0 ~ 1.0` |
| **代码位置** | `BaseConfig.java:59` |

**说明**：
- `1.0`：人脸完全在图像边界内
- `0.0`：人脸溢出图像边界

---

## 摄像头相关配置

### 1. 摄像头类型 (cameraType)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `cameraType` |
| **类型** | `int` |
| **默认值** | `0` |
| **代码位置** | `BaseConfig.java:170` |

**可选值**：
| 值 | 摄像头型号 | 分辨率 |
|----|-----------|--------|
| 0 | 奥比中光海燕、大白 | 640*400 |
| 1 | 奥比中光海燕Pro、Atlas | 400*640 |
| 2 | 奥比中光蝴蝶、Astra Pro/Pro S | 640*480 |
| 3 | 舜宇Seeker06 | - |
| 4 | 螳螂慧视天蝎P1 | - |
| 5 | 瑞识M720N | - |
| 6 | 奥比中光Deeyea（结构光） | - |
| 7 | 华捷艾米A100S、A200（结构光） | - |
| 8 | Pico DCAM710（ToF） | - |

---

### 2. 摄像头分辨率配置

| 配置项 | 字段名 | 默认值 | 说明 |
|--------|--------|--------|------|
| RGB宽度 | `rgbAndNirWidth` | `640` | RGB/NIR摄像头宽度 |
| RGB高度 | `rgbAndNirHeight` | `480` | RGB/NIR摄像头高度 |
| Depth宽度 | `depthWidth` | `640` | Depth摄像头宽度 |
| Depth高度 | `depthHeight` | `400` | Depth摄像头高度 |

---

### 3. 摄像头方向配置

| 配置项 | 字段名 | 默认值 | 可选值 | 说明 |
|--------|--------|--------|--------|------|
| RGB视频方向 | `rgbVideoDirection` | `0` | `0, 90, 180, 270` | RGB预览旋转角度 |
| NIR视频方向 | `nirVideoDirection` | `0` | `0, 90, 180, 270` | NIR预览旋转角度 |
| RGB检测方向 | `rgbDetectDirection` | `0` | `0, 90, 180, 270` | RGB检测旋转角度 |
| NIR检测方向 | `nirDetectDirection` | `0` | `0, 90, 180, 270` | NIR检测旋转角度 |

---

### 4. 镜像配置

| 配置项 | 字段名 | 默认值 | 说明 |
|--------|--------|--------|------|
| RGB视频镜像 | `mirrorVideoRGB` | `0` | 0=无镜像，1=有镜像 |
| NIR视频镜像 | `mirrorVideoNIR` | `0` | 0=无镜像，1=有镜像 |
| RGB检测镜像 | `mirrorDetectRGB` | `0` | 0=无镜像，1=有镜像 |
| NIR检测镜像 | `mirrorDetectNIR` | `0` | 0=无镜像，1=有镜像 |

---

## 显示相关配置

### 1. OpenGL渲染开关 (isOpenGl)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `isOpenGl` |
| **类型** | `boolean` |
| **默认值** | `false` |
| **代码位置** | `BaseConfig.java:109` |

**说明**：
- 是否开启OpenGL渲染
- 建议开启后设置为 640*480 分辨率
- 可提升渲染性能

---

### 2. 调试模式开关 (debug)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `debug` |
| **类型** | `boolean` |
| **默认值** | `false` |
| **代码位置** | `BaseConfig.java:19` |

**说明**：
- 是否开启调试显示
- 开启后会在所有视频流识别页面显示调试信息

---

### 3. 日志开关 (log)

| 配置项 | 值 |
|--------|-----|
| **字段名** | `log` |
| **类型** | `boolean` |
| **默认值** | `true` |
| **代码位置** | `BaseConfig.java:104` |

**说明**：
- 是否开启Log日志输出

---

## 配置修改方法

### 方法1：修改配置文件（推荐）

1. **连接设备并进入文件管理**
   ```
   路径：/data/data/ywdemo.example.yaoxiaowen/files/Settings/gateFaceConfig.txt
   ```

2. **编辑JSON配置文件**
   ```json
   {
     "rgbLiveScore": 0.80,
     "depthLiveScore": 0.80,
     "saveImageThreshold": 0.80,
     "enableMediaScan": true
   }
   ```

3. **重启应用使配置生效**

---

### 方法2：修改代码默认值

1. **修改 `BaseConfig.java` 中的默认值**
   ```java
   // 示例：修改RGB活体阈值默认值
   private float rgbLiveScore = 0.85f;  // 原值 0.80f
   ```

2. **重新编译并安装应用**

3. **删除旧配置文件或调用 `GateConfigUtils.modityJson()` 重新生成配置**

---

## 常见问题排查

### 问题1：图片保存了但图库看不到

**可能原因**：`enableMediaScan` 配置为 `false`

**解决方法**：
1. 检查配置文件中 `enableMediaScan` 的值
2. 修改为 `true` 并重启应用
3. 或者直接查看文件系统中的图片

---

### 问题2：图片一直不保存

**可能原因**：活体得分未达到 `saveImageThreshold` 要求

**排查方法**：
1. 查看Toast提示中的实际得分
2. 临时降低 `saveImageThreshold` 值（如 0.60）进行测试
3. 检查RGB和Depth两个得分是否都大于阈值

---

### 问题3：活体检测太严格，真人经常无法通过

**解决方法**：
1. 降低 `rgbLiveScore` 和 `depthLiveScore` 阈值（如改为 0.70）
2. 检查摄像头是否有遮挡或角度问题
3. 确保光照条件良好

---

## 更新记录

| 版本 | 日期 | 更新内容 | 更新人 |
|------|------|----------|--------|
| v1.0 | 2025-01-30 | 初始版本，记录活体、图片保存相关配置 | - |

---

## 附录

### 相关文件路径

| 文件 | 路径 |
|------|------|
| 配置类 | `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/huajieLibrary/idl/main/huajie/model/BaseConfig.java` |
| 配置工具类 | `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/huajieLibrary/idl/main/huajie/utils/GateConfigUtils.java` |
| 图片保存管理器 | `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/datalibrary/example/datalibrary/manager/SaveImageManager.java` |
| 图片工具类 | `app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/datalibrary/example/datalibrary/utils/BitmapUtils.java` |

---

> **提示**：本文档为公共记录文档，后续添加新配置时请及时更新本文档。
