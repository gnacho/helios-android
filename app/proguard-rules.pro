# kotlinx.serialization keeps generated serializers through reflection names.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class club.cloudless.helios.** {
    *** Companion;
}
-keepclasseswithmembers class club.cloudless.helios.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp ships optional platform integrations that are not present here.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
