package com.example.app1.sensors

import android.Manifest
import android.hardware.Sensor
import android.os.Build

enum class SensorCategory(val label: String) {
  ALL("All"),
  MOTION("Motion"),
  POSITION("Position"),
  ENVIRONMENT("Environment"),
  OTHER("Other"),
}

enum class SensorKind(val label: String) {
  HARDWARE("Hardware"),
  SOFTWARE("Software"),
  VENDOR("Vendor"),
  CUSTOM("Custom"),
}

data class StandardSensorInfo(
  val type: Int,
  val typeLabel: String,
  val category: SensorCategory,
  val kind: SensorKind,
  val unit: String,
  val axisLabels: List<String>,
  val description: String,
  val commonUses: String,
)

data class SensorSpec(
  val index: Int,
  val name: String,
  val vendor: String,
  val type: Int,
  val typeLabel: String,
  val category: SensorCategory,
  val kind: SensorKind,
  val unit: String,
  val axisLabels: List<String>,
  val description: String,
  val commonUses: String,
  val version: Int,
  val powerMa: Float,
  val maxRange: Float,
  val resolution: Float,
  val minDelayUs: Int,
  val maxDelayUs: Int,
  val reportingMode: Int,
  val isWakeUp: Boolean,
  val isDynamic: Boolean,
) {
  val isOneShot: Boolean
    get() = reportingMode == Sensor.REPORTING_MODE_ONE_SHOT

  val isVendorPrivate: Boolean
    get() = type >= Sensor.TYPE_DEVICE_PRIVATE_BASE

  val requiredRuntimePermission: String?
    get() =
      when (type) {
        Sensor.TYPE_STEP_DETECTOR,
        Sensor.TYPE_STEP_COUNTER ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACTIVITY_RECOGNITION
          } else {
            null
          }
        Sensor.TYPE_HEART_RATE,
        Sensor.TYPE_HEART_BEAT -> Manifest.permission.BODY_SENSORS
        else -> null
      }

  val reportingModeLabel: String
    get() =
      when (reportingMode) {
        Sensor.REPORTING_MODE_CONTINUOUS -> "Continuous"
        Sensor.REPORTING_MODE_ON_CHANGE -> "On-change"
        Sensor.REPORTING_MODE_ONE_SHOT -> "One-shot"
        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Special"
        else -> "Unknown"
      }
}

enum class SensorCheckStatus(val label: String) {
  UNCHECKED("Unchecked"),
  CHECKING("Checking..."),
  READING("Reading"),
  ON_CHANGE_IDLE("No change yet"),
  ONE_SHOT_ARMED("One-shot armed"),
  NEEDS_PERMISSION("Tap Test to grant permission"),
  BLOCKED("Permission denied by default"),
}

data class SensorCheckResult(
  val status: SensorCheckStatus,
  val preview: String = "",
)

sealed interface SensorStreamState {
  data object Waiting : SensorStreamState

  data class Data(
    val values: List<Float>,
    val accuracy: Int,
    val sampleCount: Long,
    val hz: Float,
  ) : SensorStreamState

  data class Triggered(
    val values: List<Float>,
    val triggerCount: Long,
  ) : SensorStreamState

  data class Unavailable(val reason: String) : SensorStreamState
}

private val XYZ = listOf("X", "Y", "Z")
private val XYZW = listOf("X", "Y", "Z", "W")
private val XYZW_ACC = listOf("X", "Y", "Z", "W", "Est. Acc")

/**
 * Standard Android sensor types based on the official Android SDK (android.hardware.Sensor)
 * and Android Developer Sensors documentation.
 */
@Suppress("DEPRECATION")
val STANDARD_ANDROID_SENSORS: List<StandardSensorInfo> =
  listOf(
    StandardSensorInfo(
      type = Sensor.TYPE_ACCELEROMETER,
      typeLabel = "Accelerometer",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "m/s²",
      axisLabels = XYZ,
      description = "Measures acceleration force (including gravity) along the X, Y, and Z axes.",
      commonUses = "Screen auto-rotation, shake detection, tilt games, step estimation.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
      typeLabel = "Accelerometer (Uncalibrated)",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "m/s²",
      axisLabels = listOf("X", "Y", "Z", "Bias X", "Bias Y", "Bias Z"),
      description = "Raw acceleration without bias compensation, plus estimated axis biases.",
      commonUses = "Custom sensor fusion, calibration diagnostics, robotics/research.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_GYROSCOPE,
      typeLabel = "Gyroscope",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "rad/s",
      axisLabels = XYZ,
      description = "Measures rate of rotation around the device's X, Y, and Z axes.",
      commonUses = "360° camera/panorama, VR/AR head tracking, image stabilization, gaming.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
      typeLabel = "Gyroscope (Uncalibrated)",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "rad/s",
      axisLabels = listOf("X", "Y", "Z", "Drift X", "Drift Y", "Drift Z"),
      description = "Raw rotation rate without drift compensation, plus estimated drift.",
      commonUses = "High-precision IMU fusion, drift analysis, dead-reckoning.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_GRAVITY,
      typeLabel = "Gravity",
      category = SensorCategory.MOTION,
      kind = SensorKind.SOFTWARE,
      unit = "m/s²",
      axisLabels = XYZ,
      description = "Software sensor isolating the direction and magnitude of gravity.",
      commonUses = "Leveler/spirit-level apps, separating device tilt from user motion.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_LINEAR_ACCELERATION,
      typeLabel = "Linear Acceleration",
      category = SensorCategory.MOTION,
      kind = SensorKind.SOFTWARE,
      unit = "m/s²",
      axisLabels = XYZ,
      description = "Acceleration along X, Y, Z with gravity removed.",
      commonUses = "Gesture recognition, impact/sudden-movement detection, motion tracking.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_ROTATION_VECTOR,
      typeLabel = "Rotation Vector",
      category = SensorCategory.MOTION,
      kind = SensorKind.SOFTWARE,
      unit = "",
      axisLabels = XYZW_ACC,
      description = "Fuses accelerometer, gyro, and magnetometer into a 3D orientation quaternion.",
      commonUses = "AR overlays, sky-map apps, 3D compasses, motion-controlled cameras.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_GAME_ROTATION_VECTOR,
      typeLabel = "Game Rotation Vector",
      category = SensorCategory.MOTION,
      kind = SensorKind.SOFTWARE,
      unit = "",
      axisLabels = XYZW,
      description = "3D orientation using accelerometer and gyro only (ignores magnetic north).",
      commonUses = "Motion gaming where magnetic interference shouldn't cause drift.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
      typeLabel = "Geomagnetic Rotation Vector",
      category = SensorCategory.POSITION,
      kind = SensorKind.SOFTWARE,
      unit = "",
      axisLabels = XYZW_ACC,
      description = "Low-power 3D orientation using accelerometer and magnetometer (no gyro).",
      commonUses = "Background compass/orientation tracking with minimal battery drain.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_SIGNIFICANT_MOTION,
      typeLabel = "Significant Motion",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "",
      axisLabels = listOf("Trigger"),
      description = "One-shot trigger that fires when the device detects meaningful user movement.",
      commonUses = "Waking location/fitness tracking when the user starts walking or driving.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_STEP_DETECTOR,
      typeLabel = "Step Detector",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "steps",
      axisLabels = listOf("Step"),
      description = "Emits an event each time the user takes a step (requires Activity Recognition).",
      commonUses = "Live cadence/pace tracking in running and walking apps.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_STEP_COUNTER,
      typeLabel = "Step Counter",
      category = SensorCategory.MOTION,
      kind = SensorKind.HARDWARE,
      unit = "steps",
      axisLabels = listOf("Total Steps"),
      description = "Tracks cumulative steps since last device reboot (requires Activity Recognition).",
      commonUses = "Low-power daily pedometers and fitness dashboards.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_STATIONARY_DETECT,
      typeLabel = "Stationary Detect",
      category = SensorCategory.MOTION,
      kind = SensorKind.SOFTWARE,
      unit = "",
      axisLabels = listOf("State"),
      description = "Detects when the device has been completely stationary for a period of time.",
      commonUses = "Power saving, Doze/sleep optimizations, pausing GPS updates.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_MOTION_DETECT,
      typeLabel = "Motion Detect",
      category = SensorCategory.MOTION,
      kind = SensorKind.SOFTWARE,
      unit = "",
      axisLabels = listOf("State"),
      description = "Detects when the device transitions from stationary into motion.",
      commonUses = "Waking background sensors or anti-theft/movement alerts.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_MAGNETIC_FIELD,
      typeLabel = "Magnetometer",
      category = SensorCategory.POSITION,
      kind = SensorKind.HARDWARE,
      unit = "µT",
      axisLabels = XYZ,
      description = "Measures ambient geomagnetic field strength along X, Y, and Z in microteslas.",
      commonUses = "Digital compass, map orientation, metal/magnet detection.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
      typeLabel = "Magnetometer (Uncalibrated)",
      category = SensorCategory.POSITION,
      kind = SensorKind.HARDWARE,
      unit = "µT",
      axisLabels = listOf("X", "Y", "Z", "Iron X", "Iron Y", "Iron Z"),
      description = "Raw magnetic field without hard-iron calibration, plus estimated iron bias.",
      commonUses = "Compass calibration tools, magnetic anomaly diagnostics.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_PROXIMITY,
      typeLabel = "Proximity",
      category = SensorCategory.POSITION,
      kind = SensorKind.HARDWARE,
      unit = "cm",
      axisLabels = listOf("Distance"),
      description = "Measures distance of an object (like your ear or hand) from the front screen.",
      commonUses = "Turning off the screen during phone calls, pocket-mode protection.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_HINGE_ANGLE,
      typeLabel = "Hinge Angle",
      category = SensorCategory.POSITION,
      kind = SensorKind.HARDWARE,
      unit = "°",
      axisLabels = listOf("Angle"),
      description = "Measures the angle in degrees between two halves of a foldable device.",
      commonUses = "Foldable flex-mode UI layouts, tabletop video/camera modes.",
    ),
    StandardSensorInfo(
      type = 27,
      typeLabel = "Device Orientation",
      category = SensorCategory.POSITION,
      kind = SensorKind.SOFTWARE,
      unit = "°",
      axisLabels = listOf("Orientation"),
      description = "Reports coarse screen orientation (0°, 90°, 180°, 270°).",
      commonUses = "System auto-rotation and camera orientation tagging.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_ORIENTATION,
      typeLabel = "Orientation (Legacy)",
      category = SensorCategory.POSITION,
      kind = SensorKind.SOFTWARE,
      unit = "°",
      axisLabels = listOf("Azimuth", "Pitch", "Roll"),
      description = "Deprecated legacy sensor reporting azimuth, pitch, and roll angles.",
      commonUses = "Legacy apps (modern apps should use Rotation Vector instead).",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_LIGHT,
      typeLabel = "Ambient Light",
      category = SensorCategory.ENVIRONMENT,
      kind = SensorKind.HARDWARE,
      unit = "lx",
      axisLabels = listOf("Illuminance"),
      description = "Measures ambient light level in lux.",
      commonUses = "Adaptive screen brightness, camera exposure assist, lux meters.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_PRESSURE,
      typeLabel = "Barometer / Pressure",
      category = SensorCategory.ENVIRONMENT,
      kind = SensorKind.HARDWARE,
      unit = "hPa",
      axisLabels = listOf("Pressure"),
      description = "Measures ambient atmospheric air pressure in hectopascals (millibars).",
      commonUses = "Elevation/floor-level changes, weather trend apps, GPS altitude assist.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_AMBIENT_TEMPERATURE,
      typeLabel = "Ambient Temperature",
      category = SensorCategory.ENVIRONMENT,
      kind = SensorKind.HARDWARE,
      unit = "°C",
      axisLabels = listOf("Temp"),
      description = "Measures ambient room/air temperature in degrees Celsius.",
      commonUses = "Thermometer apps, weather/environmental monitoring (rare on modern phones).",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_RELATIVE_HUMIDITY,
      typeLabel = "Relative Humidity",
      category = SensorCategory.ENVIRONMENT,
      kind = SensorKind.HARDWARE,
      unit = "%",
      axisLabels = listOf("Humidity"),
      description = "Measures ambient relative humidity as a percentage.",
      commonUses = "Dew-point calculation, indoor comfort & weather apps (rare on phones).",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_HEART_RATE,
      typeLabel = "Heart Rate",
      category = SensorCategory.OTHER,
      kind = SensorKind.HARDWARE,
      unit = "bpm",
      axisLabels = listOf("BPM"),
      description = "Measures user heart rate in beats per minute (requires Body Sensors permission).",
      commonUses = "Fitness, workout, and health monitoring (mostly on Wear OS / select phones).",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_HEART_BEAT,
      typeLabel = "Heart Beat",
      category = SensorCategory.OTHER,
      kind = SensorKind.HARDWARE,
      unit = "",
      axisLabels = listOf("Confidence"),
      description = "Emits an event with confidence score each time a heartbeat peak is detected.",
      commonUses = "Heart-rate variability (HRV) and biometric/wellness apps.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT,
      typeLabel = "Off-Body Detect",
      category = SensorCategory.OTHER,
      kind = SensorKind.HARDWARE,
      unit = "",
      axisLabels = listOf("On-Body"),
      description = "Reports whether a wearable device is currently worn on the body.",
      commonUses = "Auto-locking smartwatches and pausing health sensors when removed.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_POSE_6DOF,
      typeLabel = "Pose 6-DoF",
      category = SensorCategory.POSITION,
      kind = SensorKind.SOFTWARE,
      unit = "",
      axisLabels = listOf("Rot X", "Rot Y", "Rot Z", "Rot W", "Trans X", "Trans Y", "Trans Z"),
      description = "Tracks 6 degrees of freedom (3D rotation + 3D translation) from an arbitrary origin.",
      commonUses = "AR/VR spatial tracking and indoor navigation.",
    ),
    StandardSensorInfo(
      type = Sensor.TYPE_HEADING,
      typeLabel = "Heading",
      category = SensorCategory.POSITION,
      kind = SensorKind.SOFTWARE,
      unit = "°",
      axisLabels = listOf("Heading", "Accuracy"),
      description = "Reports true-north heading in degrees along with heading accuracy.",
      commonUses = "Turn-by-turn walking navigation and map orientation.",
    ),
  )

private val STANDARD_BY_TYPE: Map<Int, StandardSensorInfo> =
  STANDARD_ANDROID_SENSORS.associateBy { it.type }

fun Sensor.toSpec(index: Int): SensorSpec {
  val safeName = name.ifBlank { "Sensor ($type)" }
  val standard = STANDARD_BY_TYPE[type]

  val typeLabel: String
  val category: SensorCategory
  val kind: SensorKind
  val unit: String
  val axisLabels: List<String>
  val description: String
  val commonUses: String

  if (standard != null) {
    typeLabel = standard.typeLabel
    category = standard.category
    kind = standard.kind
    unit = standard.unit
    axisLabels = standard.axisLabels
    description = standard.description
    commonUses = standard.commonUses
  } else {
    val fallbackName =
      stringType
        ?.takeIf { it.isNotBlank() }
        ?.substringAfterLast('.')
        ?.replace('_', ' ')
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: safeName
    typeLabel = fallbackName
    category = SensorCategory.OTHER
    kind = if (type >= Sensor.TYPE_DEVICE_PRIVATE_BASE) SensorKind.VENDOR else SensorKind.CUSTOM
    unit = ""
    axisLabels = emptyList()
    description =
      if (type >= Sensor.TYPE_DEVICE_PRIVATE_BASE) {
        "Manufacturer-specific sensor (Type $type) defined by the device vendor."
      } else {
        "Custom or extended Android sensor (Type $type)."
      }
    commonUses =
      "OEM/system features such as pocket detection, cover state, grip sensing, or power management."
  }

  return SensorSpec(
    index = index,
    name = safeName,
    vendor = vendor.ifBlank { "Unknown" },
    type = type,
    typeLabel = typeLabel,
    category = category,
    kind = kind,
    unit = unit,
    axisLabels = axisLabels,
    description = description,
    commonUses = commonUses,
    version = version,
    powerMa = power,
    maxRange = maximumRange,
    resolution = resolution,
    minDelayUs = minDelay,
    maxDelayUs = maxDelay,
    reportingMode = reportingMode,
    isWakeUp = isWakeUpSensor,
    isDynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isDynamicSensor else false,
  )
}
