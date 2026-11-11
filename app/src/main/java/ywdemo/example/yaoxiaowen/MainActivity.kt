package ywdemo.example.yaoxiaowen

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val app get() = application as StationApplication
    private val repo get() = app.repository
    private lateinit var root: LinearLayout
    private var scroll: ScrollView? = null
    private var body: LinearLayout? = null
    private var titleView: TextView? = null
    private var editor: EditText? = null
    private var editorRevision = 0L
    private var previewStatus: TextView? = null
    private var photoArea: LinearLayout? = null
    private var textError: TextView? = null
    private var photoError: TextView? = null
    private var bottomAction: LinearLayout? = null
    private var inlineAction: LinearLayout? = null
    private val formControls = mutableListOf<View>()
    private val submitButtons = mutableListOf<TextView>()
    private var photoSignature = ""
    private var page = "list"
    private var selectedId: String? = null
    private var tab = OrderStatus.PENDING
    private var listPosition = 0
    private var ready = false
    private var building = false
    private var editing = false
    private var debugTarget: String? = null
    private var sceneMode = SceneMode.DEFAULT
    private var selectedScenes = SceneCatalog.defaults.toMutableList()
    private var minuteText = "1"
    private var debugSummary: TextView? = null
    private var restartButton: TextView? = null
    private var previewReturn = "detail"
    private var previewOriginal = false
    private var previewIndex = 0
    private var titleGate = TitleTapGate()
    private var titleDownX = 0f
    private var titleDownY = 0f
    private var titleDownAt = 0L
    private var titleTouch = false
    private var titleCancelled = false
    private var lastErrorAt = 0L
    private var lastError = ""
    private val changed: () -> Unit = { if (ready && !isFinishing && !building) onDataChanged() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (savedInstanceState == null) app.fast = false
            tab = repo.state.tab
            listPosition = repo.state.listPosition
            savedInstanceState?.let {
                page = it.getString("page", "list")
                selectedId = it.getString("selectedId")
                previewReturn = it.getString("previewReturn", "detail")
                previewOriginal = it.getBoolean("previewOriginal")
                previewIndex = it.getInt("previewIndex")
                debugTarget = it.getString("debugTarget")
                sceneMode = SceneMode.valueOf(it.getString("sceneMode", "DEFAULT"))
                selectedScenes = it.getStringArrayList("selectedScenes") ?: SceneCatalog.defaults.toMutableList()
                minuteText = it.getString("minutes", "1")
            }
            if (savedInstanceState == null) repo.state.pendingMedia?.let { selectedId = it.orderId; page = "form" }
            if (savedInstanceState == null && intent.action == ArrivalService.OPEN_PENDING) {
                page = "list"; tab = OrderStatus.PENDING; listPosition = 0; app.arrival.clearNotification()
                setIntent(Intent(intent).setAction(null))
            }
            if (page !in listOf("list", "debug") && current() == null) page = "list"
            ready = true
            app.listeners += changed
            render()
            savedInstanceState?.getString("unsavedText")?.takeIf { page == "form" }?.let { editor?.setText(it) }
        } catch (_: Exception) {
            showLoadFailure()
        }
    }
    override fun onResume() {
        super.onResume()
        if (!ready) return
        app.reportError = { message -> toast(message, true) }
        runAction("来单计划检查失败，请稍后重试") { app.reconcile() }
        app.startOperations()
        if (page == "debug") render(true)
    }
    override fun onPause() {
        if (ready) { flushEditor(); if (page == "list") rememberList() }
        super.onPause()
    }
    override fun onStop() { app.stopOperations(); super.onStop() }
    override fun onDestroy() { app.listeners -= changed; app.reportError = null; super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("page", page); outState.putString("selectedId", selectedId)
        outState.putString("previewReturn", previewReturn); outState.putBoolean("previewOriginal", previewOriginal); outState.putInt("previewIndex", previewIndex)
        outState.putString("debugTarget", debugTarget); outState.putString("sceneMode", sceneMode.name)
        outState.putStringArrayList("selectedScenes", ArrayList(selectedScenes)); outState.putString("minutes", minuteText)
        if (page == "form") outState.putString("unsavedText", editor?.text?.toString())
        super.onSaveInstanceState(outState)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        if (intent.action == ArrivalService.OPEN_PENDING && ready) {
            if (!flushEditor()) return
            tab = OrderStatus.PENDING; listPosition = 0; page = "list"
            app.arrival.clearNotification(); runAction { repo.saveList(tab, 0) }; setIntent(Intent(intent).setAction(null)); render()
        }
    }
    private fun current() = selectedId?.let(repo::order)
    private fun time(value: Long, full: Boolean = false) = SimpleDateFormat(if (full) "yyyy/MM/dd HH:mm:ss" else "MM/dd HH:mm", Locale.CHINA).format(Date(value))
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun text(value: String, size: Float = 16f, color: Int = StationColors.ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); includeFontPadding = false
        setLineSpacing(dp(3).toFloat(), 1.12f)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }
    private fun add(parent: LinearLayout, view: View, gap: Int = 0) {
        parent.addView(view)
        if (gap > 0) (view.layoutParams as LinearLayout.LayoutParams).bottomMargin = dp(gap)
    }
    private fun card(parent: LinearLayout, padding: Int = 14): LinearLayout = column().also {
        it.setPadding(dp(padding), dp(15), dp(padding), dp(14)); it.background = rounded(StationColors.surface, 14, StationColors.line)
        add(parent, it, 12)
    }
    private fun button(label: String, primary: Boolean = false, action: () -> Unit): TextView = text(label, if (primary) 16f else 14f,
        if (primary) Color.parseColor("#07382F") else StationColors.ink, primary).apply {
        gravity = Gravity.CENTER; minHeight = dp(if (primary) 52 else 48); setPadding(dp(12), dp(12), dp(12), dp(12))
        touchSurface(if (primary) StationColors.mint else StationColors.secondary, 10, if (primary) null else Color.parseColor("#36556D"))
        setOnClickListener { action() }; setOnLongClickListener { true }
    }
    private fun enable(view: View, enabled: Boolean) { view.isEnabled = enabled; view.alpha = if (enabled) 1f else .42f }
    private fun iconButton(kind: String, label: String, action: () -> Unit) = FrameLayout(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)); touchSurface(Color.TRANSPARENT)
        contentDescription = label; addView(StationIcon(this@MainActivity, kind, StationColors.ink), FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        setOnClickListener { action() }
    }
    private fun label(parent: LinearLayout, value: String, gap: Int = 10) = text(value, 16f, StationColors.ink, true).also { add(parent, it, gap) }
    private fun section(parent: LinearLayout, title: String): LinearLayout = card(parent).also { label(it, title) }
    private fun divider(parent: LinearLayout, margin: Int = 12) {
        parent.addView(View(this).apply { setBackgroundColor(StationColors.line) }, LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(margin); bottomMargin = dp(margin) })
    }
    private fun location(parent: LinearLayout, scene: Scene, compact: Boolean = false) {
        val row = row().apply { gravity = Gravity.TOP }
        row.addView(StationIcon(this, "location"), LinearLayout.LayoutParams(dp(16), dp(19)).apply { topMargin = dp(2); rightMargin = dp(7) })
        row.addView(text(scene.location, if (compact) 14f else 16f, Color.parseColor("#C4D3E2")), LinearLayout.LayoutParams(0, -2, 1f))
        add(parent, row)
    }
    private fun badge(status: OrderStatus): TextView {
        val colors = when (status) {
            OrderStatus.PENDING -> "#F2C484" to "#443A30"
            OrderStatus.PROCESSING -> "#9DCCFF" to "#1E3F62"
            OrderStatus.DONE -> "#7BE0CA" to "#163C3E"
        }
        return text(status.label, 14f, Color.parseColor(colors.first)).apply { setPadding(dp(8), dp(4), dp(8), dp(4)); background = rounded(Color.parseColor(colors.second), 5); layoutParams = LinearLayout.LayoutParams(-2, -2) }
    }
    private fun makeHeader(title: String) {
        val header = row().apply { setPadding(dp(3), dp(8), dp(12), dp(6)); minimumHeight = dp(64) }
        header.addView(iconButton("back", "返回") { back() })
        header.addView(text(title, 20f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)
    }
    private fun makeScroll(): LinearLayout {
        val content = column().apply { setPadding(dp(12), dp(12), dp(12), dp(12)) }
        scroll = ScrollView(this).apply { isFillViewport = false; clipToPadding = false; addView(content); layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        root.addView(scroll); body = content
        return content
    }
    private fun footer(label: String, tip: String? = null, action: () -> Unit): LinearLayout = column().also { footer ->
        footer.setPadding(dp(12), dp(10), dp(12), dp(12)); footer.setBackgroundColor(Color.parseColor("#0D1D30"))
        tip?.let { add(footer, text(it, 14f, StationColors.muted).apply { gravity = Gravity.CENTER }, 8) }
        add(footer, button(label, true, action)); root.addView(footer)
    }
    private fun summary(parent: LinearLayout, order: Order) {
        card(parent).also { label(it, order.scene.title, 8); location(it, order.scene, true) }
    }
    private fun render(keepScroll: Boolean = false) {
        if (!ready) return
        if (page in listOf("detail", "result", "form") && current()?.status == OrderStatus.DONE) page = "complete"
        building = true
        val position = if (keepScroll) scroll?.scrollY ?: 0 else 0
        titleGate.reset()
        editor = null; photoArea = null; textError = null; photoError = null; titleView = null
        bottomAction = null; inlineAction = null; formControls.clear(); submitButtons.clear(); photoSignature = ""
        root = column().apply { setBackgroundColor(StationColors.background); isFocusableInTouchMode = true }
        setContentView(root)
        when (page) {
            "list" -> renderList()
            "detail" -> renderDetail()
            "result" -> renderResults()
            "form" -> renderForm()
            "complete" -> renderComplete()
            "debug" -> renderDebug()
            "preview" -> renderPreview()
        }
        if (page != "preview") scroll?.post { scroll?.scrollTo(0, if (page == "list") listPosition else position) }
        building = false
    }
    private fun renderList() {
        val header = row().apply { setPadding(dp(12), dp(12), dp(12), dp(8)); minimumHeight = dp(83) }
        val titles = column()
        add(titles, text("智慧车站", 14f, StationColors.muted), 3)
        titleView = text("我的工单", 20f, bold = true).apply {
            minHeight = dp(48); gravity = Gravity.CENTER_VERTICAL
            setOnClickListener { if (!titleCancelled && titleGate.tap(SystemClock.elapsedRealtime())) { debugTarget = null; go("debug") } }
            setOnLongClickListener { titleCancelled = true; titleGate.reset(); true }
        }
        add(titles, titleView!!)
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        val identity = row()
        identity.addView(text("李", 14f, StationColors.mint).apply { gravity = Gravity.CENTER; background = rounded(Color.parseColor("#1B3E44"), 16) }, LinearLayout.LayoutParams(dp(32), dp(32)))
        identity.addView(text("李明", 14f).apply { setPadding(dp(7), 0, 0, 0) }, LinearLayout.LayoutParams(-2, -2))
        header.addView(identity); root.addView(header)
        val tabs = row().apply { setPadding(dp(12), 0, dp(12), 0) }
        OrderStatus.values().forEach { status ->
            val holder = column()
            val count = repo.state.orders.count { it.status == status }
            add(holder, text("${status.label} $count", 14f, if (tab == status) StationColors.mint else StationColors.muted, tab == status).apply {
                gravity = Gravity.CENTER; minHeight = dp(54); setPadding(0, dp(9), 0, dp(9)); touchSurface(Color.TRANSPARENT)
                contentDescription = "${status.label}，$count 张工单"
                setOnClickListener { tab = status; listPosition = 0; runAction { repo.saveList(tab, 0) }; render() }
            })
            holder.addView(View(this).apply { setBackgroundColor(if (tab == status) StationColors.mint else StationColors.line) }, LinearLayout.LayoutParams(-1, dp(3)).apply { leftMargin = dp(7); rightMargin = dp(7) })
            tabs.addView(holder, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(tabs)
        val content = makeScroll()
        add(content, text("云谷路地铁站", 14f, StationColors.muted), 12)
        val shown = repo.state.orders.filter { it.status == tab }
        if (shown.isEmpty()) {
            val empty = column().apply { gravity = Gravity.CENTER; minimumHeight = dp(240); setPadding(dp(8), dp(32), dp(8), dp(32)) }
            empty.addView(StationIcon(this, "empty"), LinearLayout.LayoutParams(dp(44), dp(44)).apply { bottomMargin = dp(18) })
            add(empty, text("暂无${tab.label}工单", 16f).apply { gravity = Gravity.CENTER }, 12)
            add(empty, text(when (tab) { OrderStatus.PENDING -> "新工单到达后将显示在这里"; OrderStatus.PROCESSING -> "开始处理后可在这里继续"; OrderStatus.DONE -> "完成记录将保存在这里" }, 14f, StationColors.muted).apply { gravity = Gravity.CENTER })
            add(content, empty)
        }
        shown.forEach { order ->
            val card = card(content)
            card.touchSurface(StationColors.surface, 14, StationColors.line)
            card.contentDescription = "${order.scene.title}，${order.scene.location}，${order.status.label}"
            val top = row()
            top.addView(StationIcon(this, "scan"), LinearLayout.LayoutParams(dp(16), dp(16)).apply { rightMargin = dp(7) })
            top.addView(text("${order.scene.device} 巡检", 14f, StationColors.muted), LinearLayout.LayoutParams(0, -2, 1f)); top.addView(badge(order.status))
            add(card, top, 10); add(card, text(order.scene.title, 18f, bold = true), 8); location(card, order.scene)
            if (order.status == OrderStatus.PROCESSING && order.hasDraft) add(card, text("有未提交材料", 14f, StationColors.mint).apply { setPadding(0, dp(8), 0, 0) })
            divider(card, 10)
            val meta = row().apply { gravity = Gravity.TOP }
            meta.addView(text(time(order.receivedAt), 14f, StationColors.muted), LinearLayout.LayoutParams(0, -2, .36f))
            meta.addView(text(order.number, 14f, StationColors.muted).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, .64f))
            add(card, meta)
            card.setOnClickListener { selectedId = order.id; go(if (order.status == OrderStatus.DONE) "complete" else "detail") }
        }
    }
    private fun renderDetail() {
        val order = current() ?: return
        makeHeader("工单详情"); val content = makeScroll()
        card(content).also {
            val top = row(); top.addView(badge(order.status)); top.addView(text("${order.scene.device} 巡检", 14f, StationColors.muted).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f)); add(it, top, 12)
            add(it, text(order.scene.title, 20f, bold = true)); divider(it); location(it, order.scene)
        }
        section(content, "问题说明").also { add(it, text(order.scene.description, 16f, Color.parseColor("#C5D5E3")).apply { setLineSpacing(dp(5).toFloat(), 1.2f) }) }
        evidence(content, order)
        section(content, "工单信息").also {
            fact(it, "工单编号", order.number); fact(it, "接收时间", time(order.receivedAt)); fact(it, "处理人", "李明")
            order.startedAt?.let { stamp -> fact(it, "开始时间", time(stamp)) }
        }
        order.startedAt?.let { stamp -> section(content, "处理记录").also { add(it, text("李明开始处理", 14f), 6); add(it, text(time(stamp, true), 14f, StationColors.muted)) } }
        if (order.status == OrderStatus.PENDING) footer("开始处理  →", "到达现场后再开始") {
            if (runAction { repo.start(order.id) }) { render(true); toast("已开始处理") }
        } else footer(if (order.hasDraft) "继续填写  →" else "填写结果  →") { go(if (order.selected == null) "result" else "form") }
    }
    private fun fact(parent: LinearLayout, name: String, value: String) {
        if (name.length > 6) {
            add(parent, text(name, 14f), 5); add(parent, text(value, 14f, StationColors.muted), 12)
        } else {
            val line = row().apply { gravity = Gravity.TOP }
            line.addView(text(name, 14f, StationColors.muted), LinearLayout.LayoutParams(0, -2, .3f))
            line.addView(text(value, 14f).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, .7f))
            add(parent, line, 10)
        }
    }
    private fun evidence(parent: LinearLayout, order: Order) {
        val card = section(parent, "原始证据")
        val image = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER; setBackgroundColor(Color.parseColor("#0A1623")); contentDescription = "${order.scene.check}原始检查照片" }
        card.addView(image, LinearLayout.LayoutParams(-1, dp(180))); loadImage(image, order.scene.image, true, 1000)
        image.setOnClickListener { openPreview(true, 0) }; image.isFocusable = true
        add(card, button("查看原图") { openPreview(true, 0) }, 8)
        add(card, text("巡检机器狗 ${order.scene.device} · 原始检查照片", 14f, StationColors.muted))
    }
    private fun renderResults() {
        val order = current() ?: return
        makeHeader("选择现场结果"); val content = makeScroll(); summary(content, order)
        add(content, text("请根据现场情况选择一项结果。", 14f, StationColors.muted), 16)
        ResultKind.values().forEach { kind ->
            val option = card(content)
            option.touchSurface(StationColors.surface, 14, Color.parseColor("#30475D")); option.minimumHeight = dp(104)
            val line = row().apply { gravity = Gravity.TOP }
            val symbol = FrameLayout(this).apply {
                background = rounded(Color.parseColor("#1C4346"), 10)
                addView(StationIcon(this@MainActivity, when (kind) { ResultKind.HANDLED -> "check"; ResultKind.NORMAL -> "shield"; ResultKind.PARTS -> "wrench" }, StationColors.mint), FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER))
            }
            line.addView(symbol, LinearLayout.LayoutParams(dp(35), dp(35)).apply { rightMargin = dp(12) })
            val copy = column(); add(copy, text(kind.label, 18f, bold = true), 8); add(copy, text(kind.description, 14f, StationColors.muted))
            line.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(StationIcon(this, "next"), LinearLayout.LayoutParams(dp(17), dp(17)).apply { leftMargin = dp(6); topMargin = dp(10) })
            add(option, line)
            option.contentDescription = "${kind.label}，${kind.description}"
            option.setOnClickListener { if (runAction { repo.selectResult(order.id, kind, order.revision) }) go("form") }
        }
    }
    private fun renderForm() {
        val order = current() ?: return
        val kind = order.selected ?: run { page = "result"; render(); return }
        makeHeader("填写材料"); val content = makeScroll(); summary(content, order)
        val currentResult = row()
        currentResult.addView(text(kind.label, 16f, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        val change = button("更换结果") { if (flushEditor()) go("result") }.apply { setTextColor(StationColors.mint); background = null }
        currentResult.addView(change, LinearLayout.LayoutParams(-2, -2)); formControls += change; add(content, currentResult, 8)
        val field = card(content)
        label(field, "${kind.field} *")
        editorRevision = order.revision
        editor = EditText(this).apply {
            id = View.generateViewId(); tag = "material-input"; textSize = 16f; setTextColor(StationColors.ink); setHintTextColor(Color.parseColor("#8199AF"))
            hint = "请填写${kind.field}"; gravity = Gravity.TOP; minLines = 4; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.parseColor("#0C1E32"), 10, Color.parseColor("#3A536D"))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setText(order.branch().text)
            setOnEditorActionListener { _, _, _ -> false }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (building || editing) return
                    textError?.visibility = View.GONE
                    runAction { repo.edit(order.id, kind, s.toString(), order.revision) }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        add(field, editor!!); formControls += editor!!
        textError = errorText(field)
        if (kind.photoLabel != null) {
            val photos = section(content, "${kind.photoLabel} *")
            add(photos, text("至少添加 1 张照片", 14f, StationColors.muted), 10)
            val camera = button("拍照") { launchMedia(true) }; val album = button("从相册选择") { launchMedia(false) }
            if (resources.configuration.fontScale > 1.15f) {
                add(photos, camera, 8); add(photos, album, 4)
            } else {
                val entries = row()
                entries.addView(camera, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
                entries.addView(album, LinearLayout.LayoutParams(0, -2, 1f)); add(photos, entries, 4)
            }
            formControls += camera; formControls += album
            photoArea = column(); add(photos, photoArea!!); photoError = errorText(photos)
        }
        inlineAction = column().also { inline ->
            inline.setPadding(0, dp(12), 0, dp(6)); val submit = button("提交并完成", true) { submit() }; submitButtons += submit; add(inline, submit); inline.visibility = View.GONE; add(content, inline)
        }
        bottomAction = footer("提交并完成") { submit() }
        submitButtons += bottomAction!!.getChildAt(0) as TextView
        root.viewTreeObserver.addOnGlobalLayoutListener {
            if (page != "form" || !ready) return@addOnGlobalLayoutListener
            val visible = Rect(); root.getWindowVisibleDisplayFrame(visible)
            val keyboard = if (Build.VERSION.SDK_INT >= 30) root.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
                else root.rootView.height - visible.bottom > root.rootView.height * .18
            inlineAction?.visibility = if (keyboard) View.VISIBLE else View.GONE
            bottomAction?.visibility = if (keyboard) View.GONE else View.VISIBLE
        }
        updateForm()
    }
    private fun errorText(parent: LinearLayout) = text("", 14f, StationColors.error).apply { setPadding(0, dp(8), 0, 0); visibility = View.GONE; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE; add(parent, this) }
    private fun showFieldError(view: TextView?, message: String?) { view?.text = message.orEmpty(); view?.visibility = if (message == null) View.GONE else View.VISIBLE }
    private fun updateForm() {
        val order = current() ?: return
        val kind = order.selected ?: return
        val busy = order.submitAt != null || app.importing
        formControls.forEach { enable(it, !busy) }
        submitButtons.forEach { it.text = if (order.submitAt != null) "正在提交…" else if (app.importing) "正在保存照片…" else "提交并完成"; enable(it, !busy) }
        val signature = order.branch().photos.joinToString { "${it.id}:${it.status}" } + order.branch().removed?.photo?.id + busy
        if (signature != photoSignature) { photoSignature = signature; photoArea?.let { it.removeAllViews(); photoRows(it, order, kind, true) } }
        order.submitError?.let { showFieldError(if (kind.photoLabel == null) textError else photoError, it) }
    }
    private fun photoRows(parent: LinearLayout, order: Order, kind: ResultKind, editable: Boolean) {
        val branch = order.branch(kind)
        branch.photos.forEachIndexed { index, photo ->
            val holder = column().apply { setPadding(dp(9), dp(9), dp(9), dp(9)); background = rounded(Color.parseColor("#112238"), 10, StationColors.line) }
            val row = row()
            val image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; contentDescription = "查看第 ${index + 1} 张照片"; isFocusable = true; setOnClickListener { openPreview(false, index) } }
            row.addView(image, LinearLayout.LayoutParams(dp(60), dp(68)).apply { rightMargin = dp(10) }); loadImage(image, photo.fileName, false, 180)
            val info = column(); add(info, text("照片 ${index + 1} · ${photo.source}", 14f), 5); add(info, text(photo.status.label, 14f, if (photo.status == UploadStatus.FAILED) StationColors.error else StationColors.mint))
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f)); add(holder, row)
            if (editable) add(holder, button("移除") { if (flushEditor() && runAction { repo.removePhoto(order.id, kind, photo.id) }) { app.photos.cleanup(repo); updateForm() } }.also { enable(it, order.submitAt == null && !app.importing) })
            add(parent, holder, 10)
        }
        if (editable && branch.removed != null) {
            add(parent, text("已移除照片", 14f, StationColors.muted))
            add(parent, button("撤销") { if (runAction { repo.undoPhoto(order.id, kind) }) updateForm() }.also { enable(it, order.submitAt == null && !app.importing) })
        }
    }
    private fun submit() {
        if (app.importing || !flushEditor()) return
        val order = current() ?: return
        if (order.submitAt != null) return
        val errors = StationRules.validation(order, app.photos::readable)
        showFieldError(textError, errors.first); showFieldError(photoError, errors.second)
        if (errors.first != null || errors.second != null) {
            val target = if (errors.first != null) editor else photoError
            target?.let { view -> scroll?.post { val rect = Rect(); view.getDrawingRect(rect); scroll?.offsetDescendantRectToMyCoords(view, rect); scroll?.smoothScrollTo(0, (rect.top - dp(24)).coerceAtLeast(0)) } }
            return
        }
        hideKeyboard()
        if (runAction { repo.submit(order.id, app.fast, app.photos::readable) }) updateForm()
    }
    private fun renderComplete() {
        val order = current() ?: return
        val kind = order.selected ?: return
        makeHeader("完成记录"); val content = makeScroll()
        val done = column().apply { gravity = Gravity.CENTER; setPadding(0, dp(14), 0, dp(22)) }
        val doneIcon = FrameLayout(this).apply {
            background = rounded(Color.parseColor("#1A4146"), 26)
            addView(StationIcon(this@MainActivity, "check", StationColors.mint), FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER))
        }
        done.addView(doneIcon, LinearLayout.LayoutParams(dp(52), dp(52)).apply { bottomMargin = dp(12) })
        add(done, text("工单已完成", 20f, bold = true).apply { gravity = Gravity.CENTER }, 8)
        add(done, text(if (kind == ResultKind.PARTS) "已记录配件需求，仍需配件支持" else "现场结果已保存", 14f, StationColors.muted).apply { gravity = Gravity.CENTER }); add(content, done)
        summary(content, order)
        section(content, "现场结果").also {
            add(it, text(kind.label, 16f, StationColors.mint, true), 14); label(it, kind.field); add(it, text(order.branch().text), 12)
            if (kind.photoLabel != null) { label(it, kind.photoLabel); photoRows(it, order, kind, false) }
        }
        add(content, text("✓  系统默认验收通过", 14f, StationColors.mint).apply { setPadding(dp(12), dp(14), dp(12), dp(14)); background = rounded(Color.parseColor("#123B3E"), 10) }, 12)
        section(content, "处理记录").also {
            fact(it, "李明提交并完成", time(requireNotNull(order.completedAt), true)); order.startedAt?.let { stamp -> fact(it, "李明开始处理", time(stamp, true)) }; fact(it, "工单编号", order.number)
        }
        evidence(content, order)
        footer("返回工单列表") { go("list") }
    }
    private fun renderDebug() {
        makeHeader("工单调试"); val content = makeScroll()
        val status = section(content, "状态信息")
        OrderStatus.values().forEach { add(status, text("${it.label}  ${repo.state.orders.count { order -> order.status == it }}", 14f, StationColors.muted), 6) }
        repo.state.plan?.let { plan ->
            if (plan.deliveredAt == null) {
                divider(status); label(status, "等待到达")
                fact(status, "预计到达时间", time(plan.dueAt, true)); fact(status, "工单数量", "${plan.sceneIds.size} 张")
                add(status, text(plan.sceneIds.joinToString("\n") { SceneCatalog.get(it).title }, 14f, StationColors.muted))
            }
        }
        val operations = section(content, "工单操作")
        label(operations, "作用工单", 8)
        val target = debugTarget?.let(repo::order)
        add(operations, button(target?.let { "${it.scene.title}\n${it.number}" } ?: "请选择作用工单") { chooseTarget() }, 10)
        if (target != null) {
            fact(operations, "当前状态", target.status.label); fact(operations, "未提交材料", if (target.status == OrderStatus.PROCESSING && target.hasDraft) "有" else "无")
            fact(operations, "当前结果", target.selected?.label ?: "尚未选择")
            ResultKind.values().forEach { kind ->
                val branch = target.branch(kind)
                val upload = UploadStatus.values().mapNotNull { state -> branch.photos.count { it.status == state }.takeIf { it > 0 }?.let { "${state.label} $it 张" } }.joinToString("，")
                add(operations, text("${kind.label}：${branch.photos.size} 张${if (upload.isBlank()) "" else "\n$upload"}", 14f, StationColors.muted), 8)
            }
        }
        add(operations, button("重置所选工单") {
            val chosen = debugTarget?.let(repo::order) ?: return@button
            confirm("重置所选工单", "将重置“${chosen.scene.title}”\n${chosen.number}\n\n清除该工单的所有处理材料、照片、开始和完成记录，恢复为待处理。其他工单不受影响。", "确认重置") {
                if (runAction { repo.resetOrder(chosen.id, false) }) { app.photos.cleanup(repo); render(true); toast("重置成功") }
            }
        }.also { enable(it, target != null) }, 8)
        add(operations, button("清除未提交材料") {
            val chosen = debugTarget?.let(repo::order) ?: return@button
            confirm("清除未提交材料", "将清除“${chosen.scene.title}”\n${chosen.number}\n\n清除全部结果分支的材料和照片，保留处理中状态、开始时间及原始证据。", "确认清除") {
                if (runAction { repo.resetOrder(chosen.id, true) }) { app.photos.cleanup(repo); render(true); toast("已清除未提交材料") }
            }
        }.also { enable(it, target?.let { value -> value.status == OrderStatus.PROCESSING && value.hasDraft } == true) }, 8)
        add(operations, button("清除本地数据") {
            confirm("清除本地数据", "将取消来单计划和旧通知，清除全部工单、处理记录及照片副本，立即恢复垃圾桶、消火栓、闸机三张待处理工单及默认设置。\n\n系统相册中的原图不受影响。", "确认清除") {
                if (runAction { repo.clearAll(app.arrival) }) {
                    app.arrival.clearNotification(); app.photos.cleanup(repo); app.fast = false
                    sceneMode = SceneMode.DEFAULT; selectedScenes = SceneCatalog.defaults.toMutableList(); minuteText = "1"; debugTarget = null
                    tab = OrderStatus.PENDING; listPosition = 0; go("list"); toast("已清除本地数据")
                }
            }
        })
        val settings = section(content, "操作设置")
        label(settings, "操作速度")
        val speed = row()
        listOf(false to "正常", true to "快速").forEach { (fast, name) ->
            speed.addView(button(if (app.fast == fast) "✓ $name" else name) { app.fast = fast; render(true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(4) })
        }
        add(settings, speed, 8)
        add(settings, text("仅影响之后开始的照片上传与提交；不会改变来单等待时间。重新启动后恢复正常。", 14f, StationColors.muted), 8)
        add(settings, button("恢复默认设置") { app.fast = false; render(true); toast("设置已恢复") }, 8)
        add(settings, text("新工单通知：${if (app.arrival.notificationReady()) "已开启" else "未开启"}\n定时提醒：${if (app.arrival.alarmReady()) "可用" else "需授权"}", 14f, StationColors.muted), 8)
        add(settings, button("通知设置") { openNotificationSettings() }, 8)
        if (Build.VERSION.SDK_INT >= 31) add(settings, button("定时提醒设置") { openSettings(app.arrival.alarmSettings()) }, 8)
        if (!app.arrival.backgroundReady()) {
            add(settings, text("后台运行受到系统限制", 14f, StationColors.error), 8)
            add(settings, button("后台运行设置") { openSettings(app.arrival.applicationSettings()) }, 8)
        }
        add(settings, text("声音、振动和锁屏展示遵从系统通知、静音与勿扰设置。", 14f, StationColors.muted))
        val restart = section(content, "重新开始")
        label(restart, "场景选择")
        SceneMode.values().forEach { mode ->
            add(restart, button((if (sceneMode == mode) "✓ " else "") + mode.label) {
                sceneMode = mode
                selectedScenes = when (mode) { SceneMode.SINGLE -> mutableListOf(); else -> SceneCatalog.defaults.toMutableList() }
                render(true)
            }, 8)
        }
        if (sceneMode == SceneMode.MULTIPLE) {
            val actions = row()
            actions.addView(button("全选") { selectedScenes = SceneCatalog.all.map { it.id }.toMutableList(); render(true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
            actions.addView(button("清空选择") { selectedScenes.clear(); render(true) }, LinearLayout.LayoutParams(0, -2, 1f))
            add(restart, actions, 12)
        }
        listOf("A01", "A02").forEach { device ->
            label(restart, "$device 检查点", 8)
            SceneCatalog.all.filter { it.device == device }.forEach { scene ->
                val checked = scene.id in selectedScenes
                val option = button("${if (checked) "✓" else "○"} ${scene.title}\n${scene.device} · ${scene.check}") {
                    if (sceneMode == SceneMode.SINGLE) selectedScenes = mutableListOf(scene.id)
                    else if (sceneMode == SceneMode.MULTIPLE) { if (checked) selectedScenes.remove(scene.id) else selectedScenes.add(scene.id) }
                    render(true)
                }.apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL; if (checked) setTextColor(StationColors.mint) }
                enable(option, sceneMode != SceneMode.DEFAULT); add(restart, option, 8)
            }
        }
        add(restart, button("恢复默认三项") { sceneMode = SceneMode.DEFAULT; selectedScenes = SceneCatalog.defaults.toMutableList(); render(true) }, 12)
        label(restart, "来单等待时间（分钟）")
        val presets = row()
        listOf("1", "2").forEach { value -> presets.addView(button("$value 分钟") { minuteText = value; render(true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) }) }
        add(restart, presets, 8)
        val minutes = EditText(this).apply {
            tag = "waiting-minutes"; textSize = 16f; setTextColor(StationColors.ink); setHintTextColor(StationColors.muted); hint = "输入正整数分钟"
            minHeight = dp(52); setPadding(dp(12), dp(12), dp(12), dp(12)); background = rounded(Color.parseColor("#0C1E32"), 10, StationColors.line)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(minuteText)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { minuteText = s.toString(); updateRestartSummary() }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        add(restart, minutes, 12)
        debugSummary = text("", 14f, StationColors.muted); add(restart, debugSummary!!, 12)
        restartButton = button("重新开始", true) { confirmRestart() }; add(restart, restartButton!!)
        updateRestartSummary()
    }
    private fun chooseTarget() {
        val orders = repo.state.orders
        if (orders.isEmpty()) { toast("当前没有可选择的工单"); return }
        val content = column().apply { setPadding(dp(16), 0, dp(16), dp(12)) }
        val dialog = AlertDialog.Builder(this).setTitle("选择作用工单").setView(ScrollView(this).apply { addView(content) }).setNegativeButton("取消", null).create()
        orders.forEach { order -> add(content, button("${order.scene.title}\n${order.number}") { debugTarget = order.id; dialog.dismiss(); render(true) }, 8) }
        dialog.show()
    }
    private fun updateRestartSummary() {
        val now = System.currentTimeMillis()
        val minutes = StationRules.waitingMinutes(minuteText, now)
        val error = StationRules.selectionError(sceneMode, selectedScenes) ?: if (minutes == null) "请输入有效的正整数分钟" else null
        debugSummary?.text = "已选择 ${selectedScenes.size} 项，将生成 ${selectedScenes.size} 张工单\n\n" +
            selectedScenes.joinToString("\n") { SceneCatalog.get(it).title } +
            if (error != null) "\n\n$error" else "\n\n预计到达：${time(now + requireNotNull(minutes) * 60_000, true)}\n以确认后成功安排的时间为准。"
        restartButton?.let { enable(it, error == null && !app.importing) }
    }
    private fun confirmRestart() {
        val scenes = selectedScenes.toList(); val mode = sceneMode; val minutes = minuteText
        val count = StationRules.waitingMinutes(minutes, System.currentTimeMillis()) ?: return
        if (StationRules.selectionError(mode, scenes) != null) return
        confirm("重新开始", "将清除当前全部工单、处理记录和未提交材料，并返回桌面。$count 分钟后将收到 ${scenes.size} 张新工单。\n\n" + scenes.joinToString("\n") { SceneCatalog.get(it).title }, "确认并返回桌面") {
            if (!app.arrival.notificationReady()) {
                confirm("请开启新工单通知", "开启应用通知及“新工单提醒”类别后，再重新开始。当前工单和计划已保留。", "去设置") { openNotificationSettings() }
                return@confirm
            }
            if (!app.arrival.alarmReady()) {
                confirm("请允许定时提醒", "需要系统允许准时安排来单提醒。授权后请再次重新开始，当前工单和计划已保留。", "去设置") { openSettings(app.arrival.alarmSettings()) }
                return@confirm
            }
            if (!app.arrival.backgroundReady()) {
                confirm("请允许后台运行", "当前后台运行受到系统限制。请在应用的电池设置中允许后台运行，再重新开始。当前工单和计划已保留。", "去设置") { openSettings(app.arrival.applicationSettings()) }
                return@confirm
            }
            if (runAction("未能重新开始，请重试") { repo.restart(mode, scenes, minutes, app.arrival) }) {
                app.arrival.clearNotification(); app.photos.cleanup(repo)
                sceneMode = SceneMode.DEFAULT; selectedScenes = SceneCatalog.defaults.toMutableList(); minuteText = "1"
                hideKeyboard()
                runCatching { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { moveTaskToBack(true) }
                finishAndRemoveTask()
            }
        }
    }
    private fun confirm(title: String, message: String, positive: String, action: () -> Unit) {
        val content = text(message, 16f).apply { setPadding(dp(20), dp(10), dp(20), dp(16)) }
        AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply { addView(content) })
            .setNegativeButton("取消", null).setPositiveButton(positive) { _, _ -> action() }.show()
    }
    private fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 201)
        else openSettings(app.arrival.notificationSettings())
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 201) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) toast("通知许可已开启")
            else confirm("请开启新工单通知", "可在系统设置中开启应用通知。当前工单、材料和计划已保留。", "去设置") { openSettings(app.arrival.notificationSettings()) }
            if (page == "debug") render(true)
        }
    }
    private fun openSettings(intent: Intent) { runAction("无法打开系统设置，请在手机设置中查看智慧车站权限") { startActivity(intent) } }
    private fun launchMedia(camera: Boolean) {
        if (app.importing || !flushEditor()) return
        val order = current() ?: return
        if (order.submitAt != null) return
        var request: MediaRequest? = null
        try {
            if (camera) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (intent.resolveActivity(packageManager) == null) { toast("相机不可用，请从相册选择照片"); return }
                val name = UUID.randomUUID().toString() + ".jpg"
                val file = File(app.photos.captureDirectory, name)
                check(file.createNewFile())
                request = repo.beginMedia(order.id, name)
                val uri = FileProvider.getUriForFile(this, "$packageName.photos", file)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intent.clipData = ClipData.newRawUri("现场照片", uri)
                startActivityForResult(intent, 101)
            } else {
                request = repo.beginMedia(order.id)
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, 102)
            }
        } catch (_: Exception) {
            request?.let { runAction { repo.finishMedia(it.token) } }; app.photos.cleanup(repo)
            toast(if (camera) "无法打开相机，请检查系统权限或从相册选择" else "无法打开相册，请检查系统权限后重试")
        }
    }
    @Deprecated("系统兼容入口")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode !in listOf(101, 102) || !ready) return
        val request = repo.state.pendingMedia ?: return
        if (resultCode != RESULT_OK) { runAction { repo.finishMedia(request.token) }; app.photos.cleanup(repo); return }
        val uris = mutableListOf<Uri>()
        if (requestCode == 101) request.captureName?.let { uris += Uri.fromFile(File(app.photos.captureDirectory, it)) }
        else {
            data?.clipData?.let { clip -> repeat(clip.itemCount) { uris += clip.getItemAt(it).uri } }
            if (uris.isEmpty()) data?.data?.let { uris += it }
        }
        if (uris.isEmpty()) { toast("未能读取所选照片，请重新选择"); runAction { repo.finishMedia(request.token) }; return }
        app.importing = true; updateForm()
        val fast = app.fast
        app.io.execute {
            val imported = mutableListOf<String>()
            var failed = 0
            uris.distinct().forEach { uri -> try { imported += app.photos.import(uri) } catch (_: Exception) { failed++ } }
            app.main.post {
                imported.forEach { name ->
                    try {
                        if (repo.addPhoto(request, name, if (requestCode == 101) "拍照" else "相册", fast)) app.photos.releaseImport(name) else app.photos.discard(name)
                    } catch (_: Exception) { app.photos.discard(name); failed++ }
                }
                runAction { repo.finishMedia(request.token) }; app.importing = false; app.photos.cleanup(repo)
                if (failed > 0) toast("$failed 张照片读取或保存失败，请重新选择")
                app.listeners.toList().forEach { it() }
            }
        }
    }
    private fun loadImage(view: ImageView, name: String, evidence: Boolean, size: Int) {
        view.tag = name
        app.images.execute {
            val bitmap = runCatching { if (evidence) app.photos.evidence(name, size) else app.photos.decode(app.photos.file(name), size) }.getOrNull()
            app.main.post { if (!isDestroyed && view.tag == name) { view.setImageBitmap(bitmap); if (bitmap == null) view.contentDescription = "照片无法读取，请重新选择" } }
        }
    }
    private fun openPreview(original: Boolean, index: Int) {
        if (!flushEditor()) return
        previewReturn = page; previewOriginal = original; previewIndex = index; go("preview")
    }
    private fun renderPreview() {
        val order = current() ?: return
        scroll = null; body = null
        makeHeader(if (previewOriginal) "原始证据" else "照片预览")
        val photos = if (previewOriginal) emptyList() else order.branch().photos
        if (!previewOriginal && photos.isEmpty()) { page = previewReturn; render(); return }
        previewIndex = if (previewOriginal) 0 else previewIndex.coerceIn(0, photos.lastIndex)
        val photo = if (previewOriginal) null else photos[previewIndex]
        val name = photo?.fileName ?: order.scene.image
        val zoom = ZoomPhotoView(this).apply { contentDescription = "照片，可双指缩放或双击放大" }
        root.addView(zoom, LinearLayout.LayoutParams(-1, 0, 1f))
        val loading = text("正在读取照片…", 14f, StationColors.muted).apply { gravity = Gravity.CENTER }; root.addView(loading)
        app.images.execute {
            val bitmap = runCatching { if (photo == null) app.photos.evidence(name, 1800) else app.photos.decode(app.photos.file(name), 1800) }.getOrNull()
            app.main.post { if (!isDestroyed && page == "preview" && zoom.isAttachedToWindow) { zoom.setPhoto(bitmap); loading.text = if (bitmap == null) "照片无法读取，请返回后重新选择" else ""; loading.visibility = if (bitmap == null) View.VISIBLE else View.GONE } }
        }
        val bottom = column().apply { setPadding(dp(12), dp(10), dp(12), dp(12)) }
        previewStatus = text(previewDescription(order), 14f, StationColors.muted).apply { gravity = Gravity.CENTER }
        add(bottom, previewStatus!!, 10)
        val actions = row()
        actions.addView(iconButton("back", "上一张") { if (previewIndex > 0) { previewIndex--; render() } }.also { enable(it, !previewOriginal && previewIndex > 0) })
        actions.addView(button("放大 / 还原") { zoom.toggleZoom() }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(iconButton("next", "下一张") { if (previewIndex < photos.lastIndex) { previewIndex++; render() } }.also { enable(it, !previewOriginal && previewIndex < photos.lastIndex) })
        add(bottom, actions); root.addView(bottom)
    }
    private fun previewDescription(order: Order): String {
        val photos = if (previewOriginal) emptyList() else order.branch().photos
        val photo = photos.getOrNull(previewIndex)
        return "${previewIndex + 1} / ${if (previewOriginal) 1 else photos.size} · ${photo?.source ?: "巡检机器狗 ${order.scene.device}"}\n${photo?.status?.label ?: "原始检查照片"}"
    }
    private fun flushEditor(): Boolean {
        if (page != "form" || editor == null || !ready) return true
        val order = current() ?: return false
        val kind = order.selected ?: return false
        if (order.submitAt != null) return true
        return runAction("本机保存失败，当前输入尚未保存，请重试") { repo.edit(order.id, kind, editor!!.text.toString(), editorRevision) }
    }
    private fun rememberList(): Boolean {
        listPosition = scroll?.scrollY ?: listPosition
        return runAction { repo.saveList(tab, listPosition) }
    }
    private fun go(next: String) {
        if (!flushEditor()) return
        if (page == "list" && !rememberList()) return
        hideKeyboard(); page = next; titleGate.reset(); render()
    }
    private fun back() {
        if (!flushEditor()) return
        when (page) {
            "list" -> { rememberList(); finish() }
            "detail", "complete", "debug" -> go("list")
            "result" -> go("detail")
            "form" -> go("detail")
            "preview" -> go(previewReturn)
        }
    }
    @Deprecated("系统兼容入口")
    override fun onBackPressed() { if (ready) back() else super.onBackPressed() }
    private fun hideKeyboard() { currentFocus?.let { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0) } }
    private fun onDataChanged() {
        if (page == "form") {
            val order = current()
            if (order?.status == OrderStatus.DONE) { hideKeyboard(); page = "complete"; render(); toast("提交成功") }
            else if (order?.status != OrderStatus.PROCESSING || order.selected == null) { page = "list"; render() }
            else updateForm()
        } else if (page == "preview") {
            current()?.let { order ->
                previewStatus?.text = previewDescription(order)
                if (order.status == OrderStatus.DONE && previewReturn != "complete") { previewReturn = "complete"; toast("提交成功") }
            }
        } else if (page in listOf("list", "detail", "debug")) {
            if (page == "debug" && currentFocus is EditText) { updateRestartSummary(); return }
            if (page == "list") listPosition = scroll?.scrollY ?: listPosition
            render(true)
        }
    }
    private fun runAction(message: String = "本机保存失败，请重试", action: () -> Unit): Boolean = try { action(); true } catch (_: Exception) { toast(message, true); false }
    private fun toast(message: String, throttle: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (throttle && message == lastError && now - lastErrorAt < 4_000) return
        lastError = message; lastErrorAt = now
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun showLoadFailure() {
        ready = false
        val view = column().apply { setBackgroundColor(StationColors.background); setPadding(dp(20), dp(40), dp(20), dp(20)) }
        add(view, text("本机数据无法读取", 20f, bold = true), 20)
        add(view, text("原有数据已保留，请重新打开后重试。", 16f, StationColors.muted), 20)
        add(view, button("重新读取", true) { recreate() }); setContentView(view)
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (page == "list") {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val bounds = Rect(); val hit = titleView?.getGlobalVisibleRect(bounds) == true && bounds.contains(event.rawX.toInt(), event.rawY.toInt())
                    titleTouch = hit; titleCancelled = false; titleDownX = event.rawX; titleDownY = event.rawY; titleDownAt = event.eventTime
                    if (!hit) titleGate.reset()
                }
                MotionEvent.ACTION_MOVE -> if (titleTouch && (kotlin.math.abs(event.rawX - titleDownX) > ViewConfiguration.get(this).scaledTouchSlop || kotlin.math.abs(event.rawY - titleDownY) > ViewConfiguration.get(this).scaledTouchSlop)) { titleGate.reset(); titleCancelled = true }
                MotionEvent.ACTION_UP -> if (titleTouch && event.eventTime - titleDownAt >= ViewConfiguration.getLongPressTimeout()) { titleGate.reset(); titleCancelled = true }
                MotionEvent.ACTION_CANCEL -> { titleGate.reset(); titleCancelled = true }
            }
        } else titleGate.reset()
        return super.dispatchTouchEvent(event)
    }
}
