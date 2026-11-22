package com.example.app1.ui.main

import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.navigation3.runtime.NavKey
import com.example.app1.R
import com.example.app1.sensors.AndroidSensorRepository
import com.example.app1.sensors.STANDARD_ANDROID_SENSORS
import com.example.app1.sensors.SensorCategory
import com.example.app1.sensors.SensorCheckResult
import com.example.app1.sensors.SensorCheckStatus
import com.example.app1.sensors.SensorKind
import com.example.app1.sensors.SensorSpec
import com.example.app1.sensors.SensorStreamState
import com.example.app1.sensors.StandardSensorInfo
import com.example.app1.ui.theme.App1Theme
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

private enum class SamplingRate(val label: String, val delayUs: Int) {
  NORMAL("Normal", SensorManager.SENSOR_DELAY_NORMAL),
  UI("UI", SensorManager.SENSOR_DELAY_UI),
  GAME("Game", SensorManager.SENSOR_DELAY_GAME),
  FASTEST("Fast", SensorManager.SENSOR_DELAY_FASTEST),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val repository = remember(context) { AndroidSensorRepository(context) }
  val allSensors = remember(repository) { repository.getAllSensors() }

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedCategory by rememberSaveable { mutableStateOf(SensorCategory.ALL) }
  var activeSensorIndex by rememberSaveable { mutableStateOf<Int?>(null) }
  var selectedRate by rememberSaveable { mutableStateOf(SamplingRate.UI) }
  var rearmNonce by remember { mutableIntStateOf(0) }
  var showGuide by rememberSaveable { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()
  val checkResults = remember { mutableStateMapOf<Int, SensorCheckResult>() }
  var isCheckingAll by remember { mutableStateOf(false) }
  var checkProgress by remember { mutableIntStateOf(0) }

  var pendingPermissionIndex by remember { mutableStateOf<Int?>(null) }
  var permissionNonce by remember { mutableIntStateOf(0) }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      permissionNonce++
      val targetIdx = pendingPermissionIndex
      pendingPermissionIndex = null
      if (targetIdx != null) {
        activeSensorIndex = targetIdx
        if (granted && checkResults.containsKey(targetIdx)) {
          val entry = allSensors.getOrNull(targetIdx)
          if (entry != null) {
            coroutineScope.launch {
              checkResults[targetIdx] = repository.checkSensor(entry.first, entry.second)
            }
          }
        }
      }
    }

  val supportedTypes = remember(allSensors) { allSensors.map { it.second.type }.toSet() }

  val filteredSensors =
    remember(allSensors, searchQuery, selectedCategory) {
      val q = searchQuery.trim().lowercase(Locale.US)
      allSensors.filter { (_, spec) ->
        val matchesCategory =
          selectedCategory == SensorCategory.ALL || spec.category == selectedCategory
        val matchesQuery =
          q.isEmpty() ||
            spec.name.lowercase(Locale.US).contains(q) ||
            spec.typeLabel.lowercase(Locale.US).contains(q) ||
            spec.vendor.lowercase(Locale.US).contains(q)
        matchesCategory && matchesQuery
      }
    }

  val filteredGuideSensors =
    remember(searchQuery, selectedCategory) {
      val q = searchQuery.trim().lowercase(Locale.US)
      STANDARD_ANDROID_SENSORS.filter { info ->
        val matchesCategory =
          selectedCategory == SensorCategory.ALL || info.category == selectedCategory
        val matchesQuery =
          q.isEmpty() ||
            info.typeLabel.lowercase(Locale.US).contains(q) ||
            info.description.lowercase(Locale.US).contains(q) ||
            info.commonUses.lowercase(Locale.US).contains(q)
        matchesCategory && matchesQuery
      }
    }

  val activeEntry =
    remember(allSensors, activeSensorIndex) {
      activeSensorIndex?.let { idx -> allSensors.getOrNull(idx) }
    }

  val activeHasPermission =
    remember(activeEntry, permissionNonce) {
      val perm = activeEntry?.second?.requiredRuntimePermission
      perm == null || repository.hasPermission(perm)
    }

  val liveState: SensorStreamState =
    if (activeEntry != null && activeHasPermission) {
      val (sensor, _) = activeEntry
      val flow =
        remember(activeSensorIndex, selectedRate, rearmNonce, permissionNonce) {
          repository.observeSensor(sensor, selectedRate.delayUs)
        }
      val collected by flow.collectAsStateWithLifecycle(initialValue = SensorStreamState.Waiting)
      collected
    } else {
      SensorStreamState.Waiting
    }

  val sampleHistory = remember(activeSensorIndex) { mutableStateListOf<List<Float>>() }
  LaunchedEffect(liveState) {
    if (liveState is SensorStreamState.Data) {
      sampleHistory.add(liveState.values)
      if (sampleHistory.size > 50) {
        sampleHistory.removeAt(0)
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
          Row(
            modifier = Modifier.padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              onClick = { showGuide = !showGuide },
              shape = CircleShape,
              color =
                if (showGuide) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
            ) {
              Text(
                text = if (showGuide) "Device Sensors" else "All Types Guide",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color =
                  if (showGuide) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
            ) {
              Text(
                text =
                  if (showGuide) "${filteredGuideSensors.size}/${STANDARD_ANDROID_SENSORS.size}"
                  else "${filteredSensors.size}/${allSensors.size}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
      )
    },
    bottomBar = {
      BannerAd(modifier = Modifier.fillMaxWidth().navigationBarsPadding())
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CompactSearchBar(
          query = searchQuery,
          onQueryChange = { searchQuery = it },
          modifier = Modifier.weight(1f),
        )
        if (!showGuide) {
          Button(
            onClick = {
              if (!isCheckingAll) {
                activeSensorIndex = null
                isCheckingAll = true
                checkProgress = 0
                checkResults.clear()
                coroutineScope.launch {
                  try {
                    allSensors.forEachIndexed { idx, (sensor, spec) ->
                      checkResults[spec.index] = SensorCheckResult(SensorCheckStatus.CHECKING)
                      checkResults[spec.index] = repository.checkSensor(sensor, spec)
                      checkProgress = idx + 1
                    }
                  } finally {
                    isCheckingAll = false
                  }
                }
              }
            },
            enabled = !isCheckingAll,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(40.dp),
          ) {
            Text(if (isCheckingAll) "$checkProgress/${allSensors.size}" else "Check All")
          }
        }
      }

      if (isCheckingAll && allSensors.isNotEmpty()) {
        LinearProgressIndicator(
          progress = { checkProgress.toFloat() / allSensors.size.toFloat() },
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(3.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
      }

      if (!showGuide && checkResults.isNotEmpty()) {
        val readingCount = checkResults.values.count { it.status == SensorCheckStatus.READING }
        val idleCount = checkResults.values.count { it.status == SensorCheckStatus.ON_CHANGE_IDLE }
        val armedCount = checkResults.values.count { it.status == SensorCheckStatus.ONE_SHOT_ARMED }
        val permCount = checkResults.values.count { it.status == SensorCheckStatus.NEEDS_PERMISSION }
        val blockedCount = checkResults.values.count { it.status == SensorCheckStatus.BLOCKED }
        Text(
          text =
            "Scan: $readingCount reading • $idleCount idle • $armedCount armed • $permCount ask perm • $blockedCount denied",
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(SensorCategory.entries) { category ->
          val count =
            if (showGuide) {
              if (category == SensorCategory.ALL) STANDARD_ANDROID_SENSORS.size
              else STANDARD_ANDROID_SENSORS.count { it.category == category }
            } else {
              if (category == SensorCategory.ALL) allSensors.size
              else allSensors.count { it.second.category == category }
            }
          FilterChip(
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            label = { Text("${category.label} ($count)") },
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (showGuide) {
        if (filteredGuideSensors.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = "No matching sensor types in guide.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(filteredGuideSensors, key = { it.type }) { info ->
              StandardSensorGuideCard(
                info = info,
                isSupportedOnDevice = info.type in supportedTypes,
              )
            }
          }
        }
      } else if (filteredSensors.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = "No matching sensors found on this device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredSensors, key = { (_, spec) -> spec.index }) { (_, spec) ->
            val isTesting = activeSensorIndex == spec.index
            val reqPerm = spec.requiredRuntimePermission
            val missingPerm =
              remember(reqPerm, permissionNonce) {
                reqPerm != null && !repository.hasPermission(reqPerm)
              }
            SensorCard(
              spec = spec,
              isTesting = isTesting,
              missingPermission = missingPerm,
              checkResult = checkResults[spec.index],
              streamState = if (isTesting) liveState else SensorStreamState.Waiting,
              sampleHistory = if (isTesting) sampleHistory else emptyList(),
              selectedRate = selectedRate,
              onSelectRate = { selectedRate = it },
              onToggleTest = {
                if (isTesting) {
                  activeSensorIndex = null
                } else if (missingPerm && reqPerm != null) {
                  pendingPermissionIndex = spec.index
                  permissionLauncher.launch(reqPerm)
                } else {
                  activeSensorIndex = spec.index
                }
              },
              onRequestPermission = {
                if (reqPerm != null) {
                  pendingPermissionIndex = spec.index
                  permissionLauncher.launch(reqPerm)
                }
              },
              onRearm = { rearmNonce++ },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SensorCard(
  spec: SensorSpec,
  isTesting: Boolean,
  missingPermission: Boolean,
  checkResult: SensorCheckResult?,
  streamState: SensorStreamState,
  sampleHistory: List<List<Float>>,
  selectedRate: SamplingRate,
  onSelectRate: (SamplingRate) -> Unit,
  onToggleTest: () -> Unit,
  onRequestPermission: () -> Unit,
  onRearm: () -> Unit,
) {
  val accentColor = categoryAccentColor(spec.category)
  ElevatedCard(
    onClick = onToggleTest,
    modifier = Modifier.fillMaxWidth(),
    colors =
      if (isTesting) {
        CardDefaults.elevatedCardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        )
      } else {
        CardDefaults.elevatedCardColors()
      },
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.width(4.dp)
              .fillMaxHeight()
              .clip(CircleShape)
              .background(accentColor)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = spec.typeLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          val subtitlePrefix =
            if (spec.typeLabel.equals(spec.name, ignoreCase = true)) {
              "${spec.vendor} • Type ${spec.type}"
            } else {
              "${spec.vendor} • ${spec.category.label}"
            }
          Text(
            text = String.format(Locale.US, "%s • %.2f mA", subtitlePrefix, spec.powerMa),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (checkResult != null) {
            val (statusText, statusColor) =
              when (checkResult.status) {
                SensorCheckStatus.CHECKING -> "Checking..." to MaterialTheme.colorScheme.primary
                SensorCheckStatus.READING ->
                  "✓ Reading: ${checkResult.preview}" to Color(0xFF16A34A)
                SensorCheckStatus.ON_CHANGE_IDLE ->
                  "• On-change (idle)" to MaterialTheme.colorScheme.onSurfaceVariant
                SensorCheckStatus.ONE_SHOT_ARMED ->
                  "• One-shot armed" to Color(0xFFD97706)
                SensorCheckStatus.NEEDS_PERMISSION ->
                  "• Tap Test to grant permission" to Color(0xFFD97706)
                SensorCheckStatus.BLOCKED ->
                  "✕ Permission denied by default" to MaterialTheme.colorScheme.error
                SensorCheckStatus.UNCHECKED -> "" to MaterialTheme.colorScheme.onSurfaceVariant
              }
            if (statusText.isNotEmpty()) {
              Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Button(
          onClick = onToggleTest,
          shape = CircleShape,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
          colors =
            if (isTesting) {
              ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
              ButtonDefaults.buttonColors()
            },
        ) {
          Text(if (isTesting) "Stop" else "Test")
        }
      }

      AnimatedVisibility(visible = isTesting) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          HorizontalDivider()

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(text = spec.description, style = MaterialTheme.typography.bodySmall)
              Text(
                text = "Uses: ${spec.commonUses}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text =
                    String.format(
                      Locale.US,
                      "Range: %.4g %s • Res: %.4g",
                      spec.maxRange,
                      spec.unit,
                      spec.resolution,
                    ),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${spec.reportingModeLabel} • ${spec.minDelayUs} µs",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (missingPermission) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Runtime permission required to stream live data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
              )
              Button(onClick = onRequestPermission, shape = CircleShape) {
                Text("Grant")
              }
            }
          } else {
            if (!spec.isOneShot) {
              SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SamplingRate.entries.forEachIndexed { idx, rate ->
                  SegmentedButton(
                    selected = selectedRate == rate,
                    onClick = { onSelectRate(rate) },
                    shape = SegmentedButtonDefaults.itemShape(idx, SamplingRate.entries.size),
                  ) {
                    Text(rate.label, maxLines = 1)
                  }
                }
              }
            }

            LiveReadingSection(
              spec = spec,
              streamState = streamState,
              sampleHistory = sampleHistory,
              onRearm = onRearm,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiveReadingSection(
  spec: SensorSpec,
  streamState: SensorStreamState,
  sampleHistory: List<List<Float>>,
  onRearm: () -> Unit,
) {
  when (streamState) {
    SensorStreamState.Waiting -> {
      val waitingText =
        when {
          spec.isOneShot -> "Armed — move or shake the phone to trigger..."
          spec.reportingMode == Sensor.REPORTING_MODE_ON_CHANGE ->
            if (spec.isVendorPrivate) {
              "Listening (on-change vendor sensor) — only updates on specific system/device state changes."
            } else {
              "Listening (on-change) — waiting for a value change..."
            }
          spec.reportingMode == Sensor.REPORTING_MODE_SPECIAL_TRIGGER ->
            "Listening (event-triggered) — waiting for an event..."
          else -> "Waiting for sensor events..."
        }
      Text(
        text = waitingText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    is SensorStreamState.Unavailable -> {
      Text(
        text = streamState.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }

    is SensorStreamState.Triggered -> {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Triggered! (#${streamState.triggerCount})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          if (streamState.values.isNotEmpty()) {
            Text(
              text = streamState.values.joinToString(", ") { formatValue(it) },
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
        Button(onClick = onRearm) { Text("Re-arm") }
      }
    }

    is SensorStreamState.Data -> {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Accuracy: ${accuracyLabel(streamState.accuracy)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              String.format(
                Locale.US,
                "%d samples  •  %.1f Hz",
                streamState.sampleCount,
                streamState.hz,
              ),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            fontFamily = FontFamily.Monospace,
          )
        }

        if (sampleHistory.size >= 2) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SensorWaveformChart(
              history = sampleHistory,
              maxRange = spec.maxRange,
              modifier = Modifier.weight(1f).height(96.dp),
            )
            if (streamState.values.size >= 2 &&
              (spec.category == SensorCategory.MOTION || spec.category == SensorCategory.POSITION)
            ) {
              TiltBubbleLevel(
                x = streamState.values[0],
                y = streamState.values[1],
                maxRange = spec.maxRange.takeIf { it > 0f } ?: 10f,
                modifier = Modifier.size(96.dp),
              )
            }
          }
        }

        streamState.values.forEachIndexed { idx, value ->
          val axisLabel = spec.axisLabels.getOrElse(idx) { "Val[$idx]" }
          val unitSuffix = if (spec.unit.isNotEmpty()) " ${spec.unit}" else ""
          val maxAbs = spec.maxRange.takeIf { it > 0f } ?: 1f
          val fraction = (abs(value) / maxAbs).coerceIn(0f, 1f)

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = axisLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "${formatValue(value)}$unitSuffix",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                fontFamily = FontFamily.Monospace,
              )
            }
            LinearProgressIndicator(
              progress = { fraction },
              modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StandardSensorGuideCard(
  info: StandardSensorInfo,
  isSupportedOnDevice: Boolean,
) {
  ElevatedCard(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = info.typeLabel,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f).padding(end = 8.dp),
        )
        Surface(
          shape = CircleShape,
          color =
            if (isSupportedOnDevice) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = if (isSupportedOnDevice) "On this phone" else "Not on phone",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isSupportedOnDevice) MaterialTheme.colorScheme.onPrimaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        SpecBadge(info.category.label)
        SpecBadge(info.kind.label)
        if (info.unit.isNotEmpty()) SpecBadge("Unit: ${info.unit}")
        SpecBadge("Type ${info.type}")
      }

      Text(
        text = info.description,
        style = MaterialTheme.typography.bodySmall,
      )
      Text(
        text = "Uses: ${info.commonUses}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CompactSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
  Surface(
    modifier = modifier.height(40.dp),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Canvas(modifier = Modifier.size(16.dp)) {
        val radius = size.minDimension * 0.34f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
          color = iconColor,
          radius = radius,
          center = center,
          style = Stroke(width = 2.dp.toPx()),
        )
        drawLine(
          color = iconColor,
          start = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f),
          end = Offset(size.width * 0.9f, size.height * 0.9f),
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (query.isEmpty()) {
          Text(
            text = "Search sensors...",
            style = MaterialTheme.typography.bodyMedium,
            color = iconColor,
            maxLines = 1,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle =
            MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface
            ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (query.isNotEmpty()) {
        TextButton(
          onClick = { onQueryChange("") },
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
          modifier = Modifier.height(28.dp),
        ) {
          Text("✕", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

private val AXIS_COLORS =
  listOf(
    Color(0xFFEF4444), // X: Coral Red
    Color(0xFF14B8A6), // Y: Teal
    Color(0xFF6366F1), // Z: Indigo
  )

@Composable
private fun SensorWaveformChart(
  history: List<List<Float>>,
  maxRange: Float,
  modifier: Modifier = Modifier,
) {
  val bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  val peak =
    remember(history, maxRange) {
      val dataMax = history.flatten().maxOfOrNull { abs(it) } ?: 1f
      maxOf(dataMax, maxRange.takeIf { it > 0f }?.times(0.25f) ?: 1f, 1f)
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
      val midY = size.height / 2f
      drawLine(
        color = gridColor,
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
        strokeWidth = 1.dp.toPx(),
      )

      val axisCount = (history.maxOfOrNull { it.size } ?: 0).coerceAtMost(3)
      for (axis in 0 until axisCount) {
        val path = Path()
        history.forEachIndexed { idx, sample ->
          val v = sample.getOrElse(axis) { 0f }
          val x =
            if (history.size <= 1) 0f
            else (idx.toFloat() / (history.size - 1)) * size.width
          val norm = (v / peak).coerceIn(-1f, 1f)
          val y = midY - norm * (size.height * 0.44f)
          if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
          path = path,
          color = AXIS_COLORS[axis],
          style =
            Stroke(
              width = 2.dp.toPx(),
              cap = StrokeCap.Round,
              join = StrokeJoin.Round,
            ),
        )
      }
    }
  }
}

@Composable
private fun TiltBubbleLevel(
  x: Float,
  y: Float,
  maxRange: Float,
  modifier: Modifier = Modifier,
) {
  val bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  val ringColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
  val dotColor = MaterialTheme.colorScheme.primary

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = size.minDimension * 0.42f
      drawCircle(
        color = ringColor,
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx()),
      )
      drawLine(
        color = ringColor,
        start = Offset(center.x - 10.dp.toPx(), center.y),
        end = Offset(center.x + 10.dp.toPx(), center.y),
        strokeWidth = 1.dp.toPx(),
      )
      drawLine(
        color = ringColor,
        start = Offset(center.x, center.y - 10.dp.toPx()),
        end = Offset(center.x, center.y + 10.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
      )

      val scale = maxRange.coerceAtLeast(1f) * 0.5f
      val dx = (-x / scale).coerceIn(-1f, 1f) * (radius * 0.8f)
      val dy = (y / scale).coerceIn(-1f, 1f) * (radius * 0.8f)
      drawCircle(
        color = dotColor,
        radius = 7.dp.toPx(),
        center = Offset(center.x + dx, center.y + dy),
      )
    }
  }
}

private fun categoryAccentColor(category: SensorCategory): Color =
  when (category) {
    SensorCategory.MOTION -> Color(0xFF5C6BC0)
    SensorCategory.POSITION -> Color(0xFF009688)
    SensorCategory.ENVIRONMENT -> Color(0xFFF59E0B)
    SensorCategory.OTHER, SensorCategory.ALL -> Color(0xFFE11D48)
  }

@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier,
    factory = { context ->
      AdView(context).apply {
        setAdSize(AdSize.BANNER)
        // Google's official test Banner Ad Unit ID
        adUnitId = "ca-app-pub-3940256099942544/6300978111"
        loadAd(AdRequest.Builder().build())
      }
    },
    onRelease = { adView -> adView.destroy() },
  )
}

@Composable
private fun SpecBadge(text: String) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun formatValue(value: Float): String = String.format(Locale.US, "%.4f", value)

private fun accuracyLabel(accuracy: Int): String =
  when (accuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
    SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
    SensorManager.SENSOR_STATUS_NO_CONTACT -> "No Contact"
    else -> "Unknown"
  }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SensorCardPreview() {
  App1Theme {
    SensorCard(
      spec =
        SensorSpec(
          index = 0,
          name = "LSM6DSO Accelerometer",
          vendor = "STMicroelectronics",
          type = Sensor.TYPE_ACCELEROMETER,
          typeLabel = "Accelerometer",
          category = SensorCategory.MOTION,
          kind = SensorKind.HARDWARE,
          unit = "m/s²",
          axisLabels = listOf("X", "Y", "Z"),
          description = "Measures acceleration force (including gravity) along X, Y, and Z.",
          commonUses = "Auto-rotation, shake detection, tilt games.",
          version = 1,
          powerMa = 0.25f,
          maxRange = 78.45f,
          resolution = 0.0024f,
          minDelayUs = 2000,
          maxDelayUs = 1000000,
          reportingMode = Sensor.REPORTING_MODE_CONTINUOUS,
          isWakeUp = false,
          isDynamic = false,
        ),
      isTesting = true,
      missingPermission = false,
      checkResult = SensorCheckResult(SensorCheckStatus.READING, "0.12, 9.78, 0.34"),
      streamState =
        SensorStreamState.Data(
          values = listOf(0.12f, 9.78f, 0.34f),
          accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
          sampleCount = 42,
          hz = 15.2f,
        ),
      sampleHistory = listOf(listOf(0.1f, 9.7f, 0.3f), listOf(0.12f, 9.78f, 0.34f)),
      selectedRate = SamplingRate.UI,
      onSelectRate = {},
      onToggleTest = {},
      onRequestPermission = {},
      onRearm = {},
    )
  }
}
