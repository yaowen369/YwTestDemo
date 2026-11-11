package ywdemo.example.yaoxiaowen

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File
import java.io.FileOutputStream
import java.time.Duration
import java.util.concurrent.TimeUnit

class TestStationApplication : StationApplication() {
    override fun currentTimeMillis(): Long = 1_794_355_200_000L + SystemClock.elapsedRealtime()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestStationApplication::class, qualifiers = "zh-rCN-w320dp-h509dp-hdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StationAndroidTest {
    private lateinit var app: StationApplication
    @Before fun setup() {
        app = RuntimeEnvironment.getApplication() as StationApplication
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(true)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }
    private fun settle() { shadowOf(Looper.getMainLooper()).idle() }
    private fun tree(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { tree(view.getChildAt(it)) } else emptyList()
    private fun root(activity: Activity): View = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
    private fun textView(activity: Activity, label: String): TextView = tree(root(activity)).filterIsInstance<TextView>().first { it.text.toString().startsWith(label) }
    private fun click(activity: Activity, label: String) {
        var view: View = textView(activity, label)
        while (!view.isClickable) view = view.parent as View
        assertTrue("控件应可用：$label", view.isEnabled); view.performClick(); settle()
    }
    private fun contains(activity: Activity, text: String) = tree(root(activity)).filterIsInstance<TextView>().any { it.text.toString().contains(text) }
    private fun edit(activity: Activity, value: String) { tree(root(activity)).filterIsInstance<EditText>().single().setText(value); settle() }
    private fun enterForm(activity: Activity, kind: String) { click(activity, "垃圾桶满溢"); click(activity, "开始处理"); click(activity, "填写结果"); click(activity, kind) }
    private fun measure(activity: Activity, width: Int = 480, height: Int = 764) {
        root(activity).measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root(activity).layout(0, 0, width, height); settle()
    }
    private fun snapshot(activity: Activity, name: String, width: Int = 480, height: Int = 764) {
        app.images.submit {}.get(20, TimeUnit.SECONDS); settle(); measure(activity, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root(activity).draw(Canvas(bitmap))
        val directory = File("build/verification-native").apply { mkdirs() }
        FileOutputStream(File(directory, "$name.png")).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    private fun sourceImage(): File {
        val file = File(app.cacheDir, "test-photo.png")
        val bitmap = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.GREEN)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle(); return file
    }
    @Test fun nativeMainFlowStartsInDetailAndCompletesPartsWithoutPhotos() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        assertTrue(contains(activity, "智慧车站")); assertTrue(contains(activity, "待处理 3")); snapshot(activity, "01-我的工单-320dp")
        click(activity, "垃圾桶满溢"); snapshot(activity, "02-工单详情-320dp"); click(activity, "开始处理")
        assertTrue(contains(activity, "工单详情")); assertTrue(contains(activity, "填写结果")); assertFalse(contains(activity, "选择现场结果"))
        click(activity, "填写结果"); snapshot(activity, "03-选择结果-320dp"); click(activity, "需配件支持")
        click(activity, "提交并完成"); assertTrue(contains(activity, "请填写配件类型")); assertEquals(OrderStatus.PROCESSING, app.repository.state.orders.first().status)
        edit(activity, "箱门合页"); snapshot(activity, "04-填写配件-320dp"); click(activity, "提交并完成")
        assertTrue(contains(activity, "正在提交")); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_500))
        assertTrue(contains(activity, "完成记录")); assertTrue(contains(activity, "系统默认验收通过")); assertTrue(contains(activity, "仍需配件支持"))
        snapshot(activity, "05-完成记录-320dp"); controller.pause().stop().destroy()
    }
    @Test fun albumImportCopiesReadableFileAndSupportsRemoveUndoAndSubmit() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        enterForm(activity, "已处理"); edit(activity, "已清理桶内垃圾，并整理周边摆放。"); click(activity, "从相册选择")
        val launch = shadowOf(activity).nextStartedActivityForResult
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, launch.intent.action)
        val original = sourceImage()
        shadowOf(activity).receiveResult(launch.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(original)))
        app.io.submit {}.get(20, TimeUnit.SECONDS); settle()
        val order = app.repository.state.orders.first(); assertEquals(1, order.branch().photos.size)
        val copy = app.photos.file(order.branch().photos.single().fileName)
        assertTrue(copy.isFile); assertNotEquals(original.path, copy.path)
        assertTrue(contains(activity, "待上传")); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600)); assertTrue(contains(activity, "正在上传"))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_500)); assertTrue(contains(activity, "上传成功"))
        click(activity, "移除"); assertEquals(0, app.repository.state.orders.first().branch().photos.size); assertTrue(original.isFile); assertTrue(copy.isFile)
        click(activity, "撤销"); assertEquals(1, app.repository.state.orders.first().branch().photos.size)
        snapshot(activity, "06-填写处理材料-320dp"); click(activity, "提交并完成")
        tree(root(activity)).first { it.contentDescription == "查看第 1 张照片" }.performClick(); settle()
        assertTrue(contains(activity, "照片预览")); click(activity, "放大 / 还原")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_500))
        activity.onBackPressed(); settle()
        assertTrue(contains(activity, "完成记录")); assertTrue(original.isFile)
        controller.pause().stop().destroy()
    }
    @Test fun cameraUsesFileProviderAndCancellationPreservesText() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        shadowOf(app.packageManager).addResolveInfoForIntent(cameraIntent, ResolveInfo().apply { activityInfo = ActivityInfo().apply { packageName = "test.camera"; name = "CameraActivity"; exported = true } })
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        enterForm(activity, "核查无异常"); edit(activity, "现场未发现异常"); click(activity, "拍照")
        val launch = shadowOf(activity).nextStartedActivityForResult
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, launch.intent.action)
        val uri = launch.intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
        assertEquals("content", uri.scheme); assertEquals(app.packageName + ".photos", uri.authority)
        shadowOf(activity).receiveResult(launch.intent, Activity.RESULT_CANCELED, null); settle()
        assertEquals("现场未发现异常", app.repository.state.orders.first().branch().text); assertTrue(app.repository.state.orders.first().branch().photos.isEmpty())
        click(activity, "拍照"); val second = shadowOf(activity).nextStartedActivityForResult
        val output = second.intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)!!
        app.contentResolver.openOutputStream(output)!!.use { it.write(sourceImage().readBytes()) }
        shadowOf(activity).receiveResult(second.intent, Activity.RESULT_OK, null); app.io.submit {}.get(20, TimeUnit.SECONDS); settle()
        assertEquals("拍照", app.repository.state.orders.first().branch().photos.single().source)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2_000)); click(activity, "提交并完成")
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_500))
        assertTrue(contains(activity, "完成记录")); assertEquals(ResultKind.NORMAL, app.repository.state.orders.first().selected)
        controller.pause().stop().destroy()
    }
    @Test fun recreationPreservesSelectedBranchAndDraft() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        enterForm(activity, "已处理"); edit(activity, "已保存的现场处理说明"); click(activity, "更换结果"); click(activity, "需配件支持"); edit(activity, "闭门器")
        controller.recreate(); settle()
        assertTrue(contains(controller.get(), "填写材料")); assertEquals("闭门器", tree(root(controller.get())).filterIsInstance<EditText>().single().text.toString())
        click(controller.get(), "更换结果"); click(controller.get(), "已处理")
        assertEquals("已保存的现场处理说明", tree(root(controller.get())).filterIsInstance<EditText>().single().text.toString())
        controller.pause().stop().destroy()
    }
    @Test fun hiddenEntryAndTargetSelectionNeverDefaultsToFirstOrder() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        repeat(3) { click(activity, "我的工单") }; assertFalse(contains(activity, "工单调试")); click(activity, "我的工单")
        assertTrue(contains(activity, "工单调试")); assertFalse(textView(activity, "重置所选工单").isEnabled); assertFalse(textView(activity, "清除未提交材料").isEnabled)
        snapshot(activity, "07-工单调试-320dp")
        click(activity, "多项"); assertTrue(contains(activity, "已选择 3 项")); click(activity, "清空选择"); assertFalse(tree(root(activity)).filterIsInstance<TextView>().last { it.text == "重新开始" }.isEnabled)
        click(activity, "全选"); assertTrue(contains(activity, "已选择 12 项")); assertEquals(3, app.repository.state.orders.size)
        controller.pause().stop().destroy()
    }
    @Test fun deniedNotificationsPreserveOrdersOnRestart() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        repeat(4) { click(activity, "我的工单") }
        val restart = tree(root(activity)).filterIsInstance<TextView>().last { it.text == "重新开始" }; restart.performClick(); settle()
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick(); settle()
        assertEquals(3, app.repository.state.orders.size); assertNull(app.repository.state.plan); assertFalse(activity.isFinishing)
        assertTrue(tree(ShadowAlertDialog.getLatestAlertDialog().window!!.decorView).filterIsInstance<TextView>().any { it.text.toString().contains("请开启新工单通知") })
        controller.pause().stop().destroy()
    }
    @Test fun systemAlarmAndSingleNotificationUseActualBatchCountAndPendingIntent() {
        val repo = app.repository
        val plan = repo.restart(SceneMode.MULTIPLE, listOf("A01/escalator", "A02/escalator"), "1", app.arrival)
        val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
        assertEquals(1, alarms.scheduledAlarms.size); assertEquals(plan.dueAt, alarms.scheduledAlarms.single().triggerAtTime)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(1))
        ArrivalReceiver().onReceive(app, Intent(app, ArrivalReceiver::class.java).setData(Uri.parse("station-arrival://${plan.id}")))
        val manager = shadowOf(app.getSystemService(NotificationManager::class.java))
        assertEquals(1, manager.size()); val notification = manager.getNotification(ArrivalService.NOTIFICATION)
        assertEquals("新工单待处理", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("您有 2 张新工单待处理，点击查看", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals(2, repo.state.orders.size)
        ArrivalReceiver().onReceive(app, Intent(app, ArrivalReceiver::class.java).setData(Uri.parse("station-arrival://${plan.id}")))
        assertEquals(2, repo.state.orders.size); assertEquals(1, manager.size())
        notification.contentIntent.send(); val intent = shadowOf(app).nextStartedActivity
        assertEquals(ArrivalService.OPEN_PENDING, intent.action)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup(); settle()
        assertTrue(contains(controller.get(), "待处理 2")); assertEquals(0, manager.size())
        controller.pause().stop().destroy()
    }
    @Test fun disabledChannelAndExactAlarmAreCheckedSeparately() {
        assertTrue(app.arrival.notificationReady()); ShadowAlarmManager.setCanScheduleExactAlarms(false); assertFalse(app.arrival.alarmReady())
        val original = app.repository.state
        try { app.repository.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", app.arrival); fail("应拒绝安排") } catch (_: IllegalStateException) {}
        assertEquals(original, app.repository.state)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(ArrivalService.CHANNEL, "新工单提醒", NotificationManager.IMPORTANCE_NONE))
        assertFalse(app.arrival.notificationReady()); assertTrue(app.arrival.alarmReady())
    }
    @Test fun privateSnapshotAndPhotoSurviveRepositoryReconstruction() {
        val repo = app.repository; val order = repo.state.orders.first(); repo.start(order.id); repo.selectResult(order.id, ResultKind.HANDLED); repo.edit(order.id, ResultKind.HANDLED, "材料已保存")
        val request = repo.beginMedia(order.id); val name = app.photos.import(Uri.fromFile(sourceImage())); repo.addPhoto(request, name, "相册", false); app.photos.releaseImport(name); repo.finishMedia(request.token)
        val reloaded = StationRepository(AtomicStateStorage(app.filesDir))
        assertEquals(repo.state, reloaded.state); assertTrue(app.photos.readable(reloaded.state.orders.first().branch().photos.single()))
        repo.resetOrder(order.id, false); app.photos.cleanup(repo); assertFalse(app.photos.file(name).exists())
    }
    @Test fun confirmedRestartReturnsHomeAndEarlyReopenKeepsSelectedPlan() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
        repeat(4) { click(activity, "我的工单") }; click(activity, "单项"); click(activity, "○ TVM购票机")
        val restart = tree(root(activity)).filterIsInstance<TextView>().last { it.text == "重新开始" }; restart.performClick(); settle()
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(tree(dialog.window!!.decorView).filterIsInstance<TextView>().any { it.text.toString().contains("1 张新工单") })
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); settle()
        assertTrue(activity.isFinishing); assertTrue(app.repository.state.orders.isEmpty())
        val plan = app.repository.state.plan!!; assertEquals(listOf("A01/tvm"), plan.sceneIds)
        assertTrue(shadowOf(activity).nextStartedActivity.hasCategory(Intent.CATEGORY_HOME))
        controller.pause().stop().destroy()
        val reopened = Robolectric.buildActivity(MainActivity::class.java).setup(); settle()
        assertTrue(contains(reopened.get(), "暂无待处理工单")); assertEquals(plan, app.repository.state.plan)
        repeat(4) { click(reopened.get(), "我的工单") }; assertTrue(contains(reopened.get(), "等待到达")); assertTrue(contains(reopened.get(), "已选择 3 项"))
        assertEquals(listOf("A01/tvm"), app.repository.state.plan!!.sceneIds)
        reopened.pause().stop().destroy()
    }
    @Test fun notificationLaunchConsumedOnceAndRotationKeepsForm() {
        val intent = Intent(app, MainActivity::class.java).setAction(ArrivalService.OPEN_PENDING)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup(); settle()
        enterForm(controller.get(), "需配件支持"); edit(controller.get(), "门锁组件"); controller.recreate(); settle()
        assertTrue(contains(controller.get(), "填写材料")); assertTrue(contains(controller.get(), "门锁组件"))
        controller.pause().stop().destroy()
    }
    @Test fun newActivityResetsSpeedButLeavingDebugDoesNot() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); settle()
        repeat(4) { click(controller.get(), "我的工单") }; click(controller.get(), "快速"); assertTrue(app.fast)
        controller.get().onBackPressed(); settle(); assertTrue(app.fast)
        controller.pause().stop().destroy()
        val reopened = Robolectric.buildActivity(MainActivity::class.java).setup(); settle(); assertFalse(app.fast)
        reopened.pause().stop().destroy()
    }
    @Test fun everyEvidenceAssetCanBeDecodedAndNoNetworkPermissionIsDeclared() {
        SceneCatalog.all.forEach { scene -> val bitmap = app.photos.evidence(scene.image, 200); assertNotNull(scene.id, bitmap); bitmap!!.recycle() }
        val packageInfo = app.packageManager.getPackageInfo(app.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        assertFalse(packageInfo.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
    }
    @Test fun corruptStorageIsPreservedAndNeverReplacedWithDefaultOrders() {
        val directory = File(app.cacheDir, "corrupt-storage").apply { mkdirs() }
        val file = File(directory, "station-state-v2.bin").apply { writeText("无法解码的原始数据") }
        val before = file.readBytes()
        try { StationRepository(AtomicStateStorage(directory)); fail("损坏数据必须明确报错") } catch (_: java.io.IOException) {}
        assertArrayEquals(before, file.readBytes())
    }
    @Test fun nativeLayoutSupports320And390WithLargeFontAcrossCorePages() {
        listOf(320, 390).forEach { width ->
            listOf(1f, 1.25f, 1.5f).forEach { scale ->
                RuntimeEnvironment.setQualifiers("zh-rCN-w${width}dp-h730dp-hdpi")
                RuntimeEnvironment.setFontScale(scale)
                val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); settle()
                val repo = app.repository
                repo.clearAll(app.arrival); settle()
                fun checkPage(name: String) {
                    measure(activity, (width * 1.5).toInt(), 1050)
                    tree(root(activity)).filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE && it.width > 0 && it.text.isNotEmpty() }.forEach { view ->
                        assertTrue("$width / $scale / $name：文本区域必须为正 ${view.text}", view.width - view.paddingLeft - view.paddingRight > 0)
                        view.layout?.let { layout -> for (line in 0 until layout.lineCount) {
                            assertEquals("不允许截断文本 ${view.text}", 0, layout.getEllipsisCount(line))
                            assertTrue("$width / $scale / $name：文字不可横向溢出 ${view.text}", layout.getLineMax(line) <= layout.width + 2)
                        } }
                    }
                    if (scale == 1.5f) snapshot(activity, "${name}-${width}dp-150", (width * 1.5).toInt(), 1050)
                }
                checkPage("列表"); click(activity, "垃圾桶满溢"); checkPage("详情"); click(activity, "开始处理"); click(activity, "填写结果"); checkPage("结果选择"); click(activity, "核查无异常"); edit(activity, "现场核查未发现异常。长说明用于检查大字体下的换行与滚动。"); checkPage("材料填写")
                controller.pause().stop().destroy()
            }
        }
    }
}
