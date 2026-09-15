package ywdemo.example.yaoxiaowen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoScenarioCatalogTest {

    @Test
    fun catalogContainsAllEightUniqueScenarios() {
        val scenarios = DemoScenarioCatalog.all

        assertEquals(8, scenarios.size)
        assertEquals(8, scenarios.map { it.id }.distinct().size)
        assertEquals(
            listOf(
                "站厅地面液体污渍",
                "消防栓门被打开",
                "应急指示灯异常状态",
                "站厅有人打架",
                "TVM 售票机有乘客遗留物",
                "垃圾桶溢出",
                "闸机出入口有异常人员堆积",
                "防汛挡板物资摆放异常"
            ),
            scenarios.map { it.name }
        )
    }

    @Test
    fun unknownStoredIdFallsBackToFloorSpill() {
        assertEquals(DemoScenarioCatalog.floorSpill, DemoScenarioCatalog.findById("unknown"))
        assertEquals(DemoScenarioCatalog.floorSpill, DemoScenarioCatalog.findById(null))
    }

    @Test
    fun everyScenarioHasCompleteAndSpecificCopy() {
        DemoScenarioCatalog.all.forEach { scenario ->
            assertTrue(scenario.location.isNotBlank())
            assertTrue(scenario.source.isNotBlank())
            assertTrue(scenario.riskLevel.isNotBlank())
            assertTrue(scenario.alarmMessage.isNotBlank())
            assertTrue(scenario.waitingDescription.isNotBlank())
            assertTrue(scenario.travelingDescription.isNotBlank())
            assertTrue(scenario.arrivedDescription.isNotBlank())
            assertTrue(scenario.processingDescription.isNotBlank())
            assertTrue(scenario.reviewingDescription.isNotBlank())
            assertTrue(scenario.completedDescription.isNotBlank())
            assertTrue(scenario.evidenceResId != 0)
            assertNotEquals(scenario.waitingDescription, scenario.completedDescription)
        }
    }

    @Test
    fun suppliedScenePhotosAreMappedToTheirMatchingOrders() {
        val scenarios = DemoScenarioCatalog.all.associateBy { it.id }

        assertEquals(
            R.drawable.evidence_fire_hydrant_scene,
            scenarios.getValue("fire_hydrant_open").evidenceResId
        )
        assertEquals(
            R.drawable.evidence_passenger_fight,
            scenarios.getValue("passenger_fight").evidenceResId
        )
        assertEquals(
            R.drawable.evidence_tvm_left_item,
            scenarios.getValue("tvm_left_item").evidenceResId
        )
        assertEquals(
            R.drawable.evidence_trash_overflow,
            scenarios.getValue("trash_overflow").evidenceResId
        )
    }
}
