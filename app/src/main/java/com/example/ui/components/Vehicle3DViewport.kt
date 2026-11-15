package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import java.io.File
import kotlin.math.roundToInt

@Composable
fun Sketchfab3DModelViewport(
  entryFilePath: String,
  modelTitle: String,
  authorName: String,
  licenseLabel: String,
  autoRotate: Boolean,
  modifier: Modifier = Modifier,
  isOpaque: Boolean = true,
  interactiveCamera: Boolean = true,
  sideProfilePitchRad: Float? = null,
) {
  if (LocalInspectionMode.current) {
    Fallback3DBox(modelTitle, "3D Preview ($licenseLabel)", modifier)
    return
  }

  val fileExists = remember(entryFilePath) { entryFilePath.isNotBlank() && File(entryFilePath).exists() }
  if (!fileExists) {
    Fallback3DBox(modelTitle, "3D file missing locally. Please re-download.", modifier)
    return
  }

  var loadError by remember(entryFilePath) { mutableStateOf<String?>(null) }
  var isLoading by remember(entryFilePath) { mutableStateOf(true) }
  var modelInstance by remember(entryFilePath) { mutableStateOf<ModelInstance?>(null) }

  val engine = rememberEngine()
  val modelLoader = rememberModelLoader(engine)
  val view = rememberView(engine)
  val renderer = rememberRenderer(engine)
  val scene = rememberScene(engine)
  val materialLoader = rememberMaterialLoader(engine)
  val environmentLoader = rememberEnvironmentLoader(engine)
  val collisionSystem = rememberCollisionSystem(view)
  val mainLightNode = rememberMainLightNode(engine) { intensity = 110_000f }
  val cameraNode =
    rememberCameraNode(engine) {
      position =
        if (sideProfilePitchRad != null) Position(0f, 0.35f, 3.2f)
        else Position(0f, 0.55f, 2.75f)
      lookAt(Position(0f, 0f, 0f))
    }
  val cameraManipulator =
    if (interactiveCamera) rememberCameraManipulator(orbitHomePosition = Position(0f, 0.55f, 2.75f))
    else null

  LaunchedEffect(entryFilePath) {
    isLoading = true
    loadError = null
    modelInstance = null
    try {
      val fileUri = File(entryFilePath).toURI().toString()
      val loaded = modelLoader.loadModelInstance(fileLocation = fileUri)
      if (loaded == null) {
        loadError = "Unable to decode 3D mesh for $modelTitle."
      } else {
        modelInstance = loaded
      }
    } catch (t: Throwable) {
      loadError = t.message ?: "3D renderer failed to load mesh."
    } finally {
      isLoading = false
    }
  }

  val childNodes = rememberNodes {}
  var rotationDegY by remember(entryFilePath) { mutableFloatStateOf(if (sideProfilePitchRad != null) 90f else -32f) }

  LaunchedEffect(modelInstance, entryFilePath) {
    childNodes.clear()
    val inst = modelInstance ?: return@LaunchedEffect
    val node =
      ModelNode(
        modelInstance = inst,
        scaleToUnits = if (sideProfilePitchRad != null) 1.55f else 1.85f,
        centerOrigin = Position(0f, 0f, 0f),
        autoAnimate = true,
      )
    node.rotation =
      if (sideProfilePitchRad != null) {
        Rotation(x = 0f, y = 90f, z = Math.toDegrees(sideProfilePitchRad.toDouble()).toFloat())
      } else {
        Rotation(x = 0f, y = rotationDegY, z = 0f)
      }
    childNodes.add(node)
  }

  LaunchedEffect(sideProfilePitchRad) {
    if (sideProfilePitchRad != null) {
      val deg = Math.toDegrees(sideProfilePitchRad.toDouble()).toFloat()
      childNodes.filterIsInstance<ModelNode>().forEach { node ->
        node.rotation = Rotation(x = 0f, y = 90f, z = deg)
      }
    }
  }

  Box(
    modifier =
      modifier.background(
        if (isOpaque) Brush.verticalGradient(listOf(Color(0xFF0E172D), Color(0xFF172646), Color(0xFF0A1020)))
        else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
      )
  ) {
    if (loadError == null) {
      Scene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        view = view,
        renderer = renderer,
        scene = scene,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        collisionSystem = collisionSystem,
        isOpaque = isOpaque,
        mainLightNode = mainLightNode,
        cameraNode = cameraNode,
        cameraManipulator = cameraManipulator,
        childNodes = childNodes,
        onFrame = {
          if (autoRotate && sideProfilePitchRad == null) {
            rotationDegY = (rotationDegY + 0.45f) % 360f
            childNodes.filterIsInstance<ModelNode>().forEach { node ->
              node.rotation = Rotation(x = 0f, y = rotationDegY, z = 0f)
            }
          }
        },
      )
    }

    if (isLoading) {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CircularProgressIndicator(color = RacingOrangeAccent, modifier = Modifier.size(34.dp))
        Text("Loading 3D Mesh...", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
      }
    }

    loadError?.let { msg ->
      Fallback3DBox(modelTitle, msg, Modifier.fillMaxSize())
    }

    if (isOpaque && loadError == null) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkNavyBg.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, ElectricBlueSecondary.copy(alpha = 0.45f)),
        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(Icons.Filled.RotateRight, null, tint = SkyCyan, modifier = Modifier.size(14.dp))
          Text(
            text = "360° Drag & Pinch • By $authorName ($licenseLabel)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
          )
        }
      }
    }
  }
}

@Composable
private fun Fallback3DBox(title: String, reason: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.background(MidnightBluePrimary),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(16.dp),
    ) {
      Icon(Icons.Filled.ViewInAr, null, tint = RacingOrangeAccent, modifier = Modifier.size(36.dp))
      Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
      Text(reason, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
  }
}

fun formatFileSizeMb(bytes: Long): String {
  val mb = bytes / (1024f * 1024f)
  return "${(mb * 10f).roundToInt() / 10f} MB"
}
