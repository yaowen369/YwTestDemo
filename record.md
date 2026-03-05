当前现状(0301 12:25)


项目运行 可以实现 拍照以及图片保存。

但是 无法使用设备指纹去激活。

怀疑和log中输出的 libbd_facecollect_unifylicense.so 文件找不到有关。


MMKV                    <MMKV.cpp:203::initializeMMKV> root dir: /data/user/0/ywdemo.example.yaoxiaowen/files/mmkv
LogPrefix               mmkv root: /data/user/0/ywdemo.example.yaoxiaowen/files/mmkv, 线程信息：Thread[main,5,main], id=1,  是否主线程: true
System.err              java.lang.UnsatisfiedLinkError: dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/ywdemo.example.yaoxiaowen-1/base.apk"],nativeLibraryDirectories=[/data/app/ywdemo.example.yaoxiaowen-1/lib/arm64, /data/app/ywdemo.example.yaoxiaowen-1/base.apk!/lib/arm64-v8a, /system/lib64, /vendor/lib64]]] couldn't find "libbd_unifylicense.so"
System.err               at java.lang.Runtime.loadLibrary0(Runtime.java:984)
System.err               at java.lang.System.loadLibrary(System.java:1562)
System.err               at com.baidu.idl.main.facesdk.FaceAuth.<init>(FaceAuth.java:44)
System.err               at com.baidu.idl.main.facesdk.FaceAuth.<init>(FaceAuth.java:32)
System.err               at ywdemo.example.yaoxiaowen.baiduface.AuthLibrary.example.authlibrary.BdFaceAuth.<init>(BdFaceAuth.java:38)
System.err               at ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager.<init>(FaceSDKManager.java:34)
System.err               at ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager.<init>(FaceSDKManager.java:17)
System.err               at ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager$HolderClass.<clinit>(FaceSDKManager.java:41)
System.err               at ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager$HolderClass.access$100(FaceSDKManager.java:40)
System.err               at ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager.getInstance(FaceSDKManager.java:45)
System.err               at ywdemo.example.yaoxiaowen.baiduface.BdStartActivity.initLicense(BdStartActivity.kt:63)
System.err               at ywdemo.example.yaoxiaowen.baiduface.BdStartActivity.onCreate(BdStartActivity.kt:59)
System.err               at android.app.Activity.performCreate(Activity.java:6709)
System.err               at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1118)
System.err               at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2628)
System.err               at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2736)
System.err               at android.app.ActivityThread.-wrap12(ActivityThread.java)
System.err               at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1487)
System.err               at android.os.Handler.dispatchMessage(Handler.java:102)
System.err               at android.os.Looper.loop(Looper.java:154)
System.err               at android.app.ActivityThread.main(ActivityThread.java:6157)
System.err               at java.lang.reflect.Method.invoke(Native Method)
System.err                at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:912)
System.err               at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:802)
FaceSDK --value--       <line 78: Java_com_baidu_idl_main_facesdk_FaceAuth_nativeSetActiveLog> 开启日志
System.err              java.lang.UnsatisfiedLinkError: dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/ywdemo.example.yaoxiaowen-1/base.apk"],nativeLibraryDirectories=[/data/app/ywdemo.example.yaoxiaowen-1/lib/arm64, /data/app/ywdemo.example.yaoxiaowen-1/base.apk!/lib/arm64-v8a, /system/lib64, /vendor/lib64]]] couldn't find "libbd_unifylicense.so"
System.err               at java.lang.Runtime.loadLibrary0(Runtime.java:984)
System.err               at java.lang.System.loadLibrary(System.java:1562)
System.err               at com.baidu.idl.main.facesdk.FaceAuth.<init>(FaceAuth.java:44)
System.err               at com.baidu.idl.main.facesdk.FaceAuth.<init>(FaceAuth.java:32)
System.err               at ywdemo.example.yaoxiaowen.baiduface.idl.face.main.activity.FaceSDKManager.init(FaceSDKManager.java:71)
System.err               at ywdemo.example.yaoxiaowen.baiduface.BdStartActivity.initLicense(BdStartActivity.kt:63)
System.err               at ywdemo.example.yaoxiaowen.baiduface.BdStartActivity.onCreate(BdStartActivity.kt:59)
System.err               at android.app.Activity.performCreate(Activity.java:6709)
System.err               at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1118)
System.err               at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2628)
System.err               at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2736)
System.err               at android.app.ActivityThread.-wrap12(ActivityThread.java)
System.err               at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1487)
System.err               at android.os.Handler.dispatchMessage(Handler.java:102)
System.err               at android.os.Looper.loop(Looper.java:154)
System.err               at android.app.ActivityThread.main(ActivityThread.java:6157)
System.err               at java.lang.reflect.Method.invoke(Native Method)
System.err               at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:912)
System.err               at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:802)
mple.yaoxiaowen         type=1400 audit(0.0:3322): avc: denied { read } for name="platform" dev="tmpfs" ino=8383 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:block_device:s0 tclass=dir permissive=1
mple.yaoxiaowen         type=1400 audit(0.0:3323): avc: denied { open } for path="/dev/block/platform" dev="tmpfs" ino=8383 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:block_device:s0 tclass=dir permissive=1
mple.yaoxiaowen         type=1400 audit(0.0:3324): avc: denied { read } for name="cid" dev="sysfs" ino=18716 scontext=u:r:untrusted_app:s0:c512,c768 tcont


然后 cluade帮忙加载这个so，

Claude记录如下:

1. 修改激活策略

修改了 FaceSDKManager.java，实现了三层激活策略：
1. 离线激活（优先） - SDK自动读取设备授权文件（适用于厂商已激活的设备）
2. 在线激活（备选） - 需要在线激活码
3. 应用激活（最后） - 需要应用激活码

2. 解决native库缺失问题

发现问题：FaceSDK_8.5_20241205-release.aar 中：
- arm64-v8a 和 armeabi-v7a 只有 libbd_facecollect_unifylicense.so
- 但代码尝试加载 libbd_unifylicense.so

已通过复制文件解决：
cp libbd_facecollect_unifylicense.so → libbd_unifylicense.so


但是实际上尝试 加载so之后，log并没有 UnsatisfiedLinkError 了，(并且这个时候，好像也没有离线激活成功)。 但是运行到主页面， 模型会加载失败 ，界面无法显示出RGB的图片.

BdFaceDepthGateActivity.kt 中会回掉 initModelFail
日志如下：


但是我把之前App卸载了，重新Run， 然后进入 @app/src/main/java/ywdemo/example/yaoxiaowen/baiduface/BdFaceDepthGateActivity.kt ，却无法正常工作了。

log日志如下：
LogPrefix BdFaceActy    initModelFail() -> -1  抠图能力加载失败 instanceIndex=0
face_model              FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
face_model              FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
face_model              FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
face_model              FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> -1  暗光恢复能力加载失败 instanceIndex=0
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败
LogPrefix BdFaceActy    initModelFail() -> 1  FaceInstance初始化失败


所以我又把 libbd_unifylicense.so内容给删除了

