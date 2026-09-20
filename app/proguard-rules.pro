# ==================== BioPay ProGuard Rules ====================

# ---- Xposed Module Entry Point ----
-keep class io.github.libxposed.api.XposedModule { *; }
-keep class io.github.libxposed.api.XposedModuleInterface { *; }
-keep class io.github.libxposed.api.XposedInterface { *; }
-adaptresourcefilecontents META-INF/xposed/java_init.list
-keep,allowoptimization public class * extends io.github.libxposed.api.XposedModule {
    public <init>();
}
-keepclassmembers class * extends io.github.libxposed.api.XposedModule {
    public void onModuleLoaded(*);
    public void onPackageLoaded(*);
    public boolean onHotReloading(*);
    public void onHotReloaded(*);
}

# ---- BiometricPrompt callbacks (passed to system API) ----
-keep class io.github.kiriashi.biopay.payment.BiometricGate$BiometricAuthCallback { *; }
-keep class io.github.kiriashi.biopay.payment.BiometricPaymentController$KeyboardAttachListener { *; }

# ---- settings.DialogHost is internal-only; safe to shrink/obfuscate ----

# ---- Reflection targets (WeChat internal classes) ----
# NOTE: 微信类名通过 StringEncoder 混淆后动态 loadClass，R8 无法静态分析引用关系
-dontwarn com.tencent.mm.**
-dontwarn com.tencent.kinda.**
-dontwarn com.tenpay.**

# ---- General Android rules ----
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---- Aggressive obfuscation ----
-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

# ---- Remove all logging in release (anti-detection) ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
