package com.example.app1.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidSensorRepository(context: Context) {
  private val appContext = context.applicationContext
  private val sensorManager =
    appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

  fun hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

  fun getAllSensors(): List<Pair<Sensor, SensorSpec>> {
    val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
    return sensors.mapIndexed { index, sensor -> sensor to sensor.toSpec(index) }
  }

  fun observeSensor(
    sensor: Sensor,
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI,
  ): Flow<SensorStreamState> = callbackFlow {
    trySend(SensorStreamState.Waiting)

    if (sensor.reportingMode == Sensor.REPORTING_MODE_ONE_SHOT) {
      var triggerCount = 0L
      val triggerListener =
        object : TriggerEventListener() {
          override fun onTrigger(event: TriggerEvent?) {
            triggerCount++
            val vals = event?.values?.toList().orEmpty()
            trySend(SensorStreamState.Triggered(values = vals, triggerCount = triggerCount))
          }
        }
      val ok = sensorManager.requestTriggerSensor(triggerListener, sensor)
      if (!ok) {
        trySend(
          SensorStreamState.Unavailable(
            "Could not arm one-shot trigger sensor. It may be unsupported or require permissions."
          )
        )
      }
      awaitClose { sensorManager.cancelTriggerSensor(triggerListener, sensor) }
      return@callbackFlow
    }

    var sampleCount = 0L
    var lastAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    var windowStartNs = 0L
    var windowSamples = 0
    var currentHz = 0f

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          if (event == null) return
          sampleCount++
          windowSamples++
          if (windowStartNs == 0L) {
            windowStartNs = event.timestamp
          } else {
            val elapsedNs = event.timestamp - windowStartNs
            if (elapsedNs >= 500_000_000L) {
              currentHz = (windowSamples * 1_000_000_000f) / elapsedNs.toFloat()
              windowStartNs = event.timestamp
              windowSamples = 0
            }
          }
          trySend(
            SensorStreamState.Data(
              values = event.values.toList(),
              accuracy = event.accuracy.takeIf { it != 0 } ?: lastAccuracy,
              sampleCount = sampleCount,
              hz = currentHz,
            )
          )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
          lastAccuracy = accuracy
        }
      }

    val registered =
      try {
        sensorManager.registerListener(listener, sensor, samplingPeriodUs)
      } catch (e: SecurityException) {
        false
      }
    if (!registered) {
      trySend(
        SensorStreamState.Unavailable(
          "Permission denied by default (system/OEM-only sensor), or hardware unavailable."
        )
      )
    }

    awaitClose { sensorManager.unregisterListener(listener) }
  }

  suspend fun checkSensor(
    sensor: Sensor,
    spec: SensorSpec,
    timeoutMs: Long = 450L,
  ): SensorCheckResult {
    val reqPerm = spec.requiredRuntimePermission
    if (reqPerm != null && !hasPermission(reqPerm)) {
      Log.i("SensorLabCheck", "${spec.name} (${spec.type}): NEEDS_PERMISSION")
      return SensorCheckResult(SensorCheckStatus.NEEDS_PERMISSION)
    }

    if (sensor.reportingMode == Sensor.REPORTING_MODE_ONE_SHOT) {
      val listener =
        object : TriggerEventListener() {
          override fun onTrigger(event: TriggerEvent?) {}
        }
      val armed = sensorManager.requestTriggerSensor(listener, sensor)
      if (armed) sensorManager.cancelTriggerSensor(listener, sensor)
      val result =
        if (armed) SensorCheckResult(SensorCheckStatus.ONE_SHOT_ARMED)
        else SensorCheckResult(SensorCheckStatus.BLOCKED)
      Log.i("SensorLabCheck", "${spec.name} (${spec.type}): ${result.status}")
      return result
    }

    var registered = false
    val firstValues =
      withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<FloatArray?> { cont ->
          val listener =
            object : SensorEventListener {
              override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && cont.isActive) {
                  sensorManager.unregisterListener(this)
                  cont.resume(event.values.copyOf())
                }
              }

              override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
          registered =
            try {
              sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            } catch (e: SecurityException) {
              false
            }
          if (!registered) {
            if (cont.isActive) cont.resume(null)
          } else {
            cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
          }
        }
      }

    val result =
      when {
        firstValues != null -> {
          val preview =
            firstValues.take(3).joinToString(", ") { String.format(Locale.US, "%.2f", it) }
          SensorCheckResult(SensorCheckStatus.READING, preview)
        }
        !registered -> SensorCheckResult(SensorCheckStatus.BLOCKED)
        else -> SensorCheckResult(SensorCheckStatus.ON_CHANGE_IDLE)
      }
    Log.i("SensorLabCheck", "${spec.name} (${spec.type}): ${result.status} ${result.preview}")
    return result
  }
}
