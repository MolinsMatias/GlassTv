package tv.glass.launcher

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var model: HomeModel
    private lateinit var media: MediaRepository
    internal val navigation = NavigationState()
    private lateinit var root: FrameLayout
    private lateinit var home: FrameLayout
    private lateinit var wallpaper: ImageView
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var weather: TextView
    private lateinit var dock: LinearLayout
    private lateinit var controlButton: TextView
    private lateinit var mediaCard: LinearLayout
    private var overlay: View? = null
    private var returnFocus: View? = null
    private var lastAppId: String? = null
    private var tick: Job? = null
    private var reload: Job? = null
    private var active = false
    private var safeMode = false
    private var customBitmap: Bitmap? = null
    private val prefs get() = model.preferences
    private val appearance get() = prefs.appearance.value
    private val textColor get() = if (appearance.dark) Color.WHITE else 0xFF15253C.toInt()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshApps() }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        model = HomeModel(this, scope); media = MediaRepository(this)
        val recovery = getSharedPreferences("recovery", MODE_PRIVATE)
        safeMode = recovery.getInt("crashes", 0) >= 3 && System.currentTimeMillis() - recovery.getLong("last", 0) < 300_000
        if (safeMode) { showRecovery(); return }
        buildHome()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }
        registerReceiver(receiver, filter)
        scope.launch { model.installed.collectLatest { renderDock(); if (navigation.surface == Surface.APPS) showApps() } }
        scope.launch { model.weather.collectLatest { renderWeather(it) } }
        scope.launch { media.revision.collectLatest { renderMedia() } }
    }

    override fun onResume() {
        super.onResume(); active = true
        if (safeMode) return
        refreshApps(); media.start(); model.refreshWeather(); updateClock()
        tick = scope.launch {
            var minute = 0
            while (isActive) {
                updateClock()
                if (minute++ % 15 == 0) model.refreshWeather()
                if (navigation.surface == Surface.CONTROL) updateSystemStatus()
                delay(60_000 - System.currentTimeMillis() % 60_000)
            }
        }
    }
    override fun onPause() { active = false; tick?.cancel(); media.stop(); super.onPause() }
    override fun onDestroy() {
        if (!safeMode) unregisterReceiver(receiver)
        scope.cancel(); customBitmap = null; super.onDestroy()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); if (!safeMode) closeOverlay() }
    private fun refreshApps() {
        reload?.cancel()
        reload = scope.launch { model.refreshApps(); renderDock() }
    }
    private fun frame(width: Int, height: Int, gravity: Int, x: Int = 0, y: Int = 0) = FrameLayout.LayoutParams(
        if (width < 0) width else dp(width), if (height < 0) height else dp(height), gravity
    ).apply { leftMargin = dp(x); rightMargin = dp(x); topMargin = dp(y); bottomMargin = dp(y) }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren = false; clipToPadding = false }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; clipChildren = false; clipToPadding = false }
    private fun text(value: String, size: Float = 14f) = label(value, size, textColor)
    private fun action(value: String, onClick: () -> Unit): TextView = text(value).apply {
        id = View.generateViewId(); gravity = Gravity.CENTER_VERTICAL; minHeight = dp(42)
        setPadding(dp(14), dp(7), dp(14), dp(7)); isFocusable = true; isClickable = true
        background = focusSurface(this@MainActivity, appearance.dark, 14)
        setOnClickListener { onClick() }; focusAnimation(appearance.reduceMotion, 1.025f)
    }
    private fun buildHome() {
        root = FrameLayout(this)
        wallpaper = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        root.addView(wallpaper, frame(-1, -1, Gravity.FILL)); applyBackground()
        root.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x33203961, 0x00000000, 0x77051124))
        }, frame(-1, -1, Gravity.FILL))
        home = FrameLayout(this); root.addView(home, frame(-1, -1, Gravity.FILL))
        val timeBlock = column()
        clock = label("", 62f).apply { typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); includeFontPadding = false }
        date = label("", 17f).apply { alpha = .93f }
        weather = label("", 14f).apply { setPadding(0, dp(14), 0, 0); maxWidth = dp(350); maxLines = 4 }
        timeBlock.addView(clock); timeBlock.addView(date); timeBlock.addView(weather)
        home.addView(timeBlock, frame(-2, -2, Gravity.TOP or Gravity.START, 40, 28))
        controlButton = action("⚙  Centro de control  ﹀") { showControl() }
        controlButton.contentDescription = "Abrir centro de control"
        home.addView(controlButton, frame(210, 42, Gravity.TOP or Gravity.END, 28, 20))
        mediaCard = column().apply { background = glassSurface(this@MainActivity, appearance.dark); setPadding(dp(16), dp(14), dp(16), dp(12)); elevation = dp(6).toFloat(); visibility = View.GONE }
        home.addView(mediaCard, frame(230, -2, Gravity.TOP or Gravity.END, 28, 78))
        val dockSurface = FrameLayout(this).apply {
            background = glassSurface(this@MainActivity, appearance.dark, radius = 28); elevation = dp(14).toFloat(); clipChildren = false
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false; clipToPadding = false; clipChildren = false; isFillViewport = true
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }
        dock = row().apply { gravity = Gravity.CENTER; setPadding(dp(6), dp(4), dp(6), 0) }
        scroll.addView(dock, ViewGroup.LayoutParams(-1, -1))
        dockSurface.addView(scroll, frame(-1, -1, Gravity.FILL))
        val width = (resources.displayMetrics.widthPixels / resources.displayMetrics.density - 96).toInt().coerceAtMost(960)
        home.addView(dockSurface, frame(width, 112, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 28))
        home.addView(label("Mantené OK para editar favoritos  ·  ↑ Centro de control", 11f).apply { alpha = .75f; gravity = Gravity.CENTER }, frame(-1, 22, Gravity.BOTTOM, 0, 2))
        setContentView(root); updateClock(); renderDock(); renderWeather(model.weather.value); renderMedia()
    }
    private fun applyBackground() {
        val custom = File(filesDir, "wallpaper.jpg")
        if (appearance.background == 2 && custom.exists()) {
            customBitmap = BitmapFactory.decodeFile(custom.absolutePath)
            wallpaper.setImageBitmap(customBitmap)
        } else {
            customBitmap = null
            wallpaper.setImageResource(R.drawable.landscape)
        }
        wallpaper.colorFilter = if (appearance.background == 1) android.graphics.PorterDuffColorFilter(0x886077C0.toInt(), android.graphics.PorterDuff.Mode.MULTIPLY) else null
    }
    private fun updateClock() {
        val now = Date()
        clock.text = SimpleDateFormat(if (appearance.twentyFourHour) "HH:mm" else "hh:mm a", Locale("es")).format(now)
        date.text = SimpleDateFormat("EEE, d 'de' MMMM", Locale("es")).format(now).replaceFirstChar { it.titlecase(Locale("es")) }
    }
    private fun renderWeather(state: WeatherState) {
        weather.visibility = if (state == WeatherState.Hidden) View.GONE else View.VISIBLE
        weather.text = when (state) {
            WeatherState.Hidden -> ""
            WeatherState.Loading -> "Consultando el clima…"
            WeatherState.Offline -> "Clima · Sin conexión"
            is WeatherState.Error -> "Clima · ${state.message}"
            is WeatherState.Ready -> "${"%.0f".format(state.temperature)}°C  ·  ${weatherDescription(state.code)}\n${state.place}\nOpen-Meteo · ${state.observed.substringAfter('T')}"
        }
    }
    private fun renderDock() {
        if (!::dock.isInitialized) return
        val focused = currentFocus?.tag as? String ?: lastAppId
        dock.removeAllViews()
        val apps = model.installed.value.associateBy(TvApp::id)
        val favorites = prefs.favorites().orEmpty().mapNotNull { apps[it] }
        favorites.forEach { app -> dock.addView(appTile(app), LinearLayout.LayoutParams(dp(96), dp(90))) }
        val all = tile("Apps", null) { showApps() }.apply { contentDescription = "Todas las aplicaciones"; tag = "all-apps" }
        dock.addView(all, LinearLayout.LayoutParams(dp(96), dp(90)))
        val children = (0 until dock.childCount).map { dock.getChildAt(it) }
        children.forEach { it.nextFocusUpId = controlButton.id }
        controlButton.nextFocusDownId = children.first().id
        if (navigation.surface == Surface.HOME) {
            (children.firstOrNull { it.tag == focused } ?: children.first()).requestFocus()
        }
    }
    private fun appTile(app: TvApp) = tile(app.label, app) {
        lastAppId = app.id
        if (!model.apps.launch(app.id)) { toast("La aplicación ya no está disponible"); refreshApps() }
    }.apply {
        tag = app.id; contentDescription = app.label
        setOnLongClickListener { appOptions(app); true }
        setOnKeyListener { _, key, event ->
            if (key == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) { appOptions(app); true } else false
        }
    }
    private fun tile(title: String, app: TvApp?, click: () -> Unit): LinearLayout = column().apply {
        id = View.generateViewId(); gravity = Gravity.CENTER; isFocusable = true; isClickable = true
        setPadding(dp(7), dp(4), dp(7), dp(4)); background = focusSurface(this@MainActivity, appearance.dark, 19)
        if (app != null) {
            addView(ImageView(this@MainActivity).apply {
                setImageDrawable(model.apps.icon(app)); scaleType = ImageView.ScaleType.FIT_CENTER
                background = glassSurface(this@MainActivity, appearance.dark, radius = 15); clipToOutline = true
                setPadding(dp(4), dp(4), dp(4), dp(4))
            }, LinearLayout.LayoutParams(dp(62), dp(54)))
        } else addView(text("▦", 38f).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(62), dp(54)))
        addView(text(title, 11f).apply { gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(-1, dp(25)))
        focusAnimation(appearance.reduceMotion); setOnClickListener { click() }
    }
    private fun appOptions(app: TvApp) {
        val favorites = prefs.favorites().orEmpty()
        val included = app.id in favorites
        val options = mutableListOf("Abrir", if (included) "Quitar de favoritos" else "Añadir a favoritos")
        if (included) options.addAll(listOf("Mover a la izquierda", "Mover a la derecha"))
        AlertDialog.Builder(this).setTitle(app.label).setItems(options.toTypedArray()) { _, which ->
            when (which) {
                0 -> if (!model.apps.launch(app.id)) toast("No se pudo abrir la aplicación")
                1 -> prefs.saveFavorites(FavoriteOrder.toggle(favorites, app.id))
                2 -> prefs.saveFavorites(FavoriteOrder.move(favorites, app.id, -1))
                3 -> prefs.saveFavorites(FavoriteOrder.move(favorites, app.id, 1))
            }
            renderDock()
            if (navigation.surface == Surface.APPS) showApps()
        }.setNegativeButton("Cerrar", null).show()
    }
    private fun installOverlay(view: View, surface: Surface, params: FrameLayout.LayoutParams) {
        if (overlay == null) returnFocus = currentFocus
        overlay?.let(root::removeView)
        home.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        navigation.open(surface); overlay = view
        view.isFocusable = true; view.isFocusableInTouchMode = true
        view.background = glassSurface(this, appearance.dark, radius = 26); view.elevation = dp(20).toFloat()
        root.addView(view, params)
        view.alpha = if (appearance.reduceMotion) 1f else 0f
        view.animate().alpha(1f).setDuration(if (appearance.reduceMotion) 0 else 160).start()
    }
    private fun closeOverlay() {
        overlay?.let(root::removeView); overlay = null; navigation.back()
        home.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        if (returnFocus?.isAttachedToWindow == true) returnFocus?.requestFocus() else dock.getChildAt(0)?.requestFocus()
    }
    private fun showApps() {
        val content = column().apply { setPadding(dp(24), dp(16), dp(24), dp(16)) }
        val close = action("‹  Todas las aplicaciones · Atrás para cerrar") { closeOverlay() }
        content.addView(close, LinearLayout.LayoutParams(-1, dp(44)))
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; setPadding(0, dp(10), 0, dp(8)) }
        val grid = GridLayout(this).apply { columnCount = 6; clipChildren = false; clipToPadding = false }
        model.installed.value.forEach { app ->
            grid.addView(appTile(app), GridLayout.LayoutParams().apply {
                width = dp(108); height = dp(94); setMargins(dp(4), dp(5), dp(4), dp(5))
            })
        }
        if (model.installed.value.isEmpty()) content.addView(text(model.appsError ?: "No se encontraron aplicaciones de Android TV"))
        scroll.addView(grid); content.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(text("OK abre · Mantené OK para añadir, quitar o reordenar favoritos", 12f))
        val width = minOf(752, (resources.displayMetrics.widthPixels / resources.displayMetrics.density - 48).toInt())
        installOverlay(content, Surface.APPS, frame(width, -1, Gravity.CENTER, 0, 24))
        (grid.getChildAt(0) ?: close).requestFocus()
    }
    private var wifiStatus: TextView? = null
    private var bluetoothStatus: TextView? = null
    private fun showControl() {
        val box = column().apply { setPadding(dp(16), dp(12), dp(16), dp(12)) }
        val close = action("Centro de control    ⌃") { closeOverlay() }
        box.addView(close, LinearLayout.LayoutParams(-1, dp(42)))
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        val entries = column()
        fun entry(value: String, action: () -> Unit): TextView = action(value, action).also {
            entries.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        }
        wifiStatus = entry("Wi-Fi") { systemSettings(Settings.ACTION_WIFI_SETTINGS) }
        bluetoothStatus = entry("Bluetooth") { systemSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
        entry("◐  Apariencia · ${if (appearance.dark) "Oscura" else "Clara"}") {
            changeAppearance(appearance.copy(dark = !appearance.dark))
        }
        entry("▧  Fondo de pantalla") { chooseBackground() }
        entry("◷  Reloj · ${if (appearance.twentyFourHour) "24 horas" else "12 horas"}") { changeAppearance(appearance.copy(twentyFourHour = !appearance.twentyFourHour)) }
        entry("☁  Ciudad del clima") { chooseCity() }
        entry("↻  Actualizar clima") { model.refreshWeather(); toast("Actualizando clima") }
        val audio = getSystemService(AudioManager::class.java)
        if (!audio.isVolumeFixed) {
            entries.addView(text("Volumen multimedia", 12f).apply { setPadding(dp(12), dp(12), 0, 0) })
            val volume = SeekBar(this).apply {
                id = View.generateViewId(); max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                progress = audio.getStreamVolume(AudioManager.STREAM_MUSIC); isFocusable = true; minHeight = dp(42)
                contentDescription = "Volumen multimedia, izquierda y derecha para ajustar"
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                        if (fromUser) runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0) }.onFailure { toast("Usá el control de volumen del sistema") }
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) {}
                    override fun onStopTrackingTouch(bar: SeekBar?) {}
                })
            }
            entries.addView(volume, LinearLayout.LayoutParams(-1, dp(42)))
        } else entry("Volumen · control del televisor") { systemSettings(Settings.ACTION_SOUND_SETTINGS) }
        entry("♫  Autorizar controles multimedia") {
            AlertDialog.Builder(this).setTitle("Acceso multimedia opcional")
                .setMessage("Android usa el permiso de acceso a notificaciones para consultar sesiones de otras apps. GlassTV no lee ni almacena notificaciones. Podés revocarlo en Ajustes. Algunas apps no exponen sesiones.")
                .setPositiveButton("Abrir ajustes") { _, _ -> systemSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }.setNegativeButton("Cancelar", null).show()
        }
        entry("⚙  Ajustes del sistema") { systemSettings(Settings.ACTION_SETTINGS) }
        entry("⌂  Seleccionar aplicación de inicio") { systemSettings(Settings.ACTION_HOME_SETTINGS) }
        entry("⌂  Abrir launcher original") { originalHome() }
        entry("◇  Reducir movimiento · ${if (appearance.reduceMotion) "Sí" else "No"}") { changeAppearance(appearance.copy(reduceMotion = !appearance.reduceMotion)) }
        entry("ⓘ  Información del dispositivo") {
            AlertDialog.Builder(this).setTitle("GlassTV ${BuildConfig.VERSION_NAME}")
                .setMessage("${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\nABI: ${Build.SUPPORTED_ABIS.joinToString()}\n\nNo se modifican servicios de Google Cast, DRM, Bluetooth ni actualizaciones.\n\nGlassTV Connect: receptor AirPlay todavía no disponible. Consultá CONNECT.md.")
                .setPositiveButton("Cerrar", null).show()
        }
        scroll.addView(entries); box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val available = (resources.displayMetrics.heightPixels / resources.displayMetrics.density - 160).toInt()
        installOverlay(box, Surface.CONTROL, frame(290, available, Gravity.TOP or Gravity.END, 24, 70))
        updateSystemStatus(); close.requestFocus()
    }
    private fun updateSystemStatus() {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val wifi = getSystemService(WifiManager::class.java)
        val status = when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) "Conectado" else "Red sin Internet verificada"
            wifi?.isWifiEnabled == true -> "Activado · sin conexión Wi-Fi"
            else -> "Desactivado"
        }
        wifiStatus?.text = "Wi-Fi · $status"
        val bluetooth = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled }.getOrNull()
        bluetoothStatus?.text = "Bluetooth · ${when (bluetooth) { true -> "Activado"; false -> "Desactivado"; null -> "Consultar en Ajustes" }}"
    }
    private fun changeAppearance(next: Appearance) {
        closeOverlay(); prefs.update(next); buildHome(); showControl()
    }
    private fun chooseBackground() {
        AlertDialog.Builder(this).setTitle("Fondo de pantalla")
            .setItems(arrayOf("Paisaje panorámico", "Paisaje nocturno", "Elegir imagen del dispositivo")) { _, index ->
                if (index == 2) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
                    runCatching { startActivityForResult(intent, 101) }.onFailure { toast("No hay selector de archivos. Instalá uno compatible o usá los fondos incluidos") }
                } else changeAppearance(appearance.copy(background = index))
            }.setNegativeButton("Cancelar", null).show()
    }
    @Deprecated("Android TV 9 activity result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 101 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                    require(options.outWidth > 0 && options.outHeight > 0)
                    var sample = 1
                    while (options.outWidth / sample > 2048 || options.outHeight / sample > 2048) sample *= 2
                    options.inJustDecodeBounds = false; options.inSampleSize = sample
                    val bitmap = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: error("Imagen no válida")
                    val target = File(filesDir, "wallpaper.tmp")
                    try { target.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) }; check(target.renameTo(File(filesDir, "wallpaper.jpg"))) }
                    finally { bitmap.recycle(); target.delete() }
                }.isSuccess
            }
            if (imported) changeAppearance(appearance.copy(background = 2)) else toast("No se pudo cargar esa imagen")
        }
    }
    private fun chooseCity() {
        val input = EditText(this).apply { setSingleLine(); setText(appearance.city); hint = "Ej.: Córdoba, Argentina"; maxLines = 1; filters = arrayOf(android.text.InputFilter.LengthFilter(100)) }
        AlertDialog.Builder(this).setTitle("Ciudad · clima opcional")
            .setMessage("Open-Meteo buscará la ciudad indicada. Revisá el lugar mostrado; si no coincide, usá un nombre más específico. El campo vacío oculta el clima. No se solicita ubicación GPS.")
            .setView(input).setPositiveButton("Guardar") { _, _ -> prefs.update(appearance.copy(city = input.text.toString().trim())); model.refreshWeather() }
            .setNegativeButton("Cancelar", null).show()
    }
    private fun systemSettings(action: String) {
        val intent = Intent(action)
        runCatching { startActivity(intent) }.onFailure {
            runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }.onFailure { toast("El firmware no ofrece este panel de ajustes") }
        }
    }
    private fun originalHome() {
        val homes = model.apps.originalHomes()
        if (homes.isEmpty()) { systemSettings(Settings.ACTION_SETTINGS); return }
        AlertDialog.Builder(this).setTitle("Launcher original")
            .setItems(homes.map { it.label }.toTypedArray()) { _, which ->
                if (!model.apps.launch(homes[which].id)) toast("No se pudo abrir el launcher original")
            }.setNegativeButton("Cancelar", null).show()
    }
    private fun renderMedia() {
        if (!::mediaCard.isInitialized) return
        mediaCard.removeAllViews()
        val session = media.session.value
        val metadata = session?.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        mediaCard.visibility = if (session == null || title.isNullOrBlank()) View.GONE else View.VISIBLE
        if (session == null || title.isNullOrBlank()) return
        val info = row()
        val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (art != null) info.addView(ImageView(this).apply { setImageBitmap(art); scaleType = ImageView.ScaleType.CENTER_CROP; background = glassSurface(this@MainActivity); clipToOutline = true }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(10) })
        val titles = column()
        titles.addView(text(title, 13f).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END })
        titles.addView(text(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(), 11f).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; alpha = .75f })
        val playing = session.playbackState?.state == PlaybackState.STATE_PLAYING
        titles.addView(text(if (playing) "Reproduciendo" else "En pausa", 10f).apply { alpha = .7f })
        info.addView(titles, LinearLayout.LayoutParams(0, -2, 1f)); mediaCard.addView(info)
        val controls = row().apply { gravity = Gravity.CENTER }
        val actions = session.playbackState?.actions ?: 0
        fun transport(label: String, enabled: Boolean, block: () -> Unit) {
            if (enabled) controls.addView(action(label) { runCatching(block).onFailure { toast("La sesión ya no está disponible") } }, LinearLayout.LayoutParams(0, dp(40), 1f))
        }
        transport("|‹", actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) { session.transportControls.skipToPrevious() }
        val playAction = if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
        transport(if (playing) "Ⅱ" else "▶", actions and (playAction or PlaybackState.ACTION_PLAY_PAUSE) != 0L) { if (playing) session.transportControls.pause() else session.transportControls.play() }
        transport("›|", actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) { session.transportControls.skipToNext() }
        mediaCard.addView(controls)
    }
    private fun showRecovery() {
        val box = column().apply { setPadding(dp(48), dp(36), dp(48), dp(36)); setBackgroundColor(0xFF13243E.toInt()) }
        box.addView(label("GlassTV · inicio seguro", 30f))
        box.addView(label("Se detectaron varios cierres inesperados. Tus favoritos están guardados.", 16f))
        val original = action("Abrir launcher original") { originalHome() }; box.addView(original)
        box.addView(action("Abrir ajustes del sistema") { systemSettings(Settings.ACTION_SETTINGS) })
        box.addView(action("Reintentar GlassTV") { getSharedPreferences("recovery", MODE_PRIVATE).edit().clear().apply(); recreate() })
        setContentView(box); original.requestFocus()
    }
    @Deprecated("Android TV 9 Back behavior")
    override fun onBackPressed() {
        if (safeMode) return
        if (navigation.surface != Surface.HOME) closeOverlay() else dock.getChildAt(0)?.requestFocus()
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!safeMode && keyCode == KeyEvent.KEYCODE_MENU && navigation.surface == Surface.HOME && currentFocus?.tag !is String) { showControl(); return true }
        return super.onKeyDown(keyCode, event)
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
