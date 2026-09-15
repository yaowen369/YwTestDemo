package ywdemo.example.yaoxiaowen

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkOrderFlowTest {

    @Test
    fun advanceMovesOrderThroughTheFieldWorkflow() {
        var state = WorkOrderState.newOrder()

        state = state.advance()
        assertEquals(WorkOrderStatus.TRAVELING, state.status)

        state = state.advance()
        assertEquals(WorkOrderStatus.ARRIVED, state.status)

        state = state.advance()
        assertEquals(WorkOrderStatus.PROCESSING, state.status)

        state = state.advance()
        assertEquals(WorkOrderStatus.REVIEWING, state.status)

        state = state.advance()
        assertEquals(WorkOrderStatus.COMPLETED, state.status)
    }

    @Test
    fun completedOrderDoesNotAdvanceAgain() {
        val completed = WorkOrderState(WorkOrderStatus.COMPLETED)

        assertEquals(completed, completed.advance())
    }
}
