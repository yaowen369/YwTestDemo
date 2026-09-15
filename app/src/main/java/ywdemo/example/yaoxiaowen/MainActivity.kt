package ywdemo.example.yaoxiaowen

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private val backgroundColor = Color.rgb(7, 20, 31)
    private val surfaceColor = Color.rgb(15, 39, 54)
    private val surfaceLightColor = Color.rgb(20, 52, 68)
    private val textPrimaryColor = Color.rgb(241, 249, 251)
    private val textSecondaryColor = Color.rgb(155, 185, 195)
    private val accentColor = Color.rgb(88, 225, 210)
    private val warningColor = Color.rgb(255, 67, 77)
    private val warningBrightColor = Color.rgb(255, 103, 111)
    private val warningSurfaceColor = Color.rgb(54, 20, 28)
    private val warningBadgeColor = Color.rgb(75, 22, 31)

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var currentScenario: DemoScenario
    private var workOrderState = WorkOrderState.newOrder()
    private var hasWorkOrder = false
    private var alarmOverlay: FrameLayout? = null
    private var alarmCard: LinearLayout? = null
    private var statusText: TextView? = null
    private var sourceText: TextView? = null
    private var orderTitleText: TextView? = null
    private var orderLocationText: TextView? = null
    private var orderCard: LinearLayout? = null
    private var evidenceFrame: FrameLayout? = null
    private var evidenceImage: ImageView? = null
    private var evidenceTag: TextView? = null
    private var evidencePlaceholderText: TextView? = null
    private var riskLabel: TextView? = null
    private var summaryText: TextView? = null
    private var progressPanel: LinearLayout? = null
    private var primaryButton: Button? = null
    private var alarmRunnable: Runnable? = null
    private var reviewRunnable: Runnable? = null
    private var toneGenerator: ToneGenerator? = null
    private var alarmPending = false
    private var manualAlarmScheduled = false
    private var scenarioBannerText: TextView? = null
    private var alarmMessageText: TextView? = null
    private var alarmLocationText: TextView? = null
    private var activeMenu: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentScenario = loadSelectedScenario()
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        setContentView(buildScreen())
        renderState()
    }

    override fun onDestroy() {
        alarmRunnable?.let(handler::removeCallbacks)
        reviewRunnable?.let(handler::removeCallbacks)
        handler.removeCallbacksAndMessages(null)
        toneGenerator?.release()
        toneGenerator = null
        super.onDestroy()
    }

    private fun buildScreen(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(backgroundColor) }
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val horizontalPadding = pageHorizontalPadding()
            setPadding(horizontalPadding, dp(18), horizontalPadding, dp(30))
        }
        scrollView.addView(content, matchParentWrapContent())
        root.addView(scrollView, matchParent())

        buildHeader(content)
        buildStationBanner(content)
        buildOrderSection(content)
        buildFooter(content)
        buildAlertPanel(root)
        return root
    }

    private fun buildHeader(parent: LinearLayout) {
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titleBlock = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBlock.addView(label("智慧车站", 24f, textPrimaryColor, Typeface.BOLD), wrapContent())
        titleBlock.addView(label("现场工单", 13f, textSecondaryColor, Typeface.NORMAL).apply {
            setPadding(0, dp(3), 0, 0)
        }, wrapContent())
        header.addView(titleBlock, weightParams(1f))

        val menuButton = Button(this).apply {
            text = "⋯"
            textSize = 24f
            setTextColor(textSecondaryColor)
            background = roundedDrawable(Color.TRANSPARENT, Color.rgb(55, 86, 99), 1, 22)
            setPadding(0, 0, 0, dp(4))
            contentDescription = "更多操作"
            setOnClickListener { showDemoMenu() }
        }
        header.addView(menuButton, fixedParams(48, 44))
        parent.addView(header, wrapContent())
    }

    private fun buildStationBanner(parent: LinearLayout) {
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedDrawable(surfaceColor, Color.rgb(49, 91, 104), 1, 12)
        }
        banner.addView(label("合肥 · 云谷路站", 17f, textPrimaryColor, Typeface.BOLD), wrapContent())
        scenarioBannerText = label("", 12f, textSecondaryColor, Typeface.NORMAL).apply {
            setPadding(0, dp(5), 0, 0)
        }
        banner.addView(scenarioBannerText, wrapContent())
        parent.addView(banner, marginParams(matchParentWrapContent(), top = 18))
    }

    private fun buildOrderSection(parent: LinearLayout) {
        val sectionTitle = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(22), 0, dp(10))
        }
        sectionTitle.addView(label("当前工单", 18f, textPrimaryColor, Typeface.BOLD), weightParams(1f))
        statusText = label("等待新工单", 12f, textSecondaryColor, Typeface.NORMAL)
        sectionTitle.addView(statusText, wrapContent())
        parent.addView(sectionTitle, wrapContent())

        val orderCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedDrawable(surfaceColor, Color.rgb(49, 91, 104), 1, 14)
        }
        this.orderCard = orderCard

        val riskRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val riskBadge = label("运行正常", 12f, Color.rgb(103, 221, 200), Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(5), dp(9), dp(5))
            background = roundedDrawable(Color.rgb(24, 81, 82), Color.rgb(103, 221, 200), 1, 8)
        }
        riskLabel = riskBadge
        riskRow.addView(riskBadge, wrapContent())
        val source = label("  ·  现场监测正常", 12f, textSecondaryColor, Typeface.NORMAL)
        sourceText = source
        riskRow.addView(source, wrapContent())
        orderCard.addView(riskRow, wrapContent())

        orderTitleText = label("暂无待处理工单", 22f, textPrimaryColor, Typeface.BOLD).apply {
            setPadding(0, dp(13), 0, dp(5))
        }
        orderCard.addView(orderTitleText, wrapContent())
        orderLocationText = label("站点设备与客流监测正常", 14f, textSecondaryColor, Typeface.NORMAL)
        orderCard.addView(orderLocationText, wrapContent())

        summaryText = label("当前无待处理工单，系统正在持续监测站点状态。", 13f, textSecondaryColor, Typeface.NORMAL).apply {
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, dp(12), 0, 0)
        }
        orderCard.addView(summaryText, wrapContent())

        val evidenceContainer = FrameLayout(this).apply {
            setPadding(0, dp(14), 0, 0)
            visibility = View.GONE
        }
        evidenceFrame = evidenceContainer
        val evidence = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = roundedDrawable(Color.rgb(5, 22, 31), null, 0, 10)
        }
        evidenceImage = evidence
        evidenceContainer.addView(evidence, fixedHeightParams(matchParentWrapContent(), if (isTablet()) 240 else 168))
        val placeholderText = label("", 16f, Color.WHITE, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setLineSpacing(dp(4).toFloat(), 1f)
            visibility = View.GONE
        }
        evidencePlaceholderText = placeholderText
        evidenceContainer.addView(placeholderText, matchParent())
        val evidenceTag = label("机器狗 01  ·  12:08:03", 11f, textPrimaryColor, Typeface.NORMAL).apply {
            setPadding(dp(8), dp(5), dp(8), dp(5))
            background = roundedDrawable(Color.argb(190, 6, 25, 35), null, 0, 4)
        }
        this.evidenceTag = evidenceTag
        evidenceContainer.addView(evidenceTag, frameBottomStartParams(wrapContent()))
        orderCard.addView(evidenceContainer, matchParentWrapContent())

        progressPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, dp(3))
        }
        orderCard.addView(progressPanel, wrapContent())

        primaryButton = Button(this).apply {
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.rgb(6, 28, 34))
            background = roundedDrawable(accentColor, null, 0, 10)
            minHeight = dp(52)
            setOnClickListener { onPrimaryAction() }
        }
        orderCard.addView(primaryButton, marginParams(matchParentWrapContent(), top = 14))
        parent.addView(orderCard, matchParentWrapContent())
    }

    private fun buildFooter(parent: LinearLayout) {
        parent.addView(label("本地运行 · 无需联网", 11f, Color.rgb(103, 138, 149), Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }, matchParentWrapContent())
    }

    private fun buildAlertPanel(root: FrameLayout) {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(178, 30, 0, 5))
            visibility = View.GONE
            isClickable = true
            isFocusable = true
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(22), dp(22), dp(20))
            background = roundedDrawable(warningSurfaceColor, warningColor, 2, 18)
            elevation = dp(10).toFloat()
        }
        val alarmIcon = TextView(this).apply {
            text = "!"
            gravity = Gravity.CENTER
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = roundedDrawable(warningColor, null, 0, 40)
        }
        panel.addView(alarmIcon, fixedParams(58, 58))
        panel.addView(label("现场异常告警", 21f, textPrimaryColor, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setTextColor(warningBrightColor)
            setPadding(0, dp(15), 0, dp(3))
        }, matchParentWrapContent())
        alarmMessageText = label("", 14f, Color.WHITE, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, dp(4))
        }
        panel.addView(alarmMessageText, matchParentWrapContent())
        alarmLocationText = label("", 13f, textSecondaryColor, Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
        }
        panel.addView(alarmLocationText, matchParentWrapContent())
        panel.addView(label("请确认接收告警，再进入现场处理流程。", 12f, textSecondaryColor, Typeface.NORMAL).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(9), 0, 0)
        }, matchParentWrapContent())
        val action = Button(this).apply {
            text = "确认接收"
            textSize = 15f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedDrawable(warningColor, null, 0, 8)
            minHeight = dp(48)
            setOnClickListener { confirmAlarm() }
        }
        panel.addView(action, marginParams(matchParentWrapContent(), top = 17))
        val alertWidth = if (isTablet()) dp(560) else -1
        val params = FrameLayout.LayoutParams(alertWidth, -2).apply {
            gravity = Gravity.CENTER
            if (alertWidth == -1) setMargins(dp(28), 0, dp(28), 0)
        }
        overlay.addView(panel, params)
        alarmOverlay = overlay
        alarmCard = panel
        root.addView(overlay, matchParent())
    }

    private fun renderState() {
        val state = workOrderState.status
        scenarioBannerText?.text = "1 号线  ·  当前场景：${currentScenario.name}"
        alarmMessageText?.text = currentScenario.alarmMessage
        alarmLocationText?.text = "合肥 · 云谷路站  ·  ${currentScenario.location}"
        statusText?.text = if (!hasWorkOrder) {
            if (alarmPending) "待确认告警" else "运行正常"
        } else {
            statusLabel(state)
        }
        statusText?.setTextColor(
            if (alarmPending || (hasWorkOrder && state != WorkOrderStatus.COMPLETED)) {
                warningBrightColor
            } else {
                accentColor
            }
        )
        statusText?.typeface = Typeface.create(
            Typeface.DEFAULT,
            if (alarmPending || hasWorkOrder) Typeface.BOLD else Typeface.NORMAL
        )
        val activeAlert = alarmPending || (hasWorkOrder && state != WorkOrderStatus.COMPLETED)
        orderCard?.background = roundedDrawable(
            surfaceColor,
            if (activeAlert) warningColor else Color.rgb(49, 91, 104),
            if (activeAlert) 2 else 1,
            14
        )
        sourceText?.text = if (!hasWorkOrder) "  ·  现场监测正常" else "  ·  ${currentScenario.source}"
        sourceText?.setTextColor(if (hasWorkOrder) warningBrightColor else textSecondaryColor)
        orderTitleText?.text = if (!hasWorkOrder) "暂无待处理工单" else currentScenario.name
        orderLocationText?.text = if (!hasWorkOrder) "站点设备与客流监测正常" else currentScenario.location
        evidenceFrame?.visibility = if (hasWorkOrder) View.VISIBLE else View.GONE
        evidenceImage?.apply {
            setImageResource(currentScenario.evidenceResId)
            contentDescription = "${currentScenario.name}现场证据"
        }
        evidencePlaceholderText?.apply {
            visibility = if (currentScenario.evidenceResId == R.drawable.evidence_default) {
                View.VISIBLE
            } else {
                View.GONE
            }
            text = "${currentScenario.name}\n现场监控画面"
        }
        evidenceTag?.text = currentScenario.evidenceLabel
        riskLabel?.apply {
            val alertState = hasWorkOrder && state != WorkOrderStatus.COMPLETED
            text = when {
                !hasWorkOrder -> "运行正常"
                state == WorkOrderStatus.COMPLETED -> "已恢复"
                else -> currentScenario.riskLevel
            }
            setTextColor(if (alertState) warningBrightColor else Color.rgb(103, 221, 200))
            background = if (alertState) {
                roundedDrawable(warningBadgeColor, warningBrightColor, 1, 8)
            } else {
                roundedDrawable(Color.rgb(24, 81, 82), Color.rgb(103, 221, 200), 1, 8)
            }
        }
        summaryText?.text = if (!hasWorkOrder) {
            when {
                alarmPending -> "新的现场告警已到达，请在中央弹窗中确认接收。"
                manualAlarmScheduled -> "模拟告警将在 10 秒后触发，请留意声音与震动提醒。"
                else -> "当前无待处理工单，系统正在持续监测站点状态。"
            }
        } else {
            stateDescription(state)
        }
        summaryText?.setTextColor(if (alarmPending) warningBrightColor else textSecondaryColor)
        primaryButton?.apply {
            text = if (!hasWorkOrder) {
                when {
                    alarmPending -> "等待确认告警"
                    manualAlarmScheduled -> "告警倒计时中"
                    else -> "警告"
                }
            } else {
                primaryActionLabel(state)
            }
            isEnabled = if (!hasWorkOrder) {
                !alarmPending && !manualAlarmScheduled
            } else {
                state != WorkOrderStatus.REVIEWING
            }
            val activeButton = hasWorkOrder && state != WorkOrderStatus.COMPLETED
            setTextColor(
                when {
                    !isEnabled -> textSecondaryColor
                    activeButton -> Color.WHITE
                    else -> Color.rgb(6, 28, 34)
                }
            )
            background = roundedDrawable(
                when {
                    !isEnabled -> Color.rgb(72, 34, 42)
                    activeButton -> warningColor
                    else -> accentColor
                },
                null,
                0,
                10
            )
        }
        renderProgress(state, hasWorkOrder)
    }

    private fun renderProgress(state: WorkOrderStatus, visible: Boolean) {
        val panel = progressPanel ?: return
        panel.removeAllViews()
        if (!visible) {
            panel.addView(label("等待系统派发新的现场工单", 13f, textSecondaryColor, Typeface.NORMAL), wrapContent())
            return
        }

        panel.addView(label("处理进度", 13f, textSecondaryColor, Typeface.BOLD), wrapContent())
        val steps = listOf(
            WorkOrderStatus.WAITING to "工单派发",
            WorkOrderStatus.TRAVELING to "前往现场",
            WorkOrderStatus.ARRIVED to "到达点位",
            WorkOrderStatus.PROCESSING to "现场处理",
            WorkOrderStatus.REVIEWING to "系统复核",
            WorkOrderStatus.COMPLETED to "工单关闭"
        )
        val currentIndex = steps.indexOfFirst { it.first == state }.coerceAtLeast(0)
        steps.forEachIndexed { index, step ->
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
            }
            val dotColor = when {
                index < currentIndex -> accentColor
                index == currentIndex -> if (state == WorkOrderStatus.COMPLETED) accentColor else warningColor
                else -> Color.rgb(71, 101, 111)
            }
            val dot = TextView(this).apply {
                text = if (index < currentIndex || state == WorkOrderStatus.COMPLETED && index == currentIndex) "✓" else ""
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(Color.rgb(6, 28, 34))
                background = roundedDrawable(dotColor, null, 0, 20)
            }
            row.addView(dot, fixedParams(22, 22))
            val title = label(step.second, 13f, if (index <= currentIndex) textPrimaryColor else textSecondaryColor, if (index == currentIndex) Typeface.BOLD else Typeface.NORMAL).apply {
                setPadding(dp(10), 0, 0, 0)
            }
            row.addView(title, weightParams(1f))
            if (index == currentIndex) {
                row.addView(
                    label(
                        statusLabel(state),
                        11f,
                        if (state == WorkOrderStatus.COMPLETED) accentColor else warningBrightColor,
                        Typeface.NORMAL
                    ),
                    wrapContent()
                )
            }
            panel.addView(row, wrapContent())
        }
    }

    private fun onPrimaryAction() {
        if (!hasWorkOrder) {
            scheduleManualAlarm()
            return
        }
        if (workOrderState.status == WorkOrderStatus.REVIEWING) return
        if (workOrderState.status == WorkOrderStatus.COMPLETED) {
            resetDemo()
            return
        }
        workOrderState = workOrderState.advance()
        if (workOrderState.status == WorkOrderStatus.REVIEWING) startReview()
        renderState()
    }

    private fun triggerNewOrder() {
        alarmRunnable?.let(handler::removeCallbacks)
        reviewRunnable?.let(handler::removeCallbacks)
        workOrderState = WorkOrderState.newOrder()
        hasWorkOrder = false
        alarmPending = false
        manualAlarmScheduled = false
        renderState()
        showAlarm()
    }

    private fun scheduleManualAlarm() {
        alarmRunnable?.let(handler::removeCallbacks)
        reviewRunnable?.let(handler::removeCallbacks)
        workOrderState = WorkOrderState.newOrder()
        hasWorkOrder = false
        alarmPending = false
        manualAlarmScheduled = true
        renderState()
        alarmRunnable = Runnable {
            manualAlarmScheduled = false
            showAlarm()
        }
        handler.postDelayed(alarmRunnable!!, MANUAL_ALARM_DELAY_MS)
    }

    private fun confirmAlarm() {
        if (!alarmPending) return
        alarmPending = false
        hasWorkOrder = true
        workOrderState = WorkOrderState.newOrder()
        renderState()

        val overlay = alarmOverlay ?: return
        overlay.animate()
            .alpha(0f)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                overlay.visibility = View.GONE
                overlay.alpha = 1f
                alarmCard?.scaleX = 1f
                alarmCard?.scaleY = 1f
                alarmCard?.alpha = 1f
            }
            .start()
    }

    private fun startReview() {
        reviewRunnable?.let(handler::removeCallbacks)
        reviewRunnable = Runnable {
            workOrderState = workOrderState.advance()
            renderState()
            Toast.makeText(this, currentScenario.completionMessage, Toast.LENGTH_SHORT).show()
        }
        handler.postDelayed(reviewRunnable!!, 1800)
    }

    private fun resetDemo() {
        alarmRunnable?.let(handler::removeCallbacks)
        reviewRunnable?.let(handler::removeCallbacks)
        workOrderState = WorkOrderState.newOrder()
        hasWorkOrder = false
        alarmPending = false
        manualAlarmScheduled = false
        alarmOverlay?.animate()?.cancel()
        alarmOverlay?.visibility = View.GONE
        alarmOverlay?.alpha = 1f
        renderState()
    }

    private fun showAlarm() {
        if (hasWorkOrder || alarmPending) return
        manualAlarmScheduled = false
        alarmPending = true
        renderState()
        val overlay = alarmOverlay ?: return
        val card = alarmCard ?: return
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        card.alpha = 0f
        card.scaleX = 0.84f
        card.scaleY = 0.84f
        overlay.animate()
            .alpha(1f)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
            .start()
        card.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(360)
            .setInterpolator(DecelerateInterpolator())
            .start()
        val vibrator = getSystemService(Vibrator::class.java)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 100, 180), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 180, 100, 180), -1)
            }
        }
        if (toneGenerator == null) toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 260)
    }

    private fun showDemoMenu() {
        activeMenu?.let(::removeMenu)
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
            background = roundedDrawable(surfaceLightColor, Color.rgb(66, 111, 121), 1, 12)
        }
        activeMenu = menu
        val selectScenario = menuButton("选择告警场景") {
            removeMenu(menu)
            showScenarioPicker()
        }
        val reset = menuButton("重置流程") {
            resetDemo()
            Toast.makeText(this@MainActivity, "流程已重置", Toast.LENGTH_SHORT).show()
            removeMenu(menu)
        }
        val trigger = menuButton("重新触发工单") {
            triggerNewOrder()
            removeMenu(menu)
        }
        menu.addView(selectScenario, fixedHeightParams(matchParentWrapContent(), 46))
        menu.addView(reset, fixedHeightParams(matchParentWrapContent(), 46))
        menu.addView(trigger, fixedHeightParams(matchParentWrapContent(), 46))
        val root = window.decorView as? ViewGroup ?: return
        val params = FrameLayout.LayoutParams(dp(190), -2).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, dp(72), dp(14), 0)
        }
        root.addView(menu, params)
        menu.postDelayed({ removeMenu(menu) }, 5000)
    }

    private fun menuButton(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        setTextColor(textPrimaryColor)
        background = ColorDrawable(Color.TRANSPARENT)
        setOnClickListener { action() }
    }

    private fun removeMenu(menu: View) {
        (menu.parent as? ViewGroup)?.removeView(menu)
        if (activeMenu === menu) activeMenu = null
    }

    private fun showScenarioPicker() {
        val scenarios = DemoScenarioCatalog.all
        val selectedIndex = scenarios.indexOfFirst { it.id == currentScenario.id }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("选择告警场景")
            .setSingleChoiceItems(scenarios.map { it.name }.toTypedArray(), selectedIndex) { dialog, index ->
                selectScenario(scenarios[index])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun selectScenario(scenario: DemoScenario) {
        currentScenario = scenario
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCE_SCENARIO_ID, scenario.id)
            .apply()
        resetDemo()
        Toast.makeText(this, "已切换为：${scenario.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadSelectedScenario(): DemoScenario {
        val selectedId = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(PREFERENCE_SCENARIO_ID, null)
        return DemoScenarioCatalog.findById(selectedId)
    }

    private fun statusLabel(status: WorkOrderStatus): String = when (status) {
        WorkOrderStatus.WAITING -> "待处理"
        WorkOrderStatus.TRAVELING -> "前往现场"
        WorkOrderStatus.ARRIVED -> "已到达"
        WorkOrderStatus.PROCESSING -> "处理中"
        WorkOrderStatus.REVIEWING -> "系统复核"
        WorkOrderStatus.COMPLETED -> "已完成"
    }

    private fun stateDescription(status: WorkOrderStatus): String = when (status) {
        WorkOrderStatus.WAITING -> currentScenario.waitingDescription
        WorkOrderStatus.TRAVELING -> currentScenario.travelingDescription
        WorkOrderStatus.ARRIVED -> currentScenario.arrivedDescription
        WorkOrderStatus.PROCESSING -> currentScenario.processingDescription
        WorkOrderStatus.REVIEWING -> currentScenario.reviewingDescription
        WorkOrderStatus.COMPLETED -> currentScenario.completedDescription
    }

    private fun primaryActionLabel(status: WorkOrderStatus): String = when (status) {
        WorkOrderStatus.WAITING -> "去处理"
        WorkOrderStatus.TRAVELING -> "我已到达"
        WorkOrderStatus.ARRIVED -> "开始处理"
        WorkOrderStatus.PROCESSING -> "完成处理"
        WorkOrderStatus.REVIEWING -> "复核中"
        WorkOrderStatus.COMPLETED -> "再次模拟"
    }

    private fun label(text: String, size: Float, color: Int, style: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        typeface = Typeface.create(Typeface.DEFAULT, style)
        includeFontPadding = true
    }

    private fun roundedDrawable(fill: Int, stroke: Int?, strokeWidth: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null && strokeWidth > 0) setStroke(dp(strokeWidth), stroke)
        }

    private fun isTablet(): Boolean = resources.configuration.smallestScreenWidthDp >= 600

    private fun pageHorizontalPadding(): Int = dp(
        when {
            isTablet() -> 32
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE -> 28
            else -> 20
        }
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun matchParent(): ViewGroup.LayoutParams = ViewGroup.LayoutParams(-1, -1)

    private fun matchParentWrapContent(): ViewGroup.LayoutParams = ViewGroup.LayoutParams(-1, -2)

    private fun wrapContent(): ViewGroup.LayoutParams = ViewGroup.LayoutParams(-2, -2)

    private fun fixedParams(width: Int, height: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(width), dp(height))

    private fun fixedHeightParams(params: ViewGroup.LayoutParams, height: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(params.width, dp(height))

    private fun weightParams(weight: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, -2, weight)

    private fun marginParams(params: ViewGroup.LayoutParams, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(params.width, params.height).apply { topMargin = dp(top) }

    private fun frameBottomStartParams(params: ViewGroup.LayoutParams): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(params.width, params.height).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            setMargins(dp(8), 0, 0, dp(8))
        }

    companion object {
        private const val PREFERENCES_NAME = "demo_settings"
        private const val PREFERENCE_SCENARIO_ID = "selected_scenario_id"
        private const val MANUAL_ALARM_DELAY_MS = 10_000L
    }
}
