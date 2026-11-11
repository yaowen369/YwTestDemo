package ywdemo.example.yaoxiaowen

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** 本期业务只使用本机场景、材料和计划。 */
data class Scene(val id: String, val device: String, val check: String, val title: String,
                 val point: String, val node: Int, val image: String, val note: String = "") {
    val location get() = "云谷路地铁站 · $device ${point}检查点"
    val description get() = note.ifEmpty { "巡检记录提示$title，请到达对应检查点进行现场核查。" }
}

object SceneCatalog {
    val all = listOf(
        Scene("A01/gate", "A01", "闸机运行状态", "闸机运行异常", "闸机", 2, "gate-running-status.jpg", "巡检记录提示闸机运行异常，请到达检查点核查设备运行状态。"),
        Scene("A01/tvm", "A01", "TVM购票机状态", "TVM购票机状态异常", "TVM购票机", 5, "tvm-status.jpg"),
        Scene("A01/light", "A01", "照明灯亮度识别", "照明灯亮度异常", "照明灯", 6, "lighting-brightness.png"),
        Scene("A01/escalator", "A01", "电扶梯运行状态", "电扶梯运行异常（A01 检查点）", "电扶梯", 8, "escalator-a02.jpg"),
        Scene("A01/rolling-door", "A01", "卷帘门开/关是否正常", "卷帘门开关异常", "卷帘门", 17, "rolling-door-status.png"),
        Scene("A02/trash", "A02", "垃圾桶满溢乱放", "垃圾桶满溢或摆放异常", "垃圾桶", 2, "trash-overflow.jpg", "巡检记录提示垃圾桶满溢或摆放异常，请到达检查点核查桶内情况与摆放位置。"),
        Scene("A02/hydrant-door", "A02", "消火栓门状态识别", "消火栓门状态异常", "消火栓", 3, "hydrant-door.jpg", "巡检记录提示消火栓门状态异常，请到达检查点核查箱门及周边情况。"),
        Scene("A02/fight", "A02", "人员打架识别", "发现人员打架", "人员打架", 5, "passenger-fight.jpg"),
        Scene("A02/emergency-light", "A02", "应急指示灯状态", "应急指示灯状态异常", "应急指示灯", 6, "emergency-exit-sign.jpg"),
        Scene("A02/comatose", "A02", "人员躺卧/昏迷识别", "发现人员躺卧或疑似昏迷", "人员躺卧", 8, "passenger-lying.jpg"),
        Scene("A02/flood-supply", "A02", "防汛物资状态", "防汛物资状态异常", "防汛物资", 9, "flood-supplies.jpg"),
        Scene("A02/escalator", "A02", "电扶梯状态", "电扶梯状态异常（A02 检查点）", "电扶梯", 10, "escalator-a01.jpg")
    )
    val defaults = listOf("A02/trash", "A02/hydrant-door", "A01/gate")
    fun get(id: String) = all.first { it.id == id }
}

enum class OrderStatus(val label: String) { PENDING("待处理"), PROCESSING("处理中"), DONE("已完成") }
enum class ResultKind(val label: String, val field: String, val photoLabel: String?, val description: String) {
    HANDLED("已处理", "处理说明", "处理后照片", "问题已处理，填写处理情况并提供处理后照片。"),
    NORMAL("核查无异常", "核查说明", "现场照片", "现场核查未发现异常，填写核查情况并提供现场照片。"),
    PARTS("需配件支持", "配件类型", null, "需要配件支持，填写所需配件类型。")
}
enum class UploadStatus(val label: String) { WAITING("待上传"), UPLOADING("正在上传"), SUCCESS("上传成功"), FAILED("照片读取失败") }
enum class SceneMode(val label: String) { DEFAULT("默认三项"), SINGLE("单项"), MULTIPLE("多项") }

data class Photo(val id: String, val fileName: String, val source: String,
                 val status: UploadStatus = UploadStatus.WAITING, val uploadAt: Long, val finishAt: Long) : Serializable

data class RemovedPhoto(val photo: Photo, val index: Int) : Serializable
data class Branch(val text: String = "", val photos: List<Photo> = emptyList(), val removed: RemovedPhoto? = null) : Serializable

data class Order(val id: String, val number: String, val sceneId: String, val receivedAt: Long,
                 val status: OrderStatus = OrderStatus.PENDING, val startedAt: Long? = null,
                 val completedAt: Long? = null, val selected: ResultKind? = null,
                 val branches: Map<ResultKind, Branch> = ResultKind.values().associateWith { Branch() },
                 val revision: Long = 0, val submitAt: Long? = null, val submitError: String? = null) : Serializable {
    val hasDraft get() = selected != null || branches.values.any { it.text.isNotEmpty() || it.photos.isNotEmpty() || it.removed != null }
    val scene get() = SceneCatalog.get(sceneId)
    fun branch(kind: ResultKind = requireNotNull(selected)) = branches.getValue(kind)
}

data class ArrivalPlan(val id: String, val sceneIds: List<String>, val dueAt: Long,
                       val deliveredAt: Long? = null, val notified: Boolean = false) : Serializable

data class MediaRequest(val token: String, val orderId: String, val kind: ResultKind, val revision: Long,
                        val captureName: String? = null) : Serializable

data class StationState(val version: Int = 2, val orders: List<Order>, val plan: ArrivalPlan? = null,
                        val tab: OrderStatus = OrderStatus.PENDING, val listPosition: Int = 0,
                        val pendingMedia: MediaRequest? = null) : Serializable

object StationRules {
    fun createOrders(sceneIds: List<String>, now: Long): List<Order> {
        require(sceneIds.isNotEmpty() && sceneIds.distinct().size == sceneIds.size)
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date(now))
        return sceneIds.mapIndexed { index, id ->
            SceneCatalog.get(id)
            Order(UUID.randomUUID().toString(), "GD$date${(index + 1).toString().padStart(2, '0')}", id, now)
        }
    }
    fun initial(now: Long) = StationState(orders = createOrders(SceneCatalog.defaults, now))
    fun selectionError(mode: SceneMode, selected: List<String>): String? {
        if (selected.distinct().size != selected.size || selected.any { id -> SceneCatalog.all.none { it.id == id } }) return "场景选择无效，请重新选择"
        return when (mode) {
            SceneMode.DEFAULT -> if (selected != SceneCatalog.defaults) "请恢复默认三项" else null
            SceneMode.SINGLE -> if (selected.size != 1) "请选择 1 项异常场景" else null
            SceneMode.MULTIPLE -> if (selected.size !in 2..12) "请选择至少 2 项，或切换为单项" else null
        }
    }
    fun waitingMinutes(text: String, now: Long): Long? {
        if (!text.matches(Regex("[0-9]+"))) return null
        val minutes = text.toLongOrNull() ?: return null
        return minutes.takeIf { it > 0 && it <= (Long.MAX_VALUE - now) / 60_000 }
    }
    fun validation(order: Order, readable: (Photo) -> Boolean): Pair<String?, String?> {
        val kind = order.selected ?: return "请选择现场结果" to null
        val branch = order.branch(kind)
        val textError = if (branch.text.isBlank()) "请填写${kind.field}" else null
        val photoError = when {
            kind == ResultKind.PARTS -> null
            branch.photos.isEmpty() -> "请至少添加 1 张${kind.photoLabel}"
            branch.photos.any { !readable(it) } -> "照片读取失败，请移除后重新选择"
            branch.photos.any { it.status != UploadStatus.SUCCESS } -> "请等待当前照片全部上传成功"
            else -> null
        }
        return textError to photoError
    }
}

/** 点击时间使用单调时钟，滑动、长按和其他位置由页面主动清零。 */
class TitleTapGate {
    private var firstAt = 0L
    private var count = 0
    fun reset() { count = 0; firstAt = 0 }
    fun tap(now: Long): Boolean {
        if (count == 0 || now - firstAt > 2_000) { firstAt = now; count = 0 }
        count++
        if (count == 4) { reset(); return true }
        return false
    }
}
