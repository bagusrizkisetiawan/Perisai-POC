package id.co.alphanusa.perisaipoc.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.hilt.navigation.compose.hiltViewModel
import id.co.alphanusa.perisaipoc.R
import id.co.alphanusa.perisaipoc.core.util.Constants
import id.co.alphanusa.perisaipoc.domain.model.MapBounds
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayItem
import id.co.alphanusa.perisaipoc.domain.model.MapOverlayType
import id.co.alphanusa.perisaipoc.ui.viewmodel.DrawViewModel
import id.co.alphanusa.perisaipoc.utils.GoogleSatelliteTile
import kotlinx.coroutines.delay
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

// Berapa lama (ms) tanpa sentuhan sebelum peta otomatis kembali mengikuti device
private val RECENTER_IDLE_MS = Constants.MAP_RECENTER_IDLE_MS

@Composable
fun OsmdroidMapView(
    modifier: Modifier = Modifier,
    deviceLocation: GeoPoint? = null,
    deviceMarkerTitle: String = "Lokasi Saya",
    deviceMarkerIcon: Int? = null,
    initialZoom: Double = Constants.Map.DEFAULT_ZOOM,
    pocYaw: Float? = null,
    followDevice: Boolean = true,
) {
    val context = LocalContext.current

    // ── Setup auth & API ───────────────────────────────────────────────────
    val drawVm: DrawViewModel = hiltViewModel()
    val drawItems by drawVm.items.collectAsState()
    var bounds by remember { mutableStateOf<BoundingBox?>(null) }

    // ── Cache icon per iconId (Map<id, Drawable>) ──────────────────────────
    var iconCache by remember { mutableStateOf<Map<String, Drawable>>(emptyMap()) }

    // Setiap kali drawItems berubah, download iconId yang belum ada di cache
    LaunchedEffect(drawItems) {
        val neededIds = drawItems
            .filter { it.type == MapOverlayType.PIN }
            .mapNotNull { it.iconId }
            .toSet()

        val missing = neededIds - iconCache.keys
        if (missing.isEmpty()) return@LaunchedEffect

        val fetched = mutableMapOf<String, Drawable>()
        missing.forEach { iconId ->
            drawVm.loadSticker(iconId)?.let { bytes ->
                bytes.toDrawable(context)?.let { drawable -> fetched[iconId] = drawable }
            }
        }
        if (fetched.isNotEmpty()) {
            iconCache = iconCache + fetched
        }
    }

    LaunchedEffect(bounds) {
        bounds?.let { b ->
            drawVm.loadOverlay(
                MapBounds(
                    westLongitude = b.lonWest,
                    northLatitude = b.latNorth,
                    eastLongitude = b.lonEast,
                    southLatitude = b.latSouth,
                ),
            )
        }
    }

    val arrowDrawable = remember {
        ContextCompat.getDrawable(context, R.drawable.arrow)
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val initialCenter = remember { GeoPoint(Constants.Map.DEFAULT_LATITUDE, Constants.Map.DEFAULT_LONGITUDE) }

    // Auto-follow: peta mengikuti lokasi device (device selalu di tengah).
    // Saat user menggeser/zoom manual -> follow dimatikan sementara; setelah idle
    // (tanpa sentuhan) selama RECENTER_IDLE_MS -> follow menyala lagi & balik ke device.
    var isFollowing by remember { mutableStateOf(true) }
    var lastInteractionAt by remember { mutableLongStateOf(0L) }

    // Recenter otomatis ke device saat follow aktif & lokasi berubah
    LaunchedEffect(deviceLocation, isFollowing, mapViewRef) {
        if (!followDevice || !isFollowing) return@LaunchedEffect
        val mv = mapViewRef ?: return@LaunchedEffect
        val loc = deviceLocation ?: return@LaunchedEffect

        mv.controller.animateTo(loc)
        // update bounds setelah animasi supaya data ke-fetch untuk area baru
        mv.postDelayed({ bounds = mv.boundingBox }, 600)
    }

    // Setelah user berhenti berinteraksi selama RECENTER_IDLE_MS -> aktifkan follow lagi
    LaunchedEffect(lastInteractionAt) {
        if (lastInteractionAt == 0L || !followDevice) return@LaunchedEffect
        delay(RECENTER_IDLE_MS)
        isFollowing = true
    }

    Card(modifier = modifier, shape = RoundedCornerShape(4.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapViewRef = this
                        setTileSource(GoogleSatelliteTile)
//                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        // Deteksi interaksi user: begitu disentuh/digeser, hentikan follow
                        // sementara. animateTo() programatik tidak memicu ini (bukan touch).
                        setOnTouchListener { _, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                                    isFollowing = false
                                    lastInteractionAt = System.currentTimeMillis()
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    lastInteractionAt = System.currentTimeMillis()
                                }
                            }
                            false
                        }
                        controller.setZoom(initialZoom)
                        controller.setCenter(deviceLocation ?: initialCenter)
                        addMapListener(
                            DelayedMapListener(
                                object : MapListener {
                                    override fun onScroll(e: ScrollEvent?): Boolean {
                                        bounds = boundingBox
                                        return false
                                    }
                                    override fun onZoom(e: ZoomEvent?): Boolean {
                                        bounds = boundingBox
                                        return false
                                    }
                                },
                                Constants.Map.BOUNDS_DEBOUNCE_MS,
                            ),
                        )
                        post { bounds = boundingBox }
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()

                    drawItems.forEach { item ->
                        when (item.type) {
                            MapOverlayType.PIN ->
                                item.toMarker(mapView, iconCache)?.let(mapView.overlays::add)

                            MapOverlayType.LINE ->
                                item.toPolyline(mapView, arrowDrawable)
                                    ?.let(mapView.overlays::addAll)

                            MapOverlayType.AREA -> item.toPolygon()?.let(mapView.overlays::add)
                            MapOverlayType.CIRCLE -> item.toCircle()?.let(mapView.overlays::add)
                            MapOverlayType.UNKNOWN -> Unit
                        }
                    }

                    val deviceMarker = Marker(mapView).apply {
                        position = deviceLocation
                        title = deviceMarkerTitle
                        setAnchor(Marker.ANCHOR_CENTER, 5f / 8f)
                        deviceMarkerIcon?.let { iconRes ->
                            ContextCompat.getDrawable(mapView.context, iconRes)?.let {
                                icon = resizeDrawableByWidth(mapView.context, it, 48)
                            }
                        }
                        pocYaw?.let { yaw ->
                            rotation = (360 - yaw) % 360
                            isFlat = true
                        }
                    }
                    mapView.overlays.add(deviceMarker)

                    mapView.invalidate()
                },
            )
        }
    }
}

// ── Extensions ─────────────────────────────────────────────────────────────

private fun MapOverlayItem.parseColor(default: Int = android.graphics.Color.BLUE): Int =
    runCatching { android.graphics.Color.parseColor(color) }.getOrDefault(default)

/**
 * Pin marker. Icon di-resolve dari [iconCache] berdasarkan field [MapOverlayItem.icon] (UUID).
 * Ukuran pakai field [MapOverlayItem.size] (default 48 px kalau 0).
 */
private fun MapOverlayItem.toMarker(
    mapView: MapView,
    iconCache: Map<String, Drawable>,
): Marker? {
    val p = point ?: return null
    val iconId = this.iconId ?: return null
    val resolvedIcon = iconCache[iconId] ?: return null

    return Marker(mapView).apply {
        position = GeoPoint(p.latitude, p.longitude)
        title = this@toMarker.name ?: ""
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

        if (resolvedIcon != null) {
            val baseSize = size.toInt().takeIf { it > 0 } ?: 64
            val targetWidth = baseSize * 1.5
            icon = resizeDrawableByWidth(mapView.context, resolvedIcon, targetWidth.toInt())
        }
    }
}

private fun MapOverlayItem.toPolyline(
    mapView: MapView,
    arrowDrawable: Drawable?,
): List<Overlay>? {
    val pts = points.takeIf { it.size >= 2 } ?: return null
    val geo = pts.map { GeoPoint(it.latitude, it.longitude) }
    val color = parseColor()
    val stroke = (size.takeIf { it > 0 } ?: 6.0).toFloat()

    val overlays = mutableListOf<Overlay>()

    // 🔹 1. Garis putus-putus
    val polyline = Polyline().apply {
        setPoints(geo)
        outlinePaint.color = color
        outlinePaint.strokeWidth = stroke
        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(24f, 12f), 0f)
        title = this@toPolyline.name ?: ""
    }
    overlays.add(polyline)

    // 🔹 2. Dot di titik AWAL
    val dotSize = (stroke * 4).toInt().coerceAtLeast(20)
    overlays.add(makeDotMarker(mapView, geo.first(), color, dotSize))

    // 🔹 3. Arrow di titik AKHIR
    if (arrowDrawable != null) {
        val tinted = arrowDrawable.constantState?.newDrawable()?.mutate()
            ?: arrowDrawable.mutate()
        DrawableCompat.setTint(tinted, color)

        val arrowSize = (stroke * 6).toInt().coerceAtLeast(32)
        val endBearing = geo[geo.size - 2].bearingTo(geo.last()).toFloat()
        overlays.add(makeArrowMarker(mapView, geo.last(), tinted, endBearing, arrowSize))
    }

    return overlays
}

/** Bikin marker dot (lingkaran solid) di posisi tertentu. */
private fun makeDotMarker(
    mapView: MapView,
    position: GeoPoint,
    color: Int,
    sizePx: Int,
): Marker = Marker(mapView).apply {
    this.position = position
    icon = createDotDrawable(mapView.context, color, sizePx)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    setInfoWindow(null)
}

/** Generate bitmap berbentuk lingkaran solid. */
private fun createDotDrawable(
    context: Context,
    color: Int,
    sizePx: Int,
    withWhiteBorder: Boolean = true,
): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Border putih (opsional, bikin dot lebih kelihatan di atas line)
    if (withWhiteBorder) {
        val borderPaint = Paint().apply {
            this.color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, sizePx / 2f, borderPaint)
    }

    // Lingkaran utama
    val fillPaint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val innerRadius = if (withWhiteBorder) sizePx / 2f - sizePx * 0.15f else sizePx / 2f
    canvas.drawCircle(cx, cy, innerRadius, fillPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun makeArrowMarker(
    mapView: MapView,
    position: GeoPoint,
    drawable: Drawable,
    bearing: Float,
    size: Int,
): Marker = Marker(mapView).apply {
    this.position = position
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    icon = resizeDrawableByWidth(mapView.context, drawable, size)
    rotation = -bearing // osmdroid rotation: counter-clockwise; bearing: clockwise from north
    isFlat = true // ikut rotasi peta
    setInfoWindow(null) // disable popup saat di-tap
}

private fun MapOverlayItem.toPolygon(): Polygon? {
    val pts = points.takeIf { it.size >= 3 } ?: return null
    return Polygon().apply {
        points = pts.map { GeoPoint(it.latitude, it.longitude) }
        val c = parseColor()
        fillPaint.color = (c and 0x00FFFFFF) or 0x40000000
        outlinePaint.color = c
        outlinePaint.strokeWidth = (size.takeIf { it > 0 } ?: 3.0).toFloat()
        title = this@toPolygon.name ?: ""
    }
}

private fun MapOverlayItem.toCircle(): Polygon? {
    val p = point ?: return null
    return Polygon().apply {
        points = Polygon.pointsAsCircle(GeoPoint(p.latitude, p.longitude), radius)
        val c = parseColor()
        fillPaint.color = (c and 0x00FFFFFF) or 0x40000000
        outlinePaint.color = c
        outlinePaint.strokeWidth = (size.takeIf { it > 0 } ?: 3.0).toFloat()
        title = this@toCircle.name ?: ""
    }
}

// ── Decoder: ubah byte ikon menjadi Drawable ──────────────────────────────
private fun ByteArray.toDrawable(context: Context): Drawable? = runCatching {
    BitmapFactory.decodeByteArray(this, 0, size)?.let { BitmapDrawable(context.resources, it) }
}.getOrNull()

/** Menyesuaikan lebar drawable ikon dengan tetap menjaga rasio aslinya. */
fun resizeDrawableByWidth(context: Context, drawable: Drawable, targetWidth: Int): BitmapDrawable {
    val bitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth.takeIf { it > 0 } ?: 1,
            drawable.intrinsicHeight.takeIf { it > 0 } ?: 1,
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bmp).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(it)
        }
        bmp
    }
    val ratio = bitmap.height.toFloat() / bitmap.width.toFloat()
    val targetHeight = (targetWidth * ratio).toInt()
    val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    return BitmapDrawable(context.resources, resized)
}
