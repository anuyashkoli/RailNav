package com.app.railnav

import android.graphics.Color
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.app.railnav.data.*
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.view.MotionEvent
import org.osmdroid.views.overlay.Overlay

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
    isTrackingModeActive: Boolean,
    onDisableTracking: () -> Unit,
    isDarkTheme: Boolean,
    @Suppress("UNUSED_PARAMETER") startNode: NodeFeature?
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

    // NEW: The "Follow Me" Camera logic
    LaunchedEffect(userGpsLocation, isTrackingModeActive) {
        if (isTrackingModeActive && userGpsLocation != null) {
            // Smoothly pan the camera to the new GPS location
            mapView.controller.animateTo(userGpsLocation)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            // Clear old overlays but keep MapEventsOverlay
            if (view.overlays.size > 1) view.overlays.subList(1, view.overlays.size).clear()

            // =================================================================
            // NEW: DYNAMIC MAP STYLING (DARK MODE)
            // =================================================================
            if (isDarkTheme) {
                // Creates a sleek, dark-blue inverted look (Standard Dark Mode)
                val inverseMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    -1.0f,  0.0f,  0.0f,  0.0f, 255.0f,
                    0.0f, -1.0f,  0.0f,  0.0f, 255.0f,
                    0.0f,  0.0f, -1.0f,  0.0f, 255.0f,
                    0.0f,  0.0f,  0.0f,  1.0f,   0.0f
                ))

                // Optional: Tint it slightly blue so it's not purely harsh black/white
                val tintMatrix = android.graphics.ColorMatrix(floatArrayOf(
                    0.8f, 0.0f, 0.0f, 0.0f, 0.0f, // Red
                    0.0f, 0.9f, 0.0f, 0.0f, 0.0f, // Green
                    0.0f, 0.0f, 1.2f, 0.0f, 0.0f, // Blue
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f  // Alpha
                ))

                inverseMatrix.postConcat(tintMatrix)
                val filter = android.graphics.ColorMatrixColorFilter(inverseMatrix)
                view.overlayManager.tilesOverlay.setColorFilter(filter)
            } else {
                // Light Mode: Remove any filters to show standard map
                view.overlayManager.tilesOverlay.setColorFilter(null)
            }

            val touchOverlay = object : Overlay() {
                override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
                    if (event?.action == MotionEvent.ACTION_DOWN && isTrackingModeActive) {
                        onDisableTracking()
                    }
                    return false // Return false so the user can still actually drag the map
                }
            }
            view.overlays.add(touchOverlay)

            // Create a lookup map for node NAMES to detect Escalators
            val nodeDataMap = allNodes.associate {
                it.properties.node_id to Pair(
                    it.properties.node_name?.uppercase() ?: "",
                    it.properties.node_level ?: 0
                )
            }

            // 1. Draw Edges (with Data-Driven Stair/Escalator Highlighting)
            allEdges.forEach { edge ->
                val startId = edge.properties.start_id.trim().toIntOrNull() ?: return@forEach
                val endId = edge.properties.end_id.trim().toIntOrNull() ?: return@forEach

                val edgeType = edge.properties.edge_type?.uppercase() ?: ""

                val startData = nodeDataMap[startId] ?: Pair("", 0)
                val endData = nodeDataMap[endId] ?: Pair("", 0)

                val startName = startData.first
                val startLevel = startData.second
                val endName = endData.first
                val endLevel = endData.second

                // FIX: Check for Escalators FIRST!
                // Both nodes must have "ESCALATOR" in their name AND be on different floors.
                val isEscalator = edgeType.contains("ESCALATOR") ||
                        (startName.contains("ESCALATOR") && endName.contains("ESCALATOR") && startLevel != endLevel)

                // If it is an escalator, it mathematically cannot be a standard stair,
                // even if the GIS mapper accidentally tagged it as "STAIRWAY" in edge_type.
                val isStair = edgeType.contains("STAIR") && !isEscalator

                val polyline = Polyline().apply {
                    setPoints(edge.geometry.coordinates.map { GeoPoint(it[1], it[0]) })
                    outlinePaint.strokeWidth = 8f

                    // FIX: Reversed Priority. Check Purple before Orange.
                    outlinePaint.color = when {
                        isEscalator -> Color.parseColor("#9C27B0") // Purple for Escalators
                        isStair -> Color.parseColor("#FF0800") // Red for Stairs
                        else -> Color.parseColor("#EED202") // IDE File Text Blue
                    }
                }
                view.overlays.add(polyline)
            }

            // 2. Draw Active Path
            if (path != null && path.size > 1) {
                val edgeMap = allEdges.associateBy {
                    Pair(it.properties.start_id.trim().toIntOrNull() ?: 0, it.properties.end_id.trim().toIntOrNull() ?: 0)
                }

                for (i in 0 until path.size - 1) {
                    val edge = edgeMap[Pair(path[i].properties.node_id, path[i+1].properties.node_id)]
                    if (edge != null) {
                        val points = edge.geometry.coordinates.map { GeoPoint(it[1], it[0]) }

                        view.overlays.add(Polyline().apply {
                            setPoints(points)
                            outlinePaint.color = Color.WHITE
                            outlinePaint.strokeWidth = 18f
                        })
                        view.overlays.add(Polyline().apply {
                            setPoints(points)
                            outlinePaint.color = Color.parseColor("#1976D2")
                            outlinePaint.strokeWidth = 12f
                        })
                    }
                }

                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(path.first().coordinates[1], path.first().coordinates[0])
                    icon = MapUtils.getSystemMarker(context, "START")
                    relatedObject = "SYSTEM_MARKER"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                view.overlays.add(Marker(view).apply {
                    position = GeoPoint(path.last().coordinates[1], path.last().coordinates[0])
                    icon = MapUtils.getSystemMarker(context, "END")
                    relatedObject = "SYSTEM_MARKER"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
            }

            // 3. Draw User GPS Location
            userGpsLocation?.let { gpsLoc ->
                view.overlays.add(Marker(view).apply {
                    position = gpsLoc
                    icon = MapUtils.getSystemMarker(context, "GPS")
                    relatedObject = "SYSTEM_MARKER"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                })
            }

            // 4. Draw Nodes
            val currentZoom = view.zoomLevelDouble
            allNodes.forEach { node ->
                val nodeType = node.properties.node_type?.uppercase() ?: ""
                val nodeName = node.properties.node_name?.uppercase() ?: ""

                val searchStr = "$nodeType $nodeName"
                val isEntryExit = (nodeType == "ENTRY/EXIT")

                // FIX: Never ignore an ENTRY/EXIT node, even if it has "STAIR" in the name!
                // Only ignore internal stairs/escalators and generic OSM facilities.
                if (!isEntryExit && (searchStr.contains("STAIR") || searchStr.contains("ESCALATOR") || nodeType == "FACILITY")) {
                    return@forEach
                }

                val marker = Marker(view).apply {
                    position = GeoPoint(node.geometry.coordinates[1], node.geometry.coordinates[0])
                    relatedObject = searchStr
                    icon = MapUtils.getNodeIcon(context, nodeType, searchStr, currentThemeColor)

                    isEnabled = when {
                        currentZoom < 16.0 -> false
                        isEntryExit -> currentZoom >= 16.0
                        searchStr.contains("LIFT") || searchStr.contains("ELEVATOR") || searchStr.contains("TICKET") -> currentZoom >= 17.5
                        else -> currentZoom >= 19.5
                    }

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
            id = View.generateViewId()
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 13.0
            controller.setZoom(19.0)
            controller.setCenter(GeoPoint(19.186, 72.975))

            overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean { p?.let { onMapTap(it) }; return true }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))

            addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean = false
                override fun onZoom(event: ZoomEvent?): Boolean {
                    val zoom = event?.zoomLevel ?: return false

                    overlays.filterIsInstance<Marker>().forEach { marker ->
                        val typeStr = (marker.relatedObject as? String) ?: return@forEach

                        if (typeStr == "SYSTEM_MARKER") {
                            marker.isEnabled = true
                        } else {
                            val isEntryExit = typeStr.startsWith("ENTRY/EXIT ") || typeStr == "ENTRY/EXIT"

                            marker.isEnabled = when {
                                zoom < 16.0 -> false
                                isEntryExit -> zoom >= 16.0
                                typeStr.contains("LIFT") || typeStr.contains("ELEVATOR") || typeStr.contains("TICKET") -> zoom >= 17.5
                                else -> zoom >= 19.5
                            }
                        }
                    }
                    invalidate()
                    return true
                }
            })
        }
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mapView.onResume()
            else if (event == Lifecycle.Event.ON_PAUSE) mapView.onPause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return mapView
}