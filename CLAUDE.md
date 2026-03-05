# 项目指南 - YwTestDemo

## 项目简介
本项目是一个用于接入百度人脸SDK的Android演示项目，主要参考百度官方demo实现人脸识别相关功能。

**当前状态**：当前功能已经基本实现



## 主要关注范围
请只关注 `ywdemo.example.yaoxiaowen.baiduface` 包名下的内容，忽略其他测试类。

### 主要页面结构
- **BdStartActivity** - 启动页面
  - 负责SDK初始化和License认证
  - 初始配置加载（GateConfigUtils、RegisterConfigUtils）
  - 跳转到主页面 
  - 该页面 主要是鉴权,实现激活功能，在pad上实际运行时, 是在线激活方式，使用的是 激活码 "XALX-FRXM-JYWX-7SX6",
     激活代码相关是:  @app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/idl/face/main/activity/FaceSDKManager.java:155-175

- **BdHomeActivity** - 主页面
  - 人脸库数据库初始化和加载
  - 显示加载进度
  - 跳转到人脸识别页面

- **BdFaceDepthGateActivity** - 人脸识别深度门页面
  - OpenNI SDK初始化
  - 人脸识别功能实现
  - 该页面已经可以实现 完成活体检测功能。 @app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/BdFaceDepthGateActivity.kt:502-508, 
    该处的 onSaveSuccess 回调 实际运行 已经可以实现 保存图片了



## 参考资源
> 这些参考资源只在有必要时，再去读取内容进行参考
- **百度官方Demo路径**：`/Users/yaowen/Desktop/八维通/需求-App/pad-天津/0205-百度给的新demo/Baidu_Face_Offline_SDK_Android_8.5/Baidu_Face_Offline_SDK_Android_8.5/FaceSDKAndroid`
- **百度SDK官方文档**：https://ai.baidu.com/ai-doc/FACE/pk37c1mqu

## 技术栈
- Kotlin + Java 混合开发
- minSdk: 24, targetSdk: 33
- ViewBinding
- 百度人脸SDK (FaceSDK_8.5_20241205-release.aar)
- Orbbec 3D摄像头SDK (orbbec_module-debug.aar)
- MMKV 存储库
- 协程 (kotlinx-coroutines-android)

## 构建注意事项
- 支持 armeabi-v7a 和 arm64-v8a 架构
- 已处理 libc++_shared.so 重复问题
- 使用 multiDexEnabled true

## 当前分支
- **feature_baiduFaceSdk** - 百度人脸SDK集成功能分支
