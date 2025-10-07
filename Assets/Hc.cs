using UnityEngine;

public static class Hc
{
#if UNITY_ANDROID && !UNITY_EDITOR
    static AndroidJavaObject sActivity;
    static AndroidJavaClass sBridge;

    // Kotlin 패키지/클래스와 반드시 동일하게 
    const string BridgeClass = "com.DefaultCompany.AndroidHealthConnectTest.HcBridge";

    static bool TryInit()
    {
        if (sBridge != null) return true;
        try
        {
            var up = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            sActivity = up.GetStatic<AndroidJavaObject>("currentActivity");
            sBridge = new AndroidJavaClass(BridgeClass);
            return true;
        }
        catch (System.Exception e)
        {
            Debug.LogWarning($"[HC] Bridge init failed: {e}");
            return false;
        }
    }

    static void Call(string method)
    {
        if (!TryInit()) { Debug.LogWarning($"[HC] Bridge not ready. Skip call: {method}"); return; }
        // UI 스레드에서 호출
        sActivity.Call("runOnUiThread", new AndroidJavaRunnable(() =>
        {
            sBridge.CallStatic(method, sActivity);
        }));
    }

    public static int SdkStatus()
    {
        if (!TryInit()) return -1;
        return sBridge.CallStatic<int>("getSdkStatus", sActivity);
    }

    public static void OpenHealthConnect()  => Call("openHealthConnect");
    public static void RequestPermission()  => Call("requestPermission"); // 내부적으로 Open과 동일 처리(설정 화면)
    public static void ReadTodaySteps()     => Call("readTodaySteps");
    public static void Disconnect()         => Call("disconnect");
#else
    public static int SdkStatus() => -1;
    public static void OpenHealthConnect() { }
    public static void RequestPermission() { }
    public static void ReadTodaySteps() { }
    public static void Disconnect() { }
#endif
}
