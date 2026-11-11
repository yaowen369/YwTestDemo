package ywdemo.example.yaoxiaowen

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.*

class StationRepositoryTest {
    private class MemoryStorage : StateStorage {
        var saved: StationState? = null
        var fail = false
        override fun read() = saved
        override fun write(state: StationState) { if (fail) throw IOException("写入失败"); saved = state }
    }
    private class Scheduler : PlanScheduler {
        val active = mutableMapOf<String, ArrivalPlan>()
        var fail = false
        override fun schedule(plan: ArrivalPlan) { if (fail) throw IllegalStateException("安排失败"); active[plan.id] = plan }
        override fun cancel(plan: ArrivalPlan) { active.remove(plan.id) }
    }
    private lateinit var storage: MemoryStorage
    private lateinit var repo: StationRepository
    private lateinit var scheduler: Scheduler
    private var now = 1_794_355_200_000L
    private val id get() = repo.state.orders.first().id
    @Before fun setup() { storage = MemoryStorage(); repo = StationRepository(storage) { now }; scheduler = Scheduler() }
    private fun prepare(kind: ResultKind = ResultKind.HANDLED): String {
        val target = id; repo.start(target); repo.selectResult(target, kind); repo.edit(target, kind, "现场已核查")
        return target
    }
    private fun photo(target: String, name: String = "photo.jpg", fast: Boolean = false): MediaRequest {
        val request = repo.beginMedia(target); assertTrue(repo.addPhoto(request, name, "相册", fast)); repo.finishMedia(request.token); return request
    }
    private fun expectFailure(action: () -> Unit) { try { action(); fail("应当失败且保留数据") } catch (_: IllegalArgumentException) {} catch (_: IOException) {} catch (_: IllegalStateException) {} }

    @Test fun initialOrdersAreExactlyDefaultThree() {
        assertEquals(SceneCatalog.defaults, repo.state.orders.map { it.sceneId }); assertEquals(3, repo.state.orders.size)
        assertTrue(repo.state.orders.all { it.status == OrderStatus.PENDING && !it.hasDraft })
    }
    @Test fun twelveScenesAreIndependentAndImageBindingsStayUnchanged() {
        assertEquals(12, SceneCatalog.all.size); assertEquals(12, SceneCatalog.all.map { it.id }.distinct().size)
        assertEquals(5, SceneCatalog.all.count { it.device == "A01" }); assertEquals(7, SceneCatalog.all.count { it.device == "A02" })
        assertEquals("escalator-a02.jpg", SceneCatalog.get("A01/escalator").image)
        assertEquals("escalator-a01.jpg", SceneCatalog.get("A02/escalator").image)
    }
    @Test fun startIsIdempotentAndAllowsConcurrentProcessing() {
        val first = id; repo.start(first); val started = repo.order(first)!!.startedAt; now += 100
        repo.start(first); repo.start(repo.state.orders[1].id)
        assertEquals(started, repo.order(first)!!.startedAt); assertEquals(2, repo.state.orders.count { it.status == OrderStatus.PROCESSING })
    }
    @Test fun resultBranchesKeepIndependentTextAndPhotos() {
        val target = prepare(); photo(target)
        repo.selectResult(target, ResultKind.NORMAL); repo.edit(target, ResultKind.NORMAL, "未发现异常"); photo(target, "normal.jpg")
        repo.selectResult(target, ResultKind.PARTS); repo.edit(target, ResultKind.PARTS, "箱门合页")
        val order = repo.order(target)!!
        assertEquals("现场已核查", order.branch(ResultKind.HANDLED).text)
        assertEquals("photo.jpg", order.branch(ResultKind.HANDLED).photos.single().fileName)
        assertEquals("normal.jpg", order.branch(ResultKind.NORMAL).photos.single().fileName)
        assertEquals("箱门合页", order.branch(ResultKind.PARTS).text)
    }
    @Test fun handledRequiresTextAndAtLeastOnePhoto() {
        val target = prepare(); repo.edit(target, ResultKind.HANDLED, "  \n")
        val errors = StationRules.validation(repo.order(target)!!) { true }
        assertNotNull(errors.first); assertNotNull(errors.second); expectFailure { repo.submit(target, false) { true } }
    }
    @Test fun normalAlsoRequiresPhoto() {
        val target = prepare(ResultKind.NORMAL)
        assertNotNull(StationRules.validation(repo.order(target)!!) { true }.second)
    }
    @Test fun allCurrentPhotosMustFinishUploading() {
        val target = prepare(); photo(target); now += 2_000; repo.advance { true }; photo(target, "second.jpg")
        expectFailure { repo.submit(target, false) { true } }
        now += 2_000; repo.advance { true }; repo.submit(target, false) { true }
        assertNotNull(repo.order(target)!!.submitAt)
    }
    @Test fun partsCanCompleteDespitePhotosInOtherBranch() {
        val target = prepare(); photo(target); repo.selectResult(target, ResultKind.PARTS); repo.edit(target, ResultKind.PARTS, "箱门合页")
        repo.submit(target, true) { false }; assertEquals(OrderStatus.PROCESSING, repo.order(target)!!.status)
        now += 600; assertEquals(listOf(target), repo.advance { false })
        val done = repo.order(target)!!; assertEquals(OrderStatus.DONE, done.status); assertEquals(ResultKind.PARTS, done.selected)
        assertEquals("箱门合页", done.branch().text); assertTrue(done.branch(ResultKind.HANDLED).photos.isEmpty())
    }
    @Test fun uploadProgressHasThreePersistedStates() {
        val target = prepare(); photo(target)
        assertEquals(UploadStatus.WAITING, repo.order(target)!!.branch().photos.single().status)
        now += 600; repo.advance { true }; assertEquals(UploadStatus.UPLOADING, repo.order(target)!!.branch().photos.single().status)
        now += 1_400; repo.advance { true }; assertEquals(UploadStatus.SUCCESS, repo.order(target)!!.branch().photos.single().status)
    }
    @Test fun realReadFailureDoesNotReportUploadSuccess() {
        val target = prepare(); photo(target); now += 2_000; repo.advance { false }
        assertEquals(UploadStatus.FAILED, repo.order(target)!!.branch().photos.single().status)
        expectFailure { repo.submit(target, false) { false } }
    }
    @Test fun fileDisappearingDuringSubmitDoesNotComplete() {
        val target = prepare(); photo(target); now += 2_000; repo.advance { true }; repo.submit(target, false) { true }
        now += 2_000; assertTrue(repo.advance { false }.isEmpty()); assertEquals(OrderStatus.PROCESSING, repo.order(target)!!.status)
        assertNotNull(repo.order(target)!!.submitError)
    }
    @Test fun repeatedSubmitDoesNotChangeDeadlineOrDuplicateCompletion() {
        val target = prepare(ResultKind.PARTS); repo.submit(target, false) { true }; val deadline = repo.order(target)!!.submitAt
        now += 100; repo.submit(target, true) { true }; assertEquals(deadline, repo.order(target)!!.submitAt)
        now += 2_000; assertEquals(1, repo.advance { true }.size); assertEquals(0, repo.advance { true }.size)
    }
    @Test fun completedOrderCannotBeEdited() {
        val target = prepare(ResultKind.PARTS); repo.submit(target, true) { true }; now += 600; repo.advance { true }
        val done = repo.order(target); repo.edit(target, ResultKind.PARTS, "修改"); repo.selectResult(target, ResultKind.NORMAL)
        assertEquals(done, repo.order(target))
    }
    @Test fun removeAndUndoPersistOriginalPhotoAndPosition() {
        val target = prepare(); photo(target, "one.jpg"); photo(target, "two.jpg")
        val first = repo.order(target)!!.branch().photos.first(); repo.removePhoto(target, ResultKind.HANDLED, first.id)
        assertEquals(setOf("one.jpg", "two.jpg"), repo.referencedPhotos())
        repo = StationRepository(storage) { now }; repo.undoPhoto(target, ResultKind.HANDLED)
        assertEquals(listOf("one.jpg", "two.jpg"), repo.order(target)!!.branch().photos.map { it.fileName })
    }
    @Test fun resetOneOrderLeavesOthersAndOriginalIdentityUntouched() {
        val target = prepare(); photo(target); val original = repo.order(target)!!; val others = repo.state.orders.drop(1)
        repo.resetOrder(target, false); now += 10_000; repo.advance { true }
        val reset = repo.order(target)!!; assertEquals(original.number, reset.number); assertEquals(original.sceneId, reset.sceneId)
        assertEquals(OrderStatus.PENDING, reset.status); assertFalse(reset.hasDraft); assertEquals(others, repo.state.orders.drop(1)); assertTrue(repo.referencedPhotos().isEmpty())
    }
    @Test fun clearMaterialsKeepsStartedStateButClearsEveryBranch() {
        val target = prepare(); photo(target); repo.selectResult(target, ResultKind.PARTS); repo.edit(target, ResultKind.PARTS, "配件")
        val started = repo.order(target)!!.startedAt; repo.resetOrder(target, true)
        assertEquals(OrderStatus.PROCESSING, repo.order(target)!!.status); assertEquals(started, repo.order(target)!!.startedAt); assertFalse(repo.order(target)!!.hasDraft)
    }
    @Test fun staleEditorCannotRestoreMaterialsAfterReset() {
        val target = prepare(); val revision = repo.order(target)!!.revision; repo.resetOrder(target, true)
        repo.edit(target, ResultKind.HANDLED, "旧编辑器回写", revision); repo.selectResult(target, ResultKind.HANDLED, revision)
        assertFalse(repo.order(target)!!.hasDraft)
    }
    @Test fun resetDuringSubmitNeverResurrectsOldResult() {
        val target = prepare(ResultKind.PARTS); repo.submit(target, false) { true }; repo.resetOrder(target, false)
        now += 3_000; assertTrue(repo.advance { true }.isEmpty()); assertEquals(OrderStatus.PENDING, repo.order(target)!!.status)
    }
    @Test fun resetInvalidatesExternalCameraCallback() {
        val target = prepare(); val request = repo.beginMedia(target, "capture.jpg"); repo.resetOrder(target, true)
        assertFalse(repo.addPhoto(request, "late.jpg", "拍照", false)); assertTrue(repo.referencedPhotos().isEmpty())
    }
    @Test fun failedResetPreservesPhotosAndState() {
        val target = prepare(); photo(target); val before = repo.state; storage.fail = true
        expectFailure { repo.resetOrder(target, false) }; assertEquals(before, repo.state); assertEquals(setOf("photo.jpg"), repo.referencedPhotos())
    }
    @Test fun failedSubmitSavePreservesDraftAndNeverCompletes() {
        val target = prepare(ResultKind.PARTS); repo.submit(target, false) { true }; val before = repo.state
        storage.fail = true; now += 2_000; expectFailure { repo.advance { true } }; assertEquals(before, repo.state)
    }
    @Test fun multipleRequiresTwoToTwelveUniqueKnownScenes() {
        assertNotNull(StationRules.selectionError(SceneMode.MULTIPLE, emptyList())); assertNotNull(StationRules.selectionError(SceneMode.MULTIPLE, listOf("A01/gate")))
        assertNull(StationRules.selectionError(SceneMode.MULTIPLE, SceneCatalog.all.map { it.id }))
        assertNotNull(StationRules.selectionError(SceneMode.MULTIPLE, listOf("A01/gate", "A01/gate")))
        assertNotNull(StationRules.selectionError(SceneMode.SINGLE, listOf("unknown")))
    }
    @Test fun waitingTimeRejectsBlankZeroNegativeDecimalAndOverflow() {
        listOf("", "0", "-1", "1.2", "abc", "999999999999999999999", "9223372036854775807").forEach { assertNull(it, StationRules.waitingMinutes(it, now)) }
        assertEquals(1L, StationRules.waitingMinutes("1", now)); assertEquals(137L, StationRules.waitingMinutes("137", now))
    }
    @Test fun invalidRestartDoesNotClearOrReplaceAnything() {
        val before = repo.state
        expectFailure { repo.restart(SceneMode.SINGLE, emptyList(), "1", scheduler) }; expectFailure { repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "0", scheduler) }
        assertEquals(before, repo.state); assertTrue(scheduler.active.isEmpty())
    }
    @Test fun earlyReopenKeepsEmptyListAndOriginalDeadlineAndSelection() {
        val plan = repo.restart(SceneMode.SINGLE, listOf("A01/escalator"), "2", scheduler)
        now += 50_000; repo = StationRepository(storage) { now }
        assertTrue(repo.state.orders.isEmpty()); assertEquals(plan, repo.state.plan); assertTrue(repo.deliver(plan.id, true).isEmpty())
    }
    @Test fun customArrivalIsAddedOnceWithActualArrivalTime() {
        val selected = listOf("A01/escalator", "A02/escalator")
        val plan = repo.restart(SceneMode.MULTIPLE, selected, "1", scheduler); now += 61_000
        val added = repo.deliver(plan.id, true)
        assertEquals(selected, added.map { it.sceneId }); assertTrue(added.all { it.receivedAt == now && !it.hasDraft })
        assertTrue(repo.deliver(plan.id, true).isEmpty()); assertEquals(2, repo.state.orders.size); assertTrue(repo.state.plan!!.notified)
    }
    @Test fun allTwelveCanArriveAndFinishIndependently() {
        val plan = repo.restart(SceneMode.MULTIPLE, SceneCatalog.all.map { it.id }, "1", scheduler); now += 60_000
        assertEquals(12, repo.deliver(plan.id, true).size)
        repo.state.orders.forEach { order -> repo.start(order.id); repo.selectResult(order.id, ResultKind.PARTS); repo.edit(order.id, ResultKind.PARTS, order.scene.title + "配件"); repo.submit(order.id, true) { true } }
        now += 600; assertEquals(12, repo.advance { true }.size); assertEquals(12, repo.state.orders.count { it.status == OrderStatus.DONE })
    }
    @Test fun replacePlanLeavesOneEffectiveScheduleAndOldCallbackCannotAddOrders() {
        val first = repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler)
        val second = repo.restart(SceneMode.SINGLE, listOf("A02/fight"), "2", scheduler)
        assertEquals(setOf(second.id), scheduler.active.keys); now += 120_000
        assertTrue(repo.deliver(first.id, true).isEmpty()); assertEquals("A02/fight", repo.deliver(second.id, true).single().sceneId)
    }
    @Test fun schedulingFailurePreservesOldPlanAndData() {
        repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler); val before = repo.state; val active = scheduler.active.toMap(); scheduler.fail = true
        expectFailure { repo.restart(SceneMode.SINGLE, listOf("A01/light"), "2", scheduler) }; assertEquals(before, repo.state); assertEquals(active, scheduler.active)
    }
    @Test fun persistenceFailureCancelsCandidateAndPreservesOldPlan() {
        repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler); val before = repo.state; val active = scheduler.active.toMap(); storage.fail = true
        expectFailure { repo.restart(SceneMode.SINGLE, listOf("A01/light"), "2", scheduler) }; assertEquals(before, repo.state); assertEquals(active, scheduler.active)
    }
    @Test fun firstRestartFailureKeepsExistingDraft() {
        prepare(); val before = repo.state; storage.fail = true
        expectFailure { repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler) }; assertEquals(before, repo.state); assertTrue(scheduler.active.isEmpty())
    }
    @Test fun clearingAllCancelsPlanAndRestoresThreeImmediately() {
        val plan = repo.restart(SceneMode.SINGLE, listOf("A02/fight"), "2", scheduler); repo.clearAll(scheduler); now += 120_000
        assertNull(repo.state.plan); assertEquals(SceneCatalog.defaults, repo.state.orders.map { it.sceneId }); assertTrue(repo.deliver(plan.id, true).isEmpty()); assertTrue(scheduler.active.isEmpty())
    }
    @Test fun clearingAllFailureLeavesOldScheduleActive() {
        repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler); val before = repo.state; storage.fail = true
        expectFailure { repo.clearAll(scheduler) }; assertEquals(before, repo.state); assertEquals(1, scheduler.active.size)
    }
    @Test fun failedArrivalSaveCanBeRetriedWithoutPartialOrders() {
        val plan = repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler); now += 60_000; storage.fail = true
        expectFailure { repo.deliver(plan.id, true) }; assertTrue(repo.state.orders.isEmpty()); assertNull(repo.state.plan!!.deliveredAt)
        storage.fail = false; assertEquals(3, repo.deliver(plan.id, true).size)
    }
    @Test fun fallbackAddsOrdersWithoutMarkingOrRepeatingNotification() {
        val plan = repo.restart(SceneMode.DEFAULT, SceneCatalog.defaults, "1", scheduler); now += 60_000
        assertEquals(3, repo.deliver(plan.id, false).size); assertFalse(repo.state.plan!!.notified); assertTrue(repo.deliver(plan.id, true).isEmpty())
    }
    @Test fun serializedReopenKeepsDraftPhotosAndListContext() {
        val target = prepare(); photo(target); repo.saveList(OrderStatus.PROCESSING, 248)
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(repo.state) } }.toByteArray()
        storage.saved = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as StationState }
        repo = StationRepository(storage) { now }
        assertEquals(OrderStatus.PROCESSING, repo.state.tab); assertEquals(248, repo.state.listPosition); assertEquals("photo.jpg", repo.order(target)!!.branch().photos.single().fileName)
    }
    @Test fun fastTimingOnlyAppliesToNewPhotoOperations() {
        val target = prepare(); photo(target, "normal.jpg", false); photo(target, "fast.jpg", true)
        now += 800; repo.advance { true }
        assertEquals(listOf(UploadStatus.UPLOADING, UploadStatus.SUCCESS), repo.order(target)!!.branch().photos.map { it.status })
    }
    @Test fun titleGateRequiresFourTapsWithinTwoSecondsAndResets() {
        val gate = TitleTapGate(); assertFalse(gate.tap(10)); assertFalse(gate.tap(100)); assertFalse(gate.tap(200)); assertTrue(gate.tap(300))
        assertFalse(gate.tap(400)); gate.reset(); assertFalse(gate.tap(500)); assertFalse(gate.tap(2_600)); assertFalse(gate.tap(2_700)); assertFalse(gate.tap(2_800)); assertTrue(gate.tap(2_900))
    }
}
