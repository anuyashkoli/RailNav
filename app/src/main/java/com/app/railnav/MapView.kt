package com.app.railnav

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleEventObserver
import com.app.railnav.data.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.DashPathEffect
import kotlin.math.pow
import kotlin.math.sqrt

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
    mapLayer: MapLayer = MapLayer.STREET,
    userGpsLocation: GeoPoint?,
    startNode: NodeFeature?
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle(onMapTap)

    // Handle zoom to bounding box
    LaunchedEffect(boundingBox) {
        boundingBox?.let {
            mapView.zoomToBoundingBox(it, true, 100)
            kotlinx.coroutines.delay(500)
            onZoomComplete()
        }
    }

    // Handle map layer changes
    LaunchedEffect(mapLayer) {
        mapView.setTileSource(getMapTileSource(mapLayer))
        mapView.invalidate()
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            // Clear all previous overlays except the first one (MapEventsOverlay)
            if (view.overlays.size > 1) {
                view.overlays.subList(1, view.overlays.size).clear()
            }

            // 1. DRAW BASE MAP EDGES (Railway infrastructure)
            allEdges.forEach { edgeFeature ->
                val polyline = Polyline()
                val geoPoints = edgeFeature.geometry.coordinates.map {
                    GeoPoint(it[1], it[0])
                }
                polyline.setPoints(geoPoints)
                polyline.color = 0xFFBDBDBD.toInt()
                polyline.width = 6f
                polyline.outlinePaint.apply {
                    color = 0xFFFFFFFF.toInt()
                    strokeWidth = 8f
                }
                view.overlays.add(polyline)
            }

            // 2. DRAW THE CALCULATED PATH with enhanced styling
            var snappedUserPosition: GeoPoint? = null
            if (path != null && path.size > 1) {
                val edgeGeometryMap = allEdges.associateBy {
                    Pair(it.properties.start_id.toInt(), it.properties.end_id.toInt())
                }

                // Calculate snapped position if user location exists
                if (userGpsLocation != null && startNode != null) {
                    snappedUserPosition = findNearestPointOnPath(
                        userGpsLocation,
                        path,
                        edgeGeometryMap
                    )
                }

                for (i in 0 until path.size - 1) {
                    val startPathNode = path[i]
                    val endPathNode = path[i + 1]
                    val pathSegmentKey = Pair(
                        startPathNode.properties.node_id,
                        endPathNode.properties.node_id
                    )
                    val edgeFeature = edgeGeometryMap[pathSegmentKey]

                    if (edgeFeature != null) {
                        val geoPoints = edgeFeature.geometry.coordinates.map {
                            GeoPoint(it[1], it[0])
                        }

                        // Outer white outline
                        val outlinePolyline = Polyline()
                        outlinePolyline.setPoints(geoPoints)
                        outlinePolyline.color = 0xFFFFFFFF.toInt()
                        outlinePolyline.width = 18f
                        view.overlays.add(outlinePolyline)

                        // Main blue path
                        val mainPolyline = Polyline()
                        mainPolyline.setPoints(geoPoints)
                        mainPolyline.color = 0xFF1976D2.toInt()
                        mainPolyline.width = 12f
                        view.overlays.add(mainPolyline)

                        // Inner accent line
                        val accentPolyline = Polyline()
                        accentPolyline.setPoints(geoPoints)
                        accentPolyline.color = 0xFF64B5F6.toInt()
                        accentPolyline.width = 4f
                        view.overlays.add(accentPolyline)
                    }
                }

                // Add start marker
                if (path.isNotEmpty()) {
                    val startMarker = Marker(view)
                    val startPathNode = path.first()
                    startMarker.position = GeoPoint(
                        startPathNode.coordinates[1],
                        startPathNode.coordinates[0]
                    )
                    startMarker.icon = MapUtils.createStartMarkerIcon(context)
                    startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    startMarker.title = "Start: ${startPathNode.properties.node_name}"
                    view.overlays.add(startMarker)
                }

                // Add end marker
                if (path.size > 1) {
                    val endMarker = Marker(view)
                    val endPathNode = path.last()
                    endMarker.position = GeoPoint(
                        endPathNode.coordinates[1],
                        endPathNode.coordinates[0]
                    )
                    endMarker.icon = MapUtils.createEndMarkerIcon(context)
                    endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    endMarker.title = "Destination: ${endPathNode.properties.node_name}"
                    view.overlays.add(endMarker)
                }
            }

            // 3. DRAW SNAP LINE from GPS to route (if applicable)
            if (userGpsLocation != null && startNode != null && snappedUserPosition != null) {
                val snapLine = Polyline()
                snapLine.setPoints(listOf(userGpsLocation, snappedUserPosition))
                snapLine.color = 0xFF4CAF50.toInt() // Green color
                snapLine.width = 3f

                // Dashed line effect
                val paint = android.graphics.Paint()
                paint.pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
                paint.color = 0xFF4CAF50.toInt()
                paint.strokeWidth = 3f
                paint.style = android.graphics.Paint.Style.STROKE
                snapLine.outlinePaint.set(paint)

                view.overlays.add(snapLine)
            }

            // 4. DRAW USER'S ACTUAL GPS LOCATION (small circle)
            if (userGpsLocation != null) {
                val gpsMarker = Marker(view)
                gpsMarker.position = userGpsLocation
                gpsMarker.icon = MapUtils.createGpsLocationIcon(context)
                gpsMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                gpsMarker.title = "Your GPS Location"
                view.overlays.add(gpsMarker)
            }

            // 5. DRAW USER'S NAVIGATION ICON (snapped to route)
            if (snappedUserPosition != null && path != null) {
                val navigationMarker = Marker(view)
                navigationMarker.position = snappedUserPosition
                navigationMarker.icon = MapUtils.createNavigationIcon(context)
                navigationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                navigationMarker.title = "You are here (on route)"
                view.overlays.add(navigationMarker)
            }

            // 6. DRAW ALL NODE MARKERS (Facilities)
            allNodes.forEach { nodeFeature ->
                val marker = Marker(view)
                marker.position = GeoPoint(
                    nodeFeature.geometry.coordinates[1],
                    nodeFeature.geometry.coordinates[0]
                )

                val iconId = MapUtils.getIconForNodeType(nodeFeature.properties.node_type)
                marker.icon = MapUtils.createScaledIcon(context, iconId, 28)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker.title = nodeFeature.properties.node_name

                marker.setOnMarkerClickListener { _, _ ->
                    onMarkerTap(nodeFeature)
                    true
                }

                view.overlays.add(marker)
            }

            view.invalidate()
        }
    )
}

/**
 * Finds the nearest point on the calculated path to the user's GPS location
 * This "snaps" the user to the route for indoor navigation
 */
private fun findNearestPointOnPath(
    userLocation: GeoPoint,
    path: List<GraphNode>,
    edgeGeometryMap: Map<Pair<Int, Int>, EdgeFeature>
): GeoPoint? {
    var nearestPoint: GeoPoint? = null
    var minDistance = Double.MAX_VALUE

    for (i in 0 until path.size - 1) {
        val startNode = path[i]
        val endNode = path[i + 1]
        val pathSegmentKey = Pair(startNode.properties.node_id, endNode.properties.node_id)
        val edgeFeature = edgeGeometryMap[pathSegmentKey]

        if (edgeFeature != null) {
            val coordinates = edgeFeature.geometry.coordinates

            // Check each line segment in the edge
            for (j in 0 until coordinates.size - 1) {
                val p1 = GeoPoint(coordinates[j][1], coordinates[j][0])
                val p2 = GeoPoint(coordinates[j + 1][1], coordinates[j + 1][0])

                val closestPoint = getClosestPointOnSegment(userLocation, p1, p2)
                val distance = userLocation.distanceToAsDouble(closestPoint)

                if (distance < minDistance) {
                    minDistance = distance
                    nearestPoint = closestPoint
                }
            }
        }
    }

    return nearestPoint
}

/**
 * Finds the closest point on a line segment to a given point
 */
private fun getClosestPointOnSegment(
    point: GeoPoint,
    lineStart: GeoPoint,
    lineEnd: GeoPoint
): GeoPoint {
    val x = point.latitude
    val y = point.longitude
    val x1 = lineStart.latitude
    val y1 = lineStart.longitude
    val x2 = lineEnd.latitude
    val y2 = lineEnd.longitude

    val dx = x2 - x1
    val dy = y2 - y1

    if (dx == 0.0 && dy == 0.0) {
        return lineStart
    }

    val t = ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)

    return when {
        t < 0 -> lineStart
        t > 1 -> lineEnd
        else -> GeoPoint(x1 + t * dx, y1 + t * dy)
    }
}

@Composable
fun rememberMapViewWithLifecycle(onMapTap: (GeoPoint) -> Unit): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = ViewCompat.generateViewId()
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            isTilesScaledToDpi = true
            setBuiltInZoomControls(false)
            minZoomLevel = 15.0
            maxZoomLevel = 20.0

            controller.setZoom(19.0)
            controller.setCenter(GeoPoint(19.18612894230161, 72.97589331357065))

            val eventReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    p?.let { onMapTap(it) }
                    return true
                }
                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }
            overlays.add(0, MapEventsOverlay(eventReceiver))
        }
    }

    val lifecycleObserver = rememberMapLifecycleObserver(mapView)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

@Composable
fun rememberMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
    }

private fun getMapTileSource(layer: MapLayer): org.osmdroid.tileprovider.tilesource.ITileSource {
    return when (layer) {
        MapLayer.STREET -> TileSourceFactory.MAPNIK
        MapLayer.SATELLITE -> TileSourceFactory.USGS_SAT
        MapLayer.TERRAIN -> TileSourceFactory.OPEN_SEAMAP
        MapLayer.CYCLE -> TileSourceFactory.HIKEBIKEMAP
        MapLayer.TRANSIT -> TileSourceFactory.PUBLIC_TRANSPORT
    }
}