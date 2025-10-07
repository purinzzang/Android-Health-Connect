using System;
using TMPro;
using UnityEngine;
using UnityEngine.Rendering;

public class HcReceiver : MonoBehaviour
{
    [Header("UI (선택)")]
    [SerializeField] TextMeshProUGUI stepsText;   // Steps 표시
    [SerializeField] TextMeshProUGUI statusText;  // 상태/에러 표시
    [SerializeField] bool setNameForUnityMessage = true;
    [SerializeField] string unityMessageTargetName = "HcReceiver";

    [Serializable] class HcResult { public bool ok; public long steps; public string error; public string msg; }

    void Awake()
    {
        if (setNameForUnityMessage) gameObject.name = unityMessageTargetName;
        GraphicsSettings.useScriptableRenderPipelineBatching = false;
        Log("READY");
    }

    void Start()
    {
        // 앱 시작 시 Health Connect 상태 확인(설치/업데이트 유도용)
#if UNITY_ANDROID && !UNITY_EDITOR
        Hc.SdkStatus();
#endif
    }

    void OnApplicationQuit()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        Hc.Disconnect(); // 사용 중이라면 정리
#endif
    }

    // -------- Health Connect 콜백 수신 --------
    // HcBridge.kt: UnityPlayer.UnitySendMessage("HcReceiver", "OnHcSteps", json)
    public void OnHcSteps(string json) => OnHcMessage(json);

    // 범용 수신
    public void OnHcMessage(string json)
    {
        HcResult r;
        try { r = JsonUtility.FromJson<HcResult>(json); }
        catch (Exception e) { Log($"PARSE ERR: {e.Message}", true); return; }

        if (r.ok)
        {
            if (r.steps > 0)
            {
                if (stepsText) stepsText.text = $"Steps: {r.steps:N0}";
                Log($"OK {r.steps:N0}");
            }
            else
            {
                Log(string.IsNullOrEmpty(r.msg) ? "OK" : r.msg);
            }
        }
        else
        {
            Log($"ERR: {r.error}", true);
        }
    }

    // -------- 버튼용 메서드 --------
    public void Btn_OpenHealthConnect() { Hc.OpenHealthConnect(); Log("OPEN HC"); }
    public void Btn_RequestPermission() { Hc.RequestPermission(); Log("REQUEST PERMISSION"); }
    public void Btn_ReadTodaySteps() { Hc.ReadTodaySteps(); Log("READ TODAY"); }
    public void Btn_CheckSdkStatus() { Hc.SdkStatus(); Log("CHECK STATUS"); }
    public void Btn_Disconnect() { Hc.Disconnect(); Log("DISCONNECT"); }

    // -------- 공용 로깅 --------
    void Log(string msg, bool warn = false)
    {
        if (statusText) statusText.text = msg;
        if (warn) Debug.LogWarning($"[HcReceiver] {msg}");
        else Debug.Log($"[HcReceiver] {msg}");
    }
}
