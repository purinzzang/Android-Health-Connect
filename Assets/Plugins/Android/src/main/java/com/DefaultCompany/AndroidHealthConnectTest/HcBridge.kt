package com.DefaultCompany.AndroidHealthConnectTest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.unity3d.player.UnityPlayer
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.*

object HcBridge {
    private const val GO = "HcReceiver"
    private const val METHOD = "OnHcSteps"

    @JvmStatic
    fun getSdkStatus(context: Context): Int =
        HealthConnectClient.getSdkStatus(context)

    // 시스템 권한 필요한 MANAGE_HEALTH_PERMISSIONS는 사용 금지.
    // 설정 화면만 안전하게 연다.
    @JvmStatic
    fun openHealthConnect(activity: Activity) {
        try {
            // 공식 Health Connect 설정 화면
            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
            activity.startActivity(intent)
        } catch (_: Throwable) {
            // 폴백: Health Connect 앱의 앱 정보 화면
            val hcPkg = "com.google.android.healthconnect" // Android 14+ 시스템 통합
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$hcPkg")
            )
            try {
                activity.startActivity(intent)
            } catch (_: Throwable) {
                // 마지막 폴백: 일반 설정
                activity.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    @JvmStatic
    fun requestPermission(activity: Activity) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val hc = HealthConnectClient.getOrCreate(activity)
                val perms = setOf(HealthPermission.getReadPermission(StepsRecord::class))

                // 1) 이미 허용?
                val granted = hc.permissionController.getGrantedPermissions()
                if (granted.containsAll(perms)) {
                    UnityPlayer.UnitySendMessage(GO, METHOD, """{"ok":true,"msg":"ALREADY_GRANTED"}""")
                    return@launch
                }

                // 2) 권한 UI 띄우기 (Activity Result Contract로 생성한 인텐트)
                try {
                    val contract = PermissionController.createRequestPermissionResultContract()
                    val intent = contract.createIntent(activity, perms)
                    activity.startActivity(intent)
                    // 결과 콜백은 쓰지 않으니, 사용자가 돌아온 뒤 "Read Today"를 다시 눌러 확인.
                } catch (_: Throwable) {
                    // 3) 일부 기기/에뮬레이터에서 위 인텐트가 막혀있으면 설정 화면으로 폴백
                    openHealthConnect(activity)
                }
            } catch (t: Throwable) {
                val msg = t.message?.replace("\"", "\\\"") ?: "UNKNOWN"
                UnityPlayer.UnitySendMessage(GO, METHOD, """{"ok":false,"error":"$msg"}""")
            }
        }
    }

    @JvmStatic
    fun readTodaySteps(activity: Activity) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val hc = HealthConnectClient.getOrCreate(activity)
                val perms = setOf(HealthPermission.getReadPermission(StepsRecord::class))
                val granted = hc.permissionController.getGrantedPermissions()
                if (!granted.containsAll(perms)) {
                    UnityPlayer.UnitySendMessage(GO, METHOD, """{"ok":false,"error":"NO_PERMISSION"}""")
                    return@launch
                }

                val now = java.time.ZonedDateTime.now()
                val start = now.toLocalDate().atStartOfDay(now.zone).toInstant()
                val end = java.time.Instant.now()

                val result = hc.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                val steps = result[StepsRecord.COUNT_TOTAL] ?: 0L
                UnityPlayer.UnitySendMessage(GO, METHOD, """{"ok":true,"steps":$steps}""")
            } catch (t: Throwable) {
                val msg = t.message?.replace("\"", "\\\"") ?: "UNKNOWN"
                UnityPlayer.UnitySendMessage(GO, METHOD, """{"ok":false,"error":"$msg"}""")
            }
        }
    }

    // Android 13 이하 기기만 사용 (Play 설치 경로)
    @JvmStatic
    fun redirectToInstall(activity: Activity) {
        val pkg = "com.google.android.apps.healthdata"
        val uri = Uri.parse("market://details?id=$pkg&url=healthconnect%3A%2F%2Fonboarding")
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
