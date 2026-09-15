package ywdemo.example.yaoxiaowen

import androidx.annotation.DrawableRes

data class DemoScenario(
    val id: String,
    val name: String,
    val riskLevel: String,
    val source: String,
    val location: String,
    val alarmMessage: String,
    val waitingDescription: String,
    val travelingDescription: String,
    val arrivedDescription: String,
    val processingDescription: String,
    val reviewingDescription: String,
    val completedDescription: String,
    val evidenceLabel: String,
    val completionMessage: String,
    @DrawableRes val evidenceResId: Int
)

object DemoScenarioCatalog {
    val floorSpill = DemoScenario(
        id = "floor_spill",
        name = "站厅地面液体污渍",
        riskLevel = "低风险",
        source = "机器狗 01 发现",
        location = "站厅 B2 扶梯口",
        alarmMessage = "机器狗 01 发现疑似地面液体污渍",
        waitingDescription = "请查看现场证据，确认后前往站厅 B2 扶梯口处理。",
        travelingDescription = "请沿站厅通道前往 B2 扶梯口，到达后点击确认。",
        arrivedDescription = "已到达目标点位，请确认污渍范围并放置防滑警示牌。",
        processingDescription = "正在清洁地面液体并疏导乘客，请确认通行区域安全。",
        reviewingDescription = "正在对比处理前后画面，确认地面无残留，请稍候。",
        completedDescription = "处理前后画面对比通过，地面已清洁并恢复通行。",
        evidenceLabel = "机器狗 01  ·  12:08:03",
        completionMessage = "地面污渍复核通过，工单已关闭",
        evidenceResId = R.drawable.evidence_floor_spill
    )

    val all = listOf(
        floorSpill,
        DemoScenario(
            id = "fire_hydrant_open",
            name = "消防栓门被打开",
            riskLevel = "高风险",
            source = "消防设备监测",
            location = "站厅 B1 东侧消防箱 F-03",
            alarmMessage = "消防栓门磁异常，检测到箱门被打开",
            waitingDescription = "请查看消防箱现场画面，并立即前往 F-03 点位核查。",
            travelingDescription = "请前往站厅 B1 东侧消防箱，途中留意是否存在误用情况。",
            arrivedDescription = "已到达消防箱，请检查箱内器材、门锁和封签状态。",
            processingDescription = "正在清点消防器材并关闭箱门，请确认门磁恢复正常。",
            reviewingDescription = "系统正在复核门磁信号及消防箱关闭画面，请稍候。",
            completedDescription = "消防器材完整，箱门及门磁已恢复正常状态。",
            evidenceLabel = "消防设备监测  ·  12:11:26",
            completionMessage = "消防栓门状态复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_fire_hydrant_scene
        ),
        DemoScenario(
            id = "emergency_light_fault",
            name = "应急指示灯异常状态",
            riskLevel = "中风险",
            source = "应急照明监测",
            location = "站厅 B2 北侧通道 E-12",
            alarmMessage = "应急指示灯离线，方向标识显示异常",
            waitingDescription = "请查看设备告警信息，前往北侧通道检查应急指示灯。",
            travelingDescription = "请携带电气检修工具前往 E-12 点位，到达后确认设备状态。",
            arrivedDescription = "已到达设备点位，请检查供电、灯面和方向标识。",
            processingDescription = "正在恢复设备供电并校正指示方向，请做好现场防护。",
            reviewingDescription = "系统正在复核设备在线状态与灯面显示，请稍候。",
            completedDescription = "应急指示灯已恢复在线，亮度和方向标识均正常。",
            evidenceLabel = "应急照明监测  ·  12:14:18",
            completionMessage = "应急指示灯状态复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_default
        ),
        DemoScenario(
            id = "passenger_fight",
            name = "站厅有人打架",
            riskLevel = "高风险",
            source = "视频智能分析",
            location = "站厅 B2 南侧付费区",
            alarmMessage = "视频识别到多人发生激烈肢体冲突",
            waitingDescription = "请查看现场画面，通知车站公安并前往南侧付费区处置。",
            travelingDescription = "请与增援人员共同前往现场，并保持与综控室联络。",
            arrivedDescription = "已到达事发区域，请先疏散围观乘客并隔离冲突双方。",
            processingDescription = "正在协同车站公安控制现场，请持续关注周边乘客安全。",
            reviewingDescription = "综控室正在复核现场秩序和人员疏散情况，请稍候。",
            completedDescription = "冲突人员已移交处理，站厅秩序恢复正常。",
            evidenceLabel = "视频智能分析  ·  12:17:42",
            completionMessage = "现场秩序复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_passenger_fight
        ),
        DemoScenario(
            id = "tvm_left_item",
            name = "TVM 售票机有乘客遗留物",
            riskLevel = "中风险",
            source = "视频智能分析",
            location = "站厅 B1 TVM-06 售票机",
            alarmMessage = "TVM-06 附近检测到乘客遗留物",
            waitingDescription = "请查看现场画面，前往 TVM-06 核查遗留物情况。",
            travelingDescription = "请前往站厅 B1 自助售票区，并提醒乘客暂时避让。",
            arrivedDescription = "已到达 TVM-06，请确认物品外观并设置临时隔离区。",
            processingDescription = "正在按遗失物品流程登记、封存并移交车站服务台。",
            reviewingDescription = "系统正在复核点位清空画面和物品登记信息，请稍候。",
            completedDescription = "遗留物已登记并移交，TVM-06 周边恢复正常使用。",
            evidenceLabel = "视频智能分析  ·  12:20:09",
            completionMessage = "乘客遗留物处置复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_tvm_left_item
        ),
        DemoScenario(
            id = "trash_overflow",
            name = "垃圾桶溢出",
            riskLevel = "低风险",
            source = "保洁巡检设备",
            location = "站厅 B2 西侧垃圾桶 C-07",
            alarmMessage = "保洁巡检发现垃圾桶容量溢出",
            waitingDescription = "请查看现场画面，通知保洁人员前往 C-07 点位处理。",
            travelingDescription = "请携带垃圾袋和清洁工具前往站厅 B2 西侧。",
            arrivedDescription = "已到达垃圾桶点位，请确认溢出范围和周边卫生情况。",
            processingDescription = "正在更换垃圾袋并清洁桶体及周边地面。",
            reviewingDescription = "系统正在复核垃圾桶容量和周边环境，请稍候。",
            completedDescription = "垃圾已清运，桶体及周边环境恢复整洁。",
            evidenceLabel = "保洁巡检设备  ·  12:23:35",
            completionMessage = "垃圾桶溢出处置复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_trash_overflow
        ),
        DemoScenario(
            id = "gate_crowding",
            name = "闸机出入口有异常人员堆积",
            riskLevel = "高风险",
            source = "客流智能监测",
            location = "站厅 B1 进站闸机 2 号口",
            alarmMessage = "闸机出入口客流密度异常，人员持续堆积",
            waitingDescription = "请查看实时客流画面，立即前往进站闸机 2 号口疏导。",
            travelingDescription = "请联系综控室准备调整闸机通道，并尽快到达现场。",
            arrivedDescription = "已到达闸机口，请确认拥堵原因并划分进出站流线。",
            processingDescription = "正在增开通道并广播引导乘客，请持续监控客流密度。",
            reviewingDescription = "系统正在复核闸机口客流密度和通行速度，请稍候。",
            completedDescription = "人员堆积已消散，闸机出入口恢复有序通行。",
            evidenceLabel = "客流智能监测  ·  12:26:51",
            completionMessage = "闸机口客流状态复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_default
        ),
        DemoScenario(
            id = "flood_barrier_material",
            name = "防汛挡板物资摆放异常",
            riskLevel = "中风险",
            source = "防汛物资巡检",
            location = "设备区 B1 防汛物资点 FW-02",
            alarmMessage = "防汛挡板数量或摆放位置异常",
            waitingDescription = "请查看巡检记录，前往 FW-02 点位核对防汛挡板物资。",
            travelingDescription = "请携带物资清单前往设备区 B1 防汛物资点。",
            arrivedDescription = "已到达物资点，请核对挡板数量、规格和存放状态。",
            processingDescription = "正在补齐缺失物资并按编号重新归位防汛挡板。",
            reviewingDescription = "系统正在复核物资数量和规范摆放画面，请稍候。",
            completedDescription = "防汛挡板数量齐全，已按预案要求规范归位。",
            evidenceLabel = "防汛物资巡检  ·  12:29:14",
            completionMessage = "防汛挡板物资复核通过，工单已关闭",
            evidenceResId = R.drawable.evidence_default
        )
    )

    fun findById(id: String?): DemoScenario = all.firstOrNull { it.id == id } ?: floorSpill
}
