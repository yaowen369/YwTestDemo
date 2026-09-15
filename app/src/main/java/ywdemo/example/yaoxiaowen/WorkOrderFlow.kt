package ywdemo.example.yaoxiaowen

enum class WorkOrderStatus {
    WAITING,
    TRAVELING,
    ARRIVED,
    PROCESSING,
    REVIEWING,
    COMPLETED
}

data class WorkOrderState(val status: WorkOrderStatus) {
    fun advance(): WorkOrderState {
        val nextStatus = when (status) {
            WorkOrderStatus.WAITING -> WorkOrderStatus.TRAVELING
            WorkOrderStatus.TRAVELING -> WorkOrderStatus.ARRIVED
            WorkOrderStatus.ARRIVED -> WorkOrderStatus.PROCESSING
            WorkOrderStatus.PROCESSING -> WorkOrderStatus.REVIEWING
            WorkOrderStatus.REVIEWING -> WorkOrderStatus.COMPLETED
            WorkOrderStatus.COMPLETED -> WorkOrderStatus.COMPLETED
        }
        return WorkOrderState(nextStatus)
    }

    companion object {
        fun newOrder() = WorkOrderState(WorkOrderStatus.WAITING)
    }
}
