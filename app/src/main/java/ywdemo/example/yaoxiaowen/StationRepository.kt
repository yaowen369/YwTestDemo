package ywdemo.example.yaoxiaowen

import java.util.UUID

interface StateStorage {
    fun read(): StationState?
    fun write(state: StationState)
}
interface PlanScheduler {
    fun schedule(plan: ArrivalPlan)
    fun cancel(plan: ArrivalPlan)
}

/** 所有状态变更先写入完整快照，成功后再替换内存状态；文件清理只能在提交以后进行。 */
class StationRepository(private val storage: StateStorage, private val clock: () -> Long = System::currentTimeMillis) {
    var state: StationState = storage.read()?.also {
        require(it.version == 2) { "本机数据版本无法读取" }
        require(it.orders.all { order -> SceneCatalog.all.any { scene -> scene.id == order.sceneId } }) { "本机工单数据无法读取" }
    } ?: StationRules.initial(clock()).also(storage::write)
        private set
    var onChanged: (() -> Unit)? = null

    @Synchronized
    private fun commit(next: StationState) {
        if (next == state) return
        storage.write(next)
        state = next
        onChanged?.invoke()
    }
    fun order(id: String) = state.orders.firstOrNull { it.id == id }
    @Synchronized
    private fun update(id: String, change: (Order) -> Order) {
        val current = order(id) ?: return
        val changed = change(current)
        if (current != changed) commit(state.copy(orders = state.orders.map { if (it.id == id) changed else it }))
    }
    fun start(id: String) = update(id) {
        if (it.status == OrderStatus.PENDING) it.copy(status = OrderStatus.PROCESSING, startedAt = clock()) else it
    }
    fun selectResult(id: String, kind: ResultKind, expectedRevision: Long? = null) = update(id) {
        if (expectedRevision != null && it.revision != expectedRevision) return@update it
        if (it.status == OrderStatus.PROCESSING && it.submitAt == null) it.copy(selected = kind, submitError = null) else it
    }
    fun edit(id: String, kind: ResultKind, text: String, expectedRevision: Long? = null) = update(id) {
        if (expectedRevision != null && it.revision != expectedRevision) return@update it
        if (it.status != OrderStatus.PROCESSING || it.submitAt != null) it else
            it.copy(branches = it.branches + (kind to it.branch(kind).copy(text = text)), submitError = null)
    }
    fun removePhoto(id: String, kind: ResultKind, photoId: String) = update(id) { order ->
        if (order.status != OrderStatus.PROCESSING || order.submitAt != null) return@update order
        val branch = order.branch(kind)
        val index = branch.photos.indexOfFirst { it.id == photoId }
        if (index < 0) order else order.copy(branches = order.branches + (kind to branch.copy(
            photos = branch.photos.filter { it.id != photoId }, removed = RemovedPhoto(branch.photos[index], index))))
    }
    fun undoPhoto(id: String, kind: ResultKind) = update(id) { order ->
        if (order.status != OrderStatus.PROCESSING || order.submitAt != null) return@update order
        val branch = order.branch(kind)
        val removed = branch.removed ?: return@update order
        val photos = branch.photos.toMutableList().apply { add(removed.index.coerceAtMost(size), removed.photo) }
        order.copy(branches = order.branches + (kind to branch.copy(photos = photos, removed = null)))
    }
    @Synchronized
    fun beginMedia(id: String, captureName: String? = null): MediaRequest {
        val order = requireNotNull(order(id))
        require(order.status == OrderStatus.PROCESSING && order.submitAt == null)
        val request = MediaRequest(UUID.randomUUID().toString(), id, requireNotNull(order.selected), order.revision, captureName)
        commit(state.copy(pendingMedia = request))
        return request
    }
    @Synchronized
    fun addPhoto(request: MediaRequest, fileName: String, source: String, fast: Boolean): Boolean {
        val order = order(request.orderId) ?: return false
        if (state.pendingMedia?.token != request.token || order.revision != request.revision || order.status != OrderStatus.PROCESSING || order.submitAt != null) return false
        val now = clock()
        val photo = Photo(UUID.randomUUID().toString(), fileName, source, uploadAt = now + if (fast) 250 else 500,
            finishAt = now + if (fast) 750 else 1_900)
        update(order.id) { it.copy(branches = it.branches + (request.kind to it.branch(request.kind).copy(photos = it.branch(request.kind).photos + photo))) }
        return true
    }
    @Synchronized
    fun finishMedia(token: String) {
        if (state.pendingMedia?.token == token) commit(state.copy(pendingMedia = null))
    }
    fun submit(id: String, fast: Boolean, readable: (Photo) -> Boolean) = update(id) {
        require(it.status == OrderStatus.PROCESSING) { "当前工单不可提交" }
        if (it.submitAt != null) return@update it
        val errors = StationRules.validation(it, readable)
        require(errors.first == null && errors.second == null) { errors.first ?: errors.second.orEmpty() }
        it.copy(submitAt = clock() + if (fast) 550 else 1_200, submitError = null)
    }
    /** 到期状态推进基于当前快照，已被清除的工单、分支或照片没有可回写的旧引用。 */
    @Synchronized
    fun advance(readable: (Photo) -> Boolean): List<String> {
        val now = clock()
        val completed = mutableListOf<String>()
        val orders = state.orders.map { order ->
            if (order.status != OrderStatus.PROCESSING) return@map order
            val branches = order.branches.mapValues { (_, branch) -> branch.copy(photos = branch.photos.map { photo ->
                if (photo.status == UploadStatus.SUCCESS || photo.status == UploadStatus.FAILED) photo
                else photo.copy(status = when {
                    now >= photo.finishAt -> if (readable(photo)) UploadStatus.SUCCESS else UploadStatus.FAILED
                    now >= photo.uploadAt -> UploadStatus.UPLOADING
                    else -> UploadStatus.WAITING
                })
            }) }
            var changed = order.copy(branches = branches)
            if (order.submitAt != null && now >= order.submitAt) {
                val error = StationRules.validation(changed, readable).let { it.first ?: it.second }
                changed = if (error != null) changed.copy(submitAt = null, submitError = error) else {
                    completed += order.id
                    // 完成仅保留本次提交分支，未选分支材料不进入处理记录。
                    val selected = requireNotNull(order.selected)
                    changed.copy(status = OrderStatus.DONE, completedAt = now, submitAt = null,
                        branches = ResultKind.values().associateWith { if (it == selected) changed.branch(it).copy(removed = null) else Branch() })
                }
            }
            changed
        }
        commit(state.copy(orders = orders))
        return completed
    }
    @Synchronized
    fun resetOrder(id: String, materialsOnly: Boolean) {
        val order = order(id) ?: return
        if (materialsOnly) require(order.status == OrderStatus.PROCESSING && order.hasDraft) { "当前工单没有可清除的未提交材料" }
        val reset = Order(order.id, order.number, order.sceneId, order.receivedAt,
            status = if (materialsOnly) OrderStatus.PROCESSING else OrderStatus.PENDING,
            startedAt = if (materialsOnly) order.startedAt else null, revision = order.revision + 1)
        commit(state.copy(orders = state.orders.map { if (it.id == id) reset else it },
            pendingMedia = state.pendingMedia?.takeUnless { it.orderId == id }))
    }
    @Synchronized
    fun saveList(tab: OrderStatus, position: Int) = commit(state.copy(tab = tab, listPosition = position.coerceAtLeast(0)))
    @Synchronized
    fun restart(mode: SceneMode, selected: List<String>, minuteText: String, scheduler: PlanScheduler): ArrivalPlan {
        require(StationRules.selectionError(mode, selected) == null) { StationRules.selectionError(mode, selected).orEmpty() }
        val now = clock()
        val minutes = requireNotNull(StationRules.waitingMinutes(minuteText, now)) { "请输入有效的正整数分钟" }
        val plan = ArrivalPlan(UUID.randomUUID().toString(), selected.toList(), now + minutes * 60_000)
        val old = state.plan
        // 新计划使用独立标识，安排失败或存储失败不会覆盖旧定时任务。
        scheduler.schedule(plan)
        try {
            commit(StationState(orders = emptyList(), plan = plan))
        } catch (error: Exception) {
            runCatching { scheduler.cancel(plan) }
            throw error
        }
        old?.let { runCatching { scheduler.cancel(it) } }
        return plan
    }
    @Synchronized
    fun clearAll(scheduler: PlanScheduler) {
        val old = state.plan
        commit(StationRules.initial(clock()))
        old?.let { runCatching { scheduler.cancel(it) } }
    }
    /** 通知的消费标记与新增工单在同一快照落盘，重入不会重复建单或补响。 */
    @Synchronized
    fun deliver(planId: String, notify: Boolean): List<Order> {
        val plan = state.plan ?: return emptyList()
        if (plan.id != planId || plan.deliveredAt != null || clock() < plan.dueAt) return emptyList()
        val orders = StationRules.createOrders(plan.sceneIds, clock())
        commit(state.copy(orders = state.orders + orders, plan = plan.copy(deliveredAt = clock(), notified = notify)))
        return orders
    }
    fun referencedPhotos(): Set<String> = state.orders.flatMap { order ->
        order.branches.values.flatMap { it.photos.map(Photo::fileName) + listOfNotNull(it.removed?.photo?.fileName) }
    }.toSet()
}
