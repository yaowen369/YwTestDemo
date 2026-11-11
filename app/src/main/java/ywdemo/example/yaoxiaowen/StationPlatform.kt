package ywdemo.example.yaoxiaowen

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.AtomicFile
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.*
import java.util.UUID
import java.util.concurrent.Executors

class AtomicStateStorage(directory: File) : StateStorage {
    private val file = AtomicFile(File(directory, "station-state-v2.bin"))
    override fun read(): StationState? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        return file.openRead().use { input -> ObjectInputStream(input).use { it.readObject() as StationState } }
    }
    override fun write(state: StationState) {
        val bytes = ByteArrayOutputStream().use { buffer ->
            ObjectOutputStream(buffer).use { it.writeObject(state) }
            buffer.toByteArray()
        }
        var output: FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(bytes)
            output.flush()
            output.fd.sync()
            file.finishWrite(output)
            if (!file.baseFile.readBytes().contentEquals(bytes)) throw IOException("本机数据写入未完成")
        } catch (error: Exception) {
            file.failWrite(output)
            throw IOException("本机保存失败，请重试", error)
        }
    }
}

class PhotoStore(private val context: Context) {
    private val directory = File(context.filesDir, "station-photos").apply { mkdirs() }
    val captureDirectory = File(context.filesDir, "station-capture").apply { mkdirs() }
    private val inFlight = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    fun file(name: String): File {
        require(name == File(name).name && name.isNotBlank())
        return File(directory, name)
    }
    fun readable(photo: Photo): Boolean = runCatching {
        val file = file(photo.fileName)
        if (!file.isFile || file.length() == 0L) return@runCatching false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        options.outWidth > 0 && options.outHeight > 0
    }.getOrDefault(false)
    fun import(uri: Uri): String {
        val name = UUID.randomUUID().toString() + ".image"
        inFlight += name
        val target = file(name)
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("无法读取照片")
            input.use { source -> FileOutputStream(target).use { output -> source.copyTo(output); output.flush(); output.fd.sync() } }
            val bitmap = decode(target, 120) ?: throw IOException("照片无法打开，请选择有效图片")
            bitmap.recycle()
            return name
        } catch (error: Exception) {
            target.delete()
            inFlight -= name
            throw IOException("照片读取或保存失败，请重新选择", error)
        }
    }
    fun releaseImport(name: String) { inFlight -= name }
    fun discard(name: String) { inFlight -= name; file(name).delete() }
    fun cleanup(repository: StationRepository) {
        val referenced = repository.referencedPhotos()
        directory.listFiles()?.filter { it.name !in referenced && it.name !in inFlight }?.forEach { it.delete() }
        val capture = repository.state.pendingMedia?.captureName
        captureDirectory.listFiles()?.filter { it.name != capture }?.forEach { it.delete() }
    }
    fun decode(file: File, maxSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, maxSize)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
        val orientation = runCatching { ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(1)
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)
            5 -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            6 -> matrix.setRotate(90f)
            7 -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
            8 -> matrix.setRotate(270f)
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { if (it !== bitmap) bitmap.recycle() }
    }
    fun evidence(name: String, maxSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("evidence/$name").use { BitmapFactory.decodeStream(it, null, options) }
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, maxSize)
        options.inJustDecodeBounds = false
        return context.assets.open("evidence/$name").use { BitmapFactory.decodeStream(it, null, options) }
    }
    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > target * 2) sample *= 2
        return sample
    }
}

class ArrivalService(private val context: Context) : PlanScheduler {
    companion object {
        const val CHANNEL = "station_new_orders_v2"
        const val NOTIFICATION = 2101
        const val OPEN_PENDING = "station.OPEN_PENDING"
    }
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "新工单提醒", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "新工单到达时提醒"
            channel.enableVibration(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            notifications.createNotificationChannel(channel)
        }
    }
    fun notificationReady(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26 && notifications.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return true
    }
    fun alarmReady() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    fun backgroundReady() = Build.VERSION.SDK_INT < 28 || !context.getSystemService(ActivityManager::class.java).isBackgroundRestricted
    fun applicationSettings() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    private fun pending(plan: ArrivalPlan): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, ArrivalReceiver::class.java).setData(Uri.parse("station-arrival://${plan.id}")),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    override fun schedule(plan: ArrivalPlan) {
        check(notificationReady()) { "请开启新工单通知" }
        check(alarmReady()) { "请允许定时提醒" }
        check(backgroundReady()) { "请允许后台运行" }
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.dueAt, pending(plan))
    }
    override fun cancel(plan: ArrivalPlan) { val intent = pending(plan); alarms.cancel(intent); intent.cancel() }
    fun retry(plan: ArrivalPlan) {
        if (alarmReady()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000, pending(plan))
    }
    fun clearNotification() = notifications.cancel(NOTIFICATION)
    fun notifyArrival(count: Int, planId: String) {
        if (count <= 0 || !notificationReady()) return
        val launch = Intent(context, MainActivity::class.java).setAction(OPEN_PENDING)
            .setData(Uri.parse("station-orders://$planId"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val content = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle("新工单待处理")
            .setContentText("您有 $count 张新工单待处理，点击查看")
            .setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(Notification.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true)
            .setContentIntent(content).setOnlyAlertOnce(true).build()
        notifications.notify(NOTIFICATION, notification)
    }
    fun notificationSettings(): Intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL)
    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    fun alarmSettings(): Intent = if (Build.VERSION.SDK_INT >= 31) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}

open class StationApplication : Application() {
    open fun currentTimeMillis(): Long = System.currentTimeMillis()
    val main = Handler(Looper.getMainLooper())
    val io = Executors.newSingleThreadExecutor()
    val images = Executors.newSingleThreadExecutor()
    val photos by lazy { PhotoStore(this) }
    val arrival by lazy { ArrivalService(this) }
    val repository by lazy {
        StationRepository(AtomicStateStorage(filesDir), ::currentTimeMillis).also { repository ->
            repository.onChanged = { main.post { listeners.toList().forEach { it() } } }
        }
    }
    val listeners = mutableSetOf<() -> Unit>()
    var fast = false
    var importing = false
    var reportError: ((String) -> Unit)? = null
    private var active = false
    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            try {
                val completed = repository.advance(photos::readable)
                if (completed.isNotEmpty()) photos.cleanup(repository)
                if (repository.state.orders.isNotEmpty() && repository.state.orders.all { it.status == OrderStatus.DONE }) arrival.clearNotification()
            } catch (_: Exception) {
                reportError?.invoke("本机保存失败，请保留当前材料并重试")
            }
            main.postDelayed(this, 200)
        }
    }
    override fun onCreate() { super.onCreate(); arrival.createChannel() }
    fun startOperations() {
        if (active) return
        active = true
        main.post(tick)
    }
    fun stopOperations() { active = false; main.removeCallbacks(tick) }
    fun reconcile() {
        val plan = repository.state.plan ?: return
        if (plan.deliveredAt == null && currentTimeMillis() >= plan.dueAt) {
            repository.deliver(plan.id, false)
            arrival.cancel(plan)
        } else if (plan.deliveredAt == null && arrival.notificationReady() && arrival.alarmReady() && arrival.backgroundReady()) {
            // 再次打开仅校准同一计划的系统安排，不改变原到达时刻。
            arrival.schedule(plan)
        }
    }
}

class ArrivalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as StationApplication
        val planId = intent.data?.host ?: return
        // 事务与界面共用单进程仓库；不依赖 Activity 或前台计时器。
        try {
            val orders = app.repository.deliver(planId, true)
            if (orders.isNotEmpty()) app.arrival.notifyArrival(orders.size, planId)
        } catch (_: Exception) {
            app.repository.state.plan?.takeIf { it.id == planId && it.deliveredAt == null }?.let {
                runCatching { app.arrival.retry(it) }
            }
        }
    }
}
