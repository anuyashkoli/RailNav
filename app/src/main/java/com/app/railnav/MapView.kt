package com.app.railnav

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.app.railnav.data.*
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    path: List<GraphNode>?,
    boundingBox: BoundingBox?,
    onZoomComplete: () -> Unit,
    onMapTap: (GeoPoint) -> Unit,
    allEdges: List<EdgeFeature>,
    allNodes: List<NodeFeature>,
    onMarkerTap: (NodeFeature) -> Unit,
    userGpsLocation: GeoPoint?,
    startNode: NodeFeature?
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle(onMapTap)
    val currentThemeColor = MaterialTheme.colorScheme.primary.toArgb()

    LaunchedEffect(boundingBox) {
        boundingBox?.let {
            mapView.zoomToBoundingBox(it, true, 100)
            delay(500)
            onZoomComplete()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            if (view.overlays.size > 1) view.overlays.subList(1, view.overlays.size).clear()

            allEdges.forEach { edge ->
                val polyline = Polyline().apply {
                    setPoints(edge.geometry.coordinates.map { GeoPoint(it[1], it[0]) })
                    color = 0xFFBDBDBD.toInt()
                    width = 6f
                    outlinePaint.apply { color = 0xFFFFFFFF.toInt(); strokeWidth = 8f }
                }
                view.overlays.add(polyline)
            }

            if (path != null && path.size > 1) {
                val edgeMap = allEdges.associateBy { Pair(it.properties.start_id.toInt(), it.properties.end_id.toInt()) }
                for (i in 0 until path.size - 1) {
                    val edge = edgeMap[Pair(path[i].properties.node_id, path[i+1].properties.node_id)]
                    if (edge != null) {
                        val points = edge.geometry.coordinates.map { GeoPoint(it[1], it[0]) }
                        view.overlays.add(Polyline().apply { setPoints(points); color = 0xFFFFFFFF.toInt(); width = 18f })
                        view.overlays.add(Polyline().apply { setPoints(points); color = 0xFF1976D2.toInt(); width = 12f })
                    }
                }
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(path.first().coordinates[1], path.first().coordinates[0])
                    icon = MapUtils.getCachedMarker(context, "START")
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(path.last().coordinates[1], path.last().coordinates[0])
                    icon = MapUtils.getCachedMarker(context, "END")
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }

            allNodes.forEach { node ->
                val marker = Marker(view).apply {
                    position = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
                    val iconRes = when (node.properties.node_type) {
                        "ENTRY/EXIT" -> R.drawable.outline_transit_enterexit_24
                        "STAIRWAY_TOP", "STAIRWAY_BOT" -> R.drawable.stairway
                        "LIFT_TOP", "LIFT_BOT" -> R.drawable.lift
                        else -> org.osmdroid.library.R.drawable.ic_menu_mylocation
                    }
                    icon = MapUtils.getThemedIcon(context, iconRes, currentThemeColor, 28)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = node.properties.node_name
                    setOnMarkerClickListener { _, _ -> onMarkerTap(node); true }
                }
                view.overlays.add(marker)
            }
            view.invalidate()
        }
    )
}

@Composable
fun rememberMapViewWithLifecycle(onMapTap: (GeoPoint) -> Unit): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = ViewCompat.generateViewId()
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 15.0
            controller.setZoom(19.0)
            controller.setCenter(GeoPoint(19.186, 72.975))
            overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean { p?.let { onMapTap(it) }; return true }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) mapView.onResume() else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return mapView
}