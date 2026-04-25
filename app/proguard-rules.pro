-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.** <fields>;
    @kotlinx.serialization.** <methods>;
}
-dontwarn org.slf4j.impl.StaticLoggerBinder
